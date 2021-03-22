package org.mtransit.android.datasource

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

    // AGENCY

    fun getAllAgencies() = dataSourcesCache.getAllAgencies()
        .sortedWith(defaultAgencyComparator)

    fun readingAllAgencies() = dataSourcesCache.readingAllAgencies().map {
        it.sortedWith(defaultAgencyComparator)
    }

    fun getAllAgenciesCount() = dataSourcesCache.getAllAgenciesCount()

    fun readingAllAgenciesCount() = dataSourcesCache.readingAllAgenciesCount()

    fun getAgency(authority: String) = dataSourcesCache.getAgency(authority)

    fun getAllDataSourceTypes() = dataSourcesCache.getAllDataSourceTypes()
        .sortedWith(defaultDataSourceTypeComparator)

    fun readingAllDataSourceTypes() = dataSourcesCache.readingAllDataSourceTypes().map {
        it.sortedWith(defaultDataSourceTypeComparator)
    }

    fun getTypeDataSources(dst: DataSourceType): List<AgencyProperties> = dataSourcesCache.getTypeDataSources(dst)
        .sortedWith(defaultAgencyComparator)

    fun getAgencyPkg(authority: String) = dataSourcesCache.getAgencyPkg(authority)

    fun getAgencyColorInt(authority: String) = dataSourcesCache.getAgencyColorInt(authority)

    // STATUS

    fun getAllStatusProviders() = dataSourcesCache.getAllStatusProviders()

    fun getStatusProviders(targetAuthority: String) = dataSourcesCache.getStatusProviders(targetAuthority)

    fun getStatusProvider(authority: String) = dataSourcesCache.getStatusProvider(authority)

    // SCHEDULE

    fun getAllScheduleProviders() = dataSourcesCache.getAllScheduleProviders()

    fun getScheduleProviders(targetAuthority: String) = dataSourcesCache.getScheduleProviders(targetAuthority)

    fun getScheduleProvider(authority: String) = dataSourcesCache.getScheduleProvider(authority)

    // SERVICE UPDATE

    fun getAllServiceUpdateProviders() = dataSourcesCache.getAllServiceUpdateProviders()

    fun getServiceUpdateProviders(targetAuthority: String) = dataSourcesCache.getServiceUpdateProviders(targetAuthority)

    fun getServiceUpdateProvider(authority: String) = dataSourcesCache.getServiceUpdateProvider(authority)

    // NEWS

    fun getAllNewsProviders() = dataSourcesCache.getAllNewsProviders()

    fun getNewsProviders(targetAuthority: String) = dataSourcesCache.getNewsProviders(targetAuthority)

    fun getNewsProvider(authority: String) = dataSourcesCache.getNewsProvider(authority)

    private var runningUpdate: Boolean = false

    @JvmOverloads
    fun updateAsync(force: Boolean = false): CompletableFuture<Boolean> { // JAVA
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
        var updated: Boolean
        withContext(Dispatchers.IO) {
            updated = dataSourcesReader.update()
        }
        return updated
    }

    fun isProvider(pkg: String): Boolean {
        return this.dataSourcesReader.isProvider(pkg)
    }
}