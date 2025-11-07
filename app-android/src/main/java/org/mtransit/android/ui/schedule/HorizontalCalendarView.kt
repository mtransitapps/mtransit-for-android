package org.mtransit.android.ui.schedule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.commons.beginningOfDay
import org.mtransit.commons.toCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HorizontalCalendarView(
    private val context: Context,
    private val scrollView: HorizontalScrollView,
    private val daysContainer: LinearLayout,
    private var onDaySelected: ((Long) -> Unit)? = null
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = HorizontalCalendarView::class.java.simpleName
        private const val DAYS_TO_SHOW = 21 // Show 3 weeks worth of days
    }

    override fun getLogTag(): String = LOG_TAG

    private val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val dayViews = mutableListOf<Pair<Long, View>>() // Store (dayInMs, view) pairs
    private var selectedDayInMs: Long? = null
    private var localTimeZone: TimeZone = TimeZone.getDefault()

    fun setTimeZone(timeZone: TimeZone) {
        localTimeZone = timeZone
        dayNameFormat.timeZone = timeZone
    }

    fun setupCalendar(centerDateInMs: Long) {
        MTLog.d(this, "setupCalendar() > centerDateInMs: $centerDateInMs")
        daysContainer.removeAllViews()
        dayViews.clear()

        val calendar = centerDateInMs.toCalendar(localTimeZone).beginningOfDay
        // Start from half of DAYS_TO_SHOW before the center date
        calendar.add(Calendar.DAY_OF_YEAR, -(DAYS_TO_SHOW / 2))

        val inflater = LayoutInflater.from(context)
        
        for (i in 0 until DAYS_TO_SHOW) {
            val dayInMs = calendar.timeInMillis
            val dayView = inflater.inflate(R.layout.layout_schedule_calendar_day_item, daysContainer, false)
            
            val dayNameTextView = dayView.findViewById<TextView>(R.id.day_name)
            val dayNumberTextView = dayView.findViewById<TextView>(R.id.day_number)

            dayNameTextView.text = dayNameFormat.format(calendar.time)
            dayNumberTextView.text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            dayView.setOnClickListener {
                selectDay(dayInMs)
                onDaySelected?.invoke(dayInMs)
            }

            dayViews.add(dayInMs to dayView)
            daysContainer.addView(dayView)
            
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Select the center date (today or specified date)
        selectDay(centerDateInMs.toCalendar(localTimeZone).beginningOfDay.timeInMillis)
    }

    fun selectDay(dayInMs: Long) {
        MTLog.d(this, "selectDay() > dayInMs: $dayInMs")
        selectedDayInMs = dayInMs
        
        val targetCalendar = dayInMs.toCalendar(localTimeZone).beginningOfDay
        
        dayViews.forEach { (viewDayInMs, dayView) ->
            val selectionIndicator = dayView.findViewById<View>(R.id.selection_indicator)
            val dayNameTextView = dayView.findViewById<TextView>(R.id.day_name)
            val dayNumberTextView = dayView.findViewById<TextView>(R.id.day_number)
            
            val viewCalendar = viewDayInMs.toCalendar(localTimeZone).beginningOfDay
            val isSelected = viewCalendar.timeInMillis == targetCalendar.timeInMillis
            
            if (isSelected) {
                selectionIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.schedule_calendar_selection))
                dayNameTextView.alpha = 1f
                dayNumberTextView.alpha = 1f
            } else {
                selectionIndicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                dayNameTextView.alpha = 0.7f
                dayNumberTextView.alpha = 0.7f
            }
        }

        // Scroll to make the selected day visible
        scrollToSelectedDay()
    }

    private fun scrollToSelectedDay() {
        selectedDayInMs?.let { selectedMs ->
            val targetCalendar = selectedMs.toCalendar(localTimeZone).beginningOfDay
            dayViews.forEachIndexed { index, (viewDayInMs, dayView) ->
                val viewCalendar = viewDayInMs.toCalendar(localTimeZone).beginningOfDay
                if (viewCalendar.timeInMillis == targetCalendar.timeInMillis) {
                    scrollView.post {
                        val scrollX = dayView.left - (scrollView.width - dayView.width) / 2
                        scrollView.smoothScrollTo(scrollX, 0)
                    }
                    return@let
                }
            }
        }
    }

    fun setOnDaySelectedListener(listener: (Long) -> Unit) {
        onDaySelected = listener
    }
}
