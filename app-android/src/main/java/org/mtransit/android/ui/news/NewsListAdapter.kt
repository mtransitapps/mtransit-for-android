package org.mtransit.android.ui.news

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.R
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.registerReceiverCompat
import org.mtransit.android.data.AuthorityAndUuid
import org.mtransit.android.data.authorWithUserName
import org.mtransit.android.data.authorityAndUuidT
import org.mtransit.android.data.authorityT
import org.mtransit.android.data.getAuthority
import org.mtransit.android.data.getUuid
import org.mtransit.android.data.hasVideo
import org.mtransit.android.data.uuidT
import org.mtransit.android.databinding.LayoutNewListMomentSeparatorBinding
import org.mtransit.android.databinding.LayoutNewsListItemBinding
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.ui.view.common.StickyHeaderItemDecorator
import org.mtransit.android.util.UITimeUtils
import org.mtransit.android.util.UITimeUtils.TimeChangedReceiver
import java.util.Locale

class NewsListAdapter(
    private val imageManager: ImageManager,
    private val onClick: (View, News) -> Unit,
    private val minLines: Int? = null,
    private val horizontal: Boolean = false,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyHeaderItemDecorator.StickyAdapter<RecyclerView.ViewHolder>,
    MTLog.Loggable {

    companion object {
        private val LOG_TAG = NewsListAdapter::class.java.simpleName

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(ITEM_VIEW_TYPE_MOMENT_SEPARATORS, ITEM_VIEW_TYPE_NEWS)
        annotation class NewsItemViewType

        private const val ITEM_VIEW_TYPE_MOMENT_SEPARATORS = 0
        private const val ITEM_VIEW_TYPE_NEWS = 1

        private val dayDateFormat by lazy { ThreadSafeDateFormatter("EEE, MMM d, yyyy", Locale.getDefault()) }

        private fun formatMoment(
            context: Context?,
            timeMs: Long,
        ): CharSequence {
            if (context == null) {
                return dayDateFormat.formatThreadSafe(UITimeUtils.getNewCalendar(timeMs).time)
            }
            return UITimeUtils.getNearRelativeDay(
                context,
                timeMs,
                dayDateFormat.formatThreadSafe(UITimeUtils.getNewCalendar(timeMs).time)
            )
        }
    }

    override fun getLogTag(): String = LOG_TAG

    private val hasSeparator = !horizontal

    private val momentToNewsList = mutableListOf<Pair<Long, MutableList<News>>>()

    private val timeChangedReceiver = TimeChangedReceiver { resetNowToTheMinute() }

    private var timeChangedReceiverEnabled = false

    private var selectedArticleAuthorityAndUuid: AuthorityAndUuid? = null

    private var timeFormatter: ThreadSafeDateFormatter? = null

    private fun getTimeFormatter(context: Context): ThreadSafeDateFormatter {
        return timeFormatter ?: UITimeUtils.getNewFormatTime(context).also { timeFormatter = it }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun resetNowToTheMinute() {
        notifyDataSetChanged()
    }

    private fun enableTimeChangedReceiver(context: IContext) {
        if (!timeChangedReceiverEnabled) {
            context.context?.registerReceiverCompat(timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER, RECEIVER_NOT_EXPORTED)
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

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newsList: List<News>?) {
        this.momentToNewsList.clear()
        val originalTimeCount = itemCount
        if (newsList == null) {
            if (originalTimeCount > 0) {
                notifyDataSetChanged()
            }
            return
        }
        var currentMoment: CharSequence? = null
        var currentMomentMs: Long
        var momentToNewsList: Pair<Long, MutableList<News>>? = null
        newsList.forEach { news ->
            val newsMomentMs = news.createdAtInMs
            val newsMoment = formatMoment(null, newsMomentMs)
            if (currentMoment != newsMoment) {
                currentMomentMs = newsMomentMs
                currentMoment = newsMoment
                momentToNewsList = this.momentToNewsList.firstOrNull { (momentMs, _) -> momentMs == currentMomentMs }
                    ?: Pair<Long, MutableList<News>>(currentMomentMs, mutableListOf()).also {
                        this.momentToNewsList.add(it)
                    }
            }
            momentToNewsList?.second?.add(news)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return this.momentToNewsList.sumOf { (_, newsList) -> (if (hasSeparator) 1 else 0) + newsList.size }
    }

    @NewsItemViewType
    override fun getItemViewType(position: Int): Int {
        var index = 0
        this.momentToNewsList.forEach { (_, newsList) ->
            if (hasSeparator) {
                if (position == index) {
                    return ITEM_VIEW_TYPE_MOMENT_SEPARATORS
                }
                index++ // moment separator
            }
            if (position >= index && position < index + newsList.size) {
                return ITEM_VIEW_TYPE_NEWS
            }
            index += newsList.size
        }
        throw RuntimeException("View type not found at $position! (index:$index)")
    }

    override fun onCreateViewHolder(parent: ViewGroup, @NewsItemViewType viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_MOMENT_SEPARATORS -> MomentSeparatorViewHolder.from(parent)
            ITEM_VIEW_TYPE_NEWS -> NewsListItemViewHolder.from(parent)
            else -> throw RuntimeException("Unexpected view type $viewType!")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ITEM_VIEW_TYPE_MOMENT_SEPARATORS -> {
                (holder as? MomentSeparatorViewHolder)?.bind(
                    geMomentItem(position),
                )
            }

            ITEM_VIEW_TYPE_NEWS -> {
                val newsArticle = getNewsItem(position)
                val selected = isSelected(newsArticle)
                (holder as? NewsListItemViewHolder)?.bind(
                    imageManager, position, itemCount, newsArticle, selected, minLines, horizontal,
                    getTimeFormatter(holder.context),
                    onClick
                )
            }

            else -> throw RuntimeException("Unexpected view to bind $position!")
        }
    }

    private fun geMomentItem(position: Int): Long? {
        var index = 0
        this.momentToNewsList.forEach { (moment, newsList) ->
            if (hasSeparator) {
                if (position == index) {
                    return moment
                }
                index++ // moment separator
            }
            index += newsList.size
        }
        return null
    }


    private fun getNewsItem(position: Int): News? {
        var index = 0
        this.momentToNewsList.forEach { (_, newsList) ->
            if (hasSeparator) {
                index++ // moment separator
            }
            if (position >= index && position < index + newsList.size) {
                return newsList[position - index]
            }
            index += newsList.size
        }
        return null
    }

    fun setSelectedArticle(newAuthorityAndUuid: AuthorityAndUuid?) {
        if (this.selectedArticleAuthorityAndUuid == newAuthorityAndUuid
        ) {
            return // SKIP
        }
        val oldAuthorityAndUuid = this.selectedArticleAuthorityAndUuid
        this.selectedArticleAuthorityAndUuid = newAuthorityAndUuid
        getNewsItemPosition(newAuthorityAndUuid)?.let {
            notifyItemChanged(it)
        }
        getNewsItemPosition(oldAuthorityAndUuid)?.let {
            notifyItemChanged(it)
        }
    }

    private fun isSelected(newsArticle: News?): Boolean {
        return newsArticle?.let { isSelected(it.authorityAndUuidT) } ?: false
    }

    private fun isSelected(authorityAndUuid: AuthorityAndUuid?): Boolean {
        return this.selectedArticleAuthorityAndUuid == authorityAndUuid
    }

    fun getNewsItemPosition(authorityAndUuid: AuthorityAndUuid?): Int? {
        authorityAndUuid?.let {
            var index = 0
            this.momentToNewsList.forEach { (_, newsList) ->
                if (hasSeparator) {
                    index++ // moment separator
                }
                newsList.indexOfFirst {
                    it.authorityT == authorityAndUuid.getAuthority()
                            && it.uuidT == authorityAndUuid.getUuid()
                }.takeIf { it >= 0 }?.let {
                    return index + it
                }
                index += newsList.size
            }
        }
        MTLog.d(this, "getItemPosition() > No news article for '$authorityAndUuid'!")
        return null
    }

    // region sticky header

    override fun onCreateHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return MomentSeparatorViewHolder.from(parent)
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, headerPosition: Int) {
        (holder as? MomentSeparatorViewHolder)?.bind(
            geMomentItem(headerPosition),
        )
    }

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        if (!hasSeparator) {
            throw RuntimeException("Header ID disabled in non vertical!")
        }
        var index = 0
        this.momentToNewsList.forEach { (_, newsList) ->
            val momentPosition = index
            val startIndex = index
            index++ // moment separator
            index += newsList.size
            val endIndex = index - 1
            if (itemPosition in (startIndex..endIndex)) {
                return momentPosition
            }
        }
        throw RuntimeException("Header ID NOT found at $itemPosition! (index:$index)")
    }

    // endregion

    private class MomentSeparatorViewHolder private constructor(
        private val binding: LayoutNewListMomentSeparatorBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): MomentSeparatorViewHolder {
                val binding = LayoutNewListMomentSeparatorBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return MomentSeparatorViewHolder(binding)
            }
        }

        val context: Context
            get() = binding.root.context

        fun bind(
            momentInMs: Long?,
        ) {
            if (momentInMs == null) {
                binding.moment.text = null
                return
            }
            val timeSb = SpannableStringBuilder(
                formatMoment(context, momentInMs)
            )
            binding.moment.text = timeSb
        }
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

        val context: Context
            get() = binding.root.context

        fun bind(
            imageManager: ImageManager,
            position: Int,
            itemCount: Int,
            newsArticle: News?,
            articleSelected: Boolean,
            minLines: Int? = null,
            horizontal: Boolean,
            timeFormatter: ThreadSafeDateFormatter,
            onClick: (View, News) -> Unit,
        ) {
            if (newsArticle == null) {
                binding.root.isVisible = false
                return
            }
            val firstItem = position == 0
            val lastItem = position >= itemCount - 1
            binding.apply {
                author.apply {
                    text = context.getString(
                        R.string.news_shared_on_and_author_and_source,
                        newsArticle.authorWithUserName,
                        newsArticle.sourceLabel
                    )
                    setTextColor(
                        newsArticle.colorIntOrNull?.let {
                            ColorUtils.adaptColorToTheme(context, it)
                        } ?: run {
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
                thumbnail.apply {
                    isVisible = if (newsArticle.hasValidImageUrls()) {
                        imageManager.loadInto(context, newsArticle.firstValidImageUrl, this)
                        true
                    } else {
                        imageManager.clear(context, this)
                        false
                    }
                }
                thumbnailGallery.isVisible = newsArticle.imageURLsCount > 1
                        && !newsArticle.hasVideo // UI does NOT support video + images gallery
                thumbnailVideo.isVisible = newsArticle.hasVideo
                date.apply {
                    text = if (horizontal || UITimeUtils.isToday(newsArticle.createdAtInMs)) {
                        UITimeUtils.formatRelativeTime(newsArticle.createdAtInMs)
                    } else {
                        UITimeUtils.cleanNoRealTime(
                            false,
                            timeFormatter.formatThreadSafe(newsArticle.createdAtInMs)
                        )
                    }
                }
                newsText.apply {
                    minLines?.let {
                        this.minLines = it
                    }
                    text = newsArticle.text
                    setLinkTextColor(
                        newsArticle.colorIntOrNull?.let {
                            ColorUtils.adaptColorToTheme(context, it)
                            // it
                        } ?: run {
                            ColorUtils.getTextColorPrimary(context)
                        }
                    )
                }
                root.apply {
                    setOnClickListener { view ->
                        onClick(view, newsArticle)
                    }
                    setItemSelected(articleSelected)
                    isVisible = true
                    if (horizontal) {
                        val horizontalListMargin = context.resources.getDimension(R.dimen.news_article_horizontal_list_margin).toInt()
                        val horizontalItemMargin = context.resources.getDimension(R.dimen.news_article_horizontal_list_item_margin).toInt()
                        val horizontalItemMarginFirstLast = horizontalItemMargin - horizontalListMargin // negative #clipToPaddingFalse
                        updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            marginStart = if (!firstItem) horizontalItemMargin else horizontalItemMarginFirstLast
                            marginEnd = if (!lastItem) horizontalItemMargin else horizontalItemMarginFirstLast
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

        private fun setItemSelected(selected: Boolean?) {
            binding.apply {
                val isSelected = selected == true
                root.isChecked = isSelected
            }
        }
    }
}