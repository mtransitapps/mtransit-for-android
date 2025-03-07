package org.mtransit.android.datasource

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.billing.filterExpansiveAgencies
import org.mtransit.android.billing.filterExpansiveNewsProviders
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.data.ServiceUpdateProviderProperties
import org.mtransit.android.data.StatusProviderProperties
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.dev.filterDemoModeAgency
import org.mtransit.android.dev.filterDemoModeTargeted
import org.mtransit.android.dev.filterDemoModeType
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.commons.addAllNNE
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("MemberVisibilityCanBePrivate")
@Singleton
class DataSourcesInMemoryCache @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dataSourcesCache: DataSourcesCache,
    private val billingManager: IBillingManager,
    private val demoModeManager: DemoModeManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DataSourcesInMemoryCache::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val defaultAgencyComparator: Comparator<IAgencyProperties> = IAgencyProperties.SHORT_NAME_COMPARATOR

    private val defaultDataSourceTypeComparator: Comparator<DataSourceType> by lazy { DataSourceType.DataSourceTypeShortNameComparator(appContext) }

    private var _agencyProperties = listOf<AgencyProperties>() // sorted
    private var _agencyBaseProperties = listOf<AgencyBaseProperties>() // sorted
    private var _supportedDataSourceTypes = listOf<DataSourceType>() // sorted

    private var _statusProviderProperties = listOf<StatusProviderProperties>() // sorted for stability
    private var _scheduleProviderProperties = listOf<ScheduleProviderProperties>() // sorted for stability
    private var _serviceUpdateProviderProperties = listOf<ServiceUpdateProviderProperties>() // sorted for stability
    private var _newsProviderProperties = listOf<NewsProviderProperties>() // sorted for stability

    init {
        // START LISTENING FOR CHANGE TO FILL IN-MEMORY CACHE
        startListeningForChangesIntoMemory()
    }

    private fun startListeningForChangesIntoMemory() {
        dataSourcesCache.readingAllAgencies().observeForever { agencies -> // SINGLETON
            this._agencyProperties = agencies
                .filterExpansiveAgencies(billingManager)
                .filterDemoModeAgency(demoModeManager)
                .sortedWith(defaultAgencyComparator)
        }
        dataSourcesCache.readingAllAgenciesBase().observeForever { agencies -> // SINGLETON
            this._agencyBaseProperties = agencies
                .filterExpansiveAgencies(billingManager)
                .filterDemoModeAgency(demoModeManager)
                .sortedWith(defaultAgencyComparator)
        }
        PairMediatorLiveData(
            dataSourcesCache.readingAllNotExtendedDataSourceTypes(),
            dataSourcesCache.readingAllExtendedDataSourceTypes(),
        ).observeForever { (notExtendedDST, extendedDST) -> // SINGLETON
            if (notExtendedDST == null || extendedDST == null) return@observeForever
            this._supportedDataSourceTypes = notExtendedDST.toMutableList().apply { addAllNNE(extendedDST) }
                .filterDemoModeType(demoModeManager)
                .sortedWith(defaultDataSourceTypeComparator)
        }
        dataSourcesCache.readingAllStatusProviders().observeForever { // SINGLETON
            this._statusProviderProperties = it
                .filterDemoModeTargeted(demoModeManager)
        }
        dataSourcesCache.readingAllScheduleProviders().observeForever { // SINGLETON
            this._scheduleProviderProperties = it
                .filterDemoModeTargeted(demoModeManager)
        }
        dataSourcesCache.readingAllServiceUpdateProviders().observeForever { // SINGLETON
            this._serviceUpdateProviderProperties = it
                .filterDemoModeTargeted(demoModeManager)
        }
        dataSourcesCache.readingAllNewsProviders().observeForever { newsProviders -> // SINGLETON
            this._newsProviderProperties = newsProviders
                .filterExpansiveNewsProviders(billingManager)
                .filterDemoModeTargeted(demoModeManager)
        }
    }

    // AGENCY

    fun getAllAgencies() = this._agencyProperties

    fun getAgency(authority: String) = this._agencyProperties.singleOrNull { it.authority == authority }

    fun getAgencyBase(authority: String) = this._agencyBaseProperties.singleOrNull { it.authority == authority }

    fun getAgencyForPkg(pkg: String) = this._agencyProperties.singleOrNull { it.pkg == pkg }

    fun getAllSupportedDataSourceTypes() = this._supportedDataSourceTypes

    // STATUS

    fun getAllStatusProviders() = this._statusProviderProperties

    fun getStatusProviders(targetAuthority: String) = this._statusProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getStatusProvider(authority: String) = this._statusProviderProperties.singleOrNull { it.authority == authority }

    // SCHEDULE

    fun getAllScheduleProviders() = this._scheduleProviderProperties

    fun getScheduleProviders(targetAuthority: String?) = this._scheduleProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getScheduleProvidersList(targetAuthority: String?) = this._scheduleProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getScheduleProvider(authority: String) = this._scheduleProviderProperties.singleOrNull { it.authority == authority }

    // SERVICE UPDATE

    fun getAllServiceUpdateProviders() = this._serviceUpdateProviderProperties

    fun getServiceUpdateProviders(targetAuthority: String) = this._serviceUpdateProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getServiceUpdateProvider(authority: String) = this._serviceUpdateProviderProperties.singleOrNull { it.authority == authority }

    // NEWS

    fun getAllNewsProviders() = this._newsProviderProperties

    fun getNewsProviders(targetAuthority: String) = this._newsProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getNewsProvidersList(targetAuthority: String) = this._newsProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getNewsProvider(authority: String) = this._newsProviderProperties.singleOrNull { it.authority == authority }
}