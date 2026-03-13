package org.mtransit.android.data

import android.content.Context
import org.mtransit.android.R
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.originalDepartureDelay
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

fun Schedule.Timestamp.getAbsoluteDepartureDiffString(context: Context, minDiffSecs: Int, short: Boolean): String? {
    val absDepartureDelay = originalDepartureDelay.absoluteValue
    if (absDepartureDelay < minDiffSecs.seconds) return null
    val absDiffMin = absDepartureDelay
        .toDouble(DurationUnit.MINUTES).roundToLong()
    return if (originalDepartureDelay.isPositive()) {
        context.getString(if (short) R.string.minutes_late_short else R.string.minutes_late, absDiffMin)
    } else {
        context.getString(if (short) R.string.minutes_early_short else R.string.minutes_early, absDiffMin)
    }
}
