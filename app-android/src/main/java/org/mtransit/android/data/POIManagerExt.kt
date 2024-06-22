package org.mtransit.android.data

import android.content.Context
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.POI
import org.mtransit.android.util.UIAccessibilityUtils
import org.mtransit.commons.FeatureFlags

@Suppress("unused")
fun Iterable<POIManager>.toStringUUID(): String {
    return this.joinToString {
        it.poi.uuid
    }
}

fun POI.getLabelDecorated(context: Context, isShowingAccessibilityInfo: Boolean): CharSequence {
    if (FeatureFlags.F_ACCESSIBILITY_CONSUMER) {
        return UIAccessibilityUtils.decorate(context, this.label, isShowingAccessibilityInfo, UIAccessibilityUtils.ImageSize.LARGE, alignBottom = false)
    }
    return this.label
}

fun <P : POI> P.toPOIM() = POIManager(this)

fun POIManager.distanceToInMeters(other: POIManager): Float? {
    return this.poi.distanceToInMeters(other.poi)
}

fun POI.distanceToInMeters(other: POI): Float? {
    if (!this.hasLocation() || !other.hasLocation()) return null
    return LocationUtils.distanceToInMeters(this.lat, this.lng, other.lat, other.lng)
}

val POIManager.shortUUID: String
    get() = this.poi.shortUUID

val POI.shortUUID: String
    get() = this.uuid.substring(this.authority.length + 1)
