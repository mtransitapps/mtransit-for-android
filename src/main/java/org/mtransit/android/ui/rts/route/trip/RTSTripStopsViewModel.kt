package org.mtransit.android.ui.rts.route.trip

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject


@HiltViewModel
class RTSTripStopsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val poiRepository: POIRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = RTSTripStopsViewModel::class.java.simpleName

        internal const val EXTRA_AGENCY_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_TRIP_ID = "extra_trip_id"
        internal const val EXTRA_SELECTED_TRIP_STOP_ID = "extra_trip_stop_id"

        internal const val EXTRA_CLOSEST_POI_SHOWN = "extra_closest_poi_shown"
    }

    override fun getLogTag(): String = tripId.value?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    val agencyAuthority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AGENCY_AUTHORITY)

    val tripId = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_TRIP_ID)

    val selectedTripStopId = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_SELECTED_TRIP_STOP_ID)

    val closestPOIShown = savedStateHandle.getLiveDataDistinct(EXTRA_CLOSEST_POI_SHOWN, false)

    fun setSelectedOrClosestStopShown() {
        savedStateHandle[EXTRA_SELECTED_TRIP_STOP_ID] = null
        savedStateHandle[EXTRA_CLOSEST_POI_SHOWN] = true
    }

    val poiList: LiveData<List<POIManager>?> = PairMediatorLiveData(agencyAuthority, tripId).switchMap { (agencyAuthority, tripId) ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getPOIList(agencyAuthority, tripId))
        }
    }

    private fun getPOIList(agencyAuthority: String?, tripId: Long?): List<POIManager>? {
        if (agencyAuthority == null || tripId == null) {
            return null
        }
        val poiFilter = POIProviderContract.Filter.getNewSqlSelectionFilter(
            SqlUtils.getWhereEquals(
                GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ID, tripId
            )
        ).apply {
            addExtra(
                POIProviderContract.POI_FILTER_EXTRA_SORT_ORDER,
                SqlUtils.getSortOrderAscending(GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE)
            )
        }
        return this.poiRepository.findPOIMs(
            agencyAuthority,
            poiFilter
        )
    }
}