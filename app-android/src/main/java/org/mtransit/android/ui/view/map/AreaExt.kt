package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.VisibleRegion
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation

fun VisibleRegion.toArea() = this.latLngBounds.toArea()

fun LatLngBounds.toArea(): Area {
    return Area(
        minLat = this.southwest.latitude,
        maxLat = this.northeast.latitude,
        minLng = this.southwest.longitude,
        maxLng = this.northeast.longitude
    )
}

fun Area.countPOIInside(poiList: Collection<POI>?): Int {
    return poiList?.count { it.hasLocation() && this.isInside(it.lat, it.lat) } ?: 0
}

fun Area.countPOIMarkersInside(poiMarkers: Collection<MTPOIMarker>?): Int {
    return poiMarkers?.count { this.isInside(it.position.latitude, it.position.longitude) } ?: 0
}

fun Area.countMarkersInside(markers: Collection<IMarker>?): Int {
    return markers?.count { this.isInside(it.position.latitude, it.position.longitude) } ?: 0
}

fun Area.countVehicleLocationsInside(markers: Collection<VehicleLocation>?): Int {
    return markers?.count { this.isInside(it.latitude.toDouble(), it.longitude.toDouble()) } ?: 0
}
