package org.mtransit.android.ui.schedule.day

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.provider.POIProviderContract
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.di.Injection
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import java.util.Locale
import java.util.concurrent.TimeUnit

class ScheduleDayViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = ScheduleDayViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_POI_UUID = "extra_poi_uuid"
        internal const val EXTRA_DAY_START_AT_IN_MS = "extra_day_starts_at_ms"
        internal const val EXTRA_SCROLLED_TO_NOW = "extra_scrolled_to_now"
    }

    private val yearMonthDayFormat by lazy { ThreadSafeDateFormatter("yyyy-MM-dd", Locale.getDefault()) }

    override fun getLogTag(): String = yearMonthDay.value?.let { "${LOG_TAG}-$it" } ?: LOG_TAG

    private val dataSourcesRepository: DataSourcesRepository by lazy { Injection.providesDataSourcesRepository() }

    private val dataSourceRequestManager: DataSourceRequestManager by lazy { Injection.providesDataSourceRequestManager() }

    private val authority = savedStateHandle.getLiveData<String?>(EXTRA_AUTHORITY, null).distinctUntilChanged()

    private val uuid = savedStateHandle.getLiveData<String?>(EXTRA_POI_UUID, null).distinctUntilChanged()

    val dayStartsAtInMs = savedStateHandle.getLiveData<Long?>(EXTRA_DAY_START_AT_IN_MS, null).distinctUntilChanged()

    val yearMonthDay: LiveData<String?> = dayStartsAtInMs.map {
        it?.let { yearMonthDayFormat.formatThreadSafe(it) }
    }

    val scrolledToNow = savedStateHandle.getLiveData(EXTRA_SCROLLED_TO_NOW, false).distinctUntilChanged()

    fun setScrolledToNow(scrolledToNow: Boolean) {
        savedStateHandle[EXTRA_SCROLLED_TO_NOW] = scrolledToNow
    }

    private val _scheduleProviders: LiveData<List<ScheduleProviderProperties>> = this.authority.switchMap { authority ->
        this.dataSourcesRepository.readingScheduleProviders(authority)
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
        return this.dataSourceRequestManager.findPOI(authority, POIProviderContract.Filter.getNewUUIDFilter(uuid))?.let { poim ->
            if (poim.poi is RouteTripStop) {
                poim.poi
            } else {
                MTLog.d(this, "getRouteTripStop() > SKIP (POI is not RTS!)")
                null
            }
        }
    }

    val timestamps: LiveData<List<Schedule.Timestamp>?> =
        TripleMediatorLiveData(rts, dayStartsAtInMs, _scheduleProviders).switchMap { (rts, dayStartsAtInMs, scheduleProviders) ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(getTimestamps(rts, dayStartsAtInMs, scheduleProviders))
            }
        }

    private fun getTimestamps(rts: RouteTripStop?, startsAtInMs: Long?, scheduleProviders: List<ScheduleProviderProperties>?): List<Schedule.Timestamp>? {
        if (rts == null || startsAtInMs == null || scheduleProviders == null) {
            MTLog.d(this, "getTimestamps() > SKIP (no RTS OR no day start OR no schedule providers)")
            return null // not loaded (loading)
        }
        val scheduleFilter = ScheduleTimestampsProviderContract.Filter(
            rts,
            startsAtInMs,
            startsAtInMs + TimeUnit.DAYS.toMillis(1L)
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
}