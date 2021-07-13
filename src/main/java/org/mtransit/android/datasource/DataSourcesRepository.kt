package org.mtransit.android.datasource

import android.content.Context
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.DataSourceType.DataSourceTypeShortNameComparator
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.dev.filterDemoModeAgency
import org.mtransit.android.dev.filterDemoModeTargeted
import org.mtransit.android.dev.filterDemoModeType
import org.mtransit.android.dev.takeIfDemoModeAgency
import org.mtransit.android.dev.takeIfDemoModeTargeted
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DataSourcesRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dataSourcesInMemoryCache: DataSourcesInMemoryCache,
    private val dataSourcesIOCache: DataSourcesCache, // I/O - DB
    private val dataSourcesReader: DataSourcesReader,
    private val demoModeManager: DemoModeManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DataSourcesRepository::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val defaultAgencyComparator: Comparator<IAgencyProperties> = IAgencyProperties.SHORT_NAME_COMPARATOR

    val defaultDataSourceTypeComparator: Comparator<DataSourceType> by lazy { DataSourceTypeShortNameComparator(appContext) }

    // AGENCY

    fun getAllAgencies() = this.dataSourcesInMemoryCache.getAllAgencies().filterDemoModeAgency(demoModeManager)

    fun readingAllAgencies() = this.dataSourcesIOCache.readingAllAgencies().map {
        it.filterDemoModeAgency(demoModeManager).sortedWith(defaultAgencyComparator)
    }.distinctUntilChanged()

    fun readingAllAgenciesBase() = this.dataSourcesIOCache.readingAllAgenciesBase().map {
        it.filterDemoModeAgency(demoModeManager).sortedWith(defaultAgencyComparator)
    }.distinctUntilChanged()

    fun readingAllAgencyAuthorities() = readingAllAgencies().map { agencyList ->
        agencyList.map { agency ->
            agency.authority
        }
    }

    @Deprecated(message = "Use live data")
    fun getAllAgenciesCount() = this.dataSourcesInMemoryCache.getAllAgencies().filterDemoModeAgency(demoModeManager).size

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

    // STATUS

    fun getAllStatusProviders() = this.dataSourcesInMemoryCache.getAllStatusProviders().filterDemoModeTargeted(demoModeManager)

    fun getStatusProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getStatusProviders(targetAuthority).filterDemoModeTargeted(demoModeManager)

    fun getStatusProvider(authority: String) = this.dataSourcesInMemoryCache.getStatusProvider(authority).takeIfDemoModeTargeted(demoModeManager)

    // SCHEDULE

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

    // SERVICE UPDATE

    fun getAllServiceUpdateProviders() = this.dataSourcesInMemoryCache.getAllServiceUpdateProviders().filterDemoModeTargeted(demoModeManager)

    fun getServiceUpdateProviders(targetAuthority: String) =
        this.dataSourcesInMemoryCache.getServiceUpdateProviders(targetAuthority).filterDemoModeTargeted(demoModeManager)

    fun getServiceUpdateProvider(authority: String) = this.dataSourcesInMemoryCache.getServiceUpdateProvider(authority).takeIfDemoModeTargeted(demoModeManager)

    // NEWS

    fun getAllNewsProviders() = this.dataSourcesInMemoryCache.getAllNewsProviders().filterDemoModeTargeted(demoModeManager)

    fun readingAllNewsProviders() = liveData {
        emit(dataSourcesInMemoryCache.getAllNewsProviders().filterDemoModeTargeted(demoModeManager).toList())
        emitSource(dataSourcesIOCache.readingAllNewsProviders().map { it.filterDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
    }.distinctUntilChanged()

    fun getNewsProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getNewsProviders(targetAuthority).filterDemoModeTargeted(demoModeManager)

    fun readingNewsProviders(targetAuthority: String?) = liveData {
        targetAuthority?.let { providerAuthority ->
            emit(dataSourcesInMemoryCache.getNewsProvidersList(providerAuthority).filterDemoModeTargeted(demoModeManager))
            emitSource(dataSourcesIOCache.readingNewsProviders(providerAuthority).map { it.filterDemoModeTargeted(demoModeManager) }) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun getNewsProvider(authority: String) = this.dataSourcesInMemoryCache.getNewsProvider(authority)

    private var runningUpdate: Boolean = false

    val mutex = Mutex()

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

    suspend fun refreshAvailableVersions(forceAppUpdateRefresh: Boolean = false): Boolean {
        var updated = false
        withContext(Dispatchers.IO) {
            dataSourcesReader.refreshAvailableVersions(skipTimeCheck = true, forceAppUpdateRefresh = forceAppUpdateRefresh) { updated = true }
        }
        return updated
    }

    fun isAProvider(pkg: String?): Boolean {
        return this.dataSourcesReader.isAProvider(pkg)
    }
}