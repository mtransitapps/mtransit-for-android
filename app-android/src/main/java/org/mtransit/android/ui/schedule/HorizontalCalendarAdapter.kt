package org.mtransit.android.ui.schedule

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.UISchedule
import org.mtransit.android.databinding.LayoutScheduleCalendarDayItemBinding
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.beginningOfDay
import org.mtransit.commons.isSameDay
import org.mtransit.commons.toCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HorizontalCalendarAdapter : RecyclerView.Adapter<HorizontalCalendarAdapter.DayViewHolder>(),
    MTLog.Loggable {

    companion object {
        private val LOG_TAG = HorizontalCalendarAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    @ColorInt
    var colorInt: Int? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    private val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthNameFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val days = mutableListOf<Long>() // List of day timestamps in milliseconds
    private var selectedDayInMs: Long? = null
    private var localTimeZone: TimeZone = TimeZone.getDefault()
    private var onDaySelected: ((Long) -> Unit)? = null

    var startInMs: Long? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field == value) return
            field = value
            updateDays()
        }

    var endInMs: Long? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field == value) return
            field = value
            updateDays()
        }

    fun setTimeZone(timeZone: TimeZone) {
        localTimeZone = timeZone
        dayNameFormat.timeZone = timeZone
        monthNameFormat.timeZone = timeZone
    }

    fun setOnDaySelectedListener(listener: (Long) -> Unit) {
        onDaySelected = listener
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDays() {
        val startInMs = this.startInMs ?: return
        val endInMs = this.endInMs ?: return

        days.clear()

        val calendar = startInMs.toCalendar(localTimeZone).beginningOfDay
        val endCalendar = endInMs.toCalendar(localTimeZone).beginningOfDay

        while (calendar.timeInMillis <= endCalendar.timeInMillis) {
            days.add(calendar.timeInMillis)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectDay(dayInMs: Long, notifyAdapter: Boolean = true) {
        val previousSelectedIndex = getSelectedPosition()
        selectedDayInMs = dayInMs
        val newSelectedIndex = getSelectedPosition()
        if (previousSelectedIndex == newSelectedIndex) return
        if (notifyAdapter) {
            notifyDataSetChanged() // need to redraw all days to remove selected
        }
    }

    fun getPositionForDay(dayInMs: Long): Int {
        val targetCalendar = dayInMs.toCalendar(localTimeZone).beginningOfDay
        return days.indexOfFirst { dayMs ->
            val dayCalendar = dayMs.toCalendar(localTimeZone).beginningOfDay
            dayCalendar.isSameDay(targetCalendar)
        }
    }

    fun getSelectedPosition(): Int {
        return selectedDayInMs?.let { getPositionForDay(it) } ?: -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = LayoutScheduleCalendarDayItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val dayInMs = days[position]
        holder.bind(dayInMs)
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(
        private val binding: LayoutScheduleCalendarDayItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @get:ColorInt
        private val pastTextColorInt: Int by lazy { UISchedule.getDefaultPastTextColor(binding.root.context) }

        @get:ColorInt
        private val nowTextColorInt: Int by lazy { UISchedule.getDefaultNowTextColor(binding.root.context) }

        @get:ColorInt
        private val futureTextColorInt: Int by lazy { UISchedule.getDefaultFutureTextColor(binding.root.context) }

        private val calendar = Calendar.getInstance(localTimeZone)

        fun bind(dayInMs: Long) = binding.apply {
            calendar.timeInMillis = dayInMs

            val todayBeginning = UITimeUtils.currentTimeMillis().toCalendar(localTimeZone).beginningOfDay
            val dayBeginning = dayInMs.toCalendar(localTimeZone).beginningOfDay
            
            val isSelected = selectedDayInMs?.let { selectedMs ->
                val selectedBeginning = selectedMs.toCalendar(localTimeZone).beginningOfDay
                dayBeginning.isSameDay(selectedBeginning)
            } == true
            
            val isToday = dayBeginning.isSameDay(todayBeginning)
            val isPast = dayInMs < todayBeginning.timeInMillis

            // Set text values
            dayName.text = dayNameFormat.format(calendar.time)
            dayNumber.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
            monthName.text = monthNameFormat.format(calendar.time)

            val typeface = if (isToday) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            // Make today bold
            dayName.typeface = typeface
            dayNumber.typeface = typeface
            monthName.typeface = typeface

            // Update selection state
            selectionIndicator.apply {
                colorInt?.let { setBackgroundColor(it) }
                isVisible = isSelected
            }
            // Set alpha for past days (75% = 0.75f)
            val color = when {
                isPast -> pastTextColorInt
                isToday -> nowTextColorInt
                else -> futureTextColorInt
            }
            dayName.setTextColor(color)
            dayNumber.setTextColor(color)
            monthName.setTextColor(color)

            root.setOnClickListener {
                selectDay(dayInMs)
                onDaySelected?.invoke(dayInMs)
            }
        }
    }
}
