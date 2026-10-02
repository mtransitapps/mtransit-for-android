package org.mtransit.android.commons

import org.mtransit.android.commons.LocationUtils.LocationPOI
import org.mtransit.android.commons.LocationUtils.MAX_DISTANCE_ON_EARTH_IN_METERS
import org.mtransit.android.commons.data.POI
import org.mtransit.android.data.POIManager
import java.util.Arrays
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val distanceToInMetersJVM: (startLat: Double, startLng: Double, endLat: Double, endLng: Double) -> Float =
    { lat1: Double, lon1: Double, lat2: Double, lon2: Double ->
        val earthRadiusKm = 6371.0 // Use 3958.8 for miles instead

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val originLatRad = Math.toRadians(lat1)
        val destinationLatRad = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2) +
            sin(dLon / 2).pow(2) *
            cos(originLatRad) * cos(destinationLatRad)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        (earthRadiusKm * c * 1000.0).toFloat() // Returns distance in meters
    }

val getAroundCoveredDistanceInMetersJVM: (lat: Double, lng: Double, aroundDiff: Double) -> Float = { lat, lng, aroundDiff ->
    val area = LocationUtils.getArea(lat, lng, aroundDiff)
    val distanceToSouth =
        if (area.minLat > LocationUtils.MIN_LAT) distanceToInMetersJVM(lat, lng, area.minLat, lng) else MAX_DISTANCE_ON_EARTH_IN_METERS
    val distanceToNorth =
        if (area.maxLat < LocationUtils.MAX_LAT) distanceToInMetersJVM(lat, lng, area.maxLat, lng) else MAX_DISTANCE_ON_EARTH_IN_METERS
    val distanceToWest =
        if (area.minLng > LocationUtils.MIN_LNG) distanceToInMetersJVM(lat, lng, lat, area.minLng) else MAX_DISTANCE_ON_EARTH_IN_METERS
    val distanceToEast =
        if (area.maxLng < LocationUtils.MAX_LNG) distanceToInMetersJVM(lat, lng, lat, area.maxLng) else MAX_DISTANCE_ON_EARTH_IN_METERS
    val distances = floatArrayOf(distanceToNorth, distanceToSouth, distanceToWest, distanceToEast)
    Arrays.sort(distances)
    distances[0] // return the closest
}

@Suppress("unused")
fun POIManager.distanceToInMetersJVM(other: POIManager) = this.poi.distanceToInMetersJVM(other.poi)
fun POI.distanceToInMetersJVM(other: POI): Float? =
    if (this.hasLocation() && other.hasLocation()) distanceToInMetersJVM(this.lat, this.lng, other.lat, other.lng) else null

fun <POI : LocationPOI> MutableList<POI>.updateDistanceMJVM(lat: Double, lng: Double): MutableList<POI> {
    this.forEach { poi ->
        if (poi.hasLocation()) {
            poi.distance = distanceToInMetersJVM(lat, lng, poi.lat, poi.lng)
        }
    }
    return this
}
