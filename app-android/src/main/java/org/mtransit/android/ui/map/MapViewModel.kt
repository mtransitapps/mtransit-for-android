package org.mtransit.android.ui.map

import android.app.PendingIntent
import android.content.pm.PackageManager
import android.location.Location
import androidx.collection.ArrayMap
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.common.repository.LocalPreferenceRepository
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
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.MediatorLiveData4
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.ui.view.map.MTMapIconDef
import org.mtransit.android.ui.view.map.MTMapIconsProvider.iconDefForRotation
import org.mtransit.android.ui.view.map.MTPOIMarker
import org.mtransit.android.util.containsEntirely
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class MapViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val adManager: IAdManager,
    private val pm: PackageManager,
) : MTViewModelWithLocation(),
    ModuleDisabledAwareViewModel,
    LocationSettingsAwareViewModel {

    companion object {
        private val LOG_TAG = MapViewModel::class.java.simpleName

        internal const val EXTRA_INITIAL_LOCATION = "extra_initial_location"
        internal const val EXTRA_SELECTED_UUID = "extra_selected_uuid"
        internal const val EXTRA_INCLUDE_TYPE_ID = "extra_include_type_id"
        internal const val EXTRA_INCLUDE_TYPE_ID_DEFAULT: Int = -1
    }

    override fun getLogTag(): String = LOG_TAG

    val initialLocation = savedStateHandle.getLiveDataDistinct<Location?>(EXTRA_INITIAL_LOCATION)

    fun onInitialLocationSet() {
        savedStateHandle[EXTRA_INITIAL_LOCATION] = null // set once only
    }

    override val locationSettingsNeededResolution: LiveData<PendingIntent?> =
        MediatorLiveData2(deviceLocation, locationSettingsResolution).map { (deviceLocation, resolution) ->
            if (deviceLocation != null) null else resolution
        } // .distinctUntilChanged() < DO NOT USE DISTINCT BECAUSE TOAST MIGHT NOT BE SHOWN THE 1ST TIME

    override val locationSettingsNeeded: LiveData<Boolean> = locationSettingsNeededResolution.map {
        it != null
    } // .distinctUntilChanged() < DO NOT USE DISTINCT BECAUSE TOAST MIGHT NOT BE SHOWN THE 1ST TIME

    override fun getAdBannerHeightInPx(activity: IAdScreenActivity?) = this.adManager.getBannerHeightInPx(activity)

    override val moduleDisabled = this.dataSourcesRepository.readingAllAgenciesBase().map {
        it.filter { agency -> !agency.isEnabled }
    }.distinctUntilChanged()

    override val hasDisabledModule = moduleDisabled.map {
        it.any { agency -> !pm.isAppEnabled(agency.pkg) }
    }

    val selectedUUID = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_SELECTED_UUID)

    fun onSelectedUUIDSet() {
        savedStateHandle[EXTRA_SELECTED_UUID] = null // set once only (then manage by map controller
    }

    private val allTypes = this.dataSourcesRepository.readingAllSupportedDataSourceTypes() // #onModulesUpdated

    val mapTypes = allTypes.map {
        it.filter { dst -> dst.isMapScreen }
    }

    private val includedTypeId = savedStateHandle.getLiveDataDistinct(EXTRA_INCLUDE_TYPE_ID, EXTRA_INCLUDE_TYPE_ID_DEFAULT)
        .map { if (it < 0) null else it }

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

    val filterTypeIds: LiveData<Collection<Int>?> =
        MediatorLiveData3(mapTypes, filterTypeIdsPref, includedTypeId).map { (mapTypes, filterTypeIdsPref, includedTypeId) ->
            makeFilterTypeId(mapTypes, filterTypeIdsPref, includedTypeId)
        }.distinctUntilChanged()

    private fun makeFilterTypeId(
        availableTypes: List<DataSourceType>?,
        filterTypeIdsPref: Set<String>?,
        inclTypeId: Int? = null,
    ): Collection<Int>? {
        if (filterTypeIdsPref == null || availableTypes == null) {
            MTLog.d(this, "makeFilterTypeId() > SKIP (no pref or available types")
            return null
        }
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
        inclTypeId?.let { includedTypeId ->
            if (filterTypeIds.isNotEmpty() && !filterTypeIds.contains(includedTypeId)) {
                prefHasChanged = try {
                    val type = DataSourceType.parseId(includedTypeId)
                    if (type == null) {
                        MTLog.d(this, "makeFilterTypeId() > included '$includedTypeId' not valid")
                        return@let // DO NOTHING
                    }
                    if (!availableTypes.contains(type)) {
                        MTLog.d(this, "makeFilterTypeId() > included '$includedTypeId' not available")
                        return@let  // DO NOTHING
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
    private val _loadingArea = MutableLiveData<LatLngBounds?>(null)

    fun resetLoadedPOIMarkers() {
        this._loadedArea.value = null // loaded w/ wrong filter -> RESET -> trigger new load
        this._poiMarkersReset.value = Event(true)
    }

    private val _allAgencies = this.dataSourcesRepository.readingAllAgenciesBase() // #onModulesUpdated

    val typeMapAgencies: LiveData<List<IAgencyNearbyUIProperties>?> = MediatorLiveData2(_allAgencies, filterTypeIds).map { (allAgencies, filterTypeIds) ->
        filterTypeIds?.let { theFilterTypeIds ->
            allAgencies?.filter { agency ->
                agency.getSupportedType().isMapScreen
                        && (theFilterTypeIds.isEmpty() || theFilterTypeIds.contains(agency.getSupportedType().id))
            }
        }
    }
    private val areaTypeMapAgencies: LiveData<List<IAgencyNearbyUIProperties>?> =
        MediatorLiveData2(typeMapAgencies, _loadingArea).map { (typeMapAgencies, loadingArea) ->
            loadingArea?.let { theLoadingArea -> // loading area REQUIRED
                typeMapAgencies?.filter { agency ->
                    agency.isInArea(theLoadingArea)
                }
            }
        }.distinctUntilChanged()

    val loaded: LiveData<Boolean?> = MediatorLiveData2(_loadingArea, _loadedArea).map { (loadingArea, loadedArea) ->
        loadedArea.containsEntirely(loadingArea)
    }

    fun onCameraChanged(newVisibleArea: LatLngBounds, getBigCameraPosition: () -> LatLngBounds?): Boolean {
        val loadedArea: LatLngBounds? = this._loadedArea.value
        val loadingArea: LatLngBounds? = this._loadingArea.value
        val loaded = loadedArea.containsEntirely(newVisibleArea)
        val loading = loadingArea.containsEntirely(newVisibleArea)
        if (loaded || loading) {
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

    private val _poiMarkersReset = MutableLiveData(Event(false))

    private val _poiMarkers = MutableLiveData<Collection<MTPOIMarker>?>(null)
    val poiMarkers: LiveData<Collection<MTPOIMarker>?> = _poiMarkers

    val poiMarkersTrigger: LiveData<Any?> =
        MediatorLiveData4(
            areaTypeMapAgencies,
            _loadedArea,
            _loadingArea,
            _poiMarkersReset
        ).map {
            loadPOIMarkers()
            null
        }

    private var poiMarkersLoadJob: Job? = null

    private fun loadPOIMarkers() {
        poiMarkersLoadJob?.cancel()
        val reset: Boolean = _poiMarkersReset.value?.getContentIfNotHandled() == true
        if (reset) {
            _poiMarkers.value = null
        }
        poiMarkersLoadJob = viewModelScope.launch(Dispatchers.IO) {
            val areaTypeMapAgencies: List<IAgencyNearbyUIProperties>? = areaTypeMapAgencies.value
            val loadedArea: LatLngBounds? = _loadedArea.value
            val loadingArea: LatLngBounds? = _loadingArea.value
            val currentPOIMarkers: Collection<MTPOIMarker>? = _poiMarkers.value
            if (loadingArea == null || areaTypeMapAgencies == null) {
                MTLog.d(this@MapViewModel, "loadPOIMarkers() > SKIP (no loading area OR agencies)")
                return@launch // SKIP (missing loading area or agencies)
            }
            val positionToPoiMarkers = ArrayMap<LatLng, MTPOIMarker>()
            var positionTrunc: LatLng
            if (!reset) {
                currentPOIMarkers?.forEach { poiMarker ->
                    positionTrunc = MTPOIMarker.getLatLngTrunc(poiMarker.position.latitude, poiMarker.position.longitude)
                    positionToPoiMarkers[positionTrunc] = positionToPoiMarkers[positionTrunc]?.apply {
                        merge(poiMarker)
                    } ?: poiMarker
                }
            }
            var hasChanged = false
            areaTypeMapAgencies.filter { agency ->
                !agency.isEntirelyInside(loadedArea)
            }.map { agency ->
                getAgencyPOIMarkers(agency, loadingArea, loadedArea, this).also {
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

    private suspend fun getAgencyPOIMarkers(
        agency: IAgencyNearbyUIProperties,
        loadingArea: LatLngBounds,
        loadedArea: LatLngBounds? = null,
        coroutineScope: CoroutineScope,
    ): ArrayMap<LatLng, MTPOIMarker> {
        val clusterItems = ArrayMap<LatLng, MTPOIMarker>()
        val poiFilter = POIProviderContract.Filter.getNewAreaFilter(
            loadingArea.let { min(it.northeast.latitude, it.southwest.latitude) }, // MIN LAT
            loadingArea.let { max(it.northeast.latitude, it.southwest.latitude) }, // MAX LAT
            loadingArea.let { min(it.northeast.longitude, it.southwest.longitude) }, // MIN LNG
            loadingArea.let { max(it.northeast.longitude, it.southwest.longitude) }, // MAX LNG
            loadedArea?.let { min(it.northeast.latitude, it.southwest.latitude) },
            loadedArea?.let { max(it.northeast.latitude, it.southwest.latitude) },
            loadedArea?.let { min(it.northeast.longitude, it.southwest.longitude) },
            loadedArea?.let { max(it.northeast.longitude, it.southwest.longitude) },
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
            iconDef = rotation.iconDefForRotation
            color = poim.getColor(dataSourcesRepository)
            secondaryColor = agency.colorInt
            clusterItems[positionTrunc] = clusterItems[positionTrunc]?.apply {
                merge(position, name, agencyShortName, extra, iconDef, color, secondaryColor, alpha, rotation, zIndex, uuid, authority)
            } ?: MTPOIMarker(position, name, agencyShortName, extra, iconDef, color, secondaryColor, alpha, rotation, zIndex, uuid, authority)
        }
        return clusterItems
    }
}