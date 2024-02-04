package org.mtransit.android.datasource

import org.mtransit.android.data.DataSourceType
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("unused")
@Singleton
class DataSourcesCache @Inject constructor(
    private val dataSourcesDatabase: DataSourcesDatabase,
) {

    // region AGENCY

    private fun agencyPropertiesDao() = dataSourcesDatabase.agencyPropertiesDao()

    suspend fun getAllAgencies() = agencyPropertiesDao().getAllAgencies()

    fun readingAllAgencies() = agencyPropertiesDao().readingAllAgencies()

    fun readingAllAgenciesBase() = agencyPropertiesDao().readingAllAgenciesBase()

    suspend fun getAgency(authority: String) = agencyPropertiesDao().getAgency(authority)

    fun readingAgency(authority: String) = agencyPropertiesDao().readingAgency(authority)

    fun readingAgencyBase(authority: String) = agencyPropertiesDao().readingAgencyBase(authority)

    suspend fun getAllNotExtendedDataSourceTypes() = agencyPropertiesDao().getAllNotExtendedDataSourceTypes()
    suspend fun getAllExtendedDataSourceTypes() = agencyPropertiesDao().getAllExtendedDataSourceTypes()

    fun readingAllNotExtendedDataSourceTypes() = agencyPropertiesDao().readingAllNotExtendedDataSourceTypes()
    fun readingAllExtendedDataSourceTypes() = agencyPropertiesDao().readingAllExtendedDataSourceTypes()

    suspend fun getAgencyPkg(authority: String) = agencyPropertiesDao().getAgencyPkg(authority)

    suspend fun getAgencyColorInt(authority: String) = agencyPropertiesDao().getAgencyColorInt(authority)

    // endregion

    // region STATUS

    private fun statusProviderPropertiesDao() = dataSourcesDatabase.statusProviderPropertiesDao()

    suspend fun getAllStatusProviders() = statusProviderPropertiesDao().getAllStatusProvider()

    fun readingAllStatusProviders() = statusProviderPropertiesDao().readingAllStatusProviders()

    suspend fun getStatusProviders(targetAuthority: String) = statusProviderPropertiesDao().getTargetAuthorityStatusProvider(targetAuthority)

    suspend fun getStatusProvider(authority: String) = statusProviderPropertiesDao().getStatusProvider(authority)

    // endregion

    // region SCHEDULE

    private fun scheduleProviderPropertiesDao() = dataSourcesDatabase.scheduleProviderPropertiesDao()

    suspend fun getAllScheduleProviders() = scheduleProviderPropertiesDao().getAllScheduleProvider()

    fun readingAllScheduleProviders() = scheduleProviderPropertiesDao().readingAllScheduleProvider()

    suspend fun getScheduleProviders(targetAuthority: String) = scheduleProviderPropertiesDao().getTargetAuthorityScheduleProviders(targetAuthority)

    fun readingScheduleProviders(targetAuthority: String?) = scheduleProviderPropertiesDao().readingTargetAuthorityScheduleProviders(targetAuthority)

    suspend fun getScheduleProvider(authority: String) = scheduleProviderPropertiesDao().getScheduleProvider(authority)

    fun readingScheduleProvider(authority: String) = scheduleProviderPropertiesDao().readingScheduleProvider(authority)

    // endregion

    // region SERVICE UPDATE

    private fun serviceUpdateProviderPropertiesDao() = dataSourcesDatabase.serviceUpdateProviderPropertiesDao()

    suspend fun getAllServiceUpdateProviders() = serviceUpdateProviderPropertiesDao().getAllServiceUpdateProvider()

    fun readingAllServiceUpdateProviders() = serviceUpdateProviderPropertiesDao().readingAllServiceUpdateProvider()

    suspend fun getServiceUpdateProviders(targetAuthority: String) =
        serviceUpdateProviderPropertiesDao().getTargetAuthorityServiceUpdateProvider(targetAuthority)

    suspend fun getServiceUpdateProvider(authority: String) = serviceUpdateProviderPropertiesDao().getServiceUpdateProvider(authority)

    // endregion

    // region NEWS

    private fun newsProviderPropertiesDao() = dataSourcesDatabase.newsProviderPropertiesDao()

    suspend fun getAllNewsProviders() = newsProviderPropertiesDao().getAllNewsProvider()

    fun readingAllNewsProviders() = newsProviderPropertiesDao().readingAllNewsProvider()

    suspend fun getNewsProviders(targetAuthority: String) = newsProviderPropertiesDao().getTargetAuthorityNewsProviders(targetAuthority)

    fun readingNewsProviders(targetAuthority: String) = newsProviderPropertiesDao().readingTargetAuthorityNewsProviders(targetAuthority)

    suspend fun getNewsProvider(authority: String) = newsProviderPropertiesDao().getNewsProvider(authority)

    // endregion
}