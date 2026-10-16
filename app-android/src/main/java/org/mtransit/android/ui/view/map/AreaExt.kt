package org.mtransit.android.ui.view.map

import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import kotlin.math.max
import kotlin.math.min
import com.google.android.gms.maps.model.LatLng as GLatLng
import com.google.android.gms.maps.model.LatLngBounds as GLatLngBounds
import com.google.android.gms.maps.model.VisibleRegion as GVisibleRegion

fun Area.toLatLngBounds() = GLatLngBounds.builder()
    .include(GLatLng(this@toLatLngBounds.minLat, this@toLatLngBounds.minLng))
    .include(GLatLng(this@toLatLngBounds.maxLat, this@toLatLngBounds.maxLng))
    .build()

@Suppress("unused")
fun GLatLng.isInside(area: Area) = Area.isInside(this.latitude, this.longitude, area)

fun GVisibleRegion.toArea() = this.latLngBounds.toArea()

fun GLatLngBounds.toArea() = Area(
    minLat = min(this.southwest.latitude, this.northeast.latitude),
    maxLat = max(this.northeast.latitude, this.southwest.latitude),
    minLng = min(this.southwest.longitude, this.northeast.longitude),
    maxLng = max(this.northeast.longitude, this.southwest.longitude),
)

fun Area.toLngLngList() = buildList {
    add(GLatLng(this@toLngLngList.minLat, this@toLngLngList.minLng))
    add(GLatLng(this@toLngLngList.maxLat, this@toLngLngList.maxLng))
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

@Suppress("unused")
fun Area.countVehicleLocationsInside(markers: Collection<VehicleLocation>?): Int {
    return markers?.count { this.isInside(it.latitude.toDouble(), it.longitude.toDouble()) } ?: 0
}
