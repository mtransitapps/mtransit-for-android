package org.mtransit.android.ui.type.poi

import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class AgencyPOIsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val demoModeManager: DemoModeManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = AgencyPOIsViewModel::class.java.simpleName

        internal const val EXTRA_AGENCY_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_COLOR_INT = "extra_color_int"
    }

    override fun getLogTag(): String = agency.value?.shortName?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    private val _authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AGENCY_AUTHORITY)

    val colorInt = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_COLOR_INT)

    val agency: LiveData<AgencyBaseProperties?> = this._authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgencyBase(authority) // #onModulesUpdated
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
        return poiRepository.findPOIMs(authority, POIProviderContract.Filter.getNewEmptyFilter())
    }

    val showingListInsteadOfMap: LiveData<Boolean> = _authority.switchMap { authority ->
        liveData {
            authority?.let {
                if (demoModeManager.enabled) {
                    emit(false) // show map (demo mode ON)
                    return@liveData
                }
                emitSource(
                    defaultPrefRepository.pref.liveData(
                        PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(it),
                        defaultPrefRepository.getValue(
                            PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET,
                            PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT
                        )
                    )
                )
            }
        }
    }.distinctUntilChanged()

    fun saveShowingListInsteadOfMap(showingListInsteadOfMap: Boolean) {
        if (demoModeManager.enabled) {
            return // SKIP (demo mode ON)
        }
        defaultPrefRepository.pref.edit {
            putBoolean(PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET, showingListInsteadOfMap)
            _authority.value?.let { authority ->
                putBoolean(PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(authority), showingListInsteadOfMap)
            }
        }
    }
}