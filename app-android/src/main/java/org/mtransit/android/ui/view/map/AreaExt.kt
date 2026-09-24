package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.VisibleRegion
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import kotlin.math.max
import kotlin.math.min

fun VisibleRegion.toArea() = this.latLngBounds.toArea()

fun LatLngBounds.toArea() = Area(
    minLat = min(this.southwest.latitude, this.northeast.latitude),
    maxLat = max(this.northeast.latitude, this.southwest.latitude),
    minLng = min(this.southwest.longitude, this.northeast.longitude),
    maxLng = max(this.northeast.longitude, this.southwest.longitude),
)

fun Area.toLatLngBounds() = LatLngBounds.builder().apply {
    include(LatLng(this@toLatLngBounds.minLat, this@toLatLngBounds.minLng))
    include(LatLng(this@toLatLngBounds.minLat, this@toLatLngBounds.maxLng))
}.build()

fun Area.toLngLngList(): Collection<LatLng> = buildList {
    add(LatLng(this@toLngLngList.minLat, this@toLngLngList.minLng))
    add(LatLng(this@toLngLngList.minLat, this@toLngLngList.maxLng))
}

fun Area.countPOIInside(poiList: Collection<POI>?): Int {
    return poiList?.count { it.hasLocation() && this.isInside(it.lat, it.lng) } ?: 0
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
