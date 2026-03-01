@file:JvmName("FavoritesFragment") // ANALYTICS
package org.mtransit.android.ui.favorites

import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.databinding.FragmentFavoritesBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.FavoriteManager.FavoriteUpdateListener
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
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : ABFragment(R.layout.fragment_favorites),
    DeviceLocationListener,
    FavoriteUpdateListener,
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
        fun newInstanceArgs() = bundleOf(
        )
    }

    override fun getLogTag() = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

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
    lateinit var localPreferenceRepository: LocalPreferenceRepository

    @Inject
    lateinit var poiRepository: POIRepository

    @Inject
    lateinit var favoriteManager: FavoriteManager

    @Inject
    lateinit var statusLoader: StatusLoader

    @Inject
    lateinit var serviceUpdateLoader: ServiceUpdateLoader

    private var binding: FragmentFavoritesBinding? = null

    private val listAdapter: POIArrayAdapter by lazy {
        POIArrayAdapter(
            this,
            this.sensorManager,
            this.dataSourcesRepository,
            this.defaultPrefRepository,
            this.localPreferenceRepository,
            this.poiRepository,
            this.favoriteManager,
            this.statusLoader,
            this.serviceUpdateLoader
        ).apply {
            logTag = this@FavoritesFragment.logTag
            setShowFavorite(false) // all items in this screen are favorites
            setFavoriteUpdateListener(this@FavoritesFragment)
            setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_ALL_NEARBY)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.listAdapter.setActivity(this)
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
        viewModel.oneAgency.observe(viewLifecycleOwner) { oneAgency ->
            updateEmptyLayout(pkg = oneAgency?.pkg)
        }
        viewModel.hasFavoritesAgencyDisabled.observe(viewLifecycleOwner) { hasFavoritesAgencyDisabled ->
            updateEmptyLayout(hasFavoritesAgencyDisabled = hasFavoritesAgencyDisabled)
        }
        viewModel.favoritePOIs.observe(viewLifecycleOwner) { favoritePOIS ->
            listAdapter.setPois(favoritePOIS)
            listAdapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
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
        ModuleDisabledUI.onViewCreated(this)
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.scrollToTopEvent.observe(viewLifecycleOwner, EventObserver { scroll ->
                if (scroll) {
                    binding?.listLayout?.list?.setSelection(0)
                }
            })
        }
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

    override fun onFavoriteUpdated() {
        attachedViewModel?.onFavoriteUpdated()
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
                this.favoriteManager.showAddFolderDialog(requireActivity(), this, null, null)
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