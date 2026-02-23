package org.mtransit.android.ui.view.map

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.ui.view.MapViewController
import org.mtransit.android.util.MapUtils
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.toDate
import kotlin.time.Duration.Companion.milliseconds

val VehicleLocation.position: LatLng get() = LatLng(this.latitude.toDouble(), this.longitude.toDouble())

val VehicleLocation.uuidOrGenerated: String
    get() = this.uuid
        ?: ("generated-uuid-" + TimeUtils.currentTimeMillis()).also { MTLog.d(this, "getUuidOrGenerated() > FAKE uuid: $it.") }

private const val MIN_ALPHA = 0.33f

fun VehicleLocation.getMapMarkerAlpha(): Float? {
    return reportTimestamp?.let {
        (TimeUtils.currentTimeMillis().milliseconds - it).toComponents { minutes, seconds, _ ->
            when {
                minutes > 0 -> MIN_ALPHA
                else -> MapUtils.MAP_MARKER_ALPHA_DEFAULT - (seconds / 60.0f)
            }.coerceIn(MIN_ALPHA, MapUtils.MAP_MARKER_ALPHA_DEFAULT)
        }
    }
}

fun VehicleLocation.getMapMarkerTitle(context: Context): String? =
    reportTimestamp?.let {
        (TimeUtils.currentTimeMillis().milliseconds - it).toComponents { days, hours, minutes, seconds, _ ->
            when {
                days > 0L -> context.getString(R.string.short_days_count, days)
                hours > 0L -> context.getString(R.string.short_hours_count, hours)
                minutes > 0L -> context.getString(R.string.short_minutes_count, minutes)
                else -> context.getString(R.string.short_seconds_count, seconds)
            }
        }
    }

fun VehicleLocation.getMapMarkerSnippet(context: Context): String? = buildList {
    vehicleLabel?.takeIf { it.isNotEmpty() }?.let { add(it) }
        ?: reportTimestampMs?.let { UITimeUtils.formatTime(false, context, it.toDate()) }?.let { add(it) }
}.takeIf { it.isNotEmpty() }?.joinToString(separator = " | ")

fun VehicleLocation.getRotation(default: Float) =
    this.bearingDegrees?.toFloat() ?: default

fun VehicleLocation.toExtendedMarkerOptions(
    context: Context,
    iconDef: MTMapIconDef,
    @ColorInt iconColorInt: Int?,
    currentZoomGroup: MTMapIconZoomGroup?,
    hideSnippet: Boolean,
) = ExtendedMarkerOptions().apply {
    position(this@toExtendedMarkerOptions.position)
    anchor(iconDef.anchorU, iconDef.anchorV)
    infoWindowAnchor(iconDef.infoWindowAnchorU, iconDef.infoWindowAnchorV)
    rotation(getRotation(default = 0.0f))
    flat(iconDef.flat)
    icon(context, iconDef.getZoomResId(currentZoomGroup), iconDef.getZoomSize(currentZoomGroup), iconDef.replaceColor, iconColorInt, null, Color.BLACK)
    title(getMapMarkerTitle(context))
    snippet(if (hideSnippet) null else getMapMarkerSnippet(context))
    alpha(getMapMarkerAlpha() ?: MapUtils.MAP_MARKER_ALPHA_DEFAULT)
    data(this@toExtendedMarkerOptions) // used to update marker with countdown
    zIndex(MapViewController.MAP_MARKER_Z_INDEX_VEHICLE)
}

fun VehicleLocation.updateMarker(
    marker: IMarker,
    context: Context,
    iconDef: MTMapIconDef,
    @ColorInt iconColorInt: Int?,
    currentZoomGroup: MTMapIconZoomGroup?,
    hideSnippet: Boolean,
) = marker.apply {
    val wasInfoWindowShown = this.isInfoWindowShown
    updatePosition(this@updateMarker.position, animate = true)
    setAnchor(iconDef.anchorU, iconDef.anchorV)
    setInfoWindowAnchor(iconDef.infoWindowAnchorU, iconDef.infoWindowAnchorV)
    updateRotation(getRotation(default = 0.0f))
    updateFlat(iconDef.flat)
    setIcon(context, iconDef.getZoomResId(currentZoomGroup), iconDef.getZoomSize(currentZoomGroup), iconDef.replaceColor, iconColorInt, null, Color.BLACK)
    updateTitle(getMapMarkerTitle(context))
    updateSnippet(if (hideSnippet) null else getMapMarkerSnippet(context))
    updateAlpha(getMapMarkerAlpha() ?: MapUtils.MAP_MARKER_ALPHA_DEFAULT)
    updateData(this@updateMarker) // used to update marker with countdown
    updateZIndex(MapViewController.MAP_MARKER_Z_INDEX_VEHICLE)
    if (wasInfoWindowShown) this.showInfoWindow()
}
