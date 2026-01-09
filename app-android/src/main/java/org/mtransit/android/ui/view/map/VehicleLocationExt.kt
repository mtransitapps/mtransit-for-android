package org.mtransit.android.ui.view.map

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.R
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.toDate
import kotlin.time.Duration.Companion.milliseconds

val VehicleLocation.position: LatLng get() = LatLng(this.latitude.toDouble(), this.longitude.toDouble())

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

fun VehicleLocation.getMapMarkerSnippet(context: Context): String? =
    vehicleLabel?.takeIf { it.isNotEmpty() }
        ?: reportTimestampMs?.let { UITimeUtils.formatTime(false, context, it.toDate()) }

fun VehicleLocation.getRotation(default: Float) =
    this.bearing ?: default
