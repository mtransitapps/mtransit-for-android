package org.mtransit.android.ui.type.rds

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
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.RouteManager
import org.mtransit.android.data.toRouteM
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class RDSAgencyRoutesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val defaultPrefRepository: DefaultPreferenceRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = RDSAgencyRoutesViewModel::class.java.simpleName

        internal const val EXTRA_AGENCY_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_COLOR_INT = "extra_color_int"
    }

    override fun getLogTag(): String = agency.value?.shortName?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    private val _authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AGENCY_AUTHORITY)

    val authorityShort: LiveData<String?> = _authority.map {
        it?.substringAfter(IAgencyProperties.PKG_COMMON)
    }

    val colorInt = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_COLOR_INT)

    val agency: LiveData<AgencyProperties?> = this._authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgency(authority) // #onModulesUpdated
    }

    private val _routes: LiveData<List<Route>> = this._authority.switchMap { authority ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            authority ?: return@liveData
            val newRoutes = dataSourceRequestManager.findAllRDSAgencyRoutes(authority)
                .sortedWith(Route.SHORT_NAME_COMPARATOR)
            if (newRoutes != _routes.value) { // do not
                emit(newRoutes)
            }
        }
    }.distinctUntilChanged()

    val routesM: LiveData<List<RouteManager>> = MediatorLiveData2(agency, _routes).switchMap { (agency, routes) ->
        liveData(viewModelScope.coroutineContext) {
            agency ?: return@liveData
            routes ?: return@liveData
            emit(routes.map { route ->
                route.toRouteM(agency.authority)
                    .apply {
                        addServiceUpdateLoaderListener(serviceUpdateLoaderListener)
                    }
            })
        }
    }

    private val _serviceUpdateLoadedEvent = MutableLiveData<Event<String>>()
    val serviceUpdateLoadedEvent: LiveData<Event<String>> = _serviceUpdateLoadedEvent

    private var serviceUpdateLoadedJob: Job? = null

    private val serviceUpdateLoaderListener = ServiceUpdateLoader.ServiceUpdateLoaderListener { targetUUID, _ ->
        serviceUpdateLoadedJob?.cancel()
        serviceUpdateLoadedJob = viewModelScope.launch {
            delay(333L) // wait for 0.333 secs BECAUSE many routes & will trigger RecyclerView.notifyDataSetChanged()
            _serviceUpdateLoadedEvent.postValue(Event(targetUUID))
        }
    }

    private val _routeColorInts: LiveData<List<Int>> = _routes.map { routes ->
        routes.filter {
            it.hasColor()
        }.map {
            it.colorInt
        }
    }

    val colorIntDistinct: LiveData<Int?> = MediatorLiveData2(colorInt, _routeColorInts).map { (colorInt, routeColorInts) ->
        colorInt?.let {
            if (routeColorInts == null || (routeColorInts.isNotEmpty() && !routeColorInts.contains(colorInt))) {
                colorInt
            } else {
                val distinctColorInt = routeColorInts.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: colorInt
                if (ColorUtils.isTooLight(distinctColorInt)) {
                    ColorUtils.darkenColor(distinctColorInt, 0.2F)
                } else {
                    ColorUtils.lightenColor(distinctColorInt, 0.2F)
                }
            }
        }
    }

    val showingListInsteadOfGrid: LiveData<Boolean> = MediatorLiveData2(_authority, _routes).switchMap { (authority, routes) ->
        liveData(viewModelScope.coroutineContext) { // emit source Live Data = stay Main Thread
            routes ?: return@liveData
            authority ?: return@liveData
            emitSource(
                defaultPrefRepository.pref.liveData(
                    DefaultPreferenceRepository.getPREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(authority),
                    defaultPrefRepository.getPREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT(routes.size)
                )
            )
        }
    }.distinctUntilChanged()

    fun saveShowingListInsteadOfGrid(showingListInsteadOfGrid: Boolean) {
        defaultPrefRepository.pref.edit {
            _authority.value?.let { authority ->
                putBoolean(DefaultPreferenceRepository.getPREFS_RDS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(authority), showingListInsteadOfGrid)
            }
        }
    }
}