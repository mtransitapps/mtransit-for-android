package org.mtransit.android.ui.common

import org.mtransit.android.R
import android.content.Context
import android.widget.TextView
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.ui.view.common.textAndVisibility

object UISourceLabelUtils {

    @JvmStatic
    fun setSourceLabelTextView(textView: TextView, status: POIStatus?) = setSourceLabelTextView(textView, status?.sourceLabel)

    @JvmStatic
    fun setSourceLabelTextView(textView: TextView, vararg sourceLabels: String?) {
        textView.textAndVisibility = getSourceLabelText(textView.context, *sourceLabels)
    }

    @JvmStatic
    fun getSourceLabelText(context: Context, vararg sourceLabels: String?) = sourceLabels
        .filter { it?.isNotBlank() == true }
        .takeIf { it.isNotEmpty() }
        ?.let {
            context.resources.getQuantityString(
                R.plurals.source_label_and_sources,
                it.size,
                it.joinToString()
            )
        }
}