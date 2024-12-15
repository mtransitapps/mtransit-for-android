@file:JvmName("MapFragment") // ANALYTICS
package org.mtransit.android.ui.map

import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.FragmentMapBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.provider.permission.LocationPermissionProvider
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTActivityWithLocation.DeviceLocationListener
import org.mtransit.android.ui.MTDialog
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.inappnotification.locationsettings.LocationSettingsAwareFragment
import org.mtransit.android.ui.inappnotification.locationsettings.LocationSettingsUI
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledUI
import org.mtransit.android.ui.setStatusBarHeight
import org.mtransit.android.ui.setUpEdgeToEdgeTopMap
import org.mtransit.android.ui.view.MapViewController
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.util.UIFeatureFlags
import javax.inject.Inject


@AndroidEntryPoint
class MapFragment : ABFragment(R.layout.fragment_map),
    DeviceLocationListener,
    LocationSettingsAwareFragment,
    ModuleDisabledAwareFragment,
    MenuProvider {

    companion object {
        private val LOG_TAG = MapFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "Map"

        @JvmStatic
        fun newInstance(poim: POIManager) = newInstance(
            LocationUtils.getNewLocation(poim.lat, poim.lng),
            poim.poi.uuid,
            poim.poi.dataSourceTypeId
        )

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            optInitialLocation: Location? = null,
            optSelectedUUID: String? = null,
            optIncludeTypeId: Int? = null,
        ): MapFragment {
            return MapFragment().apply {
                arguments = newInstanceArgs(optInitialLocation, optSelectedUUID, optIncludeTypeId)
            }
        }

        @JvmStatic
        fun newInstanceArgs(poim: POIManager) = newInstanceArgs(
            LocationUtils.getNewLocation(poim.lat, poim.lng),
            poim.poi.uuid,
            poim.poi.dataSourceTypeId
        )

        @JvmStatic
        @JvmOverloads
        fun newInstanceArgs(
            optInitialLocation: Location? = null,
            optSelectedUUID: String? = null,
            optIncludeTypeId: Int? = null,
        ) = bundleOf(
            MapViewModel.EXTRA_INITIAL_LOCATION to optInitialLocation,
            MapViewModel.EXTRA_SELECTED_UUID to optSelectedUUID,
            MapViewModel.EXTRA_INCLUDE_TYPE_ID to (optIncludeTypeId ?: MapViewModel.EXTRA_INCLUDE_TYPE_ID_DEFAULT),
        )

        private const val TOP_PADDING_SP = 64
        private const val BOTTOM_PADDING_SP = 0
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    override val viewModel by viewModels<MapViewModel>()
    override val attachedViewModel
        get() = if (isAttached()) viewModel else null

    override fun getContextView(): View? = this.binding?.contextView ?: this.view

    override fun getAnchorView(): View? = this.binding?.root?.findViewById(R.id.map_type_switch)

    private var binding: FragmentMapBinding? = null

    @Inject
    lateinit var dataSourcesRepository: DataSourcesRepository

    @Inject
    lateinit var locationPermissionProvider: LocationPermissionProvider

    private val mapListener = object : MapViewController.MapListener {

        override fun onMapClick(position: LatLng) {
            // DO NOTHING
        }

        override fun onCameraChange(latLngBounds: LatLngBounds) {
            attachedViewModel?.onCameraChange(latLngBounds) {
                mapViewController.getBigCameraPosition(activity, 1.0f)
            }
        }

        override fun onMapReady() {
            attachedViewModel?.poiMarkers?.value?.let {
                mapViewController.clearMarkers()
                mapViewController.addMarkers(it)
                mapViewController.showMap(view)
            }
        }
    }

    private val mapViewController: MapViewController by lazy {
        MapViewController(
            logTag,
            null, // DO NOTHING (not linked with list adapter)
            mapListener,
            true,
            true,
            true,
            false,
            false,
            false,
            TOP_PADDING_SP,
            BOTTOM_PADDING_SP,
            false,
            true,
            true,
            false,
            true,
            this.dataSourcesRepository
        ).apply {
            setAutoClickInfoWindow(true)
            logTag = this@MapFragment.logTag
            setLocationPermissionGranted(locationPermissionProvider.allRequiredPermissionsGranted(requireContext()))
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mapViewController.apply {
            setDataSourcesRepository(dataSourcesRepository)
            onAttach(requireActivity())
            setLocationPermissionGranted(locationPermissionProvider.allRequiredPermissionsGranted(context))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapViewController.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MenuHost).addMenuProvider(
            this, viewLifecycleOwner, Lifecycle.State.RESUMED
        )
        this.mapViewController.onViewCreated(view, savedInstanceState)
        binding = FragmentMapBinding.bind(view).apply {
            // if (UIFeatureFlags.F_EDGE_TO_EDGE) {
            this.map.setUpEdgeToEdgeTopMap(mapViewController, TOP_PADDING_SP, BOTTOM_PADDING_SP)
            if (UIFeatureFlags.F_EDGE_TO_EDGE_TRANSLUCENT_TOP) {
                fragmentStatusBarBg.setStatusBarHeight(context.resources.getDimensionPixelSize(R.dimen.action_bar_size_static))
            }
        }
        viewModel.initialLocation.observe(viewLifecycleOwner) { location ->
            location?.let {
                mapViewController.setInitialLocation(it)
                viewModel.onInitialLocationSet()
            }
        }
        viewModel.selectedUUID.observe(viewLifecycleOwner) {
            it?.let {
                mapViewController.setInitialSelectedUUID(it)
                viewModel.onSelectedUUIDSet()
            }
        }
        viewModel.deviceLocation.observe(viewLifecycleOwner) {
            context?.let { context ->
                mapViewController.setLocationPermissionGranted(locationPermissionProvider.allRequiredPermissionsGranted(context))
            }
            mapViewController.onDeviceLocationChanged(it)
        }
        LocationSettingsUI.onViewCreated(this)
        ModuleDisabledUI.onViewCreated(this)
        viewModel.filterTypeIds.observe(viewLifecycleOwner) {
            abController?.setABTitle(this, getABTitle(context), true)
        }
        viewModel.typeMapAgencies.observe(viewLifecycleOwner) {
            viewModel.resetLoadedPOIMarkers()
        }
        viewModel.poiMarkersTrigger.observe(viewLifecycleOwner) {
            // DO NOTHING
        }
        viewModel.loaded.observe(viewLifecycleOwner) {
            if (it == false) {
                mapViewController.showLoading()
            } else if (it == true) {
                mapViewController.hideLoading()
            }
        }
        viewModel.poiMarkers.observe(viewLifecycleOwner) {
            mapViewController.clearMarkers() // new types -> RESET
            it?.let { poiMarkers ->
                mapViewController.addMarkers(poiMarkers)
                mapViewController.showMap(view)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapViewController.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDetach() {
        super.onDetach()
        this.mapViewController.onDetach()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mapViewController.onConfigurationChanged(newConfig)
    }

    override fun onResume() {
        super.onResume()
        mapViewController.onResume()
        mapViewController.showMap(view)
        (activity as? MTActivityWithLocation)?.let { onLocationSettingsResolution(it.lastLocationSettingsResolution) }
        (activity as? MTActivityWithLocation)?.let { onDeviceLocationChanged(it.lastLocation) }
    }

    override fun onLocationSettingsResolution(resolution: PendingIntent?) {
        attachedViewModel?.onLocationSettingsResolution(resolution)
    }

    override fun onDeviceLocationChanged(newLocation: Location?) {
        attachedViewModel?.onDeviceLocationChanged(newLocation)
    }

    override fun onPause() {
        super.onPause()
        mapViewController.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapViewController.onLowMemory()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_map, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return if (menuItem.itemId == R.id.menu_filter) {
            showMenuFilterDialog()
        } else false // not handled
    }

    private fun showMenuFilterDialog(): Boolean {
        val filterTypeIds = attachedViewModel?.filterTypeIds?.value ?: return false
        val activity = requireActivity()
        val typeNames = mutableListOf<CharSequence>()
        val checked = mutableListOf<Boolean>()
        val typeIds = mutableListOf<Int>()
        val selectedItems = mutableSetOf<Int>()
        attachedViewModel?.mapTypes?.value?.forEach { type ->
            typeIds.add(type.id)
            typeNames.add(type.getPoiShortName(activity))
            checked.add(filterTypeIds.isEmpty() || filterTypeIds.contains(type.id))
        }
        val checkedItems = BooleanArray(checked.size)
        for (c in checked.indices) {
            checkedItems[c] = checked[c]
            if (checkedItems[c]) {
                selectedItems.add(c)
            }
        }
        MTDialog.Builder(activity)
            .setTitle(R.string.menu_action_filter)
            .setMultiChoiceItems(typeNames.toTypedArray(), checkedItems) { _, which, isChecked ->
                selectedItems.apply { if (isChecked) add(which) else remove(which) }
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                applyNewFilter(typeIds, selectedItems)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .show()
        return true // handled
    }

    private fun applyNewFilter(availableTypeIds: List<Int>, selectedTypesIdx: Set<Int>) {
        viewModel.saveFilterTypeIdsPref(
            if (selectedTypesIdx.isEmpty() || selectedTypesIdx.size == availableTypeIds.size) { // ALL SELECTED
                null
            } else { // NOT ALL SELECTED
                availableTypeIds.filterIndexed { idx, _ ->
                    selectedTypesIdx.contains(idx)
                }
            }
        )
    }

    override fun isABStatusBarTransparent() = true

    override fun isABOverrideGradient() = UIFeatureFlags.F_EDGE_TO_EDGE_TRANSLUCENT_TOP

    override fun getABTitle(context: Context?): CharSequence? {
        return context?.let { makeABTitle(it) } ?: super.getABTitle(null)
    }

    private fun makeABTitle(context: Context): CharSequence {
        return (attachedViewModel?.filterTypeIds?.value?.let { it.ifEmpty { null } } // empty = all
            ?.mapNotNull { typeId ->
                DataSourceType.parseId(typeId)?.shortNamesResId?.let { context.getString(it) }
            } ?: listOf(context.getString(R.string.all)))
            .joinToString(prefix = "${context.getString(R.string.map)} (", separator = ", ", postfix = ")")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapViewController.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mapViewController.onDestroy()
    }
}