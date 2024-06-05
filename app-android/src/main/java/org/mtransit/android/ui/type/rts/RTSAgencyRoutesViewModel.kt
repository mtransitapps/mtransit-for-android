package org.mtransit.android.ui.type.rts

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
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class RTSAgencyRoutesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val defaultPrefRepository: DefaultPreferenceRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = RTSAgencyRoutesViewModel::class.java.simpleName

        internal const val EXTRA_AGENCY_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_COLOR_INT = "extra_color_int"
    }

    override fun getLogTag(): String = agency.value?.shortName?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    private val _authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AGENCY_AUTHORITY)

    val authorityShort: LiveData<String?> = _authority.map {
        it?.substringAfter(IAgencyProperties.PKG_COMMON)
    }

    val colorInt = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_COLOR_INT)

    val agency: LiveData<AgencyBaseProperties?> = this._authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgencyBase(authority) // #onModulesUpdated
    }

    val routes: LiveData<List<Route>?> = this._authority.switchMap { authority ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            authority?.let {
                emit(
                    dataSourceRequestManager.findAllRTSAgencyRoutes(authority)
                        ?.sortedWith(Route.SHORT_NAME_COMPARATOR)
                )
            }
        }
    }

    private val _routeColorInts: LiveData<List<Int>?> = routes.map { routes ->
        routes?.filter {
            it.hasColor()
        }?.map {
            it.colorInt
        }
    }

    val colorIntDistinct: LiveData<Int?> = PairMediatorLiveData(colorInt, _routeColorInts).map { (colorInt, routeColorInts) ->
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

    val showingListInsteadOfGrid: LiveData<Boolean> = PairMediatorLiveData(_authority, routes).switchMap { (authority, routes) ->
        liveData {
            routes?.let { routesNN ->
                authority?.let { authorityNN ->
                    emitSource(
                        defaultPrefRepository.pref.liveData(
                            DefaultPreferenceRepository.getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(authorityNN),
                            defaultPrefRepository.getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID_DEFAULT(routesNN.size)
                        )
                    )
                }
            }
        }
    }.distinctUntilChanged()

    fun saveShowingListInsteadOfGrid(showingListInsteadOfGrid: Boolean) {
        defaultPrefRepository.pref.edit {
            _authority.value?.let { authority ->
                putBoolean(DefaultPreferenceRepository.getPREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_GRID(authority), showingListInsteadOfGrid)
            }
        }
    }
}