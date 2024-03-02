package org.mtransit.android.ui.home

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.removeTooFar
import org.mtransit.android.commons.removeTooMuchWhenNotInCoverage
import org.mtransit.android.commons.updateDistanceM
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyNearbyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.provider.location.MTLocationProvider
import org.mtransit.android.provider.location.network.NetworkLocationRepository
import org.mtransit.android.provider.permission.LocationPermissionProvider
import org.mtransit.android.ui.MTActivityWithLocation
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.favorites.FavoritesViewModel
import org.mtransit.android.ui.inappnotification.locationpermission.LocationPermissionAwareViewModel
import org.mtransit.android.ui.inappnotification.locationsettings.LocationSettingsAwareViewModel
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareViewModel
import org.mtransit.android.ui.inappnotification.newlocation.NewLocationAwareViewModel
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.addAllN
import org.mtransit.commons.removeAllAnd
import java.util.SortedMap
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val locationPermissionProvider: LocationPermissionProvider,
    private val locationProvider: MTLocationProvider,
    private val networkLocationRepository: NetworkLocationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val adManager: IAdManager,
    private val demoModeManager: DemoModeManager,
    private val pm: PackageManager,
) : MTViewModelWithLocation(),
    NewLocationAwareViewModel,
    LocationSettingsAwareViewModel,
    LocationPermissionAwareViewModel,
    ModuleDisabledAwareViewModel {

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

    fun checkIfNetworkLocationRefreshNecessary() {
        viewModelScope.launch {
            networkLocationRepository.fetchIPLocationIfNecessary(deviceLocation.value)
        }
    }

    private val _ipLocation = networkLocationRepository.ipLocation

    private val _nearbyLocationForceReset = MutableLiveData(Event(false))

    private val _nearbyLocation: LiveData<Location?> =
        TripleMediatorLiveData(deviceLocation, _nearbyLocationForceReset, _ipLocation).switchMap { (lastDeviceLocation, forceResetEvent, ipLocation) ->
            liveData {
                val forceReset: Boolean = forceResetEvent?.getContentIfNotHandled() ?: false
                if (forceReset) {
                    emit(null) // force reset
                }
                emit(getNearbyLocation(lastDeviceLocation ?: ipLocation, forceReset))
            }
        }.distinctUntilChanged()

    private fun getNearbyLocation(lastDeviceLocation: Location?, forceReset: Boolean): Location? {
        if (!forceReset) {
            _nearbyLocation.value?.let {
                MTLog.d(this, "getNearbyLocation() > keep same ($it)")
                return it
            }
        }
        MTLog.d(this, "getNearbyLocation() > use last device location ($lastDeviceLocation)")
        return lastDeviceLocation
    }

    override val hasAgenciesAdded: LiveData<Boolean> = this.dataSourcesRepository.readingHasAgenciesAdded()

    private var _locationPermissionNeeded = MutableLiveData(!locationPermissionProvider.allRequiredPermissionsGranted(appContext))

    override val locationPermissionNeeded: LiveData<Boolean> = _locationPermissionNeeded
    // .distinctUntilChanged() < DO NOT USE DISTINCT BECAUSE TOAST MIGHT NOT BE SHOWN THE 1ST TIME

    override fun refreshLocationPermissionNeeded() {
        _locationPermissionNeeded.value = !locationPermissionProvider.allRequiredPermissionsGranted(appContext)
    }

    override fun enableLocationPermission(activity: MTActivityWithLocation) {
        this.locationProvider.doSetup(activity)
    }

    override val locationSettingsNeededResolution: LiveData<PendingIntent?> =
        PairMediatorLiveData(_nearbyLocation, locationSettingsResolution).map { (nearbyLocation, resolution) ->
            if (nearbyLocation != null) null else resolution
        } // .distinctUntilChanged() < DO NOT USE DISTINCT BECAUSE TOAST MIGHT NOT BE SHOWN THE 1ST TIME

    override val locationSettingsNeeded: LiveData<Boolean> = locationSettingsNeededResolution.map {
        it != null
    } // .distinctUntilChanged() < DO NOT USE DISTINCT BECAUSE TOAST MIGHT NOT BE SHOWN THE 1ST TIME

    val nearbyLocationAddress: LiveData<String?> = _nearbyLocation.switchMap { nearbyLocation ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getNearbyLocationAddress(nearbyLocation))
        }
    }

    @WorkerThread
    private fun getNearbyLocationAddress(location: Location?) = location?.let { locationProvider.getLocationAddressString(it) }

    override val newLocationAvailable: LiveData<Boolean?> =
        PairMediatorLiveData(_nearbyLocation, deviceLocation).map { (nearbyLocation, newDeviceLocation) ->
            if (nearbyLocation == null) {
                null // not new if current unknown
            } else {
                newDeviceLocation != null
                        && !LocationUtils.areAlmostTheSame(nearbyLocation, newDeviceLocation, LocationUtils.LOCATION_CHANGED_NOTIFY_USER_IN_METERS)
            }
        }.distinctUntilChanged()

    private val _allAgencies = this.dataSourcesRepository.readingAllAgenciesBase() // #onModulesUpdated

    private val _dstToHomeAgencies: LiveData<SortedMap<DataSourceType, List<AgencyBaseProperties>>?> = _allAgencies.map { allAgencies ->
        if (allAgencies.isEmpty()) {
            null
        } else {
            allAgencies
                .filter { it.getSupportedType().isHomeScreen }
                .groupBy { it.getSupportedType() }
                .toSortedMap(dataSourcesRepository.defaultDataSourceTypeComparator)
        }
    }.distinctUntilChanged()

    private val _loadingPOIs = MutableLiveData(true)
    val loadingPOIs: LiveData<Boolean?> = _loadingPOIs

    private val _nearbyPOIsTrigger = MutableLiveData<Event<Boolean>>()
    val nearbyPOIsTrigger: LiveData<Event<Boolean>> = _nearbyPOIsTrigger

    val nearbyPOIsTriggerListener: LiveData<Void> = PairMediatorLiveData(_dstToHomeAgencies, _nearbyLocation).switchMap { (dstToHomeAgencies, nearbyLocation) ->
        liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
            if (dstToHomeAgencies?.isNotEmpty() == true && nearbyLocation != null) {
                nearbyPOIsLoadJob?.cancel()
                nearbyPOIsLoadJob = viewModelScope.launch {
                    getNearbyPOIs(this, dstToHomeAgencies, nearbyLocation)
                }
            }
        }
    }

    private val _nearbyPOIs = MutableLiveData<List<POIManager>?>(null)
    val nearbyPOIs: LiveData<List<POIManager>?> = _nearbyPOIs

    private var nearbyPOIsLoadJob: Job? = null

    private suspend fun getNearbyPOIs(
        scope: CoroutineScope,
        dstToHomeAgencies: SortedMap<DataSourceType, List<AgencyBaseProperties>>?,
        nearbyLocation: Location?,
    ) {
        if (dstToHomeAgencies.isNullOrEmpty() || nearbyLocation == null) {
            MTLog.d(this@HomeViewModel, "loadNearbyPOIs() > SKIP (no agencies OR no location)")
            _loadingPOIs.postValue(false)
            _nearbyPOIs.postValue(null)
            _nearbyPOIsTrigger.postValue(Event(true))
            return
        }
        _nearbyPOIsTrigger.postValue(Event(true))
        _loadingPOIs.postValue(true)
        if (!_nearbyPOIs.value.isNullOrEmpty()) {
            delay(333L) // debounce / throttle (agencies being updated)
        }
        val favoriteUUIDs = favoriteRepository.findFavoriteUUIDs()
        val nbMaxByType = when (dstToHomeAgencies.keys.size) {
            in 0..1 -> NB_MAX_BY_TYPE_ONE_TYPE
            2 -> NB_MAX_BY_TYPE_TWO_TYPES
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
            typePOIs.sortWith(POI_ALPHA_COMPARATOR)
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
                if (poim.poi is RouteTripStop) { // RTS
                    val rts = poim.poi
                    val routeTripId = "${rts.route.id}-${rts.trip.id}"
                    if (routeTripKept.contains(routeTripId)) {
                        it.remove()
                        continue
                    }
                } else { // bike station, modules... (need to honor MAX BY TYPE)
                    if (nbKept >= nbMaxByType && lastKeptDistance != poim.distance) {
                        it.remove()
                        continue
                    }
                }
            }
            if (nbKept >= nbMaxByType && lastKeptDistance != poim.distance && poim.distance > minDistanceInMeters * 2f) {
                it.remove()
                continue
            }
            if (poim.poi is RouteTripStop) {
                routeTripKept += "${poim.poi.route.id}-${poim.poi.trip.id}"
            }
            lastKeptDistance = poim.distance
            nbKept++
        }
    }

    private suspend fun getTypeNearbyPOIs(
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
            if (this.demoModeManager.isFullDemo()) { // filter now to get min number of POI
                typePOIs = typePOIs.distinctBy { poim ->
                    if (poim.poi is RouteTripStop) {
                        "${poim.poi.route.id}-${poim.poi.trip.id}"
                    } else {
                        poim.poi.uuid // keep all
                    }
                }.toMutableList()
            }
            if (!shouldContinueSearching(typeLat, typeLng, typeAd, typePOIs, typeMinCoverageInMeters, nbMaxByType)) {
                break
            }
            lastTypeAroundDiff = if (typePOIs.isEmpty()) typeAd.aroundDiff else null
            LocationUtils.incAroundDiff(typeAd)
        }
        return typePOIs
    }

    private fun shouldContinueSearching(
        typeLat: Double,
        typeLng: Double,
        typeAd: LocationUtils.AroundDiff,
        typePOIs: List<POIManager>,
        typeMinCoverageInMeters: Float,
        nbMaxByType: Int,
    ) = when {
        LocationUtils.searchComplete(typeLat, typeLng, typeAd.aroundDiff) -> false // world exploration completed
        this.demoModeManager.isFullDemo() && typePOIs.size < DemoModeManager.MIN_POI_HOME_SCREEN -> true // continue
        typePOIs.size > nbMaxByType
                && LocationUtils.getAroundCoveredDistanceInMeters(typeLat, typeLng, typeAd.aroundDiff) >= typeMinCoverageInMeters -> {
            false  // enough POIs / type & enough distance covered
        }

        else -> true  // continue
    }

    private suspend fun getAreaTypeNearbyPOIs(
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
        val area = Area.getArea(lat, lng, ad.aroundDiff)
        // TODO latter optimize val optLastArea = if (optLastAroundDiff == null) null else LocationUtils.getArea(lat, lng, optLastAroundDiff)
        val aroundDiff = ad.aroundDiff
        val maxDistance = LocationUtils.getAroundCoveredDistanceInMeters(lat, lng, aroundDiff)
        val hideBookingRequired = lclPrefRepository.getValue(
            LocalPreferenceRepository.PREF_LCL_HIDE_BOOKING_REQUIRED, LocalPreferenceRepository.PREF_LCL_HIDE_BOOKING_REQUIRED_DEFAULT
        )
        val poiFilter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, aroundDiff).apply {
            addExtra(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, true)
            addExtra(GTFSProviderContract.POI_FILTER_EXTRA_NO_PICKUP, true)
        }
        typeAgencies
            .filter { Area.areOverlapping(it.area, area) } // TODO latter optimize && !agency.isEntirelyInside(optLastArea)
            .forEach { agency ->
                scope.ensureActive()
                typePOIs.addAllN(
                    poiRepository.findPOIMs(agency, poiFilter)
                        ?.removeAllAnd {
                            if (FeatureFlags.F_USE_ROUTE_TYPE_FILTER) {
                                hideBookingRequired && (it.poi as? RouteTripStop)?.route?.type in GTFSCommons.ROUTE_TYPES_REQUIRES_BOOKING
                            } else false
                        }
                        ?.updateDistanceM(lat, lng)
                        ?.removeTooFar(maxDistance)
                        ?.removeTooMuchWhenNotInCoverage(
                            typeMinCoverageInMeters,
                            if (this.demoModeManager.isFullDemo()) Int.MAX_VALUE else maxSize // keep all
                        )
                )
            }
        typePOIs.removeTooMuchWhenNotInCoverage(
            typeMinCoverageInMeters,
            if (this.demoModeManager.isFullDemo()) Int.MAX_VALUE else maxSize // keep all
        )
        return typePOIs
    }

    override fun initiateRefresh(): Boolean {
        val newDeviceLocation = this.deviceLocation.value ?: return false
        val currentNearbyLocation = this._nearbyLocation.value
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

    override fun getAdBannerHeightInPx(activity: IActivity?) = this.adManager.getBannerHeightInPx(activity)

    override val moduleDisabled = _allAgencies.map {
        it.filter { agency -> !agency.isEnabled }
    }.distinctUntilChanged()

    override val hasDisabledModule = moduleDisabled.map {
        it.any { agency -> !pm.isAppEnabled(agency.pkg) }
    }
}