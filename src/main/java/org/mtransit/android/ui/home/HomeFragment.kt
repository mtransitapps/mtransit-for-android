@file:JvmName("HomeFragment") // ANALYTICS
package org.mtransit.android.ui.home

import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentHomeBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.DeviceLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.fragment.POIFragment
import org.mtransit.android.ui.inappnotification.locationsettings.LocationSettingsAwareFragment
import org.mtransit.android.ui.inappnotification.locationsettings.LocationSettingsUI
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledUI
import org.mtransit.android.ui.inappnotification.newlocation.NewLocationAwareFragment
import org.mtransit.android.ui.inappnotification.newlocation.NewLocationUI
import org.mtransit.android.ui.main.MainViewModel
import org.mtransit.android.ui.map.MapFragment
import org.mtransit.android.ui.nearby.NearbyFragment
import org.mtransit.android.ui.type.AgencyTypeFragment
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class HomeFragment : ABFragment(R.layout.fragment_home),
    DeviceLocationListener,
    NewLocationAwareFragment,
    LocationSettingsAwareFragment,
    ModuleDisabledAwareFragment,
    MenuProvider {

    companion object {
        private val LOG_TAG = HomeFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Home"

        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    override val viewModel by viewModels<HomeViewModel>()
    override val attachedViewModel
        get() = if (isAttached()) viewModel else null
    private val mainViewModel by activityViewModels<MainViewModel>()

    @Inject
    lateinit var sensorManager: MTSensorManager

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var defaultPrefRepository: DefaultPreferenceRepository

    @Inject
    lateinit var poiRepository: POIRepository

    @Inject
    lateinit var favoriteManager: FavoriteManager

    @Inject
    lateinit var statusLoader: StatusLoader

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    @Inject
    lateinit var demoModeManager: DemoModeManager

    private var binding: FragmentHomeBinding? = null

    private val infiniteLoadingListener = object : POIArrayAdapter.InfiniteLoadingListener {
        override fun isLoadingMore(): Boolean {
            return attachedViewModel?.loadingPOIs?.value == true
        }

        override fun showingDone() = false
    }

    private val typeHeaderButtonsClickListener = POIArrayAdapter.TypeHeaderButtonsClickListener { buttonId, type ->
        when (buttonId) {
            POIArrayAdapter.TypeHeaderButtonsClickListener.BUTTON_MORE -> {
                if (FeatureFlags.F_NAVIGATION) {
                    var extras: FragmentNavigator.Extras? = null
                    if (FeatureFlags.F_TRANSITION) {
                        extras = null // TODO button ? extras = FragmentNavigatorExtras(view to view.transitionName)
                    }
                    findNavController().navigate(
                        R.id.nav_to_nearby_screen,
                        NearbyFragment.newNearbyInstanceArgs(type),
                        null,
                        extras
                    )
                } else {
                    (activity as? MainActivity)?.addFragmentToStack(
                        NearbyFragment.newNearbyInstance(type),
                        this@HomeFragment
                    )
                }
                true
            }

            else -> false
        }
    }

    private val adapter: POIArrayAdapter by lazy {
        POIArrayAdapter(
            this,
            this.sensorManager,
            this.dataSourcesRepository,
            this.defaultPrefRepository,
            this.poiRepository,
            this.favoriteManager,
            this.statusLoader,
            this.serviceUpdateLoader
        ).apply {
            logTag = this@HomeFragment.logTag
            setFavoriteUpdateListener { adapter.onFavoriteUpdated() }
            setShowBrowseHeaderSection(true)
            setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE)
            setShowTypeHeaderNearby(true)
            setInfiniteLoading(true)
            setInfiniteLoadingListener(infiniteLoadingListener)
            setOnTypeHeaderButtonsClickListener(typeHeaderButtonsClickListener)
            setPois(attachedViewModel?.nearbyPOIs?.value)
            setLocation(attachedViewModel?.deviceLocation?.value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MTTransitions.setContainerTransformTransition(this)
        // if (FeatureFlags.F_TRANSITION) {
        // exitTransition = MTTransitions.newHoldTransition() // not working with AdapterView // FIXME #RecyclerView
        // }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MTTransitions.postponeEnterTransition(this)
        (requireActivity() as MenuHost).addMenuProvider(
            this, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
        binding = FragmentHomeBinding.bind(view).apply {
            listLayout.list.let { listView ->
                swipeRefresh.setListViewWR(listView)
                listView.isVisible = adapter.isInitialized
                adapter.setListView(listView)
                MTTransitions.startPostponedEnterTransitionOnPreDraw(view.parent as? ViewGroup, this@HomeFragment)
            }
            swipeRefresh.apply {
                setColorSchemeColors(
                    ThemeUtils.resolveColorAttribute(view.context, android.R.attr.colorAccent)
                )
                setOnRefreshListener {
                    if (!viewModel.initiateRefresh()) {
                        isRefreshing = false
                    }
                }
            }
        }
        LocationSettingsUI.onViewCreated(this)
        viewModel.deviceLocation.observe(viewLifecycleOwner) {
            adapter.setLocation(it)
        }
        viewModel.nearbyLocationAddress.observe(viewLifecycleOwner) {
            abController?.setABSubtitle(this, getABSubtitle(context), true)
            if (FeatureFlags.F_NAVIGATION) {
                mainViewModel.setABSubtitle(getABSubtitle(context))
            }
        }
        viewModel.nearbyPOIsTriggerListener.observe(viewLifecycleOwner) {
            // DO NOTHING
        }
        viewModel.nearbyPOIsTrigger.observe(viewLifecycleOwner, EventObserver { triggered ->
            if (triggered) {
                adapter.clear()
            }
        })
        viewModel.nearbyPOIs.observe(viewLifecycleOwner) {
            it?.let {
                val scrollToTop = adapter.poisCount <= 0
                adapter.appendPois(it)
                if (scrollToTop) {
                    binding?.listLayout?.list?.setSelection(0)
                }
                if (isResumed) {
                    adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
                } else {
                    adapter.onPause()
                }
                switchView()
            }
        }
        viewModel.loadingPOIs.observe(viewLifecycleOwner) {
            if (it == false) {
                binding?.swipeRefresh?.isRefreshing = false
            }
        }
        NewLocationUI.onViewCreated(this)
        ModuleDisabledUI.onViewCreated(this)
        if (FeatureFlags.F_NAVIGATION) {
            mainViewModel.scrollToTopEvent.observe(viewLifecycleOwner, EventObserver { scroll ->
                if (scroll) {
                    binding?.listLayout?.list?.setSelection(0)
                }
            })
        }
    }

    private fun switchView() {
        binding?.apply {
            loadingLayout.isVisible = false
            emptyLayout.isVisible = false
            listLayout.isVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        this.adapter.onResume(this, viewModel.deviceLocation.value)
        switchView()
        (activity as? MTActivityWithLocation)?.let { onLocationSettingsResolution(it.lastLocationSettingsResolution) }
        (activity as? MTActivityWithLocation)?.checkLocationSettings()
        (activity as? MTActivityWithLocation)?.let { onDeviceLocationChanged(it.lastLocation) }
        if (FeatureFlags.F_NAVIGATION) {
            mainViewModel.setABTitle(getABTitle(context))
            mainViewModel.setABSubtitle(getABSubtitle(context))
        }
        if (demoModeManager.isEnabledPOIScreen()) {
            val poiAuthority = demoModeManager.filterAgencyAuthority ?: throw RuntimeException("Demo mode: missing authority!")
            val poiUUID = demoModeManager.filterUUID ?: throw RuntimeException("Demo mode: missing UUID!")
            view?.post {
                if (FeatureFlags.F_NAVIGATION) {
                    findNavController().navigate(
                        R.id.nav_to_poi_screen,
                        POIFragment.newInstanceArgs(poiAuthority, poiUUID),
                    )
                } else {
                    (activity as MainActivity).addFragmentToStack(
                        POIFragment.newInstance(poiAuthority, poiUUID),
                    )
                }
            }
        } else if (demoModeManager.isEnabledBrowseScreen()) {
            val type = demoModeManager.filterTypeId ?: throw RuntimeException("Demo mode: missing type!")
            view?.post {
                if (FeatureFlags.F_NAVIGATION) {
                    findNavController().navigate(
                        R.id.nav_to_type_screen,
                        AgencyTypeFragment.newInstanceArgs(type),
                    )
                } else {
                    (activity as MainActivity).addFragmentToStack(
                        AgencyTypeFragment.newInstance(type),
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        this.adapter.onPause()
    }

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        attachedViewModel?.onLocationSettingsResolution(resolution)
    }

    override fun onDeviceLocationChanged(newLocation: Location?) {
        attachedViewModel?.onDeviceLocationChanged(newLocation)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_home, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.nav_map -> {
                if (FeatureFlags.F_NAVIGATION) {
                    false // handled by navigation library
                } else {
                    (activity as? MainActivity)?.addFragmentToStack(
                        MapFragment.newInstance(),
                        this
                    )
                    true // handled
                }
            }

            else -> false // not handled
        }
    }

    override fun getABTitle(context: Context?) = context?.getString(R.string.app_name) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) =
        this.attachedViewModel?.nearbyLocationAddress?.value ?: context?.getString(commonsR.string.ellipsis) ?: super.getABSubtitle(context)

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.onDestroyView()
        binding?.swipeRefresh?.apply {
            onDestroyView()
            setOnRefreshListener(null)
        }
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        this.adapter.onDestroy()
    }
}