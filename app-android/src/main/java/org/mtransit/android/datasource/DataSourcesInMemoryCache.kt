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
import org.mtransit.android.data.VehicleLocationProviderProperties
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.dev.filterDemoModeAgency
import org.mtransit.android.dev.filterDemoModeTargeted
import org.mtransit.android.dev.filterDemoModeType
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.addAllNNE
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("MemberVisibilityCanBePrivate")
@Singleton
class DataSourcesInMemoryCache @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val dataSourcesStorage: DataSourcesStorage,
    private val billingManager: IBillingManager,
    private val remoteConfigProvider: RemoteConfigProvider,
    private val demoModeManager: DemoModeManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = DataSourcesInMemoryCache::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private val defaultAgencyComparator: Comparator<IAgencyProperties> = IAgencyProperties.SHORT_NAME_COMPARATOR

    private val defaultDataSourceTypeComparator: Comparator<DataSourceType> by lazy { DataSourceType.DataSourceTypeShortNameComparator(appContext) }

    private var agencyProperties = listOf<AgencyProperties>() // sorted
    private var agencyBaseProperties = listOf<AgencyBaseProperties>() // sorted
    private var supportedDataSourceTypes = listOf<DataSourceType>() // sorted

    private var statusProviderProperties = listOf<StatusProviderProperties>() // sorted for stability
    private var scheduleProviderProperties = listOf<ScheduleProviderProperties>() // sorted for stability
    private var serviceUpdateProviderProperties = listOf<ServiceUpdateProviderProperties>() // sorted for stability
    private var vehicleLocationProviderProperties = listOf<VehicleLocationProviderProperties>() // sorted for stability
    private var newsProviderProperties = listOf<NewsProviderProperties>() // sorted for stability

    init {
        // START LISTENING FOR CHANGE TO FILL IN-MEMORY CACHE
        startListeningForChangesIntoMemory()
    }

    private fun startListeningForChangesIntoMemory() {
        dataSourcesStorage.readingAllAgencies().observeForever { agencies -> // SINGLETON
            this.agencyProperties = agencies
                .filterDemoModeAgency(demoModeManager)
                .sortedWith(defaultAgencyComparator)
        }
        dataSourcesStorage.readingAllAgenciesBase().observeForever { agencies -> // SINGLETON
            this.agencyBaseProperties = agencies
                .filterDemoModeAgency(demoModeManager)
                .sortedWith(defaultAgencyComparator)
        }
        MediatorLiveData2(
            dataSourcesStorage.readingAllNotExtendedDataSourceTypes(),
            dataSourcesStorage.readingAllExtendedDataSourceTypes(),
        ).observeForever { (notExtendedDST, extendedDST) -> // SINGLETON
            if (notExtendedDST == null || extendedDST == null) return@observeForever
            this.supportedDataSourceTypes = notExtendedDST.toMutableList().apply { addAllNNE(extendedDST) }
                .filterDemoModeType(demoModeManager)
                .sortedWith(defaultDataSourceTypeComparator)
        }
        dataSourcesStorage.readingAllStatusProviders().observeForever { // SINGLETON
            this.statusProviderProperties = it
                .filterDemoModeTargeted(demoModeManager)
        }
        dataSourcesStorage.readingAllScheduleProviders().observeForever { // SINGLETON
            this.scheduleProviderProperties = it
                .filterDemoModeTargeted(demoModeManager)
        }
        dataSourcesStorage.readingAllServiceUpdateProviders().observeForever { // SINGLETON
            this.serviceUpdateProviderProperties = it
                .filterDemoModeTargeted(demoModeManager)
        }
        if (UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) {
            dataSourcesStorage.readingAllVehicleLocationProviders().observeForever { // SINGLETON
                this.vehicleLocationProviderProperties = it
                    .filterDemoModeTargeted(demoModeManager)
            }
        }
        dataSourcesStorage.readingAllNewsProviders().observeForever { newsProviders -> // SINGLETON
            this.newsProviderProperties = newsProviders
                .filterDemoModeTargeted(demoModeManager)
        }
    }

    // AGENCY

    private val filteredAgencyProperties get() = this.agencyProperties.filterExpansiveAgencies(billingManager, remoteConfigProvider)

    fun getAllAgencies() = this.filteredAgencyProperties

    fun getAgency(authority: String) = this.filteredAgencyProperties.singleOrNull { it.authority == authority }

    private val filteredAgencyBaseProperties get() = this.agencyBaseProperties.filterExpansiveAgencies(billingManager, remoteConfigProvider)

    fun getAgencyBase(authority: String) = this.filteredAgencyBaseProperties.singleOrNull { it.authority == authority }

    fun getAgencyForPkg(pkg: String) = this.filteredAgencyProperties.singleOrNull { it.pkg == pkg }

    fun getAllSupportedDataSourceTypes() = this.supportedDataSourceTypes

    // STATUS

    fun getAllStatusProviders() = this.statusProviderProperties

    fun getStatusProviders(targetAuthority: String) = this.statusProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getStatusProvider(authority: String) = this.statusProviderProperties.singleOrNull { it.authority == authority }

    // SCHEDULE

    fun getAllScheduleProviders() = this.scheduleProviderProperties

    fun getScheduleProviders(targetAuthority: String?) = this.scheduleProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getScheduleProvidersList(targetAuthority: String?) = this.scheduleProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getScheduleProvider(authority: String) = this.scheduleProviderProperties.singleOrNull { it.authority == authority }

    // SERVICE UPDATE

    fun getAllServiceUpdateProviders() = this.serviceUpdateProviderProperties

    fun getServiceUpdateProviders(targetAuthority: String) = this.serviceUpdateProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getServiceUpdateProvider(authority: String) = this.serviceUpdateProviderProperties.singleOrNull { it.authority == authority }

    // VEHICLE LOCATION

    fun getAllVehicleLocationProviders() = this.vehicleLocationProviderProperties

    fun getVehicleLocationProviders(targetAuthority: String) = this.vehicleLocationProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getVehicleLocationProvidersList(targetAuthority: String) = this.vehicleLocationProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getVehicleLocationProvider(authority: String) = this.vehicleLocationProviderProperties.singleOrNull { it.authority == authority }

    // NEWS

    private val filteredNewsProviderProperties get() = this.newsProviderProperties.filterExpansiveNewsProviders(billingManager, remoteConfigProvider)

    fun getAllNewsProviders() = this.filteredNewsProviderProperties

    fun getNewsProviders(targetAuthority: String) = this.filteredNewsProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getNewsProvidersList(targetAuthority: String) = this.filteredNewsProviderProperties.filter { it.targetAuthority == targetAuthority }

    fun getNewsProvider(authority: String) = this.filteredNewsProviderProperties.singleOrNull { it.authority == authority }
}
