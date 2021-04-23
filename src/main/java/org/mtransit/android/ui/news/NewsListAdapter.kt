package org.mtransit.android.ui.news

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.databinding.LayoutNewsBaseBinding
import org.mtransit.android.ui.news.NewsListAdapter.NewsListItemViewHolder
import org.mtransit.android.util.UITimeUtils
import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver

class NewsListAdapter(private val onClick: (News) -> Unit) : ListAdapter<News, NewsListItemViewHolder>(NewsDiffCallback), MTLog.Loggable {

    companion object {
        private val LOG_TAG = NewsListAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private val timeChangedReceiver = TimeChangedReceiver { resetNowToTheMinute() }

    private var timeChangedReceiverEnabled = false

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
        holder.bind(getItem(position), onClick)
    }

    class NewsListItemViewHolder private constructor(
        private val binding: LayoutNewsBaseBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): NewsListItemViewHolder {
                val binding = LayoutNewsBaseBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return NewsListItemViewHolder(binding)
            }
        }

        fun bind(newsArticle: News, onClick: (News) -> Unit) {
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
                date.apply {
                    text = UITimeUtils.formatRelativeTime(newsArticle.createdAtInMs)
                }
                newsText.apply {
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
                    setOnClickListener { onClick(newsArticle) }
                }
            }
        }
    }
}