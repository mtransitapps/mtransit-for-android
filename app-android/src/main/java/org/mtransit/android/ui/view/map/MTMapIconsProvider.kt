package org.mtransit.android.ui.view.map

import androidx.annotation.DrawableRes
import org.mtransit.android.R

object MTMapIconsProvider {

    @JvmStatic
    val oldDefaultIconDef = MTMapIconDef(R.drawable.map_icon_place_white_slim_original)

    @JvmStatic
    val defaultIconDef = MTMapIconDef(R.drawable.map_icon_stop_white_flat, 0.5f, 0.5f, true)

    @JvmStatic
    val defaultClusterIconDef = MTMapIconDef(R.drawable.map_icon_cluster_blur_white, 0.5f, 0.5f, true)

    // TODO: actually no, we should only show place icon or generic stop icon for stops/stations
    // type icons should be reserved for actual vehicles
    @JvmStatic
    @get:DrawableRes
    val Int.iconDef: MTMapIconDef get() = defaultIconDef
}
