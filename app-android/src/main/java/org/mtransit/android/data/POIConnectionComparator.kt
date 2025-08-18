package org.mtransit.android.data

import androidx.annotation.VisibleForTesting
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.commons.data.DefaultPOI
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteDirectionStop

class POIConnectionComparator(
    private val maxDistanceInMeters: (@DataSourceTypeId.DataSourceType Int) -> Float = { SAME_LOCATION_DISTANCE_IN_METER },
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
        ) {
            val poim1Connection = isConnection(poim1.poi)
            val poim2Connection = isConnection(poim2.poi)
            if (poim1Connection && !poim2Connection) {
                return ComparatorUtils.BEFORE
            } else if (!poim1Connection && poim2Connection) {
                return ComparatorUtils.AFTER
            }
        }
        return ComparatorUtils.SAME
    }

    @VisibleForTesting
    fun isAlmostSameLocation(poim1: POIManager, poim2: POIManager) = isAlmostSameLocation(poim1.poi, poim2.poi)

    fun isAlmostSameLocation(poi1: POI, poi2: POI): Boolean {
        val distanceInMeter = computeDistance(poi1, poi2) ?: return false
        return targetedPOI?.dataSourceTypeId?.let { dataSourceTypeId ->
            distanceInMeter <= maxDistanceInMeters(dataSourceTypeId)
        } ?: false
    }

    @VisibleForTesting
    fun isConnection(poi: POI) = targetedPOI
        ?.takeIf { it.authority == poi.authority } // same agency
        ?.takeIf { isAlmostSameLocation(it, poi) }
        ?.let { targetedPOI ->
            if (targetedPOI is RouteDirectionStop && poi is RouteDirectionStop) {
                return@let poi.route.id == targetedPOI.route.id
            } else if (targetedPOI is DefaultPOI && poi is DefaultPOI) {
                return@let true // nearby [bike] station...
            }
            null
        } ?: false
}