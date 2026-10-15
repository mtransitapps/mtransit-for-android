package org.mtransit.android.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.LocationPOI
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.location.AroundDiff
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.removeTooMuchWhenNotInCoverage
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyNearbyProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.dataSourceTypeId
import org.mtransit.android.data.distanceOrNull
import org.mtransit.android.data.isNoPickup
import org.mtransit.android.data.positiveDistanceOrNull
import org.mtransit.android.data.uuid
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.location.UILocationUtils
import org.mtransit.commons.removeAllAnd
import org.mtransit.commons.sortWithAnd
import org.mtransit.commons.takeAnd
import javax.inject.Inject

class GetNearbyPOIListUseCase(
    private val poiRepository: POIRepository,
    private val getAroundCoveredDistanceInMeters: (lat: Double, lng: Double, aroundDiff: Double) -> Float,
    private val ioDispatcher: CoroutineDispatcher,
) : MTLog.Loggable {

    @Inject
    constructor(
        poiRepository: POIRepository,
    ) : this(
        poiRepository = poiRepository,
        getAroundCoveredDistanceInMeters = { lat, lng, aroundDiff ->
            LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, aroundDiff)
        },
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG: String = GetNearbyPOIListUseCase::class.java.simpleName

        private const val SAME_LOGIC_FOR_TARGET_AGENCY = true

        private const val INITIAL_COVERAGE_IN_METERS = 77f

        private const val MIN_LASTS_DISTANCE_DIFF_IN_METERS = 13f

        private const val MAX_DISTANCE_INCREASE = 1.5f
    }

    private var logTag: String = LOG_TAG

    override fun getLogTag(): String = logTag

    fun setLogTag(tag: String) {
        this.logTag = "$LOG_TAG-$tag"
    }

    suspend operator fun invoke(
        lat: Double,
        lng: Double,
        allAgencies: List<IAgencyNearbyProperties>,
        minSize: Int? = UILocationUtils.MIN_NEARBY_LIST,
        maxSize: Int = UILocationUtils.MAX_NEARBY_LIST,
        minCoverageInMeters: Float = UILocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS,
        enoughCoverageInMeters: Float? = null,
        getMaxDistanceInMeters: (maxDistanceInMeters: Float, dst: DataSourceType, count: Int) -> Float = { maxDistanceInMeters, _, _ ->
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
            if (maxDistanceInMeters >= getAroundCoveredDistanceInMeters(lat, lng, aroundDiff.ad)) {
                aroundDiff.increment()
            }
            nearbyAgencies.forEach { nearbyAgency ->
                nearbyPOIs.addAll(
                    agenciesToPOIMs.getOrFindPOIMAroundLoc(nearbyAgency, lat, lng, aroundDiff.ad)
                        .toMutableList() // creates new list to modify (keep cached list unchanged)
                        .removeAllAnd(excludePOI)
                        .removeTooFar(
                            getMaxDistanceInMeters(
                                maxDistanceInMeters,
                                nearbyAgency.type,
                                nearbyPOIs.count { it.dataSourceTypeId == nearbyAgency.type.id },
                            )
                        )
                        .removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
                        .removeAllAnd { new -> nearbyPOIs.any { it.uuid == new.uuid } }
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
            val removed = nearbyPOIs.removeDuplicateRouteDirection()
            newNearbyPOIsLoadedCount -= removed
            @Suppress("SimplifyBooleanWithConstants")
            if (mainAgency == null || SAME_LOGIC_FOR_TARGET_AGENCY) {
                if ((nearbyPOIs.size <= (minSize ?: 0) || newNearbyPOIsLoadedCount > 0)
                    && maxDistanceInMeters <= (enoughCoverageInMeters ?: Float.MAX_VALUE)
                    && aroundDiff.increment <= (AroundDiff.AD_MINIMUM + AroundDiff.DEFAULT_INCREMENT)
                    && !LocationUtils.searchComplete(lat, lng, aroundDiff.ad) // world explored
                ) {
                    val lastDistance = nearbyPOIs.takeIf { it.isNotEmpty() }?.lastOrNull { it.distance < maxDistanceInMeters }?.distanceOrNull
                    val firstLastDistanceDiff = nearbyPOIs
                        .filter { it.positiveDistanceOrNull?.let { distanceNN -> distanceNN < maxDistanceInMeters } == true }
                        .takeIf { it.size >= 2 }
                        ?.let {
                            val firstDistance = it.first().distance
                            val lastDistance = it.last().distance
                            lastDistance - firstDistance
                        }
                        ?.takeIf { it > 0f }?.coerceAtMost(maxDistanceInMeters)
                    val lastsDistanceDiff = if (lastDistance == null) {
                        null
                    } else {
                        nearbyPOIs
                            .filter { it.positiveDistanceOrNull != null }
                            .takeIf { it.size > 2 }
                            ?.lastOrNull { poim ->
                                poim.distance < lastDistance
                            }?.distance
                            ?.let { distinctLastDistance ->
                                lastDistance - distinctLastDistance
                            }?.coerceAtLeast(
                                MIN_LASTS_DISTANCE_DIFF_IN_METERS
                            )
                    }
                    val avgDistanceDiff = (
                        (firstLastDistanceDiff ?: INITIAL_COVERAGE_IN_METERS) +
                            (lastsDistanceDiff ?: MIN_LASTS_DISTANCE_DIFF_IN_METERS)
                        ).div(2f)
                        .coerceAtMost(.50f * maxDistanceInMeters)
                    if (nearbyPOIs.size >= 2 && avgDistanceDiff > 0f) {
                        maxDistanceInMeters = (lastDistance?.takeIf { it + avgDistanceDiff > maxDistanceInMeters } ?: maxDistanceInMeters) + avgDistanceDiff
                    } else {
                        maxDistanceInMeters *= MAX_DISTANCE_INCREASE
                    }
                    continue
                }
                break
            } else {
                val firstRelevantDistance = nearbyPOIs.firstOrNull { it.distanceOrNull != null && !excludePOI(it) }?.distanceOrNull
                val significantDistance = firstRelevantDistance
                    ?.coerceAtLeast(.5f * INITIAL_COVERAGE_IN_METERS)
                    ?.let { it * MAX_DISTANCE_INCREASE }
                    ?.coerceAtLeast( // 1st relevant distance x2 ( min initial coverage)
                        firstRelevantDistance + firstRelevantDistance.coerceAtLeast(.5f * INITIAL_COVERAGE_IN_METERS)
                    )
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
                val firstLastDistanceDiff = nearbyPOIs
                    .filter { it.distanceOrNull != null }
                    .takeIf { it.size >= 2 }
                    ?.let { it.last().distance - it.first().distance }
                    ?.takeIf { it > 0f }?.coerceAtMost(maxDistanceInMeters)
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
        return@withContext nearbyPOIs
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
                    .removeAllAnd { new -> any { it.uuid == new.uuid } }
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

    private fun MutableList<POIManager>.removeDuplicateRouteDirection(): Int {
        sortWithAnd(LocationPOI.DISTANCE_COMPARATOR)
        val it = iterator()
        val routeDirectionUUIDKept = mutableSetOf<String>()
        var poiRemoved = 0
        while (it.hasNext()) {
            val poim = it.next()
            val rds = poim.poi as? RouteDirectionStop ?: continue
            val routeDirectionId = rds.routeDirectionUUID
            if (routeDirectionUUIDKept.contains(routeDirectionId)) {
                it.remove()
                poiRemoved++
            } else {
                routeDirectionUUIDKept += routeDirectionId
            }
        }
        return poiRemoved
    }
}
