package org.mtransit.android.ui.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.util.UITimeUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class POIViewModel @Inject constructor(
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = POIViewModel::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val poi = MutableLiveData<POI>(null)

    fun onNewPOI(poim: POIManager?) {
        onNewPOI(poim?.poi)
    }

    fun onNewPOI(poi: POI?) {
        this.poi.postValue(poi)
    }

    // like Home screen (no infinite loading like in Nearby screen)
    val nearbyPOIs: LiveData<List<POIManager>?> = poi.switchMap { poi ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getNearbyPOIs(poi))
        }
    }

    private fun getNearbyPOIs(poi: POI?): List<POIManager>? {
        return getNearbyPOIs(poi?.authority, poi?.lat, poi?.lng)
    }

    private fun getNearbyPOIs(
        authority: String? = poi.value?.authority,
        lat: Double? = poi.value?.lat,
        lng: Double? = poi.value?.lng,
        excludedUUID: String? = poi.value?.uuid
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
            dataSourceRequestManager.findPOIMs(authority, poiFilter)
                ?.filterNot { it.poi.uuid == excludedUUID }
                ?.let { agencyPOIs ->
                    LocationUtils.updateDistance(agencyPOIs, lat, lng)
                    LocationUtils.removeTooFar(agencyPOIs, maxDistance)
                    LocationUtils.removeTooMuchWhenNotInCoverage(agencyPOIs, minCoverageInMeters, maxSize)
                    nearbyPOIs.addAll(agencyPOIs)
                }
            if (nearbyPOIs.size > LocationUtils.MIN_NEARBY_LIST // enough POI
                || LocationUtils.searchComplete(lat, lng, aroundDiff) // world explored
            ) {
                break
            } else {
                // TODO latter ? lastTypeAroundDiff = if (nearbyPOIs.isNullOrEmpty()) aroundDiff else null
                LocationUtils.incAroundDiff(ad)
            }
        }
        return nearbyPOIs.take(LocationUtils.MAX_POI_NEARBY_POIS_LIST)
    }

    val latestNewsArticleList: LiveData<List<News>?> = poi.switchMap { poi ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getLatestNewsArticles(poi))
        }
    }

    private fun getLatestNewsArticles(poi: POI?): List<News>? {
        if (poi == null) {
            return null
        }
        val nowInMs = UITimeUtils.currentTimeMillis()
        val last2Weeks = nowInMs - TimeUnit.DAYS.toMillis(14L)
        val newsFilter = NewsProviderContract.Filter
            .getNewTargetFilter(poi)
            .setMinCreatedAtInMs(last2Weeks)
        val allNews = dataSourcesRepository.getNewsProviders(poi)
            .mapNotNull { newsProvider ->
                this.dataSourceRequestManager.findNews(newsProvider, newsFilter)
            }.flatten()
            .toMutableList()
        allNews.sortWith(News.NEWS_SEVERITY_COMPARATOR)
        val selectedNews = mutableListOf<News>()
        var noteworthiness = 1L
        while (selectedNews.isEmpty() && noteworthiness < 10L) {
            for (news in allNews) {
                if (news.createdAtInMs + news.noteworthyInMs * noteworthiness < nowInMs) {
                    continue  // news too old to be worthy
                }
                selectedNews.add(0, news)
                break // found news article
            }
            noteworthiness++
        }
        return selectedNews
    }
}