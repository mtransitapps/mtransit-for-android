package org.mtransit.android.ui.type.rts

import androidx.recyclerview.widget.DiffUtil
import org.mtransit.android.commons.data.Route
import org.mtransit.android.data.AgencyProperties

object RoutesDiffCallback : DiffUtil.ItemCallback<Route>() {

    override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem == newItem
    }
}