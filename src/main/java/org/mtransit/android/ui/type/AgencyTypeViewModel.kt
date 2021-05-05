package org.mtransit.android.ui.type

import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.di.Injection
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.PairMediatorLiveData

class AgencyTypeViewModel(savedStateHandle: SavedStateHandle) : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG = AgencyTypeViewModel::class.java.simpleName

        const val EXTRA_TYPE_ID = "extra_type_id"
    }

    override fun getLogTag(): String = LOG_TAG

    private val dataSourcesRepository: DataSourcesRepository by lazy { Injection.providesDataSourcesRepository() }

    private val lclPrefRepository: LocalPreferenceRepository by lazy { Injection.providesLocalPreferenceRepository() }

    private val _typeId = savedStateHandle.getLiveData<Int?>(EXTRA_TYPE_ID, null).distinctUntilChanged()

    val type: LiveData<DataSourceType?> = _typeId.map { typeId ->
        DataSourceType.parseId(typeId)
    }

    private val allAvailableAgencies = this.dataSourcesRepository.readingAllAgenciesDistinct() // #onModulesUpdated

    val typeAgencies: LiveData<List<AgencyProperties>?> = PairMediatorLiveData(type, allAvailableAgencies).map { (dst, allAgencies) ->
        allAgencies?.filter { it.type == dst }
    }

    private val selectedTypeAgencyAuthority: LiveData<String?> = _typeId.switchMap { typeId ->
        typeId?.let {
            lclPrefRepository.pref.liveData(
                PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(it),
                PreferenceUtils.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT
            )
        } ?: MutableLiveData(null)
    }
    val selectedTypeAgencyPosition: LiveData<Int?> = PairMediatorLiveData(selectedTypeAgencyAuthority, typeAgencies).switchMap { (agencyAuthority, agencies) ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            if (agencyAuthority == null || agencies == null) {
                emit(null)
            } else {
                emit(agencies.indexOfFirst { it.authority == agencyAuthority }.coerceAtLeast(0))
            }
        }
    }

    fun saveSelectedTypeAgency(position: Int) {
        saveSelectedTypeAgency(
            typeAgencies.value?.get(position) ?: return
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun saveSelectedTypeAgency(agency: AgencyProperties) {
        val typeId: Int = _typeId.value ?: return
        lclPrefRepository.pref.edit {
            putString(PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(typeId), agency.authority)
        }
    }
}