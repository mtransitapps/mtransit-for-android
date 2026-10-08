package org.mtransit.android.data

import android.content.Context
import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.ui.view.map.ExtendedMarkerOptions
import org.mtransit.android.ui.view.map.IMarker
import org.mtransit.android.ui.view.map.MTMapIconZoomGroup
import org.mtransit.android.ui.view.map.MTMapIconsProvider
import org.mtransit.android.ui.view.map.updateData
import org.mtransit.android.ui.view.map.updateFlat
import org.mtransit.android.ui.view.map.updatePosition
import org.mtransit.android.ui.view.map.updateSnippet
import org.mtransit.android.ui.view.map.updateTitle

val Place.latLng: LatLng get() = LatLng(this.lat, this.lng)

fun Place.updateMarker(
    marker: IMarker,
    context: Context,
) = marker.apply {
    val wasInfoWindowShown = this.isInfoWindowShown
    updatePosition(this@updateMarker.latLng, animate = true)
    val darkTheme = ColorUtils.isDarkTheme(context)
    val iconColorInt = if (darkTheme) Color.LTGRAY else Color.DKGRAY
    val iconDef = MTMapIconsProvider.selectedDefaultIconDef
    val zoomGroup = MTMapIconZoomGroup.DEFAULT
    setAnchor(iconDef.anchorU, iconDef.anchorV)
    setInfoWindowAnchor(iconDef.infoWindowAnchorU, iconDef.infoWindowAnchorV)
    updateFlat(iconDef.flat)
    setIcon(context, iconDef.getZoomResId(zoomGroup), iconDef.getZoomSize(zoomGroup), iconDef.replaceColor, iconColorInt, null, iconColorInt)
    updateTitle(this@updateMarker.name)
    updateSnippet(this@updateMarker.subTitle?.takeIf { it.isNotBlank() })
    updateData(this@updateMarker) // used to update marker with countdown
    if (wasInfoWindowShown) this.showInfoWindow()
}

fun Place.toExtendedMarkerOptions(context: Context) = ExtendedMarkerOptions().apply {
    position(this@toExtendedMarkerOptions.latLng)
    val darkTheme = ColorUtils.isDarkTheme(context)
    val iconColorInt = if (darkTheme) Color.LTGRAY else Color.DKGRAY
    val iconDef = MTMapIconsProvider.selectedDefaultIconDef
    val zoomGroup = MTMapIconZoomGroup.DEFAULT
    anchor(iconDef.anchorU, iconDef.anchorV)
    infoWindowAnchor(iconDef.infoWindowAnchorU, iconDef.infoWindowAnchorV)
    flat(iconDef.flat)
    draggable(true)
    icon(context, iconDef.getZoomResId(zoomGroup), iconDef.getZoomSize(zoomGroup), iconDef.replaceColor, iconColorInt, null, iconColorInt)
    title(this@toExtendedMarkerOptions.name)
    snippet(this@toExtendedMarkerOptions.subTitle?.takeIf { it.isNotBlank() })
    data(this@toExtendedMarkerOptions) // used to know which place selected (to open Nearby screen...)
}
