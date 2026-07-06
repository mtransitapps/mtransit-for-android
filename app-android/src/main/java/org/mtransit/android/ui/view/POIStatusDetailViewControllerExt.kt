package org.mtransit.android.ui.view

import android.view.LayoutInflater
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleDepartureHourSeparatorBinding
import org.mtransit.android.ui.view.POIStatusDetailViewController.ScheduleStatusViewHolder
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

fun addHourSeparator(diffMs: Long, layoutInflater: LayoutInflater, scheduleStatusViewHolder: ScheduleStatusViewHolder) {
    val diff = diffMs.milliseconds
    if (diff <= 1.hours) return
    var hoursCount = (diff / 1.hours).roundToInt()
    hoursCount = min(12, hoursCount) // max 12 hours shown
    scheduleStatusViewHolder.nextDeparturesLL.addView(
        LayoutPoiDetailStatusScheduleDepartureHourSeparatorBinding.inflate(layoutInflater, scheduleStatusViewHolder.nextDeparturesLL, false).apply {
            nextDepartureHour.text = ".".repeat(hoursCount)
        }.root
    )
}