package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

interface MapListener {

    fun onMapClick(position: LatLng) = Unit

    fun onMapLongClick(position: LatLng) = Unit

    fun onMarkerClick(marker: IMarker?): Boolean = false

    fun onCameraChanged(latLngBounds: LatLngBounds, zoom: Float) = Unit

    fun onMapReady() = Unit
}
