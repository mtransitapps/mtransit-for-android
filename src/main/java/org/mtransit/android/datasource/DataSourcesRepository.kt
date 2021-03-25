package org.mtransit.android.datasource

import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
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
import org.mtransit.commons.FeatureFlags.F_CACHE_DATA_SOURCES
import java.util.concurrent.CompletableFuture

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
        if (F_CACHE_DATA_SOURCES) { // no-op injection <= WIP
            // START LISTENING FOR CHANGE TO FILL IN-MEMORY CACHE
            startListeningForChangesIntoMemory()
        }
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

    fun getAllAgenciesCount() = this._agencyProperties.size

    fun readingAllAgenciesCount() = dataSourcesCache.readingAllAgenciesCount()

    fun getAgency(authority: String) = this._agencyProperties.singleOrNull { it.authority == authority }

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

    fun getNewsProviders(targetAuthority: String) = this._newsProviderProperties.filterTo(HashSet()) { it.targetAuthority == targetAuthority }

    fun getNewsProvider(authority: String) = this._newsProviderProperties.singleOrNull { it.authority == authority }

    private var runningUpdate: Boolean = false

    @JvmOverloads
    fun updateAsync(force: Boolean = false): CompletableFuture<Boolean> { // JAVA
        if (!F_CACHE_DATA_SOURCES) {
            return GlobalScope.future { false }
        }
        if (runningUpdate) {
            MTLog.d(this@DataSourcesRepository, "updateAsync() > SKIP (was running - before sync)")
            return GlobalScope.future { false }
        }
        synchronized(this) {
            if (runningUpdate) {
                MTLog.d(this@DataSourcesRepository, "updateAsync() > SKIP (was running - in sync)")
                return GlobalScope.future { false }
            }
            return GlobalScope.future {
                var updated = false
                if (!runningUpdate) {
                    runningUpdate = true
                    updated = update()
                    runningUpdate = false
                } else {
                    MTLog.d(this@DataSourcesRepository, "updateAsync() > SKIP (was running - in future)")
                }
                updated
            }
        }
    }

    suspend fun update(): Boolean {
        if (!F_CACHE_DATA_SOURCES) {
            return false
        }
        var updated: Boolean
        withContext(Dispatchers.IO) {
            updated = dataSourcesReader.update()
        }
        return updated
    }

    fun isAProvider(pkg: String): Boolean {
        if (!F_CACHE_DATA_SOURCES) {
            return false
        }
        return this.dataSourcesReader.isAProvider(pkg)
    }
}