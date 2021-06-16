package org.mtransit.android.ui.modules

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import javax.inject.Inject

@HiltViewModel
class ModulesViewModel @Inject constructor(
    private val dataSourcesRepository: DataSourcesRepository,
) : ViewModel() {

    private val filteredAgencies = dataSourcesRepository.readingAllAgencies().map { agencies ->
        agencies.filter { agency ->
            agency.type != DataSourceType.TYPE_PLACE
                    && agency.type != DataSourceType.TYPE_MODULE
        }
    }

    private val sortByPkgOrMaxValid = MutableLiveData(true)

    val agencies: LiveData<List<AgencyProperties>?> =
        PairMediatorLiveData(filteredAgencies, sortByPkgOrMaxValid).map { (newFilteredAgencies, newSortByPkgOrMaxValid) ->
            if (newSortByPkgOrMaxValid != false) {
                newFilteredAgencies?.sortedBy { it.pkg }
            } else {
                newFilteredAgencies?.sortedBy { it.maxValidSecSorted }
            }
        }

    fun flipSort() {
        this.sortByPkgOrMaxValid.postValue(this.sortByPkgOrMaxValid.value == false)
    }

    fun refreshAvailableVersions() {
        viewModelScope.launch {
            dataSourcesRepository.refreshAvailableVersions() // time check skipped
        }
    }

    fun forceRefreshAvailableVersions() {
        viewModelScope.launch {
            dataSourcesRepository.refreshAvailableVersions(forceAppUpdateRefresh = true) // time check skipped
        }
    }
}