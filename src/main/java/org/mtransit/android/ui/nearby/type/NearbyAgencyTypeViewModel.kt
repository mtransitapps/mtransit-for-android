package org.mtransit.android.ui.nearby.type

import android.location.Location
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.IAgencyNearbyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.type.AgencyTypeViewModel
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class NearbyAgencyTypeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = NearbyAgencyTypeViewModel::class.java.simpleName

        internal const val EXTRA_TYPE_ID = "extra_type_id"

        private const val MIN_NEARBY_LIST_COVERAGE_IN_METERS = LocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS.toFloat()
    }

    override fun getLogTag(): String = typeId.value?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    val typeId = savedStateHandle.getLiveDataDistinct<Int?>(AgencyTypeViewModel.EXTRA_TYPE_ID)

    private val _allAgencies = this.dataSourcesRepository.readingAllAgenciesBase() // #onModuleChanged

    val typeAgencies = PairMediatorLiveData(typeId, _allAgencies).map { (typeId, allAgencies) ->
        val currentParams = this.params.value ?: NearbyParams()
        this.params.value = currentParams.copy(
            typeId = typeId,
            allAgencies = allAgencies,
        )
        typeId?.let { dstId -> allAgencies?.filter { agency -> agency.type.id == dstId } }
    }

    fun setNearbyLocation(newNearbyLocation: Location?) {
        val currentLocation = params.value?.nearbyLocation
        if (newNearbyLocation == currentLocation) {
            MTLog.d(this, "setNearbyLocation() > SKIP (same)")
            return
        }
        val currentParams: NearbyParams = params.value ?: NearbyParams()
        val newAD = LocationUtils.getNewDefaultAroundDiff()
        if (newNearbyLocation == null) {
            _sizeCovered.value = 0
            _distanceCoveredInMeters.value = 0f
            params.value = currentParams.copy(
                nearbyLocation = newNearbyLocation,
                ad = newAD,
                minSize = null,
                maxSize = null,
                minCoverageInMeters = null,
                // TODO ? lastEmptyAroundDiff = null,
            )
            return
        }
        val minSize = params.value?.minSize ?: -1
        val maxSize = params.value?.maxSize ?: -1
        val minCoverageInMeters = params.value?.minCoverageInMeters ?: -1f
        if (minSize < 0 || maxSize < 0 || minCoverageInMeters < 0f) {
            _sizeCovered.value = 0
            _distanceCoveredInMeters.value = 0f
            params.value = currentParams.copy(
                nearbyLocation = newNearbyLocation,
                minSize = LocationUtils.MIN_NEARBY_LIST,
                maxSize = LocationUtils.MAX_NEARBY_LIST,
                ad = newAD,
                minCoverageInMeters = LocationUtils.getAroundCoveredDistanceInMeters(
                    newNearbyLocation.latitude,
                    newNearbyLocation.longitude,
                    newAD.aroundDiff
                ).coerceAtLeast(max(MIN_NEARBY_LIST_COVERAGE_IN_METERS, newNearbyLocation.accuracy)),
                // TODO ? lastEmptyAroundDiff = null,
            )
        }
    }

    private val _sizeCovered = MutableLiveData(0)

    private val _distanceCoveredInMeters = MutableLiveData(0f)

    private val params = MutableLiveData(NearbyParams())

    val nearbyPOIs: LiveData<List<POIManager>?> = params.switchMap { params ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getNearbyPOIs(params))
        }
    }

    @WorkerThread
    private fun getNearbyPOIs(
        currentParams: NearbyParams? = null,
    ): List<POIManager>? {
        if (currentParams == null || !currentParams.isReady) {
            MTLog.d(this, "getNearbyPOIs() > SKIP (not ready)")
            return null
        }
        val typeAgencies: List<IAgencyNearbyProperties> = currentParams.typeAgencies ?: return null
        val nearbyLocation: Location = currentParams.nearbyLocation ?: return null
        val ad: LocationUtils.AroundDiff = currentParams.ad ?: return null
        val minCoverageInMeters: Float = currentParams.minCoverageInMeters ?: return null
        val minSize: Int = currentParams.minSize ?: return null
        val maxSize: Int = currentParams.maxSize ?: return null
        val area: LocationUtils.Area = currentParams.area ?: return null
        val maxDistance: Float = currentParams.maxDistance ?: return null
        val lat = nearbyLocation.latitude
        val lng = nearbyLocation.longitude
        val aroundDiff = ad.aroundDiff
        val nearbyPOIs = mutableListOf<POIManager>()
        val poiFilter = POIProviderContract.Filter.getNewAroundFilter(lat, lng, aroundDiff).apply {
            addExtra(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, true)
        }
        typeAgencies
            .filter { it.isInArea(area) } // TODO latter optimize && !agency.isEntirelyInside(optLastArea)
            .forEach { agency ->
                dataSourceRequestManager.findPOIMs(agency.authority, poiFilter)?.let { agencyPOIs ->
                    LocationUtils.updateDistance(agencyPOIs, lat, lng)
                    LocationUtils.removeTooFar(agencyPOIs, maxDistance)
                    LocationUtils.removeTooMuchWhenNotInCoverage(agencyPOIs, minCoverageInMeters, maxSize)
                    nearbyPOIs.addAll(agencyPOIs)
                }
            }
        LocationUtils.removeTooMuchWhenNotInCoverage(nearbyPOIs, minCoverageInMeters, maxSize)
        // TODO ? this.lastEmptyAroundDiff = ad.aroundDiff
        if (nearbyPOIs.size < minSize
            && !LocationUtils.searchComplete(nearbyLocation.latitude, nearbyLocation.longitude, aroundDiff)
        ) {
            this.params.postValue(
                currentParams.copy(
                    ad = LocationUtils.incAroundDiff(ad) // trigger new data load
                )
            )
        } else {
            _distanceCoveredInMeters.postValue(minCoverageInMeters)
            _sizeCovered.postValue(nearbyPOIs.size)
        }
        return nearbyPOIs
    }

    fun isLoadingMore(): Boolean {
        val currentParams = this.params.value ?: NearbyParams()
        val nearbyLocation = currentParams.nearbyLocation ?: return false
        val ad = currentParams.ad ?: return false
        val minSize = currentParams.minSize ?: return false
        val maxSize = currentParams.maxSize ?: return false
        val minCoverageInMeters = currentParams.minCoverageInMeters ?: return false
        if (minSize < 0 || maxSize < 0 || minCoverageInMeters < 0f) {
            return false
        }
        if (LocationUtils.searchComplete(nearbyLocation.latitude, nearbyLocation.longitude, ad.aroundDiff)) {
            return false
        }
        val sizeCovered = this._sizeCovered.value ?: 0
        if (sizeCovered < minSize) {
            return true // already loading
        }
        val distanceCoveredInMeters = this._distanceCoveredInMeters.value ?: 0f
        if (distanceCoveredInMeters < minCoverageInMeters) {
            return true // already loading
        }
        doLoadMore()
        return true // now loading
    }

    private fun doLoadMore() {
        val currentParams = this.params.value ?: NearbyParams()
        this.params.value = currentParams.copy(
            minSize = currentParams.minSize?.let { it * 2 } ?: LocationUtils.MIN_NEARBY_LIST,
            maxSize = currentParams.maxSize?.let { it * 2 } ?: LocationUtils.MAX_NEARBY_LIST,
            minCoverageInMeters = currentParams.minCoverageInMeters?.let { it * 2f } ?: run {
                val nearbyLocation = currentParams.nearbyLocation
                val ad = currentParams.ad
                if (nearbyLocation == null || ad == null) -1f else {
                    LocationUtils.getAroundCoveredDistanceInMeters(nearbyLocation.latitude, nearbyLocation.longitude, ad.aroundDiff)
                }.coerceAtLeast(max(MIN_NEARBY_LIST_COVERAGE_IN_METERS, nearbyLocation?.accuracy ?: 0f))
            },
            ad = LocationUtils.incAroundDiff(currentParams.ad ?: LocationUtils.getNewDefaultAroundDiff())
        )
    }
}