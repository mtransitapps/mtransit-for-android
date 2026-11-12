package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.GoogleMap

@Suppress("unused")
object MapConstants {

    const val MAP_ZOOM_LEVEL_FARTHEST = 0f
    const val MAP_ZOOM_LEVEL_WORLD = 1f
    const val MAP_ZOOM_LEVEL_CONTINENT = 5f
    const val MAP_ZOOM_LEVEL_CITY = 10f
    const val MAP_ZOOM_LEVEL_STREETS = 15f
    const val MAP_ZOOM_LEVEL_STREETS_BUSY = 16f
    const val MAP_ZOOM_LEVEL_STREETS_BUSY_BUSY = 17f
    const val MAP_ZOOM_LEVEL_CLOSEST = Float.MAX_VALUE // 20 or 21?

    const val MAP_MARKER_ALPHA_DEFAULT = 1.0f
    const val MAP_MARKER_ROTATION_DEFAULT = 0.0f

    const val MAP_MARKER_Z_INDEX_HIGHEST = 1.0f // drawn on top
    const val MAP_MARKER_Z_INDEX_PRIMARY = MAP_MARKER_Z_INDEX_HIGHEST
    const val MAP_MARKER_Z_INDEX_SECONDARY = 0.9f
    const val MAP_MARKER_Z_INDEX_TERTIARY = 0.8f
    const val MAP_MARKER_Z_INDEX_QUATERNARY = 0.7f
    const val MAP_MARKER_Z_INDEX_QUINARY = 0.6f
    const val MAP_MARKER_Z_INDEX_LOWEST = 0.0f
    const val MAP_MARKER_Z_INDEX_DEFAULT = MAP_MARKER_Z_INDEX_LOWEST

    const val MAP_TYPE_NORMAL: Int = GoogleMap.MAP_TYPE_NORMAL
    const val MAP_TYPE_SATELLITE: Int = GoogleMap.MAP_TYPE_HYBRID

    const val PREFS_LCL_MAP_TYPE = "pMapType"
    const val PREFS_LCL_MAP_TYPE_DEFAULT = MAP_TYPE_NORMAL
}
