package org.mtransit.android.datasource

import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mtransit.android.common.IApplication
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.AgencyProperties.Companion.SHORT_NAME_COMPARATOR
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.DataSourceType.DataSourceTypeShortNameComparator
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.data.ServiceUpdateProviderProperties
import org.mtransit.android.data.StatusProviderProperties


@Suppress("MemberVisibilityCanBePrivate", "unused")
class DataSourcesRepository(
    private val app: IApplication,
    private val dataSourcesCache: DataSourcesCache,
    private val dataSourcesReader: DataSourcesReader,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DataSourcesRepository::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val defaultAgencyComparator: Comparator<AgencyProperties> = SHORT_NAME_COMPARATOR

    private val defaultDataSourceTypeComparator: Comparator<DataSourceType> by lazy { DataSourceTypeShortNameComparator(app.requireContext()) }

    // IN-MEMORY CACHE
    private var _agencyProperties = listOf<AgencyProperties>() // sorted
    private var _dataSourceTypes = listOf<DataSourceType>() // sorted
    private var _statusProviderProperties = setOf<StatusProviderProperties>() // not sorted
    private var _scheduleProviderProperties = setOf<ScheduleProviderProperties>() // not sorted
    private var _serviceUpdateProviderProperties = setOf<ServiceUpdateProviderProperties>() // not sorted
    private var _newsProviderProperties = setOf<NewsProviderProperties>() // not sorted

    init {
        // START LISTENING FOR CHANGE TO FILL IN-MEMORY CACHE
        startListeningForChangesIntoMemory()
    }

    private fun startListeningForChangesIntoMemory() {
        dataSourcesCache.readingAllAgencies().observeForever { // SINGLETON
            this._agencyProperties = it.sortedWith(defaultAgencyComparator)
        }
        dataSourcesCache.readingAllDataSourceTypes().observeForever { // SINGLETON
            this._dataSourceTypes = it.sortedWith(defaultDataSourceTypeComparator)
        }
        dataSourcesCache.readingAllStatusProviders().observeForever { // SINGLETON
            this._statusProviderProperties = it.toSet() // not sorted
        }
        dataSourcesCache.readingAllScheduleProviders().observeForever { // SINGLETON
            this._scheduleProviderProperties = it.toSet() // not sorted
        }
        dataSourcesCache.readingAllServiceUpdateProviders().observeForever { // SINGLETON
            this._serviceUpdateProviderProperties = it.toSet() // not sorted
        }
        dataSourcesCache.readingAllNewsProviders().observeForever { // SINGLETON
            this._newsProviderProperties = it.toSet() // not sorted
        }
    }

    // AGENCY

    fun getAllAgencies() = this._agencyProperties

    fun readingAllAgencies() = dataSourcesCache.readingAllAgencies().map {
        it.sortedWith(defaultAgencyComparator)
    }

    fun readingAllAgenciesDistinct() = readingAllAgencies().distinctUntilChanged()

    fun readingAllAgencyAuthorities() = readingAllAgencies().map { agencyList ->
        agencyList.map { agency ->
            agency.authority
        }
    }

    fun getAllAgenciesCount() = this._agencyProperties.size

    fun readingAllAgenciesCount() = dataSourcesCache.readingAllAgenciesCount()

    fun getAgency(authority: String) = this._agencyProperties.singleOrNull { it.authority == authority }

    fun getAgencyForPkg(pkg: String) = this._agencyProperties.singleOrNull { it.pkg == pkg }

    fun readingAgency(authority: String) = dataSourcesCache.readingAgency(authority)

    fun getAllDataSourceTypes() = this._dataSourceTypes

    fun readingAllDataSourceTypes() = dataSourcesCache.readingAllDataSourceTypes().map {
        it.sortedWith(defaultDataSourceTypeComparator)
    }

    fun readingAllDataSourceTypesDistinct() = readingAllDataSourceTypes().distinctUntilChanged()

    fun getTypeDataSources(typeId: Int): List<AgencyProperties> = this._agencyProperties.filter { it.type.id == typeId }
        .sortedWith(defaultAgencyComparator)

    fun getTypeDataSources(dst: DataSourceType) = getTypeDataSources(dst.id)

    fun readingTypeDataSources(dst: DataSourceType) = this.dataSourcesCache.readingTypeDataSources(dst).map {
        it.sortedWith(defaultAgencyComparator)
    }

    fun readingTypeDataSourcesDistinct(dst: DataSourceType) = readingTypeDataSources(dst).distinctUntilChanged()

    fun getAgencyPkg(authority: String) = getAgency(authority)?.pkg

    fun getAgencyColorInt(authority: String) = getAgency(authority)?.colorInt

    // STATUS

    fun getAllStatusProviders() = this._statusProviderProperties

    fun getStatusProviders(targetAuthority: String) = this._statusProviderProperties.filterTo(HashSet()) { it.targetAuthority == targetAuthority }

    fun getStatusProvider(authority: String) = this._statusProviderProperties.singleOrNull { it.authority == authority }

    // SCHEDULE

    fun getAllScheduleProviders() = this._scheduleProviderProperties

    fun getScheduleProviders(targetAuthority: String) = this._scheduleProviderProperties.filterTo(HashSet()) { it.targetAuthority == targetAuthority }

    fun getScheduleProvider(authority: String) = this._scheduleProviderProperties.singleOrNull { it.authority == authority }

    // SERVICE UPDATE

    fun getAllServiceUpdateProviders() = this._serviceUpdateProviderProperties

    fun getServiceUpdateProviders(targetAuthority: String) = this._serviceUpdateProviderProperties.filterTo(HashSet()) { it.targetAuthority == targetAuthority }

    fun getServiceUpdateProvider(authority: String) = this._serviceUpdateProviderProperties.singleOrNull { it.authority == authority }

    // NEWS

    fun getAllNewsProviders() = this._newsProviderProperties

    fun readingAllNewsProviders() = dataSourcesCache.readingAllNewsProviders()

    fun readingAllNewsProvidersDistinct() = readingAllNewsProviders().distinctUntilChanged()

    fun getNewsProviders(targetAuthority: String) = this._newsProviderProperties.filterTo(HashSet()) { it.targetAuthority == targetAuthority }

    fun readingAllNewsProvidersDistinct(targetAuthority: String) = dataSourcesCache.readingNewsProviders(targetAuthority)

    fun getNewsProvider(authority: String) = this._newsProviderProperties.singleOrNull { it.authority == authority }

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

    suspend fun forceRefreshAvailableVersions(): Boolean {
        var updated = false
        withContext(Dispatchers.IO) {
            dataSourcesReader.refreshAvailableVersions(force = true) { updated = true }
        }
        return updated
    }

    fun isAProvider(pkg: String?): Boolean {
        return this.dataSourcesReader.isAProvider(pkg)
    }
}