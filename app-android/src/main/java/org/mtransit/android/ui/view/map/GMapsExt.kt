package org.mtransit.android.ui.view.map

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.LocationUtils

fun Location.toLatLng() = LatLng(this.latitude, this.longitude)

fun LatLng.distanceToInMeters(other: LatLng) =
    LocationUtils.distanceToInMeters(
        this.latitude, this.longitude,
        other.latitude, other.longitude
    )
