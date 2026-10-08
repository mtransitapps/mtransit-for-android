package org.mtransit.android.ui.view

import org.mtransit.android.ui.view.map.MapListener
import org.mtransit.android.ui.view.map.MapMarkerProvider
import java.lang.ref.WeakReference

class MapViewConfig(
    markerProvider: MapMarkerProvider? = null,
    mapListener: MapListener? = null,
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
    val markerProviderWR: WeakReference<MapMarkerProvider> = WeakReference(markerProvider)
    val mapListenerWR: WeakReference<MapListener> = WeakReference(mapListener)

    val mapListener: MapListener? get() = this.mapListenerWR.get()
    val markerProvider: MapMarkerProvider? get() = this.markerProviderWR.get()
}
