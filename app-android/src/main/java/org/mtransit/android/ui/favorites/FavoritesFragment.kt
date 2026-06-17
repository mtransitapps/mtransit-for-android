package org.mtransit.android.ui.favorites

import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.MainThread
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentFavoritesBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.provider.favorite.FavoritesUI.showAddFolderDialog
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.DeviceLocationListener
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.applyStatusBarsInsetsEdgeToEdge
import org.mtransit.android.ui.empty.EmptyLayoutUtils.updateEmptyLayout
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledUI
import org.mtransit.android.ui.main.NextMainViewModel
import org.mtransit.android.ui.news.NewsListDetailFragment
import org.mtransit.android.ui.setUpListEdgeToEdge
import org.mtransit.android.ui.view.DefaultPOIListFooterManager
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.ui.view.common.observeEvent
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : ABFragment(R.layout.fragment_favorites),
    DeviceLocationListener,
    ModuleDisabledAwareFragment,
    MenuProvider {

    companion object {
        private val LOG_TAG: String = FavoritesFragment::class.java.simpleName

        const val TRACKING_SCREEN_NAME = "Favorites"

        @JvmStatic
        fun newInstance(): FavoritesFragment {
            return FavoritesFragment()
        }

        @JvmStatic
        fun newInstanceArgs() = Bundle()
    }

    override fun getLogTag() = LOG_TAG

    override val screenName = TRACKING_SCREEN_NAME

    override val viewModel by viewModels<FavoritesViewModel>()
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
    lateinit var analyticsManager: IAnalyticsManager

    @Inject
    lateinit var demoModeManager: DemoModeManager

    @Inject
    lateinit var billingManager: IBillingManager

    private var binding: FragmentFavoritesBinding? = null

    private val poiListFooterManager by lazy {
        DefaultPOIListFooterManager(
            adManager = adManager,
            analyticsManager = analyticsManager,
            demoModeManager = demoModeManager,
            billingManager = billingManager,
            dataSourcesRepository = dataSourcesRepository,
            getFragment = { this },
            getShowLoading = { attachedViewModel?.favoritePOIs?.value == null },
            getHideText = {
                val favoritePOIs = attachedViewModel?.favoritePOIs?.value
                    ?: return@DefaultPOIListFooterManager false
                val minListItemToNotHide = context?.let { DefaultPOIListFooterManager.getMinListItemToNotHide(it) }
                    ?: return@DefaultPOIListFooterManager false
                favoritePOIs.size < minListItemToNotHide
            },
            canShowRewardedAd = { adManager.isRewardedAdAvailableToShow() },
        )
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
            this.serviceUpdateLoader,
            this.analyticsManager,
        ).apply {
            logTag = this@FavoritesFragment.logTag
            setShowFavorite(false) // all items in this screen are favorites
            setShowTypeSectionHeader(POIArrayAdapter.SECTION_TYPE_HEADER_ALL_NEARBY)
            setTimeChangedListener { this@FavoritesFragment.onTimeChanged() }
            setShowFooter(true)
            setFooterManager(poiListFooterManager)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.listAdapter.setFragment(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFavoritesBinding.bind(view).apply {
            applyStatusBarsInsetsEdgeToEdge() // not drawing behind status bar
            listLayout.list.apply {
                isVisible = listAdapter.isInitialized
                listAdapter.setListView(this)
                setUpListEdgeToEdge()
            }
            setupScreenToolbar(screenToolbarLayout)
        }
        listAdapter.onCreateView(viewLifecycleOwner)
        viewModel.oneAgency.observe(viewLifecycleOwner) { oneAgency ->
            updateEmptyLayout(pkg = oneAgency?.pkg)
        }
        viewModel.hasFavoritesAgencyDisabled.observe(viewLifecycleOwner) { hasFavoritesAgencyDisabled ->
            updateEmptyLayout(hasFavoritesAgencyDisabled = hasFavoritesAgencyDisabled)
        }
        viewModel.favoritePOIs.observe(viewLifecycleOwner) { favoritePOIS ->
            listAdapter.setPois(favoritePOIS)
            listAdapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
            updateFooter()
            updateEmptyLayout(empty = favoritePOIS.isNullOrEmpty())
            binding?.apply {
                when {
                    favoritePOIS == null -> { // LOADING
                        listLayout.isVisible = false
                        emptyLayout.isVisible = false
                        loadingLayout.isVisible = true
                    }

                    favoritePOIS.isEmpty() -> { // EMPTY
                        loadingLayout.isVisible = false
                        listLayout.isVisible = false
                        emptyLayout.isVisible = true
                    }

                    else -> { // LIST
                        loadingLayout.isVisible = false
                        emptyLayout.isVisible = false
                        listLayout.isVisible = true
                    }
                }
            }
        }
        viewModel.deviceLocation.observe(viewLifecycleOwner) { deviceLocation ->
            listAdapter.setLocation(deviceLocation)
        }
        DefaultPOIListFooterManager.observe(viewLifecycleOwner, billingManager, dataSourcesRepository) {
            updateFooter()
        }
        ModuleDisabledUI.onViewCreated(this)
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.scrollToTopEvent.observeEvent(viewLifecycleOwner) { scroll ->
                if (scroll) {
                    binding?.listLayout?.list?.setSelection(0)
                }
            }
        }
    }

    private fun updateFooter() {
        listAdapter.notifyDataSetChanged(false)
    }

    @MainThread
    private fun onTimeChanged() {
        (activity as? IAdScreenActivity)?.let { adManager.onTimeChanged(it) }
    }

    private fun updateEmptyLayout(
        hasFavoritesAgencyDisabled: Boolean = attachedViewModel?.hasFavoritesAgencyDisabled?.value == true,
        empty: Boolean = attachedViewModel?.favoritePOIs?.value.isNullOrEmpty(),
        pkg: String? = attachedViewModel?.oneAgency?.value?.pkg,
    ) = binding?.apply {
        emptyLayout.updateEmptyLayout(empty = hasFavoritesAgencyDisabled && empty, pkg = pkg, activity)
        if (!hasFavoritesAgencyDisabled) {
            emptyLayout.apply {
                emptyTitle.apply {
                    setText(R.string.no_favorites)
                    isVisible = true
                }
                emptyText.apply {
                    setText(R.string.no_favorites_details)
                    isVisible = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        listAdapter.onResume(this, viewModel.deviceLocation.value)
        (activity as? MTActivityWithLocation)?.let { onLocationSettingsResolution(it.lastLocationSettingsResolution) }
        (activity as? MTActivityWithLocation)?.let { onDeviceLocationChanged(it.lastLocation) }
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.setABTitle(getABTitle(context))
        }
    }

    override fun onPause() {
        super.onPause()
        listAdapter.onPause()
    }

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        attachedViewModel?.onLocationSettingsResolution(resolution)
    }

    override fun onDeviceLocationChanged(newLocation: Location?) {
        attachedViewModel?.onDeviceLocationChanged(newLocation)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_favorites, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_add_favorite_folder -> {
                activity?.let {
                    favoriteRepository.showAddFolderDialog(it)
                }
                true // handled
            }

            R.id.menu_show_news -> {
                (activity as? MainActivity)?.apply {
                    addFragmentToStack(NewsListDetailFragment.newInstance(color = null))
                }
                true // handled
            }

            else -> false // not handled
        }
    }

    override fun hasToolbar() = true

    override fun getABTitle(context: Context?) = context?.getString(R.string.favorites) ?: super.getABTitle(context)

    override fun onDestroyView() {
        super.onDestroyView()
        listAdapter.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        listAdapter.onDestroy()
    }
}
