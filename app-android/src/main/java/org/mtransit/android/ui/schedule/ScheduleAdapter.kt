package org.mtransit.android.ui.schedule

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.util.forEach
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SpanUtils
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.data.Accessibility
import org.mtransit.android.commons.data.RouteTripStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.equalOrAfter
import org.mtransit.android.data.UISchedule
import org.mtransit.android.data.decorateDirection
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleDaySeparatorBinding
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleHourSeparatorBinding
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleLoadingBinding
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleTimeBinding
import org.mtransit.android.ui.view.common.StickyHeaderItemDecorator
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.util.UIAccessibilityUtils
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.Constants
import org.mtransit.commons.FeatureFlags
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ScheduleAdapter
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyHeaderItemDecorator.StickyAdapter<RecyclerView.ViewHolder>,
    MTLog.Loggable {

    companion object {
        private val LOG_TAG = ScheduleAdapter::class.java.simpleName

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(ITEM_VIEW_TYPE_DAY_SEPARATORS, ITEM_VIEW_TYPE_HOUR_SEPARATORS, ITEM_VIEW_TYPE_TIME, ITEM_VIEW_TYPE_LOADING)
        annotation class ScheduleItemViewType

        private const val ITEM_VIEW_TYPE_DAY_SEPARATORS = 0
        private const val ITEM_VIEW_TYPE_HOUR_SEPARATORS = 1
        private const val ITEM_VIEW_TYPE_TIME = 2
        private const val ITEM_VIEW_TYPE_LOADING = 3

        private const val HOUR_SEPARATORS_COUNT = 24

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

    private var theLogTag: String = LOG_TAG

    override fun getLogTag(): String = this.theLogTag

    private var timesCount: Int? = null

    private fun makeDayHours(): SparseArray<MutableList<Schedule.Timestamp>> {
        return SparseArray<MutableList<Schedule.Timestamp>>().apply {
            for (hourOfTheDay in 0 until HOUR_SEPARATORS_COUNT) {
                put(hourOfTheDay, mutableListOf())
            }
        }
    }

    private val dayToHourToTimes = mutableListOf<Pair<Long, SparseArray<MutableList<Schedule.Timestamp>>>>()

    private var nextTimestamp: Schedule.Timestamp? = null

    private var nowToTheMinute: Long = UITimeUtils.currentTimeToTheMinuteMillis()

    private var optRts: RouteTripStop? = null

    private var startInMs: Long? = null
    private var endInMs: Long? = null

    private var hourFormatter: ThreadSafeDateFormatter? = null

    private fun getHourFormatter(context: Context): ThreadSafeDateFormatter {
        return hourFormatter ?: UITimeUtils.getNewHourFormat(context, true).also { hourFormatter = it }
    }

    private val dayDateFormat by lazy { ThreadSafeDateFormatter("EEEE, MMM d, yyyy", Locale.getDefault()) }

    fun setRTS(rts: RouteTripStop?) {
        this.optRts = rts
    }

    var showingAccessibility: Boolean? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                notifyDataSetChanged()
            }
            field = value
        }

    @SuppressLint("NotifyDataSetChanged")
    fun onTimeChanged(newTimeInMs: Long = UITimeUtils.currentTimeToTheMinuteMillis()) {
        this.nowToTheMinute = newTimeInMs
        if (this.nowToTheMinute > 0L) {
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateStartEndTimes() {
        if (this.startInMs == null || this.endInMs == null) {
            return
        }
        val cal = this.startInMs?.let { UITimeUtils.setBeginningOfDay(UITimeUtils.getNewCalendar(it)) } ?: return
        val lastDayBeginning = this.endInMs?.let { UITimeUtils.setBeginningOfDay(UITimeUtils.getNewCalendar(it)).timeInMillis } ?: return
        var i = 0
        var dataSetChanged = false
        while (i < this.dayToHourToTimes.size && cal.timeInMillis <= lastDayBeginning) {
            val existingDayBeginning = this.dayToHourToTimes[i].first
            while (cal.timeInMillis < existingDayBeginning) {
                this.dayToHourToTimes.add(i, Pair(cal.timeInMillis, makeDayHours()))
                dataSetChanged = true
                i++
                cal.add(Calendar.DATE, 1)
            }
            i++
            cal.add(Calendar.DATE, 1)
        }
        while (cal.timeInMillis < lastDayBeginning) {
            this.dayToHourToTimes.add(Pair(cal.timeInMillis, makeDayHours()))
            dataSetChanged = true
            cal.add(Calendar.DATE, 1)
        }
        if (dataSetChanged) {
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setTimes(times: List<Schedule.Timestamp>?) {
        val originalTimeCount = this.timesCount ?: -1
        clearTimes()
        if (times == null) {
            if (originalTimeCount > 0) {
                notifyDataSetChanged()
            }
            return
        }
        var newTimesCount = 0
        var dayBeginning = -1L
        var dayToHourToTime: Pair<Long, SparseArray<MutableList<Schedule.Timestamp>>>? = null
        for (time in times) {
            val hourOfTheDay: Int = UITimeUtils.getHourOfTheDay(time.t)
            if (dayBeginning < 0L || !UITimeUtils.isSameDay(dayBeginning, time.t)) {
                dayBeginning = UITimeUtils.setBeginningOfDay(UITimeUtils.getNewCalendar(time.t)).timeInMillis
                dayToHourToTime = dayToHourToTimes.firstOrNull { it.first == dayBeginning }
                if (dayToHourToTime == null) {
                    dayToHourToTime = Pair(dayBeginning, makeDayHours())
                    dayToHourToTimes.add(dayToHourToTime)
                    dayToHourToTime.second.forEach { _, timestamps ->
                        timestamps.clear() // do not keep old schedule from that day
                    }
                }
            }
            dayToHourToTime?.second?.get(hourOfTheDay)?.add(time)
            newTimesCount++
            if (this.nextTimestamp == null && time.t >= this.nowToTheMinute) {
                this.nextTimestamp = time
            }
        }
        this.timesCount = newTimesCount
        notifyDataSetChanged()
    }

    fun getScrollToNowPosition(): Int? {
        if (this.startInMs != null && this.endInMs != null && this.itemCount > 0) { // isReady()
            return getTodaySelectPosition()
        }
        return null
    }

    private fun getTodaySelectPosition(): Int {
        nextTimestamp?.let { nextTimestamp ->
            val nextTimePosition: Int = getPosition(nextTimestamp)
            if (nextTimePosition == NO_POSITION) {
                return 0 // ELSE show 1st of the list
            }
            return nextTimePosition
        }
        return 0 // ELSE show 1st of the list
    }

    private fun getPosition(item: Any): Int {
        if (item !is Schedule.Timestamp) {
            return NO_POSITION
        }
        var index = 0
        val date = Date(item.t)
        var thatDate: Date
        var nextDate: Date?
        var nextHourOfTheDay: Int
        this.dayToHourToTimes.forEach { (dayBeginning, hourToTimes) ->
            index++ // day separator
            val dayCal = UITimeUtils.getNewCalendar(dayBeginning)
            hourToTimes.forEach { hour, hourTimes ->
                index++ // hour separator
                if (hourTimes.isNotEmpty()) {
                    dayCal[Calendar.HOUR_OF_DAY] = hour
                    thatDate = dayCal.time
                    if (date.equalOrAfter(thatDate)) {
                        nextHourOfTheDay = hour + 1
                        nextDate = null
                        if (nextHourOfTheDay < hourTimes.size) {
                            dayCal[Calendar.HOUR_OF_DAY] = nextHourOfTheDay
                            nextDate = dayCal.time
                        }
                        if (nextDate == null || date.before(nextDate)) {
                            for (hourTime in hourTimes) {
                                if (item.t == hourTime.t) {
                                    return index
                                }
                                index++ // after
                            }
                        } else {
                            index += hourTimes.size // after
                        }
                    } else {
                        index += hourTimes.size // after
                    }
                }
            }
        }
        return NO_POSITION
    }

    private fun clearTimes() {
        dayToHourToTimes.forEach { (_, hourToTimes) ->
            hourToTimes.forEach { _, timestamps -> timestamps.clear() }
        }
        timesCount = null
        this.nextTimestamp = null
    }

    fun isReady() = this.timesCount != null

    override fun getItemCount(): Int {
        return if (!isReady()) 0
        else
            (this.timesCount ?: 0) + // times
                    dayToHourToTimes.size + // day separator
                    dayToHourToTimes.size * HOUR_SEPARATORS_COUNT + // time separator
                    1 // loading
    }

    private fun getTimestampItem(position: Int): Schedule.Timestamp? {
        var index = 0
        this.dayToHourToTimes.forEach { (_, hourToTimes) ->
            index++ // day separator
            (0 until HOUR_SEPARATORS_COUNT).forEach { hourOfTheDay ->
                index++ // hour separator
                if (position >= index && position < index + hourToTimes.get(hourOfTheDay).size) {
                    return hourToTimes.get(hourOfTheDay)[position - index]
                }
                index += hourToTimes.get(hourOfTheDay).size
            }
        }
        return null
    }

    // region sticky header

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var index = 0
        this.dayToHourToTimes.forEach { (_, hourToTimes) ->
            val dayPosition = index
            val startIndex = index
            index++ // day separator
            (0 until HOUR_SEPARATORS_COUNT).forEach { hourOfTheDay ->
                index++ // hour separator
                index += hourToTimes.get(hourOfTheDay).size
            }
            val endIndex = index - 1
            if (itemPosition in (startIndex..endIndex)) {
                return dayPosition
            }
        }
        throw RuntimeException("Header ID NOT found at $itemPosition! (index:$index)")
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return DaySeparatorViewHolder.from(parent)
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, headerPosition: Int) {
        (holder as? DaySeparatorViewHolder)?.bind(
            getDayItem(headerPosition),
            nowToTheMinute,
            this.dayDateFormat
        )
    }

    // endregion

    private fun getDayItem(position: Int): Long? {
        var index = 0
        this.dayToHourToTimes.forEach { (dayBeginning, hourToTimes) ->
            if (position == index) {
                return dayBeginning
            }
            index++ // day separator
            (0 until HOUR_SEPARATORS_COUNT).forEach { hourOfTheDay ->
                index++ // hour separator
                index += hourToTimes.get(hourOfTheDay).size
            }
        }
        return null
    }

    private fun getHourItemTimestamp(position: Int): Long? {
        var index = 0
        this.dayToHourToTimes.forEach { (dayBeginning, hourToTimes) ->
            index++ // day separator
            val cal: Calendar = UITimeUtils.getNewCalendar(dayBeginning)
            (0 until HOUR_SEPARATORS_COUNT).forEach { hourOfTheDay ->
                if (index == position) {
                    cal[Calendar.HOUR_OF_DAY] = hourOfTheDay
                    return cal.timeInMillis
                }
                index++ // hour separator
                index += hourToTimes.get(hourOfTheDay).size
            }
        }
        return null
    }

    @ScheduleItemViewType
    override fun getItemViewType(position: Int): Int {
        var index = 0
        this.dayToHourToTimes.forEach { (_, hourToTimes) ->
            if (position == index) {
                return ITEM_VIEW_TYPE_DAY_SEPARATORS
            }
            index++ // separator
            (0 until HOUR_SEPARATORS_COUNT).forEach { hourOfTheDay ->
                if (index == position) {
                    return ITEM_VIEW_TYPE_HOUR_SEPARATORS
                }
                index++ // separator
                if (position >= index && position < index + hourToTimes.get(hourOfTheDay).size) {
                    return ITEM_VIEW_TYPE_TIME
                }
                index += hourToTimes.get(hourOfTheDay).size
            }
        }
        if (position == index) {
            return ITEM_VIEW_TYPE_LOADING
        }
        throw RuntimeException("View type not found at $position! (index:$index)")
    }

    override fun onCreateViewHolder(parent: ViewGroup, @ScheduleItemViewType viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_DAY_SEPARATORS -> DaySeparatorViewHolder.from(parent)
            ITEM_VIEW_TYPE_HOUR_SEPARATORS -> HourSeparatorViewHolder.from(parent)
            ITEM_VIEW_TYPE_TIME -> TimeViewHolder.from(parent)
            ITEM_VIEW_TYPE_LOADING -> LoadingViewHolder.from(parent)
            else -> throw RuntimeException("Unexpected view type $viewType!")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ITEM_VIEW_TYPE_DAY_SEPARATORS -> {
                (holder as? DaySeparatorViewHolder)?.bind(
                    getDayItem(position),
                    nowToTheMinute,
                    this.dayDateFormat
                )
            }

            ITEM_VIEW_TYPE_HOUR_SEPARATORS -> {
                (holder as? HourSeparatorViewHolder)?.bind(
                    getHourItemTimestamp(position),
                    getHourFormatter(holder.context),
                )
            }

            ITEM_VIEW_TYPE_TIME -> {
                (holder as? TimeViewHolder)?.bind(
                    getTimestampItem(position),
                    nowToTheMinute,
                    nextTimestamp,
                    this.optRts,
                    this.showingAccessibility,
                )
            }

            ITEM_VIEW_TYPE_LOADING -> {
                (holder as? LoadingViewHolder)?.bind(
                )
            }

            else -> throw RuntimeException("Unexpected view to bind $position!")
        }
    }

    fun setStartEnd(startInMs: Long?, endInMs: Long?) {
        this.startInMs = startInMs
        this.endInMs = endInMs
        updateStartEndTimes()
    }

    private class DaySeparatorViewHolder private constructor(
        private val binding: LayoutPoiDetailStatusScheduleDaySeparatorBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): DaySeparatorViewHolder {
                val binding = LayoutPoiDetailStatusScheduleDaySeparatorBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return DaySeparatorViewHolder(binding)
            }

        }

        val context: Context
            get() = binding.root.context

        fun bind(
            timestampInMs: Long?,
            nowToTheMinuteInMs: Long = -1L,
            dayDateFormat: ThreadSafeDateFormatter,
        ) {
            if (timestampInMs == null) {
                binding.day.text = null
                return
            }
            val timeSb = SpannableStringBuilder(
                UITimeUtils.getNearRelativeDay(
                    context,
                    timestampInMs,
                    dayDateFormat.formatThreadSafe(UITimeUtils.getNewCalendar(timestampInMs).time),
                )
            )
            if (nowToTheMinuteInMs > 0L) {
                val compareToNow = nowToTheMinuteInMs - timestampInMs
                val sameDay = UITimeUtils.isSameDay(nowToTheMinuteInMs, timestampInMs)
                if (sameDay
                ) { // now
                    SpanUtils.setAll(timeSb, getScheduleListTimesNowTextColor(context), SCHEDULE_LIST_TIMES_NOW_STYLE)
                } else if (compareToNow > 0L) { // past
                    SpanUtils.setAll(timeSb, getScheduleListTimesPastTextColor(context), SCHEDULE_LIST_TIMES_PAST_STYLE)
                } else if (compareToNow < 0L) { // future
                    SpanUtils.setAll(timeSb, getScheduleListTimesFutureTextColor(context), SCHEDULE_LIST_TIMES_FUTURE_STYLE)
                }
            }
            binding.day.text = timeSb
        }
    }

    private class HourSeparatorViewHolder private constructor(
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

        fun bind(hourInMs: Long?, hourFormatter: ThreadSafeDateFormatter) {
            if (hourInMs == null) {
                binding.hour.text = null
            } else {
                binding.hour.text = UITimeUtils.cleanNoRealTime(
                    false,
                    hourFormatter.formatThreadSafe(hourInMs)
                )
            }
        }
    }

    private class TimeViewHolder private constructor(
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

        }

        private val deviceTimeZone = TimeZone.getDefault()

        val context: Context
            get() = binding.context

        fun bind(
            timestamp: Schedule.Timestamp? = null,
            nowToTheMinuteInMs: Long = -1L,
            nextTimestamp: Schedule.Timestamp? = null,
            optRts: RouteTripStop? = null,
            showingAccessibility: Boolean? = null,
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
            if (FeatureFlags.F_ACCESSIBILITY_CONSUMER) {
                timeSb.append(
                    UIAccessibilityUtils.decorate(
                        context,
                        Accessibility.decorate(Constants.EMPTY, timestamp.accessibleOrDefault),
                        showingAccessibility == true,
                        UIAccessibilityUtils.ImageSize.SMALL,
                        false
                    )
                )
            }
            val timeOnly = timeSb.toString()
            if (timestamp.hasHeadsign()) {
                val timestampHeading = timestamp.getHeading(context)
                if (!Trip.isSameHeadsign(timestampHeading, optRts?.trip?.getHeading(context))) {
                    timeSb.append(P1).append(
                        timestamp.decorateDirection(context, false)
                    ).append(P2)
                }
            }
            UITimeUtils.cleanTimes(timeOnly, timeSb, 0.55)
            timeSb = UISchedule.decorateRealTime(context, timestamp, userTime, timeSb)
            timeSb = UISchedule.decorateOldSchedule(timestamp, timeSb)
            val nextTimeInMsT = nextTimestamp?.t ?: -1L
            if (nowToTheMinuteInMs > 0L) {
                val compareToNow = nowToTheMinuteInMs - timestamp.t
                val sameTimestamp = nextTimeInMsT == timestamp.t
                if (sameTimestamp
                ) { // now
                    SpanUtils.setAll(timeSb, getScheduleListTimesNowTextColor(context), SCHEDULE_LIST_TIMES_NOW_STYLE)
                } else if (compareToNow > 0L) { // past
                    SpanUtils.setAll(timeSb, getScheduleListTimesPastTextColor(context), SCHEDULE_LIST_TIMES_PAST_STYLE)
                } else if (compareToNow < 0L) { // future
                    if (timestamp.isNoPickup) {
                        SpanUtils.setAll(timeSb, getScheduleListTimesPastTextColor(context), SCHEDULE_LIST_TIMES_PAST_STYLE)
                    } else {
                        SpanUtils.setAll(timeSb, getScheduleListTimesFutureTextColor(context), SCHEDULE_LIST_TIMES_FUTURE_STYLE)
                    }
                }
            }
            binding.time.text = timeSb
        }
    }

    class LoadingViewHolder private constructor(
        private val binding: LayoutPoiDetailStatusScheduleLoadingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): LoadingViewHolder {
                val binding = LayoutPoiDetailStatusScheduleLoadingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return LoadingViewHolder(binding)
            }

        }

        val context: Context
            get() = binding.root.context

        fun bind() {
            // NOTHING
        }
    }
}