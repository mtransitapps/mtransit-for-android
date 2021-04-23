package org.mtransit.android.ui.news

import androidx.recyclerview.widget.DiffUtil
import org.mtransit.android.commons.data.News

object NewsDiffCallback : DiffUtil.ItemCallback<News>() {

    override fun areItemsTheSame(oldItem: News, newItem: News): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: News, newItem: News): Boolean {
        return oldItem == newItem
    }
}
