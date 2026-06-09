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
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.billing.BillingUtils
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.commons.getQuantityText
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.data.POIListFooterManager
import org.mtransit.android.databinding.FragmentHomeBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.DeviceLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.applyStatusBarsInsetsEdgeToEdge
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.fragment.POIFragment
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
import org.mtransit.android.ui.nearby.NearbyFragment
import org.mtransit.android.ui.setUpListEdgeToEdge
import org.mtransit.android.ui.type.AgencyTypeFragment
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.ui.view.common.observeEvent
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject
import kotlin.random.Random
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class HomeFragment : ABFragment(R.layout.fragment_home),
    DeviceLocationListener,
    NewLocationAwareFragment,
    LocationSettingsAwareFragment,
    LocationPermissionAwareFragment,
    ModuleDisabledAwareFragment,
    MenuProvider {

    companion object {
        private val LOG_TAG: String = HomeFragment::class.java.simpleName

        const val TRACKING_SCREEN_NAME = "Home"

        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun getLogTag() = LOG_TAG

    override val screenName = TRACKING_SCREEN_NAME

    override val viewModel by viewModels<HomeViewModel>()
    override val attachedViewModel
        get() = if (isAttached()) viewModel else null
    private val nextMainViewModel by activityViewModels<NextMainViewModel>()

    override fun getContextView(): View? = this.binding?.contextView ?: this.view

    @Inject
    lateinit var sensorManager: MTSensorManager

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var defaultPrefRepository: DefaultPreferenceRepository

    @Inject
    lateinit var lclPrefRepository: LocalPreferenceRepository

    @Inject
    lateinit var poiRepository: POIRepository

    @Inject
    lateinit var favoriteRepository: FavoriteRepository

    @Inject
    lateinit var statusLoader: StatusLoader

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    @Inject
    lateinit var adManager: IAdManager

    @Inject
    lateinit var demoModeManager: DemoModeManager

    @Inject
    lateinit var billingManager: IBillingManager

    private var mapMenuItem: MenuItem? = null

    private var binding: FragmentHomeBinding? = null

    private val poiListFooterManager by lazy {
        object : POIListFooterManager {

            private val SHOW_SUPPORT_INSTEAD_OF_REWARDED_AD_PCT = 50 // 50% support | 50% rewarded

            @Volatile
            private var _showSupportInsteadOfRewardedAd: Boolean? = null

            private var showSupportInsteadOfRewardedAd: Boolean
                get() {
                    if (_showSupportInsteadOfRewardedAd == null) {
                        _showSupportInsteadOfRewardedAd = Random.nextInt(1, 100) > SHOW_SUPPORT_INSTEAD_OF_REWARDED_AD_PCT
                    }
                    return _showSupportInsteadOfRewardedAd ?: false
                }
                set(value) {
                    _showSupportInsteadOfRewardedAd = value
                }

            private fun canShowRewardedAd() = adManager.isRewardedAdAvailableToShow()

            override val isShowLoading get() = attachedViewModel?.loadingPOIs?.value == true

            override val isShowText
                get() =
                    dataSourcesRepository.hasAgenciesEnabled()
                            && billingManager.hasSubscription.value != true
                            && !demoModeManager.isFullDemo()

            override val text: CharSequence?
                get() =
                    if (!isShowText) {
                        null
                    } else if (canShowRewardedAd() && !showSupportInsteadOfRewardedAd) {
                        MTLog.d(this@HomeFragment, "adManager.rewardedAdAmountInDays: ${adManager.rewardedAdAmountInDays}")
                        resources.getQuantityText(
                            if (adManager.isRewardedNow()) R.plurals.watch_rewarded_ad_btn_more_and_days_formatted
                            else R.plurals.watch_rewarded_ad_btn_and_days_formatted,
                            adManager.rewardedAdAmountInDays,
                            adManager.rewardedAdAmountInDays
                        )
                    } else {
                        showSupportInsteadOfRewardedAd = true
                        context?.getString(R.string.support)
                    }

            override val textStartDrawableRes: Int?
                get() = if (!isShowText) {
                    null
                } else if (canShowRewardedAd() && !showSupportInsteadOfRewardedAd) {
                    R.drawable.ic_on_demand_video_black_24
                } else {
                    showSupportInsteadOfRewardedAd = true
                    R.drawable.ic_volunteer_activism_black_24
                }

            override val onTextClickListener = View.OnClickListener {
                if (!isShowText) {
                    return@OnClickListener
                } else if (!showSupportInsteadOfRewardedAd) { // rewarded ad
                    if (!adManager.isRewardedAdAvailableToShow()) {
                        MTLog.w(this@HomeFragment, "footer.onTextClick() > skip (no ad available)")
                        ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_not_ready)
                        return@OnClickListener
                    }
                    (activity as? IAdScreenActivity)?.let { adManager.showRewardedAd(it) }
                        ?: run {
                            MTLog.w(this@HomeFragment, "onRewardedAdButtonClick() > skip (no view or no activity)")
                            ToastUtils.makeTextAndShow(context, R.string.support_watch_rewarded_ad_default_failure_message)
                            return@OnClickListener
                        }
                } else { // support
                    activity?.let { BillingUtils.showPurchaseDialog(it) }
                }
            }
        }
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

    private val listAdapter: POIArrayAdapter by lazy {
        POIArrayAdapter(
            this,
            this.sensorManager,
            this.dataSourcesRepository,
            this.defaultPrefRepository,
            this.lclPrefRepository,
            this.poiRepository,
            this.favoriteRepository,
            this.statusLoader,
            this.serviceUpdateLoader
        ).apply {
            logTag = this@HomeFragment.logTag
            setShowBrowseHeaderSection(true)
            setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE)
            setShowTypeHeaderNearby(true)
            setShowFooter(true)
            setFooterManager(poiListFooterManager)
            setOnTypeHeaderButtonsClickListener(typeHeaderButtonsClickListener)
            setPois(attachedViewModel?.nearbyPOIs?.value)
            setLocation(attachedViewModel?.deviceLocation?.value)
            setTimeChangedListener { this@HomeFragment.onTimeChanged() }
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
        binding = FragmentHomeBinding.bind(view).apply {
            applyStatusBarsInsetsEdgeToEdge() // not drawing behind status bar
            listLayout.list.apply {
                swipeRefresh.setListViewWR(this)
                isVisible = listAdapter.isInitialized
                listAdapter.setListView(this)
                setUpListEdgeToEdge()
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
            setupScreenToolbar(screenToolbarLayout)
        }
        listAdapter.onCreateView(viewLifecycleOwner)
        billingManager.hasSubscription.observe(viewLifecycleOwner) {
            listAdapter.notifyDataSetChanged(false)
        }
        dataSourcesRepository.readingHasAgenciesEnabled().observe(viewLifecycleOwner) {
            listAdapter.notifyDataSetChanged(false)
        }
        viewModel.deviceLocation.observe(viewLifecycleOwner) {
            listAdapter.setLocation(it)
        }
        viewModel.nearbyLocationAddress.observe(viewLifecycleOwner) {
            binding?.apply { updateScreenToolbarSubtitle(screenToolbarLayout.screenToolbar) }
            abController?.setABSubtitle(this, getABSubtitle(context), true)
            if (FeatureFlags.F_NAVIGATION) {
                nextMainViewModel.setABSubtitle(getABSubtitle(context))
            }
        }
        viewModel.nearbyPOIsTriggerListener.observe(viewLifecycleOwner) {
            // DO NOTHING
        }
        viewModel.nearbyPOIsTrigger.observeEvent(viewLifecycleOwner) { triggered ->
            if (triggered) {
                listAdapter.clear()
            }
        }
        viewModel.sortedTypeToHomeAgencies.observe(viewLifecycleOwner) {
            listAdapter.initPOITypes(it ?: return@observe)
        }
        viewModel.nearbyPOIs.observe(viewLifecycleOwner) { nearbyPOIs ->
            val scrollToTop = listAdapter.poisCount <= 0
            listAdapter.appendPois(nearbyPOIs)
            if (scrollToTop) {
                binding?.listLayout?.list?.setSelection(0)
            }
            if (isResumed) {
                listAdapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
            } else {
                listAdapter.onPause()
            }
            switchView()
        }
        viewModel.loadingPOIs.observe(viewLifecycleOwner) { loading ->
            if (loading == false) {
                binding?.swipeRefresh?.isRefreshing = false
            } // else do nothing
            listAdapter.notifyDataSetChanged(false) // footer
        }
        viewModel.hasAgenciesAdded.observe(viewLifecycleOwner) {
            updateMenuItemsVisibility(hasAgenciesAdded = it)
        }
        ModuleDisabledUI.onViewCreated(this)
        LocationSettingsUI.onViewCreated(this)
        LocationPermissionUI.onViewCreated(this)
        NewLocationUI.onViewCreated(this)
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.scrollToTopEvent.observeEvent(viewLifecycleOwner) { scroll ->
                if (scroll) {
                    binding?.listLayout?.list?.setSelection(0)
                }
            }
        }
    }

    private fun onTimeChanged() {
        (activity as? IAdScreenActivity)?.let { adManager.onTimeChanged(it) }
    }

    private fun switchView() = binding?.apply {
        loadingLayout.isVisible = false
        emptyLayout.isVisible = false

        listLayout.isVisible = true // list layout header w/ buttons always shown
    }

    override fun onResume() {
        super.onResume()
        this.listAdapter.onResume(this, viewModel.deviceLocation.value)
        switchView()
        (activity as? MTActivityWithLocation)?.let { onLocationSettingsResolution(it.lastLocationSettingsResolution) }
        (activity as? MTActivityWithLocation)?.checkLocationSettings()
        (activity as? MTActivityWithLocation)?.let { onDeviceLocationChanged(it.lastLocation) }
        viewModel.checkIfNetworkLocationRefreshNecessary()
        viewModel.refreshLocationPermissionNeeded()
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.setABTitle(getABTitle(context))
            nextMainViewModel.setABSubtitle(getABSubtitle(context))
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
            val dstId = demoModeManager.filterTypeId ?: throw RuntimeException("Demo mode: missing type!")
            view?.post {
                if (FeatureFlags.F_NAVIGATION) {
                    findNavController().navigate(
                        R.id.nav_to_type_screen,
                        AgencyTypeFragment.newInstanceArgs(dstId),
                    )
                } else {
                    (activity as MainActivity).addFragmentToStack(
                        AgencyTypeFragment.newInstance(dstId),
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        this.listAdapter.onPause()
    }

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        attachedViewModel?.onLocationSettingsResolution(resolution)
    }

    override fun onDeviceLocationChanged(newLocation: Location?) {
        attachedViewModel?.onDeviceLocationChanged(newLocation)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_home, menu)
        this.mapMenuItem = menu.findItem(R.id.nav_map)
        updateMenuItemsVisibility()
    }

    private fun updateMenuItemsVisibility(
        hasAgenciesAdded: Boolean? = viewModel.hasAgenciesAdded.value,
    ) {
        mapMenuItem?.isVisible = hasAgenciesAdded == true
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

    override fun hasToolbar() = true

    override fun getABTitle(context: Context?) =
        if (attachedViewModel?.isFullDemo() == true) "MonTransit"
        else context?.getString(R.string.app_name) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) =
        this.attachedViewModel?.nearbyLocationAddress?.value
            ?: context?.getString(commonsR.string.ellipsis)
            ?: super.getABSubtitle(context)

    override fun onDestroyView() {
        super.onDestroyView()
        listAdapter.onDestroyView()
        binding?.swipeRefresh?.apply {
            onDestroyView()
            setOnRefreshListener(null)
        }
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        this.listAdapter.onDestroy()
    }
}