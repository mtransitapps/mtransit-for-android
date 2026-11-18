package org.mtransit.android.util

import com.google.android.gms.maps.model.LatLng as GLatLng
import com.google.android.gms.maps.model.LatLngBounds as GLatLngBounds

fun GLatLngBounds?.containsEntirely(other: GLatLngBounds?): Boolean {
    val otherArea = other ?: return false
    val thisArea = this ?: return false
    return thisArea.contains(otherArea.northeast) && thisArea.contains(otherArea.southwest)
}

fun GLatLngBounds.Builder.include(latLngList: Collection<GLatLng>) = apply {
    latLngList.forEach { include(it) }
}
