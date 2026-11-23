package org.mtransit.android.data

import android.content.Context
import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.applyTo
import org.mtransit.android.commons.data.getAuthorityFromJson
import org.mtransit.android.commons.data.getLatFromJson
import org.mtransit.android.commons.data.getLngFromJson
import org.mtransit.android.commons.data.getNameFromJson
import org.mtransit.android.commons.data.toJsonCommon
import org.mtransit.android.ui.view.map.ExtendedMarkerOptions
import org.mtransit.android.ui.view.map.IMarker
import org.mtransit.android.ui.view.map.MTMapIconZoomGroup
import org.mtransit.android.ui.view.map.MTMapIconsProvider
import org.mtransit.android.ui.view.map.updateData
import org.mtransit.android.ui.view.map.updateFlat
import org.mtransit.android.ui.view.map.updatePosition
import org.mtransit.android.ui.view.map.updateSnippet
import org.mtransit.android.ui.view.map.updateTitle
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

val Place.latLng: LatLng get() = LatLng(this.lat, this.lng)

fun Place.updateMarker(
    marker: IMarker,
    context: Context,
) = marker.apply {
    val wasInfoWindowShown = this.isInfoWindowShown
    updatePosition(this@updateMarker.latLng)
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
    clusterGroup(-1) // exclude from clustering
    icon(context, iconDef.getZoomResId(zoomGroup), iconDef.getZoomSize(zoomGroup), iconDef.replaceColor, iconColorInt, null, iconColorInt)
    title(this@toExtendedMarkerOptions.name)
    snippet(this@toExtendedMarkerOptions.subTitle?.takeIf { it.isNotBlank() })
    data(this@toExtendedMarkerOptions) // used to know which place selected (to open Nearby screen...)
}

private const val JSON_PROVIDER_ID = "provider_id"
private const val JSON_LANG = "lang"
private const val JSON_READ_AT_IN_MS = "read_at_in_ms"
private const val JSON_SUB_TITLE = "sub_title"
private const val JSON_ICON_URL = "icon_url"
private const val JSON_ICON_BG_COLOR = "icon_bg_color"

fun Place.toJsonK() = try {
    buildJsonObject {
        putK(JSON_PROVIDER_ID, this@toJsonK.providerId)
        putK(JSON_LANG, this@toJsonK.lang)
        putK(JSON_READ_AT_IN_MS, this@toJsonK.readAtInMs)
        this@toJsonK.subTitle?.let { putK(JSON_SUB_TITLE, it) }
        this@toJsonK.iconUrl?.let { putK(JSON_ICON_URL, it) }
        this@toJsonK.iconBgColor?.let { putK(JSON_ICON_BG_COLOR, it) }
        toJsonCommon(this@toJsonK)
    }
} catch (se: SerializationException) {
    MTLogK.w(this, "Error while converting to JSON ($this)!", se)
    throw se
}

@Suppress("unused")
fun JsonObject.toPlace() = try {
    Place(
        getAuthorityFromJson(),
        getValue(JSON_PROVIDER_ID).jsonPrimitive.content,
        getValue(JSON_LANG).jsonPrimitive.content,
        getValue(JSON_READ_AT_IN_MS).jsonPrimitive.long,
        getLatFromJson(),
        getLngFromJson(),
        getNameFromJson(),
    ).apply {
        subTitle = this@toPlace[JSON_SUB_TITLE]?.jsonPrimitiveOrNull?.contentOrNull
        iconUrl = this@toPlace[JSON_ICON_URL]?.jsonPrimitiveOrNull?.contentOrNull
        iconBgColor = this@toPlace[JSON_ICON_BG_COLOR]?.jsonPrimitiveOrNull?.intOrNull
        applyTo(this)
    }
} catch (se: SerializationException) {
    MTLogK.w(Module.LOG_TAG, "Error while parsing JSON '$this'!", se)
    throw se
}
