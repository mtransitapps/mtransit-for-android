package org.mtransit.android.ui.view.common

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class MTCommonViewHolder<in T>(
    parent: ViewGroup,
    layoutRes: Int,
    val rootView: View = parent.inflate(layoutRes, false)
) : RecyclerView.ViewHolder(rootView) {
    abstract fun bindItem(item: T?)
}