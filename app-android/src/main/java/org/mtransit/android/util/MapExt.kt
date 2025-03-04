package org.mtransit.android.util

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.Area

fun LatLngBounds?.containsEntirely(other: LatLngBounds?): Boolean {
    return other?.let { otherArea ->
        this?.let { thisArea -> thisArea.contains(otherArea.northeast) && thisArea.contains(otherArea.southwest) }
    } ?: false
}

val Area.southwest: LatLng
    get() = LatLng(southLat, westLng)

val Area.northeast: LatLng
    get() = LatLng(northLat, eastLng)

fun Area.toLatLngBounds() = LatLngBounds(southwest, northeast)

fun LatLng.isInside(area: Area): Boolean {
    return Area.isInside(this.latitude, this.longitude, area)
}

fun Location?.toLatLngS(): String {
    return this?.let { "{lat: ${it.latitude}, lng: ${it.longitude}}" } ?: "null"
}