package org.mtransit.android.data

import androidx.annotation.VisibleForTesting
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.commons.data.DefaultPOI
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteDirectionStop

class POIConnectionComparator(
    private val targetedPOI: POI? = null,
    private val maxDistanceInMeters: (@DataSourceTypeId.DataSourceType Int) -> Float,
    private val computeDistance: (POI, POI) -> Float? = { poi1: POI, poi2: POI -> poi1.distanceToInMeters(poi2) },
    private val sameAgency1st: Boolean = false,
) : Comparator<POIManager> {

    override fun compare(poim1: POIManager?, poim2: POIManager?): Int {
        if (this.targetedPOI != null && poim1 != null && poim2 != null) {
            if (this.sameAgency1st) {
                val poim1SameAgency = poim1.poi.authority == this.targetedPOI.authority
                val poim2SameAgency = poim2.poi.authority == this.targetedPOI.authority
                if (poim1SameAgency && !poim2SameAgency) {
                    return ComparatorUtils.BEFORE
                }
                else if (!poim1SameAgency && poim2SameAgency) {
                    return ComparatorUtils.AFTER
                }
            }
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
    fun isCloseEnough(poim1: POIManager, poim2: POIManager) = isCloseEnough(poim1.poi, poim2.poi)

    fun isCloseEnough(poi1: POI, poi2: POI): Boolean {
        val distanceInMeter = computeDistance(poi1, poi2) ?: return false
        return targetedPOI?.dataSourceTypeId?.let { dataSourceTypeId ->
            distanceInMeter <= maxDistanceInMeters(dataSourceTypeId)
        } ?: false
    }

    @VisibleForTesting
    fun isConnection(poi: POI) = targetedPOI
        ?.takeIf { targetedPOI -> targetedPOI.authority == poi.authority }
        ?.takeIf { targetedPOI -> isCloseEnough(targetedPOI, poi) }
        ?.let { targetedPOI ->
            when (targetedPOI) {
                is RouteDirectionStop if poi is RouteDirectionStop -> return@let poi.route.id == targetedPOI.route.id // same route
                is DefaultPOI if poi is DefaultPOI -> return@let true // nearby [bike] station...
                else -> null // mixed POI
            }
        } ?: false
}
