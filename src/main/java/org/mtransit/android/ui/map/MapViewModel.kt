package org.mtransit.android.ui.map

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
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyNearbyUIProperties
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.MapViewController.POIMarker
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.QuadrupleMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.util.containsEntirely
import java.util.HashSet
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class MapViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
) : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG = MapViewModel::class.java.simpleName

        internal const val EXTRA_INITIAL_LOCATION = "extra_initial_location"
        internal const val EXTRA_SELECTED_UUID = "extra_selected_uuid"
        internal const val EXTRA_INCLUDE_TYPE_ID = "extra_include_type_id"
    }

    override fun getLogTag(): String = LOG_TAG

    val initialLocation = savedStateHandle.getLiveDataDistinct<Location?>(EXTRA_INITIAL_LOCATION)

    fun onInitialLocationSet() {
        savedStateHandle[EXTRA_INITIAL_LOCATION] = null // set once only
    }

    val selectedUUID = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_SELECTED_UUID)

    fun onSelectedUUIDSet() {
        savedStateHandle[EXTRA_SELECTED_UUID] = null // set once only (then manage by map controller
    }

    private val allTypes = this.dataSourcesRepository.readingAllDataSourceTypes() // #onModulesUpdated

    val mapTypes = allTypes.map {
        it.filter { dst -> dst.isMapScreen }
    }

    private val includedTypeId = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_INCLUDE_TYPE_ID)

    private val filterTypeIdsPref: LiveData<Set<String>> = lclPrefRepository.pref.liveData(
        PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS, PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT
    ).distinctUntilChanged()


    fun saveFilterTypeIdsPref(filterTypeIds: Collection<Int>?) {
        val newFilterTypeIdStrings: Set<String> = filterTypeIds?.mapTo(HashSet()) { it.toString() }
            ?: PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS_DEFAULT // NULL = EMPTY = ALL (valid)
        lclPrefRepository.pref.edit {
            putStringSet(PreferenceUtils.PREFS_LCL_MAP_FILTER_TYPE_IDS, newFilterTypeIdStrings)
        }
    }

    val filterTypeIds: LiveData<Collection<Int>?> =
        TripleMediatorLiveData(mapTypes, filterTypeIdsPref, includedTypeId).map { (mapTypes, filterTypeIdsPref, includedTypeId) ->
            makeFilterTypeId(mapTypes, filterTypeIdsPref, includedTypeId)
        }.distinctUntilChanged()

    private fun makeFilterTypeId(
        availableTypes: List<DataSourceType>?,
        filterTypeIdsPref: Set<String>?,
        inclTypeId: Int?
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
            savedStateHandle[EXTRA_INCLUDE_TYPE_ID] = null // only once
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

    val typeMapAgencies: LiveData<List<IAgencyNearbyUIProperties>?> = PairMediatorLiveData(_allAgencies, filterTypeIds).map { (allAgencies, filterTypeIds) ->
        filterTypeIds?.let { theFilterTypeIds ->
            allAgencies?.filter { agency ->
                agency.type.isMapScreen
                        && (theFilterTypeIds.isEmpty() || theFilterTypeIds.contains(agency.type.id))
            }
        }
    }
    val areaTypeMapAgencies: LiveData<List<IAgencyNearbyUIProperties>?> =
        PairMediatorLiveData(typeMapAgencies, _loadingArea).map { (typeMapAgencies, loadingArea) ->
            loadingArea?.let { theLoadingArea -> // loading area REQUIRED
                typeMapAgencies?.filter { agency ->
                    agency.isInArea(theLoadingArea)
                }
            }
        }.distinctUntilChanged()

    val loaded: LiveData<Boolean?> = PairMediatorLiveData(_loadingArea, _loadedArea).map { (loadingArea, loadedArea) ->
        loadedArea.containsEntirely(loadingArea)
    }

    fun onCameraChange(newVisibleArea: LatLngBounds, getBigCameraPosition: () -> LatLngBounds?): Boolean {
        val loadedArea: LatLngBounds? = this._loadedArea.value
        val loadingArea: LatLngBounds? = this._loadingArea.value
        val loaded = loadedArea.containsEntirely(newVisibleArea)
        val loading = loadingArea.containsEntirely(newVisibleArea)
        if (loaded || loading) {
            return false // no change
        }
        var newLoadingArea = loadingArea?.let {
            if (!it.contains(newVisibleArea.northeast)) it.including(newVisibleArea.northeast) else it
        }
        newLoadingArea = newLoadingArea?.let {
            if (!it.contains(newVisibleArea.southwest)) it.including(newVisibleArea.southwest) else it
        }
        newLoadingArea = newLoadingArea ?: loadedArea?.let {
            if (!it.contains(newVisibleArea.northeast)) it.including(newVisibleArea.northeast) else it
        }
        newLoadingArea = newLoadingArea ?: loadedArea?.let {
            if (!it.contains(newVisibleArea.southwest)) it.including(newVisibleArea.southwest) else it
        }
        newLoadingArea = newLoadingArea ?: newVisibleArea.let {
            getBigCameraPosition() ?: it
        }
        this._loadingArea.value = newLoadingArea // set NOW (no post)
        return newLoadingArea != loadingArea // same area?
    }

    private val _poiMarkersReset = MutableLiveData(Event(false))

    private val _poiMarkers = MutableLiveData<Collection<POIMarker>?>(null)
    val poiMarkers: LiveData<Collection<POIMarker>?> = _poiMarkers

    val poiMarkersTrigger: LiveData<Any?> =
        QuadrupleMediatorLiveData(
            areaTypeMapAgencies,
            _loadedArea,
            _loadingArea,
            _poiMarkersReset
        ).map {
            loadPOIMarkers()
            null
        }

    private var poiMarkersLoadJob: Job? = null

    fun loadPOIMarkers() {
        poiMarkersLoadJob?.cancel()
        val reset: Boolean = _poiMarkersReset.value?.getContentIfNotHandled() ?: false
        if (reset) {
            _poiMarkers.value = null
        }
        poiMarkersLoadJob = viewModelScope.launch(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            val areaTypeMapAgencies: List<IAgencyNearbyUIProperties>? = areaTypeMapAgencies.value
            val loadedArea: LatLngBounds? = _loadedArea.value
            val loadingArea: LatLngBounds? = _loadingArea.value
            val currentPOIMarkers: Collection<POIMarker>? = _poiMarkers.value
            if (loadingArea == null || areaTypeMapAgencies == null) {
                MTLog.d(this@MapViewModel, "loadPOIMarkers() > SKIP (no loading area OR agencies)")
                return@launch // SKIP (missing loading area or agencies)
            }
            val positionToPoiMarkers = ArrayMap<LatLng, POIMarker>()
            var positionTrunc: LatLng
            if (!reset) {
                currentPOIMarkers?.forEach { poiMarker ->
                    positionTrunc = POIMarker.getLatLngTrunc(poiMarker.position.latitude, poiMarker.position.longitude)
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
                    if (!hasChanged && !it.isNullOrEmpty()) {
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

    private fun getAgencyPOIMarkers(
        agency: IAgencyNearbyUIProperties,
        loadingArea: LatLngBounds,
        loadedArea: LatLngBounds? = null,
        coroutineScope: CoroutineScope,
    ): ArrayMap<LatLng, POIMarker> {
        val clusterItems = ArrayMap<LatLng, POIMarker>()
        val poiFilter = POIProviderContract.Filter.getNewAreaFilter(
            loadingArea.let { min(it.northeast.latitude, it.southwest.latitude) },  // MIN LAT
            loadingArea.let { max(it.northeast.latitude, it.southwest.latitude) }, // MAX LAT
            loadingArea.let { min(it.northeast.longitude, it.southwest.longitude) },  // MIN LNG
            loadingArea.let { max(it.northeast.longitude, it.southwest.longitude) }, // MAX LNG
            loadedArea?.let { min(it.northeast.latitude, it.southwest.latitude) },
            loadedArea?.let { max(it.northeast.latitude, it.southwest.latitude) },
            loadedArea?.let { min(it.northeast.longitude, it.southwest.longitude) },
            loadedArea?.let { max(it.northeast.longitude, it.southwest.longitude) },
        )
        coroutineScope.ensureActive()
        val agencyPOIs = poiRepository.findPOIMs(agency.authority, poiFilter)
        val agencyShortName = agency.shortName
        var positionTrunc: LatLng
        var name: String
        var extra: String?
        var uuid: String
        var authority: String
        var color: Int?
        var secondaryColor: Int?
        agencyPOIs?.map {
            it to POIMarker.getLatLng(it)
        }?.filterNot { (_, position) ->
            !loadingArea.contains(position)
                    && loadedArea?.contains(position) == true
        }?.forEach { (poim, position) ->
            coroutineScope.ensureActive()
            positionTrunc = POIMarker.getLatLngTrunc(poim)
            name = poim.poi.name
            extra = (poim.poi as? RouteTripStop)?.route?.shortestName
            uuid = poim.poi.uuid
            authority = poim.poi.authority
            color = poim.getColor(dataSourcesRepository)
            secondaryColor = agency.colorInt
            clusterItems[positionTrunc] = clusterItems[positionTrunc]?.apply {
                merge(position, name, agencyShortName, extra, color, secondaryColor, uuid, authority)
            } ?: POIMarker(position, name, agencyShortName, extra, color, secondaryColor, uuid, authority)
        }
        return clusterItems
    }
}