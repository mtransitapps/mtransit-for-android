package org.mtransit.android.ui.news.pager

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.data.AuthorityAndUuid
import org.mtransit.android.data.authorityT
import org.mtransit.android.data.getAuthority
import org.mtransit.android.data.getUuid
import org.mtransit.android.data.uuidT
import org.mtransit.android.ui.news.details.NewsDetailsFragment

class NewsPagerAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment), MTLog.Loggable {

    companion object {
        private val LOG_TAG = NewsPagerAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private var items: List<News> = emptyList()

    override fun getItemCount() = items.size

    override fun createFragment(position: Int): Fragment {
        return getItem(position)?.let { newsArticle ->
            NewsDetailsFragment.newInstance(newsArticle)
        } ?: run {
            throw RuntimeException("No news article at position $position!")
        }
    }

    fun getItem(position: Int): News? {
        if (items.size < position) {
            MTLog.w(this, "getItem() > No item for $position (item:${items.size})!")
        }
        return this.items.getOrNull(position)
    }

    fun getItemPosition(authorityAndUuid: AuthorityAndUuid?): Int? {
        return authorityAndUuid?.let {
            items.indexOfFirst {
                it.authorityT == authorityAndUuid.getAuthority()
                        && it.uuidT == authorityAndUuid.getUuid()
            }.takeIf { it >= 0 }
        } ?: run {
            MTLog.w(this, "getItemPosition() > No news article for '$authorityAndUuid'!")
            null
        }
    }

    private fun createIdSnapshot(): List<Long> =
        (0 until itemCount).map { position -> getItemId(position) }

    val size: Int get() = items.size

    fun submitList(newItems: List<News>?) {
        val idsOld = createIdSnapshot()
        items = newItems ?: emptyList()
        val idsNew = createIdSnapshot()
        DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = idsOld.size
                override fun getNewListSize(): Int = idsNew.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    idsOld[oldItemPosition] == idsNew[newItemPosition]

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    areItemsTheSame(oldItemPosition, newItemPosition)
            },
            true
        ).dispatchUpdatesTo(this@NewsPagerAdapter)
    }
}