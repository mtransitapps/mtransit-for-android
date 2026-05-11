package org.mtransit.android.util

import android.content.Context
import org.mtransit.android.commons.SpanUtils

object UIRouteUtils {

    private const val MIN_LENGTH_CONDENSED = 4

    private val FONT_REGULAR = SpanUtils.getNewSansSerifTypefaceSpan()
    private val FONT_CONDENSED = SpanUtils.getNewSansSerifCondensedTypefaceSpan()

    @JvmStatic
    fun decorateRouteShortName(
        @Suppress("unused") context: Context,
        rsn: String,
    ): CharSequence {
        return SpanUtils.setAll(
            rsn,
            getRouteShortNameFont(rsn)
        )
    }

    private fun isRouteShortNameCondensed(rsn: String) = rsn.length >= MIN_LENGTH_CONDENSED

    fun getRouteShortNameFont(rsn: String) = getRouteShortNameFont(isRouteShortNameCondensed(rsn))

    fun getRouteShortNameFont(condensed: Boolean) = if (condensed) FONT_CONDENSED else FONT_REGULAR

}