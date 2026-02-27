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
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
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
import org.mtransit.android.ui.view.map.toLatLng
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

val POIFragment.visibleMarkersLocationList: Collection<LatLng>
    get() = buildSet {
        val poim = poim ?: return@buildSet
        val poimLatLng = poim.latLng ?: return@buildSet
        val poiLocation = poim.poi.location ?: return@buildSet
        val isRds = poim.poi is RouteDirectionStop
        add(poimLatLng)
        val poiDistanceToDeviceLocationInMeters = deviceLocation?.distanceTo(poiLocation) // in meters
        var poiDistanceToNextRelevantPOIM: Float? = null
        if (isRds) {
            viewModel?.poiList?.value?.let { poiList ->
                poiList.indexOfFirst { it.poi.uuid == poim.poi.uuid }.takeIf { it > -1 }?.let { poimIndex ->
                    val previousPOIM = poiList.getOrNull(poimIndex - 1)
                    val nextPOIM = poiList.getOrNull(poimIndex + 1)
                    val nextRelevantPOIM = nextPOIM ?: previousPOIM  // next or previous
                    poiDistanceToNextRelevantPOIM = nextRelevantPOIM?.latLng?.distanceToInMeters(poimLatLng)?.coerceAtMost(
                        previousPOIM?.latLng?.distanceToInMeters(poimLatLng) ?: Float.MAX_VALUE
                    )
                    nextPOIM?.latLng?.let {
                        add(it)
                    }
                    previousPOIM?.latLng?.let {
                        add(it)
                    }
                    for (i in poimIndex + 2 until poiList.size) {
                        val maxDistance = poiDistanceToNextRelevantPOIM?.times(1.5f) ?: break
                        val nextNextPOIM = poiList.getOrNull(i)
                        nextNextPOIM?.latLng?.let {
                            if (it.distanceToInMeters(poimLatLng) <= maxDistance) {
                                add(it)
                            } else {
                                break
                            }
                        }
                    }
                }
            }
        } else {
            val isShortList = viewModel?.nearbyPOIs?.value != null
            val nearbySameTypePOIS = viewModel?.nearbyPOIs?.value?.filter { it.poi.type == poim.poi.type }?.takeIf { it.isNotEmpty() }
                ?: viewModel?.poiList?.value?.filter { it.poi.uuid != poim.poi.uuid }?.takeIf { it.isNotEmpty() }
            nearbySameTypePOIS?.let { nearbyPOIs ->
                val sortedPOIList = nearbyPOIs.sortedWith(LocationUtils.POI_DISTANCE_COMPARATOR)
                val nextPOIM = sortedPOIList.getOrNull(0)
                poiDistanceToNextRelevantPOIM = nextPOIM?.latLng?.distanceToInMeters(poimLatLng)
                for (nextNextPOIM in sortedPOIList) {
                    val maxDistance = poiDistanceToNextRelevantPOIM?.times(2.0f)?.coerceAtLeast(250f) ?: break
                    nextNextPOIM.latLng?.let {
                        if (isShortList || it.distanceToInMeters(poimLatLng) <= maxDistance) {
                            add(it)
                            if (!isShortList && size >= 3) {
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
            }
        }
        val maxVehicleDistanceInMeters = max(
            poiDistanceToNextRelevantPOIM?.times(3f)?.coerceAtMost(poiDistanceToDeviceLocationInMeters?.times(3f) ?: Float.MAX_VALUE) ?: 0f,
            poiDistanceToDeviceLocationInMeters?.coerceAtMost(poiDistanceToNextRelevantPOIM?.times(3f) ?: Float.MAX_VALUE) ?: 0f
        ).takeIf { it > 0f }
        viewModel?.vehicleLocations?.value
            ?.minByOrNull { it.position.distanceToInMeters(poimLatLng) }
            ?.let { vehicleLocation ->
                if (maxVehicleDistanceInMeters != null && vehicleLocation.position.distanceToInMeters(poimLatLng) <= maxVehicleDistanceInMeters) {
                    add(vehicleLocation.position)
                }
            }
        deviceLocation?.let {
            val maxDistanceToDeviceLocation = poiDistanceToNextRelevantPOIM?.times(2f) ?: Float.MIN_VALUE
            if (it.distanceTo(poiLocation) <= maxDistanceToDeviceLocation) {
                add(it.toLatLng())
            }
        }
    }

fun POIFragment.getClosestVehicleLocationUuid(
    vehicleLocations: Collection<VehicleLocation>? = viewModel?.vehicleLocations?.value
): String? {
    val poimLatLng = poim?.latLng ?: return null
    return vehicleLocations?.minByOrNull { it.position.distanceToInMeters(poimLatLng) }?.uuid
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
