package org.mtransit.android.ui.type.rds

import androidx.recyclerview.widget.DiffUtil
import org.mtransit.android.commons.data.Route
import org.mtransit.android.data.RouteManager

object RoutesDiffCallback : DiffUtil.ItemCallback<RouteManager>() {

    override fun areItemsTheSame(oldItem: RouteManager, newItem: RouteManager): Boolean {
        return oldItem.route.id == newItem.route.id
                && oldItem.authority == newItem.authority
    }

    override fun areContentsTheSame(oldItem: RouteManager, newItem: RouteManager): Boolean {
        return oldItem == newItem
    }
}
