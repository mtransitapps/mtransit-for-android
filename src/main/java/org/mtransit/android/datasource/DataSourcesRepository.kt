package org.mtransit.android.datasource

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.DataSourceType.DataSourceTypeShortNameComparator
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.dev.filterDemoModeAgency
import org.mtransit.android.dev.filterDemoModeTargeted
import org.mtransit.android.dev.filterDemoModeType
import org.mtransit.android.dev.takeIfDemoModeAgency
import org.mtransit.android.dev.takeIfDemoModeTargeted
import javax.inject.Inject
import javax.inject.Singleton


@Suppress("unused")
@Singleton
class DataSourcesRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dataSourcesInMemoryCache: DataSourcesInMemoryCache,
    private val dataSourcesIOCache: DataSourcesCache, // I/O - DB
    private val dataSourcesReader: DataSourcesReader,
    private val demoModeManager: DemoModeManager,
    private val pm: PackageManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DataSourcesRepository::class.java.simpleName

        const val DEFAULT_AGENCY_COUNT = 2
    }

    override fun getLogTag(): String = LOG_TAG

    private val defaultAgencyComparator: Comparator<IAgencyProperties> = IAgencyProperties.SHORT_NAME_COMPARATOR

    val defaultDataSourceTypeComparator: Comparator<DataSourceType> by lazy { DataSourceTypeShortNameComparator(appContext) }

    // region AGENCY

    fun getAllAgencies() = this.dataSourcesInMemoryCache.getAllAgencies().filterDemoModeAgency(demoModeManager)

    fun readingAllAgencies() = this.dataSourcesIOCache.readingAllAgencies().map {
        it.filterDemoModeAgency(demoModeManager).sortedWith(defaultAgencyComparator)
    }.distinctUntilChanged()

    fun readingAllAgenciesBase() = this.dataSourcesIOCache.readingAllAgenciesBase().map {
        it.filterDemoModeAgency(demoModeManager).sortedWith(defaultAgencyComparator)
    }.distinctUntilChanged()

    fun readingAllAgenciesEnabledCount() = readingAllAgenciesBase().map {
        it.filter { agency -> agency.isEnabled(pm) }.size
    }

    fun getAllAgenciesEnabledCount() =
        getAllAgencies().filter { agency -> agency.isEnabled(pm) }.size

    fun readingAllAgencyAuthorities() = readingAllAgenciesBase().map { agencyList ->
        agencyList.map { agency ->
            agency.authority
        }
    }

    fun getAllAgenciesCount() = getAllAgencies().size

    fun readingAllAgenciesCount() = liveData {
        emit(dataSourcesInMemoryCache.getAllAgencies().filterDemoModeAgency(demoModeManager).size)
        emitSource(dataSourcesIOCache.readingAllAgencies().map { it.filterDemoModeAgency(demoModeManager).size }) // #onModulesUpdated
    }

    fun getAgency(authority: String) = this.dataSourcesInMemoryCache.getAgency(authority).takeIfDemoModeAgency(demoModeManager)

    fun getAgencyBase(authority: String) = this.dataSourcesInMemoryCache.getAgencyBase(authority).takeIfDemoModeAgency(demoModeManager)

    fun getAgencyForPkg(pkg: String) = this.dataSourcesInMemoryCache.getAgencyForPkg(pkg).takeIfDemoModeAgency(demoModeManager)

    fun readingAgency(authority: String?) = liveData {
        authority?.let {
            emit(dataSourcesInMemoryCache.getAgency(it).takeIfDemoModeAgency(demoModeManager))
            emitSource(dataSourcesIOCache.readingAgency(it).map { agency -> agency.takeIfDemoModeAgency(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun readingAgencyBase(authority: String?) = liveData {
        authority?.let {
            emit(dataSourcesInMemoryCache.getAgencyBase(it).takeIfDemoModeAgency(demoModeManager))
            emitSource(dataSourcesIOCache.readingAgencyBase(it).map { agency -> agency.takeIfDemoModeAgency(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    @Deprecated(message = "Use live data")
    fun getAllDataSourceTypes() = this.dataSourcesInMemoryCache.getAllDataSourceTypes().filterDemoModeType(demoModeManager)

    private fun readingAllDataSourceTypesIO() = dataSourcesIOCache.readingAllDataSourceTypes().map {
        it.filterDemoModeType(demoModeManager).sortedWith(defaultDataSourceTypeComparator)
    }

    fun readingAllDataSourceTypes() = liveData {
        emit(dataSourcesInMemoryCache.getAllDataSourceTypes().filterDemoModeType(demoModeManager))
        emitSource(readingAllDataSourceTypesIO().map { it.filterDemoModeType(demoModeManager) }) // #onModulesUpdated
    }.distinctUntilChanged()

    private fun readingTypeDataSourcesIO(dst: DataSourceType) = this.dataSourcesIOCache.readingTypeDataSources(dst).map {
        it.filterDemoModeAgency(demoModeManager).sortedWith(defaultAgencyComparator)
    }

    fun readingTypeDataSources(dst: DataSourceType?) = liveData {
        dst?.let {
            emit(dataSourcesInMemoryCache.getTypeDataSources(it).filterDemoModeAgency(demoModeManager))
            emitSource(readingTypeDataSourcesIO(it).map { agency -> agency.filterDemoModeAgency(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    // endregion

    // region STATUS

    fun getAllStatusProviders() = this.dataSourcesInMemoryCache.getAllStatusProviders().filterDemoModeTargeted(demoModeManager)

    fun getStatusProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getStatusProviders(targetAuthority).filterDemoModeTargeted(demoModeManager)

    fun getStatusProvider(authority: String) = this.dataSourcesInMemoryCache.getStatusProvider(authority).takeIfDemoModeTargeted(demoModeManager)

    // endregion

    // region SCHEDULE

    fun getAllScheduleProviders() = this.dataSourcesInMemoryCache.getAllScheduleProviders().filterDemoModeTargeted(demoModeManager)

    fun getScheduleProviders(targetAuthority: String?) =
        this.dataSourcesInMemoryCache.getScheduleProviders(targetAuthority).filterDemoModeTargeted(demoModeManager)

    fun readingScheduleProviders(targetAuthority: String?) = liveData {
        targetAuthority?.let { providerAuthority ->
            emit(dataSourcesInMemoryCache.getScheduleProvidersList(providerAuthority).filterDemoModeTargeted(demoModeManager))
            emitSource(dataSourcesIOCache.readingScheduleProviders(providerAuthority).map { it.filterDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun getScheduleProvider(authority: String) = this.dataSourcesInMemoryCache.getScheduleProvider(authority).takeIfDemoModeTargeted(demoModeManager)

    fun readingScheduleProvider(authority: String?) = liveData {
        authority?.let { providerAuthority ->
            emit(dataSourcesInMemoryCache.getScheduleProvider(providerAuthority).takeIfDemoModeTargeted(demoModeManager))
            emitSource(dataSourcesIOCache.readingScheduleProvider(providerAuthority).map { it.takeIfDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    // endregion

    // region SERVICE UPDATE

    fun getAllServiceUpdateProviders() = this.dataSourcesInMemoryCache.getAllServiceUpdateProviders().filterDemoModeTargeted(demoModeManager)

    fun getServiceUpdateProviders(targetAuthority: String) =
        this.dataSourcesInMemoryCache.getServiceUpdateProviders(targetAuthority).filterDemoModeTargeted(demoModeManager)

    fun getServiceUpdateProvider(authority: String) = this.dataSourcesInMemoryCache.getServiceUpdateProvider(authority).takeIfDemoModeTargeted(demoModeManager)

    // endregion

    // region NEWS

    private fun List<NewsProviderProperties>.filterTwitter(): List<NewsProviderProperties> {
        return this.filter { !it.authority.contains("news.twitter") }
    }

    private fun Set<NewsProviderProperties>.filterTwitter(): Set<NewsProviderProperties> {
        return this.filterTo(HashSet()) { !it.authority.contains("news.twitter") }
    }

    fun getAllNewsProviders() = this.dataSourcesInMemoryCache.getAllNewsProviders()
        .filterDemoModeTargeted(demoModeManager).filterTwitter()

    fun readingAllNewsProviders() = liveData {
        emit(
            dataSourcesInMemoryCache.getAllNewsProviders()
                .filterDemoModeTargeted(demoModeManager).filterTwitter()
        )
        emitSource(dataSourcesIOCache.readingAllNewsProviders().map {
            it.toSet().filterDemoModeTargeted(demoModeManager).filterTwitter()
        }) // #onModulesUpdated
    }.distinctUntilChanged()

    fun getNewsProviders(targetAuthority: String) =
        this.dataSourcesInMemoryCache.getNewsProviders(targetAuthority)
            .filterDemoModeTargeted(demoModeManager).filterTwitter()

    fun readingNewsProviders(targetAuthority: String?) = liveData {
        targetAuthority?.let { providerAuthority ->
            emit(
                dataSourcesInMemoryCache.getNewsProvidersList(providerAuthority)
                    .filterDemoModeTargeted(demoModeManager).filterTwitter()
            )
            emitSource(
                dataSourcesIOCache.readingNewsProviders(providerAuthority).map {
                    it.filterDemoModeTargeted(demoModeManager).filterTwitter()
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