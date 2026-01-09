package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation

val VehicleLocation.position: LatLng get() = LatLng(this.latitude.toDouble(), this.longitude.toDouble())
