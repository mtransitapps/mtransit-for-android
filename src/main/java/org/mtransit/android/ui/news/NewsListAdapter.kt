package org.mtransit.android.ui.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.mtransit.android.R
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.databinding.LayoutNewsBaseBinding
import org.mtransit.android.ui.news.NewsListAdapter.NewsArticleViewHolder
import org.mtransit.android.util.UITimeUtils
import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver
import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver.TimeChangedListener

class NewsListAdapter :
    TimeChangedListener,
    ListAdapter<NewsArticle, NewsArticleViewHolder>(NewsArticleDiffCallback()) {

    private val timeChangedReceiver = TimeChangedReceiver(this)

    private var timeChangedReceiverEnabled = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsArticleViewHolder {
        return NewsArticleViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: NewsArticleViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item)
    }

    private fun resetNowToTheMinute() {
        notifyDataSetChanged()
    }

    override fun onTimeChanged() {
        resetNowToTheMinute()
    }

    private fun enableTimeChangedReceiver(iContext: IContext) {
        if (!timeChangedReceiverEnabled) {
            iContext.context?.registerReceiver(
                timeChangedReceiver,
                UITimeUtils.TIME_CHANGED_INTENT_FILTER
            )
            timeChangedReceiverEnabled = true
        }
    }

    private fun disableTimeChangedReceiver(iContext: IContext) {
        if (timeChangedReceiverEnabled) {
            iContext.context?.unregisterReceiver(timeChangedReceiver)
            timeChangedReceiverEnabled = false
        }
    }

    fun onPause(iContext: IContext) {
        disableTimeChangedReceiver(iContext)
    }

    fun onResume(iContext: IContext) {
        enableTimeChangedReceiver(iContext)
    }

    fun onDestroy(iContext: IContext) {
        disableTimeChangedReceiver(iContext)
    }

    class NewsArticleViewHolder private constructor(
        private val binding: LayoutNewsBaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): NewsArticleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = LayoutNewsBaseBinding.inflate(layoutInflater, parent, false)
                return NewsArticleViewHolder(binding)
            }
        }

        fun bind(news: NewsArticle) {
            val context = itemView.context
            if (news.hasValidImageUrls) {
                Glide.with(context)
                    .load(news.firstValidImageUrl)
                    .into(binding.thumbnail)
                binding.thumbnail.visibility = View.VISIBLE
            } else {
                Glide.with(context)
                    .clear(binding.thumbnail)
                binding.thumbnail.visibility = View.GONE
            }
            binding.author.text = context.getString(
                R.string.news_shared_on_and_author_and_source,
                news.authorOneLine,
                news.sourceLabel
            )
            if (news.hasColor()) {
                binding.author.setTextColor(
                    ColorUtils.adaptColorToTheme(
                        context,
                        news.colorInt
                    )
                )
            } else {
                binding.author.setTextColor(
                    ColorUtils.getTextColorSecondary(
                        context
                    )
                )
            }
            binding.date.text = UITimeUtils.formatRelativeTime(news.createdAtInMs)
            binding.newsText.text = news.text
            if (news.hasColor()) {
                binding.newsText.setLinkTextColor(news.colorInt)
            } else {
                binding.newsText.setLinkTextColor(
                    ColorUtils.getTextColorPrimary(
                        context
                    )
                )
            }
            itemView.visibility = View.VISIBLE
        }
    }
}


/**
 * Callback for calculating the diff between two non-null items in a list.
 *
 * Used by ListAdapter to calculate the minimum number of changes between and old list and a new
 * list that's been passed to `submitList`.
 */
class NewsArticleDiffCallback : DiffUtil.ItemCallback<NewsArticle>() {
    override fun areItemsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
        return oldItem == newItem
    }
}