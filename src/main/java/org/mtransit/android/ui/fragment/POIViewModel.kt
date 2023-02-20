package org.mtransit.android.ui.fragment

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
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.removeTooMuchWhenNotInCoverage
import org.mtransit.android.commons.updateDistanceM
import org.mtransit.android.data.AgencyProperties
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
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class POIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val newsRepository: NewsRepository,
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

    private val _agencyAuthority = this.agency.map { it?.authority } // #onModulesUpdated

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    val poim: LiveData<POIManager?> = PairMediatorLiveData(_agencyAuthority, uuid).switchMap { (agencyAuthority, uuid) -> // #onModulesUpdated
        getPOIManager(agencyAuthority, uuid)
    }

    private fun getPOIManager(agencyAuthority: String?, uuid: String?) = poiRepository.readingPOIM(agencyAuthority, uuid, poim.value) {
        dataSourceRemovedEvent.postValue(Event(true))
    }

    private val _poi = this.poim.map {
        it?.poi
    }

    val scheduleProviders: LiveData<List<ScheduleProviderProperties>> = _authority.switchMap { authority ->
        this.dataSourcesRepository.readingScheduleProviders(authority)
    }

    // like Home screen (no infinite loading like in Nearby screen)
    val nearbyPOIs: LiveData<List<POIManager>?> = _poi.switchMap { poi ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getNearbyPOIs(poi))
        }
    }

    private suspend fun getNearbyPOIs(poi: POI?): List<POIManager>? {
        return getNearbyPOIs(poi?.authority, poi?.lat, poi?.lng)
    }

    private suspend fun getNearbyPOIs(
        authority: String? = _poi.value?.authority,
        lat: Double? = _poi.value?.lat,
        lng: Double? = _poi.value?.lng,
        excludedUUID: String? = _poi.value?.uuid
    ): List<POIManager>? {
        if (Constants.FORCE_NEARBY_POI_LIST_OFF) {
            MTLog.d(this, "getNearbyPOIs() > SKIP (feature disabled)")
            return null
        }
        if (authority == null || lat == null || lng == null) {
            MTLog.d(this, "getNearbyPOIs() > SKIP (no authority or nor lat/lng)")
            return null
        }
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
                poiRepository.findPOIMs(authority, poiFilter)
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
            context = viewModelScope.coroutineContext + Dispatchers.IO,
        )
    }
}