package org.mtransit.android.ui.schedule.day

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.util.forEach
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SpanUtils
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.data.UISchedule
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleHourSeparatorBinding
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleTimeBinding
import org.mtransit.android.util.UITimeUtils
import java.util.Calendar
import java.util.Date
import java.util.TimeZone


class ScheduleDayAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = ScheduleDayAdapter::class.java.simpleName

        private const val ITEM_VIEW_TYPE_HOUR_SEPARATORS = 0

        private const val ITEM_VIEW_TYPE_TIME = 1

        private const val HOUR_SEPARATORS_COUNT = 24

        private const val COUNT_UNKNOWN = -1

        private const val UNKNOWN = -1L
    }

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private var timesCount = COUNT_UNKNOWN

    private val hours = mutableListOf<Date>()

    private val hourToTimes = SparseArray<MutableList<Schedule.Timestamp>>().apply {
        for (hourOfTheDay in 0 until HOUR_SEPARATORS_COUNT) {
            put(hourOfTheDay, mutableListOf())
        }
    }

    private var nextTimestamp: Schedule.Timestamp? = null

    private var nowToTheMinute: Long = UITimeUtils.currentTimeToTheMinuteMillis()

    private var optRts: RouteTripStop? = null

    private var dayStartsAtMs: Long = UNKNOWN

    private val todayStartsAt = UITimeUtils.getBeginningOfTodayCal()

    private val todayEndsAt: Calendar by lazy { UITimeUtils.getBeginningOfTomorrowCal().apply { add(Calendar.MILLISECOND, -1) } }

    private var hourFormatter: ThreadSafeDateFormatter? = null

    private fun getHourFormatter(context: Context): ThreadSafeDateFormatter {
        return hourFormatter ?: UITimeUtils.getNewHourFormat(context).also { hourFormatter = it }
    }

    fun setRTS(rts: RouteTripStop?) {
        this.optRts = rts
    }

    @SuppressLint("NotifyDataSetChanged")
    fun onTimeChanged(newTimeInMs: Long = UITimeUtils.currentTimeToTheMinuteMillis()) {
        this.nowToTheMinute = newTimeInMs
        if (this.nowToTheMinute > 0L) {
            notifyDataSetChanged()
        }
    }

    fun setTimes(times: List<Schedule.Timestamp>?) {
        clearTimes()
        if (times != null) {
            timesCount = 0
            for (time in times) {
                val hourOfTheDay = UITimeUtils.getHourOfTheDay(time.t)
                hourToTimes[hourOfTheDay].add(time)
                timesCount++
                if (this.nextTimestamp == null && time.t >= this.nowToTheMinute) {
                    this.nextTimestamp = time
                }
            }
        }
    }

    fun getScrollToNowPosition(): Int? {
        if (dayStartsAtMs > UNKNOWN && itemCount > 0) {
            return when {
                dayStartsAtMs < todayStartsAt.timeInMillis -> { // past
                    itemCount - 1 // scroll down (has to be < itemCount to not be ignored by LinearLayoutManager)
                }
                todayEndsAt.timeInMillis < dayStartsAtMs -> { // future
                    0 // scroll up
                }
                else -> { // today
                    getTodaySelectPosition()
                }
            }
        }
        return null
    }

    private fun getTodaySelectPosition(): Int {
        nextTimestamp?.let { nextTimeInMs ->
            var nextTimePosition: Int = getPosition(nextTimeInMs)
            if (nextTimePosition > 0) {
                nextTimePosition-- // show 1 more time on top of the list
                if (nextTimePosition > 0) {
                    nextTimePosition-- // show 1 more time on top of the list
                }
            }
            if (nextTimePosition >= 0) {
                return nextTimePosition
            }
        }
        return 0
    }

    private fun getPosition(item: Any): Int {
        var index = 0
        if (item !is Schedule.Timestamp) {
            return index
        }
        val date = Date(item.t)
        var thatDate: Date
        var nextDate: Date?
        var nextHourOfTheDay: Int
        for (hourOfTheDay in 0 until HOUR_SEPARATORS_COUNT) {
            index++ // separator
            if (hourToTimes[hourOfTheDay].size > 0) {
                thatDate = hours[hourOfTheDay]
                if (date.after(thatDate)) {
                    nextHourOfTheDay = hourOfTheDay + 1
                    nextDate = if (nextHourOfTheDay < hours.size) hours[nextHourOfTheDay] else null
                    if (nextDate == null || date.before(nextDate)) {
                        for (hourTime in hourToTimes[hourOfTheDay]) {
                            if (item.t == hourTime.t) {
                                return index
                            }
                            index++ // after
                        }
                    } else {
                        index += hourToTimes[hourOfTheDay].size // after
                    }
                } else {
                    index += hourToTimes[hourOfTheDay].size // after
                }
            }
        }
        return -1
    }

    private fun clearTimes() {
        for (hourOfTheDay in 0 until HOUR_SEPARATORS_COUNT) {
            hourToTimes[hourOfTheDay]?.clear()
        }
        timesCount = COUNT_UNKNOWN
        this.nextTimestamp = null
    }

    fun isReady() = this.timesCount != COUNT_UNKNOWN

    override fun getItemCount(): Int {
        return if (isReady()) this.timesCount + HOUR_SEPARATORS_COUNT else 0
    }

    private fun getItem(position: Int): Schedule.Timestamp? {
        var index = 0
        for (hourOfTheDay in 0 until HOUR_SEPARATORS_COUNT) {
            index++ // separator
            if (position >= index && position < index + this.hourToTimes.get(hourOfTheDay).size) {
                return this.hourToTimes.get(hourOfTheDay)[position - index]
            }
            index += this.hourToTimes.get(hourOfTheDay).size
        }
        return null
    }

    override fun getItemViewType(position: Int): Int {
        val item: Any? = getItem(position)
        return if (item is Schedule.Timestamp) {
            ITEM_VIEW_TYPE_TIME
        } else {
            ITEM_VIEW_TYPE_HOUR_SEPARATORS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HOUR_SEPARATORS -> HourSeparatorViewHolder.from(parent)
            ITEM_VIEW_TYPE_TIME -> TimeViewHolder.from(parent)
            else -> throw RuntimeException("Unexpected view type $viewType!")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ITEM_VIEW_TYPE_HOUR_SEPARATORS -> {
                (holder as? HourSeparatorViewHolder)?.bind(
                    getItemHourSeparator(position)?.let { this.hours[it] },
                    getHourFormatter(holder.context),
                )
            }
            ITEM_VIEW_TYPE_TIME -> {
                (holder as? TimeViewHolder)?.bind(
                    getItem(position),
                    nowToTheMinute,
                    nextTimestamp,
                    this.optRts
                )
            }
            else -> throw RuntimeException("Unexpected view to bind $position!")
        }
    }

    private fun getItemHourSeparator(position: Int): Int? {
        var index = 0
        for (hourOfTheDay in 0 until HOUR_SEPARATORS_COUNT) {
            if (index == position) {
                return hourOfTheDay
            }
            index++
            index += hourToTimes[hourOfTheDay].size
        }
        return null
    }

    fun setYearMonthDay(yearMonthDay: String?) {
        theLogTag = yearMonthDay?.let { "${LOG_TAG}-${yearMonthDay}" } ?: LOG_TAG
    }

    fun setDayStartsAt(dayStartsAtMs: Long) {
        this.dayStartsAtMs = dayStartsAtMs
        resetHours()
        if (this.dayStartsAtMs > UNKNOWN) {
            initHours(dayStartsAtMs)
        }
    }

    private fun initHours(dayStartsAtMs: Long) {
        resetHours()
        val cal: Calendar = UITimeUtils.getNewCalendar(dayStartsAtMs)
        for (hourOfTheDay in 0 until HOUR_SEPARATORS_COUNT) {
            cal[Calendar.HOUR_OF_DAY] = hourOfTheDay
            hours.add(cal.time)
        }
    }

    private fun resetHours() {
        hours.clear()
        hourToTimes.forEach { _, value ->
            value.clear()
        }
    }

    class HourSeparatorViewHolder private constructor(
        private val binding: LayoutPoiDetailStatusScheduleHourSeparatorBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): HourSeparatorViewHolder {
                val binding = LayoutPoiDetailStatusScheduleHourSeparatorBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return HourSeparatorViewHolder(binding)
            }
        }

        val context: Context
            get() = binding.root.context

        fun bind(hour: Date?, hourFormatter: ThreadSafeDateFormatter) {
            if (hour == null) {
                binding.hour.text = null
            } else {
                binding.hour.text = UITimeUtils.cleanNoRealTime(
                    false,
                    hourFormatter.formatThreadSafe(hour)
                )
            }
        }
    }

    class TimeViewHolder private constructor(
        private val binding: LayoutPoiDetailStatusScheduleTimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): TimeViewHolder {
                val binding = LayoutPoiDetailStatusScheduleTimeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return TimeViewHolder(binding)
            }

            private const val P2 = ")"
            private const val P1 = " ("

            private val SCHEDULE_LIST_TIMES_PAST_STYLE = SpanUtils.getNewNormalStyleSpan()

            private val SCHEDULE_LIST_TIMES_NOW_STYLE = SpanUtils.getNewBoldStyleSpan()

            private val SCHEDULE_LIST_TIMES_FUTURE_STYLE = SpanUtils.getNewNormalStyleSpan()

            private var scheduleListTimesNowTextColor: ForegroundColorSpan? = null

            private var scheduleListTimesPastTextColor: ForegroundColorSpan? = null

            private var scheduleListTimesFutureTextColor: ForegroundColorSpan? = null

            fun getScheduleListTimesNowTextColor(context: Context): ForegroundColorSpan {
                return scheduleListTimesNowTextColor ?: SpanUtils.getNewTextColor(UISchedule.getDefaultNowTextColor(context))
                    .also { scheduleListTimesNowTextColor = it }
            }

            fun getScheduleListTimesPastTextColor(context: Context): ForegroundColorSpan {
                return scheduleListTimesPastTextColor ?: SpanUtils.getNewTextColor(UISchedule.getDefaultPastTextColor(context))
                    .also { scheduleListTimesPastTextColor = it }
            }

            fun getScheduleListTimesFutureTextColor(context: Context): ForegroundColorSpan {
                return scheduleListTimesFutureTextColor ?: SpanUtils.getNewTextColor(UISchedule.getDefaultFutureTextColor(context))
                    .also { scheduleListTimesFutureTextColor = it }
            }

            @JvmStatic
            fun resetColorCache() {
                scheduleListTimesNowTextColor = null
                scheduleListTimesPastTextColor = null
                scheduleListTimesFutureTextColor = null
            }
        }

        private val deviceTimeZone = TimeZone.getDefault()

        val context: Context
            get() = binding.root.context

        fun bind(
            timestamp: Schedule.Timestamp? = null,
            nowToTheMinuteInMs: Long = -1L,
            nextTimestamp: Schedule.Timestamp? = null,
            optRts: RouteTripStop? = null
        ) {
            if (timestamp == null) {
                binding.time.text = null
                return
            }
            val userTime: String = UITimeUtils.formatTime(context, timestamp)
            var timeSb = SpannableStringBuilder(userTime)
            val timestampTZ: TimeZone = TimeZone.getTimeZone(timestamp.localTimeZone)
            if (timestamp.hasLocalTimeZone() && this.deviceTimeZone != timestampTZ) {
                val localTime = UITimeUtils.formatTime(context, timestamp, timestampTZ)
                if (!localTime.equals(userTime, ignoreCase = true)) {
                    timeSb.append(P1).append(context.getString(R.string.local_time_and_time, localTime)).append(P2)
                }
            }
            val timeOnly = timeSb.toString()
            if (timestamp.hasHeadsign()) {
                val timestampHeading = timestamp.getHeading(context)
                if (!Trip.isSameHeadsign(timestampHeading, optRts?.trip?.getHeading(context))) {
                    timeSb.append(P1).append(timestamp.getUIHeading(context, false)).append(P2)
                }
            }
            UITimeUtils.cleanTimes(timeOnly, timeSb, 0.55)
            timeSb = UISchedule.decorateRealTime(context, timestamp, userTime, timeSb)
            timeSb = UISchedule.decorateOldSchedule(timestamp, timeSb)
            val nextTimeInMsT = nextTimestamp?.t ?: -1L
            if (nowToTheMinuteInMs > 0L) {
                val compareToNow = nowToTheMinuteInMs - timestamp.t
                val sameDay = nextTimeInMsT >= 0L && UITimeUtils.isSameDay(nowToTheMinuteInMs, nextTimeInMsT)
                if (sameDay
                    && compareToNow == 0L
                ) { // now
                    SpanUtils.setAll(timeSb, getScheduleListTimesNowTextColor(context), SCHEDULE_LIST_TIMES_NOW_STYLE)
                } else if (compareToNow > 0L) { // past
                    SpanUtils.setAll(timeSb, getScheduleListTimesPastTextColor(context), SCHEDULE_LIST_TIMES_PAST_STYLE)
                } else if (compareToNow < 0L) { // future
                    SpanUtils.setAll(timeSb, getScheduleListTimesFutureTextColor(context), SCHEDULE_LIST_TIMES_FUTURE_STYLE)
                }
            }
            binding.time.text = timeSb
        }
    }
}