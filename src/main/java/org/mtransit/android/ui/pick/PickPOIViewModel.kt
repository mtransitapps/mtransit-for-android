package org.mtransit.android.ui.pick

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.favorites.FavoritesViewModel
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import java.util.ArrayList
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class PickPOIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
) : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG = PickPOIViewModel::class.java.simpleName

        internal const val EXTRA_POI_UUIDS = "extra_poi_uuids"
        internal const val EXTRA_POI_AUTHORITIES = "extra_poi_authorities"

        private val POI_ALPHA_COMPARATOR = FavoritesViewModel.POIAlphaComparator()
    }

    override fun getLogTag(): String = LOG_TAG

    private val _uuids = savedStateHandle.getLiveDataDistinct<ArrayList<String>?>(EXTRA_POI_UUIDS)

    private val _authorities = savedStateHandle.getLiveDataDistinct<ArrayList<String>?>(EXTRA_POI_AUTHORITIES)

    private val _allAgencyAuthorities = dataSourcesRepository.readingAllAgencyAuthorities()

    val dataSourceRemovedEvent: LiveData<Event<Boolean?>> =
        PairMediatorLiveData(_authorities, _allAgencyAuthorities).switchMap { (authorities, allAgencyAuthorities) ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(Event(checkForDataSourceRemoved(authorities, allAgencyAuthorities)))
            }
        }

    private fun checkForDataSourceRemoved(authorities: List<String>?, allAgencyAuthorities: List<String>?): Boolean? {
        if (authorities == null || allAgencyAuthorities == null) {
            return null // SKIP
        }
        authorities.firstOrNull { !allAgencyAuthorities.contains(it) }?.let {
            MTLog.d(this, "Authority $it doesn't exist anymore, dismissing dialog.")
            return true
        }
        return false
    }

    val poiList: LiveData<List<POIManager>?> = PairMediatorLiveData(_uuids, _authorities).switchMap { (uuids, authorities) ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getPOIList(uuids, authorities))
        }
    }

    private fun getPOIList(uuids: List<String>?, authorities: List<String>?): List<POIManager>? {
        if (uuids == null || authorities == null) {
            return null
        }
        val size = min(uuids.size, authorities.size)
        val authorityToUUIDs = mutableMapOf<String, MutableList<String>>()
        for (i in 0 until size) {
            authorityToUUIDs.getOrPut(authorities[i]) { mutableListOf() }.add(uuids[i])
        }
        val poiList = mutableListOf<POIManager>()
        authorityToUUIDs.forEach { (authority, uuids) ->
            dataSourceRequestManager.findPOIMs(authority, POIProviderContract.Filter.getNewUUIDsFilter(uuids))?.let {
                poiList.addAll(it)
            }
        }
        poiList.sortWith(POI_ALPHA_COMPARATOR)
        return poiList
    }
}