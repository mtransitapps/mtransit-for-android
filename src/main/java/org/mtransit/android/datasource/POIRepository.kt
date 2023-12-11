package org.mtransit.android.datasource

import android.location.Location
import androidx.collection.LruCache
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.set
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.updateDistance
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("unused", "MemberVisibilityCanBePrivate")
@Singleton
class POIRepository(
    val dataSourceRequestManager: DataSourceRequestManager,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : MTLog.Loggable {

    @Inject
    constructor(
        dataSourceRequestManager: DataSourceRequestManager,
        lclPrefRepository: LocalPreferenceRepository,
    ) : this(
        dataSourceRequestManager,
        lclPrefRepository,
        Dispatchers.IO,
    )

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

    private fun commonSetup(filter: POIProviderContract.Filter): POIProviderContract.Filter {
        if (FeatureFlags.F_USE_ROUTE_TYPE_FILTER) {
            filter.excludeBookingRequired = lclPrefRepository.getValue(
                LocalPreferenceRepository.PREF_LCL_HIDE_BOOKING_REQUIRED, LocalPreferenceRepository.PREF_LCL_HIDE_BOOKING_REQUIRED_DEFAULT
            )
        }
        return filter
    }

    suspend fun findPOI(authority: String, poiFilter: POIProviderContract.Filter): POI? {
        return dataSourceRequestManager.findPOI(authority, commonSetup(poiFilter))
    }

    suspend fun findPOIM(authority: String, poiFilter: POIProviderContract.Filter): POIManager? {
        return dataSourceRequestManager.findPOIM(authority, commonSetup(poiFilter))
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
                ?.also { emit(it) }
            dataSourceRequestManager.findPOIM(authority, commonSetup(POIProviderContract.Filter.getNewUUIDFilter(uuid)))?.let { newPOIM ->
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

    suspend fun findPOIs(authority: String, poiFilter: POIProviderContract.Filter): List<POI>? {
        return dataSourceRequestManager.findPOIs(authority, commonSetup(poiFilter))
    }

    suspend fun findPOIMs(provider: IAgencyProperties, poiFilter: POIProviderContract.Filter) = findPOIMs(provider.authority, poiFilter)

    suspend fun findPOIMs(authority: String, poiFilter: POIProviderContract.Filter): MutableList<POIManager>? {
        return dataSourceRequestManager.findPOIMs(authority, commonSetup(poiFilter))
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

    fun loadingPOIMs(
        providers: List<IAgencyProperties>?,
        filter: POIProviderContract.Filter?,
        deviceLocation: Location? = null,
        comparator: Comparator<POIManager?> = compareBy { null },
        let: ((List<POIManager>) -> List<POIManager>?) = { it },
        onSuccess: (() -> Unit)? = null,
        context: CoroutineContext = EmptyCoroutineContext,
    ) = liveData(context) {
        if (providers == null || filter == null) {
            return@liveData // SKIP
        }
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
                        ?.updateDistance(deviceLocation)
                }
            }
            .awaitAll()
            .filterNotNull()
            .flatten()
            .sortedWith(comparator)
            .let { let.invoke(it) }
    }

    suspend fun loadPOIMs(
        agency: IAgencyProperties,
        poiFilter: POIProviderContract.Filter,
        context: CoroutineContext = ioDispatcher
    ) = withContext(context) {
        findPOIMs(agency.authority, poiFilter).orEmpty()
    }
}