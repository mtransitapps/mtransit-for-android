package org.mtransit.android.data

import android.content.Context
import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.ui.view.map.ExtendedMarkerOptions
import org.mtransit.android.ui.view.map.MTMapIconZoomGroup
import org.mtransit.android.ui.view.map.MTMapIconsProvider

val Place.latLng: LatLng get() = LatLng(this.lat, this.lng)

fun Place.toExtendedMarkerOptions(context: Context) = ExtendedMarkerOptions().apply {
    position(this@toExtendedMarkerOptions.latLng)
    title(this@toExtendedMarkerOptions.name)
    snippet(this@toExtendedMarkerOptions.subTitle?.takeIf { it.isNotBlank() })
    val darkTheme = ColorUtils.isDarkTheme(context)
    val iconColorInt = if (darkTheme) Color.LTGRAY else Color.DKGRAY
    val iconDef = MTMapIconsProvider.selectedDefaultIconDef
    val zoomGroup = MTMapIconZoomGroup.DEFAULT
    anchor(iconDef.anchorU, iconDef.anchorV)
    infoWindowAnchor(iconDef.infoWindowAnchorU, iconDef.infoWindowAnchorV)
    flat(iconDef.flat)
    draggable(true)
    icon(context, iconDef.getZoomResId(zoomGroup), iconDef.getZoomSize(zoomGroup), iconDef.replaceColor, iconColorInt, null, iconColorInt)
    data(this@toExtendedMarkerOptions) // used to know which place selected (to open Nearby screen...)
}
