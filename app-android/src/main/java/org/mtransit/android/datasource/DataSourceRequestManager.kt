package org.mtransit.android.datasource

import android.content.Context
import androidx.annotation.Discouraged
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.provider.common.ProviderContract.Filter.Companion.toProvidedKeys
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
import org.mtransit.android.data.JPaths
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.ServiceUpdateProviderProperties
import org.mtransit.android.data.StatusProviderProperties
import org.mtransit.android.data.VehicleLocationProviderProperties
import org.mtransit.android.util.KeysManager
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.FeatureFlags
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

    suspend fun ping(agencyAuthority: String) = withContext(ioDispatcher) {
        DataSourceManager.ping(appContext, agencyAuthority)
    }

    // region POI

    suspend fun findPOI(authority: String, poiFilter: POIProviderContract.Filter): POI? = findPOIM(authority, poiFilter)?.poi

    suspend fun findPOIM(authority: String, poiFilter: POIProviderContract.Filter): POIManager? = withContext(ioDispatcher) {
        DataSourceManager.findPOIM(appContext, authority, poiFilter)
    }

    @Suppress("unused")
    suspend fun findPOIs(authority: String, poiFilter: POIProviderContract.Filter): List<POI> = findPOIMs(authority, poiFilter).map { it.poi }

    suspend fun findPOIMs(authority: String, poiFilter: POIProviderContract.Filter): MutableList<POIManager> = withContext(ioDispatcher) {
        DataSourceManager.findPOIMs(appContext, authority, poiFilter)
    }

    // endregion POI

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
            filter.copy(providedEncryptKeysMap = toProvidedKeys(keysManager.getKeysMap(vehicleLocationProviderProperties.authority))),
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
        serviceUpdateProvider: ServiceUpdateProviderProperties,
        serviceUpdateFilter: ServiceUpdateProviderContract.Filter
    ) = withContext(ioDispatcher) {
        DataSourceManager.findServiceUpdates(
            appContext,
            serviceUpdateProvider.authority,
            serviceUpdateFilter.copy(providedEncryptKeysMap = toProvidedKeys(keysManager.getKeysMap(serviceUpdateProvider.authority))),
        )
    }

    @Discouraged("use suspend function instead")
    @WorkerThread
    fun findServiceUpdatesSync(serviceUpdateProvider: ServiceUpdateProviderProperties, serviceUpdateFilter: ServiceUpdateProviderContract.Filter) =
        DataSourceManager.findServiceUpdates(
            appContext,
            serviceUpdateProvider.authority,
            serviceUpdateFilter.copy(providedEncryptKeysMap = toProvidedKeys(keysManager.getKeysMap(serviceUpdateProvider.authority))),
        )

    suspend fun findStatus(statusProvider: StatusProviderProperties, statusFilter: StatusProviderContract.Filter) = withContext(ioDispatcher) {
        DataSourceManager.findStatus(
            appContext,
            statusProvider.authority,
            statusFilter.copyWithProvidedEncryptKeysMap(toProvidedKeys(keysManager.getKeysMap(statusProvider.authority))),
        )
    }

    @Discouraged("use suspend function instead")
    @WorkerThread
    fun findStatusSync(statusProvider: StatusProviderProperties, statusFilter: StatusProviderContract.Filter) =
        DataSourceManager.findStatus(
            appContext,
            statusProvider.authority,
            statusFilter.copyWithProvidedEncryptKeysMap(toProvidedKeys(keysManager.getKeysMap(statusProvider.authority))),
        )

    suspend fun findScheduleTimestamps(authority: String, scheduleTimestampsFilter: ScheduleTimestampsProviderContract.Filter?) = withContext(ioDispatcher) {
        DataSourceManager.findScheduleTimestamps(appContext, authority, scheduleTimestampsFilter)
    }

    suspend fun findNews(newsProvider: NewsProviderProperties, newsFilter: NewsProviderContract.Filter) = withContext(ioDispatcher) {
        DataSourceManager.findNews(
            appContext,
            newsProvider.authority,
            newsFilter.copy(providedEncryptKeysMap = toProvidedKeys(keysManager.getKeysMap(newsProvider.authority))),
        )
    }
}
