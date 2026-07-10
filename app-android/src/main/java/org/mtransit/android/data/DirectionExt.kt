package org.mtransit.android.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import org.mtransit.android.commons.StringUtils.EMPTY
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.util.UIDirectionUtils

fun Direction.decorateDirection(context: Context, small: Boolean, centered: Boolean = false) =
    UIDirectionUtils.decorateDirection(context, this.getUIHeading(context, small), centered)

fun Schedule.Timestamp.decorateDirection(context: Context, small: Boolean) =
    UIDirectionUtils.decorateDirection(context, this.getUIHeading(context, small), false)

fun Schedule.Timestamp.makeHeading(context: Context, optDirectionHeading: String? = null, small: Boolean): CharSequence? {
    if (!hasHeadsign()) return null
    val timestampHeading = getHeading(context)
    val directionHeading = optDirectionHeading.orEmpty()
    if (Direction.isSameHeadsign(timestampHeading, directionHeading)) return null
    return if (timestampHeading.startsWith(directionHeading)) {
        removeStartFromHeadingStarts(timestampHeading, directionHeading)
    } else {
        decorateDirection(context, small = small)
    }
}

private val newHeadsignCleanup = """^[:-]\s+""".toRegex()

@VisibleForTesting
internal fun removeStartFromHeadingStarts(heading: String, startsWith: String) =
    heading.substring(startsWith.length).trim()
        .let {
            newHeadsignCleanup.replace(it, EMPTY)
        }
