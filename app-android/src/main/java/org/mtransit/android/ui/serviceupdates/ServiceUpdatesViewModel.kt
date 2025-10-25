package org.mtransit.android.ui.serviceupdates

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.data.toRouteDirectionM
import org.mtransit.android.data.toRouteM
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.serviceupdate.ServiceUpdatesHolder
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class ServiceUpdatesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourceRequestManager: DataSourceRequestManager,
) : ViewModel(), MTLog.Loggable {
    companion object {
        private val LOG_TAG: String = ServiceUpdatesViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_authority"
        internal const val EXTRA_ROUTE_ID = "extra_route_id"
        internal const val EXTRA_DIRECTION_ID = "extra_direction_id"
    }

    override fun getLogTag() = LOG_TAG

    private val _authority = savedStateHandle.getLiveDataDistinct<String>(EXTRA_AUTHORITY)
    private val _routeId = savedStateHandle.getLiveDataDistinct<Long>(EXTRA_ROUTE_ID)

    private val _directionId = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_DIRECTION_ID)

    private val _route: LiveData<Route?> = PairMediatorLiveData(_authority, _routeId).switchMap { (authority, routeId) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            authority ?: return@liveData
            routeId ?: return@liveData
            emit(dataSourceRequestManager.findRDSRoute(authority, routeId))
        }
    }

    private val _direction: LiveData<Direction?> = PairMediatorLiveData(_authority, _directionId).switchMap { (authority, directionId) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            authority ?: return@liveData
            directionId ?: return@liveData
            emit(dataSourceRequestManager.findRDSDirection(authority, directionId))
        }
    }

    val holder: LiveData<ServiceUpdatesHolder> = TripleMediatorLiveData(_authority, _route, _direction).switchMap { (authority, route, direction) ->
        liveData(viewModelScope.coroutineContext) {
            authority ?: return@liveData
            route ?: return@liveData
            val holder: ServiceUpdatesHolder = direction?.let {
                RouteDirection(route, it).toRouteDirectionM(authority)
            } ?: route.toRouteM(authority)
            emit(
                holder
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
            _serviceUpdateLoadedEvent.postValue(Event(targetUUID))
        }
    }
}
