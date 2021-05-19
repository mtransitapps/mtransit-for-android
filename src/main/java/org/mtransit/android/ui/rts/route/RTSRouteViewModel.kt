package org.mtransit.android.ui.rts.route

import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import javax.inject.Inject

@HiltViewModel
class RTSRouteViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val dataSourcesRepository: DataSourcesRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val statusLoader: StatusLoader,
    private val serviceUpdateLoader: ServiceUpdateLoader,
) : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG = RTSRouteViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_authority"
        internal const val EXTRA_ROUTE_ID = "extra_route_id"
        internal const val EXTRA_SELECTED_TRIP_ID = "extra_trip_id"
        internal const val EXTRA_SELECTED_STOP_ID = "extra_stop_id"

        internal const val EXTRA_SHOWING_LIST_INSTEAD_OF_MAP = "extra_showing_list_instead_of_map"
    }

    override fun getLogTag(): String = LOG_TAG

    val authority = savedStateHandle.getLiveData<String?>(EXTRA_AUTHORITY, null).distinctUntilChanged()
    val routeId = savedStateHandle.getLiveData<Long?>(EXTRA_ROUTE_ID, null).distinctUntilChanged()
    private val _selectedTripId = savedStateHandle.getLiveData<Long?>(EXTRA_SELECTED_TRIP_ID, null).distinctUntilChanged()
    val selectedStopId = savedStateHandle.getLiveData<Int?>(EXTRA_SELECTED_STOP_ID, null).distinctUntilChanged()

    val listInsteadOfMap = savedStateHandle.getLiveData<Boolean?>(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, null).distinctUntilChanged()

    val route: LiveData<Route?> = PairMediatorLiveData(authority, routeId).switchMap { (authority, routeId) ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (routeId == null || authority == null) {
                emit(null)
            } else {
                emit(dataSourceRequestManager.findRTSRoute(authority, routeId))
            }
        }
    }

    private val _agency: LiveData<AgencyProperties?> = authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgency(authority)
    }

    val colorInt: LiveData<Int?> = PairMediatorLiveData(route, _agency).map { (route, agency) ->
        route?.let { if (it.hasColor()) route.colorInt else agency?.colorInt }
    }

    val routeTrips: LiveData<List<Trip>?> = PairMediatorLiveData(authority, routeId).switchMap { (authority, routeId) ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getRouteTrips(authority, routeId))
        }
    }

    private fun getRouteTrips(authority: String?, routeId: Long?): List<Trip>? {
        if (authority == null || routeId == null) {
            return null
        }
        return this.dataSourceRequestManager.findRTSRouteTrips(authority, routeId)
    }

    private val _selectedTripIdPref: LiveData<Long?> = PairMediatorLiveData(authority, routeId).switchMap { (authority, routeId) ->
        if (authority == null || routeId == null) {
            MutableLiveData(null)
        } else {
            lclPrefRepository.pref.liveData(
                PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(authority, routeId),
                PreferenceUtils.PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT
            )
        }
    }

    fun onPagetSelected(position: Int) {
        this.statusLoader.clearAllTasks()
        this.serviceUpdateLoader.clearAllTasks()
        saveSelectedRouteTripIdPosition(position)
    }

    private fun saveSelectedRouteTripIdPosition(position: Int) {
        saveSelectedRouteTripId(
            routeTrips.value?.getOrNull(position) ?: return
        )
    }

    private fun saveSelectedRouteTripId(trip: Trip) {
        val authority: String = this.authority.value ?: return
        val routeId: Long = this.routeId.value ?: return
        lclPrefRepository.pref.edit {
            putLong(PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(authority, routeId), trip.id)
        }
    }

    val selectedTripId: LiveData<Long?> = PairMediatorLiveData(_selectedTripId, _selectedTripIdPref).map { (selectedTripId, selectedTripIdPref) ->
        selectedTripId ?: selectedTripIdPref
    }.distinctUntilChanged()

    val selectedRouteTripPosition: LiveData<Int?> = PairMediatorLiveData(selectedTripId, routeTrips).map { (tripId, routeTrips) ->
        if (tripId == null || routeTrips == null) {
            null
        } else {
            routeTrips.indexOfFirst { it.id == tripId }.coerceAtLeast(0)
        }
    }

    private val _listInsteadOfMapDefault: LiveData<Boolean?> = MutableLiveData(
        PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT
    )

    val showingListInsteadOfMap: LiveData<Boolean?> = PairMediatorLiveData(listInsteadOfMap, _listInsteadOfMapDefault).map { (listInsteadOfMap, default) ->
        listInsteadOfMap ?: default
    }.distinctUntilChanged()

    fun saveShowingListInsteadOfMap(showingListInsteadOfMap: Boolean) {
        savedStateHandle[EXTRA_SHOWING_LIST_INSTEAD_OF_MAP] = showingListInsteadOfMap
    }
}