package org.mtransit.android.ui.map

import android.app.PendingIntent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Location
import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import androidx.core.content.edit
import androidx.core.location.component1
import androidx.core.location.component2
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.ktx.utils.component1
import com.google.maps.android.ktx.utils.component2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.common.roundTo
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyNearbyUIProperties
import org.mtransit.android.data.latLng
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.inappnotification.locationsettings.LocationSettingsAwareViewModel
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareViewModel
import org.mtransit.android.ui.location.GeocoderManager
import org.mtransit.android.ui.location.UILocationUtils
import org.mtransit.android.ui.location.toAddressOrNull
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.ui.view.common.MediatorLiveData4
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.ui.view.map.MTMapIconDef
import org.mtransit.android.ui.view.map.MTMapIconsProvider.getIconDefForRotation
import org.mtransit.android.ui.view.map.MTPOIMarker
import org.mtransit.android.ui.view.map.toLngLngList
import org.mtransit.android.ui.view.map.toLocation
import org.mtransit.android.usecase.GetNearbyPOIListUseCase
import org.mtransit.android.util.containsEntirely
import org.mtransit.commons.sortWithAnd
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class MapViewModel @Inject constructor(
    private val geocoderManager: GeocoderManager,
    private val savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val adManager: IAdManager,
    getNearbyPOIListUseCase: GetNearbyPOIListUseCase,
    pm: PackageManager,
) : MTViewModelWithLocation(),
    ModuleDisabledAwareViewModel,
    LocationSettingsAwareViewModel {

    companion object {
        private val LOG_TAG: String = MapViewModel::class.java.simpleName

        internal const val EXTRA_INITIAL_LOCATION = "extra_initial_location"
        internal const val EXTRA_SELECTED_UUID = "extra_selected_uuid"
        internal const val EXTRA_INCLUDE_TYPE_ID = "extra_include_type_id"
        internal const val EXTRA_INCLUDE_TYPE_ID_DEFAULT = -1

        internal const val EXTRA_MAP_CAMERA_MOVED = "extra_map_camera_moved"
    }

    init {
        getNearbyPOIListUseCase.logTag = LOG_TAG
    }

    override fun getLogTag() = LOG_TAG

    val initialLocation = savedStateHandle.getLiveDataDistinct<Location?>(EXTRA_INITIAL_LOCATION)

    fun onInitialLocationSet() {
        savedStateHandle[EXTRA_INITIAL_LOCATION] = null // set once only
    }

    override val locationSettingsNeededResolution: LiveData<PendingIntent?> = MediatorLiveData2(deviceLocation, locationSettingsResolution)
        .map { (deviceLocation, resolution) ->
            resolution?.takeIf { deviceLocation == null }
        } // .distinctUntilChanged() < DO NOT USE DISTINCT BECAUSE TOAST MIGHT NOT BE SHOWN THE 1ST TIME

    override val locationSettingsNeeded: LiveData<Boolean> = locationSettingsNeededResolution.map {
        it != null
    } // .distinctUntilChanged() < DO NOT USE DISTINCT BECAUSE TOAST MIGHT NOT BE SHOWN THE 1ST TIME

    override fun getAdBannerHeightInPx(activity: IAdScreenActivity?) = this.adManager.getBannerHeightInPx(activity)

    override val moduleDisabled = this.dataSourcesRepository.readingAllAgenciesBase()
        .map {
            it.filter { agency -> !agency.isEnabled }
        }.distinctUntilChanged()

    override val hasDisabledModule = moduleDisabled.map {
        it.any { agency -> !pm.isAppEnabled(agency.pkg) }
    }

    private val _selectedLocation = MutableLiveData<LatLng?>()
    val selectedLocation: LiveData<LatLng?> = _selectedLocation

    private val loadingSelectedLocationAddress = MutableLiveData<Boolean>()
    private val _selectedAddress = MutableLiveData<Address?>()
    val selectedAddress: LiveData<Address?> = _selectedAddress

    fun onLocationSelected(selectedLocation: LatLng) {
        _selectedLocation.postValue(selectedLocation)
        loadSelectedLocationAddress(selectedLocation)
    }

    private var loadSelectedLocationAddressJob: Job? = null

    private fun loadSelectedLocationAddress(selectedLocation: LatLng) {
        loadSelectedLocationAddressJob?.cancel()
        loadSelectedLocationAddressJob = viewModelScope.launch(Dispatchers.IO) {
            loadingSelectedLocationAddress.postValue(true)
            val selectedAddress = selectedLocation.toLocation().toAddressOrNull(geocoderManager)
            _selectedAddress.postValue(selectedAddress)
            loadingSelectedLocationAddress.postValue(false)
        }
    }

    val selectedUUID = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_SELECTED_UUID)

    fun onSelectedUUIDSet() {
        savedStateHandle[EXTRA_SELECTED_UUID] = null // set once only (then manage by map controller
    }

    private val allTypes = this.dataSourcesRepository.readingAllSupportedDataSourceTypes() // #onModulesUpdated

    val mapTypes = allTypes.map {
        it.filter { dst -> dst.isMapScreen }
    }

    private val includedTypeIdOrDefault = savedStateHandle.getLiveDataDistinct(EXTRA_INCLUDE_TYPE_ID, EXTRA_INCLUDE_TYPE_ID_DEFAULT)

    private val filterTypeIdsPref: LiveData<Set<String>> = lclPrefRepository.pref.liveData(
        LocalPreferenceRepository.PREFS_LCL_MAP_FILTER_TYPE_IDS, LocalPreferenceRepository.PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT
    ).distinctUntilChanged()

    fun saveFilterTypeIdsPref(filterTypeIds: Collection<Int>?) {
        val newFilterTypeIdStrings: Set<String> = filterTypeIds?.mapTo(HashSet()) { it.toString() }
            ?: LocalPreferenceRepository.PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT // NULL = EMPTY = ALL (valid)
        lclPrefRepository.pref.edit {
            putStringSet(LocalPreferenceRepository.PREFS_LCL_MAP_FILTER_TYPE_IDS, newFilterTypeIdStrings)
        }
    }

    val filterTypeIds: LiveData<Collection<Int>?> = MediatorLiveData3(mapTypes, filterTypeIdsPref, includedTypeIdOrDefault)
        .map { (mapTypes, filterTypeIdsPref, includedTypeIdOrDefault) ->
            mapTypes ?: return@map null
            filterTypeIdsPref ?: return@map null
            includedTypeIdOrDefault ?: return@map null
            makeFilterTypeId(mapTypes, filterTypeIdsPref, includedTypeIdOrDefault)
        }.distinctUntilChanged()

    private fun makeFilterTypeId(
        availableTypes: List<DataSourceType>,
        filterTypeIdsPref: Set<String>,
        includedTypeIdOrDefault: Int,
    ): Collection<Int> {
        val filterTypeIds = mutableSetOf<Int>()
        var prefHasChanged = false
        filterTypeIdsPref.forEach { typeIdString ->
            try {
                val type = DataSourceType.parseId(typeIdString.toInt())
                if (type == null) {
                    MTLog.d(this, "makeFilterTypeId() > '$typeIdString' not valid")
                    prefHasChanged = true
                    return@forEach
                }
                if (!availableTypes.contains(type)) {
                    MTLog.d(this, "makeFilterTypeId() > '$type' not available (in map screen)")
                    prefHasChanged = true
                    return@forEach
                }
                filterTypeIds.add(type.id)
            } catch (e: Exception) {
                MTLog.w(this, e, "Error while parsing filter type ID '%s'!", typeIdString)
                prefHasChanged = true
            }
        }
        includedTypeIdOrDefault.takeIf { it != EXTRA_INCLUDE_TYPE_ID_DEFAULT }?.let { includedTypeId ->
            if (filterTypeIds.isNotEmpty() && !filterTypeIds.contains(includedTypeId)) {
                prefHasChanged = try {
                    val type = DataSourceType.parseId(includedTypeId)
                    if (type == null) {
                        MTLog.d(this, "makeFilterTypeId() > included '$includedTypeId' not valid")
                        return@let // DO NOTHING
                    }
                    if (!availableTypes.contains(type)) {
                        MTLog.d(this, "makeFilterTypeId() > included '$includedTypeId' not available")
                        return@let // DO NOTHING
                    }
                    filterTypeIds.add(type.id)
                    true
                } catch (e: java.lang.Exception) {
                    MTLog.w(this, e, "Error while parsing filter type ID '%s'!", includedTypeId)
                    true
                }
            }
            savedStateHandle[EXTRA_INCLUDE_TYPE_ID] = EXTRA_INCLUDE_TYPE_ID_DEFAULT // only once
        }
        if (prefHasChanged) { // old setting not valid anymore
            saveFilterTypeIdsPref(if (filterTypeIds.size == availableTypes.size) null else filterTypeIds) // asynchronous
        }
        return filterTypeIds
    }

    private val _loadedArea = MutableLiveData<LatLngBounds?>(null)
    private val _loadingArea = MutableLiveData<LatLngBounds>()

    @MainThread
    fun resetLoadedPOIMarkers() {
        this._loadedArea.value = null // loaded w/ wrong filter -> RESET -> trigger new load
        this._poiMarkersReset.value = Event(true)
    }

    private val allAgencies = this.dataSourcesRepository.readingAllAgenciesBase() // #onModulesUpdated

    private val mapCameraMoved = savedStateHandle.getLiveDataDistinct(EXTRA_MAP_CAMERA_MOVED, false)

    private fun onMapCameraMoved() {
        savedStateHandle[EXTRA_MAP_CAMERA_MOVED] = true
    }

    private val stableDeviceLatLng: LiveData<LatLng?> = deviceLocation.map {
        it?.let { (lat, lng) -> LatLng(lat.roundTo(3), lng.roundTo(3)) }
    }.distinctUntilChanged()

    val initialVisibleArea: LiveData<Collection<LatLng>?> = MediatorLiveData3(mapCameraMoved, stableDeviceLatLng, allAgencies)
        .switchMap { (mapCameraMoved, deviceLocation, allAgencies) ->
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                mapCameraMoved ?: return@liveData
                val (deviceLat, deviceLng) = deviceLocation ?: return@liveData
                val allAgencies = allAgencies ?: return@liveData
                val nearbyPOILatLng = getNearbyPOIListUseCase(
                    lat = deviceLat,
                    lng = deviceLng,
                    allAgencies = allAgencies,
                    minSize = 1,
                    maxSize = 1,
                    minCoverageInMeters = UILocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS,
                    enoughCoverageInMeters = UILocationUtils.MAX_NEARBY_RELEVANT_COVERAGE_IN_METERS,
                    excludeAgency = { agency ->
                        !agency.type.isMapScreen
                            || agency.type == DataSourceType.TYPE_MODULE
                    },
                    excludePOI = { false },
                )
                    .sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR)
                    .firstOrNull()
                    ?.latLng
                    ?: run {
                        MTLog.d(this@MapViewModel, "initialVisibleArea.onChanged() > SKIP (no POI nearby)")
                        emit(emptyList())
                        return@liveData
                    }
                val visibleArea = UILocationUtils.computeArea(LatLng(deviceLat, deviceLng), nearbyPOILatLng).toLngLngList()
                emit(visibleArea)
            }
        }

    val filteredTypeAgencies: LiveData<List<IAgencyNearbyUIProperties>?> = MediatorLiveData2(allAgencies, filterTypeIds)
        .map { (allAgencies, filterTypeIds) ->
            filterTypeIds ?: return@map null
            allAgencies ?: return@map null
            allAgencies.filter { agency ->
                agency.getSupportedType().isMapScreen
                    && (filterTypeIds.isEmpty() || filterTypeIds.contains(agency.getSupportedType().id))
            }
        }

    private val loadingAreaAgencies: LiveData<List<IAgencyNearbyUIProperties>?> = MediatorLiveData2(filteredTypeAgencies, _loadingArea)
        .map { (filteredTypeAgencies, loadingArea) ->
            loadingArea ?: return@map null // loading area REQUIRED
            filteredTypeAgencies ?: return@map null
            filteredTypeAgencies.filter { agency ->
                agency.isInArea(loadingArea)
            }
        }.distinctUntilChanged()

    val loading: LiveData<Boolean> = MediatorLiveData3(_loadingArea, _loadedArea, loadingSelectedLocationAddress)
        .map { (loadingArea, loadedArea, loadingSelectedLocationAddress) ->
            loadingSelectedLocationAddress == true || !loadedArea.containsEntirely(loadingArea)
        }

    @MainThread
    fun onCameraChanged(newVisibleArea: LatLngBounds, getBigCameraPosition: () -> LatLngBounds?): Boolean {
        onMapCameraMoved()
        val loadedArea = this._loadedArea.value
        val loadingArea = this._loadingArea.value
        val loaded = loadedArea.containsEntirely(newVisibleArea)
        val loading = loadingArea.containsEntirely(newVisibleArea)
        if (loaded || loading) {
            MTLog.d(this, "onCameraChanged() > SKIP (no change)")
            return false // no change
        }
        var newLoadingArea: LatLngBounds = getBigCameraPosition() ?: newVisibleArea
        loadingArea?.apply {
            newLoadingArea = newLoadingArea.including(southwest).including(northeast)
        }
        loadedArea?.apply {
            newLoadingArea = newLoadingArea.including(southwest).including(northeast)
        }
        this._loadingArea.value = newLoadingArea // set NOW (no post)
        return newLoadingArea != loadingArea // same area?
    }

    private val _poiMarkersReset = MutableLiveData<Event<Boolean>>()

    private val _poiMarkers = MutableLiveData<Collection<MTPOIMarker>?>(null)
    val poiMarkers: LiveData<Collection<MTPOIMarker>?> = _poiMarkers

    val poiMarkersTrigger: LiveData<Any?> = MediatorLiveData4(loadingAreaAgencies, _loadedArea, _loadingArea, _poiMarkersReset)
        .map { (loadingAreaAgencies, loadedArea, loadingArea, poiMarkersResetEvent) ->
            loadingAreaAgencies ?: return@map null
            loadingArea ?: return@map null
            val poiMarkersReset = poiMarkersResetEvent?.getContentIfNotHandled()
            loadPOIMarkers(loadingAreaAgencies, loadingArea, loadedArea, poiMarkersReset)
            null
        }

    private var poiMarkersLoadJob: Job? = null

    @MainThread
    private fun loadPOIMarkers(
        loadingAreaAgencies: List<IAgencyNearbyUIProperties>,
        loadingArea: LatLngBounds,
        loadedArea: LatLngBounds?,
        poiMarkersReset: Boolean?,
    ) {
        poiMarkersLoadJob?.cancel()
        val reset = poiMarkersReset == true
        if (reset) {
            _poiMarkers.value = null
        if (loadedArea == loadingArea) {
            MTLog.d(this@MapViewModel, "loadPOIMarkers() > SKIP (loading area already loaded)")
            return
        }
        poiMarkersLoadJob = viewModelScope.launch(Dispatchers.IO) {
            val positionToPoiMarkers = ArrayMap<LatLng, MTPOIMarker>()
            var positionTrunc: LatLng
            if (!reset) {
                _poiMarkers.value?.forEach { currentPOIMarker ->
                    positionTrunc = MTPOIMarker.getLatLngTrunc(currentPOIMarker.position.latitude, currentPOIMarker.position.longitude)
                    positionToPoiMarkers[positionTrunc] = positionToPoiMarkers[positionTrunc]?.apply {
                        merge(currentPOIMarker)
                    } ?: currentPOIMarker
                }
            }
            var hasChanged = false
            loadingAreaAgencies.filterNot { loadingAreaAgency ->
                loadingAreaAgency.isEntirelyInside(loadedArea) // ignore agencies already entirely loaded
            }.map { agency ->
                ensureActive()
                findAgencyPOIMarkers(agency, loadingArea, loadedArea, this).also {
                    if (!hasChanged && it.isNotEmpty()) {
                        hasChanged = true
                    }
                }
            }.forEach { agencyPOIMarkers ->
                ensureActive()
                agencyPOIMarkers.forEach { (positionTrunc, poiMarker) ->
                    positionToPoiMarkers[positionTrunc] = positionToPoiMarkers[positionTrunc]?.apply {
                        merge(poiMarker)
                    } ?: poiMarker
                }
            }
            ensureActive()
            if (loadedArea != loadingArea) {
                _loadedArea.postValue(loadingArea) // LOADED DONE
            }
            if (hasChanged) {
                _poiMarkers.postValue(positionToPoiMarkers.values)
            }
        }
    }

    private suspend fun findAgencyPOIMarkers(
        agency: IAgencyNearbyUIProperties,
        loadingArea: LatLngBounds,
        loadedArea: LatLngBounds? = null,
        coroutineScope: CoroutineScope,
    ): ArrayMap<LatLng, MTPOIMarker> {
        val clusterItems = ArrayMap<LatLng, MTPOIMarker>()
        val poiFilter = POIProviderContract.Filter.getNewAreaFilter(
            minLat = loadingArea.let { min(it.northeast.latitude, it.southwest.latitude) },
            maxLat = loadingArea.let { max(it.northeast.latitude, it.southwest.latitude) },
            minLng = loadingArea.let { min(it.northeast.longitude, it.southwest.longitude) },
            maxLng = loadingArea.let { max(it.northeast.longitude, it.southwest.longitude) },
            optLoadedMinLat = loadedArea?.let { min(it.northeast.latitude, it.southwest.latitude) },
            optLoadedMaxLat = loadedArea?.let { max(it.northeast.latitude, it.southwest.latitude) },
            optLoadedMinLng = loadedArea?.let { min(it.northeast.longitude, it.southwest.longitude) },
            optLoadedMaxLng = loadedArea?.let { max(it.northeast.longitude, it.southwest.longitude) },
        )
        coroutineScope.ensureActive()
        val agencyPOIs = poiRepository.findPOIMs(agency, poiFilter)
        val agencyShortName = agency.shortName
        var positionTrunc: LatLng
        var name: String
        var extra: String?
        var uuid: String
        var authority: String
        var iconDef: MTMapIconDef
        var color: Int?
        val alpha: Float? = null
        val rotation: Float? = null
        val zIndex: Float? = null
        var secondaryColor: Int?
        agencyPOIs.mapNotNull { poim ->
            poim.latLng?.let { poim to it }
        }.filterNot { (_, position) ->
            !loadingArea.contains(position)
                && loadedArea?.contains(position) == true
        }.forEach { (poim, position) ->
            coroutineScope.ensureActive()
            positionTrunc = MTPOIMarker.getLatLngTrunc(poim)
            name = poim.poi.name
            extra = (poim.poi as? RouteDirectionStop)?.route?.shortestName
            uuid = poim.poi.uuid
            authority = poim.poi.authority
            iconDef = getIconDefForRotation(rotation, poim.poi)
            color = poim.getColor(dataSourcesRepository)
            secondaryColor = agency.colorInt
            clusterItems[positionTrunc] = clusterItems[positionTrunc]?.apply {
                merge(position, name, agencyShortName, extra, iconDef, color, secondaryColor, alpha, rotation, zIndex, uuid, authority)
            } ?: MTPOIMarker(position, name, agencyShortName, extra, iconDef, color, secondaryColor, alpha, rotation, zIndex, uuid, authority)
        }
        return clusterItems
    }
}
