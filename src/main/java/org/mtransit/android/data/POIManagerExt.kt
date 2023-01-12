package org.mtransit.android.data

import android.content.Context
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