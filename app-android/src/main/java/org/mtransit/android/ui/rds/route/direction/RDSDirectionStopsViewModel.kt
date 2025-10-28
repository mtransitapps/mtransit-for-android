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
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.RouteDirectionManager
import org.mtransit.android.data.toRouteDirectionM
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject


@HiltViewModel
class RDSDirectionStopsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val poiRepository: POIRepository,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val demoModeManager: DemoModeManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = RDSDirectionStopsViewModel::class.java.simpleName

        internal const val EXTRA_AGENCY_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_ROUTE_ID = "extra_route_id"
        internal const val EXTRA_DIRECTION_ID = "extra_direction_id"
        internal const val EXTRA_SELECTED_STOP_ID = "extra_direction_stop_id"
        internal const val EXTRA_SELECTED_STOP_ID_DEFAULT: Int = -1

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

    val selectedStopId = savedStateHandle.getLiveDataDistinct(EXTRA_SELECTED_STOP_ID, EXTRA_SELECTED_STOP_ID_DEFAULT)
        .map { if (it < 0) null else it }

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

    private val serviceUpdateLoaderListener = ServiceUpdateLoader.ServiceUpdateLoaderListener { targetUUID, serviceUpdates ->
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
            emit(getPOIList(agency, directionId))
        }
    }

    private suspend fun getPOIList(agency: IAgencyProperties?, directionId: Long?): List<POIManager>? {
        if (agency == null || directionId == null) {
            return null
        }
        val poiFilter = POIProviderContract.Filter.getNewSqlSelectionFilter(
            SqlUtils.getWhereEquals(
                GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ID, directionId
            )
        ).apply {
            addExtra(
                POIProviderContract.POI_FILTER_EXTRA_SORT_ORDER,
                SqlUtils.getSortOrderAscending(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_STOP_SEQUENCE)
            )
        }
        return this.poiRepository.findPOIMs(agency, poiFilter)
            .apply {
                forEach { poim ->
                    poim.addServiceUpdateLoaderListener(serviceUpdateLoaderListener) // trigger refresh because some provider do not fetch for route #stmbus
                }
            }
    }

    val showingListInsteadOfMap: LiveData<Boolean> = TripleMediatorLiveData(_authority, _routeId, directionId).switchMap { (authority, routeId, directionId) ->
        liveData {
            if (authority == null || routeId == null || directionId == null) {
                return@liveData // SKIP
            }
            if (demoModeManager.isFullDemo()) {
                emit(false) // show map (demo mode ON)
                return@liveData
            }
            emitSource(
                lclPrefRepository.pref.liveData(
                    LocalPreferenceRepository.getPREFS_LCL_RDS_ROUTE_DIRECTION_ID_KEY(authority, routeId, directionId),
                    LocalPreferenceRepository.PREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT
                )
            )
        }
    }.distinctUntilChanged()

    fun saveShowingListInsteadOfMap(showingListInsteadOfMap: Boolean) {
        if (demoModeManager.isFullDemo()) {
            return // SKIP (demo mode ON)
        }
        lclPrefRepository.pref.edit {
            val authority = _authority.value ?: return
            val routeId = _routeId.value ?: return
            val directionId = directionId.value ?: return
            putBoolean(
                LocalPreferenceRepository.getPREFS_LCL_RDS_ROUTE_DIRECTION_ID_KEY(authority, routeId, directionId),
                showingListInsteadOfMap
            )
        }
    }
}