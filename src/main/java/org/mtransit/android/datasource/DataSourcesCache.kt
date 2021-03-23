package org.mtransit.android.datasource

import org.mtransit.android.data.DataSourceType

class DataSourcesCache(private val dataSourcesDatabase: DataSourcesDatabase) {

    // AGENCY

    private fun agencyPropertiesDao() = dataSourcesDatabase.agencyPropertiesDao()

    fun getAllAgencies() = agencyPropertiesDao().getAllAgencies()

    fun readingAllAgencies() = agencyPropertiesDao().readingAllAgencies()

    fun getAllAgenciesCount() = agencyPropertiesDao().getAllAgenciesCount()

    fun readingAllAgenciesCount() = agencyPropertiesDao().readingAllAgenciesCount()

    fun getAgency(authority: String) = agencyPropertiesDao().getAgency(authority)

    fun readingAgency(authority: String) = agencyPropertiesDao().readingAgency(authority)

    fun getAllDataSourceTypes() = agencyPropertiesDao().getAllDataSourceTypes()

    fun readingAllDataSourceTypes() = agencyPropertiesDao().readingAllDataSourceTypes()

    fun getTypeDataSources(dst: DataSourceType) = agencyPropertiesDao().getTypeDataSources(dst)

    fun getAgencyPkg(authority: String) = agencyPropertiesDao().getAgencyPkg(authority)

    fun getAgencyColorInt(authority: String) = agencyPropertiesDao().getAgencyColorInt(authority)

    // STATUS

    private fun statusProviderPropertiesDao() = dataSourcesDatabase.statusProviderPropertiesDao()

    fun getAllStatusProviders() = statusProviderPropertiesDao().getAllStatusProvider()

    fun readingAllStatusProviders() = statusProviderPropertiesDao().readingAllStatusProviders()

    fun getStatusProviders(targetAuthority: String) = statusProviderPropertiesDao().getTargetAuthorityStatusProvider(targetAuthority)

    fun getStatusProvider(authority: String) = statusProviderPropertiesDao().getStatusProvider(authority)

    // SCHEDULE

    private fun scheduleProviderPropertiesDao() = dataSourcesDatabase.scheduleProviderPropertiesDao()

    fun getAllScheduleProviders() = scheduleProviderPropertiesDao().getAllScheduleProvider()

    fun readingAllScheduleProviders() = scheduleProviderPropertiesDao().readingAllScheduleProvider()

    fun getScheduleProviders(targetAuthority: String) = scheduleProviderPropertiesDao().getTargetAuthorityScheduleProvider(targetAuthority)

    fun getScheduleProvider(authority: String) = scheduleProviderPropertiesDao().getScheduleProvider(authority)

    // SERVICE UPDATE

    private fun serviceUpdateProviderPropertiesDao() = dataSourcesDatabase.serviceUpdateProviderPropertiesDao()

    fun getAllServiceUpdateProviders() = serviceUpdateProviderPropertiesDao().getAllServiceUpdateProvider()

    fun readingAllServiceUpdateProviders() = serviceUpdateProviderPropertiesDao().readingAllServiceUpdateProvider()

    fun getServiceUpdateProviders(targetAuthority: String) = serviceUpdateProviderPropertiesDao().getTargetAuthorityServiceUpdateProvider(targetAuthority)

    fun getServiceUpdateProvider(authority: String) = serviceUpdateProviderPropertiesDao().getServiceUpdateProvider(authority)

    // NEWS

    private fun newsProviderPropertiesDao() = dataSourcesDatabase.newsProviderPropertiesDao()

    fun getAllNewsProviders() = newsProviderPropertiesDao().getAllNewsProvider()

    fun readingAllNewsProviders() = newsProviderPropertiesDao().readingAllNewsProvider()

    fun getNewsProviders(targetAuthority: String) = newsProviderPropertiesDao().getTargetAuthorityNewsProviders(targetAuthority)

    fun getNewsProvider(authority: String) = newsProviderPropertiesDao().getNewsProvider(authority)
}