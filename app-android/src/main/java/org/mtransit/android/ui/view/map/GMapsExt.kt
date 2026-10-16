package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.LocationUtils
import android.location.Location as AndroidLocation

fun AndroidLocation.toLatLng() = LatLng(this.latitude, this.longitude)

fun LatLng.toLocation(provider: String = "MT") = AndroidLocation(provider).apply {
    this.latitude = this@toLocation.latitude
    this.longitude = this@toLocation.longitude
}

fun LatLng.distanceToInMeters(other: LatLng) =
    LocationUtils.distanceToInMeters(
        this.latitude, this.longitude,
        other.latitude, other.longitude
    )
