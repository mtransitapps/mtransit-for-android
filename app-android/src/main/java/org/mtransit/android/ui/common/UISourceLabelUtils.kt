package org.mtransit.android.ui.common

import android.content.Context
import android.widget.TextView
import androidx.core.util.PatternsCompat
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.ui.view.common.textAndVisibility

object UISourceLabelUtils : MTLog.Loggable {

    private val LOG_TAG: String = UISourceLabelUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    @JvmStatic
    fun setSourceLabelTextView(textView: TextView, serviceUpdates: Iterable<ServiceUpdate>?) {
        serviceUpdates
            ?.filter { it.shouldDisplay() }
            ?.mapNotNull { it.sourceLabel }
            ?.filter { it.isNotBlank() && isUrl(it) }
            ?.let {
                setSourceLabelTextView(textView, *it.toTypedArray())
            }
    }

    // TODO remove 3 months after shipping to prod (2025-03+)
    private fun isUrl(sourceLabel: String) = try {
        PatternsCompat.WEB_URL.matcher(sourceLabel).matches()
    } catch (_: Exception) {
        MTLog.d(this, "Source label '$sourceLabel' is not a valid URL. (expected w/ old sources)")
        false
    }

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
        ?.distinct()
        ?.let {
            context.resources.getQuantityString(
                R.plurals.source_label_and_sources,
                it.size,
                it.joinToString()
            )
        }
}