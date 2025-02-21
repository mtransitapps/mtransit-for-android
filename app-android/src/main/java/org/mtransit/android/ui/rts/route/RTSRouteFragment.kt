@file:JvmName("RTSRouteFragment") // ANALYTICS
package org.mtransit.android.ui.rts.route

import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SpanUtils
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.data.decorateDirection
import org.mtransit.android.databinding.FragmentRtsRouteBinding
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.DeviceLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.common.UIColorUtils
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.main.NextMainActivity
import org.mtransit.android.ui.applyStatusBarsHeightEdgeToEdge
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.UIRouteUtils
import org.mtransit.commons.FeatureFlags
import kotlin.math.abs

@AndroidEntryPoint
class RTSRouteFragment : ABFragment(R.layout.fragment_rts_route), DeviceLocationListener {

    companion object {
        private val LOG_TAG = RTSRouteFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "RTSRoute"

        private val TITLE_RSN_STYLE = SpanUtils.getNewBoldStyleSpan()

        private val TITLE_RLN_FONT = SpanUtils.getNewSansSerifLightTypefaceSpan()

        @JvmStatic
        fun newInstance(rts: RouteTripStop) = newInstance(rts.authority, rts.route.id, rts.trip.id, rts.stop.id)

        @JvmStatic
        fun newInstance(
            authority: String,
            routeId: Long,
            optSelectedTripId: Long? = null,
            optSelectedStopId: Int? = null,
        ): RTSRouteFragment {
            return RTSRouteFragment().apply {
                arguments = newInstanceArgs(authority, routeId, optSelectedTripId, optSelectedStopId)
            }
        }

        @JvmStatic
        fun newInstanceArgs(rts: RouteTripStop) = newInstanceArgs(rts.authority, rts.route.id, rts.trip.id, rts.stop.id)

        @JvmStatic
        fun newInstanceArgs(
            authority: String,
            routeId: Long,
            optSelectedTripId: Long? = null,
            optSelectedStopId: Int? = null,
        ) = bundleOf(
            RTSRouteViewModel.EXTRA_AUTHORITY to authority,
            RTSRouteViewModel.EXTRA_ROUTE_ID to routeId,
            RTSRouteViewModel.EXTRA_SELECTED_TRIP_ID to (optSelectedTripId ?: RTSRouteViewModel.EXTRA_SELECTED_TRIP_ID_DEFAULT),
            RTSRouteViewModel.EXTRA_SELECTED_STOP_ID to (optSelectedStopId ?: RTSRouteViewModel.EXTRA_SELECTED_STOP_ID_DEFAULT),
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

    private val viewModel by viewModels<RTSRouteViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    private var binding: FragmentRtsRouteBinding? = null

    private var lastPageSelected = -1
    private var selectedPosition = -1

    private var pagerAdapter: RTSRouteTripPagerAdapter? = null

    private fun makePagerAdapter() = RTSRouteTripPagerAdapter(this).apply {
        setSelectedStopId(attachedViewModel?.selectedStopId?.value)
        setAuthority(attachedViewModel?.authority?.value)
        setRouteTrips(attachedViewModel?.routeTrips?.value)
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
        binding = FragmentRtsRouteBinding.bind(view).apply {
            viewPager.apply {
                offscreenPageLimit = 1
                registerOnPageChangeCallback(onPageChangeCallback)
                adapter = pagerAdapter ?: makePagerAdapter().also { pagerAdapter = it } // cannot re-use Adapter w/ ViewPager
                TabLayoutMediator(tabs, viewPager, true, true) { tab, position ->
                    tab.text = viewModel.routeTrips.value?.get(position)?.decorateDirection(this.context, small = false, centered = false)
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
                    viewModel.route.value?.id?.let { routeId ->
                        "r_" + authority + "_" + routeId
                    }
                }
            )
        }
        viewModel.route.observe(viewLifecycleOwner) { route ->
            MTTransitions.setTransitionName(
                view,
                route?.id?.let { routeId ->
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
        }
        viewModel.colorInt.observe(viewLifecycleOwner) { it ->
            it?.let {
                binding?.routeDirectionBackground?.setBackgroundColor(UIColorUtils.adaptBackgroundColorToLightText(view.context, it))
            }
            abController?.setABBgColor(this, getABBgColor(context), true)
        }
        viewModel.routeTrips.observe(viewLifecycleOwner) { routeTrips ->
            if (pagerAdapter?.setRouteTrips(routeTrips) == true) {
                showSelectedTab()
                abController?.setABBgColor(this, getABBgColor(context), true)
            } else {
                switchView()
            }
            routeTrips?.let {
                MTTransitions.startPostponedEnterTransitionOnPreDraw(view.parent as? ViewGroup, this)
            }
        }
        viewModel.selectedRouteTripPosition.observe(viewLifecycleOwner) { newSelectedRouteTripPosition ->
            newSelectedRouteTripPosition?.let {
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

    override fun isABReady() = attachedViewModel?.route?.value != null

    override fun isABStatusBarTransparent() = true

    override fun isABOverrideGradient() = true

    override fun getABTitle(context: Context?): CharSequence? {
        return attachedViewModel?.route?.value?.let { makeABTitle(it) }
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