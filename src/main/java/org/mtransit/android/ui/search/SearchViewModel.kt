package org.mtransit.android.ui.search

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.CollectionUtils
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.di.Injection
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.ui.view.common.PairMediatorLiveData

class SearchViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = SearchViewModel::class.java.simpleName

        const val EXTRA_QUERY = "extra_query"
        const val EXTRA_QUERY_DEFAULT = StringUtils.EMPTY
        const val EXTRA_TYPE_FILTER = "extra_type_filter"
        const val EXTRA_SEARCH_HAS_FOCUS = "extra_search_has_focus"
    }

    override fun getLogTag(): String = LOG_TAG

    private val dataSourcesRepository: DataSourcesRepository by lazy { Injection.providesDataSourcesRepository() }

    private val dataSourceRequestManager: DataSourceRequestManager by lazy { Injection.providesDataSourceRequestManager() }

    private val favoriteRepository: FavoriteRepository by lazy { Injection.providesFavoriteRepository() }

    val searchableDataSourceTypes: LiveData<List<DataSourceType>> = this.dataSourcesRepository.readingAllDataSourceTypesDistinct().map { list ->
        list.filter { dst -> dst.isSearchable }
    }

    private val _deviceLocation = MutableLiveData<Location?>()

    val deviceLocation: LiveData<Location?> = _deviceLocation

    fun onDeviceLocationChanged(newDeviceLocation: Location?) {
        newDeviceLocation?.let {
            val currentDeviceLocation = _deviceLocation.value
            if (currentDeviceLocation == null || LocationUtils.isMoreRelevant(logTag, currentDeviceLocation, it)) {
                _deviceLocation.value = it
            }
        }
    }

    private val _loading = MutableLiveData(false)

    val loading: LiveData<Boolean> = _loading

    private var searchJob: Job? = null

    fun onNewQuery(queryOrNull: String?) {
        val newQuery: String = queryOrNull.orEmpty()
        if (newQuery.trim() == this.query.value) {
            MTLog.d(this, "onNewQuery() > SKIP same value '$newQuery'.")
            return
        }
        searchJob?.cancel()
        if (newQuery.isNotBlank()) {
            _loading.value = true
        }
        searchJob = viewModelScope.launch {
            if (newQuery.isNotBlank()) {
                delay(777L) // debounce / throttle
            }
            setQuery(newQuery)
        }
    }

    private fun setQuery(query: String) {
        savedStateHandle[EXTRA_QUERY] = query.trim()
    }

    val query = savedStateHandle.getLiveData(EXTRA_QUERY, EXTRA_QUERY_DEFAULT).distinctUntilChanged()

    fun setTypeFilter(typeFilter: DataSourceType?) {
        setTypeFilterId(typeFilter?.id)
    }

    private fun setTypeFilterId(typeFilterId: Int?) {
        savedStateHandle[EXTRA_TYPE_FILTER] = typeFilterId
    }

    private val _typeFilterId = savedStateHandle.getLiveData<Int?>(EXTRA_TYPE_FILTER, null).distinctUntilChanged()

    val typeFilter: LiveData<DataSourceType?> = _typeFilterId.map { typeId ->
        typeId?.let { DataSourceType.parseId(typeId) }
    }

    fun setSearchHasFocus(focus: Boolean) {
        savedStateHandle[EXTRA_SEARCH_HAS_FOCUS] = focus
    }

    private val _searchHasFocus = savedStateHandle.getLiveData(EXTRA_SEARCH_HAS_FOCUS, true)

    val searchHasFocus: LiveData<Boolean> = _searchHasFocus

    private val _searchParameters = PairMediatorLiveData<String, Int?>(query, _typeFilterId)

    val searchResults: LiveData<List<POIManager>> =
        _searchParameters.switchMap { (query, typeFilterId) ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(getFilteredData(query, typeFilterId))
                _loading.postValue(false)
            }
        }


    private suspend fun getFilteredData(query: String?, typeFilterId: Int?): List<POIManager> {
        if (query.isNullOrBlank()) {
            MTLog.d(this, "getFilteredData() > SKIP (no query)")
            return emptyList()
        }
        val dataSourceTypes = this.dataSourcesRepository.getAllDataSourceTypes()
            .filter { it.isSearchable && (typeFilterId == null || typeFilterId == it.id) }
        if (dataSourceTypes.isEmpty()) {
            MTLog.d(this, "getFilteredData() > SKIP (no data source type)")
            return emptyList()
        }
        _loading.postValue(true)

        val keepAll = dataSourceTypes.size == 1

        val deviceLocation = _deviceLocation.value

        val favoriteUUIDs: Set<String> = favoriteRepository.findFavoriteUUIDs()
        val poiSearchComparator = POISearchComparator(favoriteUUIDs)

        val pois = mutableListOf<POIManager>()

        dataSourceTypes.forEach { dst ->
            pois.addAll(
                getFilteredDataType(dst, query, deviceLocation, keepAll, poiSearchComparator)
            )
        }

        return pois
    }

    private suspend fun getFilteredDataType(
        dst: DataSourceType,
        query: String,
        deviceLocation: Location?,
        keepAll: Boolean,
        poiSearchComparator: Comparator<POIManager?>
    ): List<POIManager> {

        val agencies = this.dataSourcesRepository.getTypeDataSources(dst)
        var typePois = mutableListOf<POIManager>()
        agencies.forEach { agency ->
            typePois.addAll(getFilteredDataTypeAgency(agency, query, deviceLocation))
        }
        LocationUtils.updateDistance(typePois, deviceLocation)
        CollectionUtils.sort(typePois, poiSearchComparator)
        if (!keepAll && typePois.size > 2) {
            typePois = typePois.subList(0, 2)
        }
        return typePois
    }

    private suspend fun getFilteredDataTypeAgency(
        agency: AgencyProperties,
        query: String,
        deviceLocation: Location?
    ): List<POIManager> {
        val poiFilter: POIProviderContract.Filter = POIProviderContract.Filter.getNewSearchFilter(query)
        poiFilter.addExtra(GTFSProviderContract.POI_FILTER_EXTRA_DESCENT_ONLY, true)
        if (deviceLocation != null) {
            poiFilter.addExtra("lat", deviceLocation.latitude)
            poiFilter.addExtra("lng", deviceLocation.longitude)
        }
        val pois: List<POIManager>
        withContext(Dispatchers.IO) {
            pois = dataSourceRequestManager.findPOIs(agency.authority, poiFilter).orEmpty()
        }
        return pois
    }

    class POISearchComparator(private val favoriteUUIDs: Set<String>) : Comparator<POIManager?> {
        override fun compare(lhs: POIManager?, rhs: POIManager?): Int {
            if (lhs == null && rhs == null) {
                return ComparatorUtils.SAME
            } else if (lhs == null) {
                return ComparatorUtils.AFTER
            } else if (rhs == null) {
                return ComparatorUtils.BEFORE
            }
            val lScore = if (lhs.poi.score == null) 0 else lhs.poi.score
            val rScore = if (rhs.poi.score == null) 0 else rhs.poi.score
            if (lScore > rScore) {
                return ComparatorUtils.BEFORE
            } else if (lScore < rScore) {
                return ComparatorUtils.AFTER
            }
            val lFav = favoriteUUIDs.contains(lhs.poi.uuid)
            val rFav = favoriteUUIDs.contains(rhs.poi.uuid)
            if (lFav && !rFav) {
                return ComparatorUtils.BEFORE
            } else if (!lFav && rFav) {
                return ComparatorUtils.AFTER
            }
            val ld = lhs.distance
            val rd = rhs.distance
            if (ld > rd) {
                return ComparatorUtils.AFTER
            } else if (ld < rd) {
                return ComparatorUtils.BEFORE
            }
            return ComparatorUtils.SAME
        }
    }
}