package org.mtransit.android.ui.schedule

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
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.commons.provider.ScheduleTimestampsProviderContract
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.POIRepository
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.QuadrupleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.beginningOfDay
import org.mtransit.commons.toCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    defaultPrefRepository: DefaultPreferenceRepository,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val poiRepository: POIRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = ScheduleViewModel::class.java.simpleName

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
        private const val LOCAL_TIME_ZONE_ID = "local_time_zone_id"
        private const val EXTRA_SELECTED_DATE_IN_MS = "extra_selected_date_in_ms"
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

    val poim: LiveData<POIManager?> = PairMediatorLiveData(agency, uuid).switchMap { (agency, uuid) -> // #onModulesUpdated
        getPOIManager(agency, uuid)
    }

    private fun getPOIManager(agency: IAgencyProperties?, uuid: String?) =
        poiRepository.readingPOIM(agency, uuid, poim.value, onDataSourceRemoved = {
            dataSourceRemovedEvent.postValue(Event(true))
        })

    val rds: LiveData<RouteDirectionStop?> = this.poim.map { it?.poi as? RouteDirectionStop }

    private val _startsAtDaysBefore = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_START_AT_DAYS_BEFORE)
    private val _endsAtDaysAfter = savedStateHandle.getLiveDataDistinct<Int?>(EXTRA_END_AT_DAYS_AFTER)
    private val _localTimeZoneId = savedStateHandle.getLiveData<String?>(LOCAL_TIME_ZONE_ID)
    private val localTimeZoneId: LiveData<String?> = _localTimeZoneId.distinctUntilChanged()

    val localTimeZone: LiveData<TimeZone?> = localTimeZoneId.map { timeZoneId ->
        timeZoneId?.let { TimeZone.getTimeZone(it) }
    }

    private val _startsAtInMs: LiveData<Long?> = PairMediatorLiveData(_startsAtDaysBefore, localTimeZone).map { (startsAtDaysBefore, localTimeZone) ->
        startsAtDaysBefore ?: return@map null
        val timeZone = localTimeZone ?: TimeZone.getDefault()
        UITimeUtils.currentTimeMillis().toCalendar(timeZone).beginningOfDay.timeInMillis - TimeUnit.DAYS.toMillis(startsAtDaysBefore.toLong())
    }.distinctUntilChanged()

    private val _endsAtInMs: LiveData<Long?> = PairMediatorLiveData(_endsAtDaysAfter, localTimeZone).map { (endsAtDaysBefore, localTimeZone) ->
        endsAtDaysBefore ?: return@map null
        val timeZone = localTimeZone ?: TimeZone.getDefault()
        UITimeUtils.currentTimeMillis().toCalendar(timeZone).beginningOfDay.timeInMillis + TimeUnit.DAYS.toMillis(endsAtDaysBefore.toLong())
    }.distinctUntilChanged()

    val startEndAt = PairMediatorLiveData(_startsAtInMs, _endsAtInMs)

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

    val selectedDateInMs = savedStateHandle.getLiveDataDistinct<Long?>(EXTRA_SELECTED_DATE_IN_MS)

    fun setSelectedDate(dateInMs: Long) {
        savedStateHandle[EXTRA_SELECTED_DATE_IN_MS] = dateInMs
    }

    private val _scheduleProviders: LiveData<List<ScheduleProviderProperties>> = this.authority.switchMap { authority ->
        this.dataSourcesRepository.readingScheduleProviders(authority)
    }

    val timestamps: LiveData<List<Schedule.Timestamp>?> =
        QuadrupleMediatorLiveData(rds, _startsAtInMs, _endsAtInMs, _scheduleProviders).switchMap { (rts, startsAtInMs, endsAtInMs, scheduleProviders) ->
            liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(getTimestamps(rts, startsAtInMs, endsAtInMs, scheduleProviders))
            }
        }

    private val _sourceLabel = MutableLiveData<String?>(null)
    val sourceLabel: LiveData<String?> = _sourceLabel

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
                    _sourceLabel.postValue(scheduleTimestamps.sourceLabel)
                    scheduleTimestamps.timestamps.firstNotNullOfOrNull { it.localTimeZone }?.let { localTimeZoneId ->
                        this._localTimeZoneId.postValue(localTimeZoneId)
                    }
                    return scheduleTimestamps.timestamps
                }
            }
        }
        if (hasProviderTimestampsReturned) {
            if (increaseEndTime(maxEnd = END_AT_DAYS_AFTER_AUTO_INC_MAX)) {
                return null // not loaded (loading)
            }
        }
        _sourceLabel.postValue(null)
        return emptyList() // loaded (not loading) == no service today
    }

    val showAccessibility: LiveData<Boolean> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY, DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY_DEFAULT
    ).distinctUntilChanged()
}