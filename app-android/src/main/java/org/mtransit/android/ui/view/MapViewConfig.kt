package org.mtransit.android.ui.view

import java.lang.ref.WeakReference

data class MapViewConfig(
    val markerProviderWR: WeakReference<MapViewController.MapMarkerProvider> = WeakReference<MapViewController.MapMarkerProvider>(null),
    val mapListenerWR: WeakReference<MapViewController.MapListener> = WeakReference<MapViewController.MapListener>(null),
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
    val buildingsEnabled: Boolean = true, // ON by default in Google Maps SDK
) {
    constructor(
        markerProvider: MapViewController.MapMarkerProvider? = null,
        mapListener: MapViewController.MapListener? = null,
        mapToolbarEnabled: Boolean = false,
        myLocationEnabled: Boolean = false,
        myLocationButtonEnabled: Boolean = false,
        indoorLevelPickerEnabled: Boolean = false,
        trafficEnabled: Boolean = false,
        indoorEnabled: Boolean = false,
        paddingTopDp: Int = 0,
        paddingBottomDp: Int = 0,
        followingDevice: Boolean = false,
        hasButtons: Boolean = false,
        clusteringEnabled: Boolean = false,
        showAllMarkersWhenReady: Boolean = false,
        markerLabelShowExtra: Boolean = false,
        hideMapMarkerSnippet: Boolean = false,
        autoClickInfoWindow: Boolean = false,
        buildingsEnabled: Boolean = true, // ON by default in Google Maps SDK
    ) : this(
        markerProviderWR = WeakReference<MapViewController.MapMarkerProvider>(markerProvider),
        mapListenerWR = WeakReference<MapViewController.MapListener>(mapListener),
        mapToolbarEnabled = mapToolbarEnabled,
        myLocationEnabled = myLocationEnabled,
        myLocationButtonEnabled = myLocationButtonEnabled,
        indoorLevelPickerEnabled = indoorLevelPickerEnabled,
        trafficEnabled = trafficEnabled,
        indoorEnabled = indoorEnabled,
        paddingTopDp = paddingTopDp,
        paddingBottomDp = paddingBottomDp,
        followingDevice = followingDevice,
        hasButtons = hasButtons,
        clusteringEnabled = clusteringEnabled,
        showAllMarkersWhenReady = showAllMarkersWhenReady,
        markerLabelShowExtra = markerLabelShowExtra,
        hideMapMarkerSnippet = hideMapMarkerSnippet,
        autoClickInfoWindow = autoClickInfoWindow,
        buildingsEnabled = buildingsEnabled,
    )

    val mapListener: MapViewController.MapListener? get() = this.mapListenerWR.get()
    val markerProvider: MapViewController.MapMarkerProvider? get() = this.markerProviderWR.get()
}
