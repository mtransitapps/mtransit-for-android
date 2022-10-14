package org.mtransit.android.ui.news.image

import androidx.recyclerview.widget.DiffUtil
import org.mtransit.android.commons.data.News
import org.mtransit.android.data.NewsImage

object NewsImageDiffCallback : DiffUtil.ItemCallback<NewsImage>() {

    override fun areItemsTheSame(oldItem: NewsImage, newItem: NewsImage): Boolean {
        return oldItem.imageUrl == newItem.imageUrl
    }

    override fun areContentsTheSame(oldItem: NewsImage, newItem: NewsImage): Boolean {
        return oldItem == newItem
    }
}
