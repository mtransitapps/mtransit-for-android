package org.mtransit.android.ui.fragment

import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mtransit.android.commons.data.RouteDirectionStop
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
                val previousPOIM = poims.getOrNull(poimIndex - 1)
                nextRelevantPOIM = poims.getOrNull(poimIndex + 1) ?: previousPOIM  // next or previous
                nextRelevantPOIM?.latLng?.let { visibleMarkersLocations.add(it) }
                previousPOIM?.latLng?.let { visibleMarkersLocations.add(it) }
            }
        }
        val maxVehicleDistanceInMeters: Float? = run {
            val distanceToNextRelevantPOIM = nextRelevantPOIM?.latLng?.distanceToInMeters(poimLatLng)
                ?.times(MAX_DISTANCE_TIMES)
            val distanceToDeviceLocation = poim.poi.location?.let { deviceLocation?.distanceTo(it) } // in meters
            max(distanceToNextRelevantPOIM ?: 0f, distanceToDeviceLocation ?: 0f)
        }.takeIf { it > 0f }
        viewModel?.vehicleLocations?.value
            ?.sortedBy { it.position.distanceToInMeters(poimLatLng) } // closest first
            ?.forEach { vehicleLocation ->
                if (maxVehicleDistanceInMeters != null && vehicleLocation.position.distanceToInMeters(poimLatLng) <= maxVehicleDistanceInMeters) {
                    visibleMarkersLocations.add(vehicleLocation.position)
                }
            }
        return visibleMarkersLocations
    }

const val NEXT_STOP_ALPHA = 0.75f
const val PREVIOUS_STOP_ALPHA = 0.50f
const val TOO_MANY_STOPS_ALPHA = 0.25f

fun POIFragment.getMapMarkerAlpha(position: Int): Float? {
    if (!FeatureFlags.F_EXPORT_TRIP_ID) return null
    val poi = this.poim?.poi ?: return null
    viewModel?.poiList?.value
        ?.map { it.poi }
        ?.let { pois ->
            val selectedPoiIndex = pois.indexOfFirst { it.uuid == poi.uuid }.takeIf { it > -1 } ?: return null
            val allRDS = pois.all { it is RouteDirectionStop }
            return if (allRDS) {
                when (position - 1) { // position = index+1
                    selectedPoiIndex -> null
                    in 0..selectedPoiIndex -> PREVIOUS_STOP_ALPHA
                    else -> NEXT_STOP_ALPHA
                }
            } else {
                when (position - 1) { // position = index+1
                    selectedPoiIndex -> null
                    else -> when (pois.size) {
                        in 0..33 -> NEXT_STOP_ALPHA
                        in 33..100 -> PREVIOUS_STOP_ALPHA
                        else -> TOO_MANY_STOPS_ALPHA
                    }
                }
            }
        }
    return null
}

fun POIFragment.getPOI(position: Int): POIManager? {
    if (FeatureFlags.F_EXPORT_TRIP_ID) {
        val poiList = viewModel?.poiList?.value ?: return null
        val distinct = poiList.mapNotNull { (it.poi as? RouteDirectionStop)?.direction?.id }.distinct()
        val count = distinct.count()
        if (count != 1) return null // only for stop on the same route direction
        return poiList.getOrNull(position)
    }
    return this.poim?.takeIf { position == 0 }
}
