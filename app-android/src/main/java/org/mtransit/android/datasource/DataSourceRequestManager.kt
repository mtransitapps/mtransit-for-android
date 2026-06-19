package org.mtransit.android.datasource

import android.content.Context
import androidx.annotation.Discouraged
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.android.commons.provider.news.NewsProviderContract
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.commons.provider.scheduletimestamp.ScheduleTimestampsProviderContract
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract
import org.mtransit.android.commons.provider.status.StatusProviderContract
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProviderContract
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceManager
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.JPaths
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.VehicleLocationProviderProperties
import org.mtransit.android.util.KeysManager
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.FeatureFlags
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSourceRequestManager(
    private val appContext: Context,
    private val keysManager: KeysManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MTLog.Loggable {

    @Inject
    constructor(
        @ApplicationContext appContext: Context,
        keysManager: KeysManager,
    ) : this(
        appContext,
        keysManager,
        Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG: String = DataSourceRequestManager::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private val providersAuthorityRunningNotCacheOnly = ConcurrentHashMap<String, Mutex>()

    private suspend fun <T> CoroutineScope.gateNotCacheOnly(authority: String, filter: ProviderContract.Filter, doWork: suspend () -> T): T {
        if (!filter.isCacheOnlyOrDefault) {
            providersAuthorityRunningNotCacheOnly.getOrPut(authority) { Mutex() }.withLock {
                ensureActive()
                return doWork()
            }
        } else {
            return doWork()
        }
    }

    private fun <T> gateNotCacheOnlySync(authority: String, filter: ProviderContract.Filter, isCancelled: () -> Boolean, doWork: () -> T): T? {
        if (!filter.isCacheOnlyOrDefault) {
            synchronized(providersAuthorityRunningNotCacheOnly.getOrPut(authority) { Mutex() }) {
                if (isCancelled()) return null
                return doWork()
            }
        } else {
            return doWork()
        }
    }

    suspend fun ping(agencyAuthority: String) = withContext(ioDispatcher) {
        DataSourceManager.ping(appContext, agencyAuthority)
    }

    suspend fun findPOI(authority: String, poiFilter: POIProviderContract.Filter): POI? = withContext(ioDispatcher) {
        DataSourceManager.findPOI(appContext, authority, poiFilter)?.poi
    }

    suspend fun findPOIM(authority: String, poiFilter: POIProviderContract.Filter): POIManager? = withContext(ioDispatcher) {
        DataSourceManager.findPOI(appContext, authority, poiFilter)
    }

    suspend fun findPOIs(authority: String, poiFilter: POIProviderContract.Filter): List<POI> = withContext(ioDispatcher) {
        DataSourceManager.findPOIs(appContext, authority, poiFilter).map { it.poi }
    }

    suspend fun findPOIMs(provider: IAgencyProperties, poiFilter: POIProviderContract.Filter) = findPOIMs(provider.authority, poiFilter)

    suspend fun findPOIMs(authority: String, poiFilter: POIProviderContract.Filter): MutableList<POIManager> = withContext(ioDispatcher) {
        DataSourceManager.findPOIs(appContext, authority, poiFilter)
    }

    suspend fun findAgencyAvailableVersionCode(authority: String, forceAppUpdateRefresh: Boolean = false, inFocus: Boolean = false): Int? =
        withContext(ioDispatcher) {
            DataSourceManager.findAgencyAvailableVersionCode(appContext, authority, forceAppUpdateRefresh, inFocus)
        }

    suspend fun findAgencyRDSRouteLogo(agencyAuthority: String): JPaths? = withContext(ioDispatcher) {
        DataSourceManager.findAgencyRDSRouteLogo(appContext, agencyAuthority)
    }

    suspend fun findAllRDSAgencyRoutes(agencyAuthority: String): List<Route> = withContext(ioDispatcher) {
        DataSourceManager.findAllRDSAgencyRoutes(appContext, agencyAuthority)
    }

    suspend fun findRDSRoute(agencyAuthority: String, routeId: Long): Route? = withContext(ioDispatcher) {
        DataSourceManager.findRDSRoute(appContext, agencyAuthority, routeId)
    }

    suspend fun findRDSDirection(agencyAuthority: String, directionId: Long): Direction? = withContext(ioDispatcher) {
        DataSourceManager.findRDSDirection(appContext, agencyAuthority, directionId)
    }

    @Suppress("unused")
    @Discouraged(message = "providers read trip IDs directly")
    suspend fun findRDSTrips(agencyAuthority: String, routeId: Long, directionId: Long? = null): List<Trip>? = withContext(ioDispatcher) {
        if (!FeatureFlags.F_EXPORT_TRIP_ID) return@withContext null
        //noinspection DiscouragedApi
        DataSourceManager.findRDSTrips(appContext, agencyAuthority, routeId, directionId)
    }

    suspend fun findRDSVehicleLocations(
        vehicleLocationProviderProperties: VehicleLocationProviderProperties,
        filter: VehicleLocationProviderContract.Filter
    ): List<VehicleLocation>? = withContext(ioDispatcher) {
        if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return@withContext null
        DataSourceManager.findVehicleLocations(
            appContext,
            vehicleLocationProviderProperties.authority,
            filter.appendProvidedKeys(keysManager.getKeysMap(vehicleLocationProviderProperties.authority))
        )
    }

    suspend fun findRDSRouteDirections(agencyAuthority: String, routeId: Long): List<Direction>? = withContext(ioDispatcher) {
        DataSourceManager.findRDSRouteDirections(appContext, agencyAuthority, routeId)
    }

    suspend fun findAgencySetupRequired(
        agencyAuthority: String
    ): Boolean? = withContext(ioDispatcher) {
        DataSourceManager.findAgencySetupRequired(appContext, agencyAuthority)
    }

    suspend fun findAgencyProperties(
        agencyAuthority: String,
        agencyType: DataSourceType,
        isRDS: Boolean,
        logo: JPaths?,
        pkg: String,
        longVersionCode: Long,
        enabled: Boolean,
        trigger: Int
    ): AgencyProperties? = withContext(ioDispatcher) {
        DataSourceManager.findAgencyProperties(appContext, agencyAuthority, agencyType, isRDS, logo, pkg, longVersionCode, enabled, trigger)
    }

    @Suppress("unused")
    suspend fun findServiceUpdates(
        authority: String,
        serviceUpdateFilter: ServiceUpdateProviderContract.Filter
    ): List<ServiceUpdate>? = withContext(ioDispatcher) {
        gateNotCacheOnly(authority, serviceUpdateFilter) {
            DataSourceManager.findServiceUpdates(appContext, authority, serviceUpdateFilter)
        }
    }

    @Discouraged("use suspend function instead")
    @WorkerThread
    fun findServiceUpdatesSync(authority: String, serviceUpdateFilter: ServiceUpdateProviderContract.Filter, isCancelled: () -> Boolean): List<ServiceUpdate>? =
        gateNotCacheOnlySync(authority, serviceUpdateFilter, isCancelled) {
            DataSourceManager.findServiceUpdates(appContext, authority, serviceUpdateFilter)
        }

    suspend fun findStatus(authority: String, statusFilter: StatusProviderContract.Filter) = withContext(ioDispatcher) {
        gateNotCacheOnly(authority, statusFilter) {
            DataSourceManager.findStatus(appContext, authority, statusFilter)
        }
    }

    @Discouraged("use suspend function instead")
    @WorkerThread
    fun findStatusSync(authority: String, statusFilter: StatusProviderContract.Filter, isCancelled: () -> Boolean) =
        gateNotCacheOnlySync(authority, statusFilter, isCancelled) {
            DataSourceManager.findStatus(appContext, authority, statusFilter)
        }

    suspend fun findScheduleTimestamps(authority: String, scheduleTimestampsFilter: ScheduleTimestampsProviderContract.Filter?) = withContext(ioDispatcher) {
        DataSourceManager.findScheduleTimestamps(appContext, authority, scheduleTimestampsFilter)
    }

    suspend fun findNews(newsProvider: NewsProviderProperties, newsFilter: NewsProviderContract.Filter) = withContext(ioDispatcher) {
        DataSourceManager.findNews(appContext, newsProvider.authority, newsFilter.appendProvidedKeys(keysManager.getKeysMap(newsProvider.authority)))
    }
}
