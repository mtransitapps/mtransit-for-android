package org.mtransit.android.util

import android.content.Context
import org.mtransit.android.commons.SpanUtils

object UIRouteUtils {

    private val FONT_REGULAR = SpanUtils.getNewSansSerifTypefaceSpan()
    private val FONT_CONDENSED = SpanUtils.getNewSansSerifCondensedTypefaceSpan()

    @JvmStatic
    fun decorateRouteShortName(
        @Suppress("UNUSED_PARAMETER", "unused") context: Context,
        rsn: String,
    ): CharSequence {
        return SpanUtils.setAll(
            rsn,
            getRouteShortNameFont(rsn),
        )
    }

    fun getRouteShortNameFont(rsn: String) = if (rsn.length < 4) FONT_REGULAR else FONT_CONDENSED

}