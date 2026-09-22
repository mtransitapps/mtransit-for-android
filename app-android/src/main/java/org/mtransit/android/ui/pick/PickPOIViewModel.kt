package org.mtransit.android.ui.pick

import androidx.collection.SimpleArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.removeTooMuchWhenNotInCoverage
import org.mtransit.android.commons.updateDistanceM
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIAlphaComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.shortUUID
import org.mtransit.android.data.simpleDistanceString
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.commons.addAllN
import org.mtransit.commons.removeAllAnd
import org.mtransit.commons.sortWithAnd
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class PickPOIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
) : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG: String = PickPOIViewModel::class.java.simpleName

        internal const val EXTRA_POI_UUIDS = "extra_poi_uuids"
        internal const val EXTRA_POI_AUTHORITIES = "extra_poi_authorities"

        internal const val EXTRA_FIXED_ON_LAT = "extra_fixed_on_lat"
        internal const val EXTRA_FIXED_ON_LNG = "extra_fixed_on_lng"

        private val POI_ALPHA_COMPARATOR = POIAlphaComparator()
    }

    override fun getLogTag() = LOG_TAG

    private val _poiUuids = savedStateHandle.getLiveDataDistinct<ArrayList<String>?>(EXTRA_POI_UUIDS)
    private val _poiAuthorities = savedStateHandle.getLiveDataDistinct<ArrayList<String>?>(EXTRA_POI_AUTHORITIES)

    private val _fixedOnLat = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_FIXED_ON_LAT)
    private val _fixedOnLng = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_FIXED_ON_LNG)

    private val _allAgencies = dataSourcesRepository.readingAllAgenciesBase()

    val dataSourceRemovedEvent: LiveData<Event<Boolean>> = MediatorLiveData2(_poiAuthorities, _allAgencies)
        .switchMap { (authorities, allAgencies) ->
            liveData {
                authorities ?: return@liveData
                allAgencies ?: return@liveData
                emit(Event(checkForDataSourceRemoved(authorities, allAgencies)))
            }
        }

    private fun checkForDataSourceRemoved(authorities: List<String>, allAgencies: List<IAgencyProperties>): Boolean {
        authorities.firstOrNull { authority -> allAgencies.none { it.authority == authority } }?.let {
            MTLog.d(this, "Authority $it doesn't exist anymore, dismissing dialog.")
            return true
        }
        return false
    }

    private val poiList: LiveData<List<POIManager>?> = MediatorLiveData3(_poiUuids, _poiAuthorities, _allAgencies)
        .switchMap { (poiUuids, poiAuthorities, allAgencies) ->
            poiUuids ?: return@switchMap null
            poiAuthorities ?: return@switchMap null
            allAgencies ?: return@switchMap null
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(getPOIList(poiUuids, poiAuthorities, allAgencies))
            }
        }

    private suspend fun getPOIList(poiUuids: List<String>, poiAuthorities: List<String>, allAgencies: List<IAgencyProperties>): List<POIManager> {
        val size = min(poiUuids.size, poiAuthorities.size)
        val agencyToUUIDs = mutableMapOf<IAgencyProperties, MutableList<String>>()
        for (i in 0 until size) {
            allAgencies.singleOrNull { it.authority == poiAuthorities[i] }?.let { agency ->
                agencyToUUIDs.getOrPut(agency, defaultValue = { mutableListOf() }).add(poiUuids[i])
            } ?: run {
                MTLog.w(this, "getPOIList() > SKIP (missing agency for ${poiAuthorities[i]}!")
            }
        }
        val poiList = mutableListOf<POIManager>()
        agencyToUUIDs.forEach { (agency, uuids) ->
            val poiFilter = POIProviderContract.Filter.getNewUUIDsFilter(uuids)
            poiRepository.findPOIMs(agency, poiFilter).let {
                poiList.addAll(it)
            }
        }
        poiList.sortWith(POI_ALPHA_COMPARATOR)
        return poiList
    }

    val nearbyList: LiveData<List<POIManager>?> = MediatorLiveData3(_fixedOnLat, _fixedOnLng, _allAgencies)
        .switchMap { (fixedOnLat, fixedOnLng, allAgencies) ->
            fixedOnLat ?: return@switchMap null
            fixedOnLng ?: return@switchMap null
            allAgencies ?: return@switchMap null
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(getNearbyPOIs(fixedOnLat, fixedOnLng, allAgencies))
            }
        }

    private suspend fun getNearbyPOIs(lat: Double, lng: Double, allAgencies: List<AgencyBaseProperties>): List<POIManager> {
        val maxSize = LocationUtils.MAX_NEARBY_LIST
        val minCoverageInMeters = LocationUtils.MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS.toFloat()
        val nearbyPOIs = mutableListOf<POIManager>()
        val ad = LocationUtils.getNewDefaultAroundDiff()
        val nearbyAgencyArea = Area.getArea(lat, lng, 0.01)
        val nearbyAgencies = allAgencies.filter { agency ->
            agency.type.isNearbyScreen
                && agency.type != DataSourceType.TYPE_MODULE
                && agency.isInArea(nearbyAgencyArea)
        }
        while (true) {
            val maxDistanceInMeters = LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, ad.aroundDiff)
            val poiFilter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, ad.aroundDiff).copy(
                extras = SimpleArrayMap<String, Any>().apply {
                    put(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, true)
                },
            )
            nearbyAgencies.forEach { nearbyAgency ->
                nearbyPOIs.addAllN(
                    poiRepository.findPOIMs(nearbyAgency, poiFilter)
                        .updateDistanceM(lat, lng)
                        .removeTooFar(maxDistanceInMeters)
                        .removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
                        .removeAllAnd { nearbyPOIs.contains(it) }
                        .let { nearbyPOISortedByDistance -> // sorted by distance
                            val routeDirectionKept = mutableSetOf<String>()
                            val poiToRemove = mutableListOf<POIManager>()
                            nearbyPOIs.forEach {
                                (it.poi as? RouteDirectionStop)?.let { rds ->
                                    routeDirectionKept.add("${rds.route.id}-${rds.direction.id}")
                                }
                            }
                            nearbyPOISortedByDistance.forEach {
                                (it.poi as? RouteDirectionStop)?.let { rds ->
                                    if (routeDirectionKept.contains("${rds.route.id}-${rds.direction.id}")) {
                                        poiToRemove.add(it)
                                    } else {
                                        routeDirectionKept.add("${rds.route.id}-${rds.direction.id}")
                                    }
                                }
                            }
                            nearbyPOISortedByDistance.removeAll(poiToRemove)
                            return@let nearbyPOISortedByDistance
                        }
                )
            }
            nearbyPOIs.sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR)
            if (nearbyPOIs.isEmpty() && ad.incAroundDiff == LocationUtils.MIN_AROUND_DIFF) {
                LocationUtils.incAroundDiff(ad) // try one more time
                continue
            }
            break
        }
        nearbyPOIs.sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR)
        if (nearbyPOIs.size > maxSize) {
            nearbyPOIs.subList(nearbyPOIs.size - maxSize, nearbyPOIs.size).clear() // remove last(s) in place
        }
        nearbyPOIs.sortWithAnd(POI_ALPHA_COMPARATOR)
        return nearbyPOIs
    }

    val poiNearbyList: LiveData<List<POIManager>?> = MediatorLiveData2(poiList, nearbyList)
        .map { (poiList, nearbyList) ->
            poiList ?: nearbyList
        }
}
