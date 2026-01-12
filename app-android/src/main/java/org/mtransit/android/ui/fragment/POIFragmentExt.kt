package org.mtransit.android.ui.fragment

import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.latLng
import org.mtransit.android.data.location
import org.mtransit.android.ui.view.map.distanceToInMeters
import org.mtransit.android.ui.view.map.position
import org.mtransit.android.ui.view.updateVehicleLocationMarkersCountdown
import org.mtransit.commons.FeatureFlags
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

fun POIFragment.startVehicleLocationCountdownRefresh() {
    _vehicleLocationCountdownRefreshJob?.cancel()
    _vehicleLocationCountdownRefreshJob = viewModel?.viewModelScope?.launch {
        while (true) {
            delay(1.seconds)
            context?.let { mapViewController.updateVehicleLocationMarkersCountdown(it) }
        }
    }
}

fun POIFragment.stopVehicleLocationCountdownRefresh() {
    _vehicleLocationCountdownRefreshJob?.cancel()
    _vehicleLocationCountdownRefreshJob = null
}

const val MAX_DISTANCE_TIMES = 3

val POIFragment.visibleMarkersLocationList: Collection<LatLng>?
    get() {
        if (!FeatureFlags.F_EXPORT_TRIP_ID) return null
        val poim = this.poim ?: return emptySet()
        val poimLatLng = poim.latLng ?: return emptySet()
        val visibleMarkersLocations = mutableSetOf<LatLng>()
        visibleMarkersLocations.add(poimLatLng)
        var nextRelevantPOIM: POIManager? = null
        viewModel?.poiList?.value?.let { poims ->
            poims.indexOfFirst { it.poi.uuid == poim.poi.uuid }.takeIf { it > -1 }?.let { poimIndex ->
                nextRelevantPOIM = poims.getOrNull(poimIndex + 1) ?: poims.getOrNull(poimIndex - 1)  // next or previous
                nextRelevantPOIM?.latLng?.let { visibleMarkersLocations.add(it) }
            }
        }
        val distanceToNextRelevantPOIM = nextRelevantPOIM?.latLng?.distanceToInMeters(poimLatLng)?.times(MAX_DISTANCE_TIMES)
        val distanceToDeviceLocation = poim.poi.location?.let { deviceLocation?.distanceTo(it) } // in meters
        val maxDistanceInMeters = max(distanceToNextRelevantPOIM ?: 0f, distanceToDeviceLocation ?: 0f)
            .takeIf { it > 0f }
        viewModel?.vehicleLocations?.value
            ?.sortedBy { it.position.distanceToInMeters(poimLatLng) } // closest first
            ?.forEach { vehicleLocation ->
                if (maxDistanceInMeters != null && vehicleLocation.position.distanceToInMeters(poimLatLng) <= maxDistanceInMeters) {
                    visibleMarkersLocations.add(vehicleLocation.position)
                }
            }
        return visibleMarkersLocations
    }

const val PREVIOUS_STOP_ALPHA = 0.5f

fun POIFragment.getMapMarkerAlpha(position: Int): Float? {
    if (!FeatureFlags.F_EXPORT_TRIP_ID) return null
    val poim = this.poim ?: return null
    viewModel?.poiList?.value?.let { poims ->
        poims.indexOfFirst { it.poi.uuid == poim.poi.uuid }.takeIf { it > -1 }?.let { poimIndex ->
            if (position <= poimIndex) {
                return PREVIOUS_STOP_ALPHA
            }
        }
    }
    return null
}
