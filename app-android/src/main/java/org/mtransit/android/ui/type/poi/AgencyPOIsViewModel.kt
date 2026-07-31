package org.mtransit.android.ui.type.poi

import androidx.lifecycle.LiveData
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
import kotlinx.coroutines.launch
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.user.UserPrefManager
import javax.inject.Inject

@HiltViewModel
class AgencyPOIsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val userPrefManager: UserPrefManager,
    private val demoModeManager: DemoModeManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = AgencyPOIsViewModel::class.java.simpleName

        internal const val EXTRA_AGENCY_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_COLOR_INT = "extra_color_int"
        internal const val EXTRA_SELECTED_MAP_CAMERA_POSITION_LAT = "extra_map_lat"
        internal const val EXTRA_SELECTED_MAP_CAMERA_POSITION_LNG = "extra_map_lng"
        internal const val EXTRA_SELECTED_MAP_CAMERA_POSITION_ZOOM = "extra_map_zoom"
        internal const val EXTRA_SELECTED_UUID = "extra_selected_uuid"
    }

    override fun getLogTag() = agency.value?.shortName?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    private val _authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AGENCY_AUTHORITY)

    val colorInt = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_COLOR_INT)

    private val _selectedMapCameraPositionLat = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_SELECTED_MAP_CAMERA_POSITION_LAT)
    private val _selectedMapCameraPositionLng = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_SELECTED_MAP_CAMERA_POSITION_LNG)
    private val _selectedMapCameraPositionZoom = savedStateHandle.getLiveDataDistinct<Float?>(EXTRA_SELECTED_MAP_CAMERA_POSITION_ZOOM)

    val selectedMapCameraPosition = MediatorLiveData3(_selectedMapCameraPositionLat, _selectedMapCameraPositionLng, _selectedMapCameraPositionZoom)
        .map { (lat, lng, zoom) ->
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

    val selectedUUID = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_SELECTED_UUID)

    fun onSelectedStopUuidSet() {
        savedStateHandle[EXTRA_SELECTED_UUID] = null
    }

    val agency: LiveData<AgencyProperties?> = this._authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgency(authority) // #onModulesUpdated
    }

    val poiList: LiveData<List<POIManager>> = agency.switchMap { agency ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            val agency = agency ?: return@liveData
            val poiFilter = POIProviderContract.Filter.getNewEmptyFilter()
            emit(poiRepository.findPOIMs(agency, poiFilter))
        }
    }

    val showingListInsteadOfMap: LiveData<Boolean> = _authority.switchMap { authority ->
        liveData {
            authority ?: return@liveData
            if (demoModeManager.isFullDemo()) {
                emit(false) // show map (demo mode ON)
                return@liveData
            }
            emitSource(
                userPrefManager.getAgencyPOIsShowingListInsteadOfMap(authority)
            )
        }
    }.distinctUntilChanged()

    fun saveShowingListInsteadOfMap(showingListInsteadOfMap: Boolean) {
        if (demoModeManager.isFullDemo()) return // SKIP (demo mode ON)
        viewModelScope.launch {
            userPrefManager.setAgencyPOIsShowingListInsteadOfMapAndLastSet(_authority.value, showingListInsteadOfMap)
        }
    }

    val useInternalWebBrowserPref: LiveData<Boolean> = userPrefManager.useInternalWebBrowser.distinctUntilChanged()
}
