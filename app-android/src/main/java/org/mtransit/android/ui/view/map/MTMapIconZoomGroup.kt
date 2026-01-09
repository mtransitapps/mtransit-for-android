package org.mtransit.android.ui.view.map

import org.mtransit.android.commons.MTLog

enum class MTMapIconZoomGroup(
    val minZoom: Float?,
    val maxZoom: Float?,
    val minCount: Int?,
    val maxCount: Int?,
) {

    DEFAULT(13.0f, null, null, 10),
    SMALL(null, 13.0f, 10, null)
    ;

    companion object {
        @JvmStatic
        fun from(zoom: Float?, markerCount: Int?): MTMapIconZoomGroup {
            if (zoom != null) {
                for (group in entries) {
                    if (zoom >= (group.minZoom ?: Float.MIN_VALUE)
                        && zoom <= (group.maxZoom ?: Float.MAX_VALUE)
                    ) {
                        if (markerCount != null) {
                            if (group.minCount != null && markerCount < group.minCount) {
                                continue
                            }
                            if (group.maxCount != null && markerCount > group.maxCount) {
                                continue
                            }
                        }
                        return group
                    }
                }
            }
            MTLog.d(this, "Unknown zoom: $zoom")
            return DEFAULT
        }
    }
}
