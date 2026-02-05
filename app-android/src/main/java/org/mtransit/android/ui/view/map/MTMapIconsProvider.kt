package org.mtransit.android.ui.view.map

import org.mtransit.android.R
import org.mtransit.android.commons.dp
import org.mtransit.android.data.DataSourceType

object MTMapIconsProvider {

    //@formatter:off

    @JvmStatic
    val selectedDefaultIconDef = MTMapIconDef(R.drawable.map_icon_place_replace_slim_original_nodpi, size= 36.dp, smallSize = 36.dp, replaceColor = true)

    @JvmStatic
    val defaultIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat_nodpi, size = 21.dp, flat = true, anchorU = 0.5f, anchorV = 0.5f, replaceColor = true)

    @JvmStatic
    val arrowIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat_arrow_nodpi, size = 21.dp, flat = true, anchorU = 0.5f, anchorV = 0.5f, replaceColor = true)

    @JvmStatic
    val defaultClusterIconDef =
        MTMapIconDef(R.drawable.map_icon_cluster_blur_replace_nodpi, size = 48.dp, smallSize = 48.dp, flat = true, anchorU = 0.5f, anchorV = 0.5f, replaceColor = true)

    @JvmStatic
    val lightRailVehicleIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat_filled_tram_nodpi, size = 41.dp, flat = true, anchorU = 0.5f, anchorV = 0.5f, replaceColor = true)

    @JvmStatic
    val subwayVehicleIconDef =
        MTMapIconDef( R.drawable.map_icon_stop_replace_flat_filled_subway_nodpi, size = 41.dp, flat = true, anchorU = 0.5f, anchorV = 0.5f, replaceColor = true)

    @JvmStatic
    val railVehicleIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat_filled_railway_nodpi, size = 41.dp, flat = true, anchorU = 0.5f, anchorV = 0.5f, replaceColor = true)

    @JvmStatic
    val busVehicleIconDef =
        MTMapIconDef(R.drawable.map_icon_stop_replace_flat_filled_bus_nodpi, size = 41.dp, flat = true, anchorU = 0.5f, anchorV = 0.5f, replaceColor = true)

    @JvmStatic
    val ferryVehicleIconDef =
        MTMapIconDef( R.drawable.map_icon_stop_replace_flat_filled_boat_nodpi, size = 41.dp, flat = true, anchorU = 0.5f, anchorV = 0.5f, replaceColor = true)

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
