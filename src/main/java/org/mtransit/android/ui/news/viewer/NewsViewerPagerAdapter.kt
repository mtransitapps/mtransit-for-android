package org.mtransit.android.ui.news.viewer

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.ui.news.viewer.page.NewsViewerPageFragment

class NewsViewerPagerAdapter(fragment: Fragment) :
    FragmentStateAdapter(fragment) {

    private var items: List<NewsArticle> = emptyList()

    override fun getItemCount() = items.size

    override fun createFragment(position: Int): NewsViewerPageFragment {
        return NewsViewerPageFragment.newInstance(getItem(position))
    }

    private fun itemToLong(value: NewsArticle): Long = value.id?.toLong() ?: -1L

    override fun getItemId(position: Int): Long = itemToLong(getItem(position))

    fun getItem(position: Int) = items[position]

    override fun containsItem(itemId: Long): Boolean = items.any { it.id == itemId.toInt() }

    @Suppress("unused")
    fun containsItem(newsArticle: NewsArticle): Boolean = items.any { it.id == newsArticle.id }

    fun getItemPosition(uuid: String?): Int {
        return uuid?.let {
            items.indexOfFirst { it.uUID == uuid }
        } ?: -1
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