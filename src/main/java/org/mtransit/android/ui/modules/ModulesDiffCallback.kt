package org.mtransit.android.ui.modules

import androidx.recyclerview.widget.DiffUtil
import org.mtransit.android.data.AgencyProperties

object ModulesDiffCallback : DiffUtil.ItemCallback<AgencyProperties>() {

    override fun areItemsTheSame(oldItem: AgencyProperties, newItem: AgencyProperties): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AgencyProperties, newItem: AgencyProperties): Boolean {
        return oldItem == newItem
    }
}