package org.mtransit.android.ui.view.map

import org.mtransit.android.commons.MTLog

enum class MTMapIconZoomGroup(
    val minZoom: Float = 0.0f,
    val maxZoom: Float = Float.MAX_VALUE,
    val minCount: Int = 0,
    val maxCount: Int = Int.MAX_VALUE,
) : MTLog.Loggable {

    DEFAULT(maxCount = 10),
    SMALL(maxZoom = 13.0f, minCount = 10)
    ;

    override fun getLogTag() = LOG_TAG

    companion object {

        private val LOG_TAG: String = MTMapIconZoomGroup::class.java.simpleName

        @JvmStatic
        fun from(zoom: Float, markerCount: Int?): MTMapIconZoomGroup {
            for (group in entries) {
                if (markerCount != null
                    && markerCount in (group.minCount..group.maxCount)
                    && zoom in (group.minZoom..group.maxZoom)
                ) {
                    return group
                }
                if (zoom in (group.minZoom..group.maxZoom)) {
                    if (markerCount != null && markerCount !in (group.minCount..group.maxCount)) {
                        continue
                    }
                    return group
                }
            }
            MTLog.d(LOG_TAG, "from() > UNEXPECTED zoom: $zoom|markerCount: $markerCount -> DEFAULT")
            return DEFAULT
        }
    }
}
