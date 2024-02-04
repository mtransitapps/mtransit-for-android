package org.mtransit.android.ui.feedback

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.databinding.LayoutPoiModuleBinding

class AgenciesFeedbackAdapter(
    private val onClick: (View, String?) -> Unit,
) : ListAdapter<AgencyProperties, AgenciesFeedbackAdapter.AgencyViewHolder>(AgenciesDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgencyViewHolder {
        return AgencyViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: AgencyViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onClick)
    }

    class AgencyViewHolder private constructor(
        private val binding: LayoutPoiModuleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): AgencyViewHolder {
                val binding = LayoutPoiModuleBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return AgencyViewHolder(binding)
            }
        }

        fun bind(
            item: AgencyProperties?,
            onClick: (View, String?) -> Unit,
        ) {
            val contactUs = item?.contactUsWebForLang
            binding.apply {
                poi.apply {
                    name.text = item?.shortName
                    location.text = contactUs
                    location.isVisible = true
                    fav.isVisible = false
                }
                extra.apply {
                    moduleTypeImg.apply {
                        setImageResource(item?.getSupportedType()?.iconResId ?: 0)
                        setBackgroundColor(item?.colorInt ?: Color.BLACK)
                    }
                }
                root.setOnClickListener {
                    onClick(it, contactUs)
                }
            }
        }
    }
}

object AgenciesDiffCallback : DiffUtil.ItemCallback<AgencyProperties>() {

    override fun areItemsTheSame(oldItem: AgencyProperties, newItem: AgencyProperties): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AgencyProperties, newItem: AgencyProperties): Boolean {
        return oldItem == newItem
    }
}