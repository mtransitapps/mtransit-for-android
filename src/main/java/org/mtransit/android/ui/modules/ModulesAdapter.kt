package org.mtransit.android.ui.modules

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.databinding.LayoutModulesItemBinding
import org.mtransit.android.ui.modules.ModulesAdapter.ModuleViewHolder
import java.util.concurrent.TimeUnit

class ModulesAdapter :
    ListAdapter<AgencyProperties, ModuleViewHolder>(ModulesDiffCallback) {

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        return ModuleViewHolder.from(parent)
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

            private const val PKG_COMMON = "org.mtransit.android."
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: AgencyProperties?) {
            val context = binding.root.context
            binding.nameTv.text = item?.let {
                "${it.shortName} ${context.getString(it.type.shortNameResId)} v${PackageManagerUtils.getAppVersionName(context, it.pkg)}"
            }
            binding.descriptionTv.apply {
                text = item?.let {
                    "${it.pkg.substringAfter(PKG_COMMON)} (${it.availableVersionCode})"
                }
                visibility = View.VISIBLE
            }
            item?.let {
                when {
                    it.maxValidSec < 0 -> {
                        binding.statusLine1.apply {
                            text = "?"
                            setTextColor(ColorUtils.getTextColorPrimary(context))
                            typeface = Typeface.DEFAULT
                        }
                        binding.statusLine2.apply {
                            text = "UNKNOWN"
                            setTextColor(ColorUtils.getTextColorPrimary(context))
                            visibility = View.VISIBLE
                        }
                    }
                    it.maxValidSec == 0 -> {
                        binding.statusLine1.apply {
                            text = "∞"
                            setTextColor(ColorUtils.getTextColorSecondary(context))
                            typeface = Typeface.DEFAULT
                        }
                        binding.statusLine2.apply {
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
                        binding.statusLine1.apply {
                            text = "$diffInDays ${context.resources.getQuantityText(R.plurals.days_capitalized, diffInDays)}"
                            setTextColor(colorInt)
                            typeface = tf
                        }
                        binding.statusLine2.apply {
                            text = FORMAT_DATE.formatThreadSafe(maxValidMs)
                            setTextColor(colorInt)
                            visibility = View.VISIBLE
                        }
                    }
                }
                binding.status.visibility = View.VISIBLE
            } ?: run {
                binding.status.visibility = View.GONE
            }
        }
    }
}