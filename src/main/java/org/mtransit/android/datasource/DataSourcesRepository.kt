package org.mtransit.android.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import java.util.concurrent.CompletableFuture

class DataSourcesRepository(
    private val dataSourcesCache: DataSourcesCache,
    private val dataSourcesReader: DataSourcesReader,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = DataSourcesRepository::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    val allAgencies = dataSourcesCache.allAgencies

    fun updateAsync(): CompletableFuture<Unit> { // JAVA
        return GlobalScope.future {
            update()
        }
    }

    suspend fun update(): Boolean {
        var updated: Boolean
        withContext(Dispatchers.IO) {
            updated = dataSourcesReader.update()
        }
        return updated
    }
}