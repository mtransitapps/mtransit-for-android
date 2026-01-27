package org.mtransit.android.ui.fragment

import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.latLng
import org.mtransit.android.data.location
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.rds.route.RDSRouteFragment
import org.mtransit.android.ui.type.AgencyTypeFragment
import org.mtransit.android.ui.view.common.navigateF
import org.mtransit.android.ui.view.map.countPOIInside
import org.mtransit.android.ui.view.map.distanceToInMeters
import org.mtransit.android.ui.view.map.position
import org.mtransit.android.ui.view.updateVehicleLocationMarkersCountdown
import org.mtransit.android.util.FragmentUtils
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.FeatureFlags
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

fun POIFragment.startVehicleLocationCountdownRefresh() {
    if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return
    _vehicleLocationCountdownRefreshJob?.cancel()
    _vehicleLocationCountdownRefreshJob = viewModel?.viewModelScope?.launch {
        while (true) {
            delay(1.seconds)
            context?.let { mapViewController.updateVehicleLocationMarkersCountdown(it) }
        }
    }
}

fun POIFragment.stopVehicleLocationCountdownRefresh() {
    if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return
    _vehicleLocationCountdownRefreshJob?.cancel()
    _vehicleLocationCountdownRefreshJob = null
}

const val MAX_DISTANCE_TIMES = 3

val POIFragment.visibleMarkersLocationList: Collection<LatLng>
    get() {
        val poim = this.poim ?: return emptySet()
        val poimLatLng = poim.latLng ?: return emptySet()
        val visibleMarkersLocations = mutableSetOf<LatLng>()
        visibleMarkersLocations.add(poimLatLng)
        var nextRelevantPOIM: POIManager? = null
        viewModel?.poiList?.value?.let { poiList ->
            poiList.indexOfFirst { it.poi.uuid == poim.poi.uuid }.takeIf { it > -1 }?.let { poimIndex ->
                val previousPOIM = poiList.getOrNull(poimIndex - 1)
                nextRelevantPOIM = poiList.getOrNull(poimIndex + 1) ?: previousPOIM  // next or previous
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

fun POIFragment.onMapClick(): Boolean {
    if (!FragmentUtils.isFragmentReady(this)) return false
    val poim = getPoimOrNull() ?: return false
    if (FeatureFlags.F_NAVIGATION) {
        val navController = NavHostFragment.findNavController(this)
        var extras: FragmentNavigator.Extras? = null
        if (FeatureFlags.F_TRANSITION) {
            extras = FragmentNavigator.Extras.Builder() // TODO marker? .addSharedElement(view, view.getTransitionName())
                .build()
        }
        when (poim.poi) {
            is RouteDirectionStop -> {
                this.localPreferenceRepository.saveAsync(
                    LocalPreferenceRepository.getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(
                        poim.poi.authority,
                        poim.poi.route.id,
                        poim.poi.direction.id,
                    ),
                    false, // show map
                )
                navController.navigateF(
                    R.id.nav_to_rds_route_screen,
                    RDSRouteFragment.newInstanceArgs(poim.poi, this.mapViewController.cameraPosition),
                    null,
                    extras,
                )
            }

            else -> {
                this.localPreferenceRepository.saveAsync(
                    LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(poim.poi.dataSourceTypeId),
                    poim.poi.authority,
                )
                this.defaultPrefRepository.saveAsync(
                    DefaultPreferenceRepository.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(poim.poi.authority),
                    false, // show map
                )
                navController.navigateF(
                    R.id.nav_to_type_screen,
                    AgencyTypeFragment.newInstanceArgs(poim.poi, this.mapViewController.cameraPosition),
                    null,
                    extras,
                )
            }
        }
    } else {
        val mainActivity = activity as? MainActivity ?: return false
        when (poim.poi) {
            is RouteDirectionStop -> {
                this.localPreferenceRepository.saveAsync(
                    LocalPreferenceRepository.getPREFS_LCL_RDS_DIRECTION_SHOWING_LIST_INSTEAD_OF_MAP_KEY(
                        poim.poi.authority,
                        poim.poi.route.id,
                        poim.poi.direction.id
                    ),
                    false, // show map
                )
                mainActivity.addFragmentToStack(
                    RDSRouteFragment.newInstance(poim.poi, this.mapViewController.cameraPosition),
                    this,
                )
            }

            else -> {
                this.localPreferenceRepository.saveAsync(
                    LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(poim.poi.dataSourceTypeId),
                    poim.poi.authority,
                )
                this.defaultPrefRepository.saveAsync(
                    DefaultPreferenceRepository.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(poim.poi.authority),
                    false, // show map
                )
                mainActivity.addFragmentToStack(
                    AgencyTypeFragment.newInstance(poim.poi, this.mapViewController.cameraPosition),
                    this,
                )
            }
        }
    }
    return true
}
