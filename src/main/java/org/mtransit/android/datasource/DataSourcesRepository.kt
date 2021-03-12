package org.mtransit.android.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture

class DataSourcesRepository(
    private val dataSourcesCache: DataSourcesCache,
    private val dataSourcesReader: DataSourcesReader,
) {

    val allAgencies = dataSourcesCache.allAgencies

    fun updateAsync(): CompletableFuture<Unit> { // JAVA
        return GlobalScope.future {
            update()
        }
    }

    suspend fun update() {
        withContext(Dispatchers.IO) {
            dataSourcesReader.update()
        }
    }
}