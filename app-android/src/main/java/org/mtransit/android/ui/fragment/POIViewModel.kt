package org.mtransit.android.ui.fragment

import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.removeTooMuchWhenNotInCoverage
import org.mtransit.android.commons.updateDistanceM
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIAlphaComparator
import org.mtransit.android.data.POIConnectionComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.data.shortUUIDAndDistance
import org.mtransit.android.data.uuidAndDistance
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.NewsRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.addAllN
import org.mtransit.commons.removeAllAnd
import org.mtransit.commons.sortWithAnd
import org.mtransit.commons.takeAnd
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class POIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val newsRepository: NewsRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = POIViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_POI_UUID = "extra_poi_uuid"

        private const val NEARBY_CONNECTIONS_INITIAL_COVERAGE = 100f
        private const val NEARBY_CONNECTIONS_MAX_COVERAGE = 2f * LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS.toFloat()
    }

    override fun getLogTag(): String = LOG_TAG

    val uuid = savedStateHandle.getLiveDataDistinct<String>(EXTRA_POI_UUID)

    private val _authority = savedStateHandle.getLiveDataDistinct<String>(EXTRA_AUTHORITY)

    val agency: LiveData<AgencyProperties?> = this._authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgency(authority) // #onModulesUpdated // UPDATE-ABLE
    }

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    val poim: LiveData<POIManager?> = PairMediatorLiveData(agency, uuid).switchMap { (agency, uuid) -> // #onModulesUpdated
        getPOIManager(agency, uuid)
    }

    private fun getPOIManager(agency: IAgencyProperties?, uuid: String?) =
        poiRepository.readingPOIM(agency, uuid, poim.value, onDataSourceRemoved = {
            dataSourceRemovedEvent.postValue(Event(true))
        })

    private val _poi = this.poim.map {
        it?.poi
    }

    val scheduleProviders: LiveData<List<ScheduleProviderProperties>> = _authority.switchMap { authority ->
        this.dataSourcesRepository.readingScheduleProviders(authority)
    }

    private val _allAgencies = this.dataSourcesRepository.readingAllAgenciesBase() // #onModulesUpdated

    private val _poiArea = _poi.map { it -> it?.let { Area.getArea(it.lat, it.lng, 0.01) } }

    private val nearbyAgencies: LiveData<List<AgencyBaseProperties>?> = PairMediatorLiveData(_poiArea, _allAgencies).map { (poiArea, allAgencies) ->
        allAgencies?.filter {
            it.type.isNearbyScreen
                    && it.type != DataSourceType.TYPE_MODULE
                    && it.isInArea(poiArea)
        }
    }

    // like Home screen (no infinite loading like in Nearby screen)
    val nearbyPOIs: LiveData<List<POIManager>?> = TripleMediatorLiveData(nearbyAgencies, agency, _poi).switchMap { (nearbyAgencies, agency, poi) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getNearbyPOIs(nearbyAgencies, agency, poi))
        }
    }

    private val poiConnectionComparator by lazy {
        POIConnectionComparator({
            when (it) {
                DataSourceType.TYPE_BIKE.id -> 250f
                else -> NEARBY_CONNECTIONS_MAX_COVERAGE
            }
        })
    }

    private val poiAlphaComparator by lazy { POIAlphaComparator() }

    private val POI.isNoPickup: Boolean
        get() = this is RouteTripStop && this.isNoPickup

    private fun POI.isSameRoute(other: POI): Boolean {
        if (this !is RouteTripStop || other !is RouteTripStop) return false
        return this.route.id == other.route.id
    }

    private fun POI.isSameRouteTrip(other: POI): Boolean {
        if (this !is RouteTripStop || other !is RouteTripStop) return false
        return this.route.id == other.route.id
                && this.trip.id == other.trip.id
    }

    private suspend fun getNearbyPOIs(
        nearbyAgencies: List<IAgencyProperties>?,
        agency: IAgencyProperties?,
        excludedPoi: POI?,
    ): List<POIManager>? {
        if (Constants.FORCE_NEARBY_POI_LIST_OFF) {
            MTLog.d(this, "getNearbyPOIs() > SKIP (feature disabled)")
            return null
        }
        if (nearbyAgencies == null || agency == null || excludedPoi == null) {
            MTLog.d(this, "getNearbyPOIs() > SKIP (no authority or nor lat/lng)")
            return null
        }
        val lat = excludedPoi.lat
        val lng = excludedPoi.lng
        val excludedUUID = excludedPoi.uuid
        this.poiConnectionComparator.targetedPOI = excludedPoi
        val maxSize = LocationUtils.MAX_POI_NEARBY_POIS_LIST
        val minCoverageInMeters = LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS.toFloat()
        val nearbyPOIs = mutableListOf<POIManager>()
        var ad = LocationUtils.getNewDefaultAroundDiff()
        var maxDistanceInMeters = NEARBY_CONNECTIONS_INITIAL_COVERAGE
        var nearbyAgencyPOIAdded = false
        // 1 - try connections only in closest nearby area
        while (true) {
            if (maxDistanceInMeters >= LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, ad.aroundDiff)) {
                LocationUtils.incAroundDiff(ad)
            }
            val aroundDiff = ad.aroundDiff
            val poiFilter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, aroundDiff).apply {
                addExtra(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, true)
            }
            nearbyAgencies
                .forEach { nearbyAgency ->
                    nearbyPOIs.addAllN(
                        poiRepository.findPOIMs(nearbyAgency, poiFilter)
                            ?.removeAllAnd {
                                it.poi.uuid == excludedUUID
                                        || (it.poi.isNoPickup && !it.poi.isSameRoute(excludedPoi))
                            }
                            ?.updateDistanceM(lat, lng)
                            ?.removeTooFar(
                                when (nearbyAgency.type) {
                                    DataSourceType.TYPE_BUS -> maxDistanceInMeters
                                    DataSourceType.TYPE_BIKE -> maxDistanceInMeters * 1.5f
                                    DataSourceType.TYPE_SUBWAY -> maxDistanceInMeters * 2f
                                    DataSourceType.TYPE_RAIL -> maxDistanceInMeters * 2f
                                    DataSourceType.TYPE_LIGHT_RAIL -> maxDistanceInMeters * 2f
                                    DataSourceType.TYPE_FERRY -> maxDistanceInMeters * 2f
                                    else -> {
                                        MTLog.w(this, "Unexpected type ${nearbyAgency.type} in POI nearby agencies!")
                                        maxDistanceInMeters
                                    }
                                }.coerceAtMost(NEARBY_CONNECTIONS_MAX_COVERAGE * 2f)
                            )
                            ?.removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
                            ?.removeAllAnd { nearbyPOIs.contains(it) }
                            ?.removeAllAnd { new -> nearbyPOIs.any { it.poi.isSameRouteTrip(new.poi) } }
                            ?.also {
                                if (!nearbyAgencyPOIAdded
                                    && nearbyAgency.authority == agency.authority && it.isNotEmpty()
                                ) {
                                    nearbyAgencyPOIAdded = true
                                }
                            }
                    )
                }
            nearbyPOIs.sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR)
            removeDuplicateRouteTrip(sortedPOIMList = nearbyPOIs)
            val firstRelevantDistance = nearbyPOIs.firstOrNull { it.distance > 0f && !it.poi.isSameRoute(excludedPoi) }?.distance
            val firstLastDistanceDiff = nearbyPOIs.takeIf { it.size >= 2 }?.let { it.last().distance - it.first().distance }
                ?.takeIf { it > 0f }?.coerceAtMost(maxDistanceInMeters)
            val minDistance = firstRelevantDistance
                ?.let { it + it.coerceAtLeast(.5f * NEARBY_CONNECTIONS_INITIAL_COVERAGE) } // 1st relevant distance x2 ( min initial coverage)
                ?: NEARBY_CONNECTIONS_INITIAL_COVERAGE
            val significantDistance = firstRelevantDistance
                ?.coerceAtLeast(.5f * NEARBY_CONNECTIONS_INITIAL_COVERAGE)
                ?.let { it * 1.5f }
                ?.coerceAtLeast(minDistance)
            if (
                maxDistanceInMeters >= 2f * NEARBY_CONNECTIONS_MAX_COVERAGE ||
                (significantDistance != null && maxDistanceInMeters >= significantDistance)
            ) {
                break
            } else {
                // TODO latter ? lastTypeAroundDiff = if (nearbyPOIs.isNullOrEmpty()) aroundDiff else null
                if (significantDistance != null && significantDistance > maxDistanceInMeters) {
                    maxDistanceInMeters = significantDistance
                    continue
                }
                if (firstLastDistanceDiff != null && firstLastDistanceDiff > 0f) {
                    maxDistanceInMeters += firstLastDistanceDiff
                    continue
                }
                maxDistanceInMeters *= 1.5f
            }
        }
        val minNotConnectionSize = when {
            nearbyPOIs.isEmpty() -> 5
            !nearbyAgencyPOIAdded -> 1
            else -> 0
        }
        // 2 - try all nearby from current agency
        if (minNotConnectionSize > 0) {
            val connectionSize = nearbyPOIs.size
            ad = LocationUtils.getNewDefaultAroundDiff()
            while (true) {
                val aroundDiff = ad.aroundDiff
                maxDistanceInMeters = LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, aroundDiff)
                val poiFilter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, aroundDiff).apply {
                    addExtra(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, true)
                }
                nearbyPOIs.addAllN(
                    poiRepository.findPOIMs(agency, poiFilter)
                        ?.removeAllAnd {
                            it.poi.uuid == excludedUUID
                                    || (it.poi.isNoPickup && it.poi.isSameRoute(excludedPoi)) // remove if no pickup && another route
                        }
                        ?.updateDistanceM(lat, lng)
                        ?.removeTooFar(maxDistanceInMeters)
                        ?.removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
                        ?.removeAllAnd { nearbyPOIs.contains(it) }
                        ?.takeAnd(minNotConnectionSize - (nearbyPOIs.size - connectionSize))
                )
                if (nearbyPOIs.size >= connectionSize + minNotConnectionSize // enough POI
                    || LocationUtils.searchComplete(lat, lng, aroundDiff) // world explored
                ) {
                    break
                } else {
                    // TODO latter ? lastTypeAroundDiff = if (nearbyPOIs.isNullOrEmpty()) aroundDiff else null
                    LocationUtils.incAroundDiff(ad)
                }
            }
        }
        nearbyPOIs.sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR)
        nearbyPOIs.sortWithAnd(poiAlphaComparator)
        nearbyPOIs.sortWithAnd(poiConnectionComparator)
        return nearbyPOIs
    }

    private fun removeDuplicateRouteTrip(sortedPOIMList: MutableList<POIManager>) {
        val it = sortedPOIMList.iterator()
        val routeTripKept = mutableSetOf<String>()
        while (it.hasNext()) {
            val poim = it.next()
            if (poim.poi is RouteTripStop) { // RTS
                val rts = poim.poi
                val routeTripId = "${rts.route.id}-${rts.trip.id}"
                if (routeTripKept.contains(routeTripId)) {
                    it.remove()
                    continue
                }
                routeTripKept += "${poim.poi.route.id}-${poim.poi.trip.id}"
            }
        }
    }

    private val _newsProviders = _authority.switchMap {
        dataSourcesRepository.readingNewsProviders(it) // #onModulesUpdated
    }

    val latestNewsArticleList: LiveData<List<News>?> = PairMediatorLiveData(_poi, _newsProviders).switchMap { (poi, newsProviders) ->
        newsRepository.loadingNewsArticles(
            newsProviders,
            poi,
            News.NEWS_SEVERITY_COMPARATOR,
            { allNews ->
                val nowInMs = UITimeUtils.currentTimeMillis()
                val selectedNews = mutableListOf<News>()
                val minSelectedArticles = min(2, allNews.size)  // encourage 2+ articles
                val maxSelectedArticles = max(5, minSelectedArticles)
                var noteworthiness = 1L
                while (selectedNews.size < minSelectedArticles
                    && noteworthiness < 13L
                ) {
                    for (news in allNews) {
                        val validityInMs: Long = news.createdAtInMs + news.noteworthyInMs * noteworthiness
                        if (validityInMs < nowInMs) {
                            continue  // news too old to be worthy
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
                selectedNews
            },
            coroutineContext = viewModelScope.coroutineContext + Dispatchers.IO,
        )
    }

    fun onBatteryOptimizationSettingsOpened() {
        hasSeenDisabledModule = false // click on the message once, show again next module disabled
    }

    @get:WorkerThread
    @set:WorkerThread
    @get:JvmName("hasSeenDisabledModule")
    var hasSeenDisabledModule: Boolean = LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED_DEFAULT
        get() = lclPrefRepository.getValue(
            LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED,
            LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED_DEFAULT
        )
        private set(value) {
            if (hasSeenDisabledModule != value) {
                lclPrefRepository.pref.edit {
                    putBoolean(LocalPreferenceRepository.PREF_USER_SEEN_APP_DISABLED, value)
                }
                field = value
            }
        }

    fun refreshAppUpdateAvailable() {
        val agencyProperties = this.agency.value ?: return
        viewModelScope.launch {
            dataSourcesRepository.refreshAvailableVersions(forcePkg = agencyProperties.pkg)
        }
    }
}