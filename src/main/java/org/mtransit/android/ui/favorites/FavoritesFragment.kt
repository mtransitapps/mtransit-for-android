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
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.BuildConfig
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.StoreUtils
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
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.main.MainViewModel
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.util.BatteryOptimizationIssueUtils
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : ABFragment(R.layout.fragment_favorites), DeviceLocationListener, FavoriteUpdateListener, MenuProvider {

    companion object {
        private val LOG_TAG = FavoritesFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Favorites"

        private const val IN_APP_NOTIFICATION_MODULE_DISABLED = 1

        @JvmStatic
        fun newInstance(): FavoritesFragment {
            return FavoritesFragment()
        }

        @JvmStatic
        fun newInstanceArgs() = bundleOf(
        )
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<FavoritesViewModel>()
    private val attachedViewModel
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

    private var binding: FragmentFavoritesBinding? = null

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
            logTag = this@FavoritesFragment.logTag
            setShowFavorite(false) // all items in this screen are favorites
            setFavoriteUpdateListener(this@FavoritesFragment)
            setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_ALL_NEARBY)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.adapter.setActivity(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MenuHost).addMenuProvider(
            this, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
        binding = FragmentFavoritesBinding.bind(view).apply {
            listLayout.list.let { listView ->
                listView.isVisible = adapter.isInitialized
                adapter.setListView(listView)
            }
        }
        viewModel.favoritePOIs.observe(viewLifecycleOwner) { favoritePOIS ->
            adapter.setPois(favoritePOIS)
            adapter.updateDistanceNowAsync(viewModel.deviceLocation.value)
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
            adapter.setLocation(deviceLocation)
        }
        viewModel.hasDisabledModule.observe(viewLifecycleOwner) { hasDisabledModule ->
            if (hasDisabledModule == true) {
                showModuleDisabledToast()
            } else {
                hideModuleDisabledToast()
            }
        }
        if (FeatureFlags.F_NAVIGATION) {
            mainViewModel.scrollToTopEvent.observe(viewLifecycleOwner, EventObserver { scroll ->
                if (scroll) {
                    binding?.listLayout?.list?.setSelection(0)
                }
            })
        }
    }

    override fun onFavoriteUpdated() {
        attachedViewModel?.onFavoriteUpdated()
    }

    override fun onResume() {
        super.onResume()
        adapter.onResume(this, viewModel.deviceLocation.value)
        (activity as? MTActivityWithLocation)?.let { onLocationSettingsResolution(it.lastLocationSettingsResolution) }
        (activity as? MTActivityWithLocation)?.let { onDeviceLocationChanged(it.lastLocation) }
        if (FeatureFlags.F_NAVIGATION) {
            mainViewModel.setABTitle(getABTitle(context))
        }
    }

    override fun onPause() {
        super.onPause()
        adapter.onPause()
    }


    private fun showModuleDisabledToast() {
        val context = context ?: return
        val firstDisabledAgency = this.viewModel.moduleDisabled.value?.firstOrNull { !PackageManagerUtils.isAppEnabled(context, it.pkg) }
        val labelText =
            firstDisabledAgency?.let { agency ->
                context.getString(
                    R.string.module_disabled_in_app_notif_label_and_agency,
                    agency.getShortNameAndType(context)
                )
            } ?: context.getText(R.string.module_disabled_in_app_notif_label)
        showInAppNotification(
            firstDisabledAgency?.pkg?.hashCode() ?: IN_APP_NOTIFICATION_MODULE_DISABLED, // TODO dynamic IDs?
            activity,
            view,
            attachedViewModel?.getAdBannerHeightInPx(this) ?: 0,
            labelText,
            context.getText(R.string.module_disabled_in_app_notif_action),
        ) {
            attachedViewModel?.moduleDisabled?.value?.let { moduleDisabled ->
                if (moduleDisabled.isNotEmpty()) {
                    val firstDisabledPkg = moduleDisabled.first().pkg
                    activity?.let {
                        return@showInAppNotification if (BuildConfig.DEBUG && BatteryOptimizationIssueUtils.isSamsungDevice()) {
                            BatteryOptimizationIssueUtils.openDeviceCare(
                                it,
                                BatteryOptimizationIssueUtils.SAMSUNG_DEVICE_CARE_EXTRA_ACTIVITY_TYPE_APP_SLEEPING_DEEP
                            )
                        } else {
                            StoreUtils.viewAppPage(
                                it,
                                firstDisabledPkg,
                                it.getString(org.mtransit.android.commons.R.string.google_play)
                            )
                        }
                    }
                }
            }
            false // not handled
        }
    }

    private fun hideModuleDisabledToast() {
        hideAllInAppNotifications() // TODO dynamic IDs? hideInAppNotification(IN_APP_NOTIFICATION_MODULE_DISABLED)
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

            else -> false // not handled
        }
    }

    override fun getABTitle(context: Context?) = context?.getString(R.string.favorites) ?: super.getABTitle(context)

    override fun onDestroyView() {
        super.onDestroyView()
        hideModuleDisabledToast()
        adapter.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.onDestroy()
    }
}