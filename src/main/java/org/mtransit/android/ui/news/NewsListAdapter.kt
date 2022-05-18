package org.mtransit.android.ui.news

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.databinding.LayoutNewsListItemBinding
import org.mtransit.android.ui.news.NewsListAdapter.NewsListItemViewHolder
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.util.UITimeUtils
import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver

class NewsListAdapter(
    private val imageManager: ImageManager,
    private val onClick: (View, News) -> Unit,
    private val minLines: Int? = null,
    private val horizontal: Boolean = false,
) : ListAdapter<News, NewsListItemViewHolder>(NewsDiffCallback), MTLog.Loggable {

    companion object {
        private val LOG_TAG = NewsListAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val timeChangedReceiver = TimeChangedReceiver { resetNowToTheMinute() }

    private var timeChangedReceiverEnabled = false

    @SuppressLint("NotifyDataSetChanged")
    private fun resetNowToTheMinute() {
        notifyDataSetChanged()
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

    fun onResume(context: IContext) {
        enableTimeChangedReceiver(context)
    }

    fun onPause(context: IContext) {
        disableTimeChangedReceiver(context)
    }

    fun onDestroy(context: IContext) {
        disableTimeChangedReceiver(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsListItemViewHolder {
        return NewsListItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: NewsListItemViewHolder, position: Int) {
        holder.bind(imageManager, position, itemCount, getItem(position), minLines, horizontal, onClick)
    }

    class NewsListItemViewHolder private constructor(
        private val binding: LayoutNewsListItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): NewsListItemViewHolder {
                val binding = LayoutNewsListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return NewsListItemViewHolder(binding)
            }
        }

        fun bind(
            imageManager: ImageManager,
            position: Int,
            itemCount: Int,
            newsArticle: News,
            minLines: Int? = null,
            horizontal: Boolean,
            onClick: (View, News) -> Unit,
        ) {
            val firstItem = position == 0
            val lastItem = position >= itemCount - 1
            val context = binding.root.context
            binding.apply {
                author.apply {
                    text = context.getString(
                        R.string.news_shared_on_and_author_and_source,
                        newsArticle.authorOneLine,
                        newsArticle.sourceLabel
                    )
                    setTextColor(
                        if (newsArticle.hasColor()) {
                            ColorUtils.adaptColorToTheme(context, newsArticle.colorInt)
                        } else {
                            ColorUtils.getTextColorSecondary(context)
                        }
                    )
                }
                authorIcon.apply {
                    isVisible = if (newsArticle.hasAuthorPictureURL()) {
                        imageManager.loadInto(context, newsArticle.authorPictureURL, this)
                        true
                    } else {
                        imageManager.clear(context, this)
                        false
                    }
                }
                date.apply {
                    text = UITimeUtils.formatRelativeTime(newsArticle.createdAtInMs)
                }
                newsText.apply {
                    minLines?.let {
                        this.minLines = it
                    }
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
                root.apply {
                    setOnClickListener { view ->
                        onClick(view, newsArticle)
                    }
                    isVisible = true
                    if (horizontal) {
                        val horizontalListMargin = context.resources.getDimension(R.dimen.news_article_horizontal_list_margin).toInt()
                        val horizontalItemMargin = context.resources.getDimension(R.dimen.news_article_horizontal_list_item_margin).toInt()
                        val horizontalItemMarginFirstLast = horizontalItemMargin - horizontalListMargin // negative #clipToPaddingFalse
                        updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            rightMargin = if (!lastItem) horizontalItemMargin else horizontalItemMarginFirstLast
                            leftMargin = if (!firstItem) horizontalItemMargin else horizontalItemMarginFirstLast
                        }
                        layout.apply {
                            val horizontalItemPadding = context.resources.getDimension(R.dimen.news_article_horizontal_list_item_padding).toInt()
                            setPadding(horizontalItemPadding)
                        }
                        val horizontalItemElevation = context.resources.getDimension(R.dimen.news_article_horizontal_list_item_elevation)
                        cardElevation = horizontalItemElevation
                    }
                }
            }
        }
    }
}