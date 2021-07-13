package org.mtransit.android.datasource

import androidx.collection.LruCache
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.set
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.POIManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class POIRepository @Inject constructor(
    val dataSourceRequestManager: DataSourceRequestManager
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = POIRepository::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val authorityUUIDtoPOIMCache = LruCache<Pair<String, String>, POIManager>(10)

    fun push(newPOIM: POIManager?) {
        newPOIM?.let {
            authorityUUIDtoPOIMCache[it.poi.authority to it.poi.uuid] = it
        }
    }

    private fun read(authority: String, uuid: String): POIManager? {
        return authorityUUIDtoPOIMCache[authority to uuid]
    }

    fun findPOI(authority: String, poiFilter: POIProviderContract.Filter): POI? {
        return dataSourceRequestManager.findPOI(authority, poiFilter)
    }

    fun findPOIM(authority: String, poiFilter: POIProviderContract.Filter): POIManager? {
        return dataSourceRequestManager.findPOIM(authority, poiFilter)
    }

    fun readingPOIM(
        authority: String?,
        uuid: String?,
        currentValue: POIManager? = null,
        onDataSourceRemoved: () -> Unit
    ): LiveData<POIManager?> = liveData {
        if (authority == null && currentValue != null) {
            MTLog.d(this@POIRepository, "readingPOIM() > SKIP (agency removed)")
            onDataSourceRemoved() // agency removed
        }
        if (authority == null || uuid == null) {
            MTLog.d(this@POIRepository, "readingPOIM() > SKIP (no authority OR no UUID)")
        } else {
            val cachePOIM = read(authority, uuid)
            cachePOIM?.let { emit(it) }
            dataSourceRequestManager.findPOIM(authority, POIProviderContract.Filter.getNewUUIDFilter(uuid))?.let { newPOIM ->
                if (cachePOIM == null // no cache POI
                    || newPOIM.poi != cachePOIM.poi // new POI != cache POI
                ) {
                    emit(newPOIM)
                    push(newPOIM)
                } else { // ELSE same POI, keep cache w/ extras (status...)
                    MTLog.d(this@POIRepository, "readingPOIM() > SKIP (new POI == cache POI, keep extras)")
                }
            } ?: run {
                MTLog.d(this@POIRepository, "readingPOIM() > SKIP (removed from agency)")
                onDataSourceRemoved() // POI removed from agency
                emit(null)
            }
        }
    }.distinctUntilChanged()

    fun findPOIs(authority: String, poiFilter: POIProviderContract.Filter): List<POI>? {
        return dataSourceRequestManager.findPOIs(authority, poiFilter)
    }

    fun findPOIMs(authority: String, poiFilter: POIProviderContract.Filter): List<POIManager>? {
        return dataSourceRequestManager.findPOIMs(authority, poiFilter)
    }
}