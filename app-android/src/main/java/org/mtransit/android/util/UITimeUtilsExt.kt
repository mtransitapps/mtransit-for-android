package org.mtransit.android.util

import android.content.Context
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.Schedule
import java.util.TimeZone

@JvmOverloads
fun Schedule.Timestamp.formatTimestamp(
    context: Context,
    timestampInMs: Long = this.departureT,
    realTime: Boolean = this.isRealTime,
    localTimeZoneId: String? = this.localTimeZoneId,
) = formatTime(
    context = context,
    timestampInMs = timestampInMs,
    realTime = realTime,
    localTimeZoneId = localTimeZoneId
)

@JvmOverloads
fun formatTime(
    context: Context,
    timestampInMs: Long,
    localTimeZoneId: String?,
    realTime: Boolean = false,
) = formatTime(
    context = context,
    timestampInMs = timestampInMs,
    localTimeZone = localTimeZoneId?.let { TimeZone.getTimeZone(it) },
    realTime = realTime
)

@JvmOverloads
fun formatTime(
    context: Context,
    timestampInMs: Long,
    localTimeZone: TimeZone?,
    realTime: Boolean = false,
) = TimeUtils.cleanNoRealTime(
    realTime,
    localTimeZone?.let { UITimeUtils.formatTime(context, timestampInMs, localTimeZone) }
        ?: UITimeUtils.formatTime(context, timestampInMs)
)
