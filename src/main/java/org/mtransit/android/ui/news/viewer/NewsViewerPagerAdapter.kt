package org.mtransit.android.ui.news.viewer

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.data.DataSourceProvider
import org.mtransit.android.di.ServiceLocator
import org.mtransit.android.ui.news.viewer.page.NewsViewerPageFragment

class NewsViewerPagerAdapter(
    fragment: Fragment,
    private val dst: DataSourceProvider = ServiceLocator.dataSourceProvider
) : FragmentStateAdapter(fragment), MTLog.Loggable {

    companion object {
        val LOG_TAG = NewsViewerPagerAdapter::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private var items: List<NewsArticle> = emptyList()

    override fun getItemCount() = items.size

    override fun createFragment(position: Int): NewsViewerPageFragment {
        val newsArticle = getItem(position)
        return NewsViewerPageFragment.newInstance(newsArticle)
    }


    private fun itemToLong(providerIndex: Int, newsArticle: NewsArticle): Long {
        return (providerIndex + 1) * 10_000_000_000L +
                (newsArticle.id?.toLong() ?: run {
                    MTLog.w(this, "itemToLong() > No ID for $newsArticle!")
                    -1L
                })
    }

    override fun getItemId(position: Int): Long {
        val newsArticle = getItem(position)
        val index = dst.allNewsProvider.indexOf(dst.getNewsProvider(newsArticle.authority))
        if (index < 0) {
            MTLog.w(this, "getItemId() > No ID for $newsArticle!")
        }
        return itemToLong(index, newsArticle)
    }

    fun getItem(position: Int): NewsArticle {
        if (items.size < position) {
            MTLog.w(this, "getItem() > No item for $position (item:${items.size})!")
        }
        return items[position]
    }

    override fun containsItem(itemId: Long): Boolean {
        return items.any { it.id == toNewsArticleId(itemId) }
    }

    private fun toNewsArticleId(itemId: Long): Int {
        return itemId.toInt()
    }

    @Suppress("unused")
    fun containsItem(newsArticle: NewsArticle): Boolean {
        return items.any { it.id == newsArticle.id }
    }

    fun getItemPosition(uuid: String?): Int {
        return uuid?.let {
            items.indexOfFirst { it.uUID == uuid }
        } ?: run {
            MTLog.w(this, "getItemPosition() > No ID for $uuid!")
            -1
        }
    }

    private fun createIdSnapshot(): List<Long> =
        (0 until itemCount).map { position -> getItemId(position) }

    val size: Int get() = items.size

    fun submitList(newItems: List<NewsArticle>) {
        val idsOld = createIdSnapshot()
        items = newItems
        val idsNew = createIdSnapshot()
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = idsOld.size
            override fun getNewListSize(): Int = idsNew.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                idsOld[oldItemPosition] == idsNew[newItemPosition]

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                areItemsTheSame(oldItemPosition, newItemPosition)
        }, true)
            .dispatchUpdatesTo(this)
    }
}