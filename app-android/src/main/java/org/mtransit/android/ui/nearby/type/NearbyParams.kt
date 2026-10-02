package org.mtransit.android.ui.nearby.type

import android.location.Location
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.location.AroundDiff
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.util.toLatLngS

data class NearbyParams(
    val typeId: Int? = null,
    val allAgencies: List<AgencyBaseProperties>? = null,
    val aroundDiff: AroundDiff? = AroundDiff(),
    val nearbyLocation: Location? = null,
    val minCoverageInMeters: Float? = null,
    val minSize: Int? = null,
    val maxSize: Int? = null,
    // TODO ? val lastEmptyAroundDiff: Double? = null,
) {
    val typeAgencies: List<AgencyBaseProperties>?
        get() = typeId?.let { dstId -> allAgencies?.filter { agency -> agency.getSupportedType().id == dstId } }

    val aroundDiffOrDefault: AroundDiff get() = aroundDiff ?: AroundDiff()

    val area: Area?
        get() {
            return if (nearbyLocation == null || aroundDiff == null) {
                null
            } else {
                Area.getArea(nearbyLocation.latitude, nearbyLocation.longitude, aroundDiff.ad)
            }
        }

    val maxDistance: Float?
        get() {
            return if (nearbyLocation == null || aroundDiff == null) {
                null
            } else {
                LocationUtils.getAroundCoveredDistanceInMeters(nearbyLocation.latitude, nearbyLocation.longitude, aroundDiff.ad)
            }
        }

    val isReady: Boolean
        get() = typeAgencies != null && nearbyLocation != null && aroundDiff != null && minCoverageInMeters != null && maxSize != null

    @Suppress("unused")
    fun toStringS() = buildString {
        append("NearbyParams(")
        append("type=$typeId, ")
        append("agencies=${allAgencies?.size}, ")
        append("ad=$aroundDiff, ")
        append("nearby=${nearbyLocation.toLatLngS()}, ")
        append("minCoverageMeter=$minCoverageInMeters, ")
        append("min=$minSize, ")
        append("max=$maxSize")
        append(")")
    }
}
