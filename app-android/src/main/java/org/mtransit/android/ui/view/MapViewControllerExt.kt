package org.mtransit.android.ui.view

import android.content.Context
import android.view.View
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.R
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.dpToPx
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.data.Place
import org.mtransit.android.data.latLng
import org.mtransit.android.data.toExtendedMarkerOptions
import org.mtransit.android.data.updateMarker
import org.mtransit.android.ui.location.UILocationUtils
import org.mtransit.android.ui.location.latLng
import org.mtransit.android.ui.location.toNameOnly
import org.mtransit.android.ui.view.map.MTMapIconZoomGroup
import org.mtransit.android.ui.view.map.MTMapIconsProvider.vehicleIconDef
import org.mtransit.android.ui.view.map.MapMarkerProvider
import org.mtransit.android.ui.view.map.countMarkersInside
import org.mtransit.android.ui.view.map.distanceToInMeters
import org.mtransit.android.ui.view.map.getMapMarkerAlpha
import org.mtransit.android.ui.view.map.getMapMarkerSnippet
import org.mtransit.android.ui.view.map.getMapMarkerTitle
import org.mtransit.android.ui.view.map.position
import org.mtransit.android.ui.view.map.toArea
import org.mtransit.android.ui.view.map.toExtendedMarkerOptions
import org.mtransit.android.ui.view.map.updateAlpha
import org.mtransit.android.ui.view.map.updateMarker
import org.mtransit.android.ui.view.map.updateSnippet
import org.mtransit.android.ui.view.map.updateTitle
import org.mtransit.android.ui.view.map.uuidOrGenerated
import org.mtransit.android.util.MapUtils
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import android.location.Address as AndroidAddress
import org.mtransit.android.commons.R as commonsR

@JvmOverloads
fun MapViewController.updateVehicleLocationMarkers(
    context: Context,
    selectedUuid: String? = this.lastSelectedUUID,
    markerProvider: MapMarkerProvider? = this.config.markerProvider,
    vehicleLocations: Collection<VehicleLocation>? = markerProvider?.vehicleLocations,
    avoidCollapseLatLng: LatLng? = null,
): Boolean {
    selectedUuid?.let { setInitialSelectedUUID(selectedUuid) }
    val googleMap = this.extendedGoogleMap ?: run {
        MTLog.d(this, "updateVehicleLocationMarkers() > SKIP (no google map) #:POIFragment")
        return false
    }
    val markerProvider = markerProvider ?: run {
        MTLog.d(this, "updateVehicleLocationMarkers() > SKIP (no marker provider) #:POIFragment")
        return false
    }
    val vehicleLocations = vehicleLocations?.takeIf { it.isNotEmpty() } ?: run {
        MTLog.d(this, "updateVehicleLocationMarkers() > SKIP (no vehicle locations) #:POIFragment")
        removeMissingVehicleLocationMarkers()
        return true
    }
    val visibleArea = googleMap.getProjection().visibleRegion.toArea()
    val visibleMarkersCount = visibleArea.countMarkersInside(googleMap.getMarkers()) +
        vehicleLocations.count { !this.vehicleLocationsMarkers.containsKey(it.uuid) }
    val currentZoomGroup = getCurrentMapIconZoomGroup(googleMap, visibleMarkersCount)
    val vehicleColorInt = markerProvider.vehicleColorInt
    val vehicleDst = markerProvider.vehicleType
    val processedVehicleLocationsUUIDs = mutableSetOf<String>()
    var index = 0
    vehicleLocations.forEach { vehicleLocation ->
        val iconDef = vehicleDst.vehicleIconDef
        val uuid = vehicleLocation.uuidOrGenerated
        val poiZoomGroup = getPOIZoomGroup(currentZoomGroup, isFocused = { it == uuid })
        var marker = this.vehicleLocationsMarkers[uuid]
        if (marker == null) { // ADD new
            marker = googleMap.addMarker(
                vehicleLocation.toExtendedMarkerOptions(context, iconDef, vehicleColorInt, poiZoomGroup, config.hideMapMarkerSnippet)
            )
            this.vehicleLocationsMarkers[uuid] = marker
        } else { // UPDATE existing
            vehicleLocation.updateMarker(marker, context, iconDef, vehicleColorInt, poiZoomGroup, config.hideMapMarkerSnippet)
        }
        if (selectedUuid == uuid
            && avoidCollapseLatLng?.let { areMarkerCollapsing(it, vehicleLocation.position) } != true
        ) {
            marker.showInfoWindow()
        } else {
            marker.hideInfoWindow()
            marker.setZIndex(MapViewController.MAP_MARKER_Z_INDEX_VEHICLE) // reset original Z-Index changed by showInfoWindow()
        }
        processedVehicleLocationsUUIDs.add(uuid)
        index++
    }
    removeMissingVehicleLocationMarkers(processedVehicleLocationsUUIDs)
    return true
}

