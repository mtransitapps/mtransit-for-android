package org.mtransit.android.ui.schedule

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.databinding.LayoutScheduleCalendarDayItemBinding
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

    private val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
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

    fun selectDay(dayInMs: Long, notifyAdapter: Boolean = true) {
        MTLog.d(this, "selectDay() > dayInMs: $dayInMs")
        val previousSelectedIndex = selectedDayInMs?.let { days.indexOf(it) } ?: -1
        selectedDayInMs = dayInMs
        val newSelectedIndex = days.indexOf(dayInMs)
        
        if (notifyAdapter) {
            if (previousSelectedIndex >= 0) {
                notifyItemChanged(previousSelectedIndex)
            }
            if (newSelectedIndex >= 0) {
                notifyItemChanged(newSelectedIndex)
            }
        }
    }

    fun getPositionForDay(dayInMs: Long): Int {
        val targetCalendar = dayInMs.toCalendar(localTimeZone).beginningOfDay
        return days.indexOfFirst { dayMs ->
            val dayCalendar = dayMs.toCalendar(localTimeZone).beginningOfDay
            dayCalendar.isSameDay(targetCalendar)
        }
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

        fun bind(dayInMs: Long) {
            val calendar = dayInMs.toCalendar(localTimeZone)
            val targetCalendar = dayInMs.toCalendar(localTimeZone).beginningOfDay
            val isSelected = selectedDayInMs?.toCalendar(localTimeZone)?.beginningOfDay?.isSameDay(targetCalendar) == true

            binding.dayName.text = dayNameFormat.format(calendar.time)
            binding.dayNumber.text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            // Update selection state
            if (isSelected) {
                binding.selectionIndicator.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.schedule_calendar_selection)
                )
                binding.dayName.alpha = 1f
                binding.dayNumber.alpha = 1f
            } else {
                binding.selectionIndicator.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.transparent)
                )
                binding.dayName.alpha = 0.7f
                binding.dayNumber.alpha = 0.7f
            }

            binding.root.setOnClickListener {
                selectDay(dayInMs)
                onDaySelected?.invoke(dayInMs)
            }
        }
    }
}
