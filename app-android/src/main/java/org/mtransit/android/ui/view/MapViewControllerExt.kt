package org.mtransit.android.ui.view

import android.content.Context
import android.graphics.Color
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.ui.view.map.ExtendedMarkerOptions
import org.mtransit.android.ui.view.map.MTMapIconZoomGroup
import org.mtransit.android.ui.view.map.MTMapIconsProvider.vehicleIconDef
import org.mtransit.android.ui.view.map.getMapMarkerSnippet
import org.mtransit.android.ui.view.map.getMapMarkerTitle
import org.mtransit.android.ui.view.map.getRotation
import org.mtransit.android.ui.view.map.position
import org.mtransit.android.util.MapUtils

fun MapViewController.updateVehicleLocationMarkers(context: Context) {
    val googleMap = this.extendedGoogleMap ?: run {
        MTLog.d(this, "updateVehicleLocationMarkers() > SKIP (no google map)")
        return
    }
    val markerProvider = this.markerProviderWR?.get() ?: run {
        MTLog.d(this, "updateVehicleLocationMarkers() > SKIP (no marker provider)")
        return
    }
    val vehicleLocations = markerProvider.getVehicleLocations()?.takeIf { it.isNotEmpty() } ?: run {
        MTLog.d(this, "updateVehicleLocationMarkers() > SKIP (no vehicle locations)")
        removeMissingVehicleLocationMarkers()
        return
    }
    val currentZoomGroup = MTMapIconZoomGroup.from(googleMap.getCameraPosition().zoom, null)
    val vehicleColorInt = markerProvider.getVehicleColorInt()
    val vehicleDst = markerProvider.getVehicleType()
    val processedVehicleLocationsUUIDs = mutableSetOf<String>()
    var index = 0
    for (vehicleLocation in vehicleLocations) {
        val iconDef = vehicleDst.vehicleIconDef
        val title = vehicleLocation.getMapMarkerTitle(context)
        val snippet = vehicleLocation.getMapMarkerSnippet(context)
        val rotation = vehicleLocation.getRotation(default = 0.0f)
        val uuid = vehicleLocation.uuid
            ?: ("generated-uuid-" + TimeUtils.currentTimeMillis() + "-" + index) // cannot update
                .also { MTLog.d(this, "updateVehicleLocationMarkers() > FAKE uuid: %s.", it) }
        var marker = this.vehicleLocationsMarkers.get(uuid)
        if (marker == null) { // ADD new
            marker = googleMap.addMarker(
                ExtendedMarkerOptions()
                    .position(vehicleLocation.position)
                    .anchor(iconDef.anchorU, iconDef.anchorV)
                    .infoWindowAnchor(iconDef.inforWindowAnchorU, iconDef.inforWindowAnchorV)
                    .flat(iconDef.flat)
                    .icon(context, iconDef.getZoomResId(currentZoomGroup), vehicleColorInt, null, Color.BLACK)
                    .rotation(rotation)
                    .title(title)
                    .snippet(snippet)
                    .data(vehicleLocation)
                    .zIndex(MapUtils.MAP_MARKER_HIGHEST)
            )
            this.vehicleLocationsMarkers[uuid] = marker
        } else { // UPDATE existing
            marker.apply {
                animatePosition(vehicleLocation.position)
                setAnchor(iconDef.anchorU, iconDef.anchorV)
                setInfoWindowAnchor(iconDef.inforWindowAnchorU, iconDef.inforWindowAnchorV)
                setFlat(iconDef.flat)
                setIcon(context, iconDef.getZoomResId(currentZoomGroup), vehicleColorInt, null, Color.BLACK)
                setRotation(rotation)
                setTitle(title)
                setSnippet(snippet)
                setData(vehicleLocation)
                setZIndex(MapUtils.MAP_MARKER_HIGHEST)
            }
        }
        processedVehicleLocationsUUIDs.add(uuid)
        index++
    }
    removeMissingVehicleLocationMarkers(processedVehicleLocationsUUIDs)
}

@JvmOverloads
fun MapViewController.removeMissingVehicleLocationMarkers(
    processedVehicleLocationsUUIDs: Set<String> = emptySet(),
) {
    this.vehicleLocationsMarkers.entries.forEach { (uuid, _) ->
        this.vehicleLocationsMarkers.remove(uuid)?.let { // REMOVE
            it.remove()
        }
    }
}

fun MapViewController.updateVehicleLocationMarkersCountdown(context: Context) {
    this.vehicleLocationsMarkers.entries.forEach { (_, marker) ->
        val marker = marker.takeIf { it.isInfoWindowShown } ?: return@forEach
        val vehicleLocation = marker.getData<VehicleLocation>() ?: return@forEach
        vehicleLocation.getMapMarkerTitle(context)?.takeIf { it != marker.title }?.let {
            marker.title = it
        }
        vehicleLocation.getMapMarkerSnippet(context)?.takeIf { it != marker.snippet }?.let {
            marker.snippet = it
        }
    }
}
