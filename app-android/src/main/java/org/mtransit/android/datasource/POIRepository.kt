package org.mtransit.android.datasource

import android.location.Location
import androidx.collection.LruCache
import androidx.collection.SimpleArrayMap
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.set
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.updateDistance
import org.mtransit.android.commons.updateDistanceM
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.updateSupportedType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Singleton
class POIRepository(
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val ioDispatcher: CoroutineDispatcher,
) : MTLog.Loggable {

    @Inject
    constructor(
        dataSourceRequestManager: DataSourceRequestManager,
    ) : this(
        dataSourceRequestManager = dataSourceRequestManager,
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG: String = POIRepository::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private val authorityUUIDtoPOIMCache = LruCache<Pair<String, String>, POIManager>(10)

    fun push(newPOIM: POIManager?) {
        newPOIM?.let {
            authorityUUIDtoPOIMCache[it.poi.authority to it.poi.uuid] = it
        }
    }

    private fun read(authority: String, uuid: String): POIManager? {
        return authorityUUIDtoPOIMCache[authority to uuid]
    }

    private fun commonSetup(filter: POIProviderContract.Filter) = filter

    fun readingPOIM(
        agency: IAgencyProperties?,
        uuid: String?,
        currentValue: POIManager? = null,
        onDataSourceRemoved: () -> Unit
    ) = liveData {
        if (agency == null && currentValue != null) {
            MTLog.d(this@POIRepository, "readingPOIM() > SKIP (agency removed)")
            onDataSourceRemoved() // agency removed
            return@liveData // SKIP
        }
        val agency = agency ?: run {
            MTLog.d(this@POIRepository, "readingPOIM() > SKIP (no agency)")
            return@liveData // SKIP
        }
        val uuid = uuid ?: run {
            MTLog.d(this@POIRepository, "readingPOIM() > SKIP (no UUID)")
            return@liveData // SKIP
        }
        val cachePOIM = read(agency.authority, uuid)
            ?.also { cachedPOI ->
                emit(cachedPOI)
            }
        val poiFilter = commonSetup(POIProviderContract.Filter.getNewUUIDFilter(uuid))
        dataSourceRequestManager.findPOIM(agency, poiFilter)
            ?.updateSupportedType(agency)
            ?.let { newPOIMFromModule -> // WITHOUT status OR service update
                val newPOIFromModule = newPOIMFromModule.poi
                if (cachePOIM == null // no cache POI
                    || newPOIFromModule != cachePOIM.poi // new POI != cache POI
                ) {
                    MTLog.d(this@POIRepository, "readingPOIM() > EMIT (new POI != cache POI)")
                    cachePOIM?.serviceUpdatesOrNull?.let { newPOIMFromModule.setServiceUpdates(it) }
                    cachePOIM?.statusOrNull?.let { newPOIMFromModule.setStatus(it) }
                    emit(newPOIMFromModule)
                    push(newPOIMFromModule)
                } else { // ELSE same POI, keep cache w/ extras (status, service update...)
                    MTLog.d(this@POIRepository, "readingPOIM() > SKIP (new POI == cache POI, keep status, service update...)")
                }
            }
            ?: run {
                MTLog.d(this@POIRepository, "readingPOIM() > SKIP (removed from agency)")
                onDataSourceRemoved() // POI removed from agency
                emit(null)
            }
    }.distinctUntilChanged()

    suspend fun findPOIMs(agency: IAgencyProperties, poiFilter: POIProviderContract.Filter): MutableList<POIManager> {
        return dataSourceRequestManager.findPOIMs(agency, commonSetup(poiFilter))
            .updateSupportedType(agency)
    }

    suspend fun findPOIMsAroundLoc(
        agency: IAgencyProperties,
        lat: Double,
        lng: Double,
        aroundDiff: Double,
        avoidLoading: Boolean = false,
        noPickup: Boolean = false
    ): MutableList<POIManager> {
        val poiFilter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, aroundDiff).copy(
            extras = SimpleArrayMap<String, Any>().apply {
                put(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, avoidLoading)
                put(GTFSProviderContract.POI_FILTER_EXTRA_NO_PICKUP, noPickup)
            },
        )
        val maxAroundDiffDistanceInMeters = LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, aroundDiff)
        return findPOIMs(agency, poiFilter)
            .updateDistanceM(lat, lng)
            .removeTooFar(maxAroundDiffDistanceInMeters)
    }

    fun loadingPOIMs(
        typeToProviders: Map<DataSourceType, List<IAgencyProperties>>?,
        filter: POIProviderContract.Filter?,
        deviceLocation: Location? = null,
        comparator: Comparator<POIManager> = compareBy { null },
        typeComparator: Comparator<POIManager?> = compareBy { null },
        let: ((List<POIManager>) -> List<POIManager>?) = { it },
        typeLet: ((List<POIManager>) -> List<POIManager>?) = { it },
        onSuccess: (() -> Unit)? = null,
        context: CoroutineContext = EmptyCoroutineContext,
    ) = liveData(context) {
        if (typeToProviders == null || filter == null) {
            return@liveData // SKIP
        }
        emit(loadPOIMs(typeToProviders, filter, deviceLocation, comparator, typeComparator, let, typeLet, context))
        onSuccess?.invoke()
    }

    suspend fun loadPOIMs(
        typeToProviders: Map<DataSourceType, List<IAgencyProperties>>,
        filter: POIProviderContract.Filter,
        deviceLocation: Location? = null,
        comparator: Comparator<POIManager> = compareBy { null },
        typeComparator: Comparator<POIManager?> = compareBy { null },
        let: ((List<POIManager>) -> List<POIManager>?) = { it },
        letComparator: ((List<POIManager>) -> List<POIManager>?) = { it },
        context: CoroutineContext = ioDispatcher
    ) = withContext(context) {
        typeToProviders
            .map { (_, providers) ->
                async {
                    ensureActive()
                    loadPOIMs(providers, filter, deviceLocation, typeComparator, letComparator, context)
                }
            }
            .awaitAll()
            .filterNotNull()
            .flatten()
            .sortedWith(comparator)
            .let { let.invoke(it) }
    }

    @Suppress("unused")
    fun loadingPOIMs(
        providers: List<IAgencyProperties>?,
        filter: POIProviderContract.Filter?,
        deviceLocation: Location? = null,
        comparator: Comparator<POIManager?> = compareBy { null },
        let: ((List<POIManager>) -> List<POIManager>?) = { it },
        onSuccess: (() -> Unit)? = null,
        context: CoroutineContext = EmptyCoroutineContext,
    ) = liveData(context) {
        providers ?: return@liveData // SKIP
        filter ?: return@liveData // SKIP
        emit(loadPOIMs(providers, filter, deviceLocation, comparator, let, context))
        onSuccess?.invoke()
    }

    suspend fun loadPOIMs(
        providers: List<IAgencyProperties>,
        filter: POIProviderContract.Filter,
        deviceLocation: Location? = null,
        comparator: Comparator<POIManager?>,
        let: ((List<POIManager>) -> List<POIManager>?) = { it },
        context: CoroutineContext = ioDispatcher
    ) = withContext(context) {
        providers
            .map { provider ->
                async {
                    ensureActive()
                    findPOIMs(provider, filter)
                        .updateDistance(deviceLocation)
                }
            }
            .awaitAll()
            .flatten()
            .sortedWith(comparator)
            .let { let.invoke(it) }
    }

    @Suppress("unused")
    suspend fun loadPOIMs(
        agency: IAgencyProperties,
        poiFilter: POIProviderContract.Filter,
        context: CoroutineContext = ioDispatcher
    ) = withContext(context) {
        findPOIMs(agency, poiFilter)
    }
}
