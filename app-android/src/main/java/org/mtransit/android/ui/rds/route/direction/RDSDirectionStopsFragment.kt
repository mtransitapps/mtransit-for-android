@file:JvmName("RTSTripStopsFragment") // ANALYTICS // do not change to avoid breaking tracking
package org.mtransit.android.ui.rds.route.direction

import android.content.Context
import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.distinctByOriginalId
import org.mtransit.android.commons.data.isSeverityWarningInfo
import org.mtransit.android.commons.findClosestPOISIdxUuid
import org.mtransit.android.commons.updateDistance
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIArrayAdapter
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.RouteDirectionManager
import org.mtransit.android.databinding.FragmentRdsDirectionStopsBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteManager
import org.mtransit.android.provider.permission.LocationPermissionProvider
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.common.twoPane
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.rds.route.RDSRouteFragment
import org.mtransit.android.ui.rds.route.RDSRouteViewModel
import org.mtransit.android.ui.serviceupdates.ServiceUpdatesDialog
import org.mtransit.android.ui.setNavBarProtectionEdgeToEdge
import org.mtransit.android.ui.setUpFabEdgeToEdge
import org.mtransit.android.ui.setUpListEdgeToEdge
import org.mtransit.android.ui.setUpMapEdgeToEdge
import org.mtransit.android.ui.view.MapViewController
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.isVisible
import org.mtransit.android.ui.view.map.MTPOIMarker
import org.mtransit.android.ui.view.updateVehicleLocationMarkers
import org.mtransit.android.ui.view.updateVehicleLocationMarkersCountdown
import org.mtransit.android.util.FragmentUtils
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class RDSDirectionStopsFragment : MTFragmentX(R.layout.fragment_rds_direction_stops) {

    companion object {
        private val LOG_TAG = RDSDirectionStopsFragment::class.java.simpleName

        // internal const val SHOW_SERVICE_UPDATE_FAB = false
        internal const val SHOW_SERVICE_UPDATE_FAB = true // ON to filter alerts in stop feed to avoid duplicates warnings signs

        @JvmStatic
        fun newInstance(
            routeDirectionStop: RouteDirectionStop,
            optMapCameraPosition: CameraPosition? = null,
        ) = newInstance(routeDirectionStop.direction, routeDirectionStop.stop.id, optMapCameraPosition)

        @JvmStatic
        fun newInstance(
            direction: Direction,
            optSelectedStopId: Int? = null,
            optMapCameraPosition: CameraPosition? = null,
        ) = newInstance(
            direction.authority,
            direction.routeId,
            direction.id,
            optSelectedStopId,
            optMapCameraPosition?.target?.latitude,
            optMapCameraPosition?.target?.longitude,
            optMapCameraPosition?.zoom,
        )

        @JvmStatic
        fun newInstance(
            agencyAuthority: String,
            routeId: Long,
            directionId: Long,
            optSelectedStopId: Int? = null,
            optMapLat: Double? = null,
            optMapLng: Double? = null,
            optMapZoom: Float? = null,
        ) = RDSDirectionStopsFragment().apply {
            arguments = bundleOf(
                RDSDirectionStopsViewModel.EXTRA_AGENCY_AUTHORITY to agencyAuthority,
                RDSDirectionStopsViewModel.EXTRA_ROUTE_ID to routeId,
                RDSDirectionStopsViewModel.EXTRA_DIRECTION_ID to directionId,
                RDSDirectionStopsViewModel.EXTRA_SELECTED_STOP_ID to (optSelectedStopId ?: RDSDirectionStopsViewModel.EXTRA_SELECTED_STOP_ID_DEFAULT),
                RDSDirectionStopsViewModel.EXTRA_SELECTED_MAP_CAMERA_POSITION_LAT to optMapLat,
                RDSDirectionStopsViewModel.EXTRA_SELECTED_MAP_CAMERA_POSITION_LNG to optMapLng,
                RDSDirectionStopsViewModel.EXTRA_SELECTED_MAP_CAMERA_POSITION_ZOOM to optMapZoom,
            )
        }

        private const val TOP_PADDING_SP = 0
        private const val BOTTOM_PADDING_SP = 56
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private val viewModel by viewModels<RDSDirectionStopsViewModel>()
    private val attachedViewModel get() = if (isAttached()) viewModel else null

    private val parentViewModel by viewModels<RDSRouteViewModel>({ requireParentFragment() })
    private val attachedParentViewModel get() = if (isAttached()) parentViewModel else null

    private var binding: FragmentRdsDirectionStopsBinding? = null

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

    @Inject
    lateinit var locationPermissionProvider: LocationPermissionProvider

    private val mapMarkerProvider = object : MapViewController.MapMarkerProvider {

        override fun getPOMarkers(): Collection<MTPOIMarker>? = null

        override fun getPOIs(): Collection<POIManager>? {
            if (!listAdapter.isInitialized) return null
            return buildList {
                for (i in 0 until listAdapter.poisCount) {
                    listAdapter.getItem(i)?.let { add(it) }
                }
            }
        }

        override fun getPOI(position: Int) = listAdapter.getItem(position)

        override fun getClosestPOI() = listAdapter.closestPOI

        override fun getPOI(uuid: String?) = listAdapter.getItem(uuid)

        override fun getVehicleLocations() = attachedViewModel?.vehicleLocations?.value

        @ColorInt
        override fun getVehicleColorInt(): Int? = attachedParentViewModel?.colorInt?.value

        override fun getVehicleType(): DataSourceType? = attachedParentViewModel?.routeType?.value

        override fun getVisibleMarkersLocations(): Collection<LatLng>? = null

        override fun getMapMarkerAlpha(position: Int, visibleArea: Area): Float? = null
    }

    private val mapViewController: MapViewController by lazy {
        MapViewController(
            logTag,
            mapMarkerProvider,
            null, // DO NOTHING (map click, camera change)
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
            false,
            true,
            false,
        ).apply {
            setAutoClickInfoWindow(true)
            setLocationPermissionGranted(locationPermissionProvider.allRequiredPermissionsGranted(requireContext()))
        }
    }

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
            logTag = this@RDSDirectionStopsFragment.logTag
            setShowExtra(false) // show route short name & direction
            setLocation(attachedParentViewModel?.deviceLocation?.value)
            if (SHOW_SERVICE_UPDATE_FAB) {
                setIgnoredTargetUUIDs(attachedViewModel?.routeDirectionM?.value?.routeDirection?.allUUIDs)
            } else if (RDSRouteFragment.SHOW_SERVICE_UPDATE_IN_TOOLBAR) {
                setIgnoredTargetUUIDs(attachedParentViewModel?.routeM?.value?.route?.allUUIDs)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.mapViewController.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.mapViewController.onViewCreated(view, savedInstanceState)
        binding = FragmentRdsDirectionStopsBinding.bind(view).apply {
            listLayout.list.apply {
                isVisible = listAdapter.isInitialized
                listAdapter.setListView(this)
                setUpListEdgeToEdge()
            }
            fabListMap?.apply {
                setOnClickListener {
                    if (context.twoPane) return@setOnClickListener // LARGE SCREEN
                    viewModel.saveShowingListInsteadOfMap(viewModel.showingListInsteadOfMap.value == false) // switching
                }
                setUpFabEdgeToEdge(
                    originalMarginEndDimenRes = R.dimen.fab_mini_margin_end,
                    originalMarginBottomDimenRes = R.dimen.fab_mini_margin_bottom,
                )
            }
            fabServiceUpdate.apply {
                setOnClickListener {
                    val routeM = attachedParentViewModel?.routeM?.value ?: return@setOnClickListener
                    val directionId = attachedViewModel?.directionId?.value ?: return@setOnClickListener
                    if (FeatureFlags.F_NAVIGATION) {
                        // TODO navigate to dialog
                    } else {
                        FragmentUtils.replaceDialogFragment(
                            activity ?: return@setOnClickListener,
                            FragmentUtils.DIALOG_TAG,
                            ServiceUpdatesDialog.newInstance(routeM.authority, routeM.route.id, directionId),
                            null
                        )
                    }
                }
                setUpFabEdgeToEdge(
                    originalMarginEndDimenRes = R.dimen.fab_mini_margin_end_above_fab,
                    originalMarginBottomDimenRes = R.dimen.fab_mini_margin_bottom,
                )
                updateServiceUpdateImg()
            }
            map.setUpMapEdgeToEdge(mapViewController, TOP_PADDING_SP, BOTTOM_PADDING_SP)
        }
        viewModel.routeDirectionM.observe(viewLifecycleOwner) { routeDirectionM ->
            updateServiceUpdateImg(routeDirectionM)
            if (viewModel.mapVisible(context)) {
                applySelectedIdChanged(routeDirectionM = routeDirectionM)
            }
            if (SHOW_SERVICE_UPDATE_FAB) {
                if (listAdapter.setIgnoredTargetUUIDs(routeDirectionM.routeDirection.allUUIDs)) {
                    listAdapter.notifyDataSetChanged(true)
                }
            }
        }
        viewModel.serviceUpdateLoadedEvent.observe(viewLifecycleOwner, EventObserver { _ ->
            updateServiceUpdateImg()
        })
        parentViewModel.routeM.observe(viewLifecycleOwner) {
            if (SHOW_SERVICE_UPDATE_FAB) {
                // elsewhere
            } else if (RDSRouteFragment.SHOW_SERVICE_UPDATE_IN_TOOLBAR) {
                if (listAdapter.setIgnoredTargetUUIDs(it.route.allUUIDs)) {
                    listAdapter.notifyDataSetChanged(true)
                }
            }
        }
        parentViewModel.colorInt.observe(viewLifecycleOwner) { colorInt ->
            binding?.apply {
                colorInt?.let { colorInt ->
                    fabListMap?.apply {
                        rippleColor = colorInt
                        backgroundTintList = ColorStateList.valueOf(colorInt)
                    }
                    fabServiceUpdate.apply {
                        rippleColor = colorInt
                        backgroundTintList = ColorStateList.valueOf(colorInt)
                    }
                }
            }
        }
        viewModel.directionId.observe(viewLifecycleOwner) { directionId ->
            theLogTag = directionId?.let { "${LOG_TAG}-$it" } ?: LOG_TAG
            listAdapter.logTag = this@RDSDirectionStopsFragment.logTag
            mapViewController.logTag = this@RDSDirectionStopsFragment.logTag
        }
        parentViewModel.deviceLocation.observe(viewLifecycleOwner) { deviceLocation ->
            mapViewController.onDeviceLocationChanged(deviceLocation)
            listAdapter.setLocation(deviceLocation)
        }
        viewModel.showingListInsteadOfMap.observe(viewLifecycleOwner) { showingListInsteadOfMap ->
            updateFabListMapUI(showingListInsteadOfMap)
            if (viewModel.mapVisible(context)) {
                applySelectedIdChanged()
                mapViewController.onResume()
                viewModel.startVehicleLocationRefresh()
            } else { // LIST
                mapViewController.onPause()
                viewModel.stopVehicleLocationRefresh()
                stopVehicleLocationCountdownRefresh()
            }
            switchView(showingListInsteadOfMap)
        }
        viewModel.selectedMapCameraPosition.observe(viewLifecycleOwner) { selectedMapCameraPosition ->
            selectedMapCameraPosition?.let { cameraPosition ->
                mapViewController.setShowAllMarkersWhenReady(false)
                mapViewController.setInitialCameraPosition(cameraPosition)
                viewModel.onSelectedMapCameraPositionSet()
            }
        }
        viewModel.selectedStopId.observe(viewLifecycleOwner) { selectedStopId ->
            if (viewModel.mapVisible(context)) {
                applySelectedIdChanged(selectedStopId)
            }
        }
        viewModel.closestPOIShown.observe(viewLifecycleOwner) {
            // DO NOTHING
        }
        if (UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) {
            viewModel.vehicleLocationsDistinct.observe(viewLifecycleOwner) { vehicleLocations ->
                context?.let { mapViewController.updateVehicleLocationMarkers(it, vehicleLocations = vehicleLocations) }
                if (vehicleLocations.isNullOrEmpty()) {
                    stopVehicleLocationCountdownRefresh()
                } else {
                    startVehicleLocationCountdownRefresh()
                }
            }
            parentViewModel.colorInt.observe(viewLifecycleOwner) {
                // do nothing
            }
            parentViewModel.routeType.observe(viewLifecycleOwner) {
                // do nothing
            }
        }
        viewModel.poiList.observe(viewLifecycleOwner) { poiList ->
            var currentSelectedItemIndexUuid: Pair<Int?, String?>? = null
            val selectedStopId = viewModel.selectedStopId.value
            val closestPOIShow = viewModel.closestPOIShown.value
            if (selectedStopId != null || closestPOIShow != true) {
                if (selectedStopId != null) {
                    currentSelectedItemIndexUuid = findStopIndexUuid(selectedStopId, poiList) // can be a stop for other direction in route fragment
                }
                if (currentSelectedItemIndexUuid == null) {
                    if (closestPOIShow == false) {
                        currentSelectedItemIndexUuid = findClosestPOIIndexUuid(poiList)
                    }
                }
                viewModel.setSelectedOrClosestStopShown()
            }
            listAdapter.setPois(poiList)
            listAdapter.updateDistanceNowAsync(parentViewModel.deviceLocation.value)
            mapViewController.notifyMarkerChanged(mapMarkerProvider)
            if (viewModel.listVisible(context)) {
                val selectedPosition = currentSelectedItemIndexUuid?.first ?: -1
                if (selectedPosition > 0) {
                    binding?.listLayout?.list?.setSelection(selectedPosition - 1) // show 1 more stop on top of the list
                }
            }
            switchView()
        }
    }

    private fun applySelectedIdChanged(
        selectedStopId: Int? = viewModel.selectedStopId.value,
        routeDirectionM: RouteDirectionManager? = viewModel.routeDirectionM.value,
    ) {
        selectedStopId ?: return
        routeDirectionM ?: return
        val rdsUUID = RouteDirectionStop.makeUUID(
            routeDirectionM.authority,
            routeDirectionM.routeDirection.route.id,
            routeDirectionM.routeDirection.direction.id,
            selectedStopId
        )
        mapViewController.setInitialSelectedUUID(rdsUUID)
        viewModel.onSelectedStopIdSet()
    }

    private fun updateServiceUpdateImg(
        routeDirectionM: RouteDirectionManager? = attachedViewModel?.routeDirectionM?.value,
        fabServiceUpdate: FloatingActionButton? = binding?.fabServiceUpdate,
    ) {
        fabServiceUpdate?.apply {
            routeDirectionM ?: run { isVisible = false; return }
            val serviceUpdates = routeDirectionM.getServiceUpdates(
                serviceUpdateLoader = serviceUpdateLoader,
                ignoredUUIDsOrUnknown = routeDirectionM.routeDirection.route.allUUIDs
            ).distinctByOriginalId()
            val (isWarning, isInfo) = serviceUpdates.isSeverityWarningInfo()
            if (isWarning) {
                setImageResource(R.drawable.ic_warning_black_24dp)
                isVisible = SHOW_SERVICE_UPDATE_FAB
            } else if (isInfo) {
                setImageResource(R.drawable.ic_info_outline_black_24dp)
                isVisible = SHOW_SERVICE_UPDATE_FAB
            } else {
                isVisible = false
            }
        }
    }

    private fun updateFabListMapUI(showingListInsteadOfMap: Boolean? = viewModel.showingListInsteadOfMap.value) {
        showingListInsteadOfMap ?: return
        isResumed || return
        binding?.fabListMap?.apply {
            if (showingListInsteadOfMap) { // LIST
                setImageResource(R.drawable.switch_action_map_dark_16dp)
                contentDescription = getString(R.string.menu_action_map)
                activity?.setNavBarProtectionEdgeToEdge()
            } else { // MAP
                setImageResource(R.drawable.switch_action_view_headline_dark_16dp)
                contentDescription = getString(R.string.menu_action_list)
                activity?.setNavBarProtectionEdgeToEdge(false)
            }
        }
    }

    private fun findStopIndexUuid(stopId: Int, pois: List<POIManager>?): Pair<Int?, String?>? {
        return pois
            ?.withIndex()
            ?.firstOrNull { (it.value.poi as? RouteDirectionStop)?.stop?.id == stopId }
            ?.let {
                it.index to it.value.poi.uuid
            }
    }

    private fun findClosestPOIIndexUuid(
        pois: List<POIManager>?,
        deviceLocation: Location? = parentViewModel.deviceLocation.value,
    ): Pair<Int?, String?>? {
        if (deviceLocation != null && pois?.isNotEmpty() == true) {
            return pois
                .updateDistance(deviceLocation.latitude, deviceLocation.longitude)
                .findClosestPOISIdxUuid()
                .firstOrNull()
        }
        return null
    }

    private fun switchView(showingListInsteadOfMap: Boolean? = viewModel.showingListInsteadOfMap.value) = binding?.apply {
        when {
            !listAdapter.isInitialized || showingListInsteadOfMap == null -> { // LOADING
                emptyLayout.isVisible = false
                listLayout.isVisible = false
                mapViewController.hideMap()
                loadingLayout.isVisible = true
            }

            listAdapter.poisCount == 0 -> { // EMPTY
                loadingLayout.isVisible = false
                listLayout.isVisible = false
                mapViewController.hideMap()
                emptyLayout.isVisible = true
            }

            else -> {
                loadingLayout.isVisible = false
                emptyLayout.isVisible = false
                if (context.twoPane) { // LARGE SCREEN
                    listLayout.isVisible = true
                    mapViewController.showMap(view)
                } else if (showingListInsteadOfMap) { // LIST
                    mapViewController.hideMap()
                    listLayout.isVisible = true
                } else { // MAP
                    listLayout.isVisible = false
                    mapViewController.showMap(view)
                }
            }
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

    override fun onSaveInstanceState(outState: Bundle) {
        mapViewController.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDetach() {
        super.onDetach()
        this.mapViewController.onDetach()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.mapVisible(context)) {
            mapViewController.onResume()
            viewModel.startVehicleLocationRefresh()
        } else {
            viewModel.stopVehicleLocationRefresh()
            stopVehicleLocationCountdownRefresh()
        }
        listAdapter.onResume(this, parentViewModel.deviceLocation.value)
        updateFabListMapUI()
        switchView()
    }

    private var _vehicleLocationCountdownRefreshJob: Job? = null

    private fun startVehicleLocationCountdownRefresh() {
        if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return
        _vehicleLocationCountdownRefreshJob?.cancel()
        _vehicleLocationCountdownRefreshJob = viewModel.viewModelScope.launch {
            while (true) {
                delay(1.seconds)
                context?.let { mapViewController.updateVehicleLocationMarkersCountdown(it) }
            }
        }
    }

    private fun stopVehicleLocationCountdownRefresh() {
        if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return
        _vehicleLocationCountdownRefreshJob?.cancel()
        _vehicleLocationCountdownRefreshJob = null
    }

    override fun onPause() {
        super.onPause()
        mapViewController.onPause()
        viewModel.stopVehicleLocationRefresh()
        stopVehicleLocationCountdownRefresh()
        listAdapter.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapViewController.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listAdapter.onDestroyView()
        mapViewController.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        listAdapter.onDestroy()
        mapViewController.onDestroy()
    }

    override fun <T : View?> findViewById(id: Int) = this.view?.findViewById<T>(id)

    fun RDSDirectionStopsViewModel.listVisible(context: Context?): Boolean = context.twoPane || showingListInsteadOfMap.value == true

    fun RDSDirectionStopsViewModel.mapVisible(context: Context?): Boolean = context.twoPane || showingListInsteadOfMap.value == false
}
