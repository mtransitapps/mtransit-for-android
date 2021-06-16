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
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.util.UITimeUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class POIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = POIViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_POI_UUID = "extra_poi_uuid"
    }

    override fun getLogTag(): String = LOG_TAG

    val uuid = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_POI_UUID)

    private val _authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AUTHORITY)

    val agency: LiveData<AgencyProperties?> = this._authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgency(authority) // #onModulesUpdated // UPDATE-ABLE
    }

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    val poim: LiveData<POIManager?> = PairMediatorLiveData(agency, uuid).switchMap { (agency, uuid) -> // use agency == #onModulesUpdated
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getPOIManager(agency, uuid))
        }
    }

    private fun getPOIManager(agency: IAgencyProperties?, uuid: String?): POIManager? {
        if (uuid.isNullOrEmpty()) {
            MTLog.d(this, "getPOI() > SKIP (no uuid)")
            return null
        }
        if (agency == null) {
            if (poim.value != null) {
                MTLog.d(this, "getPOI() > data source removed (no more agency)")
                dataSourceRemovedEvent.postValue(Event(true))
            }
            return null
        }
        return this.dataSourceRequestManager.findPOIM(agency.authority, POIProviderContract.Filter.getNewUUIDFilter(uuid))
            ?: run {
                MTLog.d(this, "getPOI() > SKIP (data source removed!)")
                dataSourceRemovedEvent.postValue(Event(true))
                null
            }
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

    private fun getNearbyPOIs(poi: POI?): List<POIManager>? {
        return getNearbyPOIs(poi?.authority, poi?.lat, poi?.lng)
    }

    private fun getNearbyPOIs(
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

    private val _newsProviders = _authority.switchMap {
        dataSourcesRepository.readingNewsProviders(it) // #onModulesUpdated
    }

    val latestNewsArticleList: LiveData<List<News>?> = PairMediatorLiveData(_poi, _newsProviders).switchMap { (poi, newsProviders) ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getLatestNewsArticles(poi, newsProviders))
        }
    }

    private fun getLatestNewsArticles(poi: POI?, newsProviders: List<NewsProviderProperties>?): List<News>? {
        if (poi == null || newsProviders == null) {
            return null
        }
        val nowInMs = UITimeUtils.currentTimeMillis()
        val last2Weeks = nowInMs - TimeUnit.DAYS.toMillis(14L)
        val newsFilter = NewsProviderContract.Filter
            .getNewTargetFilter(poi)
            .setMinCreatedAtInMs(last2Weeks)
        val allNews = newsProviders
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