package org.mtransit.android.ui.pick

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIAlphaComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.isNoPickup
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.location.UILocationUtils
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.usecase.GetNearbyPOIListUseCase
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class PickPOIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    getNearbyPOIListUseCase: GetNearbyPOIListUseCase,
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

    private val poiUuids = savedStateHandle.getLiveDataDistinct<ArrayList<String>?>(EXTRA_POI_UUIDS)
    private val poiAuthorities = savedStateHandle.getLiveDataDistinct<ArrayList<String>?>(EXTRA_POI_AUTHORITIES)

    private val fixedOnLat = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_FIXED_ON_LAT)
    private val fixedOnLng = savedStateHandle.getLiveDataDistinct<Double?>(EXTRA_FIXED_ON_LNG)

    private val _allAgencies = dataSourcesRepository.readingAllAgenciesBase()

    val dataSourceRemovedEvent: LiveData<Event<Boolean>> = MediatorLiveData2(poiAuthorities, _allAgencies)
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

    private val poiList: LiveData<List<POIManager>?> = MediatorLiveData3(poiUuids, poiAuthorities, _allAgencies)
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

    val nearbyList: LiveData<List<POIManager>?> = MediatorLiveData3(fixedOnLat, fixedOnLng, _allAgencies)
        .switchMap { (fixedOnLat, fixedOnLng, allAgencies) ->
            fixedOnLat ?: return@switchMap null
            fixedOnLng ?: return@switchMap null
            allAgencies ?: return@switchMap null
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(
                    getNearbyPOIListUseCase(
                        fixedOnLat,
                        fixedOnLng,
                        allAgencies,
                        minSize = null,
                        maxCoverageInMeters = UILocationUtils.MAX_NEARBY_RELEVANT_COVERAGE_IN_METERS,
                        excludeAgency = { agency ->
                            !agency.type.isNearbyScreen
                                || agency.type == DataSourceType.TYPE_MODULE
                        },
                        excludePOI = { it.poi.isNoPickup }
                    )
                )
            }
        }

    val nearbyLatLng: LiveData<Pair<Double, Double>?> = MediatorLiveData2(fixedOnLat, fixedOnLng)
        .map { (fixedOnLat, fixedOnLng) ->
            fixedOnLat ?: return@map null
            fixedOnLng ?: return@map null
            fixedOnLat to fixedOnLng
        }

    val poiNearbyList: LiveData<List<POIManager>?> = MediatorLiveData2(poiList, nearbyList)
        .map { (poiList, nearbyList) ->
            poiList ?: nearbyList
        }
}
