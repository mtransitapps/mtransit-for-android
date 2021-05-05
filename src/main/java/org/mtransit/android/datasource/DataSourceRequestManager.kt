package org.mtransit.android.datasource

import org.mtransit.android.common.IApplication
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.ScheduleTimestamps
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceManager
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.JPaths
import org.mtransit.android.data.POIManager

class DataSourceRequestManager(private val app: IApplication) {

    private val context get() = app.requireContext()

    fun findPOI(authority: String, poiFilter: POIProviderContract.Filter): POIManager? {
        return DataSourceManager.findPOI(context, authority, poiFilter)
    }

    fun findPOIs(authority: String, poiFilter: POIProviderContract.Filter): List<POIManager>? {
        return DataSourceManager.findPOIs(context, authority, poiFilter)
    }

    fun findAgencyAvailableVersionCode(authority: String, forceAppUpdateRefresh: Boolean = false, inFocus: Boolean = false): Int {
        return DataSourceManager.findAgencyAvailableVersionCode(context, authority, forceAppUpdateRefresh, inFocus)
    }

    fun findAgencyRTSRouteLogo(agencyAuthority: String): JPaths? {
        return DataSourceManager.findAgencyRTSRouteLogo(context, agencyAuthority)
    }

    fun findAllRTSAgencyRoutes(agencyAuthority: String): List<Route>? {
        return DataSourceManager.findAllRTSAgencyRoutes(context, agencyAuthority)
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
        return DataSourceManager.findAgencyProperties(context, agencyAuthority, agencyType, rts, logo, pkg, longVersionCode, b, trigger)
    }

    fun findScheduleTimestamps(authority: String, scheduleTimestampsFilter: ScheduleTimestampsProviderContract.Filter?): ScheduleTimestamps? {
        return DataSourceManager.findScheduleTimestamps(context, authority, scheduleTimestampsFilter)
    }

    fun findNews(authority: String, newsFilter: NewsProviderContract.Filter? = null): List<News>? {
        return DataSourceManager.findNews(context, authority, newsFilter)
    }
}