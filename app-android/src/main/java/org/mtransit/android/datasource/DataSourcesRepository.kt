package org.mtransit.android.datasource

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.billing.filterExpansiveAgencies
import org.mtransit.android.billing.filterExpansiveAgencyAuthorities
import org.mtransit.android.billing.filterExpansiveNewsProviders
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.DataSourceType.DataSourceTypeShortNameComparator
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.ITargetedProviderProperties
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.dev.filterDemoModeAgency
import org.mtransit.android.dev.filterDemoModeTargeted
import org.mtransit.android.dev.filterDemoModeType
import org.mtransit.android.dev.takeIfDemoModeAgency
import org.mtransit.android.dev.takeIfDemoModeTargeted
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.commons.addAllNNE
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton


@Suppress("unused", "MemberVisibilityCanBePrivate")
@Singleton
class DataSourcesRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val dataSourcesInMemoryCache: DataSourcesInMemoryCache,
    private val dataSourcesStorage: DataSourcesStorage,
    private val dataSourcesReader: DataSourcesReader,
    private val demoModeManager: DemoModeManager,
    private val billingManager: IBillingManager,
    private val remoteConfigProvider: RemoteConfigProvider,
    private val pm: PackageManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = DataSourcesRepository::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private val defaultAgencies by lazy {
        listOf(
            appContext.getString(R.string.module_authority),
            appContext.getString(R.string.place_authority),
        )
    }

    private val includedAgencyCount: Int get() = defaultAgencies.filterExpansiveAgencyAuthorities(billingManager, remoteConfigProvider).size

    private val defaultAgencyComparator: Comparator<IAgencyProperties> = IAgencyProperties.SHORT_NAME_COMPARATOR

    val defaultDataSourceTypeComparator: Comparator<DataSourceType> by lazy { DataSourceTypeShortNameComparator(appContext) }

    // region AGENCY

    fun getAllAgencies() = this.dataSourcesInMemoryCache.getAllAgencies()

    suspend fun getAllAgenciesNow() = this.dataSourcesStorage.getAllAgencies()

    fun getAllAgenciesByType() = getAllAgencies().groupBy {
        it.getSupportedType()
    }

    fun readingAllAgencies() = this.dataSourcesStorage.readingAllAgencies().map { agencies ->
        agencies
            .filterExpansiveAgencies(billingManager, remoteConfigProvider)
            .filterDemoModeAgency(demoModeManager)
            .sortedWith(defaultAgencyComparator)
    }.distinctUntilChanged()

    fun readingAllAgenciesByType() = readingAllAgencies().map { agencies ->
        agencies.groupBy { it.getSupportedType() }
    }

    fun readingAllAgenciesBase() = this.dataSourcesStorage.readingAllAgenciesBase().map { agencies ->
        agencies
            .filterExpansiveAgencies(billingManager, remoteConfigProvider)
            .filterDemoModeAgency(demoModeManager)
            .sortedWith(defaultAgencyComparator)
    }.distinctUntilChanged()

    fun readingAllAgenciesEnabledCount() = readingAllAgenciesBase().map {
        it.filter { agency -> agency.isEnabled(pm) }.size
    }

    fun getAllAgenciesEnabled() = getAllAgencies().filter { agency -> agency.isEnabled(pm) }

    fun getAllAgenciesEnabledCount() = getAllAgenciesEnabled().size

    fun readingAllAgencyAuthorities() = readingAllAgenciesBase().map { agencyList ->
        agencyList.map { agency -> agency.authority }
    }

    fun getAllAgenciesCount() = getAllAgencies().size

    fun readingAllAgenciesCount() = liveData {
        emit(dataSourcesInMemoryCache.getAllAgencies().size)
        emitSource(readingAllAgencies().map { it.size }) // #onModulesUpdated
    }

    fun getAgency(authority: String) = this.dataSourcesInMemoryCache.getAgency(authority)

    fun getAgencyBase(authority: String) = this.dataSourcesInMemoryCache.getAgencyBase(authority)

    fun getAgencyForPkg(pkg: String) = this.dataSourcesInMemoryCache.getAgencyForPkg(pkg)

    fun readingAgency(authority: String?) = liveData {
        authority?.let {
            emit(dataSourcesInMemoryCache.getAgency(it))
            emitSource(dataSourcesStorage.readingAgency(it).map { agency -> agency.takeIfDemoModeAgency(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun readingAgencyBase(authority: String?) = liveData {
        authority?.let {
            emit(dataSourcesInMemoryCache.getAgencyBase(it))
            emitSource(dataSourcesStorage.readingAgencyBase(it).map { agency -> agency.takeIfDemoModeAgency(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    @Deprecated(message = "Use live data")
    fun getAllSupportedDataSourceTypes() = this.dataSourcesInMemoryCache.getAllSupportedDataSourceTypes()

    @Deprecated(message = "use DemoModeManager directly")
    fun filterDataSourceTypes(dtsList: List<DataSourceType>): List<DataSourceType> {
        return dtsList
            .filterDemoModeType(demoModeManager)
            .sortedWith(defaultDataSourceTypeComparator)
    }

    private fun readingAllSupportedDataSourceTypesIO() = MediatorLiveData2(
        dataSourcesStorage.readingAllNotExtendedDataSourceTypes(),
        dataSourcesStorage.readingAllExtendedDataSourceTypes()
    ).switchMap { (notExtendedDST, extendedDST) ->
        liveData {
            if (notExtendedDST == null || extendedDST == null) return@liveData
            emit(
                notExtendedDST.toMutableList().apply { addAllNNE(extendedDST) }
                    .filterDemoModeType(demoModeManager)
                    .sortedWith(defaultDataSourceTypeComparator)
            )
        }
    }

    fun readingAllSupportedDataSourceTypes() = liveData {
        emit(dataSourcesInMemoryCache.getAllSupportedDataSourceTypes())
        emitSource(readingAllSupportedDataSourceTypesIO()) // #onModulesUpdated
    }.distinctUntilChanged()

    fun hasAgenciesAdded() = getAllAgenciesCount() > includedAgencyCount

    fun readingHasAgenciesAdded() = readingAllAgenciesCount().map { agenciesCount ->
        agenciesCount > includedAgencyCount
    }.distinctUntilChanged()

    fun hasAgenciesEnabled() = getAllAgenciesEnabledCount() > includedAgencyCount

    fun readingHasAgenciesEnabled() = readingAllAgenciesEnabledCount().map { enabledAgenciesCount ->
        enabledAgenciesCount > includedAgencyCount
    }.distinctUntilChanged()

    // endregion

    // region STATUS

    fun getAllStatusProviders() = this.dataSourcesInMemoryCache.getAllStatusProviders()

    fun getStatusProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getStatusProviders(targetAuthority)

    fun readingStatusProviders(targetAuthority: String?) = liveData {
        targetAuthority?.let { providerAuthority ->
            emit(dataSourcesInMemoryCache.getStatusProviders(providerAuthority))
            emitSource(dataSourcesStorage.readingStatusProviders(providerAuthority).map { it.filterDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun getStatusProvider(authority: String) = this.dataSourcesInMemoryCache.getStatusProvider(authority)

    // endregion

    // region SCHEDULE

    fun getAllScheduleProviders() = this.dataSourcesInMemoryCache.getAllScheduleProviders()

    fun getScheduleProviders(targetAuthority: String?) =
        this.dataSourcesInMemoryCache.getScheduleProviders(targetAuthority)

    fun readingScheduleProviders(targetAuthority: String?) = liveData {
        targetAuthority?.let { providerAuthority ->
            emit(dataSourcesInMemoryCache.getScheduleProvidersList(providerAuthority))
            emitSource(dataSourcesStorage.readingScheduleProviders(providerAuthority).map { it.filterDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun getScheduleProvider(authority: String) = this.dataSourcesInMemoryCache.getScheduleProvider(authority)

    fun readingScheduleProvider(authority: String?) = liveData {
        authority?.let { providerAuthority ->
            emit(dataSourcesInMemoryCache.getScheduleProvider(providerAuthority))
            emitSource(dataSourcesStorage.readingScheduleProvider(providerAuthority).map { it.takeIfDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    // endregion

    // region SERVICE UPDATE

    fun getAllServiceUpdateProviders() = this.dataSourcesInMemoryCache.getAllServiceUpdateProviders()

    fun getServiceUpdateProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getServiceUpdateProviders(targetAuthority)

    fun getServiceUpdateProvider(authority: String) = this.dataSourcesInMemoryCache.getServiceUpdateProvider(authority)

    // endregion

    // region VEHICLE LOCATION

    fun getAllVehicleLocationProviders() = this.dataSourcesInMemoryCache.getAllVehicleLocationProviders()

    fun getVehicleLocationProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getVehicleLocationProviders(targetAuthority)

    fun getVehicleLocationProvider(authority: String) = this.dataSourcesInMemoryCache.getVehicleLocationProvider(authority)

    fun readingVehicleLocationProviders(targetAuthority: String?) = liveData {
        targetAuthority ?: return@liveData
        emit(dataSourcesInMemoryCache.getVehicleLocationProvidersList(targetAuthority))
        emitSource(dataSourcesStorage.readingVehicleLocationProviders(targetAuthority).map { it.filterDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
    }.distinctUntilChanged()

    // endregion

    // region TARGETED PROVIDERS

    fun List<ITargetedProviderProperties>.filterEnabled(): List<ITargetedProviderProperties> {
        return this.filter { pm.isAppEnabled(it.pkg) }
    }

    fun Set<ITargetedProviderProperties>.filterEnabled(): Set<ITargetedProviderProperties> {
        return this.filterTo(HashSet()) { pm.isAppEnabled(it.pkg) }
    }

    // endregion

    // region NEWS

    private fun List<NewsProviderProperties>.filterNewsProviders(): List<NewsProviderProperties> = this.filter(filterNewsProviders)

    private fun Set<NewsProviderProperties>.filterNewsProviders(): Set<NewsProviderProperties> = this.filterTo(HashSet(), filterNewsProviders)

    private val filterNewsProviders: (NewsProviderProperties) -> Boolean = {
        (
                !it.authority.contains("news.instagram") // not working
                        // || it.authority == "org.mtransit.android.news.instagram" // DEBUG
                        // || it.authority == "org.mtransit.android.debug.news.instagram" // DEBUG
                )
    }

    fun getAllNewsProviders() = this.dataSourcesInMemoryCache.getAllNewsProviders().filterNewsProviders()

    fun getAllNewsProvidersEnabled() = getAllNewsProviders().filterEnabled()

    fun readingAllNewsProviders() = liveData {
        emit(
            dataSourcesInMemoryCache.getAllNewsProviders().filterNewsProviders()
        )
        emitSource(dataSourcesStorage.readingAllNewsProviders().map { newsProviders ->
            newsProviders
                .filterExpansiveNewsProviders(billingManager, remoteConfigProvider)
                .filterDemoModeTargeted(demoModeManager)
                .filterNewsProviders()
        }) // #onModulesUpdated
    }.distinctUntilChanged()

    fun getNewsProviders(targetAuthority: String) =
        this.dataSourcesInMemoryCache.getNewsProviders(targetAuthority).filterNewsProviders()

    fun readingNewsProviders(targetAuthority: String?) = liveData {
        targetAuthority?.let { providerAuthority ->
            emit(
                dataSourcesInMemoryCache.getNewsProvidersList(providerAuthority).filterNewsProviders()
            )
            emitSource(
                dataSourcesStorage.readingNewsProviders(providerAuthority).map { newsProviders ->
                    newsProviders
                        .filterExpansiveNewsProviders(billingManager, remoteConfigProvider)
                        .filterDemoModeTargeted(demoModeManager)
                        .filterNewsProviders()
                }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun getNewsProvider(authority: String) = this.dataSourcesInMemoryCache.getNewsProvider(authority)

    // endregion

    private val runningUpdate = AtomicBoolean(false)
    private val updateLockMutex = Mutex()

    suspend fun updateLock(forcePkg: String? = null): Boolean {
        MTLog.d(this@DataSourcesRepository, "updateLock($forcePkg)")
        if (runningUpdate.get()) {
            MTLog.d(this@DataSourcesRepository, "updateLock() > SKIP (was running - before sync)")
            return false
        }
        this.updateLockMutex.withLock {
            try {
                runningUpdate.set(true)
                val updated = update(forcePkg)
                MTLog.d(this@DataSourcesRepository, "updateLock() > $updated")
                return updated
            } finally {
                runningUpdate.set(false)
            }
        }
    }

    private suspend fun update(forcePkg: String? = null) = withContext(Dispatchers.IO) {
        MTLog.i(this@DataSourcesRepository, "update() > Updating... ")
        val updated = dataSourcesReader.update(forcePkg)
        MTLog.i(this@DataSourcesRepository, "update() > Updating...  DONE")
        MTLog.d(this@DataSourcesRepository, "update() > $updated")
        updated
    }

    suspend fun refreshAvailableVersions(
        forcePkg: String? = null,
        forceAppUpdateRefresh: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        var updated = false
        dataSourcesReader.refreshAvailableVersions(
            forcePkg = forcePkg,
            skipTimeCheck = true,
            forceAppUpdateRefresh = forceAppUpdateRefresh,
        ) {
            updated = true
        }
        updated
    }

    suspend fun refreshSetupRequired(
        forcePkg: String? = null,
        skipTimeCheck: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        var updated = false
        dataSourcesReader.refreshSetupRequired(
            forcePkg = forcePkg,
            skipTimeCheck = skipTimeCheck,
        ) {
            updated = true
        }
        updated
    }

    fun isAProvider(pkg: String?, agencyOnly: Boolean = false): Boolean {
        return this.dataSourcesReader.isAProvider(pkg, agencyOnly)
    }
}