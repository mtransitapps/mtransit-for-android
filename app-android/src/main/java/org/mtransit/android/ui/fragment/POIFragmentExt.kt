package org.mtransit.android.ui.fragment

import com.google.android.gms.maps.model.LatLng
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.latLng
import org.mtransit.android.ui.view.map.countPOIInside

val POIFragment.visibleMarkersLocationList: Collection<LatLng>
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

private val MAP_MARKER_ALPHA_FOCUS_1: Float? = null // 1.00f // DEFAULT
private const val MAP_MARKER_ALPHA_FOCUS_2 = 0.75f
private const val MAP_MARKER_ALPHA_FOCUS_3 = 0.50f
private const val MAP_MARKER_ALPHA_FOCUS_4 = 0.25f

fun POIFragment.getMapMarkerAlpha(position: Int, visibleArea: Area): Float? {
    val poi = this.poim?.poi ?: return null
    viewModel?.poiList?.value
        ?.map { it.poi }
        ?.let { pois ->
            val selectedPoiIndex = pois.indexOfFirst { it.uuid == poi.uuid }.takeIf { it > -1 } ?: return null
            val allRDS = pois.all { it is RouteDirectionStop }
            return if (allRDS) {
                when (position - 1) { // position = index+1
                    selectedPoiIndex -> MAP_MARKER_ALPHA_FOCUS_1
                    in 0..selectedPoiIndex -> MAP_MARKER_ALPHA_FOCUS_2.div(2.0f)
                    else -> MAP_MARKER_ALPHA_FOCUS_2
                }
            } else {
                when (position - 1) { // position = index+1
                    selectedPoiIndex -> MAP_MARKER_ALPHA_FOCUS_1
                    else -> {
                        val visiblePOIInsideArea = visibleArea.countPOIInside(pois).takeIf { it > 0 }
                        when (visiblePOIInsideArea ?: pois.size) {
                            in 0..33 -> MAP_MARKER_ALPHA_FOCUS_2
                            in 33..100 -> MAP_MARKER_ALPHA_FOCUS_3
                            else -> MAP_MARKER_ALPHA_FOCUS_4
                        }
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
