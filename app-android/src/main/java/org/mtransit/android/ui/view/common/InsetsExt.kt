package org.mtransit.android.ui.view.common

import androidx.core.graphics.Insets

fun insetsOf(
    left: Int = 0,
    top: Int = 0,
    right: Int = 0,
    bottom: Int = 0,
) = Insets.of(left, top, right, bottom)

val Insets.height: Int
    get() = top - bottom

val Insets.width: Int
    get() = left - right

val Insets.start: Int
    get() = left

val Insets.end: Int
    get() = right
