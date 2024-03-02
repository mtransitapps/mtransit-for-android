package org.mtransit.android.datasource

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.ScheduleTimestamps
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceManager
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.JPaths
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.POIManager
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("unused", "MemberVisibilityCanBePrivate")
@Singleton
class DataSourceRequestManager(
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MTLog.Loggable {

    @Inject
    constructor(
        @ApplicationContext appContext: Context,
    ) : this(
        appContext,
        Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG = DataSourceRequestManager::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    suspend fun findPOI(authority: String, poiFilter: POIProviderContract.Filter): POI? = withContext(ioDispatcher) {
        DataSourceManager.findPOI(appContext, authority, poiFilter)?.poi
    }

    suspend fun findPOIM(authority: String, poiFilter: POIProviderContract.Filter): POIManager? = withContext(ioDispatcher) {
        DataSourceManager.findPOI(appContext, authority, poiFilter)
    }

    suspend fun findPOIs(authority: String, poiFilter: POIProviderContract.Filter): List<POI>? = withContext(ioDispatcher) {
        DataSourceManager.findPOIs(appContext, authority, poiFilter)?.map { it.poi }
    }

    suspend fun findPOIMs(provider: IAgencyProperties, poiFilter: POIProviderContract.Filter) = findPOIMs(provider.authority, poiFilter)

    suspend fun findPOIMs(authority: String, poiFilter: POIProviderContract.Filter): MutableList<POIManager>? = withContext(ioDispatcher) {
        DataSourceManager.findPOIs(appContext, authority, poiFilter)
    }

    suspend fun findAgencyAvailableVersionCode(authority: String, forceAppUpdateRefresh: Boolean = false, inFocus: Boolean = false): Int? =
        withContext(ioDispatcher) {
            DataSourceManager.findAgencyAvailableVersionCode(appContext, authority, forceAppUpdateRefresh, inFocus)
        }

    suspend fun findAgencyRTSRouteLogo(agencyAuthority: String): JPaths? = withContext(ioDispatcher) {
        DataSourceManager.findAgencyRTSRouteLogo(appContext, agencyAuthority)
    }

    suspend fun findAllRTSAgencyRoutes(agencyAuthority: String): List<Route>? = withContext(ioDispatcher) {
        DataSourceManager.findAllRTSAgencyRoutes(appContext, agencyAuthority)
    }

    suspend fun findRTSRoute(agencyAuthority: String, routeId: Long): Route? = withContext(ioDispatcher) {
        DataSourceManager.findRTSRoute(appContext, agencyAuthority, routeId)
    }

    suspend fun findRTSRouteTrips(agencyAuthority: String, routeId: Long): List<Trip>? = withContext(ioDispatcher) {
        DataSourceManager.findRTSRouteTrips(appContext, agencyAuthority, routeId)
    }

    suspend fun findAgencyProperties(
        agencyAuthority: String,
        agencyType: DataSourceType,
        rts: Boolean,
        logo: JPaths?,
        pkg: String,
        longVersionCode: Long,
        enabled: Boolean,
        trigger: Int
    ): AgencyProperties? = withContext(ioDispatcher) {
        DataSourceManager.findAgencyProperties(appContext, agencyAuthority, agencyType, rts, logo, pkg, longVersionCode, enabled, trigger)
    }

    suspend fun findScheduleTimestamps(authority: String, scheduleTimestampsFilter: ScheduleTimestampsProviderContract.Filter?) = withContext(ioDispatcher) {
        DataSourceManager.findScheduleTimestamps(appContext, authority, scheduleTimestampsFilter)
    }

    suspend fun findANews(authority: String, newsFilter: NewsProviderContract.Filter? = null): News? = withContext(ioDispatcher) {
        DataSourceManager.findANews(appContext, authority, newsFilter)
    }

    suspend fun findNews(newsProvider: NewsProviderProperties, newsFilter: NewsProviderContract.Filter? = null) = findNews(newsProvider.authority, newsFilter)

    suspend fun findNews(authority: String, newsFilter: NewsProviderContract.Filter? = null): List<News>? = withContext(ioDispatcher) {
        DataSourceManager.findNews(appContext, authority, newsFilter)
    }
}