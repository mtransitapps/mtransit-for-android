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

    companion object {
        private const val FAKE_AGENCIES_COUNT = 0
        // private const val FAKE_AGENCIES_COUNT = 10 // DEBUG

        private const val ADD_FAKE_AGENCIES = FAKE_AGENCIES_COUNT > 0
    }

    private val _filteredAgencies = dataSourcesRepository.readingAllAgencies().map { agencies ->
        agencies.filter { agency ->
            agency.type != DataSourceType.TYPE_PLACE
                    && agency.type != DataSourceType.TYPE_MODULE
        }
    }

    private val sortByPkgOrMaxValid = MutableLiveData(true)

    val agencies: LiveData<List<AgencyProperties>?> =
        PairMediatorLiveData(_filteredAgencies, sortByPkgOrMaxValid).map { (newFilteredAgencies, newSortByPkgOrMaxValid) ->
            if (ADD_FAKE_AGENCIES) { // DEBUG
                return@map newFilteredAgencies
                    ?.toMutableList()
                    ?.apply {
                        (0..FAKE_AGENCIES_COUNT).forEach { idx ->
                            add(
                                AgencyProperties(
                                    "fake$idx",
                                    DataSourceType.TYPE_MODULE,
                                    "$idx",
                                    "$idx name",
                                    "FFFFFF",
                                    org.mtransit.android.commons.LocationUtils.THE_WORLD,
                                    org.mtransit.android.commons.Constants.MAIN_APP_PACKAGE_NAME,
                                    idx.toLong(),
                                    idx,
                                    isInstalled = true,
                                    isEnabled = true,
                                    isRTS = false,
                                    logo = null,
                                    maxValidSec = 0, // unlimited
                                    trigger = 0
                                )
                            )
                        }
                    }?.sortedWith(
                        if (newSortByPkgOrMaxValid != false) {
                            compareBy { it.pkg }
                        } else {
                            compareBy { it.maxValidSecSorted }
                        }
                    )
            } // DEBUG
            newFilteredAgencies?.sortedWith(
                if (newSortByPkgOrMaxValid != false) {
                    compareBy { it.pkg }
                } else {
                    compareBy { it.maxValidSecSorted }
                }
            )
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