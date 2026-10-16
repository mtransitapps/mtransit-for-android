package org.mtransit.android.common

import kotlin.math.round

private const val DECIMAL = 10

fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= DECIMAL }
    return round(this * multiplier) / multiplier
}
