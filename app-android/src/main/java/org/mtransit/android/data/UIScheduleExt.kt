package org.mtransit.android.data

import android.content.Context
import org.mtransit.android.R
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.ServiceUpdates
import org.mtransit.android.commons.data.originalDepartureDelay
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import androidx.core.util.Pair as androidXPair

fun UISchedule.getStatusK(
    context: Context,
    after: Long,
    minCoverageInMs: Long? = null,
    maxCoverageInMs: Long? = null,
    minCount: Int? = null,
    maxCount: Int? = null,
    serviceUpdates: ServiceUpdates? = null,
): androidXPair<CharSequence?, CharSequence?>? = this.getStatus(
    context, after, minCoverageInMs, maxCoverageInMs, minCount, maxCount, serviceUpdates
)

fun ServiceUpdates?.findTripServiceUpdate(tripId: String?): ServiceUpdate? {
    tripId ?: return null // no trip ID provided == no service update targeted to this trip
    return this?.find { serviceUpdate ->
        serviceUpdate.targetTripId == tripId
    }
}

fun ServiceUpdates?.allTripsNoService() =
    this?.any { it.targetTripId == null && it.isNoService } == true

fun Schedule.Timestamp.getDepartureLateEarlyString(context: Context, minDiffEarlyMs: Long, minDiffLateMs: Long, short: Boolean): String? =
    getDepartureLateEarlyString(context, minDiffEarlyMs.milliseconds, minDiffLateMs.milliseconds, short)

fun Schedule.Timestamp.getDepartureLateEarlyString(
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

data class DetailsNextDepartures(
    val timestampMs: Long = -1L,
    val timeText: CharSequence,
    val headSignText: CharSequence? = null,
    val dateText: CharSequence? = null,
) {
    companion object {
        @JvmStatic
        fun makeTextOnly(timeText: CharSequence) = DetailsNextDepartures(timeText = timeText)
    }
}

fun ArrayList<DetailsNextDepartures>.toCancelled() = this.mapTo(ArrayList()) {
    it.toCancelled()
}

fun DetailsNextDepartures.toCancelled() = this.copy(
    timeText = this.timeText.let { UISchedule.setCancelled(it) }
)

fun Schedule.toUI() = UISchedule(this)