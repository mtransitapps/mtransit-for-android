@file:JvmName("RTSRouteFragment") // ANALYTICS // do not change to avoid breaking tracking
package org.mtransit.android.ui.rds.route

import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SpanUtils
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.data.RouteManager
import org.mtransit.android.data.decorateDirection
import org.mtransit.android.databinding.FragmentRdsRouteBinding
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.DeviceLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.applyStatusBarsHeightEdgeToEdge
import org.mtransit.android.ui.common.UIColorUtils
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.main.NextMainActivity
import org.mtransit.android.ui.serviceupdates.ServiceUpdatesDialog
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.FragmentUtils
import org.mtransit.android.util.UIRouteUtils
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class RDSRouteFragment : ABFragment(R.layout.fragment_rds_route),
    DeviceLocationListener,
    MenuProvider {

    companion object {
        private val LOG_TAG = RDSRouteFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "RTSRoute" // do not change to avoid breaking tracking

        private const val SHOW_SERVICE_UPDATE_IN_TOOLBAR = false
        // private const val SHOW_SERVICE_UPDATE_IN_TOOLBAR = true // TODO when we can only show stop specific in list

        private val TITLE_RSN_STYLE = SpanUtils.getNewBoldStyleSpan()

        private val TITLE_RLN_FONT = SpanUtils.getNewSansSerifLightTypefaceSpan()

        @JvmStatic
        fun newInstance(rds: RouteDirectionStop) = newInstance(rds.authority, rds.route.id, rds.direction.id, rds.stop.id)

        @JvmStatic
        fun newInstance(
            authority: String,
            routeId: Long,
            optSelectedTripId: Long? = null,
            optSelectedStopId: Int? = null,
        ): RDSRouteFragment {
            return RDSRouteFragment().apply {
                arguments = newInstanceArgs(authority, routeId, optSelectedTripId, optSelectedStopId)
            }
        }

        @JvmStatic
        fun newInstanceArgs(rds: RouteDirectionStop) = newInstanceArgs(rds.authority, rds.route.id, rds.direction.id, rds.stop.id)

        @JvmStatic
        fun newInstanceArgs(
            authority: String,
            routeId: Long,
            optSelectedTripId: Long? = null,
            optSelectedStopId: Int? = null,
        ) = bundleOf(
            RDSRouteViewModel.EXTRA_AUTHORITY to authority,
            RDSRouteViewModel.EXTRA_ROUTE_ID to routeId,
            RDSRouteViewModel.EXTRA_SELECTED_DIRECTION_ID to (optSelectedTripId ?: RDSRouteViewModel.EXTRA_SELECTED_DIRECTION_ID_DEFAULT),
            RDSRouteViewModel.EXTRA_SELECTED_STOP_ID to (optSelectedStopId ?: RDSRouteViewModel.EXTRA_SELECTED_STOP_ID_DEFAULT),
        )
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String {
        val authority = attachedViewModel?.authority?.value
        val routeId = attachedViewModel?.routeId?.value
        if (authority != null && routeId != null) {
            return "$TRACKING_SCREEN_NAME/$authority/$routeId"
        }
        return TRACKING_SCREEN_NAME
    }

    private val viewModel by viewModels<RDSRouteViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    private var binding: FragmentRdsRouteBinding? = null

    private var lastPageSelected = -1
    private var selectedPosition = -1

    private var pagerAdapter: RDSRouteDirectionPagerAdapter? = null

    private fun makePagerAdapter() = RDSRouteDirectionPagerAdapter(this).apply {
        setSelectedStopId(attachedViewModel?.selectedStopId?.value)
        setAuthority(attachedViewModel?.authority?.value)
        setRouteDirections(attachedViewModel?.routeDirections?.value)
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewModel.onPageSelected(position)
            lastPageSelected = position
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            if (abs(lastPageSelected - position) > 1) {
                return // TODO really?
            }
            selectedPosition = position
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MTTransitions.setContainerTransformTransition(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MTTransitions.postponeEnterTransition(this)
        (requireActivity() as MenuHost).addMenuProvider(
            this, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
        binding = FragmentRdsRouteBinding.bind(view).apply {
            viewPager.apply {
                offscreenPageLimit = 1
                registerOnPageChangeCallback(onPageChangeCallback)
                adapter = pagerAdapter ?: makePagerAdapter().also { pagerAdapter = it } // cannot re-use Adapter w/ ViewPager
                TabLayoutMediator(tabs, viewPager, true, true) { tab, position ->
                    tab.text = viewModel.routeDirections.value?.get(position)?.decorateDirection(this.context, small = false, centered = false)
                }.attach()
            }
            if (FeatureFlags.F_NAVIGATION) {
                (activity as? NextMainActivity?)?.supportActionBar?.elevation?.let {
                    tabs.elevation = it
                }
            }
            attachedViewModel?.colorInt?.value?.let {
                routeDirectionBackground.setBackgroundColor(UIColorUtils.adaptBackgroundColorToLightText(view.context, it))
            }
            showSelectedTab()
            fragmentStatusBarBg.applyStatusBarsHeightEdgeToEdge()
        }
        viewModel.selectedStopId.observe(viewLifecycleOwner) { selectedStopId ->
            this.pagerAdapter?.setSelectedStopId(selectedStopId)
        }
        viewModel.authority.observe(viewLifecycleOwner) { authority ->
            this.pagerAdapter?.setAuthority(authority)
            switchView()
            MTTransitions.setTransitionName(
                view,
                authority?.let {
                    viewModel.routeM.value?.route?.id?.let { routeId ->
                        "r_" + authority + "_" + routeId
                    }
                }
            )
        }
        viewModel.routeM.observe(viewLifecycleOwner) { routeM ->
            MTLog.d(this, "routeM.onChange(${routeM?.route?.id})")
            MTTransitions.setTransitionName(
                view,
                routeM?.route?.id?.let { routeId ->
                    viewModel.authority.value?.let { authority ->
                        "r_" + authority + "_" + routeId
                    }
                }
            )
            attachedViewModel?.colorInt?.value?.let {
                binding?.routeDirectionBackground?.setBackgroundColor(UIColorUtils.adaptBackgroundColorToLightText(view.context, it))
            }
            abController?.setABBgColor(this, getABBgColor(context), false)
            abController?.setABTitle(this, getABTitle(context), false)
            abController?.setABReady(this, isABReady, true)
            updateServiceUpdateImg(routeM = routeM)
        }
        viewModel.serviceUpdateLoadedEvent.observe(viewLifecycleOwner, EventObserver { triggered ->
            updateServiceUpdateImg()
        })
        viewModel.colorInt.observe(viewLifecycleOwner) { it ->
            it?.let {
                binding?.routeDirectionBackground?.setBackgroundColor(UIColorUtils.adaptBackgroundColorToLightText(view.context, it))
            }
            abController?.setABBgColor(this, getABBgColor(context), true)
        }
        viewModel.routeDirections.observe(viewLifecycleOwner) { routeDirections ->
            if (pagerAdapter?.setRouteDirections(routeDirections) == true) {
                showSelectedTab()
                abController?.setABBgColor(this, getABBgColor(context), true)
            } else {
                switchView()
            }
            routeDirections?.let {
                MTTransitions.startPostponedEnterTransitionOnPreDraw(view.parent as? ViewGroup, this)
            }
        }
        viewModel.selectedRouteDirectionPosition.observe(viewLifecycleOwner) { newSelectedRouteDirectionPosition ->
            newSelectedRouteDirectionPosition?.let {
                if (this.lastPageSelected < 0) {
                    this.lastPageSelected = it
                    showSelectedTab()
                    onPageChangeCallback.onPageSelected(this.lastPageSelected) // tell the current page it's selected
                }
            }
        }
        viewModel.dataSourceRemovedEvent.observe(viewLifecycleOwner, EventObserver { removed ->
            if (removed) {
                (activity as MainActivity?)?.popFragmentFromStack(this) // close this fragment
            }
        })
    }

    private var serviceUpdateImg: MenuItem? = null

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_rds_route, menu)
        this.serviceUpdateImg = menu.findItem(R.id.menu_service_update_img)
        updateServiceUpdateImg(serviceUpdateImg = this.serviceUpdateImg)
    }

    private fun updateServiceUpdateImg(
        routeM: RouteManager? = attachedViewModel?.routeM?.value,
        serviceUpdateImg: MenuItem? = this.serviceUpdateImg,
    ) {
        serviceUpdateImg?.apply {
            routeM ?: run { isVisible = false; return }
            val (isWarning, isInfo) = routeM.getServiceUpdates(serviceUpdateLoader)
                .let {
                    ServiceUpdate.isSeverityWarning(it) to ServiceUpdate.isSeverityInfo(it)
                }
            if (isWarning) {
                setIcon(R.drawable.ic_warning_black_24dp)
                isVisible = SHOW_SERVICE_UPDATE_IN_TOOLBAR
            } else if (isInfo) {
                setIcon(R.drawable.ic_info_outline_black_24dp)
                isVisible = SHOW_SERVICE_UPDATE_IN_TOOLBAR
            } else {
                icon = null
                isVisible = false
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.menu_service_update_img) {
            val authority = viewModel.authority.value ?: return false
            val routeId = viewModel.routeM.value?.route?.id ?: return false
            if (FeatureFlags.F_NAVIGATION) {
                // TODO navigate to dialog
            } else {
                FragmentUtils.replaceDialogFragment(
                    activity ?: return false,
                    FragmentUtils.DIALOG_TAG,
                    ServiceUpdatesDialog.newInstance(authority, routeId),
                    null
                )
                return true // handled
            }
        }
        return false // not handled
    }

    override fun onResume() {
        super.onResume()
        switchView()
        showSelectedTab()
        (activity as? MTActivityWithLocation)?.let { onLocationSettingsResolution(it.lastLocationSettingsResolution) }
        (activity as? MTActivityWithLocation)?.let { onDeviceLocationChanged(it.lastLocation) }
    }

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        attachedViewModel?.onLocationSettingsResolution(resolution)
    }

    override fun onDeviceLocationChanged(newLocation: Location?) {
        attachedViewModel?.onDeviceLocationChanged(newLocation)
    }

    private fun switchView() = binding?.apply {
        when {
            lastPageSelected < 0 || pagerAdapter?.isReady() != true -> { // LOADING
                emptyLayout.isVisible = false
                viewPager.isVisible = false
                tabs.isVisible = false
                loadingLayout.isVisible = true
            }

            pagerAdapter?.itemCount == 0 -> { // EMPTY
                loadingLayout.isVisible = false
                viewPager.isVisible = false
                tabs.isVisible = false
                emptyLayout.isVisible = true
            }

            else -> { // LOADED
                loadingLayout.isVisible = false
                emptyLayout.isVisible = false
                tabs.isVisible = true
                viewPager.isVisible = true
            }
        }
    }

    private fun showSelectedTab() {
        if (this.pagerAdapter?.isReady() != true) {
            MTLog.d(this, "showSelectedTab() > SKIP (no adapter items)")
            return
        }
        if (this.lastPageSelected < 0) {
            MTLog.d(this, "showSelectedTab() > SKIP (no last page selected)")
            return
        }
        val smoothScroll = this.selectedPosition >= 0
        val itemToSelect = this.lastPageSelected
        binding?.apply {
            viewPager.doOnAttach {
                viewPager.setCurrentItem(itemToSelect, smoothScroll)
            }
        }
        this.selectedPosition = this.lastPageSelected // set selected position before update tabs color
        switchView()
    }

    override fun isABReady() = attachedViewModel?.routeM?.value != null

    override fun isABStatusBarTransparent() = true

    override fun isABOverrideGradient() = true

    override fun getABTitle(context: Context?): CharSequence? {
        return attachedViewModel?.routeM?.value?.let { makeABTitle(it.route) }
            ?: super.getABTitle(context)
    }

    private fun makeABTitle(route: Route): CharSequence {
        var ssb = SpannableStringBuilder()
        var startShortName = 0
        var endShortName = 0
        if (route.shortName.isNotBlank()) {
            startShortName = ssb.length
            ssb.append(route.shortName)
            endShortName = ssb.length
        }
        var startLongName = 0
        var endLongName = 0
        if (route.longName.isNotBlank()) {
            if (ssb.isNotEmpty()) {
                ssb.append(StringUtils.SPACE_CAR).append(StringUtils.SPACE_CAR)
            }
            startLongName = ssb.length
            ssb.append(route.longName)
            endLongName = ssb.length
        }
        if (startShortName < endShortName) {
            ssb = SpanUtils.setNN(
                ssb, startShortName, endShortName,  //
                UIRouteUtils.getRouteShortNameFont(route.shortName),
                TITLE_RSN_STYLE
            )
        }
        if (startLongName < endLongName) {
            ssb = SpanUtils.setNN(ssb, startLongName, endLongName, TITLE_RLN_FONT)
        }
        return ssb
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.viewPager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding?.viewPager?.adapter = null // cannot re-use Adapter w/ ViewPager
        pagerAdapter = null // cannot re-use Adapter w/ ViewPager
        binding = null
    }
}