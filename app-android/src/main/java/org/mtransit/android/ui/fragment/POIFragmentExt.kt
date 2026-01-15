package org.mtransit.android.ui.fragment

import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.latLng

val POIFragment.visibleMarkersLocationList: Collection<LatLng>?
    get() {
        val poim = this.poim ?: return emptySet()
        val poimLatLng = poim.latLng ?: return emptySet()
        val visibleMarkersLocations = mutableSetOf<LatLng>()
        visibleMarkersLocations.add(poimLatLng)
        viewModel?.poiList?.value?.let { poiList ->
            poiList.indexOfFirst { it.poi.uuid == poim.poi.uuid }.takeIf { it > -1 }?.let { poimIndex ->
                poiList.getOrNull(poimIndex + 1)?.latLng?.let { visibleMarkersLocations.add(it) }
                poiList.getOrNull(poimIndex - 1)?.latLng?.let { visibleMarkersLocations.add(it) }
            }
        }
        return visibleMarkersLocations
    }

val MAP_MARKER_ALPHA_PRIMARY_FOCUS: Float? = null // 1.00f // DEFAULT
const val MAP_MARKER_ALPHA_SECONDARY_FOCUS = 0.75f
const val MAP_MARKER_ALPHA_TERTIARY_FOCUS = 0.50f
const val MAP_MARKER_ALPHA_QUATERNARY_FOCUS = 0.25f

fun POIFragment.getMapMarkerAlpha(position: Int): Float? {
    val poi = this.poim?.poi ?: return null
    viewModel?.poiList?.value
        ?.map { it.poi }
        ?.let { pois ->
            val selectedPoiIndex = pois.indexOfFirst { it.uuid == poi.uuid }.takeIf { it > -1 } ?: return null
            val allRDS = pois.all { it is RouteDirectionStop }
            return if (allRDS) {
                when (position - 1) { // position = index+1
                    selectedPoiIndex -> MAP_MARKER_ALPHA_PRIMARY_FOCUS
                    in 0..selectedPoiIndex -> MAP_MARKER_ALPHA_QUATERNARY_FOCUS
                    else -> MAP_MARKER_ALPHA_SECONDARY_FOCUS
                }
            } else {
                when (position - 1) { // position = index+1
                    selectedPoiIndex -> MAP_MARKER_ALPHA_PRIMARY_FOCUS
                    else -> when (pois.size) {
                        in 0..33 -> MAP_MARKER_ALPHA_SECONDARY_FOCUS
                        in 33..100 -> MAP_MARKER_ALPHA_TERTIARY_FOCUS
                        else -> MAP_MARKER_ALPHA_QUATERNARY_FOCUS
                    }
                }
            }
        }
    return null
}

fun POIFragment.getPOI(position: Int): POIManager? {
    val poiList = viewModel?.poiList?.value ?: return null
    val distinct = poiList.mapNotNull { (it.poi as? RouteDirectionStop)?.direction?.id }.distinct()
    val count = distinct.count()
    if (count != 1) return null // only for stop on the same route direction
    return poiList.getOrNull(position)
}
