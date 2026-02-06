package org.mtransit.android.ui.rds.route

import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.data.RouteManager
import org.mtransit.android.data.toRouteM
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@HiltViewModel
class RDSRouteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val dataSourcesRepository: DataSourcesRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val statusLoader: StatusLoader,
    private val serviceUpdateLoader: ServiceUpdateLoader,
) : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG = RDSRouteViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_authority"
        internal const val EXTRA_ROUTE_ID = "extra_route_id"
        internal const val EXTRA_SELECTED_DIRECTION_ID = "extra_direction_id"
        internal const val EXTRA_SELECTED_DIRECTION_ID_DEFAULT: Long = -1L
        internal const val EXTRA_SELECTED_STOP_ID = "extra_stop_id"
        internal const val EXTRA_SELECTED_STOP_ID_DEFAULT: Int = -1

        internal const val EXTRA_SELECTED_MAP_CAMERA_POSITION_LAT = "extra_map_lat"
        internal const val EXTRA_SELECTED_MAP_CAMERA_POSITION_LNG = "extra_map_lng"
        internal const val EXTRA_SELECTED_MAP_CAMERA_POSITION_ZOOM = "extra_map_zoom"
    }

    override fun getLogTag(): String = LOG_TAG

    private val _authority = savedStateHandle.getLiveDataDistinct<String>(EXTRA_AUTHORITY)
    private val _routeId = savedStateHandle.getLiveDataDistinct<Long>(EXTRA_ROUTE_ID)
    private val _selectedDirectionId = savedStateHandle.getLiveDataDistinct(EXTRA_SELECTED_DIRECTION_ID, EXTRA_SELECTED_DIRECTION_ID_DEFAULT)
        .map { if (it < 0L) null else it }
    val originalSelectedDirectionId = _selectedDirectionId
    val selectedStopId = savedStateHandle.getLiveDataDistinct(EXTRA_SELECTED_STOP_ID, EXTRA_SELECTED_STOP_ID_DEFAULT)
        .map { if (it < 0) null else it }

    private val _selectedMapCameraPositionLat = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_SELECTED_MAP_CAMERA_POSITION_LAT)
    private val _selectedMapCameraPositionLng = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_SELECTED_MAP_CAMERA_POSITION_LNG)
    private val _selectedMapCameraPositionZoom = savedStateHandle.getLiveDataDistinct<Float?>(EXTRA_SELECTED_MAP_CAMERA_POSITION_ZOOM)

    val selectedMapCameraPosition = TripleMediatorLiveData(_selectedMapCameraPositionLat, _selectedMapCameraPositionLng, _selectedMapCameraPositionZoom)
        .map { (lat, lng, zoom) ->
            lat ?: return@map null
            lng ?: return@map null
            zoom ?: return@map null
            CameraPosition.fromLatLngZoom(LatLng(lat, lng), zoom)
        }.distinctUntilChanged()

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    private val _agency: LiveData<AgencyBaseProperties?> = _authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgencyBase(authority) // #onModulesUpdated
    }

    private val _route: LiveData<Route?> = PairMediatorLiveData(_agency, _routeId).switchMap { (agency, routeId) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getRoute(agency, routeId))
        }
    }

    private val _routeTripIds: LiveData<List<String>?> = PairMediatorLiveData(_authority, _routeId)
        .switchMap { (authority, routeId) ->
            liveData(viewModelScope.coroutineContext) {
                if (!FeatureFlags.F_EXPORT_TRIP_ID) return@liveData
                if (FeatureFlags.F_PROVIDER_READS_TRIP_ID_DIRECTLY) return@liveData
                if (!FeatureFlags.F_USE_TRIP_IS_FOR_SERVICE_UPDATES) return@liveData
                authority ?: return@liveData
                routeId ?: return@liveData
                //noinspection DiscouragedApi TODO enable F_PROVIDER_READS_TRIP_ID_DIRECTLY
                emit(dataSourceRequestManager.findRDSTrips(authority, routeId)?.map { it.tripId })
            }
        }

    val routeM: LiveData<RouteManager> = TripleMediatorLiveData(_authority, _route, _routeTripIds)
        .switchMap { (authority, route, routeTripIds) ->
            liveData(viewModelScope.coroutineContext) {
                authority ?: return@liveData
                route ?: return@liveData
                if (FeatureFlags.F_USE_TRIP_IS_FOR_SERVICE_UPDATES) {
                    if (!FeatureFlags.F_PROVIDER_READS_TRIP_ID_DIRECTLY) {
                        routeTripIds ?: return@liveData
                    }
                }
                emit(
                    route.toRouteM(authority, routeTripIds)
                        .apply {
                            addServiceUpdateLoaderListener(serviceUpdateLoaderListener)
                        }
                )
            }
        }

    private val _serviceUpdateLoadedEvent = MutableLiveData<Event<String>>()
    val serviceUpdateLoadedEvent: LiveData<Event<String>> = _serviceUpdateLoadedEvent

    private val serviceUpdateLoaderListener = ServiceUpdateLoader.ServiceUpdateLoaderListener { targetUUID, _ ->
        _serviceUpdateLoadedEvent.postValue(Event(targetUUID))
    }

    private suspend fun getRoute(agency: IAgencyUIProperties?, routeId: Long?): Route? {
        routeId ?: return null
        if (agency == null) {
            if (_route.value != null) {
                MTLog.d(this, "getRoute() > data source removed (no more agency)")
                dataSourceRemovedEvent.postValue(Event(true))
            }
            return null
        }
        val newRoute = dataSourceRequestManager.findRDSRoute(agency.authority, routeId)
        if (newRoute == null) {
            MTLog.d(this, "getRoute() > data source updated (no more route)")
            dataSourceRemovedEvent.postValue(Event(true))
        }
        return newRoute
    }

    val colorInt: LiveData<Int?> = PairMediatorLiveData(_route, _agency).map { (route, agency) ->
        route?.let { if (it.hasColor()) route.colorInt else agency?.colorInt }
    }

    val routeType: LiveData<DataSourceType?> = _agency.map { it?.type }

    val routeDirections: LiveData<List<Direction>?> = PairMediatorLiveData(_authority, _routeId).switchMap { (authority, routeId) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getRouteDirections(authority, routeId))
        }
    }

    private suspend fun getRouteDirections(authority: String?, routeId: Long?): List<Direction>? {
        authority ?: return null
        routeId ?: return null
        return this.dataSourceRequestManager.findRDSRouteDirections(authority, routeId)
    }

    private val _selectedDirectionIdPref: LiveData<Long?> = PairMediatorLiveData(_authority, _routeId).switchMap { (authority, routeId) ->
        authority ?: return@switchMap MutableLiveData(null)
        routeId ?: return@switchMap MutableLiveData(null)
        lclPrefRepository.pref.liveData(
            LocalPreferenceRepository.getPREFS_LCL_RDS_ROUTE_DIRECTION_ID_TAB(authority, routeId),
            LocalPreferenceRepository.PREFS_LCL_RDS_ROUTE_DIRECTION_ID_TAB_DEFAULT
        )
    }

    fun onPageSelected(position: Int) {
        this.statusLoader.clearAllTasks()
        this.serviceUpdateLoader.clearAllTasks()
        saveSelectedRouteDirectionIdPosition(position)
    }

    private fun saveSelectedRouteDirectionIdPosition(position: Int) {
        saveSelectedRouteDirectionId(
            routeDirections.value?.getOrNull(position) ?: return
        )
    }

    private fun saveSelectedRouteDirectionId(direction: Direction) {
        val authority = this._authority.value ?: return
        val routeId = this._routeId.value ?: return
        lclPrefRepository.pref.edit {
            putLong(LocalPreferenceRepository.getPREFS_LCL_RDS_ROUTE_DIRECTION_ID_TAB(authority, routeId), direction.id)
        }
    }

    private val currentSelectedDirectionId: LiveData<Long?> = PairMediatorLiveData(_selectedDirectionId, _selectedDirectionIdPref)
        .map { (selectedDirectionId, selectedDirectionIdPref) ->
            selectedDirectionId ?: selectedDirectionIdPref
        }.distinctUntilChanged()

    val currentSelectedRouteDirectionPosition: LiveData<Int?> = PairMediatorLiveData(currentSelectedDirectionId, routeDirections)
        .map { (directionId, routeDirections) ->
            directionId ?: return@map null
            routeDirections ?: return@map null
            routeDirections.indexOfFirst { it.id == directionId }.coerceAtLeast(0)
        }
}