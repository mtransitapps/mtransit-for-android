package org.mtransit.android.ui.modules

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.ui.modules.ModulesAdapter.ModuleViewHolder
import org.mtransit.android.ui.view.common.MTCommonViewHolder
import java.util.concurrent.TimeUnit

class ModulesAdapter :
    ListAdapter<AgencyProperties, ModuleViewHolder>(ModulesDiffCallback) {

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindItem(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        return ModuleViewHolder.from(parent)
    }

    class ModuleViewHolder private constructor(parent: ViewGroup) :
        MTCommonViewHolder<AgencyProperties>(parent, R.layout.layout_modules_item) {

        companion object {
            fun from(parent: ViewGroup): ModuleViewHolder {
                return ModuleViewHolder(parent)
            }

            private val FORMAT_DATE = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM)

            private const val PKG_COMMON = "org.mtransit.android."
        }

        private val name: TextView = rootView.findViewById(R.id.nameTv)
        private val description: TextView = rootView.findViewById(R.id.descriptionTv)
        private val status: View = rootView.findViewById(R.id.status)
        private val statusLine1: TextView = rootView.findViewById(R.id.status_line_1)
        private val statusLine2: TextView = rootView.findViewById(R.id.status_line_2)

        @SuppressLint("SetTextI18n")
        override fun bindItem(item: AgencyProperties?) {
            val context = rootView.context
            name.text = item?.let { "${it.shortName} ${context.getString(it.type.shortNameResId)} v${PackageManagerUtils.getAppVersionName(context, it.pkg)}" }
            description.text = item?.pkg?.substringAfter(PKG_COMMON)
            description.visibility = View.VISIBLE
            status.visibility = View.VISIBLE
            item?.let {
                when {
                    it.maxValidSec < 0 -> {
                        statusLine1.apply {
                            text = "?"
                            setTextColor(ColorUtils.getTextColorPrimary(context))
                            typeface = Typeface.DEFAULT
                        }
                        statusLine2.apply {
                            text = "UNKNOWN"
                            setTextColor(ColorUtils.getTextColorPrimary(context))
                            visibility = View.VISIBLE
                        }
                    }
                    it.maxValidSec == 0 -> {
                        statusLine1.apply {
                            text = "∞"
                            setTextColor(ColorUtils.getTextColorSecondary(context))
                            typeface = Typeface.DEFAULT
                        }
                        statusLine2.apply {
                            text = "UNLIMITED"
                            setTextColor(ColorUtils.getTextColorSecondary(context))
                            visibility = View.VISIBLE
                        }
                    }
                    else -> {
                        val maxValidMs = it.maxValidSec * 1000L
                        val diffInDays = TimeUnit.MILLISECONDS.toDays(maxValidMs - TimeUtils.currentTimeMillis()).toInt()
                        val tf: Typeface
                        val colorInt: Int
                        when {
                            diffInDays > 365 -> { // 1 YEAR-....
                                colorInt = ContextCompat.getColor(context, R.color.green_cf)
                                tf = Typeface.DEFAULT
                            }
                            diffInDays > 14 -> { // 2 WEEKS - 1 YEAR
                                colorInt = ContextCompat.getColor(context, R.color.green_cf)
                                tf = Typeface.DEFAULT_BOLD
                            }
                            diffInDays > 7 -> { // 1 WEEK - 2 WEEKS
                                colorInt = ContextCompat.getColor(context, R.color.yellow_cf)
                                tf = Typeface.DEFAULT_BOLD
                            }
                            diffInDays > 4 -> { // 4 DAYS - 7 DAYS
                                colorInt = ContextCompat.getColor(context, R.color.orange_cf)
                                tf = Typeface.DEFAULT_BOLD
                            }
                            diffInDays > 2 -> { // 2 DAYS - 4 DAYS
                                colorInt = ContextCompat.getColor(context, R.color.orange_cf)
                                tf = Typeface.DEFAULT_BOLD
                            }
                            diffInDays > 0 -> { // 0 DAYS - 2 DAYS
                                colorInt = ContextCompat.getColor(context, R.color.red_cf)
                                tf = Typeface.DEFAULT_BOLD
                            }
                            diffInDays <= 0 -> { // ... - 0 DAYS
                                colorInt = ContextCompat.getColor(context, R.color.red_cf)
                                tf = Typeface.DEFAULT_BOLD
                            }
                            else -> {
                                colorInt = ColorUtils.getTextColorPrimary(context)
                                tf = Typeface.DEFAULT
                            }
                        }
                        statusLine1.apply {
                            text = "$diffInDays ${context.resources.getQuantityText(R.plurals.days_capitalized, diffInDays)}"
                            setTextColor(colorInt)
                            typeface = tf
                        }
                        statusLine2.apply {
                            text = FORMAT_DATE.formatThreadSafe(maxValidMs)
                            setTextColor(colorInt)
                            visibility = View.VISIBLE
                        }
                    }
                }
                status.visibility = View.VISIBLE
            } ?: run {
                status.visibility = View.GONE
            }
        }
    }
}