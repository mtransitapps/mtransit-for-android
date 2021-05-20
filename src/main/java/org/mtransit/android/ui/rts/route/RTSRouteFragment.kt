@file:JvmName("RTSRouteFragment") // ANALYTICS
package org.mtransit.android.ui.rts.route

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.location.Location
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.StateSet
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
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
import org.mtransit.android.databinding.FragmentRtsRouteBinding
import org.mtransit.android.databinding.LayoutEmptyBinding
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.UserLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.view.common.EventObserver
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class RTSRouteFragment : ABFragment(R.layout.fragment_rts_route), UserLocationListener {

    companion object {
        private val LOG_TAG = RTSRouteFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "RTSRoute"

        private val TITLE_RSN_FONT = SpanUtils.getNewSansSerifCondensedTypefaceSpan()
        private val TITLE_RSN_STYLE = SpanUtils.getNewBoldStyleSpan()
        private val TITLE_RLN_FONT = SpanUtils.getNewSansSerifLightTypefaceSpan()

        @JvmStatic
        fun newInstance(
            rts: RouteTripStop
        ): RTSRouteFragment {
            return newInstance(
                rts.authority,
                rts.route.id,
                rts.trip.id,
                rts.stop.id
            )
        }

        @JvmStatic
        fun newInstance(
            authority: String,
            routeId: Long,
            optSelectedTripId: Long? = null,
            optSelectedStopId: Int? = null
        ): RTSRouteFragment {
            return RTSRouteFragment().apply {
                arguments = bundleOf(
                    RTSRouteViewModel.EXTRA_AUTHORITY to authority,
                    RTSRouteViewModel.EXTRA_ROUTE_ID to routeId,
                    RTSRouteViewModel.EXTRA_SELECTED_TRIP_ID to optSelectedTripId,
                    RTSRouteViewModel.EXTRA_SELECTED_STOP_ID to optSelectedStopId,
                )
            }
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String {
        val authority = viewModel.authority.value
        val routeId = viewModel.routeId.value
        if (authority != null && routeId != null) {
            return "$TRACKING_SCREEN_NAME/$authority/$routeId"
        }
        return TRACKING_SCREEN_NAME
    }

    private val viewModel by viewModels<RTSRouteViewModel>()

    private var binding: FragmentRtsRouteBinding? = null
    private var emptyBinding: LayoutEmptyBinding? = null

    private var listMapToggleMenuItem: MenuItem? = null
    private var listMapSwitchMenuItem: SwitchCompat? = null

    private val listMapToggleSelector: StateListDrawable by lazy {
        StateListDrawable().apply {
            (ResourcesCompat.getDrawable(resources, R.drawable.switch_thumb_list, requireContext().theme) as? LayerDrawable)?.apply {
                viewModel.colorInt.value?.let { (findDrawableByLayerId(R.id.switch_list_oval_shape) as? GradientDrawable)?.setColor(it) }
                addState(intArrayOf(android.R.attr.state_checked), this)
            }
            (ResourcesCompat.getDrawable(resources, R.drawable.switch_thumb_map, requireContext().theme) as? LayerDrawable)?.apply {
                viewModel.colorInt.value?.let { (findDrawableByLayerId(R.id.switch_map_oval_shape) as? GradientDrawable)?.setColor(it) }
                addState(StateSet.WILD_CARD, this)
            }
        }
    }

    private var lastPageSelected = -1
    private var selectedPosition = -1

    private var adapter: RTSRouteTripPagerAdapter? = null

    private fun makeAdapter() = RTSRouteTripPagerAdapter(this).apply {
        setSelectedStopId(viewModel.selectedStopId.value)
        setAuthority(viewModel.authority.value)
        setRouteTrips(viewModel.routeTrips.value)
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewModel.onPagetSelected(position)
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
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRtsRouteBinding.bind(view).apply {
            emptyStub.setOnInflateListener { _, inflated ->
                emptyBinding = LayoutEmptyBinding.bind(inflated)
            }
            viewpager.offscreenPageLimit = 1
            viewpager.registerOnPageChangeCallback(onPageChangeCallback)
            viewpager.adapter = adapter ?: makeAdapter().also { adapter = it } // cannot re-use Adapter w/ ViewPager
            TabLayoutMediator(tabs, viewpager) { tab, position ->
                @Suppress("DEPRECATION") // TODO Kotlin 1.5.10
                tab.text = viewModel.routeTrips.value?.get(position)?.getHeading(viewpager.context)?.toUpperCase(Locale.getDefault())
            }.attach()
            getABBgColor(tabs.context)?.let { tabs.setBackgroundColor(it) }
            showSelectedTab()
        }
        viewModel.selectedStopId.observe(viewLifecycleOwner, { selectedStopId ->
            this.adapter?.setSelectedStopId(selectedStopId)
        })
        viewModel.authority.observe(viewLifecycleOwner, { authority ->
            this.adapter?.setAuthority(authority)
            switchView()
        })
        viewModel.route.observe(viewLifecycleOwner, {
            binding?.apply {
                getABBgColor(tabs.context)?.let { tabs.setBackgroundColor(it) }
            }
            abController?.setABBgColor(this, getABBgColor(context), false)
            abController?.setABTitle(this, getABTitle(context), false)
            abController?.setABReady(this, isABReady, true)
        })
        viewModel.colorInt.observe(viewLifecycleOwner, {
            binding?.apply {
                getABBgColor(tabs.context)?.let { tabs.setBackgroundColor(it) }
            }
            abController?.setABBgColor(this, getABBgColor(context), true)
            activity?.invalidateOptionsMenu() // initialize action bar list/grid switch icon
            updateListMapToggleMenuItem()
        })
        viewModel.routeTrips.observe(viewLifecycleOwner, { routeTrips ->
            if (adapter?.setRouteTrips(routeTrips) == true) {
                showSelectedTab()
                abController?.setABBgColor(this, getABBgColor(context), true)
            } else {
                switchView()
            }
        })
        viewModel.selectedRouteTripPosition.observe(viewLifecycleOwner, { newSelectedRouteTripPosition ->
            newSelectedRouteTripPosition?.let {
                if (this.lastPageSelected < 0) {
                    this.lastPageSelected = it
                    showSelectedTab()
                    onPageChangeCallback.onPageSelected(this.lastPageSelected) // tell the current page it's selected
                }
            }
        })
        viewModel.showingListInsteadOfMap.observe(viewLifecycleOwner, {
            updateListMapToggleMenuItem()
        })
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
        updateListMapToggleMenuItem()
        (activity as? MTActivityWithLocation)?.let { onUserLocationChanged(it.lastLocation) }
    }

    override fun onUserLocationChanged(newLocation: Location?) {
        viewModel.onDeviceLocationChanged(newLocation)
        MTActivityWithLocation.broadcastUserLocationChanged(this, childFragmentManager.fragments, newLocation) // TODO remove later
    }

    private fun switchView() {
        binding?.apply {
            when {
                lastPageSelected < 0 || adapter?.itemCount ?: 0 < 0 -> { // LOADING
                    emptyBinding?.root?.isVisible = false
                    viewpager.isVisible = false
                    tabs.isVisible = false
                    loading.root.isVisible = true
                }
                adapter?.itemCount == 0 -> { // EMPTY
                    loading.root.isVisible = false
                    viewpager.isVisible = false
                    tabs.isVisible = false
                    (emptyBinding?.root ?: emptyStub.inflate()).isVisible = true
                }
                else -> { // LOADED
                    loading.root.isVisible = false
                    emptyBinding?.root?.isVisible = false
                    tabs.isVisible = true
                    viewpager.isVisible = true
                }
            }
        }
    }

    private fun showSelectedTab() {
        if (this.adapter?.itemCount ?: -1 <= 0) {
            MTLog.d(this, "showSelectedTab() > SKIP (no adapter items)")
            return
        }
        if (this.lastPageSelected < 0) {
            MTLog.d(this, "showSelectedTab() > SKIP (no last page selected)")
            return
        }
        val smoothScroll = this.selectedPosition >= 0
        val itemToSelect = this.lastPageSelected
        binding?.viewpager?.doOnAttach {
            binding?.viewpager?.setCurrentItem(itemToSelect, smoothScroll)
        }
        this.selectedPosition = this.lastPageSelected // set selected position before update tabs color
        switchView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_rts_route, menu)
        listMapToggleMenuItem = menu.findItem(R.id.menu_toggle_list_map)
        listMapSwitchMenuItem = listMapToggleMenuItem?.actionView?.findViewById(R.id.action_bar_switch_list_map)
        listMapSwitchMenuItem?.thumbDrawable = viewModel.colorInt.value?.let { listMapToggleSelector }
        listMapSwitchMenuItem?.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            onCheckedChanged(buttonView, isChecked)
        }
        updateListMapToggleMenuItem()
    }

    private fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (buttonView.id == R.id.action_bar_switch_list_map) {
            viewModel.saveShowingListInsteadOfMap(isChecked)
        }
    }

    fun updateListMapToggleMenuItem() {
        listMapSwitchMenuItem?.isChecked = viewModel.showingListInsteadOfMap.value != false
        listMapSwitchMenuItem?.isVisible = true
        listMapToggleMenuItem?.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_bar_switch_list_map) {
            viewModel.saveShowingListInsteadOfMap(viewModel.showingListInsteadOfMap.value == false) // switching
            return true // handled
        }
        return super.onOptionsItemSelected(item)
    }

    override fun isABReady() = viewModel.route.value != null

    override fun getABBgColor(context: Context?) = viewModel.colorInt.value ?: super.getABBgColor(context)

    override fun getABTitle(context: Context?): CharSequence? {
        return viewModel.route.value?.let { makeABTitle(it) }
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
                TITLE_RSN_FONT, TITLE_RSN_STYLE
            )
        }
        if (startLongName < endLongName) {
            ssb = SpanUtils.setNN(ssb, startLongName, endLongName, TITLE_RLN_FONT)
        }
        return ssb
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.viewpager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding?.viewpager?.adapter = null // cannot re-use Adapter w/ ViewPager
        adapter = null // cannot re-use Adapter w/ ViewPager
        emptyBinding = null
        binding = null
    }
}