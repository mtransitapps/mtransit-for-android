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
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
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

    val authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AUTHORITY)
    val routeId = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_ROUTE_ID)
    private val _selectedTripId = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_SELECTED_TRIP_ID)
    val selectedStopId = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_SELECTED_STOP_ID)

    val listInsteadOfMap = savedStateHandle.getLiveDataDistinct<Boolean?>(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP)

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    private val _agency: LiveData<AgencyBaseProperties?> = authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgencyBase(authority) // #onModulesUpdated
    }

    val route: LiveData<Route?> = PairMediatorLiveData(_agency, routeId).switchMap { (agency, routeId) ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getRoute(agency, routeId))
        }
    }

    private fun getRoute(agency: IAgencyUIProperties?, routeId: Long?): Route? {
        if (routeId == null) {
            return null
        }
        if (agency == null) {
            if (route.value != null) {
                MTLog.d(this, "getRoute() > data source removed (no more agency)")
                dataSourceRemovedEvent.postValue(Event(true))
            }
            return null
        }
        val newRoute = dataSourceRequestManager.findRTSRoute(agency.authority, routeId)
        if (newRoute == null) {
            MTLog.d(this, "getRoute() > data source updated (no more route)")
            dataSourceRemovedEvent.postValue(Event(true))
        }
        return newRoute
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

    fun onPageSelected(position: Int) {
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
        PreferenceUtils.PREFS_LCL_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT
    )

    val showingListInsteadOfMap: LiveData<Boolean?> = PairMediatorLiveData(listInsteadOfMap, _listInsteadOfMapDefault).map { (listInsteadOfMap, default) ->
        listInsteadOfMap ?: default
    }.distinctUntilChanged()

    fun saveShowingListInsteadOfMap(showingListInsteadOfMap: Boolean) {
        savedStateHandle[EXTRA_SHOWING_LIST_INSTEAD_OF_MAP] = showingListInsteadOfMap
    }
}