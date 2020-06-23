package org.mtransit.android.ui.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.mtransit.android.R
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.databinding.LayoutNewsListItemBinding
import org.mtransit.android.ui.news.NewsListAdapter.NewsArticleViewHolder
import org.mtransit.android.util.UITimeUtils
import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver
import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver.TimeChangedListener

class NewsListAdapter(private val viewModel: NewsListViewModel) :
    TimeChangedListener,
    ListAdapter<NewsArticle, NewsArticleViewHolder>(NewsArticleDiffCallback()) {

    private val timeChangedReceiver = TimeChangedReceiver(this)

    private var timeChangedReceiverEnabled = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsArticleViewHolder {
        return NewsArticleViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: NewsArticleViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(viewModel, item)
    }

    private fun resetNowToTheMinute() {
        notifyDataSetChanged()
    }

    override fun onTimeChanged() {
        resetNowToTheMinute()
    }

    private fun enableTimeChangedReceiver(context: IContext) {
        if (!timeChangedReceiverEnabled) {
            context.context?.registerReceiver(
                timeChangedReceiver,
                UITimeUtils.TIME_CHANGED_INTENT_FILTER
            )
            timeChangedReceiverEnabled = true
        }
    }

    private fun disableTimeChangedReceiver(context: IContext) {
        if (timeChangedReceiverEnabled) {
            context.context?.unregisterReceiver(timeChangedReceiver)
            timeChangedReceiverEnabled = false
        }
    }

    fun onPause(context: IContext) {
        disableTimeChangedReceiver(context)
    }

    fun onResume(context: IContext) {
        enableTimeChangedReceiver(context)
    }

    fun onDestroy(context: IContext) {
        disableTimeChangedReceiver(context)
    }

    class NewsArticleViewHolder private constructor(
        private val binding: LayoutNewsListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): NewsArticleViewHolder {
                return NewsArticleViewHolder(
                    LayoutNewsListItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }

        fun bind(viewModel: NewsListViewModel, newsArticle: NewsArticle) {
            val context = itemView.context
            binding.authorIcon.apply {
                isVisible = if (newsArticle.hasAuthorPictureURL) {
                    Glide.with(context)
                        .load(newsArticle.authorPictureURL)
                        .into(this)
                    true
                } else {
                    Glide.with(context)
                        .clear(this)
                    false
                }
            }
            binding.thumbnail.apply {
                isVisible = if (newsArticle.hasValidImageUrls) {
                    Glide.with(context)
                        .load(newsArticle.firstValidImageUrl)
                        .into(this)
                    true
                } else {
                    Glide.with(context)
                        .clear(this)
                    false
                }
            }
            binding.author.apply {
                text = context.getString(
                    R.string.news_shared_on_and_author_and_source,
                    newsArticle.authorOneLine,
                    newsArticle.sourceLabel
                )
                if (newsArticle.hasColor()) {
                    setTextColor(
                        ColorUtils.adaptColorToTheme(
                            context,
                            newsArticle.colorInt
                        )
                    )
                } else {
                    setTextColor(
                        ColorUtils.getTextColorSecondary(
                            context
                        )
                    )
                }
            }
            binding.date.text = UITimeUtils.formatRelativeTime(newsArticle.createdAtInMs)
            binding.newsText.apply {
                text = newsArticle.text
                if (newsArticle.hasColor()) {
                    setLinkTextColor(newsArticle.colorInt)
                } else {
                    setLinkTextColor(
                        ColorUtils.getTextColorPrimary(
                            context
                        )
                    )
                }
            }
            binding.root.apply {
                setOnClickListener {
                    viewModel.openNews(newsArticle)
                }
                isVisible = true
            }
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