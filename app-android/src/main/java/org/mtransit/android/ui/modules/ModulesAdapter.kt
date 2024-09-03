package org.mtransit.android.ui.modules

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.isIgnoringBatteryOpt
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.databinding.LayoutModulesItemBinding
import org.mtransit.android.ui.modules.ModulesAdapter.ModuleViewHolder
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.ui.view.common.textAndVisibility
import java.util.concurrent.TimeUnit

class ModulesAdapter :
    ListAdapter<AgencyProperties, ModuleViewHolder>(ModulesDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        return ModuleViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ModuleViewHolder private constructor(
        private val binding: LayoutModulesItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ModuleViewHolder {
                val binding = LayoutModulesItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ModuleViewHolder(binding)
            }

            private val FORMAT_DATE = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM)
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: AgencyProperties?) = with(binding) {
            nameTv.apply {
                text = item?.let { it ->
                    "${
                        if (it.updateAvailable) "(UPDATE: r${it.availableVersionCode}) " else ""
                    }${it.getShortNameAndType(context)}${
                        if (context.getSystemService<PowerManager>()?.isIgnoringBatteryOpt(it.pkg) == true) " (Battery: Unrestricted)" else ""
                    }"
                }
                typeface = if (item?.updateAvailable == true) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            descriptionTv.apply {
                textAndVisibility = item?.let {
                    "${it.pkg.substringAfter(IAgencyProperties.PKG_COMMON)} r${it.versionCode} v${PackageManagerUtils.getAppVersionName(context, it.pkg)}"
                }
            }
            item?.let {
                when {
                    it.maxValidSec < 0 -> {
                        statusLine1.apply {
                            text = "?"
                            setTextColor(ColorUtils.getTextColorPrimary(context))
                            typeface = Typeface.DEFAULT
                        }
                        statusLine2.apply {
                            textAndVisibility = "UNKNOWN"
                            setTextColor(ColorUtils.getTextColorPrimary(context))
                        }
                    }

                    it.maxValidSec == 0 -> {
                        statusLine1.apply {
                            text = "∞"
                            setTextColor(ColorUtils.getTextColorSecondary(context))
                            typeface = Typeface.DEFAULT
                        }
                        statusLine2.apply {
                            textAndVisibility = "UNLIMITED"
                            setTextColor(ColorUtils.getTextColorSecondary(context))
                        }
                    }

                    else -> {
                        val diffInDays = TimeUnit.MILLISECONDS.toDays(it.maxValidMs - TimeUtils.currentTimeMillis()).toInt()
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
                            textAndVisibility = FORMAT_DATE.formatThreadSafe(it.maxValidMs)
                            setTextColor(colorInt)
                        }
                    }
                }
                status.isVisible = true
            } ?: run {
                status.isVisible = false
            }
        }
    }
}