package org.mtransit.android.datasource

import org.mtransit.android.common.IApplication
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceManager
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.JPaths
import org.mtransit.android.data.POIManager

class DataSourceRequestManager(private val app: IApplication) {

    private val context get() = app.requireContext()

    fun findPOIs(authority: String, poiFilter: POIProviderContract.Filter): List<POIManager>? {
        return DataSourceManager.findPOIs(context, authority, poiFilter)
    }

    fun findAgencyAvailableVersionCode(authority: String, forceRefresh: Boolean, inFocus: Boolean): Int {
        return DataSourceManager.findAgencyAvailableVersionCode(context, authority, forceRefresh, inFocus)
    }

    fun findAgencyRTSRouteLogo(agencyAuthority: String): JPaths? {
        return DataSourceManager.findAgencyRTSRouteLogo(context, agencyAuthority)
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
}