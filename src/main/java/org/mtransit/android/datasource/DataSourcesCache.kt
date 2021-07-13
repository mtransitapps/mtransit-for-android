package org.mtransit.android.datasource

import org.mtransit.android.data.DataSourceType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSourcesCache @Inject constructor(
    private val dataSourcesDatabase: DataSourcesDatabase,
) {

    // AGENCY

    private fun agencyPropertiesDao() = dataSourcesDatabase.agencyPropertiesDao()

    fun getAllAgencies() = agencyPropertiesDao().getAllAgencies()

    fun readingAllAgencies() = agencyPropertiesDao().readingAllAgencies()

    fun readingAllAgenciesBase() = agencyPropertiesDao().readingAllAgenciesBase()

    fun readingAgency(authority: String) = agencyPropertiesDao().readingAgency(authority)

    fun readingAgencyBase(authority: String) = agencyPropertiesDao().readingAgencyBase(authority)

    fun getAllDataSourceTypes() = agencyPropertiesDao().getAllDataSourceTypes()

    fun readingAllDataSourceTypes() = agencyPropertiesDao().readingAllDataSourceTypes()

    fun getTypeDataSources(dst: DataSourceType) = agencyPropertiesDao().getTypeDataSources(dst)

    fun readingTypeDataSources(dst: DataSourceType) = agencyPropertiesDao().readingTypeDataSources(dst)

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

    fun getScheduleProviders(targetAuthority: String) = scheduleProviderPropertiesDao().getTargetAuthorityScheduleProviders(targetAuthority)

    fun readingScheduleProviders(targetAuthority: String?) = scheduleProviderPropertiesDao().readingTargetAuthorityScheduleProviders(targetAuthority)

    fun getScheduleProvider(authority: String) = scheduleProviderPropertiesDao().getScheduleProvider(authority)

    fun readingScheduleProvider(authority: String) = scheduleProviderPropertiesDao().readingScheduleProvider(authority)

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

    fun readingNewsProviders(targetAuthority: String) = newsProviderPropertiesDao().readingTargetAuthorityNewsProviders(targetAuthority)

    fun getNewsProvider(authority: String) = newsProviderPropertiesDao().getNewsProvider(authority)
}