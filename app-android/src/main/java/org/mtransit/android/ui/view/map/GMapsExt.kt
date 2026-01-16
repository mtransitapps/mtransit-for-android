package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.LocationUtils

fun LatLng.distanceToInMeters(other: LatLng) =
    LocationUtils.distanceToInMeters(
        this.latitude, this.longitude,
        other.latitude, other.longitude
    )
