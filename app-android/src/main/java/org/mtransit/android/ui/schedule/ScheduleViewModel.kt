package org.mtransit.android.ui.schedule

import android.annotation.SuppressLint
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.BuildConfig
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.ScheduleTimestamps
import org.mtransit.android.commons.data.hasRealTimeOrCancelled
import org.mtransit.android.commons.data.readFromSource
import org.mtransit.android.commons.provider.scheduletimestamp.ScheduleTimestampsProviderContract
import org.mtransit.android.commons.provider.status.findClosestTripTimestamp
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.data.makeStatusFilter
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.ui.view.common.MediatorLiveData4
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.user.UserPrefManager
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.beginningOfDay
import org.mtransit.commons.toCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Instant

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    userPrefManager: UserPrefManager,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val poiRepository: POIRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = ScheduleViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_POI_UUID = "extra_poi_uuid"
        internal const val EXTRA_COLOR = "extra_color"
        internal val EXTRA_COLOR_DEFAULT: String? = null

        internal const val EXTRA_SCROLLED_TO_NOW = "extra_scrolled_to_now"

        private const val START_AT_DAYS_BEFORE_INIT = 7
        private const val END_AT_DAYS_AFTER_INIT = 14

        private const val END_AT_DAYS_AFTER_INC = 7

        private const val END_AT_DAYS_AFTER_AUTO_INC_MAX = 99

        private const val EXTRA_START_AT_DAYS_BEFORE = "extra_start_at_days_before"
        private const val EXTRA_END_AT_DAYS_AFTER = "extra_end_at_days_after"
        private const val SCHEDULE_LOCAL_TIME_ZONE_ID = "local_time_zone_id"

        private const val HIDE_REAL_TIME = "hide_real_time"
    }

    override fun getLogTag() = LOG_TAG

    val authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AUTHORITY)

    val uuid = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_POI_UUID)

    val colorInt = savedStateHandle.getLiveDataDistinct(EXTRA_COLOR, EXTRA_COLOR_DEFAULT)
        .map { it?.let { ColorUtils.parseColor(it) } }

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    val agency: LiveData<AgencyBaseProperties?> = this.authority.switchMap { authority ->
        this.dataSourcesRepository.readingAgencyBase(authority) // #onModulesUpdated
    }

    val poim: LiveData<POIManager?> = MediatorLiveData2(agency, uuid)
        .switchMap { (agency, uuid) -> // #onModulesUpdated
            poiRepository.readingPOIM(agency, uuid, currentValue = poim.value, onDataSourceRemoved = {
                dataSourceRemovedEvent.postValue(Event(true))
            })
        }

    val rds: LiveData<RouteDirectionStop?> = this.poim.map { it?.poi as? RouteDirectionStop }

    private val _stop = this.rds.map { it?.stop }

    private val _startsAtDaysBefore = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_START_AT_DAYS_BEFORE)
    private val _endsAtDaysAfter = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_END_AT_DAYS_AFTER)
    private val scheduleLocalTimeZoneId = savedStateHandle.getLiveDataDistinct<String?>(SCHEDULE_LOCAL_TIME_ZONE_ID)

    private val localTimeZoneId = MediatorLiveData3(_stop, agency, scheduleLocalTimeZoneId)
        .map { (stop, agency, scheduleLocalTimeZoneId) ->
            stop?.timeZoneIdOrNull ?: agency?.timeZoneId ?: scheduleLocalTimeZoneId
        }

    val localTimeZone: LiveData<TimeZone?> = localTimeZoneId.map { timeZoneId ->
        timeZoneId?.let { TimeZone.getTimeZone(it) }
    }

    private val _startsAtInMs: LiveData<Long?> = MediatorLiveData2(_startsAtDaysBefore, localTimeZone).map { (startsAtDaysBefore, localTimeZone) ->
        startsAtDaysBefore ?: return@map null
        val timeZone = localTimeZone ?: TimeZone.getDefault()
        UITimeUtils.currentTimeMillis().toCalendar(timeZone).beginningOfDay.timeInMillis - TimeUnit.DAYS.toMillis(startsAtDaysBefore.toLong())
    }.distinctUntilChanged()

    private val _endsAtInMs: LiveData<Long?> = MediatorLiveData2(_endsAtDaysAfter, localTimeZone).map { (endsAtDaysBefore, localTimeZone) ->
        endsAtDaysBefore ?: return@map null
        val timeZone = localTimeZone ?: TimeZone.getDefault()
        UITimeUtils.currentTimeMillis().toCalendar(timeZone).beginningOfDay.timeInMillis + TimeUnit.DAYS.toMillis(endsAtDaysBefore.toLong())
    }.distinctUntilChanged()

    val startEndAt = MediatorLiveData2(_startsAtInMs, _endsAtInMs)

    fun initStartEndTimeIfNotSet() {
        if (_startsAtDaysBefore.value == null) {
            viewModelScope.launch(Dispatchers.Main) {
                savedStateHandle[EXTRA_START_AT_DAYS_BEFORE] = START_AT_DAYS_BEFORE_INIT
                savedStateHandle[EXTRA_END_AT_DAYS_AFTER] = END_AT_DAYS_AFTER_INIT
            }
        }
    }

    fun increaseEndTime(maxEnd: Int? = null): Boolean {
        return _endsAtDaysAfter.value
            ?.takeIf { maxEnd == null || it <= maxEnd }
            ?.let { currentEndDateInDays ->
                viewModelScope.launch(Dispatchers.Main) {
                    savedStateHandle[EXTRA_END_AT_DAYS_AFTER] = currentEndDateInDays + END_AT_DAYS_AFTER_INC
                }
                true
            } ?: false
    }

    val scrolledToNow = savedStateHandle.getLiveDataDistinct(EXTRA_SCROLLED_TO_NOW, false)

    fun setScrolledToNow(scrolledToNow: Boolean) {
        savedStateHandle[EXTRA_SCROLLED_TO_NOW] = scrolledToNow
    }

    private val _scheduleProviders: LiveData<List<ScheduleProviderProperties>> = this.authority
        .switchMap { authority ->
            this.dataSourcesRepository.readingScheduleProviders(authority)
        }

    private val _scheduleTimestamps: LiveData<List<Schedule.Timestamp>?> = MediatorLiveData4(rds, _startsAtInMs, _endsAtInMs, _scheduleProviders)
        .switchMap { (rds, startsAtInMs, endsAtInMs, scheduleProviders) ->
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(getTimestamps(rds, startsAtInMs, endsAtInMs, scheduleProviders))
            }
        }

    val hideRealTime = savedStateHandle.getLiveDataDistinct(HIDE_REAL_TIME, false)

    fun setHideRealTime(hideRealTime: Boolean) {
        savedStateHandle[HIDE_REAL_TIME] = hideRealTime
    }

    private val _scheduleSourceLabel = MutableLiveData<String?>(null)
    private val _rtSourceLabel = MutableLiveData<String?>(null)

    private val _rtReadFromSource = MutableLiveData<Instant?>(null)
    val sourceLabelAndReadFromSource: LiveData<Pair<String?, Instant?>> =
        MediatorLiveData4(_scheduleSourceLabel, _rtSourceLabel, _rtReadFromSource, hideRealTime)
            .map { (scheduleSourceLabel, rtSourceLabel, rtReadFromSource, hideRealTime) ->
                if (hideRealTime == true) return@map Pair(scheduleSourceLabel, null)
                Pair(rtSourceLabel ?: scheduleSourceLabel, rtReadFromSource)
            }

    @WorkerThread
    private suspend fun getTimestamps(
        rds: RouteDirectionStop?,
        startsAtInMs: Long?,
        endAtInMS: Long?,
        scheduleProviders: List<ScheduleProviderProperties>?
    ): List<Schedule.Timestamp>? {
        if (rds == null || startsAtInMs == null || endAtInMS == null || scheduleProviders == null) {
            MTLog.d(this, "getTimestamps() > SKIP (no RDS OR no start/end OR no schedule providers)")
            return null // not loaded (loading)
        }
        val scheduleFilter = ScheduleTimestampsProviderContract.Filter(
            rds,
            startsAtInMs,
            endAtInMS
        )
        var hasProviderTimestampsReturned = false
        scheduleProviders.forEach { scheduleProvider ->
            this.dataSourceRequestManager.findScheduleTimestamps(scheduleProvider.authority, scheduleFilter)?.let { scheduleTimestamps ->
                hasProviderTimestampsReturned = true
                if (scheduleTimestamps.timestampsCount > 0) {
                    _scheduleSourceLabel.postValue(scheduleTimestamps.sourceLabel)
                    setScheduleLocalTimeZoneId(scheduleTimestamps)
                    return scheduleTimestamps.timestamps // DONE (loaded)
                }
            }
        }
        if (hasProviderTimestampsReturned) {
            if (increaseEndTime(maxEnd = END_AT_DAYS_AFTER_AUTO_INC_MAX)) {
                return null // not loaded (loading)
            }
        }
        _scheduleSourceLabel.postValue(null)
        setScheduleLocalTimeZoneId() // empty list must set a timezone to display empty calendar
        return emptyList() // loaded (not loading) == no service today
    }

    private suspend fun setScheduleLocalTimeZoneId(scheduleTimestamps: ScheduleTimestamps? = null) = withContext(Dispatchers.Main) {
        savedStateHandle[SCHEDULE_LOCAL_TIME_ZONE_ID] = scheduleTimestamps?.localTimeZoneId
            ?: scheduleTimestamps?.timestamps?.firstNotNullOfOrNull { @SuppressLint("DiscouragedApi") it.localTimeZoneId }
                    ?: run {
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException("No schedule timestamp timezone available!")
                }
                MTLog.w(LOG_TAG, "No schedule timestamp timezone available (using device TZ)!")
                TimeZone.getDefault().id // must set a timezone to display calendar
            }
    }

    val showAccessibility: LiveData<Boolean> = userPrefManager.showAccessibility.distinctUntilChanged()

    private val _statusProviders = this.authority
        .switchMap { authority ->
            this.dataSourcesRepository.readingStatusProviders(authority)
        }

    private val _nonScheduleStatusProviders = MediatorLiveData2(_statusProviders, _scheduleProviders)
        .map { (statusProviders, scheduleProviders) ->
            scheduleProviders ?: return@map null
            statusProviders?.filter { statusProvider ->
                scheduleProviders.none { it.authority == statusProvider.authority }
            }
        }

    private val _refreshRTTimestampsTrigger = MutableLiveData<Int>()

    fun triggerRealTimeTimestampRefresh() {
        MTLog.d(this, "triggerRealTimeTimestampRefresh() > trigger refresh")
        _refreshRTTimestampsTrigger.value = (_refreshRTTimestampsTrigger.value ?: 0) + 1
    }

    private val _rtTimestamps: LiveData<List<Schedule.Timestamp>?> = MediatorLiveData3(poim, _nonScheduleStatusProviders, _refreshRTTimestampsTrigger)
        .switchMap { (poim, rtStatusProviders) ->
            liveData(viewModelScope.coroutineContext) {
                rtStatusProviders ?: return@liveData
                val statusFilter = poim?.makeStatusFilter() ?: return@liveData
                rtStatusProviders.forEach { statusProvider ->
                    val schedule = dataSourceRequestManager.findStatus(statusProvider, statusFilter) as? Schedule
                    _rtReadFromSource.postValue(schedule?.readFromSource?.takeIf { schedule.hasRealTimeOrCancelled })
                    _rtSourceLabel.postValue(schedule?.sourceLabel?.takeIf { schedule.hasRealTimeOrCancelled })
                    emit(schedule?.timestamps) // always emit to erase old real-time value
                }
            }
        }

    val timestamps = MediatorLiveData2(_scheduleTimestamps, _rtTimestamps)
        .map { (scheduleTimestamps, rtTimestamps) ->
            val scheduleTimestamps = scheduleTimestamps?.toMutableList() ?: return@map null
            rtTimestamps
                ?.filter { it.isRealTimeOrCancelled }
                ?.forEach { rtTimestamp ->
                    val tripId = rtTimestamp.tripId ?: return@forEach
                    val stopSequence = rtTimestamp.stopSequenceOrNull ?: return@forEach
                    scheduleTimestamps.findClosestTripTimestamp(tripId, stopSequence)?.let { rdsTripTimestamp ->
                        scheduleTimestamps.remove(rdsTripTimestamp)
                        scheduleTimestamps.add(rtTimestamp)
                    }
                }
            scheduleTimestamps.sortWith(Schedule.TIMESTAMPS_COMPARATOR)
            scheduleTimestamps
        }

    val hasRealTime = _rtTimestamps.map { rtTimestamps ->
        rtTimestamps?.any { it.isRealTimeOrCancelled } == true
    }.distinctUntilChanged()
}
