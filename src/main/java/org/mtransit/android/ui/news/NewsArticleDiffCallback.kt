package org.mtransit.android.ui.news

import androidx.recyclerview.widget.DiffUtil
import org.mtransit.android.commons.data.NewsArticle

object NewsArticleDiffCallback : DiffUtil.ItemCallback<NewsArticle>() {

    override fun areItemsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
        return oldItem == newItem
    }
}