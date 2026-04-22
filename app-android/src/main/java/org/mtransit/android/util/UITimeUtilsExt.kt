package org.mtransit.android.util

import android.content.Context
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.Schedule
import java.util.TimeZone

fun Schedule.Timestamp.formatTime(
    context: Context,
    timestampInMs: Long = this.departureT,
    realTime: Boolean = this.isRealTime,
    localTimeZone: String? = this.localTimeZone,
) = formatTimestamp(
    context = context,
    timestampInMs = timestampInMs,
    realTime = realTime,
    localTimeZone = localTimeZone
)

fun formatTimestamp(
    context: Context,
    timestampInMs: Long,
    realTime: Boolean,
    localTimeZone: String?,
) = TimeUtils.cleanNoRealTime(
    realTime,
    localTimeZone?.let {
        UITimeUtils.formatTime(context, timestampInMs, TimeZone.getTimeZone(it))
    } ?: UITimeUtils.formatTime(context, timestampInMs)
)
