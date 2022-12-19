package org.mtransit.android.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.QuadrupleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.util.UITimeUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val poiRepository: POIRepository,
    private val statusLoader: StatusLoader,
    private val serviceUpdateLoader: ServiceUpdateLoader,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = ScheduleViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_POI_UUID = "extra_poi_uuid"
        internal const val EXTRA_COLOR = "extra_color"
        internal val EXTRA_COLOR_DEFAULT: String? = null

        /**
         * @see org.mtransit.commons.FeatureFlags#F_SCHEDULE_INFINITE - START
         */
        internal const val EXTRA_SCROLLED_TO_NOW = "extra_scrolled_to_now"
        internal const val EXTRA_START_AT_IN_MS = "extra_start_at_in_ms"
        internal const val EXTRA_END_AT_IN_MS = "extra_end_at_in_ms"
        /**
         * @see org.mtransit.commons.FeatureFlags#F_SCHEDULE_INFINITE - END
         */
    }

    override fun getLogTag(): String = LOG_TAG

    val authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AUTHORITY)

    val uuid = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_POI_UUID)

    val colorInt = savedStateHandle.getLiveDataDistinct(EXTRA_COLOR, EXTRA_COLOR_DEFAULT)
        .map { it?.let { ColorUtils.parseColor(it) } }

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

    /**
     * @see org.mtransit.commons.FeatureFlags#F_SCHEDULE_INFINITE - START
     */

    private val _startsAtInMs = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_START_AT_IN_MS)

    private val _endsAtInMs = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_END_AT_IN_MS)

    val startEndAt = PairMediatorLiveData(_startsAtInMs, _endsAtInMs)

    fun initStartEndTimeIfNotSet() {
        if (_startsAtInMs.value == null) {
            val startDateInMs = UITimeUtils.getBeginningOfTodayInMs() - TimeUnit.DAYS.toMillis(1L)
            val endDateInMs = startDateInMs + TimeUnit.DAYS.toMillis(7L)
            savedStateHandle[EXTRA_START_AT_IN_MS] = startDateInMs
            savedStateHandle[EXTRA_END_AT_IN_MS] = endDateInMs
        }
    }

    val scrolledToNow = savedStateHandle.getLiveDataDistinct(EXTRA_SCROLLED_TO_NOW, false)

    fun setScrolledToNow(scrolledToNow: Boolean) {
        savedStateHandle[EXTRA_SCROLLED_TO_NOW] = scrolledToNow
    }

    private val _scheduleProviders: LiveData<List<ScheduleProviderProperties>> = this.authority.switchMap { authority ->
        this.dataSourcesRepository.readingScheduleProviders(authority)
    }

    val timestamps: LiveData<List<Schedule.Timestamp>?> =
        QuadrupleMediatorLiveData(rts, _startsAtInMs, _endsAtInMs, _scheduleProviders).switchMap { (rts, startsAtInMs, endsAtInMs, scheduleProviders) ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(getTimestamps(rts, startsAtInMs, endsAtInMs, scheduleProviders))
            }
        }

    private suspend fun getTimestamps(
        rts: RouteTripStop?,
        startsAtInMs: Long?,
        endAtInMS: Long?,
        scheduleProviders: List<ScheduleProviderProperties>?
    ): List<Schedule.Timestamp>? {
        if (rts == null || startsAtInMs == null || endAtInMS == null || scheduleProviders == null) {
            MTLog.d(this, "getTimestamps() > SKIP (no RTS OR no start/end OR no schedule providers)")
            return null // not loaded (loading)
        }
        val scheduleFilter = ScheduleTimestampsProviderContract.Filter(
            rts,
            startsAtInMs,
            endAtInMS
        )
        scheduleProviders.forEach { scheduleProvider ->
            this.dataSourceRequestManager.findScheduleTimestamps(scheduleProvider.authority, scheduleFilter)?.let { scheduleTimestamps ->
                if (scheduleTimestamps.timestampsCount > 0) {
                    return scheduleTimestamps.timestamps
                }
            }
        }
        return emptyList() // loaded (not loading) == no service today
    }

    /**
     * @see org.mtransit.commons.FeatureFlags#F_SCHEDULE_INFINITE - END
     */
}