package org.mtransit.android.data

import android.content.Context
import org.mtransit.android.R
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.originalDepartureDelay
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

fun Schedule.Timestamp.getAbsoluteDepartureDiffString(context: Context, minDiffMs: Long, short: Boolean): String? =
    getAbsoluteDepartureDiffString(context, minDiffMs.milliseconds, short)

fun Schedule.Timestamp.getAbsoluteDepartureDiffString(
    context: Context,
    minDiff: Duration,
    short: Boolean
): String? {
    val absDepartureDelay = originalDepartureDelay.absoluteValue
    if (absDepartureDelay < minDiff) return null
    val absDiffMin = absDepartureDelay
        .toDouble(DurationUnit.MINUTES).roundToLong()
    return if (originalDepartureDelay.isPositive()) {
        context.getString(if (short) R.string.minutes_late_short else R.string.minutes_late, absDiffMin)
    } else {
        context.getString(if (short) R.string.minutes_early_short else R.string.minutes_early, absDiffMin)
    }
}
