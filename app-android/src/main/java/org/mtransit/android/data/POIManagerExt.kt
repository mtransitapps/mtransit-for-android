package org.mtransit.android.data

import android.content.Context
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.POI
import org.mtransit.android.util.UIAccessibilityUtils

@Suppress("unused")
fun Iterable<POIManager>.toStringUUID(): String {
    return this.joinToString {
        it.poi.uuid
    }
}

fun POI.getLabelDecorated(context: Context, isShowingAccessibilityInfo: Boolean): CharSequence {
    return UIAccessibilityUtils.decorate(context, this.label, isShowingAccessibilityInfo, UIAccessibilityUtils.ImageSize.LARGE, alignBottom = false)
}

fun <P : POI> P.toPOIM() = POIManager(this)

fun POIManager.distanceToInMeters(other: POIManager): Float? {
    return this.poi.distanceToInMeters(other.poi)
}

fun POI.distanceToInMeters(other: POI): Float? {
    if (!this.hasLocation() || !other.hasLocation()) return null
    return LocationUtils.distanceToInMeters(this.lat, this.lng, other.lat, other.lng)
}

val POIManager.simpleDistanceString: String
    get() = this.distance.takeIf { it >= 0f }?.let { "${it}m" } ?: "?m"

val POIManager.uuidAndDistance: String
    get() = this.poi.uuid + " " + this.simpleDistanceString

val POIManager.shortUUIDAndDistance: String
    get() = this.poi.shortUUID + " " + this.simpleDistanceString

val POIManager.shortUUID: String
    get() = this.poi.shortUUID

val POI.shortUUID: String
    get() = this.uuid.substring(this.authority.length + 1)
