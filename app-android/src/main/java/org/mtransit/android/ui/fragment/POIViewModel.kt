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
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.removeTooMuchWhenNotInCoverage
import org.mtransit.android.commons.updateDistanceM
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.NewsRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.addAllN
import org.mtransit.commons.keepFirst
import org.mtransit.commons.removeAllAnd
import org.mtransit.commons.sortWithAnd
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

    // like Home screen (no infinite loading like in Nearby screen)
    val nearbyPOIs: LiveData<List<POIManager>?> = PairMediatorLiveData(agency, _poi).switchMap { (agency, poi) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getNearbyPOIs(agency, poi))
        }
    }

    private val poiSameRouteComparator by lazy { POIManager.POISameRouteComparator() }

    private suspend fun getNearbyPOIs(
        agency: IAgencyProperties? = this.agency.value,
        excludedPoi: POI? = this._poi.value,
    ): List<POIManager>? {
        if (Constants.FORCE_NEARBY_POI_LIST_OFF) {
            MTLog.d(this, "getNearbyPOIs() > SKIP (feature disabled)")
            return null
        }
        if (agency == null || excludedPoi == null) {
            MTLog.d(this, "getNearbyPOIs() > SKIP (no authority or nor lat/lng)")
            return null
        }
        val lat = excludedPoi.lat
        val lng = excludedPoi.lng
        val excludedUUID = excludedPoi.uuid
        this.poiSameRouteComparator.targetedRoute = (excludedPoi as? RouteTripStop)?.route
        val nearbyPOIs = mutableListOf<POIManager>()
        val ad = LocationUtils.getNewDefaultAroundDiff()
        // TODO latter ? var lastTypeAroundDiff: Double? = null
        val maxSize = LocationUtils.MAX_POI_NEARBY_POIS_LIST
        val minCoverageInMeters = LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS.toFloat()
        while (true) {
            val aroundDiff = ad.aroundDiff
            val maxDistance = LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, aroundDiff)
            val poiFilter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, aroundDiff).apply {
                addExtra(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, true)
            }
            nearbyPOIs.addAllN(
                poiRepository.findPOIMs(agency, poiFilter)
                    ?.removeAllAnd { it.poi.uuid == excludedUUID }
                    ?.updateDistanceM(lat, lng)
                    ?.removeTooFar(maxDistance)
                    ?.removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
                    ?.removeAllAnd { nearbyPOIs.contains(it) }
            )
            if (nearbyPOIs.size > LocationUtils.MIN_NEARBY_LIST // enough POI
                || LocationUtils.searchComplete(lat, lng, aroundDiff) // world explored
            ) {
                break
            } else {
                // TODO latter ? lastTypeAroundDiff = if (nearbyPOIs.isNullOrEmpty()) aroundDiff else null
                LocationUtils.incAroundDiff(ad)
            }
        }
        nearbyPOIs.sortWithAnd(poiSameRouteComparator)
        return nearbyPOIs.keepFirst(LocationUtils.MAX_POI_NEARBY_POIS_LIST)
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