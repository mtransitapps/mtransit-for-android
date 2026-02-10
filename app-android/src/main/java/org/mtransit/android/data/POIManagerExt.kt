package org.mtransit.android.data

import android.content.Context
import android.location.Location
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.util.UIAccessibilityUtils

@Suppress("unused")
fun Iterable<POIManager>.toStringUUID(): String {
    return this.joinToString { it.poi.uuid }
}

fun POI.getLabelDecorated(context: Context, isShowingAccessibilityInfo: Boolean): CharSequence {
    return UIAccessibilityUtils.decorate(context, this.label, isShowingAccessibilityInfo, UIAccessibilityUtils.ImageSize.LARGE, alignBottom = false)
}

fun <P : POI> P.toPOIM(
    serviceUpdates: List<ServiceUpdate>? = null,
    status: POIStatus? = null
) = POIManager(this).apply {
    setServiceUpdates(serviceUpdates)
    status?.let { setStatus(it) }
}

val POIManager.location: Location? get() = this.poi.location
val POI.location: Location? get() = if (this.hasLocation()) LocationUtils.getNewLocation(this.lat, this.lng) else null

val POIManager.latLng: LatLng? get() = this.poi.latLng
val POI.latLng: LatLng? get() = if (this.hasLocation()) LatLng(this.lat, this.lng) else null

@Suppress("unused")
fun POIManager.distanceToInMeters(other: POIManager) = this.poi.distanceToInMeters(other.poi)
fun POI.distanceToInMeters(other: POI): Float? =
    if (this.hasLocation() && other.hasLocation()) LocationUtils.distanceToInMeters(this.lat, this.lng, other.lat, other.lng) else null

@Suppress("unused")
fun POIManager.bearingTo(other: POIManager?): Float? = this.poi.bearingTo(other?.poi)
fun POI.bearingTo(other: POI?): Float? =
    other?.location?.let { this.location?.bearingTo(it) }

fun POI.getNewOneLineDescriptionForNews(dataSourcesRepository: DataSourcesRepository) =
    getNewOneLineDescriptionForNews { dataSourcesRepository.getAgency(getAuthority()) }

fun POI.getNewOneLineDescriptionForNews(agencyResolver: POIManager.AgencyResolver) = buildString {
    if (this@getNewOneLineDescriptionForNews is RouteDirectionStop) {
        append(route.shortestName)
    }
    agencyResolver.agency?.let {
        if (isNotEmpty()) append(" - ")
        append(it.shortName)
    }
}

fun POI.getNewOneLineSubtitleForSchedule(context: Context?, agencyProperties: IAgencyUIProperties?) =
    getNewOneLineSubtitleForSchedule(context) { agencyProperties }

fun POI.getNewOneLineSubtitleForSchedule(context: Context?, agencyResolver: POIManager.AgencyResolver) = buildSpannedString {
    if (this@getNewOneLineSubtitleForSchedule is RouteDirectionStop) {
        bold {
            append(route.shortestName)
            context?.let { direction.getUIHeading(context, true) }?.takeIf { it.isNotBlank() }?.let { append(" ").append(it) }
                ?: direction.heading?.takeIf { it.isNotBlank() }?.let { append(" -> ").append(it) }
        }
    }
    agencyResolver.agency?.let {
        if (isNotEmpty()) append(" ")
        append("(")
        append(it.shortName)
        append(")")
    }
}

fun POI.getNewOneLineTitleForSchedule() = buildSpannedString {
    if (this@getNewOneLineTitleForSchedule is RouteDirectionStop) {
        append(stop.name)
        stop.code.takeIf { it.isNotBlank() }?.let {
            append(" ")
            scale(RouteDirectionStop.STOP_CODE_SIZE.sizeChange) {
                append(it)
            }
        }
    } else {
        append(name)
    }
}

val POIManager.simpleDistanceString: String
    get() = this.distance.takeIf { it >= 0f }?.let { "${it}m" } ?: "?m"

@Suppress("unused")
val POIManager.uuidAndDistance: String
    get() = this.poi.uuid + " " + this.simpleDistanceString

@Suppress("unused")
val POIManager.shortUUIDAndDistance: String
    get() = this.poi.shortUUID + " " + this.simpleDistanceString

@Suppress("unused")
val POIManager.shortUUID: String get() = this.poi.shortUUID
val POI.shortUUID: String get() = this.uuid.substring(this.authority.length + 1)
