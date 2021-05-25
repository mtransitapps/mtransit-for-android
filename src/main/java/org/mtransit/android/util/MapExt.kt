package org.mtransit.android.util

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.mtransit.android.commons.LocationUtils

fun LatLngBounds?.containsEntirely(other: LatLngBounds?): Boolean {
    return other?.let { otherArea ->
        this?.let { thisArea -> thisArea.contains(otherArea.northeast) && thisArea.contains(otherArea.southwest) }
    } ?: false
}

fun LocationUtils.Area.toLatLngBounds(): LatLngBounds {
    return LatLngBounds(
        LatLng(southLat, westLng), // SW
        LatLng(northLat, eastLng) // NE
    )
}