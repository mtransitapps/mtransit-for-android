package org.mtransit.android.ui.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.databinding.LayoutNewsListItemBinding
import org.mtransit.android.ui.news.NewsListAdapter.NewsArticleViewHolder
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.util.UITimeUtils
import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver
import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver.TimeChangedListener

class NewsListAdapter(
    private val listener: OnNewsArticleSelectedListener,
    private val minLines: Int? = null
) : TimeChangedListener,
    ListAdapter<NewsArticle, NewsArticleViewHolder>(NewsArticleDiffCallback) {

    private val timeChangedReceiver = TimeChangedReceiver(this)

    private var timeChangedReceiverEnabled = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsArticleViewHolder {
        return NewsArticleViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: NewsArticleViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(listener, minLines, item)
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
                val binding = LayoutNewsListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return NewsArticleViewHolder(binding)
            }
        }

        fun bind(
            listener: OnNewsArticleSelectedListener,
            minLines: Int?,
            newsArticle: NewsArticle
        ) {
            val context = itemView.context
            minLines?.let {
                binding.newsText.minLines = it
            }
            binding.authorIcon.apply {
                isVisible = if (newsArticle.hasAuthorPictureURL) {
                    ImageManager.loadInto(
                        context,
                        newsArticle.authorPictureURL,
                        this
                    )
                    true
                } else {
                    ImageManager.clear(
                        context,
                        this
                    )
                    false
                }
            }
            binding.thumbnail.apply {
                isVisible = if (newsArticle.hasValidImageUrls) {
                    ImageManager.loadInto(
                        context,
                        newsArticle.firstValidImageUrl,
                        this
                    )
                    true
                } else {
                    ImageManager.clear(
                        context,
                        this
                    )
                    false
                }
            }
            binding.author.apply {
                text = context.getString(
                    R.string.news_shared_on_and_author_and_source,
                    newsArticle.authorOneLine,
                    newsArticle.sourceLabel
                )
                setTextColor(
                    if (newsArticle.hasColor()) {
                        ColorUtils.adaptColorToTheme(
                            context,
                            newsArticle.colorInt
                        )
                    } else {
                        ColorUtils.getTextColorSecondary(
                            context
                        )
                    }
                )
            }
            binding.date.text = UITimeUtils.formatRelativeTime(newsArticle.createdAtInMs)
            binding.newsText.apply {
                text = newsArticle.text
                setLinkTextColor(
                    if (newsArticle.hasColor()) {
                        newsArticle.colorInt
                    } else {
                        ColorUtils.getTextColorPrimary(
                            context
                        )
                    }
                )
            }
            binding.root.apply {
                setOnClickListener {
                    listener.onNewsArticleSelected(newsArticle)
                }
                isVisible = true
            }
        }
    }

    interface OnNewsArticleSelectedListener {
        fun onNewsArticleSelected(newsArticle: NewsArticle)
    }
}
