package org.mtransit.android.data

import android.content.Context
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.util.UIDirectionUtils

fun Trip.decorateDirection(context: Context, small: Boolean, centered: Boolean = false): CharSequence {
    return UIDirectionUtils.decorateDirection(context, this.getUIHeading(context, small), centered)
}

fun Schedule.Timestamp.decorateDirection(context: Context, small: Boolean): CharSequence {
    return UIDirectionUtils.decorateDirection(context, this.getUIHeading(context, small), false)
}