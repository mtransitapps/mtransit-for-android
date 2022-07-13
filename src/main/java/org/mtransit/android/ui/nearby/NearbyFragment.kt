@file:JvmName("NearbyFragment") // ANALYTICS
package org.mtransit.android.ui.nearby

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.FragmentNearbyBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.UserLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.main.MainViewModel
import org.mtransit.android.ui.map.MapFragment
import org.mtransit.android.ui.view.common.MTTabLayoutMediator
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.MapUtils
import org.mtransit.commons.FeatureFlags

@AndroidEntryPoint
class NearbyFragment : ABFragment(R.layout.fragment_nearby), UserLocationListener {

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

    private val viewModel by viewModels<NearbyViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null
    private val mainViewModel by activityViewModels<MainViewModel>()

    private var binding: FragmentNearbyBinding? = null

    private var showDirectionsMenuItem: MenuItem? = null

    private var lastPageSelected = -1
    private var selectedPosition = -1

    private var locationToast: PopupWindow? = null
    private var toastShown: Boolean = false

    private var adapter: NearbyPagerAdapter? = null

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

    private fun makeAdapter() = NearbyPagerAdapter(this).apply {
        setTypes(attachedViewModel?.availableTypes?.value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") // TODO use MenuProvider
        setHasOptionsMenu(true)
        MTTransitions.setContainerTransformTransition(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MTTransitions.postponeEnterTransition(this)
        binding = FragmentNearbyBinding.bind(view).apply {
            viewPager.offscreenPageLimit = 3
            viewPager.registerOnPageChangeCallback(onPageChangeCallback)
            viewPager.adapter = adapter ?: makeAdapter().also { adapter = it } // cannot re-use Adapter w/ ViewPager
            MTTabLayoutMediator(tabs, viewPager, autoRefresh = true, smoothScroll = true) { tab, position ->
                tab.text = viewModel.availableTypes.value?.get(position)?.shortNameResId?.let { viewPager.context.getString(it) }
            }.attach()
            if (FeatureFlags.F_NAVIGATION) {
                (activity as? org.mtransit.android.ui.main.MainActivity?)?.supportActionBar?.elevation?.let {
                    tabs.elevation = it
                }
            }
            showSelectedTab()
            switchView()
        }
        viewModel.availableTypes.observe(viewLifecycleOwner) {
            adapter?.setTypes(it)
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
            updateDirectionsMenuItem(it)
        }
        viewModel.fixedOnName.observe(viewLifecycleOwner) {
            abController?.setABTitle(this, getABTitle(context), false)
            if (FeatureFlags.F_NAVIGATION) {
                mainViewModel.setABTitle(getABTitle(context))
            }
        }
        viewModel.fixedOnColorInt.observe(viewLifecycleOwner) {
            abController?.setABBgColor(this, getABBgColor(context), false)
            if (FeatureFlags.F_NAVIGATION) {
                mainViewModel.setABBgColor(getABBgColor(context))
            }
            setupTabTheme()
        }
        viewModel.nearbyLocationAddress.observe(viewLifecycleOwner) {
            abController?.setABSubtitle(this, getABSubtitle(context), false)
            if (FeatureFlags.F_NAVIGATION) {
                mainViewModel.setABSubtitle(getABSubtitle(context))
            }
            abController?.setABReady(this, isABReady, true)
            MTTransitions.startPostponedEnterTransitionOnPreDraw(view.parent as? ViewGroup, this)
        }
        viewModel.newLocationAvailable.observe(viewLifecycleOwner) {
            if (it == true) {
                showLocationToast()
            } else {
                hideLocationToast()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeLocationToast(): PopupWindow? {
        return ToastUtils.getNewTouchableToast(context, R.drawable.toast_frame_old, R.string.new_location_toast)?.apply {
            setTouchInterceptor { _, me ->
                when (me.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val handled = attachedViewModel?.initiateRefresh() == true
                        hideLocationToast()
                        handled
                    }
                    else -> false // not handled
                }
            }
            setOnDismissListener {
                toastShown = false
            }
        }
    }

    private fun showLocationToast() {
        if (this.toastShown) {
            return // SKIP
        }
        val theActivity: Activity? = activity
        if (theActivity == null || theActivity.isFinishing || theActivity.isDestroyed) {
            return // SKIP
        }
        (this.locationToast ?: makeLocationToast().also { this.locationToast = it })
            ?.let { locationToast ->
                this.toastShown = ToastUtils.showTouchableToastPx(
                    activity,
                    locationToast,
                    view,
                    attachedViewModel?.getAdBannerHeightInPx(this) ?: 0
                )
            }
    }

    private fun hideLocationToast() {
        this.locationToast?.dismiss()
        this.toastShown = false
    }

    private fun setupTabTheme(abBgColor: Int? = getABBgColor(context)) {
        binding?.tabs?.apply {
            abBgColor?.let {
                setBackgroundColor(it)
            }
        }
    }

    private fun switchView() {
        binding?.apply {
            when {
                lastPageSelected < 0 || adapter?.isReady() != true -> { // LOADING
                    emptyLayout.isVisible = false
                    viewPager.isVisible = false
                    tabs.isVisible = false
                    loadingLayout.isVisible = true
                }
                adapter?.itemCount == 0 -> { // EMPTY
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
        if (this.adapter?.isReady() != true) {
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

    override fun onUserLocationChanged(newLocation: Location?) {
        attachedViewModel?.onDeviceLocationChanged(newLocation)
    }

    @Deprecated(message = "TODO use MenuProvider")
    @Suppress("DEPRECATION") // TODO use MenuProvider
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_nearby, menu)
        this.showDirectionsMenuItem = menu.findItem(R.id.menu_show_directions)
        updateDirectionsMenuItem()
    }

    private fun updateDirectionsMenuItem(isFixedOn: Boolean? = viewModel.isFixedOn.value) {
        showDirectionsMenuItem?.isVisible = isFixedOn == true
    }

    @Deprecated(message = "TODO use MenuProvider")
    @Suppress("DEPRECATION") // TODO use MenuProvider
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_show_directions -> {
                val locationPick = viewModel.fixedOnLocation.value ?: viewModel.nearbyLocation.value ?: viewModel.deviceLocation.value
                locationPick?.let { location ->
                    viewModel.onShowDirectionClick()
                    MapUtils.showDirection(
                        requireActivity(),
                        location.latitude,
                        location.longitude,
                        null,
                        null,
                        viewModel.fixedOnName.value
                    )
                    true // handled
                } ?: super.onOptionsItemSelected(item)
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
                } ?: super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
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

    override fun onPause() {
        super.onPause()
        hideLocationToast()
    }

    override fun onResume() {
        super.onResume()
        switchView()
        (activity as? MTActivityWithLocation)?.let { onUserLocationChanged(it.lastLocation) }
        if (FeatureFlags.F_NAVIGATION) {
            mainViewModel.setABTitle(getABTitle(context))
            mainViewModel.setABSubtitle(getABSubtitle(context))
            mainViewModel.setABBgColor(getABBgColor(context))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideLocationToast()
        this.locationToast = null
        this.toastShown = false
        binding?.viewPager?.unregisterOnPageChangeCallback(onPageChangeCallback)
        binding?.viewPager?.adapter = null // cannot re-use Adapter w/ ViewPager
        adapter = null // cannot re-use Adapter w/ ViewPager
        binding = null
    }
}