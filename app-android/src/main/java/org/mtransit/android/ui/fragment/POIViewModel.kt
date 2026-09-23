package org.mtransit.android.ui.fragment

import androidx.collection.SimpleArrayMap
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
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProviderContract
import org.mtransit.android.commons.updateDistanceM
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.Favorite
import org.mtransit.android.data.POIConnectionComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.data.VehicleLocationProviderProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.NewsRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.usecase.GetNearbyPOIListUseCase
import org.mtransit.android.user.UserPrefManager
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.sortWithAnd
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class POIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    newsRepository: NewsRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    dataSourceRequestManager: DataSourceRequestManager,
    favoriteRepository: FavoriteRepository,
    userPrefManager: UserPrefManager,
    remoteConfigProvider: RemoteConfigProvider,
    private val getNearbyPOIListUseCase: GetNearbyPOIListUseCase,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = POIViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_POI_UUID = "extra_poi_uuid"

        private const val NEARBY_CONNECTIONS_MAX_COVERAGE = 2f * LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS
        private const val NEARBY_CONNECTIONS_MAX_COVERAGE_BIKE = 250f
    }

    override fun getLogTag() = LOG_TAG

    val uuid = savedStateHandle.getLiveDataDistinct<String>(EXTRA_POI_UUID)

    private val _authority = savedStateHandle.getLiveDataDistinct<String>(EXTRA_AUTHORITY)

    val poiAgency: LiveData<AgencyProperties?> = this._authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgency(authority) // #onModulesUpdated // UPDATE-ABLE
    }

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    val distanceUnitsPref: LiveData<String> = userPrefManager.distanceUnits.distinctUntilChanged()

    val useInternalWebBrowserPref: LiveData<Boolean> = userPrefManager.useInternalWebBrowser.distinctUntilChanged()

    val poim: LiveData<POIManager?> = MediatorLiveData2(poiAgency, uuid)
        .switchMap { (agency, uuid) -> // #onModulesUpdated
            poiRepository.readingPOIM(agency, uuid, currentValue = poim.value, onDataSourceRemoved = {
                dataSourceRemovedEvent.postValue(Event(true))
            })
        }

    private val _poi = this.poim.map {
        it?.poi
    }.distinctUntilChanged()

    private val _rds = _poi.map {
        it as? RouteDirectionStop
    }

    private val _vehicleLocationProviders: LiveData<List<VehicleLocationProviderProperties>> = _authority.switchMap {
        if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return@switchMap null
        dataSourcesRepository.readingVehicleLocationProviders(it) // #onModulesUpdated
    }

    private val _vehicleLocationRequestedTrigger = MutableLiveData<Int?>(null) // no initial value to avoid triggering onChanged()

    private var _vehicleRefreshJob: Job? = null

    private val _vehicleLocationDataRefreshMinMs = remoteConfigProvider.get(
        RemoteConfigProvider.VEHICLE_LOCATION_DATA_REFRESH_MIN_MS,
        RemoteConfigProvider.VEHICLE_LOCATION_DATA_REFRESH_MIN_MS_DEFAULT,
    ).milliseconds

    fun startVehicleLocationRefresh() {
        if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return
        _vehicleRefreshJob?.cancel()
        _vehicleRefreshJob = viewModelScope.launch {
            while (true) {
                _vehicleLocationRequestedTrigger.value = (_vehicleLocationRequestedTrigger.value ?: 0) + 1
                delay(_vehicleLocationDataRefreshMinMs)
            }
        }
    }

    fun stopVehicleLocationRefresh() {
        if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return
        _vehicleLocationRequestedTrigger.value = null // disable when not visible
        _vehicleRefreshJob?.cancel()
        _vehicleRefreshJob = null
    }

    val vehicleLocations = MediatorLiveData3(_vehicleLocationProviders, _rds, _vehicleLocationRequestedTrigger)
        .switchMap { (vehicleLocationProviders, rds, trigger) ->
            liveData(viewModelScope.coroutineContext) {
                if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return@liveData
                vehicleLocationProviders ?: return@liveData
                rds ?: return@liveData
                trigger ?: return@liveData // skip when not visible
                emit(
                    vehicleLocationProviders.mapNotNull {
                        val filter = VehicleLocationProviderContract.Filter(rds).copy(inFocus = true)
                        dataSourceRequestManager.findRDSVehicleLocations(it, filter)
                    }.flatten()
                )
            }
        }.distinctUntilChanged()

    val poiList: LiveData<List<POIManager>?> = MediatorLiveData2(poiAgency, _poi)
        .switchMap { (agency, poi) ->
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                agency ?: return@liveData
                poi ?: return@liveData
                emit(
                    poiRepository.findPOIMs(agency, getFilter(poi))
                        .apply {
                            if (poi !is RouteDirectionStop) {
                                updateDistanceM(poi.lat, poi.lng)
                                sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR)
                            }
                        }
                )
            }
        }

    private fun getFilter(poi: POI): POIProviderContract.Filter = when (poi) {
        is RouteDirectionStop -> POIProviderContract.Filter.getNewSqlSelectionFilter(
            SqlUtils.getWhereEquals(
                GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ID, poi.direction.id
            )
        ).copy(
            extras = SimpleArrayMap<String, Any>().apply {
                put(
                    POIProviderContract.POI_FILTER_EXTRA_SORT_ORDER,
                    SqlUtils.getSortOrderAscending(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_STOP_SEQUENCE)
                )
            },
        )

        else -> POIProviderContract.Filter.getNewEmptyFilter()
    }

    private val _scheduleProviders: LiveData<List<ScheduleProviderProperties>> = _authority.switchMap { authority ->
        this.dataSourcesRepository.readingScheduleProviders(authority)
    }

    val hasScheduleProviders = _scheduleProviders.map { it.isNotEmpty() }

    private val _allAgencies = this.dataSourcesRepository.readingAllAgenciesBase() // #onModulesUpdated

    // like Home screen (no infinite loading like in Nearby screen)
    val nearbyPOIs: LiveData<List<POIManager>?> = MediatorLiveData3(_allAgencies, poiAgency, _poi)
        .switchMap { (allAgencies, poiAgency, poi) ->
            allAgencies ?: return@switchMap null
            poiAgency ?: return@switchMap null
            poi ?: return@switchMap null
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                if (Constants.FORCE_NEARBY_POI_LIST_OFF) {
                    MTLog.d(this, "getNearbyPOIs() > SKIP (feature disabled)")
                    emit(emptyList())
                    return@liveData
                }
                emit(
                    getNearbyPOIListUseCase(
                        lat = poi.lat,
                        lng = poi.lng,
                        allAgencies = allAgencies,
                        maxSize = LocationUtils.MAX_POI_NEARBY_POIS_LIST,
                        minCoverageInMeters = LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS,
                        getMaxDistanceInMeters = { maxDistanceInMeters, dst ->
                            when (dst) {
                                DataSourceType.TYPE_BUS -> maxDistanceInMeters
                                DataSourceType.TYPE_BIKE -> maxDistanceInMeters * 1.5f
                                DataSourceType.TYPE_SUBWAY -> maxDistanceInMeters * 2f
                                DataSourceType.TYPE_RAIL -> maxDistanceInMeters * 2f
                                DataSourceType.TYPE_LIGHT_RAIL -> maxDistanceInMeters * 2f
                                DataSourceType.TYPE_FERRY -> maxDistanceInMeters * 2f
                                else -> {
                                    MTLog.w(this, "Unexpected type $dst in POI nearby agencies!")
                                    maxDistanceInMeters
                                }
                            }.coerceAtMost(NEARBY_CONNECTIONS_MAX_COVERAGE * 2f)
                        },
                        mainAgency = poiAgency,
                        excludedUUID = poi.uuid,
                        excludedRouteId = (poi as? RouteDirectionStop)?.route?.id,
                    ).apply {
                        poiConnectionComparator.targetedPOI = poi
                        sortWithAnd(poiConnectionComparator)
                    })
            }
        }

    private val poiConnectionComparator by lazy {
        POIConnectionComparator({ dataSourceTypeId ->
            when (dataSourceTypeId) {
                DataSourceType.TYPE_BIKE.id -> NEARBY_CONNECTIONS_MAX_COVERAGE_BIKE
                else -> NEARBY_CONNECTIONS_MAX_COVERAGE
            }
        })
    }

    private val _newsProviders = _authority.switchMap {
        dataSourcesRepository.readingNewsProviders(it) // #onModulesUpdated
    }

    val latestNewsArticleList: LiveData<List<News>?> = MediatorLiveData2(_poi, _newsProviders)
        .switchMap { (poi, newsProviders) ->
            newsRepository.loadingNewsArticles(
                newsProviders,
                poi,
                News.NEWS_SEVERITY_COMPARATOR,
                firstLoad = latestNewsArticleList.value == null,
                { allNews ->
                    val nowInMs = UITimeUtils.currentTimeMillis()
                    val selectedNews = mutableListOf<News>()
                    val minSelectedArticles = min(2, allNews.size) // encourage 2+ articles
                    val maxSelectedArticles = max(5, minSelectedArticles)
                    var noteworthiness = 1L
                    while (selectedNews.size < minSelectedArticles
                        && noteworthiness < 13L
                    ) {
                        for (news in allNews) {
                            val validityInMs: Long = news.createdAtInMs + news.noteworthyInMs * noteworthiness
                            if (validityInMs < nowInMs) {
                                continue // news too old to be worthy
                            }
                            if (!selectedNews.contains(news)) {
                                selectedNews.add(news)
                            }
                            if (selectedNews.size >= maxSelectedArticles) {
                                break // found enough news article
                            }
                        }
                        noteworthiness++
                    }
                    return@loadingNewsArticles selectedNews
                },
                coroutineContext = viewModelScope.coroutineContext + Dispatchers.IO,
            )
        }

    fun onBatteryOptimizationSettingsOpened() {
        lclPrefRepository.pref.edit {
            putBoolean(LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED, false) // click on the message once, show again next module disabled
        }
    }

    val hasSeenDisabledModule: LiveData<Boolean> = lclPrefRepository.pref.liveData(
        LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED,
        LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED_DEFAULT
    )

    fun refreshAppUpdateAvailable() {
        val agencyPkg = this.poiAgency.value?.pkg ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dataSourcesRepository.refreshAvailableVersions(forcePkg = agencyPkg)
        }
    }

    private val _favorite: LiveData<Favorite?> = MediatorLiveData2(this.uuid, favoriteRepository.readingAllFavoritesChange)
        .switchMap { (uuid, trigger) ->
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                uuid ?: return@liveData
                val favorite = favoriteRepository.getFavorite(uuid)
                emit(favorite)
                favorite?.let { emitSource(favoriteRepository.getReadingFavoriteById(favorite.id)) }
            }
        }

    val isFavorite: LiveData<Boolean> = _favorite.map { it != null }

    val usingFavoriteFolders: LiveData<Boolean> = favoriteRepository.isUsingFolders
}
