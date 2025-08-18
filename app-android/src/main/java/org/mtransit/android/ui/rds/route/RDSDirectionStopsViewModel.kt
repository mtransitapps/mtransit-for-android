package org.mtransit.android.ui.rds.route.direction

import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject


@HiltViewModel
class RDSDirectionStopsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val poiRepository: POIRepository,
    private val dataSourcesRepository: DataSourcesRepository,
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

    override fun getLogTag(): String = tripId.value?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    private val _authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AGENCY_AUTHORITY)

    private val _agency: LiveData<AgencyBaseProperties?> = this._authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgencyBase(authority) // #onModulesUpdated
    }

    private val _routeId = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_ROUTE_ID)

    val tripId = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_DIRECTION_ID)

    val selectedTripStopId = savedStateHandle.getLiveDataDistinct(EXTRA_SELECTED_STOP_ID, EXTRA_SELECTED_STOP_ID_DEFAULT)
        .map { if (it < 0) null else it }

    val closestPOIShown = savedStateHandle.getLiveDataDistinct(EXTRA_CLOSEST_POI_SHOWN, EXTRA_CLOSEST_POI_SHOWN_DEFAULT)

    fun setSelectedOrClosestStopShown() {
        savedStateHandle[EXTRA_SELECTED_STOP_ID] = EXTRA_SELECTED_STOP_ID_DEFAULT
        savedStateHandle[EXTRA_CLOSEST_POI_SHOWN] = true
    }

    val poiList: LiveData<List<POIManager>?> = PairMediatorLiveData(_agency, tripId).switchMap { (agency, tripId) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getPOIList(agency, tripId))
        }
    }

    private suspend fun getPOIList(agency: IAgencyProperties?, tripId: Long?): List<POIManager>? {
        if (agency == null || tripId == null) {
            return null
        }
        val poiFilter = POIProviderContract.Filter.getNewSqlSelectionFilter(
            SqlUtils.getWhereEquals(
                GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ID, tripId
            )
        ).apply {
            addExtra(
                POIProviderContract.POI_FILTER_EXTRA_SORT_ORDER,
                SqlUtils.getSortOrderAscending(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_STOP_SEQUENCE)
            )
        }
        return this.poiRepository.findPOIMs(agency, poiFilter)
    }

    val showingListInsteadOfMap: LiveData<Boolean> = TripleMediatorLiveData(_authority, _routeId, tripId).switchMap { (authority, routeId, tripId) ->
        liveData {
            if (authority == null || routeId == null || tripId == null) {
                return@liveData // SKIP
            }
            if (demoModeManager.isFullDemo()) {
                emit(false) // show map (demo mode ON)
                return@liveData
            }
            emitSource(
                lclPrefRepository.pref.liveData(
                    LocalPreferenceRepository.getPREFS_LCL_RDS_ROUTE_DIRECTION_ID_KEY(authority, routeId, tripId),
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
            val tripId = tripId.value ?: return
            putBoolean(
                LocalPreferenceRepository.getPREFS_LCL_RDS_ROUTE_DIRECTION_ID_KEY(authority, routeId, tripId),
                showingListInsteadOfMap
            )
        }
    }
}