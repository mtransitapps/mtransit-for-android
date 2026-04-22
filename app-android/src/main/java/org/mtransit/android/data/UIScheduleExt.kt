package org.mtransit.android.data

import android.content.Context
import android.text.SpannableStringBuilder
import org.mtransit.android.R
import org.mtransit.android.commons.SpanUtils
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.originalDepartureDelay
import org.mtransit.android.commons.data.toStringShort
import org.mtransit.android.data.UISchedule.getRealTimeImage
import org.mtransit.android.util.UITimeUtils
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

fun SpannableStringBuilder.decorateRealTimes(
    context: Context,
    t: Schedule.Timestamp,
    fTime: String,
) = this.apply {
    if (t.isRealTime) {
        var start = fTime.indexOf(UITimeUtils.REAL_TIME_CHAR)
        while (start >= 0) {
            SpanUtils.setNN(
                this,
                start,
                start + 1,
                getRealTimeImage(context)
            )
            start = fTime.indexOf(UITimeUtils.REAL_TIME_CHAR, start + 1)
        }
    }
}

fun List<ServiceUpdate>?.findServiceUpdate(tripId: String?): ServiceUpdate? {
    tripId ?: return null
    this ?: return null
    return find { serviceUpdate -> serviceUpdate.targetTripId == tripId }
}

fun Schedule.Timestamp.getAbsoluteDepartureDiffString(context: Context, minDiffEarlyMs: Long, minDiffLateMs: Long, short: Boolean): String? =
    getAbsoluteDepartureDiffString(context, minDiffEarlyMs.milliseconds, minDiffLateMs.milliseconds, short)

fun Schedule.Timestamp.getAbsoluteDepartureDiffString(
    context: Context,
    minDiffEarly: Duration,
    minDiffLate: Duration,
    short: Boolean
): String? {
    val absDepartureDelay = originalDepartureDelay.absoluteValue
    val minDiff = if (originalDepartureDelay.isPositive()) minDiffLate else minDiffEarly
    if (absDepartureDelay <= minDiff) return null
    val absDiffMin = absDepartureDelay
        .toDouble(DurationUnit.MINUTES).roundToLong()
    return if (originalDepartureDelay.isPositive()) {
        context.getString(if (short) R.string.minutes_late_short else R.string.minutes_late, absDiffMin)
    } else {
        context.getString(if (short) R.string.minutes_early_short else R.string.minutes_early, absDiffMin)
    }
}
