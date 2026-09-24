package org.mtransit.android.ui.view.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

interface MapListener {

    fun onMapClick(position: LatLng)

    fun onMapLongClick(position: LatLng)

    fun onMarkerClick(marker: IMarker?): Boolean

    fun onCameraChanged(latLngBounds: LatLngBounds, zoom: Float)

    fun onMapReady()
}
