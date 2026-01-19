package org.mtransit.android.ui.rds.route.direction

import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProviderContract
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.RouteDirectionManager
import org.mtransit.android.data.VehicleLocationProviderProperties
import org.mtransit.android.data.toRouteDirectionM
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.QuadrupleMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@HiltViewModel
class RDSDirectionStopsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val poiRepository: POIRepository,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val demoModeManager: DemoModeManager,
    remoteConfigProvider: RemoteConfigProvider,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = RDSDirectionStopsViewModel::class.java.simpleName

        internal const val EXTRA_AGENCY_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_ROUTE_ID = "extra_route_id"
        internal const val EXTRA_DIRECTION_ID = "extra_direction_id"
        internal const val EXTRA_SELECTED_STOP_ID = "extra_direction_stop_id"
        internal const val EXTRA_SELECTED_STOP_ID_DEFAULT: Int = -1
        internal const val EXTRA_SELECTED_MAP_CAMERA_POSITION_LAT = "extra_map_lat"
        internal const val EXTRA_SELECTED_MAP_CAMERA_POSITION_LNG = "extra_map_lng"
        internal const val EXTRA_SELECTED_MAP_CAMERA_POSITION_ZOOM = "extra_map_zoom"

        internal const val EXTRA_CLOSEST_POI_SHOWN = "extra_closest_poi_shown"
        internal const val EXTRA_CLOSEST_POI_SHOWN_DEFAULT: Boolean = false

    }

    override fun getLogTag(): String = directionId.value?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    private val _authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AGENCY_AUTHORITY)

    private val _agency: LiveData<AgencyBaseProperties?> = this._authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgencyBase(authority) // #onModulesUpdated
    }

    private val _routeId = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_ROUTE_ID)

    private val _route: LiveData<Route?> = PairMediatorLiveData(_authority, _routeId).switchMap { (authority, routeId) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            authority ?: return@liveData
            routeId ?: return@liveData
            emit(dataSourceRequestManager.findRDSRoute(authority, routeId))
        }
    }

    val directionId = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_DIRECTION_ID)

    private val _direction: LiveData<Direction?> = PairMediatorLiveData(_authority, directionId).switchMap { (authority, directionId) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            authority ?: return@liveData
            directionId ?: return@liveData
            emit(dataSourceRequestManager.findRDSDirection(authority, directionId))
        }
    }

    private val _routeDirection: LiveData<RouteDirection?> = PairMediatorLiveData(_route, _direction).switchMap { (route, direction) ->
        liveData(viewModelScope.coroutineContext) {
            route ?: return@liveData
            direction ?: return@liveData
            emit(RouteDirection(route, direction))
        }
    }

    private val _routeDirectionTripIds: LiveData<List<String>?> =
        TripleMediatorLiveData(_authority, _routeId, directionId).switchMap { (authority, routeId, directionId) ->
            liveData(viewModelScope.coroutineContext) {
                if (!FeatureFlags.F_EXPORT_TRIP_ID) return@liveData
                authority ?: return@liveData
                routeId ?: return@liveData
                directionId ?: return@liveData
                emit(dataSourceRequestManager.findRDSRouteDirectionTrips(authority, routeId, directionId)?.map { it.tripId })
            }
        }

    private val _vehicleLocationRequestedTrigger = MutableLiveData<Int?>(null) // no initial value to avoid triggering onChanged()

    private var _vehicleRefreshJob: Job? = null

    private val _vehicleLocationDataRefreshMinMs = remoteConfigProvider.get(
        RemoteConfigProvider.VEHICLE_LOCATION_DATA_REFRESH_MIN_MS,
        RemoteConfigProvider.VEHICLE_LOCATION_DATA_REFRESH_MIN_MS_DEFAULT,
    )

    fun startVehicleLocationRefresh() {
        _vehicleRefreshJob?.cancel()
        _vehicleRefreshJob = viewModelScope.launch {
            while (true) {
                _vehicleLocationRequestedTrigger.value = (_vehicleLocationRequestedTrigger.value ?: 0) + 1
                delay(_vehicleLocationDataRefreshMinMs)
            }
        }
    }

    fun stopVehicleLocationRefresh() {
        _vehicleLocationRequestedTrigger.value = null // disable when not visible
        _vehicleRefreshJob?.cancel()
        _vehicleRefreshJob = null
    }

    private val _vehicleLocationProviders: LiveData<List<VehicleLocationProviderProperties>> = _authority.switchMap {
        dataSourcesRepository.readingVehicleLocationProviders(it) // #onModulesUpdated
    }

    // TODO use VehicleLocationLoader like status and service update?
    val vehicleLocations: LiveData<List<VehicleLocation>?> =
        QuadrupleMediatorLiveData(
            _vehicleLocationProviders,
            _routeDirection,
            _routeDirectionTripIds,
            _vehicleLocationRequestedTrigger
        ).switchMap { (vehicleLocationProviders, rd, tripIds, trigger) ->
            liveData(viewModelScope.coroutineContext) {
                if (!FeatureFlags.F_EXPORT_TRIP_ID) return@liveData
                vehicleLocationProviders ?: return@liveData
                rd ?: return@liveData
                tripIds ?: return@liveData
                trigger ?: return@liveData // skip when not visible
                emit(
                    vehicleLocationProviders.mapNotNull {
                        dataSourceRequestManager.findRDSVehicleLocations(it, VehicleLocationProviderContract.Filter(rd, tripIds).apply { inFocus = true })
                    }.flatten()
                )
            }
        }

    val vehicleLocationsDistinct = vehicleLocations.distinctUntilChanged()

    val selectedStopId = savedStateHandle.getLiveDataDistinct(EXTRA_SELECTED_STOP_ID, EXTRA_SELECTED_STOP_ID_DEFAULT)
        .map { if (it < 0) null else it }

    fun onSelectedStopIdSet() {
        savedStateHandle[EXTRA_SELECTED_STOP_ID] = EXTRA_SELECTED_STOP_ID_DEFAULT
    }

    private val _selectedMapCameraPositionLat = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_SELECTED_MAP_CAMERA_POSITION_LAT)
    private val _selectedMapCameraPositionLng = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_SELECTED_MAP_CAMERA_POSITION_LNG)
    private val _selectedMapCameraPositionZoom = savedStateHandle.getLiveDataDistinct<Float?>(EXTRA_SELECTED_MAP_CAMERA_POSITION_ZOOM)

    val selectedMapCameraPosition =
        TripleMediatorLiveData(_selectedMapCameraPositionLat, _selectedMapCameraPositionLng, _selectedMapCameraPositionZoom).map { (lat, lng, zoom) ->
            lat ?: return@map null
            lng ?: return@map null
            zoom ?: return@map null
            CameraPosition.fromLatLngZoom(LatLng(lat, lng), zoom)
        }.distinctUntilChanged()

    fun onSelectedMapCameraPositionSet() {
        savedStateHandle[EXTRA_SELECTED_MAP_CAMERA_POSITION_LAT] = null
        savedStateHandle[EXTRA_SELECTED_MAP_CAMERA_POSITION_LNG] = null
        savedStateHandle[EXTRA_SELECTED_MAP_CAMERA_POSITION_ZOOM] = null
    }

    val closestPOIShown = savedStateHandle.getLiveDataDistinct(EXTRA_CLOSEST_POI_SHOWN, EXTRA_CLOSEST_POI_SHOWN_DEFAULT)

    fun setSelectedOrClosestStopShown() {
        savedStateHandle[EXTRA_SELECTED_STOP_ID] = EXTRA_SELECTED_STOP_ID_DEFAULT
        savedStateHandle[EXTRA_CLOSEST_POI_SHOWN] = true
    }

    val routeDirectionM: LiveData<RouteDirectionManager> = PairMediatorLiveData(_authority, _routeDirection).switchMap { (authority, routeDirection) ->
        liveData(viewModelScope.coroutineContext) {
            authority ?: return@liveData
            routeDirection ?: return@liveData
            emit(
                routeDirection.toRouteDirectionM(authority)
                    .apply {
                        addServiceUpdateLoaderListener(serviceUpdateLoaderListener)
                    }
            )
        }
    }

    private val _serviceUpdateLoadedEvent = MutableLiveData<Event<String>>()
    val serviceUpdateLoadedEvent: LiveData<Event<String>> = _serviceUpdateLoadedEvent

    private var serviceUpdateLoadedJob: Job? = null

    private val serviceUpdateLoaderListener = ServiceUpdateLoader.ServiceUpdateLoaderListener { targetUUID, _ ->
        serviceUpdateLoadedJob?.cancel()
        serviceUpdateLoadedJob = viewModelScope.launch {
            if (routeDirectionM.value?.routeDirection?.uuid != targetUUID) {
                delay(333L) // wait for 0.333 secs BECAUSE many POIMs can also trigger it
            }
            routeDirectionM.value?.apply {
                if (this.routeDirection.uuid != targetUUID) {
                    this.allowFindServiceUpdates() // allow to fetch following RDS update
                }
            }
            _serviceUpdateLoadedEvent.postValue(Event(targetUUID))
        }
    }

    val poiList: LiveData<List<POIManager>?> = PairMediatorLiveData(_agency, directionId).switchMap { (agency, directionId) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            agency ?: return@liveData
            directionId ?: return@liveData
            emit(getPOIList(agency, directionId))
        }
    }

    private suspend fun getPOIList(agency: IAgencyProperties, directionId: Long) =
        this.poiRepository.findPOIMs(
            agency,
            POIProviderContract.Filter.getNewSqlSelectionFilter(
                SqlUtils.getWhereEquals(
                    GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ID, directionId
                )
            ).apply {
                addExtra(
                    POIProviderContract.POI_FILTER_EXTRA_SORT_ORDER,
                    SqlUtils.getSortOrderAscending(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_STOP_SEQUENCE)
                )
            }
        ).apply {
            forEach { poim ->
                poim.addServiceUpdateLoaderListener(serviceUpdateLoaderListener) // trigger refresh because some provider do not fetch for route #stmbus
            }
        }

    val showingListInsteadOfMap: LiveData<Boolean> = TripleMediatorLiveData(_authority, _routeId, directionId).switchMap { (authority, routeId, directionId) ->
        liveData {
            authority ?: return@liveData
            routeId ?: return@liveData
            directionId ?: return@liveData
            if (demoModeManager.isFullDemo()) {
                emit(false) // show map (demo mode ON)
                return@liveData
            }
            emitSource(
                lclPrefRepository.pref.liveData(
                    LocalPreferenceRepository.getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(authority, routeId, directionId),
                    LocalPreferenceRepository.PREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT
                )
            )
        }
    }.distinctUntilChanged()

    fun saveShowingListInsteadOfMap(showingListInsteadOfMap: Boolean) {
        if (demoModeManager.isFullDemo()) return // SKIP (demo mode ON)
        val authority = _authority.value ?: return
        val routeId = _routeId.value ?: return
        val directionId = directionId.value ?: return
        lclPrefRepository.pref.edit {
            putBoolean(
                LocalPreferenceRepository.getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(authority, routeId, directionId),
                showingListInsteadOfMap
            )
        }
    }
}