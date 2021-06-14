package org.mtransit.android.ui.type

import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.MTViewModelWithLocation
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import javax.inject.Inject

@HiltViewModel
class AgencyTypeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val lclPrefRepository: LocalPreferenceRepository,
    private val statusLoader: StatusLoader,
    private val serviceUpdateLoader: ServiceUpdateLoader,
) : MTViewModelWithLocation() {

    companion object {
        private val LOG_TAG = AgencyTypeViewModel::class.java.simpleName

        internal const val EXTRA_TYPE_ID = "extra_type_id"
    }

    override fun getLogTag(): String = LOG_TAG

    private val _typeId = savedStateHandle.getLiveData<Int?>(EXTRA_TYPE_ID, null).distinctUntilChanged()

    val type: LiveData<DataSourceType?> = _typeId.map { typeId ->
        DataSourceType.parseId(typeId)
    }

    private val allAvailableAgencies = this.dataSourcesRepository.readingAllAgenciesDistinct() // #onModulesUpdated

    val typeAgencies: LiveData<List<IAgencyUIProperties>?> = PairMediatorLiveData(type, allAvailableAgencies).map { (dst, allAgencies) ->
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

    val selectedTypeAgencyPosition: LiveData<Int?> = PairMediatorLiveData(selectedTypeAgencyAuthority, typeAgencies).map { (agencyAuthority, agencies) ->
        if (agencyAuthority == null || agencies == null) {
            null
        } else {
            agencies.indexOfFirst { it.authority == agencyAuthority }.coerceAtLeast(0)
        }
    }

    fun onPageSelected(position: Int) {
        this.statusLoader.clearAllTasks()
        this.serviceUpdateLoader.clearAllTasks()
        saveSelectedTypeAgency(position)
    }

    private fun saveSelectedTypeAgency(position: Int) {
        saveSelectedTypeAgency(
            typeAgencies.value?.getOrNull(position) ?: return
        )
    }

    private fun saveSelectedTypeAgency(agency: IAgencyProperties) {
        val typeId: Int = _typeId.value ?: return
        lclPrefRepository.pref.edit {
            putString(PreferenceUtils.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(typeId), agency.authority)
        }
    }
}