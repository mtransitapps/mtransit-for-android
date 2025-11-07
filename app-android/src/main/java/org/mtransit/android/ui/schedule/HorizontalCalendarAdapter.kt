package org.mtransit.android.ui.schedule

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
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

        private val calendar = Calendar.getInstance(localTimeZone)

        fun bind(dayInMs: Long) {
            calendar.timeInMillis = dayInMs
            
            val todayBeginning = UITimeUtils.currentTimeMillis().toCalendar(localTimeZone).beginningOfDay
            val dayBeginning = dayInMs.toCalendar(localTimeZone).beginningOfDay
            
            val isSelected = selectedDayInMs?.let { selectedMs ->
                val selectedCal = selectedMs.toCalendar(localTimeZone).beginningOfDay
                dayBeginning.isSameDay(selectedCal)
            } == true
            
            val isToday = dayBeginning.isSameDay(todayBeginning)
            val isPast = dayInMs < todayBeginning.timeInMillis

            // Set text values
            binding.dayName.text = dayNameFormat.format(calendar.time)
            binding.dayNumber.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
            binding.monthName.text = monthNameFormat.format(calendar.time)

            // Make today bold
            if (isToday) {
                binding.dayNumber.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                binding.dayNumber.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            // Update selection state - show/hide circular background
            binding.selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Set alpha for past days (75% = 0.75f)
            val alpha = if (isPast) 0.75f else 1f
            binding.dayName.alpha = alpha
            binding.dayNumber.alpha = alpha
            binding.monthName.alpha = alpha

            binding.root.setOnClickListener {
                selectDay(dayInMs)
                onDaySelected?.invoke(dayInMs)
            }
        }
    }
}
