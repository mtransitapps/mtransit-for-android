package org.mtransit.android.ui.home

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.commons.CollectionUtils
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyNearbyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.provider.location.MTLocationProvider
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.favorites.FavoritesViewModel
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.util.toLatLngS
import java.util.SortedMap
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val locationProvider: MTLocationProvider,
    private val favoriteRepository: FavoriteRepository,
    private val adManager: IAdManager,
) : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG = HomeViewModel::class.java.simpleName

        private const val NB_MAX_BY_TYPE = 2
        private const val NB_MAX_BY_TYPE_ONE_TYPE = 6
        private const val NB_MAX_BY_TYPE_TWO_TYPES = 4

        private val POI_ALPHA_COMPARATOR = FavoritesViewModel.POIAlphaComparator()

        private const val IGNORE_SAME_LOCATION_CHECK = false
        // private const val IGNORE_SAME_LOCATION_CHECK = true // DEBUG
    }

    override fun getLogTag(): String = LOG_TAG

    private val _nearbyLocationForceReset = MutableLiveData(Event(false))

    val nearbyLocation: LiveData<Location?> =
        PairMediatorLiveData(deviceLocation, _nearbyLocationForceReset).switchMap { (lastDeviceLocation, forceResetEvent) ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                val forceReset: Boolean = forceResetEvent?.getContentIfNotHandled() ?: false
                if (forceReset) {
                    emit(null) // force reset
                }
                emit(getNearbyLocation(lastDeviceLocation, forceReset))
            }
        }.distinctUntilChanged()

    private fun getNearbyLocation(lastDeviceLocation: Location?, forceReset: Boolean): Location? {
        if (!forceReset) {
            nearbyLocation.value?.let {
                MTLog.d(this, "getNearbyLocation() > keep same ($it)")
                return it
            }
        }
        MTLog.d(this, "getNearbyLocation() > use last device location ($lastDeviceLocation)")
        return lastDeviceLocation
    }

    val nearbyLocationAddress: LiveData<String?> = nearbyLocation.switchMap { nearbyLocation ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getNearbyLocationAddress(nearbyLocation))
        }
    }

    private fun getNearbyLocationAddress(location: Location?): String? {
        return location?.let {
            locationProvider.getLocationAddressString(it)
        }
    }

    val newLocationAvailable: LiveData<Boolean?> =
        PairMediatorLiveData(nearbyLocation, deviceLocation).map { (nearbyLocation, newDeviceLocation) ->
            if (nearbyLocation == null) {
                null // not new if current unknown
            } else {
                newDeviceLocation != null
                        && !LocationUtils.areAlmostTheSame(nearbyLocation, newDeviceLocation, LocationUtils.LOCATION_CHANGED_NOTIFY_USER_IN_METERS)
            }
        }.distinctUntilChanged()

    private val _allAgencies = this.dataSourcesRepository.readingAllAgenciesBaseDistinct() // #onModulesUpdated

    private val _dstToHomeAgencies: LiveData<SortedMap<DataSourceType, List<AgencyBaseProperties>>?> = _allAgencies.map { allAgencies ->
        if (allAgencies.isNullOrEmpty()) {
            null
        } else {
            allAgencies
                .filter { it.type.isHomeScreen }
                .groupBy { it.type }
                .toSortedMap(dataSourcesRepository.defaultDataSourceTypeComparator)
        }
    }.distinctUntilChanged()

    private val _loadingPOIs = MutableLiveData(false)
    val loadingPOIs: LiveData<Boolean?> = _loadingPOIs

    val nearbyPOIsTrigger: LiveData<Any?> = PairMediatorLiveData(_dstToHomeAgencies, nearbyLocation).map {
        loadNearbyPOIs()
        null
    }

    private val _nearbyPOIs = MutableLiveData<List<POIManager>?>(null)
    val nearbyPOIs: LiveData<List<POIManager>?> = _nearbyPOIs

    private var nearbyPOIsLoadJob: Job? = null

    fun loadNearbyPOIs() {
        nearbyPOIsLoadJob?.cancel()
        nearbyPOIsLoadJob = viewModelScope.launch(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            getNearbyPOIs(this, _dstToHomeAgencies.value, nearbyLocation.value)
        }
    }

    private suspend fun getNearbyPOIs(
        scope: CoroutineScope,
        dstToHomeAgencies: SortedMap<DataSourceType, List<AgencyBaseProperties>>?,
        nearbyLocation: Location?
    ) {
        if (dstToHomeAgencies.isNullOrEmpty() || nearbyLocation == null) {
            MTLog.d(this@HomeViewModel, "loadNearbyPOIs() > SKIP (no agencies OR no location)")
            _loadingPOIs.postValue(false)
            _nearbyPOIs.postValue(null)
            return
        }
        _loadingPOIs.postValue(true)
        if (!_nearbyPOIs.value.isNullOrEmpty()) {
            delay(333L) // debounce / throttle (agencies being updated)
        }
        val favoriteUUIDs = favoriteRepository.findFavoriteUUIDs()
        val nbMaxByType = when (dstToHomeAgencies.keys.size) {
            in 0..2 -> NB_MAX_BY_TYPE_ONE_TYPE
            3 -> NB_MAX_BY_TYPE_TWO_TYPES
            else -> NB_MAX_BY_TYPE
        }
        val lat = nearbyLocation.latitude
        val lng = nearbyLocation.longitude
        val accuracyInMeters = nearbyLocation.accuracy
        val minDistanceInMeters = maxOf(
            LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, LocationUtils.MIN_AROUND_DIFF),
            LocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS.toFloat(),
            accuracyInMeters
        )
        val nearbyPOIs = mutableListOf<POIManager>()
        _nearbyPOIs.postValue(nearbyPOIs)
        dstToHomeAgencies.forEach { (_, typeAgencies) ->
            scope.ensureActive()
            val typePOIs = getTypeNearbyPOIs(scope, typeAgencies, lat, lng, minDistanceInMeters, nbMaxByType)
            filterTypePOIs(favoriteUUIDs, typePOIs, minDistanceInMeters, nbMaxByType)
            CollectionUtils.sort(typePOIs, POI_ALPHA_COMPARATOR)
            scope.ensureActive()
            _nearbyPOIs.postValue(typePOIs)
            nearbyPOIs.addAll(typePOIs)
        }
        scope.ensureActive()
        _nearbyPOIs.postValue(nearbyPOIs)
        _loadingPOIs.postValue(false)
    }

    private fun filterTypePOIs(favoriteUUIDs: Collection<String>, typePOIs: MutableList<POIManager>, minDistanceInMeters: Float, nbMaxByType: Int) {
        val it = typePOIs.iterator()
        var nbKept = 0
        var lastKeptDistance = -1f
        val routeTripKept = mutableSetOf<String>()
        while (it.hasNext()) {
            val poim = it.next()
            if (!favoriteUUIDs.contains(poim.poi.uuid)) {
                if (poim.poi is RouteTripStop) {
                    val rts = poim.poi
                    val routeTripId = "${rts.route.id}-${rts.trip.id}"
                    if (routeTripKept.contains(routeTripId)) {
                        it.remove()
                        continue
                    }
                } else if (nbKept >= nbMaxByType && lastKeptDistance != poim.distance) {
                    it.remove()
                    continue
                }
            }
            if (nbKept >= nbMaxByType && lastKeptDistance != poim.distance && poim.distance > minDistanceInMeters) {
                it.remove()
                continue
            }
            if (poim.poi is RouteTripStop) {
                routeTripKept.add("${poim.poi.route.id}-${poim.poi.trip.id}")
            }
            lastKeptDistance = poim.distance
            nbKept++
        }
    }

    private fun getTypeNearbyPOIs(
        scope: CoroutineScope,
        typeAgencies: List<IAgencyNearbyProperties>,
        typeLat: Double,
        typeLng: Double,
        typeMinCoverageInMeters: Float,
        nbMaxByType: Int
    ): MutableList<POIManager> {
        var typePOIs: MutableList<POIManager>
        val typeAd = LocationUtils.getNewDefaultAroundDiff()
        var lastTypeAroundDiff: Double? = null
        val typeMaxSize = LocationUtils.MAX_NEARBY_LIST
        while (true) {
            scope.ensureActive()
            typePOIs = getAreaTypeNearbyPOIs(scope, typeLat, typeLng, typeAd, lastTypeAroundDiff, typeMaxSize, typeMinCoverageInMeters, typeAgencies)
            if (LocationUtils.searchComplete(typeLat, typeLng, typeAd.aroundDiff)) {
                break // world exploration completed
            } else if (typePOIs.size > nbMaxByType
                && LocationUtils.getAroundCoveredDistanceInMeters(typeLat, typeLng, typeAd.aroundDiff) >= typeMinCoverageInMeters
            ) {
                break // enough POIs / type & enough distance covered
            } else {
                lastTypeAroundDiff = if (typePOIs.isNullOrEmpty()) typeAd.aroundDiff else null
                LocationUtils.incAroundDiff(typeAd)
            }
        }
        return typePOIs
    }

    private fun getAreaTypeNearbyPOIs(
        scope: CoroutineScope,
        lat: Double,
        lng: Double,
        ad: LocationUtils.AroundDiff, // TODO latter optimize
        @Suppress("UNUSED_PARAMETER") optLastAroundDiff: Double? = null,
        @Suppress("SameParameterValue") maxSize: Int,
        typeMinCoverageInMeters: Float,
        typeAgencies: List<IAgencyNearbyProperties>
    ): MutableList<POIManager> {
        val typePOIs = mutableListOf<POIManager>()
        val area = LocationUtils.getArea(lat, lng, ad.aroundDiff)
        // TODO latter optimize val optLastArea = if (optLastAroundDiff == null) null else LocationUtils.getArea(lat, lng, optLastAroundDiff)
        val aroundDiff = ad.aroundDiff
        val maxDistance = LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, aroundDiff)
        val poiFilter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, aroundDiff).apply {
            addExtra(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, true)
            addExtra(GTFSProviderContract.POI_FILTER_EXTRA_DESCENT_ONLY, true)
        }
        typeAgencies
            .filter { LocationUtils.Area.areOverlapping(it.area, area) } // TODO latter optimize && !agency.isEntirelyInside(optLastArea)
            .forEach { agency ->
                scope.ensureActive()
                dataSourceRequestManager.findPOIMs(agency.authority, poiFilter)?.let { agencyPOIs ->
                    scope.ensureActive()
                    LocationUtils.updateDistance(agencyPOIs, lat, lng)
                    LocationUtils.removeTooFar(agencyPOIs, maxDistance)
                    LocationUtils.removeTooMuchWhenNotInCoverage(agencyPOIs, typeMinCoverageInMeters, maxSize)
                    typePOIs.addAll(agencyPOIs)
                }
            }
        LocationUtils.removeTooMuchWhenNotInCoverage(typePOIs, typeMinCoverageInMeters, maxSize)
        return typePOIs
    }

    fun initiateRefresh(): Boolean {
        val newDeviceLocation = this.deviceLocation.value ?: return false
        val currentNearbyLocation = this.nearbyLocation.value
        if (!IGNORE_SAME_LOCATION_CHECK
            && LocationUtils.areAlmostTheSame(currentNearbyLocation, newDeviceLocation, LocationUtils.LOCATION_CHANGED_ALLOW_REFRESH_IN_METERS)
        ) {
            MTLog.d(this, "initiateRefresh() > SKIP (same location)")
            return false
        }
        this._nearbyLocationForceReset.value = Event(true)
        MTLog.d(this, "initiateRefresh() > use NEW location")
        return true
    }

    fun getAdBannerHeightInPx(activity: IActivity?): Int {
        return this.adManager.getBannerHeightInPx(activity)
    }
}