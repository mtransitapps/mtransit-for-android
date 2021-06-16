package org.mtransit.android.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val poiRepository: POIRepository,
    private val statusLoader: StatusLoader,
    private val serviceUpdateLoader: ServiceUpdateLoader,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = ScheduleViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_POI_UUID = "extra_poi_uuid"
        internal const val EXTRA_COLOR_INT = "extra_color_int"
    }

    override fun getLogTag(): String = LOG_TAG

    val authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AUTHORITY)

    val uuid = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_POI_UUID)

    val colorInt = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_COLOR_INT)

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    val agency: LiveData<AgencyBaseProperties?> = this.authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgencyBase(authority) // #onModulesUpdated
    }

    private val _agencyAuthority = this.agency.map { it?.authority } // #onModulesUpdated

    val poim: LiveData<POIManager?> = PairMediatorLiveData(_agencyAuthority, uuid).switchMap { (agencyAuthority, uuid) -> // #onModulesUpdated
        getPOIManager(agencyAuthority, uuid)
    }

    private fun getPOIManager(agencyAuthority: String?, uuid: String?) = poiRepository.readingPOIM(agencyAuthority, uuid, poim.value) {
        dataSourceRemovedEvent.postValue(Event(true))
    }

    val rts: LiveData<RouteTripStop?> = this.poim.map { it?.let { if (it.poi is RouteTripStop) it.poi else null } }

    fun onPageSelected(@Suppress("UNUSED_PARAMETER") position: Int) {
        this.statusLoader.clearAllTasks()
        this.serviceUpdateLoader.clearAllTasks()
    }
}