package org.mtransit.android.util

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.mtransit.android.commons.data.Area
import android.location.Location as AndroidLocation

fun LatLngBounds?.containsEntirely(other: LatLngBounds?): Boolean {
    val otherArea = other ?: return false
    val thisArea = this ?: return false
    return thisArea.contains(otherArea.northeast) && thisArea.contains(otherArea.southwest)
}

val Area.southwest: LatLng
    get() = LatLng(southLat, westLng)

val Area.northeast: LatLng
    get() = LatLng(northLat, eastLng)

fun Area.toLatLngBounds() = LatLngBounds(southwest, northeast)

fun LatLng.isInside(area: Area): Boolean {
    return Area.isInside(this.latitude, this.longitude, area)
}

fun AndroidLocation?.toLatLngS(): String {
    return this?.let { "{lat: ${it.latitude}, lng: ${it.longitude}}" } ?: "null"
}
