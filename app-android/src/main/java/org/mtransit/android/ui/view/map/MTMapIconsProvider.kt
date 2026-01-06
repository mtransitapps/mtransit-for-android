package org.mtransit.android.ui.view.map

import org.mtransit.android.R
import org.mtransit.android.data.DataSourceType

object MTMapIconsProvider {

    @JvmStatic
    val oldDefaultIconDef = MTMapIconDef(R.drawable.map_icon_place_white_slim_original)

    @JvmStatic
    val defaultIconDef = MTMapIconDef(R.drawable.map_icon_stop_white_flat, 0.5f, 0.5f, true)

    @JvmStatic
    val defaultClusterIconDef = MTMapIconDef(R.drawable.map_icon_cluster_blur_white, 0.5f, 0.5f, true)

    @JvmStatic
    val Int.iconDef: MTMapIconDef get() = oldDefaultIconDef

    @JvmStatic
    val DataSourceType?.vehicleIconDef: MTMapIconDef
        get() = when (this) {
            DataSourceType.TYPE_BUS -> MTMapIconDef(R.drawable.baseline_directions_bus_white_48, 0.5f, 0.5f, true)
            else -> oldDefaultIconDef
        }
}
