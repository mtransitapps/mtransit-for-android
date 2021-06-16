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
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DataSourcesRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dataSourcesInMemoryCache: DataSourcesInMemoryCache,
    private val dataSourcesIOCache: DataSourcesCache, // I/O - DB
    private val dataSourcesReader: DataSourcesReader,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DataSourcesRepository::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val defaultAgencyComparator: Comparator<IAgencyProperties> = IAgencyProperties.SHORT_NAME_COMPARATOR

    val defaultDataSourceTypeComparator: Comparator<DataSourceType> by lazy { DataSourceTypeShortNameComparator(appContext) }

    // AGENCY

    fun getAllAgencies() = this.dataSourcesInMemoryCache.getAllAgencies()

    fun readingAllAgencies() = dataSourcesIOCache.readingAllAgencies().map {
        it.sortedWith(defaultAgencyComparator)
    }.distinctUntilChanged()

    fun readingAllAgenciesBase() = dataSourcesIOCache.readingAllAgenciesBase().map {
        it.sortedWith(defaultAgencyComparator)
    }.distinctUntilChanged()

    fun readingAllAgencyAuthorities() = readingAllAgencies().map { agencyList ->
        agencyList.map { agency ->
            agency.authority
        }
    }

    @Deprecated(message = "Use live data")
    fun getAllAgenciesCount() = this.dataSourcesInMemoryCache.getAllAgencies().size

    fun readingAllAgenciesCount() = liveData {
        emit(dataSourcesInMemoryCache.getAllAgencies().size)
        emitSource(dataSourcesIOCache.readingAllAgenciesCount()) // #onModulesUpdated
    }

    fun getAgency(authority: String) = dataSourcesInMemoryCache.getAgency(authority)

    fun getAgencyBase(authority: String) = dataSourcesInMemoryCache.getAgencyBase(authority)

    fun getAgencyForPkg(pkg: String) = dataSourcesInMemoryCache.getAgencyForPkg(pkg)

    fun readingAgency(authority: String?) = liveData {
        authority?.let {
            emit(dataSourcesInMemoryCache.getAgency(it))
            emitSource(dataSourcesIOCache.readingAgency(it)) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun readingAgencyBase(authority: String?) = liveData {
        authority?.let {
            emit(dataSourcesInMemoryCache.getAgencyBase(it))
            emitSource(dataSourcesIOCache.readingAgencyBase(it)) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    @Deprecated(message = "Use live data")
    fun getAllDataSourceTypes() = this.dataSourcesInMemoryCache.getAllDataSourceTypes()

    private fun readingAllDataSourceTypesIO() = dataSourcesIOCache.readingAllDataSourceTypes().map {
        it.sortedWith(defaultDataSourceTypeComparator)
    }

    fun readingAllDataSourceTypes() = liveData {
        emit(dataSourcesInMemoryCache.getAllDataSourceTypes())
        emitSource(readingAllDataSourceTypesIO()) // #onModulesUpdated
    }.distinctUntilChanged()

    private fun readingTypeDataSourcesIO(dst: DataSourceType) = this.dataSourcesIOCache.readingTypeDataSources(dst).map {
        it.sortedWith(defaultAgencyComparator)
    }

    fun readingTypeDataSources(dst: DataSourceType?) = liveData {
        dst?.let {
            emit(dataSourcesInMemoryCache.getTypeDataSources(it))
            emitSource(readingTypeDataSourcesIO(it)) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    // STATUS

    fun getAllStatusProviders() = this.dataSourcesInMemoryCache.getAllStatusProviders()

    fun getStatusProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getStatusProviders(targetAuthority)

    fun getStatusProvider(authority: String) = this.dataSourcesInMemoryCache.getStatusProvider(authority)

    // SCHEDULE

    fun getAllScheduleProviders() = this.dataSourcesInMemoryCache.getAllScheduleProviders()

    fun getScheduleProviders(targetAuthority: String?) = this.dataSourcesInMemoryCache.getScheduleProviders(targetAuthority)

    fun readingScheduleProviders(targetAuthority: String?) = liveData {
        targetAuthority?.let {
            emit(dataSourcesInMemoryCache.getScheduleProvidersList(it))
            emitSource(dataSourcesIOCache.readingScheduleProviders(it)) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    fun getScheduleProvider(authority: String) = this.dataSourcesInMemoryCache.getScheduleProvider(authority)

    fun readingScheduleProvider(authority: String?) = liveData {
        authority?.let {
            emit(dataSourcesInMemoryCache.getScheduleProvider(it))
            emitSource(dataSourcesIOCache.readingScheduleProvider(it)) // #onModulesUpdated
        }
    }.distinctUntilChanged()

    // SERVICE UPDATE

    fun getAllServiceUpdateProviders() = this.dataSourcesInMemoryCache.getAllServiceUpdateProviders()

    fun getServiceUpdateProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getServiceUpdateProviders(targetAuthority)

    fun getServiceUpdateProvider(authority: String) = this.dataSourcesInMemoryCache.getServiceUpdateProvider(authority)

    // NEWS

    fun getAllNewsProviders() = this.dataSourcesInMemoryCache.getAllNewsProviders()

    fun readingAllNewsProviders() = liveData {
        emit(dataSourcesInMemoryCache.getAllNewsProviders().toList())
        emitSource(dataSourcesIOCache.readingAllNewsProviders()) // #onModulesUpdated
    }.distinctUntilChanged()

    fun getNewsProviders(targetAuthority: String) = this.dataSourcesInMemoryCache.getNewsProviders(targetAuthority)

    fun readingNewsProviders(targetAuthority: String?) = liveData {
        targetAuthority?.let {
            emit(dataSourcesInMemoryCache.getNewsProvidersList(it))
            emitSource(dataSourcesIOCache.readingNewsProviders(it)) // #onModulesUpdated
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