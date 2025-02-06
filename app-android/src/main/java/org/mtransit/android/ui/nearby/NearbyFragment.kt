@file:JvmName("NearbyFragment") // ANALYTICS
package org.mtransit.android.ui.nearby

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.FragmentNearbyBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.DeviceLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.inappnotification.locationpermission.LocationPermissionAwareFragment
import org.mtransit.android.ui.inappnotification.locationpermission.LocationPermissionUI
import org.mtransit.android.ui.inappnotification.locationsettings.LocationSettingsAwareFragment
import org.mtransit.android.ui.inappnotification.locationsettings.LocationSettingsUI
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledUI
import org.mtransit.android.ui.inappnotification.newlocation.NewLocationAwareFragment
import org.mtransit.android.ui.inappnotification.newlocation.NewLocationUI
import org.mtransit.android.ui.main.NextMainViewModel
import org.mtransit.android.ui.map.MapFragment
import org.mtransit.android.ui.setStatusBarHeight
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.MapUtils
import org.mtransit.commons.FeatureFlags

@AndroidEntryPoint
class NearbyFragment : ABFragment(R.layout.fragment_nearby),
    DeviceLocationListener,
    NewLocationAwareFragment,
    LocationSettingsAwareFragment,
    LocationPermissionAwareFragment,
    ModuleDisabledAwareFragment,
    MenuProvider {

    companion object {
        private val LOG_TAG = NearbyFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Nearby"

        @JvmStatic
        fun newFixedOnPOIInstanceArgs(
            poim: POIManager,
            dataSourcesRepository: DataSourcesRepository,
            simple: Boolean, // true = place...
        ) = newFixedOnInstanceArgs(
            optTypeId = if (simple) null else poim.poi.dataSourceTypeId,
            fixedOnLat = poim.lat.toFloat(),
            fixedOnLng = poim.lng.toFloat(),
            fixedOnName = if (simple) poim.poi.name else poim.getNewOneLineDescription(dataSourcesRepository),
            optFixedOnColorInt = if (simple) null else poim.getColor(dataSourcesRepository)
        )

        @JvmStatic
        fun newFixedOnPOIInstance(
            poim: POIManager,
            dataSourcesRepository: DataSourcesRepository,
            simple: Boolean, // true = place...
        ): NearbyFragment {
            return newFixedOnInstance(
                optTypeId = if (simple) null else poim.poi.dataSourceTypeId,
                fixedOnLat = poim.lat.toFloat(),
                fixedOnLng = poim.lng.toFloat(),
                fixedOnName = if (simple) poim.poi.name else poim.getNewOneLineDescription(dataSourcesRepository),
                optFixedOnColorInt = if (simple) null else poim.getColor(dataSourcesRepository)
            )
        }

        @JvmOverloads
        @JvmStatic
        fun newFixedOnInstanceArgs(
            optTypeId: Int? = null,
            fixedOnLat: Float,
            fixedOnLng: Float,
            fixedOnName: String,
            @ColorInt optFixedOnColorInt: Int? = null,
        ) = newInstanceArgs(
            optTypeId = optTypeId,
            optFixedOnLat = fixedOnLat,
            optFixedOnLng = fixedOnLng,
            optFixedOnName = fixedOnName,
            optFixedOnColor = optFixedOnColorInt?.let { ColorUtils.toRGBColor(it) }
        )

        @JvmOverloads
        @JvmStatic
        fun newFixedOnInstance(
            optTypeId: Int? = null,
            fixedOnLat: Float,
            fixedOnLng: Float,
            fixedOnName: String,
            @ColorInt optFixedOnColorInt: Int? = null,
        ): NearbyFragment {
            return newInstance(
                optTypeId = optTypeId,
                optFixedOnLat = fixedOnLat,
                optFixedOnLng = fixedOnLng,
                optFixedOnName = fixedOnName,
                optFixedOnColor = optFixedOnColorInt?.let { ColorUtils.toRGBColor(it) }
            )
        }

        @JvmOverloads
        @JvmStatic
        fun newNearbyInstance(optType: DataSourceType? = null) = newNearbyInstance(optTypeId = optType?.id)

        @JvmStatic
        fun newNearbyInstance(optTypeId: Int? = null) = newInstance(optTypeId = optTypeId)

        @JvmStatic
        fun newNearbyInstanceArgs(optType: DataSourceType? = null) = newInstanceArgs(optTypeId = optType?.id)

        private fun newInstance(
            optTypeId: Int? = null,
            optFixedOnLat: Float? = null,
            optFixedOnLng: Float? = null,
            optFixedOnName: String? = null,
            optFixedOnColor: String? = null,
        ): NearbyFragment {
            return NearbyFragment().apply {
                arguments = newInstanceArgs(optTypeId, optFixedOnLat, optFixedOnLng, optFixedOnName, optFixedOnColor)
            }
        }

        private fun newInstanceArgs(
            optTypeId: Int? = null,
            optFixedOnLat: Float? = null,
            optFixedOnLng: Float? = null,
            optFixedOnName: String? = null,
            optFixedOnColor: String? = null,
        ): Bundle {
            val validNearbyTypeId: Int? = optTypeId?.takeIf { DataSourceType.parseId(it)?.isNearbyScreen == true }
            return bundleOf(
                NearbyViewModel.EXTRA_SELECTED_TYPE to (validNearbyTypeId ?: NearbyViewModel.EXTRA_SELECTED_TYPE_DEFAULT),
                NearbyViewModel.EXTRA_FIXED_ON_LAT to (optFixedOnLat ?: NearbyViewModel.EXTRA_FIXED_ON_LAT_DEFAULT),
                NearbyViewModel.EXTRA_FIXED_ON_LNG to (optFixedOnLng ?: NearbyViewModel.EXTRA_FIXED_ON_LNG_DEFAULT),
                NearbyViewModel.EXTRA_FIXED_ON_NAME to (optFixedOnName ?: NearbyViewModel.EXTRA_FIXED_ON_NAME_DEFAULT),
                NearbyViewModel.EXTRA_FIXED_ON_COLOR to (optFixedOnColor ?: NearbyViewModel.EXTRA_FIXED_ON_COLOR_DEFAULT),
            )
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    override val viewModel by viewModels<NearbyViewModel>()
    override val attachedViewModel
        get() = if (isAttached()) viewModel else null
    private val nextMainViewModel by activityViewModels<NextMainViewModel>()

    override fun getContextView(): View? = this.binding?.contextView ?: this.view

    private var binding: FragmentNearbyBinding? = null

    private var showDirectionsMenuItem: MenuItem? = null
    private var mapMenuItem: MenuItem? = null

    private var lastPageSelected = -1
    private var selectedPosition = -1

    private var pagerAdapter: NearbyPagerAdapter? = null

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            attachedViewModel?.onPageSelected(position)
            lastPageSelected = position
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            selectedPosition = position
        }
    }

    private fun makePagerAdapter() = NearbyPagerAdapter(this).apply {
        setTypes(attachedViewModel?.availableTypes?.value)
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
        binding = FragmentNearbyBinding.bind(view).apply {
            viewPager.offscreenPageLimit = 3
            viewPager.registerOnPageChangeCallback(onPageChangeCallback)
            viewPager.adapter = pagerAdapter ?: makePagerAdapter().also { pagerAdapter = it } // cannot re-use Adapter w/ ViewPager
            TabLayoutMediator(tabs, viewPager, true, true) { tab, position ->
                tab.text = viewModel.availableTypes.value?.get(position)?.shortNamesResId?.let { viewPager.context.getString(it) }
            }.attach()
            if (FeatureFlags.F_NAVIGATION) {
                (activity as? org.mtransit.android.ui.main.NextMainActivity?)?.supportActionBar?.elevation?.let {
                    tabs.elevation = it
                }
            }
            showSelectedTab()
            switchView()
            fragmentStatusBarBg.setStatusBarHeight()
        }
        viewModel.availableTypes.observe(viewLifecycleOwner) {
            pagerAdapter?.setTypes(it)
            showSelectedTab()
            switchView()
        }
        viewModel.selectedTypePosition.observe(viewLifecycleOwner) {
            it?.let {
                if (this.lastPageSelected < 0) {
                    this.lastPageSelected = it
                    showSelectedTab()
                    switchView()
                    onPageChangeCallback.onPageSelected(this.lastPageSelected) // tell the current page it's selected
                }
            }
        }
        viewModel.isFixedOn.observe(viewLifecycleOwner) {
            updateMenuItemsVisibility(isFixedOn = it)
        }
        viewModel.hasAgenciesAdded.observe(viewLifecycleOwner) {
            updateMenuItemsVisibility(hasAgenciesAdded = it)
        }
        viewModel.fixedOnName.observe(viewLifecycleOwner) {
            abController?.setABTitle(this, getABTitle(context), false)
            if (FeatureFlags.F_NAVIGATION) {
                nextMainViewModel.setABTitle(getABTitle(context))
            }
        }
        viewModel.fixedOnColorInt.observe(viewLifecycleOwner) {
            abController?.setABBgColor(this, getABBgColor(context), true)
            if (FeatureFlags.F_NAVIGATION) {
                nextMainViewModel.setABBgColor(getABBgColor(context))
            }
            setupTabTheme(abBgColor = it)
        }
        LocationSettingsUI.onViewCreated(this)
        LocationPermissionUI.onViewCreated(this)
        ModuleDisabledUI.onViewCreated(this)
        NewLocationUI.onViewCreated(this)
        viewModel.nearbyLocationAddress.observe(viewLifecycleOwner) {
            abController?.setABSubtitle(this, getABSubtitle(context), false)
            if (FeatureFlags.F_NAVIGATION) {
                nextMainViewModel.setABSubtitle(getABSubtitle(context))
            }
            abController?.setABReady(this, isABReady, true)
            MTTransitions.startPostponedEnterTransitionOnPreDraw(view.parent as? ViewGroup, this)
        }
    }

    private fun setupTabTheme(abBgColor: Int? = getABBgColor(context)) = binding?.apply {
        tabs.setBackgroundColor(
            abBgColor.takeUnless { it == Color.TRANSPARENT }
                ?: getDefaultABBgColor(context)
        )
    }

    private fun switchView() {
        binding?.apply {
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
        binding?.viewPager?.doOnAttach {
            binding?.viewPager?.setCurrentItem(itemToSelect, smoothScroll)
        }
        this.selectedPosition = this.lastPageSelected // set selected position before update tabs color
    }

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        attachedViewModel?.onLocationSettingsResolution(resolution)
    }

    override fun onDeviceLocationChanged(newLocation: Location?) {
        attachedViewModel?.onDeviceLocationChanged(newLocation)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_nearby, menu)
        this.showDirectionsMenuItem = menu.findItem(R.id.menu_show_directions)
        this.mapMenuItem = menu.findItem(R.id.nav_map_custom)
        updateMenuItemsVisibility()
    }

    private fun updateMenuItemsVisibility(
        isFixedOn: Boolean? = viewModel.isFixedOn.value,
        hasAgenciesAdded: Boolean? = viewModel.hasAgenciesAdded.value,
    ) {
        showDirectionsMenuItem?.isVisible = hasAgenciesAdded == true && isFixedOn == true
        mapMenuItem?.isVisible = hasAgenciesAdded == true
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_show_directions -> {
                val locationPick = viewModel.fixedOnLocation.value ?: viewModel.nearbyLocation.value ?: viewModel.deviceLocation.value
                locationPick?.let { location ->
                    viewModel.onShowDirectionClick()
                    MapUtils.showDirection(
                        view,
                        requireActivity(),
                        location.latitude,
                        location.longitude,
                        null,
                        null,
                        viewModel.fixedOnName.value
                    )
                    true // handled
                } ?: false // not handled
            }

            R.id.nav_map_custom -> {
                val locationPick = viewModel.fixedOnLocation.value ?: viewModel.nearbyLocation.value ?: viewModel.deviceLocation.value
                locationPick?.let { location ->
                    (activity as? MainActivity)?.addFragmentToStack(
                        MapFragment.newInstance(
                            optInitialLocation = location,
                            optIncludeTypeId = viewModel.selectedTypeId.value
                        ),
                        this
                    )
                    true // handled
                } ?: false // not handled
            }

            else -> false // not handled
        }
    }

    override fun getABTitle(context: Context?): CharSequence? {
        return attachedViewModel?.fixedOnName?.value
            ?: context?.getString(R.string.nearby)
            ?: super.getABTitle(context)
    }

    override fun getABBgColor(context: Context?): Int? {
        return attachedViewModel?.fixedOnColorInt?.value
            ?: super.getABBgColor(context)
    }

    override fun getABSubtitle(context: Context?): CharSequence? {
        return attachedViewModel?.nearbyLocationAddress?.value ?: super.getABSubtitle(context)
    }

    override fun onResume() {
        super.onResume()
        switchView()
        viewModel.refreshLocationPermissionNeeded()
        (activity as? MTActivityWithLocation)?.let { onLocationSettingsResolution(it.lastLocationSettingsResolution) }
        (activity as? MTActivityWithLocation)?.let { onDeviceLocationChanged(it.lastLocation) }
        viewModel.checkIfNetworkLocationRefreshNecessary()
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.setABTitle(getABTitle(context))
            nextMainViewModel.setABSubtitle(getABSubtitle(context))
            nextMainViewModel.setABBgColor(getABBgColor(context))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.viewPager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding?.viewPager?.adapter = null // cannot re-use Adapter w/ ViewPager
        pagerAdapter = null // cannot re-use Adapter w/ ViewPager
        binding = null
    }
}