package org.mtransit.android.data

import android.content.Context
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.distinctByOriginalId

object UIServiceUpdates {

    @JvmStatic
    fun makeServiceUpdatesHTMLText(
        context: Context,
        serviceUpdates: List<ServiceUpdate>?,
    ) = buildString {
        serviceUpdates
            ?.distinctByOriginalId()
            ?.sortedWith(ServiceUpdate.HIGHER_SEVERITY_FIRST_COMPARATOR)
            ?.forEach { serviceUpdate ->
                if (!serviceUpdate.shouldDisplay()) {
                    return@forEach
                }
                if (isNotEmpty()) {
                    append(HtmlUtils.BR).append(HtmlUtils.BR)
                }
                val originalHtml = serviceUpdate.getTextHTML()
                val thisMsgFromHtml = if (serviceUpdate.isSeverityWarning) {
                    HtmlUtils.applyFontColor(originalHtml, ColorUtils.toRGBColor(ColorUtils.getTextColorPrimary(context)))
                } else {
                    HtmlUtils.applyFontColor(originalHtml, ColorUtils.toRGBColor(ColorUtils.getTextColorSecondary(context)))
                }
                append(thisMsgFromHtml)
            }
    }

    @JvmStatic
    fun hasWarnings(serviceUpdates: List<ServiceUpdate>?) =
        serviceUpdates
            ?.distinctByOriginalId()
            ?.any { it.isSeverityWarning } ?: false
}
