package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.VisibleRegion
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.ui.view.MapViewController

fun VisibleRegion.toArea() = this.latLngBounds.toArea()

fun LatLngBounds.toArea(): Area {
    return Area(
        minLat = this.southwest.latitude,
        maxLat = this.northeast.latitude,
        minLng = this.southwest.longitude,
        maxLng = this.northeast.longitude
    )
}

fun Area.countPOIMarkersInside(poiMarkers: Collection<MapViewController.POIMarker>?): Int {
    return poiMarkers?.count { this.isInside(it.position.latitude, it.position.longitude) } ?: 0
}

fun Area.countMarkersInside(markers: Collection<IMarker>?): Int {
    return markers?.count { this.isInside(it.position.latitude, it.position.longitude) } ?: 0
}

fun Area.countVehicleLocationsInside(markers: Collection<VehicleLocation>?): Int {
    return markers?.count { this.isInside(it.latitude.toDouble(), it.longitude.toDouble()) } ?: 0
}
