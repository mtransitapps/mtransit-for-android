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

    //@formatter:on

    @JvmStatic
    val Float?.iconDefForRotation: MTMapIconDef
        get() = if (this != null) arrowIconDef else defaultIconDef
}
