package org.mtransit.android.ui.common

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import org.mtransit.android.commons.ColorUtils

object UIColorUtils {

    private const val TOO_DARK_TO_LIGHTEN: Double = 0.01

    const val DEFAULT_BACKGROUND_COLOR = Color.DKGRAY

    @JvmStatic
    @ColorInt
    fun adaptBackgroundColorToLightText(context: Context?, @ColorInt backgroundColor: Int): Int {
        context ?: return backgroundColor
        if (ColorUtils.isTooDark(backgroundColor)) {
            if (ColorUtils.calculateLuminance(backgroundColor) < TOO_DARK_TO_LIGHTEN) {
                return DEFAULT_BACKGROUND_COLOR
            }
            return ColorUtils.lightenColor(backgroundColor, 0.1f)
        }
        return backgroundColor
    }
}