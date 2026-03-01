package org.mtransit.android.ui.pick

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.poi.POIProviderContract
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIAlphaComparator
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.ui.view.common.getLiveDataDistinct
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

        private val POI_ALPHA_COMPARATOR = POIAlphaComparator()
    }

    override fun getLogTag() = LOG_TAG

    private val _poiUuids = savedStateHandle.getLiveDataDistinct<ArrayList<String>?>(EXTRA_POI_UUIDS)

    private val _poiAuthorities = savedStateHandle.getLiveDataDistinct<ArrayList<String>?>(EXTRA_POI_AUTHORITIES)

    private val _allAgencies = dataSourcesRepository.readingAllAgenciesBase()

    val dataSourceRemovedEvent: LiveData<Event<Boolean?>> =
        MediatorLiveData2(_poiAuthorities, _allAgencies).switchMap { (authorities, allAgencies) ->
            liveData {
                emit(Event(checkForDataSourceRemoved(authorities, allAgencies)))
            }
        }

    private fun checkForDataSourceRemoved(authorities: List<String>?, allAgencies: List<IAgencyProperties>?): Boolean? {
        if (authorities == null || allAgencies == null) {
            return null // SKIP
        }
        authorities.firstOrNull { authority -> allAgencies.none { it.authority == authority } }?.let {
            MTLog.d(this, "Authority $it doesn't exist anymore, dismissing dialog.")
            return true
        }
        return false
    }

    val poiList: LiveData<List<POIManager>?> =
        MediatorLiveData3(_poiUuids, _poiAuthorities, _allAgencies).switchMap { (poiUuids, poiAuthorities, allAgencies) ->
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(getPOIList(poiUuids, poiAuthorities, allAgencies))
            }
        }

    private suspend fun getPOIList(
        poiUuids: List<String>?,
        poiAuthorities: List<String>?,
        allAgencies: List<IAgencyProperties>?
    ): List<POIManager>? {
        if (poiUuids == null || poiAuthorities == null || allAgencies == null) {
            return null
        }
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
            poiRepository.findPOIMs(agency, POIProviderContract.Filter.getNewUUIDsFilter(uuids)).let {
                poiList.addAll(it)
            }
        }
        poiList.sortWith(POI_ALPHA_COMPARATOR)
        return poiList
    }
}