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
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SpanUtils
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.data.Accessibility
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.equalOrAfter
import org.mtransit.android.data.UISchedule
import org.mtransit.android.data.makeHeading
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleDaySeparatorBinding
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleHourSeparatorBinding
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleLoadingBinding
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleTimeBinding
import org.mtransit.android.ui.view.common.StickyHeaderItemDecorator
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.util.UIAccessibilityUtils
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.Constants
import org.mtransit.commons.beginningOfDay
import org.mtransit.commons.date
import org.mtransit.commons.hourOfTheDay
import org.mtransit.commons.isSameDay
import org.mtransit.commons.toCalendar
import java.text.DateFormat
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

    private val dayToHourToTimestamps = mutableListOf<Pair<Long, SparseArray<MutableList<Schedule.Timestamp>>>>()

    private var nextTimestamp: Schedule.Timestamp? = null

    private var nowToTheMinute: Long = UITimeUtils.currentTimeToTheMinuteMillis()

    private var optRds: RouteDirectionStop? = null

    var timestamps: List<Schedule.Timestamp>? = null
        set(value) {
            if (field == value) return
            field = value
            updateTimes()
        }

    var localTimeZone: TimeZone? = null
        set(value) {
            if (field == value) return
            field = value
            updateDateFormattersTimeZone() // before
            updateStartEndTimes() // 1st
            updateTimes() // 2nd
        }

    var startInMs: Long? = null
        set(value) {
            if (field == value) return
            field = value
            updateStartEndTimes()
        }

    var endInMs: Long? = null
        set(value) {
            if (field == value) return
            field = value
            updateStartEndTimes()
        }

    private var hourFormatter: ThreadSafeDateFormatter? = null

    private fun getHourFormatter(context: Context): ThreadSafeDateFormatter {
        return hourFormatter ?: UITimeUtils.getNewHourFormat(context, true)
            .apply { localTimeZone?.let { setTimeZone(it) } }
            .also { hourFormatter = it }
    }

    private val dayDateFormat by lazy {
        ThreadSafeDateFormatter(DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())).apply {
            localTimeZone?.let { setTimeZone(it) }
        }
    }

    private fun updateDateFormattersTimeZone() {
        localTimeZone?.let { localTimeZone ->
            hourFormatter?.setTimeZone(localTimeZone)
            dayDateFormat.setTimeZone(localTimeZone)
        }
    }

    fun setRDS(rds: RouteDirectionStop?) {
        this.optRds = rds
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
        if (this.startInMs == null || this.endInMs == null || this.localTimeZone == null) {
            return
        }
        val startInMs = this.startInMs ?: return
        val endInMs = this.endInMs ?: return
        val localTimeZone = this.localTimeZone ?: return
        val cal = startInMs.toCalendar(localTimeZone).beginningOfDay
        val lastDayBeginning = endInMs.toCalendar(localTimeZone).beginningOfDay.timeInMillis
        var i = 0
        var dataSetChanged = false
        while (i < this.dayToHourToTimestamps.size && cal.timeInMillis <= lastDayBeginning) {
            val existingDayBeginning = this.dayToHourToTimestamps[i].first
            while (cal.timeInMillis < existingDayBeginning) {
                this.dayToHourToTimestamps.add(i, cal.timeInMillis to makeDayHours())
                dataSetChanged = true
                i++
                cal.date++
            }
            i++
            cal.date++
        }
        while (cal.timeInMillis < lastDayBeginning) {
            this.dayToHourToTimestamps.add(cal.timeInMillis to makeDayHours())
            dataSetChanged = true
            cal.date++
        }
        if (dataSetChanged) {
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTimes() {
        val originalTimeCount = this.timesCount ?: -1
        clearTimes()
        if (this.timestamps == null || this.localTimeZone == null) {
            if (originalTimeCount > 0) {
                notifyDataSetChanged()
            }
            return
        }
        val timestamps = this.timestamps ?: return
        val localTimeZone = this.localTimeZone ?: return
        var newTimesCount = 0
        var dayBeginningCalendar: Calendar? = null
        var dayToHourToTimestamp: Pair<Long, SparseArray<MutableList<Schedule.Timestamp>>>? = null
        val calendar = Calendar.getInstance(localTimeZone)
        for (timestamp in timestamps) {
            calendar.timeInMillis = timestamp.t
            if (dayBeginningCalendar == null || !dayBeginningCalendar.isSameDay(calendar)) {
                dayBeginningCalendar = calendar.beginningOfDay
                dayToHourToTimestamp = this.dayToHourToTimestamps.firstOrNull { it.first == dayBeginningCalendar.timeInMillis }
                if (dayToHourToTimestamp == null) {
                    dayToHourToTimestamp = dayBeginningCalendar.timeInMillis to makeDayHours()
                    this.dayToHourToTimestamps.add(dayToHourToTimestamp)
                    dayToHourToTimestamp.second.forEach { _, timestamps ->
                        timestamps.clear() // do not keep old schedule from that day
                    }
                }
            }
            dayToHourToTimestamp?.second?.get(calendar.hourOfTheDay)?.add(timestamp)
            newTimesCount++
            if (this.nextTimestamp == null && timestamp.t >= this.nowToTheMinute) {
                this.nextTimestamp = timestamp
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
        val localTimeZone = this.localTimeZone ?: return NO_POSITION
        var index = 0
        val date = Date(item.t)
        var thatDate: Date
        var nextDate: Date?
        var nextHourOfTheDay: Int
        this.dayToHourToTimestamps.forEach { (dayBeginningMs, hourToTimes) ->
            index++ // day separator
            val dayCal = dayBeginningMs.toCalendar(localTimeZone)
            hourToTimes.forEach { hour, hourTimes ->
                index++ // hour separator
                if (hourTimes.isNotEmpty()) {
                    dayCal.hourOfTheDay = hour
                    thatDate = dayCal.time
                    if (date.equalOrAfter(thatDate)) {
                        nextHourOfTheDay = hour + 1
                        nextDate = null
                        if (nextHourOfTheDay < hourTimes.size) {
                            dayCal.hourOfTheDay = nextHourOfTheDay
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
        dayToHourToTimestamps.forEach { (_, hourToTimes) ->
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
                    dayToHourToTimestamps.size + // day separator
                    dayToHourToTimestamps.size * HOUR_SEPARATORS_COUNT + // time separator
                    1 // loading
    }

    private fun getTimestampItem(position: Int): Schedule.Timestamp? {
        var index = 0
        this.dayToHourToTimestamps.forEach { (_, hourToTimes) ->
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
        this.dayToHourToTimestamps.forEach { (_, hourToTimes) ->
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
            this.dayDateFormat,
            this.localTimeZone,
        )
    }

    // endregion

    private fun getDayItem(position: Int): Long? {
        var index = 0
        this.dayToHourToTimestamps.forEach { (dayBeginningMs, hourToTimes) ->
            if (position == index) {
                return dayBeginningMs
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
        if (position < 0) return null
        val localTimeZone = this.localTimeZone ?: return null
        var index = 0
        this.dayToHourToTimestamps.forEach { (dayBeginningMs, hourToTimes) ->
            index++ // day separator
            val cal: Calendar = dayBeginningMs.toCalendar(localTimeZone)
            (0 until HOUR_SEPARATORS_COUNT).forEach { hourOfTheDay ->
                if (index == position) {
                    cal.hourOfTheDay = hourOfTheDay
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
        this.dayToHourToTimestamps.forEach { (_, hourToTimes) ->
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
                    this.dayDateFormat,
                    this.localTimeZone,
                )
            }

            ITEM_VIEW_TYPE_HOUR_SEPARATORS -> {
                (holder as? HourSeparatorViewHolder)?.bind(
                    getHourItemTimestamp(position),
                    getHourFormatter(holder.context),
                    this.localTimeZone,
                )
            }

            ITEM_VIEW_TYPE_TIME -> {
                (holder as? TimeViewHolder)?.bind(
                    getTimestampItem(position),
                    nowToTheMinute,
                    nextTimestamp,
                    this.optRds,
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
            localTimeZone: TimeZone?,
        ) {
            if (timestampInMs == null) {
                binding.day.text = null
                return
            }
            val cal = localTimeZone?.let { timestampInMs.toCalendar(localTimeZone) } ?: timestampInMs.toCalendar()
            val timeSb = SpannableStringBuilder(
                UITimeUtils.getNearRelativeDay(
                    context,
                    timestampInMs,
                    dayDateFormat.formatThreadSafe(cal),
                )
            )
            if (nowToTheMinuteInMs > 0L) {
                val compareToNow = nowToTheMinuteInMs - timestampInMs
                val sameDay = localTimeZone?.let { nowToTheMinuteInMs.isSameDay(timestampInMs, localTimeZone) } == true
                if (sameDay) { // now
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

        fun bind(hourInMs: Long?, hourFormatter: ThreadSafeDateFormatter, localTimeZone: TimeZone?) {
            if (hourInMs == null) {
                binding.hour.text = null
            } else {
                val cal = localTimeZone?.let { hourInMs.toCalendar(localTimeZone) } ?: hourInMs.toCalendar()
                binding.hour.text = UITimeUtils.cleanNoRealTime(
                    false,
                    hourFormatter.formatThreadSafe(cal)
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

        val context: Context
            get() = binding.context

        fun bind(
            timestamp: Schedule.Timestamp? = null,
            nowToTheMinuteInMs: Long = -1L,
            nextTimestamp: Schedule.Timestamp? = null,
            optRds: RouteDirectionStop? = null,
            showingAccessibility: Boolean? = null,
        ) {
            if (timestamp == null) {
                binding.time.text = null
                return
            }
            val formattedTime = UITimeUtils.formatTimestamp(context, timestamp)
            var timeSb = SpannableStringBuilder(formattedTime)
            timeSb.append(
                UIAccessibilityUtils.decorate(
                    context,
                    Accessibility.decorate(Constants.EMPTY, timestamp.accessibleOrDefault),
                    showingAccessibility == true,
                    UIAccessibilityUtils.ImageSize.SMALL,
                    false
                )
            )
            val timeOnly = timeSb.toString()
            timestamp.makeHeading(context, optRds?.direction?.getHeading(context), small = false)?.let {
                timeSb.append(P1).append(it).append(P2)
            }
            UITimeUtils.cleanTimes(timeOnly, timeSb, 0.55)
            timeSb = UISchedule.decorateRealTime(context, timestamp, formattedTime, timeSb)
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