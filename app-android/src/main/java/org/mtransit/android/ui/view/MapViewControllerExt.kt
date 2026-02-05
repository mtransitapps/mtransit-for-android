package org.mtransit.android.ui.view

import android.content.Context
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.ui.view.map.MTMapIconsProvider.vehicleIconDef
import org.mtransit.android.ui.view.map.countMarkersInside
import org.mtransit.android.ui.view.map.getMapMarkerAlpha
import org.mtransit.android.ui.view.map.getMapMarkerSnippet
import org.mtransit.android.ui.view.map.getMapMarkerTitle
import org.mtransit.android.ui.view.map.toArea
import org.mtransit.android.ui.view.map.toExtendedMarkerOptions
import org.mtransit.android.ui.view.map.updateAlpha
import org.mtransit.android.ui.view.map.updateMarker
import org.mtransit.android.ui.view.map.updateSnippet
import org.mtransit.android.ui.view.map.updateTitle
import org.mtransit.android.ui.view.map.uuidOrGenerated
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
    val visibleArea = googleMap.getProjection().visibleRegion.toArea()
    val visibleMarkersCount = visibleArea.countMarkersInside(googleMap.getMarkers()) +
            vehicleLocations.count { !this.vehicleLocationsMarkers.containsKey(it.uuid) }
    val currentZoomGroup = getCurrentMapIconZoomGroup(googleMap, visibleMarkersCount)
    val vehicleColorInt = markerProvider.getVehicleColorInt()
    val vehicleDst = markerProvider.getVehicleType()
    val processedVehicleLocationsUUIDs = mutableSetOf<String>()
    var index = 0
    vehicleLocations.forEach { vehicleLocation ->
        val iconDef = vehicleDst.vehicleIconDef
        val uuid = vehicleLocation.uuidOrGenerated
        var marker = this.vehicleLocationsMarkers[uuid]
        if (marker == null) { // ADD new
            marker = googleMap.addMarker(
                vehicleLocation.toExtendedMarkerOptions(context, iconDef, vehicleColorInt, currentZoomGroup)
            )
            this.vehicleLocationsMarkers[uuid] = marker
        } else { // UPDATE existing
            vehicleLocation.updateMarker(marker, context, iconDef, vehicleColorInt, currentZoomGroup)
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
        if (processedVehicleLocationsUUIDs.contains(uuid)) return@forEach // KEEP
        this.vehicleLocationsMarkers.remove(uuid)?.remove()
    }
}

fun MapViewController.updateVehicleLocationMarkersCountdown(context: Context) {
    this.vehicleLocationsMarkers.entries.forEach { (_, marker) ->
        val vehicleLocation = marker.getData<VehicleLocation>() ?: return@forEach
        marker.updateAlpha(vehicleLocation.getMapMarkerAlpha() ?: MapUtils.MAP_MARKER_ALPHA_DEFAULT)
        if (!marker.isInfoWindowShown) return@forEach
        marker.updateTitle(vehicleLocation.getMapMarkerTitle(context))
        marker.updateSnippet(vehicleLocation.getMapMarkerSnippet(context))
    }
}
