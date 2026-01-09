package org.mtransit.android.ui.view.map

import org.mtransit.android.R
import org.mtransit.android.data.DataSourceType

object MTMapIconsProvider {

    @JvmStatic
    val selectedDefaultIconDef = MTMapIconDef(R.drawable.map_icon_place_white_slim_original)

    @JvmStatic
    val defaultIconDef = MTMapIconDef(R.drawable.map_icon_stop_white_flat, R.drawable.map_icon_stop_white_flat_small, 0.5f, 0.5f, true)

    @JvmStatic
    val arrowIconDef = MTMapIconDef(R.drawable.map_icon_stop_white_flat_arrow, R.drawable.map_icon_stop_white_flat_arrow_small, 0.5f, 0.5f, true)

    @JvmStatic
    val defaultClusterIconDef = MTMapIconDef(R.drawable.map_icon_cluster_blur_white, R.drawable.map_icon_cluster_blur_white, 0.5f, 0.5f, true)

    @JvmStatic
    val busVehicleIconDef = MTMapIconDef(R.drawable.map_icon_stop_white_flat_filled_bus_big, R.drawable.map_icon_stop_white_flat_filled_bus, 0.5f, 0.5f, false)


    @JvmStatic
    val DataSourceType?.vehicleIconDef: MTMapIconDef
        get() = when (this) {
            DataSourceType.TYPE_BUS -> busVehicleIconDef
            else -> selectedDefaultIconDef
        }
}