fun MapViewController.areMarkerCollapsing(latLng1: LatLng, latLng2: LatLng): Boolean? {
    val projection = this.extendedGoogleMap?.projection ?: run {
        MTLog.d(this, "areMarkerCollapsing() > UNKNOWN (no map) #20260717")
        return null
    }
    val point1 = projection.toScreenLocation(latLng1)
    val point2 = projection.toScreenLocation(latLng2)
    val collapseMinDistancePx = (48 / 2).dpToPx
    val isCollapsed = abs(point1.x - point2.x) < collapseMinDistancePx
        && abs(point1.y - point2.y) < collapseMinDistancePx
    return isCollapsed
}

fun MapViewController.getPOIZoomGroup(currentZoomGroup: MTMapIconZoomGroup, isFocused: (String) -> Boolean) =
    focusedOnUUID?.let { if (isFocused(it)) MTMapIconZoomGroup.DEFAULT else MTMapIconZoomGroup.MEDIUM } ?: currentZoomGroup

@JvmOverloads
fun MapViewController.removeMissingVehicleLocationMarkers(
    processedVehicleLocationsUUIDs: Set<String> = emptySet(),
) {
    this.vehicleLocationsMarkers.entries.forEach { (uuid, _) ->
        if (processedVehicleLocationsUUIDs.contains(uuid)) return@forEach // KEEP
        this.vehicleLocationsMarkers.remove(uuid)?.remove()
    }
}

fun MapViewController.updateVehicleLocationMarkersCountdown(context: Context) {
    this.vehicleLocationsMarkers.entries.forEach { (_, marker) ->
        marker.apply {
            val vehicleLocation = getData<Any?>() as? VehicleLocation ?: return@forEach
            updateAlpha(vehicleLocation.getMapMarkerAlpha() ?: MapUtils.MAP_MARKER_ALPHA_DEFAULT)
            if (!isInfoWindowShown) return@forEach
            updateTitle(vehicleLocation.getMapMarkerTitle(context))
            updateSnippet(if (config.hideMapMarkerSnippet) null else vehicleLocation.getMapMarkerSnippet(context))
        }
    }
}

fun MapViewController.addOnLayoutChangeListener() {
    mapView?.addOnLayoutChangeListener(
        this.layoutChangeListener ?: View.OnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            val heightChanged = bottom != oldBottom // banner ad show/hide
            if (heightChanged && oldBottom > 0) {
                if (initialMapCameraSetup) {
                    initialMapCameraSetup = false
                    setupInitialCamera()
                }
            }
        }.also {
            this.layoutChangeListener = it
        }
    )
}

fun MapViewController.removeOnLayoutChangeListener() {
    this.layoutChangeListener?.let { mapView?.removeOnLayoutChangeListener(it) }
}

fun MapViewController.clearSelectedPlace() = this.extendedGoogleMap?.apply {
    selectedPlaceMarker?.remove()
    selectedPlaceMarker = null
}

fun MapViewController.onSelectedPlaceLocation(selectedLocation: LatLng, selectedAddress: AndroidAddress?) {
    val context: Context = activityOrNull ?: return
    var usedLocation = selectedLocation
    selectedAddress?.latLng
        ?.takeIf { it.distanceToInMeters(selectedLocation) <= UILocationUtils.PLACE_USE_ADDRESS_LAT_LNG_MAX_DISTANCE_IN_METER }
        ?.let {
            usedLocation = it // move selected PIN to exact location
        }
    val accuracyInMeters = selectedAddress?.let {
        LocationUtils.distanceToInMeters(
            selectedLocation.latitude, selectedLocation.longitude,
            it.latitude, it.longitude
        )
    } ?: 0.0F
    val subTitle = context.getString(R.string.place_pin_click_to_nearby)
    val name = selectedAddress
        ?.toNameOnly(context, accuracyInMeters <= UILocationUtils.PLACE_SHOW_ADDRESS_SELECTED_MAX_DISTANCE_IN_METER)
        ?.let {
            val maxLength = subTitle.length
            if (it.length > maxLength) {
                it.substring(0, it.length.coerceAtMost(maxLength - 1)) + context.getString(commonsR.string.ellipsis)
            } else {
                it
            }
        }
        ?: context.getString(R.string.place_pin_placed)
    val selectedPlace = Place(
        "android.location.Geocoder",
        UUID.randomUUID().toString(),
        selectedAddress?.locale?.language ?: Locale.getDefault().language,
        TimeUtils.currentTimeMillis(),
        usedLocation.latitude,
        usedLocation.longitude,
        name
    ).apply {
        this.subTitle = subTitle
    }
    setSelectedPlace(context, selectedPlace)
}

fun MapViewController.setSelectedPlace(context: Context, place: Place, selectedLocation: LatLng? = place.latLng) = this.extendedGoogleMap?.apply {
    selectedPlaceMarker?.let {
        if (it.position == selectedLocation) {
            place.updateMarker(it, context)
            return@apply
        }
    }
    clearSelectedPlace()
    selectedPlaceMarker = addMarker(place.toExtendedMarkerOptions(context))
        .apply {
            showInfoWindow()
            onMarkerClick(this)
        }
}
