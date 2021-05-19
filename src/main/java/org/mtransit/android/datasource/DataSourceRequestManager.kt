package org.mtransit.android.datasource

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.ScheduleTimestamps
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceManager
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.JPaths
import org.mtransit.android.data.POIManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSourceRequestManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    fun findPOI(authority: String, poiFilter: POIProviderContract.Filter): POIManager? {
        return DataSourceManager.findPOI(appContext, authority, poiFilter)
    }

    fun findPOIs(authority: String, poiFilter: POIProviderContract.Filter): List<POIManager>? {
        return DataSourceManager.findPOIs(appContext, authority, poiFilter)
    }

    fun findAgencyAvailableVersionCode(authority: String, forceAppUpdateRefresh: Boolean = false, inFocus: Boolean = false): Int {
        return DataSourceManager.findAgencyAvailableVersionCode(appContext, authority, forceAppUpdateRefresh, inFocus)
    }

    fun findAgencyRTSRouteLogo(agencyAuthority: String): JPaths? {
        return DataSourceManager.findAgencyRTSRouteLogo(appContext, agencyAuthority)
    }

    fun findAllRTSAgencyRoutes(agencyAuthority: String): List<Route>? {
        return DataSourceManager.findAllRTSAgencyRoutes(appContext, agencyAuthority)
    }

    fun findRTSRoute(agencyAuthority: String, routeId: Long): Route? {
        return DataSourceManager.findRTSRoute(appContext, agencyAuthority, routeId)
    }

    fun findRTSRouteTrips(agencyAuthority: String, routeId: Long): List<Trip>? {
        return DataSourceManager.findRTSRouteTrips(appContext, agencyAuthority, routeId)
    }

    fun findAgencyProperties(
        agencyAuthority: String,
        agencyType: DataSourceType,
        rts: Boolean,
        logo: JPaths?,
        pkg: String,
        longVersionCode: Long,
        b: Boolean,
        trigger: Int
    ): AgencyProperties? {
        return DataSourceManager.findAgencyProperties(appContext, agencyAuthority, agencyType, rts, logo, pkg, longVersionCode, b, trigger)
    }

    fun findScheduleTimestamps(authority: String, scheduleTimestampsFilter: ScheduleTimestampsProviderContract.Filter?): ScheduleTimestamps? {
        return DataSourceManager.findScheduleTimestamps(appContext, authority, scheduleTimestampsFilter)
    }

    fun findNews(authority: String, newsFilter: NewsProviderContract.Filter? = null): List<News>? {
        return DataSourceManager.findNews(appContext, authority, newsFilter)
    }
}