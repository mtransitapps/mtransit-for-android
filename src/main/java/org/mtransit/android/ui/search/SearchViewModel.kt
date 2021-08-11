package org.mtransit.android.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val favoriteRepository: FavoriteRepository,
) : MTViewModelWithLocation(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = SearchViewModel::class.java.simpleName

        internal const val EXTRA_QUERY = "extra_query"
        internal const val EXTRA_QUERY_DEFAULT = StringUtils.EMPTY
        internal const val EXTRA_TYPE_FILTER = "extra_type_filter"
        internal const val EXTRA_SEARCH_HAS_FOCUS = "extra_search_has_focus"
    }

    fun onScreenVisible() {
        refreshFavorites()
    }

    private fun refreshFavorites() {
        viewModelScope.launch {
            favoriteUUIDs = favoriteRepository.findFavoriteUUIDs()
        }
    }

    override fun getLogTag(): String = LOG_TAG

    val searchableDataSourceTypes: LiveData<List<DataSourceType>> = this.dataSourcesRepository.readingAllDataSourceTypes().map { list ->
        list.filter { dst -> dst.isSearchable }
    }

    private val _searchableAgencies: LiveData<List<IAgencyProperties>> = this.dataSourcesRepository.readingAllAgenciesBase().map { list ->
        list.filter { agency -> agency.type.isSearchable }
    }

    private val _loading = MutableLiveData(false)

    val loading: LiveData<Boolean> = _loading

    private var searchJob: Job? = null

    private var favoriteUUIDs = emptySet<String>()

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

    val query = savedStateHandle.getLiveDataDistinct(EXTRA_QUERY, EXTRA_QUERY_DEFAULT)

    fun setTypeFilter(typeFilter: DataSourceType?) {
        setTypeFilterId(typeFilter?.id)
    }

    private fun setTypeFilterId(typeFilterId: Int?) {
        savedStateHandle[EXTRA_TYPE_FILTER] = typeFilterId
    }

    private val _typeFilterId = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_TYPE_FILTER)

    val typeFilter: LiveData<DataSourceType?> = _typeFilterId.map { typeId ->
        typeId?.let { DataSourceType.parseId(typeId) }
    }

    fun setSearchHasFocus(focus: Boolean) {
        savedStateHandle[EXTRA_SEARCH_HAS_FOCUS] = focus
    }

    private val _searchHasFocus = savedStateHandle.getLiveDataDistinct(EXTRA_SEARCH_HAS_FOCUS, true)

    val searchHasFocus: LiveData<Boolean> = _searchHasFocus

    val searchResults: LiveData<List<POIManager>?> =
        TripleMediatorLiveData(query, _typeFilterId, _searchableAgencies).switchMap { (query, typeFilterId, searchableAgencies) ->
            var keepAll = false
            this.poiRepository.loadingPOIMs(
                typeToProviders = searchableAgencies
                    ?.filter { agency -> typeFilterId == null || typeFilterId == agency.type.id }
                    ?.groupBy { it.type }
                    ?.toSortedMap(this.dataSourcesRepository.defaultDataSourceTypeComparator)
                    ?.also { typeToAgencies ->
                        keepAll = typeToAgencies.keys.size == 1
                    },
                filter = if (query.isNullOrBlank()) null else {
                    POIProviderContract.Filter.getNewSearchFilter(query).apply {
                        addExtra(GTFSProviderContract.POI_FILTER_EXTRA_DESCENT_ONLY, true)
                        deviceLocation.value?.let {
                            addExtra("lat", it.latitude)
                            addExtra("lng", it.longitude)
                        }
                    }
                },
                deviceLocation = deviceLocation.value,
                typeComparator = POISearchComparator(this.favoriteUUIDs),
                typeLet = { typePois ->
                    if (keepAll || typePois.size <= 2) {
                        typePois
                    } else {
                        typePois.take(2)
                    }
                },
                onSuccess = {
                    _loading.postValue(false)
                },
                context = viewModelScope.coroutineContext + Dispatchers.IO,
            )
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