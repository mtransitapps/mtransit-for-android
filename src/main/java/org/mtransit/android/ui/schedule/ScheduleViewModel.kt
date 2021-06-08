package org.mtransit.android.ui.schedule

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
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val dataSourcesRepository: DataSourcesRepository,
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

    val authority = savedStateHandle.getLiveData<String?>(EXTRA_AUTHORITY, null).distinctUntilChanged()

    val uuid = savedStateHandle.getLiveData<String?>(EXTRA_POI_UUID, null).distinctUntilChanged()

    val colorInt = savedStateHandle.getLiveData<Int?>(EXTRA_COLOR_INT, null).distinctUntilChanged()

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    val agency: LiveData<AgencyProperties?> = this.authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgency(authority)
    }

    val rts: LiveData<RouteTripStop?> = PairMediatorLiveData(authority, uuid).switchMap { (authority, uuid) ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getRouteTripStop(authority, uuid))
        }
    }

    private fun getRouteTripStop(authority: String?, uuid: String?): RouteTripStop? {
        if (authority.isNullOrEmpty() || uuid.isNullOrEmpty()) {
            MTLog.d(this, "getRouteTripStop() > SKIP (no uuid OR no authority)")
            return null
        }
        return this.dataSourceRequestManager.findPOIM(authority, POIProviderContract.Filter.getNewUUIDFilter(uuid))?.let { poim ->
            if (poim.poi is RouteTripStop) {
                poim.poi
            } else {
                MTLog.d(this, "getRouteTripStop() > SKIP (POI is not RTS!)")
                null
            }
        } ?: run {
            MTLog.d(this, "getRouteTripStop() > SKIP (data source removed!)")
            dataSourceRemovedEvent.postValue(Event(true))
            null
        }
    }

    fun onPagetSelected(@Suppress("UNUSED_PARAMETER") position: Int) {
        this.statusLoader.clearAllTasks()
        this.serviceUpdateLoader.clearAllTasks()
    }
}