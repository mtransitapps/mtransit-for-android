package org.mtransit.android.ui.view.map

import org.mtransit.android.R
import org.mtransit.android.data.DataSourceType

object MTMapIconsProvider {

    //@formatter:off

    @JvmStatic
    val selectedDefaultIconDef = MTMapIconDef(R.drawable.map_icon_place_white_slim_original)

    @JvmStatic
    val defaultIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat, R.drawable.map_icon_stop_replace_flat_small, true, 0.5f, 0.5f, replaceColor = true)

    @JvmStatic
    val arrowIconDef = MTMapIconDef(R.drawable.map_icon_stop_white_flat_arrow, R.drawable.map_icon_stop_white_flat_arrow_small, true, 0.5f, 0.5f)

    @JvmStatic
    val defaultClusterIconDef = MTMapIconDef(R.drawable.map_icon_cluster_blur_white, R.drawable.map_icon_cluster_blur_white, true, 0.5f, 0.5f)

    @JvmStatic
    val lightRailVehicleIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat_filled_tram_big, R.drawable.map_icon_stop_replace_flat_filled_tram, true, 0.5f, 0.5f, replaceColor = true)

    @JvmStatic
    val subwayVehicleIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat_filled_subway_big, R.drawable.map_icon_stop_replace_flat_filled_subway, true, 0.5f, 0.5f, replaceColor = true)

    @JvmStatic
    val railVehicleIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat_filled_railway_big, R.drawable.map_icon_stop_replace_flat_filled_railway, true, 0.5f, 0.5f, replaceColor = true)

    @JvmStatic
    val busVehicleIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat_filled_bus_big, R.drawable.map_icon_stop_replace_flat_filled_bus, true, 0.5f, 0.5f, replaceColor = true)

    @JvmStatic
    val ferryVehicleIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_white_flat_filled_boat_big, R.drawable.map_icon_stop_white_flat_filled_boat, true, 0.5f, 0.5f)

    //@formatter:on

    @JvmStatic
    val Float?.iconDefForRotation: MTMapIconDef
        get() = if (this != null) arrowIconDef else defaultIconDef

    @JvmStatic
    val DataSourceType?.vehicleIconDef: MTMapIconDef
        get() = when (this) {
            DataSourceType.TYPE_LIGHT_RAIL, DataSourceType.TYPE_TRAM -> lightRailVehicleIconDef
            DataSourceType.TYPE_SUBWAY -> subwayVehicleIconDef
            DataSourceType.TYPE_RAIL -> railVehicleIconDef
            DataSourceType.TYPE_BUS -> busVehicleIconDef
            DataSourceType.TYPE_FERRY -> ferryVehicleIconDef
            else -> defaultIconDef // should not happen?
        }
}
