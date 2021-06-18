package org.mtransit.android.ui.view.common

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator
import org.mtransit.android.commons.MTLog
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.min

/**
 * Extending [com.google.android.material.tabs.TabLayoutMediator] features.
 * Added:
 * - advance smooth scroll while scrolling large number of items
 */
class MTTabLayoutMediator @JvmOverloads constructor(
    private val tabLayout: TabLayout,
    private val viewPager: ViewPager2,
    private val autoRefresh: Boolean = true,
    private val smoothScroll: Boolean = true,
    private val tabConfigurationStrategy: TabLayoutMediator.TabConfigurationStrategy
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = MTTabLayoutMediator::class.java.simpleName

        /**
         * see [ViewPager2.setCurrentItem]
         * https://issuetracker.google.com/issues/114361680
         */
        private const val MAX_SMOOTH_SCROLL_DIFF = 2
    }

    override fun getLogTag(): String = LOG_TAG


    private var adapter: RecyclerView.Adapter<*>? = null

    /**
     * Returns whether the [TabLayout] and the [ViewPager2] are linked together.
     */
    var isAttached = false
        private set
    private var onPageChangeCallback: TabLayoutOnPageChangeCallback? = null
    private var onTabSelectedListener: OnTabSelectedListener? = null
    private var pagerAdapterObserver: AdapterDataObserver? = null

    /**
     * Link the TabLayout and the ViewPager2 together.
     * Must be called after ViewPager2 has an adapter set.
     * To be called on a new instance of TabLayoutMediator or if the ViewPager2's adapter changes.
     *
     * @throws IllegalStateException If the mediator is already attached, or the ViewPager2 has no adapter.
     */
    fun attach() {
        check(!isAttached) { "TabLayoutMediator is already attached" }
        adapter = viewPager.adapter
        checkNotNull(adapter) { "TabLayoutMediator attached before ViewPager2 has an adapter" }
        isAttached = true

        // Add our custom OnPageChangeCallback to the ViewPager
        onPageChangeCallback = TabLayoutOnPageChangeCallback(tabLayout).apply {
            viewPager.registerOnPageChangeCallback(this)
        }

        // Now we'll add a tab selected listener to set ViewPager's current item
        onTabSelectedListener = ViewPagerOnTabSelectedListener(viewPager, smoothScroll).apply {
            tabLayout.addOnTabSelectedListener(this)
        }

        // Now we'll populate ourselves from the pager adapter, adding an observer if autoRefresh is enabled
        if (autoRefresh) {
            // Register our observer on the new adapter
            pagerAdapterObserver = PagerAdapterObserver().apply {
                adapter?.registerAdapterDataObserver(this)
            }
        }
        populateTabsFromPagerAdapter()

        // Now update the scroll position to match the ViewPager's current item
        tabLayout.setScrollPosition(viewPager.currentItem, 0f, true)
    }

    /**
     * Unlink the TabLayout and the ViewPager.
     * To be called on a stale TabLayoutMediator if a new one is instantiated, to prevent holding on to a view that should be garbage collected.
     * Also to be called before [attach] when a ViewPager2's adapter is changed.
     */
    fun detach() {
        if (autoRefresh && adapter != null) {
            pagerAdapterObserver?.let {
                adapter?.unregisterAdapterDataObserver(it)
            }
            pagerAdapterObserver = null
        }
        onTabSelectedListener?.let {
            tabLayout.removeOnTabSelectedListener(it)
        }
        onTabSelectedListener = null
        onPageChangeCallback?.let {
            viewPager.unregisterOnPageChangeCallback(it)
        }
        onPageChangeCallback = null
        adapter = null
        isAttached = false
    }

    fun populateTabsFromPagerAdapter() {
        tabLayout.removeAllTabs()
        if (adapter != null) {
            val adapterCount = adapter!!.itemCount
            for (i in 0 until adapterCount) {
                val tab = tabLayout.newTab()
                tabConfigurationStrategy.onConfigureTab(tab, i)
                tabLayout.addTab(tab, false)
            }
            // Make sure we reflect the currently set ViewPager item
            if (adapterCount > 0) {
                val lastItem = tabLayout.tabCount - 1
                val currItem = min(viewPager.currentItem, lastItem)
                if (currItem != tabLayout.selectedTabPosition) {
                    tabLayout.selectTab(tabLayout.getTabAt(currItem))
                }
            }
        }
    }

    /**
     * A [ViewPager2.OnPageChangeCallback] class which contains the necessary calls back to the provided [TabLayout] so that the tab position is kept in sync.
     *
     * This class stores the provided TabLayout weakly,
     * meaning that you can use [ViewPager2.registerOnPageChangeCallback] without removing the callback and not cause a leak.
     */
    private class TabLayoutOnPageChangeCallback(tabLayout: TabLayout) : OnPageChangeCallback() {

        private val tabLayoutRef: WeakReference<TabLayout> = WeakReference(tabLayout)
        private var previousScrollState = 0
        private var scrollState = 0

        init {
            reset()
        }

        override fun onPageScrollStateChanged(state: Int) {
            previousScrollState = scrollState
            scrollState = state
        }

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            val tabLayout = tabLayoutRef.get()
            if (tabLayout != null) {
                // Only update the text selection if we're not settling, or we are settling after being dragged
                val updateText = scrollState != ViewPager2.SCROLL_STATE_SETTLING || previousScrollState == ViewPager2.SCROLL_STATE_DRAGGING
                // Update the indicator if we're not settling after being idle.
                // This is caused from a setCurrentItem() call and will be handled by an animation from onPageSelected() instead.
                val updateIndicator = !(scrollState == ViewPager2.SCROLL_STATE_SETTLING && previousScrollState == ViewPager2.SCROLL_STATE_IDLE)
                tabLayout.setScrollPosition(position, positionOffset, updateText, updateIndicator)
            }
        }

        override fun onPageSelected(position: Int) {
            val tabLayout = tabLayoutRef.get()
            if (tabLayout != null && tabLayout.selectedTabPosition != position && position < tabLayout.tabCount) {
                // Select the tab, only updating the indicator if we're not being dragged/settled (since onPageScrolled will handle that).
                val updateIndicator = (scrollState == ViewPager2.SCROLL_STATE_IDLE
                        || (scrollState == ViewPager2.SCROLL_STATE_SETTLING
                        && previousScrollState == ViewPager2.SCROLL_STATE_IDLE))
                tabLayout.selectTab(tabLayout.getTabAt(position), updateIndicator)
            }
        }

        fun reset() {
            scrollState = ViewPager2.SCROLL_STATE_IDLE
            previousScrollState = scrollState
        }


    }

    /**
     * A [TabLayout.OnTabSelectedListener] class which contains the necessary calls back to the provided [ViewPager2] so that the tab position is kept in sync.
     */
    private class ViewPagerOnTabSelectedListener(
        private val viewPager: ViewPager2,
        private val smoothScroll: Boolean
    ) : OnTabSelectedListener {

        override fun onTabSelected(tab: TabLayout.Tab) {
            // MT CHANGES - START ----------
            val previewItem = viewPager.currentItem
            val item = tab.position
            if (smoothScroll
                && abs(item - previewItem) > MAX_SMOOTH_SCROLL_DIFF
            ) {
                val nearbyItem = if (item > previewItem) {
                    item - (MAX_SMOOTH_SCROLL_DIFF - 1)
                } else {
                    item + (MAX_SMOOTH_SCROLL_DIFF - 1)
                }
                viewPager.setCurrentItem(nearbyItem, false) // INSTANT from far away to nearby
                viewPager.post {
                    viewPager.setCurrentItem(item, true) // SMOOTH from nearby to item
                }
                return
            }
            // MT CHANGES - END ----------
            viewPager.setCurrentItem(item, smoothScroll)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
            // No-op
        }

        override fun onTabReselected(tab: TabLayout.Tab) {
            // No-op
        }
    }

    private inner class PagerAdapterObserver : AdapterDataObserver() {
        override fun onChanged() {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            populateTabsFromPagerAdapter()
        }
    }
}