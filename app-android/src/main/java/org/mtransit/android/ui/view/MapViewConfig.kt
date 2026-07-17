package org.mtransit.android.ui.view

import java.lang.ref.WeakReference

class MapViewConfig(
    markerProvider: MapViewController.MapMarkerProvider? = null,
    mapListener: MapViewController.MapListener? = null,
    val mapToolbarEnabled: Boolean = false,
    val myLocationEnabled: Boolean = false,
    val myLocationButtonEnabled: Boolean = false,
    val indoorLevelPickerEnabled: Boolean = false,
    val trafficEnabled: Boolean = false,
    val indoorEnabled: Boolean = false,
    var paddingTopDp: Int = 0,
    var paddingBottomDp: Int = 0,
    val followingDevice: Boolean = false,
    val hasButtons: Boolean = false,
    val clusteringEnabled: Boolean = false,
    var showAllMarkersWhenReady: Boolean = false,
    val markerLabelShowExtra: Boolean = false,
    val hideMapMarkerSnippet: Boolean = false,
    val autoClickInfoWindow: Boolean = false,
    val buildingsEnabled: Boolean = true,
) {
    val markerProviderWR: WeakReference<MapViewController.MapMarkerProvider> = WeakReference(markerProvider)
    val mapListenerWR: WeakReference<MapViewController.MapListener> = WeakReference(mapListener)

    val mapListener: MapViewController.MapListener? get() = this.mapListenerWR.get()
    val markerProvider: MapViewController.MapMarkerProvider? get() = this.markerProviderWR.get()
}
