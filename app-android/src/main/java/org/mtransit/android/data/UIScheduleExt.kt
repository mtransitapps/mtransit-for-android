package org.mtransit.android.data

import android.content.Context
import org.mtransit.android.R
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.originalDepartureDelay
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

fun Schedule.Timestamp.getAbsoluteDepartureDiffString(context: Context, minDiffSecs: Int): String? {
    if (originalDepartureDelay.absoluteValue < minDiffSecs.seconds) return null
    val absDiffMin = originalDepartureDelay.absoluteValue
        .toDouble(DurationUnit.MINUTES).roundToLong()
    return if (originalDepartureDelay.isPositive()) {
        context.getString(R.string.minutes_late, absDiffMin)
    } else {
        context.getString(R.string.minutes_early, absDiffMin)
    }
}
