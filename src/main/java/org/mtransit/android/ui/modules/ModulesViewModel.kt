package org.mtransit.android.ui.modules

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.di.Injection

class ModulesViewModel : ViewModel() {

    private val dataSourcesRepository: DataSourcesRepository by lazy { Injection.providesDataSourcesRepository() }

    private val filteredAgencies = dataSourcesRepository.readingAllAgenciesDistinct().map { agencies ->
        agencies.filter { agency ->
            agency.type != DataSourceType.TYPE_PLACE
                    && agency.type != DataSourceType.TYPE_MODULE
        }
    }

    val agencies = MediatorLiveData<List<AgencyProperties>>()

    private val sortByPkgOrMaxValid = MutableLiveData(true)

    init {
        this.agencies.addSource(this.filteredAgencies) { newAllAgencies ->
            makeAgencies(this.sortByPkgOrMaxValid.value, newAllAgencies)?.let {
                this.agencies.postValue(it)
            }
        }
        this.agencies.addSource(this.sortByPkgOrMaxValid) { newSortByPkgOrMaxValid ->
            makeAgencies(newSortByPkgOrMaxValid, this.filteredAgencies.value)?.let {
                this.agencies.postValue(it)
            }
        }
    }

    private fun makeAgencies(
        newSortByPkgOrMaxValid: Boolean?,
        newAllAgencies: List<AgencyProperties>?
    ) = if (newSortByPkgOrMaxValid != false) {
        newAllAgencies?.sortedBy { it.pkg }
    } else {
        newAllAgencies?.sortedBy { it.maxValidSecSorted }
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
            dataSourcesRepository.refreshAvailableVersions(forceRefresh = true) // time check skipped
        }
    }
}