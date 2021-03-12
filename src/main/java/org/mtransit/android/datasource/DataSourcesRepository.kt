package org.mtransit.android.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataSourcesRepository(
    private val dataSourcesCache: DataSourcesCache,
    private val dataSourcesReader: DataSourcesReader,
) {

    val allAgencies = dataSourcesCache.allAgencies

    suspend fun update() {
        withContext(Dispatchers.IO) {
            dataSourcesReader.update()
        }
    }
}