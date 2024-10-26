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
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.commons.addAllNNE
import javax.inject.Inject
import javax.inject.Singleton


@Suppress("unused", "MemberVisibilityCanBePrivate")
@Singleton
class DataSourcesRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dataSourcesInMemoryCache: DataSourcesInMemoryCache,
    private val dataSourcesIOCache: DataSourcesCache, // I/O - DB
    private val dataSourcesReader: DataSourcesReader,
    private val demoModeManager: DemoModeManager,
    private val billingManager: IBillingManager,
    private val pm: PackageManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DataSourcesRepository::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val defaultAgencies by lazy {
        listOf(
            appContext.getString(R.string.module_authority),
            appContext.getString(R.string.place_authority),
        )
    }

    private val includedAgencyCount: Int
        get() {
            return defaultAgencies.filterExpansiveAgencyAuthorities(billingManager).size
        }

    private val defaultAgencyComparator: Comparator<IAgencyProperties> = IAgencyProperties.SHORT_NAME_COMPARATOR

    val defaultDataSourceTypeComparator: Comparator<DataSourceType> by lazy { DataSourceTypeShortNameComparator(appContext) }

    // region AGENCY

    fun getAllAgencies() = this.dataSourcesInMemoryCache.getAllAgencies()

    fun getAllTypeToAgencies() = getAllAgencies().groupBy {
        it.getSupportedType()
    }

    fun readingAllAgencies() = this.dataSourcesIOCache.readingAllAgencies().map { agencies ->
        agencies
            .filterExpansiveAgencies(billingManager)
            .filterDemoModeAgency(demoModeManager)
            .sortedWith(defaultAgencyComparator)
    }.distinctUntilChanged()

    fun readingAllAgenciesBase() = this.dataSourcesIOCache.readingAllAgenciesBase().map { agencies ->
        agencies
            .filterExpansiveAgencies(billingManager)
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
            emitSource(dataSourcesIOCache.readingAgency(it).map { agency -> agency.takeIfDemoModeAgency(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun readingAgencyBase(authority: String?) = liveData {
        authority?.let {
            emit(dataSourcesInMemoryCache.getAgencyBase(it))
            emitSource(dataSourcesIOCache.readingAgencyBase(it).map { agency -> agency.takeIfDemoModeAgency(demoModeManager) }) // #onModulesUpdated
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

    private fun readingAllSupportedDataSourceTypesIO() = PairMediatorLiveData(
        dataSourcesIOCache.readingAllNotExtendedDataSourceTypes(),
        dataSourcesIOCache.readingAllExtendedDataSourceTypes()
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

    fun getStatusProvider(authority: String) = this.dataSourcesInMemoryCache.getStatusProvider(authority)

    // endregion

    // region SCHEDULE

    fun getAllScheduleProviders() = this.dataSourcesInMemoryCache.getAllScheduleProviders()

    fun getScheduleProviders(targetAuthority: String?) =
        this.dataSourcesInMemoryCache.getScheduleProviders(targetAuthority)

    fun readingScheduleProviders(targetAuthority: String?) = liveData {
        targetAuthority?.let { providerAuthority ->
            emit(dataSourcesInMemoryCache.getScheduleProvidersList(providerAuthority))
            emitSource(dataSourcesIOCache.readingScheduleProviders(providerAuthority).map { it.filterDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun getScheduleProvider(authority: String) = this.dataSourcesInMemoryCache.getScheduleProvider(authority)

    fun readingScheduleProvider(authority: String?) = liveData {
        authority?.let { providerAuthority ->
            emit(dataSourcesInMemoryCache.getScheduleProvider(providerAuthority))
            emitSource(dataSourcesIOCache.readingScheduleProvider(providerAuthority).map { it.takeIfDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    // endregion

    // region SERVICE UPDATE

    fun getAllServiceUpdateProviders() = this.dataSourcesInMemoryCache.getAllServiceUpdateProviders()

    fun getServiceUpdateProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getServiceUpdateProviders(targetAuthority)

    fun getServiceUpdateProvider(authority: String) = this.dataSourcesInMemoryCache.getServiceUpdateProvider(authority)

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
                !it.authority.contains("news.instagram")
                        || it.authority == "org.mtransit.android.news.instagram"
                        || it.authority == "org.mtransit.android.debug.news.instagram"
                )
    }

    fun getAllNewsProviders() = this.dataSourcesInMemoryCache.getAllNewsProviders().filterNewsProviders()

    fun getAllNewsProvidersEnabled() = getAllNewsProviders().filterEnabled()

    fun readingAllNewsProviders() = liveData {
        emit(
            dataSourcesInMemoryCache.getAllNewsProviders().filterNewsProviders()
        )
        emitSource(dataSourcesIOCache.readingAllNewsProviders().map { newsProviders ->
            newsProviders
                .filterExpansiveNewsProviders(billingManager)
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
                dataSourcesIOCache.readingNewsProviders(providerAuthority).map { newsProviders ->
                    newsProviders
                        .filterExpansiveNewsProviders(billingManager)
                        .filterDemoModeTargeted(demoModeManager)
                        .filterNewsProviders()
                }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun getNewsProvider(authority: String) = this.dataSourcesInMemoryCache.getNewsProvider(authority)

    private var runningUpdate: Boolean = false

    private val mutex = Mutex()

    @JvmOverloads
    suspend fun updateLock(forcePkg: String? = null): Boolean {
        if (runningUpdate) {
            MTLog.d(this@DataSourcesRepository, "updateLock() > SKIP (was running - before sync)")
            return false
        }
        this.mutex.withLock {
            if (runningUpdate) {
                MTLog.d(this@DataSourcesRepository, "updateLock() > SKIP (was running - in lock)")
                return false
            }
            runningUpdate = true
            val updated: Boolean = update(forcePkg)
            runningUpdate = false
            MTLog.d(this@DataSourcesRepository, "updateLock() > $updated")
            return updated
        }
    }

    private suspend fun update(forcePkg: String? = null): Boolean {
        var updated: Boolean
        withContext(Dispatchers.IO) {
            MTLog.i(this@DataSourcesRepository, "update() > Updating... ")
            updated = dataSourcesReader.update(forcePkg)
            MTLog.i(this@DataSourcesRepository, "update() > Updating...  DONE")
        }
        MTLog.d(this, "update() > $updated")
        return updated
    }

    suspend fun refreshAvailableVersions(forcePkg: String? = null, forceAppUpdateRefresh: Boolean = false): Boolean {
        var updated = false
        withContext(Dispatchers.IO) {
            dataSourcesReader.refreshAvailableVersions(
                forcePkg = forcePkg,
                skipTimeCheck = true,
                forceAppUpdateRefresh = forceAppUpdateRefresh,
            ) {
                updated = true
            }
        }
        return updated
    }

    fun isAProvider(pkg: String?): Boolean {
        return this.dataSourcesReader.isAProvider(pkg)
    }

    // endregion
}