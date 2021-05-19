package org.mtransit.android.ui.type.poi

import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import javax.inject.Inject

@HiltViewModel
class AgencyPOIsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val lclPrefRepository: LocalPreferenceRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = AgencyPOIsViewModel::class.java.simpleName

        internal const val EXTRA_AGENCY_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_COLOR_INT = "extra_color_int"
    }

    override fun getLogTag(): String = agency.value?.shortName?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    private val _authority = savedStateHandle.getLiveData<String?>(EXTRA_AGENCY_AUTHORITY, null).distinctUntilChanged()

    val colorInt = savedStateHandle.getLiveData<Int?>(EXTRA_COLOR_INT, null).distinctUntilChanged()

    val agency: LiveData<AgencyProperties?> = this._authority.switchMap { authority ->
        authority?.let {
            this.dataSourcesRepository.readingAgency(authority)
        } ?: MutableLiveData(null)
    }

    val poiList: LiveData<List<POIManager>?> = _authority.switchMap { authority ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getPOIList(authority))
        }
    }

    private fun getPOIList(authority: String?): List<POIManager>? {
        if (authority.isNullOrEmpty()) {
            return null
        }
        return dataSourceRequestManager.findPOIs(authority, POIProviderContract.Filter.getNewEmptyFilter())
    }

    val showingListInsteadOfMap: LiveData<Boolean?> = _authority.switchMap { authority ->
        authority?.let {
            lclPrefRepository.pref.liveData(
                PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(it),
                lclPrefRepository.getValue(
                    PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET,
                    PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT
                )
            )
        } ?: MutableLiveData(null)
    }.distinctUntilChanged()

    fun saveShowingListInsteadOfMap(showingListInsteadOfMap: Boolean) {
        lclPrefRepository.pref.edit {
            putBoolean(PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET, showingListInsteadOfMap)
            _authority.value?.let { authority ->
                putBoolean(PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(authority), showingListInsteadOfMap)
            }
        }
    }
}