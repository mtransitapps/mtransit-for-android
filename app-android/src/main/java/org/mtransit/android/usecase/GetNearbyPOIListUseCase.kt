package org.mtransit.android.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.removeTooMuchWhenNotInCoverage
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyNearbyProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIAlphaComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.isNoPickup
import org.mtransit.android.data.isSameRoute
import org.mtransit.android.datasource.POIRepository
import org.mtransit.commons.addAllN
import org.mtransit.commons.removeAllAnd
import org.mtransit.commons.sortWithAnd
import org.mtransit.commons.takeAnd
import javax.inject.Inject
import kotlin.collections.mutableMapOf

class GetNearbyPOIListUseCase(
    private val poiRepository: POIRepository,
    private val ioDispatcher: CoroutineDispatcher,
) {

    @Inject
    constructor(
        poiRepository: POIRepository,
    ) : this(
        poiRepository = poiRepository,
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private const val INITIAL_COVERAGE_IN_METERS = 100f

        private const val MAX_DISTANCE_INCREASE = 1.5f

        private val POI_ALPHA_COMPARATOR = POIAlphaComparator()
    }

    suspend operator fun invoke(
        lat: Double,
        lng: Double,
        allAgencies: List<IAgencyNearbyProperties>,
        maxSize: Int = LocationUtils.MAX_NEARBY_LIST,
        minCoverageInMeters: Float = LocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS,
        getMaxDistanceInMeters: (maxDistanceInMeters: Float, dst: DataSourceType) -> Float = { maxDistanceInMeters, _ -> maxDistanceInMeters },
        mainAgency: IAgencyProperties? = null,
        excludedUUID: String? = null,
        excludedRouteId: Long? = null,
        getOrFindPOIMAroundLoc: suspend (
            agenciesToPOIMs: MutableMap<Pair<String, Double>, List<POIManager>>,
            agency: IAgencyProperties,
            aroundDiff: Double
        ) -> List<POIManager> = { agenciesToPOIMs, agency, aroundDiff ->
            agenciesToPOIMs.getOrPut(agency.authority to aroundDiff) {
                poiRepository.findPOIMsAroundLoc(agency, lat, lng, aroundDiff, avoidLoading = true)
            }
        }
    ): MutableList<POIManager> = withContext(ioDispatcher) {
        val nearbyAgencyArea = Area.getArea(lat, lng, 0.01)
        val nearbyAgencies = allAgencies.filter { agency ->
            agency.type.isNearbyScreen
                && agency.type != DataSourceType.TYPE_MODULE
                && agency.isInArea(nearbyAgencyArea)
        }
        val nearbyPOIs = mutableListOf<POIManager>()
        val agenciesToPOIMs = mutableMapOf<Pair<String, Double>, List<POIManager>>()
        val ad = LocationUtils.getNewDefaultAroundDiff()
        var maxDistanceInMeters = INITIAL_COVERAGE_IN_METERS
        var poiAgencyPOIAdded = false
        // 1 - nearby POIs from nearby agencies
        while (true) {
            if (maxDistanceInMeters >= LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, ad.aroundDiff)) {
                LocationUtils.incAroundDiff(ad)
            }
            nearbyAgencies.forEach { nearbyAgency ->
                nearbyPOIs.addAllN(
                    getOrFindPOIMAroundLoc(agenciesToPOIMs, nearbyAgency, ad.aroundDiff)
                        .toMutableList() // creates new list to modify (keep cached list unchanged)
                        .removeAllAnd {
                            it.poi.uuid == excludedUUID
                                || (it.poi.isNoPickup && !it.poi.isSameRoute(excludedRouteId))
                        }
                        .removeTooFar(getMaxDistanceInMeters(maxDistanceInMeters, nearbyAgency.type))
                        .removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
                        .removeAllAnd { nearbyPOIs.contains(it) }
                        .also { nearbyPOIs ->
                            if (mainAgency != null
                                && !poiAgencyPOIAdded
                                && nearbyAgency.authority == mainAgency.authority
                                && nearbyPOIs.isNotEmpty()
                            ) {
                                poiAgencyPOIAdded = true
                            }
                        }
                )
            }
            nearbyPOIs.removeDuplicateRouteDirection()
            val firstRelevantDistance = nearbyPOIs.firstOrNull { it.distance > 0f && !it.poi.isSameRoute(excludedRouteId) }?.distance
            val firstLastDistanceDiff = nearbyPOIs.takeIf { it.size >= 2 }?.let { it.last().distance - it.first().distance }
                ?.takeIf { it > 0f }?.coerceAtMost(maxDistanceInMeters)
            val minDistance = firstRelevantDistance
                ?.let { it + it.coerceAtLeast(.5f * INITIAL_COVERAGE_IN_METERS) } // 1st relevant distance x2 ( min initial coverage)
                ?: INITIAL_COVERAGE_IN_METERS
            val significantDistance = firstRelevantDistance
                ?.coerceAtLeast(.5f * INITIAL_COVERAGE_IN_METERS)
                ?.let { it * MAX_DISTANCE_INCREASE }
                ?.coerceAtLeast(minDistance)
            if (
                2f * INITIAL_COVERAGE_IN_METERS <= maxDistanceInMeters
                || (significantDistance != null && significantDistance <= maxDistanceInMeters)
            ) {
                break
            } else {
                if (significantDistance != null && significantDistance > maxDistanceInMeters) {
                    maxDistanceInMeters = significantDistance
                    continue
                }
                if (firstLastDistanceDiff != null && firstLastDistanceDiff > 0f) {
                    maxDistanceInMeters += firstLastDistanceDiff
                    continue
                }
                maxDistanceInMeters *= MAX_DISTANCE_INCREASE
            }
        }
        // 2 - try all nearby from current agency
        mainAgency?.let {
            nearbyPOIs.appendMainAgencyPOI(
                lat = lat,
                lng = lng,
                maxSize = maxSize,
                minCoverageInMeters = minCoverageInMeters,
                mainAgency = it,
                excludedUUID = excludedUUID,
                excludedRouteId = excludedRouteId,
                getOrFindPOIMAroundLoc = getOrFindPOIMAroundLoc,
                poiAgencyPOIAdded = poiAgencyPOIAdded,
                agenciesToPOIMs = agenciesToPOIMs,
            )
        }
        nearbyPOIs.sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR)
        nearbyPOIs.sortWithAnd(POI_ALPHA_COMPARATOR)
        return@withContext nearbyPOIs
    }

    private suspend fun MutableList<POIManager>.appendMainAgencyPOI(
        lat: Double,
        lng: Double,
        maxSize: Int,
        minCoverageInMeters: Float,
        mainAgency: IAgencyProperties,
        excludedUUID: String?,
        excludedRouteId: Long?,
        getOrFindPOIMAroundLoc: suspend (
            agenciesToPOIMs: MutableMap<Pair<String, Double>, List<POIManager>>,
            agency: IAgencyProperties,
            aroundDiff: Double
        ) -> List<POIManager>,
        poiAgencyPOIAdded: Boolean,
        agenciesToPOIMs: MutableMap<Pair<String, Double>, List<POIManager>>,
    ) {
        val minNotConnectionSize = when {
            isEmpty() -> 5
            !poiAgencyPOIAdded -> 1
            else -> 0
        }
        if (minNotConnectionSize > 0) {
            val connectionSize = size
            val mainAgencyAd = LocationUtils.getNewDefaultAroundDiff()
            while (true) {
                addAllN(
                    getOrFindPOIMAroundLoc(agenciesToPOIMs, mainAgency, mainAgencyAd.aroundDiff)
                        .toMutableList() // creates new list to modify (keep cached list unchanged)
                        .removeAllAnd {
                            it.poi.uuid == excludedUUID
                                || (it.poi.isNoPickup && it.poi.isSameRoute(excludedRouteId)) // remove if no pickup && another route
                        }
                        .removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
                        .removeAllAnd { contains(it) }
                        .takeAnd(minNotConnectionSize - (size - connectionSize))
                )
                if (size >= connectionSize + minNotConnectionSize // enough POI
                    || LocationUtils.searchComplete(lat, lng, mainAgencyAd.aroundDiff) // world explored
                ) {
                    break
                } else {
                    // TODO latter ? lastTypeAroundDiff = if (nearbyPOIs.isNullOrEmpty()) aroundDiff else null
                    LocationUtils.incAroundDiff(mainAgencyAd)
                }
            }
        }
    }

    private fun MutableList<POIManager>.removeDuplicateRouteDirection() {
        sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR)
        val it = iterator()
        val routeDirectionKept = mutableSetOf<String>()
        while (it.hasNext()) {
            val rds = it.next().poi as? RouteDirectionStop ?: continue
            val routeDirectionId = "${rds.route.id}-${rds.direction.id}"
            if (routeDirectionKept.contains(routeDirectionId)) {
                it.remove()
            } else {
                routeDirectionKept += routeDirectionId
            }
        }
    }
}
