package org.mtransit.android.ui.nearby.type

import android.location.Location
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.util.toLatLngS

data class NearbyParams(
    val typeId: Int? = null,
    val allAgencies: List<AgencyBaseProperties>? = null,
    val ad: LocationUtils.AroundDiff? = LocationUtils.getNewDefaultAroundDiff(),
    val nearbyLocation: Location? = null,
    val minCoverageInMeters: Float? = null,
    val minSize: Int? = null,
    val maxSize: Int? = null,
    // TODO ? val lastEmptyAroundDiff: Double? = null,
) {
    val typeAgencies: List<AgencyBaseProperties>?
        get() = typeId?.let { dstId -> allAgencies?.filter { agency -> agency.getSupportedType().id == dstId } }

    val area: Area?
        get() {
            return if (nearbyLocation == null || ad == null) {
                null
            } else {
                Area.getArea(nearbyLocation.latitude, nearbyLocation.longitude, ad.aroundDiff)
            }
        }

    val maxDistance: Float?
        get() {
            return if (nearbyLocation == null || ad == null) {
                null
            } else {
                LocationUtils.getAroundCoveredDistanceInMeters(nearbyLocation.latitude, nearbyLocation.longitude, ad.aroundDiff)
            }
        }

    val poiFilter: POIProviderContract.Filter?
        get() {
            return if (nearbyLocation == null || ad == null) {
                null
            } else {
                POIProviderContract.Filter.getNewAroundFilter(nearbyLocation.latitude, nearbyLocation.longitude, ad.aroundDiff)
            }
        }

    val isReady: Boolean
        get() = typeAgencies != null && nearbyLocation != null && ad != null && minCoverageInMeters != null && maxSize != null

    @Suppress("unused")
    fun toStringS(): String {
        return "NearbyParams(type=$typeId, agencies=${allAgencies?.size}, ad=$ad, nearby=${nearbyLocation.toLatLngS()}, minCoverageMeter=$minCoverageInMeters, min=$minSize, max=$maxSize)"
    }
}
