package org.mtransit.android.datasource

import org.mtransit.android.data.AgencyProperties

class DataSourcesCache(private val dataSourcesDatabase: DataSourcesDatabase) {

    val allAgencies = dataSourcesDatabase.agencyPropertiesDao().getAllAgencies()

    val allInstalledAgencies = dataSourcesDatabase.agencyPropertiesDao().getAllInstalledAgencies()

    fun update(agencyProperties: AgencyProperties) {
        dataSourcesDatabase.agencyPropertiesDao().update(agencyProperties)
    }
}