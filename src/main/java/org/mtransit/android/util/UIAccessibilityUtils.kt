package org.mtransit.android.util

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import org.mtransit.android.R
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.SpanUtils
import org.mtransit.android.commons.data.Accessibility
import org.mtransit.commons.Constants.EMPTY
import org.mtransit.commons.Constants.SPACE_

object UIAccessibilityUtils {

    @ColorInt
    private var accessibleColorTint: Int? = null

    @ColorInt
    private fun getAccessibleColorTint(@Suppress("UNUSED_PARAMETER") context: Context): Int {
        return accessibleColorTint ?: Color.BLUE
            .also { accessibleColorTint = it }
    }

    @ColorInt
    private var accessibleNotColorTint: Int? = null

    @ColorInt
    private fun getAccessibleNotColorTint(context: Context): Int {
        return accessibleNotColorTint ?: ContextCompat.getColor(context, R.color.red_cf)
            .also { accessibleNotColorTint = it }
    }

    private var a11yImages = mutableMapOf<ImageFilter, ImageSpan?>()

    private fun getImage(context: Context, accessiblePossible: Boolean, size: ImageSize, alignBottom: Boolean): ImageSpan? {
        val imageFilter = ImageFilter(accessiblePossible, size, alignBottom)
        return a11yImages[imageFilter] ?: UISpanUtils.getNewImage(
            context,
            if (imageFilter.accessiblePossible) {
                when (size) {
                    ImageSize.SMALL -> R.drawable.ic_baseline_accessible_12
                    ImageSize.MEDIUM -> R.drawable.ic_baseline_accessible_16
                    ImageSize.LARGE -> R.drawable.ic_baseline_accessible_24
                }
            } else {
                when (size) {
                    ImageSize.SMALL -> R.drawable.ic_baseline_accessible_not_12
                    ImageSize.MEDIUM -> R.drawable.ic_baseline_accessible_not_16
                    ImageSize.LARGE -> R.drawable.ic_baseline_accessible_not_24
                }
            },
            if (imageFilter.accessiblePossible) getAccessibleColorTint(context) else getAccessibleNotColorTint(context),
            true,
            false,
            if (size == ImageSize.LARGE || imageFilter.alignBottom) DynamicDrawableSpan.ALIGN_BOTTOM else DynamicDrawableSpan.ALIGN_BASELINE
        ).also {
            a11yImages[imageFilter] = it
        }
    }

    @JvmStatic
    @JvmOverloads
    fun decorate(accessible: Int, appending: Boolean = false): String {
        return Accessibility.decorate(if (appending) SPACE_ else EMPTY, accessible, !appending)
    }

    @JvmStatic
    fun decorate(context: Context, decoratedCs: CharSequence, size: ImageSize, alignBottom: Boolean): CharSequence {
        val decoratedStringSb = decoratedCs as? SpannableStringBuilder ?: SpannableStringBuilder(decoratedCs)
        val possibleStartIdx = decoratedStringSb.indexOf(Accessibility.POSSIBLE_CHAR)
        if (possibleStartIdx >= 0) {
            val possibleEndIdx = possibleStartIdx + Accessibility.POSSIBLE_CHAR.length
            if (false) {
                decoratedStringSb.replace(possibleStartIdx, possibleEndIdx, HtmlUtils.fromHtmlCompact(Accessibility.HTML_POSSIBLE))
            } else {
                SpanUtils.setNN(
                    decoratedStringSb,
                    possibleStartIdx,
                    possibleEndIdx,
                    getImage(context, accessiblePossible = true, size, alignBottom)
                )
            }
        }
        val notPossibleStartIdx = decoratedStringSb.indexOf(Accessibility.NOT_POSSIBLE_CHAR)
        if (notPossibleStartIdx >= 0) {
            val notPossibleEndIdx = notPossibleStartIdx + Accessibility.NOT_POSSIBLE_CHAR.length
            SpanUtils.setNN(
                decoratedStringSb,
                notPossibleStartIdx,
                notPossibleEndIdx,
                getImage(context, accessiblePossible = false, size, alignBottom)
            )
        }
        return decoratedStringSb
    }

    data class ImageFilter(val accessiblePossible: Boolean, val size: ImageSize, val alignBottom: Boolean)

    enum class ImageSize {
        SMALL, MEDIUM, LARGE
    }
}