package org.mtransit.android.ui.common

import android.content.Context
import android.widget.TextView
import androidx.core.util.PatternsCompat
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.distinctByOriginalId
import org.mtransit.android.commons.data.readFromSource
import org.mtransit.android.commons.toMillis
import org.mtransit.android.ui.view.common.textAndVisibility
import org.mtransit.android.util.UITimeUtils
import kotlin.time.Instant

object UISourceLabelUtils : MTLog.Loggable {

    private val LOG_TAG: String = UISourceLabelUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    @JvmStatic
    fun setSourceLabelTextView(textView: TextView, serviceUpdates: List<ServiceUpdate>?) {
        serviceUpdates
            ?.distinctByOriginalId()
            ?.filter { it.shouldDisplay() }
            ?.mapNotNull {
                it.sourceLabel.takeIf { label ->
                    @Suppress("UselessCallOnNotNull") // sourceLabel is Java field not 100% NonNull?
                    !label.isNullOrBlank() && isUrl(label)
                }
            }
            ?.let {
                setSourceLabelTextView(textView, readFromSource = null, *it.toTypedArray())
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
    fun setSourceLabelTextView(textView: TextView, status: POIStatus?) = setSourceLabelTextView(textView, status?.readFromSource, status?.sourceLabel)

    @JvmStatic
    fun setSourceLabelTextView(textView: TextView, readFromSource: Instant? = null, vararg sourceLabels: String?) {
        textView.textAndVisibility = getSourceLabelText(textView.context, readFromSource, *sourceLabels)
    }

    @JvmStatic
    fun getSourceLabelText(context: Context, readFromSource: Instant? = null, vararg sourceLabels: String?) = sourceLabels
        .filter { it?.isNotBlank() == true }
        .takeIf { it.isNotEmpty() }
        ?.distinct()
        ?.let { sourceLabels ->
            readFromSource?.let {
                context.resources.getQuantityString(
                    R.plurals.source_label_and_sources_and_time,
                    sourceLabels.size,
                    sourceLabels.joinToString(),
                    UITimeUtils.formatRelativeTime(it.toMillis())
                )
            } ?: run {
                context.resources.getQuantityString(
                    R.plurals.source_label_and_sources,
                    sourceLabels.size,
                    sourceLabels.joinToString()
                )
            }
        }
}