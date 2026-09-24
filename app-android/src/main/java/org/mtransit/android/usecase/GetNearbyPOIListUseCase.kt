package org.mtransit.android.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.location.AroundDiff
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.removeTooMuchWhenNotInCoverage
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyNearbyProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIAlphaComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.isNoPickup
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.location.UILocationUtils
import org.mtransit.commons.removeAllAnd
import org.mtransit.commons.sortWithAnd
import org.mtransit.commons.takeAnd
import javax.inject.Inject

class GetNearbyPOIListUseCase(
    private val poiRepository: POIRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : MTLog.Loggable {

    @Inject
    constructor(
        poiRepository: POIRepository,
    ) : this(
        poiRepository = poiRepository,
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG: String = GetNearbyPOIListUseCase::class.java.simpleName

        private const val INITIAL_COVERAGE_IN_METERS = 100f

        private const val MAX_DISTANCE_INCREASE = 1.5f

        private val POI_ALPHA_COMPARATOR = POIAlphaComparator()
    }

    private var logTag: String = LOG_TAG

    override fun getLogTag(): String = logTag

    fun setLogTag(tag: String) {
        this.logTag = "$LOG_TAG-$tag"
    }

    private suspend fun MutableMap<Pair<String, Double>, List<POIManager>>.getOrFindPOIMAroundLoc(
        agency: IAgencyProperties,
        lat: Double,
        lng: Double,
        aroundDiff: Double,
    ): List<POIManager> {
        return getOrPut(agency.authority to aroundDiff) {
            poiRepository.findPOIMsAroundLoc(agency, lat, lng, aroundDiff, avoidLoading = true)
        }
    }

    suspend operator fun invoke(
        lat: Double,
        lng: Double,
        allAgencies: List<IAgencyNearbyProperties>,
        minSize: Int? = UILocationUtils.MIN_NEARBY_LIST,
        maxSize: Int = UILocationUtils.MAX_NEARBY_LIST,
        minCoverageInMeters: Float = UILocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS,
        maxCoverageInMeters: Float? = null,
        getMaxDistanceInMeters: (maxDistanceInMeters: Float, dst: DataSourceType) -> Float = { maxDistanceInMeters, _ ->
            maxDistanceInMeters
        },
        mainAgency: IAgencyProperties? = null,
        excludeAgency: (IAgencyProperties) -> Boolean = { !it.type.isNearbyScreen },
        excludePOI: (POIManager) -> Boolean = { it.poi.isNoPickup },
    ): MutableList<POIManager> = withContext(ioDispatcher) {
        val nearbyAgencyArea = Area.getArea(lat, lng, 0.01)
        allAgencies.takeIf { it.isNotEmpty() }
            ?: run {
                MTLog.d(this@GetNearbyPOIListUseCase, "() > SKIP (no agencies)")
                return@withContext mutableListOf()
            }
        val nearbyAgencies = allAgencies
            .filter { agency ->
                !excludeAgency(agency)
                    && agency.isInArea(nearbyAgencyArea)
            }.takeIf { it.isNotEmpty() }
            ?: run {
                MTLog.d(this@GetNearbyPOIListUseCase, "() > SKIP (no nearby agencies)")
                return@withContext mutableListOf()
            }
        val nearbyPOIs = mutableListOf<POIManager>()
        val agenciesToPOIMs = mutableMapOf<Pair<String, Double>, List<POIManager>>()
        val aroundDiff = AroundDiff()
        var maxDistanceInMeters = INITIAL_COVERAGE_IN_METERS
        var poiAgencyPOIAdded = false
        var newNearbyPOIsLoadedCount: Int
        // 1 - nearby POIs from nearby agencies
        while (true) {
            newNearbyPOIsLoadedCount = 0
            if (maxDistanceInMeters >= LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, aroundDiff.ad)) {
                aroundDiff.increment()
            }
            nearbyAgencies.forEach { nearbyAgency ->
                nearbyPOIs.addAll(
                    agenciesToPOIMs.getOrFindPOIMAroundLoc(nearbyAgency, lat, lng, aroundDiff.ad)
                        .toMutableList() // creates new list to modify (keep cached list unchanged)
                        .removeAllAnd(excludePOI)
                        .removeTooFar(getMaxDistanceInMeters(maxDistanceInMeters, nearbyAgency.type))
                        .removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
                        .removeAllAnd { nearbyPOIs.contains(it) }
                        .also { newAgencyNearbyPOIs ->
                            newNearbyPOIsLoadedCount += newAgencyNearbyPOIs.size
                            if (mainAgency != null
                                && !poiAgencyPOIAdded
                                && nearbyAgency.authority == mainAgency.authority
                                && newAgencyNearbyPOIs.isNotEmpty()
                            ) {
                                poiAgencyPOIAdded = true
                            }
                        }
                )
            }
            nearbyPOIs.removeDuplicateRouteDirection()
            if (mainAgency == null) {
                if ((nearbyPOIs.size <= (minSize ?: 0) || newNearbyPOIsLoadedCount > 0)
                    && maxDistanceInMeters <= (maxCoverageInMeters ?: Float.MAX_VALUE)
                    && aroundDiff.increment <= (AroundDiff.AD_MINIMUM + AroundDiff.DEFAULT_INCREMENT)
                    && !LocationUtils.searchComplete(lat, lng, aroundDiff.ad) // world explored
                ) {
                    maxDistanceInMeters *= MAX_DISTANCE_INCREASE
                    continue
                }
                break
            } else {
                val firstRelevantDistance = nearbyPOIs.firstOrNull { it.distance > 0f && !excludePOI(it) }?.distance
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
                }
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
                excludePOI = excludePOI,
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
        excludePOI: (POIManager) -> Boolean,
        poiAgencyPOIAdded: Boolean,
        agenciesToPOIMs: MutableMap<Pair<String, Double>, List<POIManager>>,
    ) {
        val minNotConnectionSize = when {
            isEmpty() -> 5
            !poiAgencyPOIAdded -> 1
            else -> 0
        }
        if (minNotConnectionSize <= 0) return
        val connectionSize = size
        val mainAgencyAd = AroundDiff()
        while (true) {
            addAll(
                agenciesToPOIMs.getOrFindPOIMAroundLoc(mainAgency, lat, lng, mainAgencyAd.ad)
                    .toMutableList() // creates new list to modify (keep cached list unchanged)
                    .removeAllAnd(excludePOI)
                    .removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
                    .removeAllAnd { contains(it) }
                    .takeAnd(minNotConnectionSize - (size - connectionSize))
            )
            if (size >= connectionSize + minNotConnectionSize // enough POI
                || LocationUtils.searchComplete(lat, lng, mainAgencyAd.ad) // world explored
            ) {
                break
            }
            mainAgencyAd.increment()
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
