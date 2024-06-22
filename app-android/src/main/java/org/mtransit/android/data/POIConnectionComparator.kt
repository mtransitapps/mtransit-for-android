package org.mtransit.android.data

import androidx.annotation.VisibleForTesting
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteTripStop

class POIConnectionComparator(
    private val computeDistance: (POI, POI) -> Float? = { poi1: POI, poi2: POI -> poi1.distanceToInMeters(poi2) }
) : Comparator<POIManager> {

    companion object {
        private const val SAME_LOCATION_DISTANCE_IN_METER: Float = 25f
    }

    var targetedPOI: POI? = null

    override fun compare(poim1: POIManager?, poim2: POIManager?): Int {
        if (this.targetedPOI != null
            && (poim1 != null)
            && (poim2 != null)
            && poim1.poi is RouteTripStop
            && poim2.poi is RouteTripStop
        ) {
            if (isAlmostSameLocation(poim1, poim2)) {
                val poim1Connection: Boolean = isConnection(poim1.poi)
                val poim2Connection: Boolean = isConnection(poim2.poi)
                if (poim1Connection && !poim2Connection) {
                    return ComparatorUtils.BEFORE
                } else if (!poim1Connection && poim2Connection) {
                    return ComparatorUtils.AFTER
                }
            }
        }
        return ComparatorUtils.SAME
    }

    @VisibleForTesting
    fun isAlmostSameLocation(poim1: POIManager, poim2: POIManager): Boolean {
        val distanceInMeter = computeDistance(poim1.poi, poim2.poi)
        if (distanceInMeter != null) {
            return distanceInMeter <= SAME_LOCATION_DISTANCE_IN_METER
        }
        return false
    }

    @VisibleForTesting
    fun isConnection(poi: POI) = targetedPOI?.let { targetedPOI ->
        if (poi.authority != targetedPOI.authority) return false
        if (targetedPOI is RouteTripStop) {
            if (poi is RouteTripStop) {
                return poi.route.id == targetedPOI.route.id
            }
        }
        return false
    } ?: false
}