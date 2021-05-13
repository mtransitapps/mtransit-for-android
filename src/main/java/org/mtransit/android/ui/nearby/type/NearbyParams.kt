package org.mtransit.android.ui.nearby.type

import android.location.Location
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyProperties

data class NearbyParams(
    val typeId: Int? = null,
    val allAgencies: List<AgencyProperties>? = null,
    val ad: LocationUtils.AroundDiff? = LocationUtils.getNewDefaultAroundDiff(),
    val nearbyLocation: Location? = null,
    val minCoverageInMeters: Float? = null,
    val minSize: Int? = null,
    val maxSize: Int? = null,
    // TODO ? val lastEmptyAroundDiff: Double? = null,
) {
    val typeAgencies: List<AgencyProperties>?
        get() = typeId?.let { dstId -> allAgencies?.filter { agency -> agency.type.id == dstId } }

    val area: LocationUtils.Area?
        get() {
            return if (nearbyLocation == null || ad == null) {
                null
            } else {
                LocationUtils.getArea(nearbyLocation.latitude, nearbyLocation.longitude, ad.aroundDiff)
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

    fun toStringS(): String {
        return "NearbyParams(type=$typeId, agencies=${allAgencies?.size}, ad=$ad, nearby=$nearbyLocation, minCoverage=$minCoverageInMeters, min=$minSize, max=$maxSize)"
    }
}
