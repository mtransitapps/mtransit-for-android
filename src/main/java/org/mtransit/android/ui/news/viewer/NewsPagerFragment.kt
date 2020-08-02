package org.mtransit.android.ui.news.viewer

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import org.mtransit.android.R
import org.mtransit.android.commons.BundleUtils
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.databinding.FragmentNewsPagerBinding
import org.mtransit.android.di.ServiceLocator
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.news.NewsListViewModel
import org.mtransit.android.ui.view.MTViewModelFactory
import java.util.ArrayList

class NewsPagerFragment : ABFragment(R.layout.fragment_news_pager) {

    companion object {

        private const val EXTRA_AUTHORITY = "extra_agency_authority"
        private const val EXTRA_NEWS_UUID = "extra_news_uuid"
        private const val EXTRA_COLOR_INT = "extra_color_int"
        private const val EXTRA_SUB_TITLE = "extra_subtitle"
        private const val EXTRA_FILTER_TARGET_AUTHORITIES =
            "extra_filter_target_authorities"
        private const val EXTRA_FILTER_TARGETS = "extra_filter_targets"
        private const val EXTRA_FILTER_UUIDS = "extra_filter_uuids"

        @JvmStatic
        fun newInstance(
            newsArticle: NewsArticle,
            colorInt: Int?,
            subtitle: String?,
            targetAuthorities: List<String>?,
            filterUUIDs: List<String>?,
            filterTargets: List<String>?
        ): NewsPagerFragment {
            return newInstance(
                newsArticle.authority,
                newsArticle.uUID,
                colorInt,
                subtitle,
                targetAuthorities?.let { ArrayList(it) },
                filterUUIDs?.let { ArrayList(it) },
                filterTargets?.let { ArrayList(it) }
            )
        }

        @JvmStatic
        fun newInstance(
            newsArticle: NewsArticle,
            colorInt: Int?,
            subtitle: String?,
            targetAuthorities: ArrayList<String>?,
            filterUUIDs: ArrayList<String>?,
            filterTargets: ArrayList<String>?
        ): NewsPagerFragment {
            return newInstance(
                newsArticle.authority,
                newsArticle.uUID,
                colorInt,
                subtitle,
                targetAuthorities,
                filterUUIDs,
                filterTargets
            )
        }

        @JvmStatic
        fun newInstance(
            authority: String,
            uuid: String,
            colorInt: Int?,
            subtitle: String?,
            targetAuthorities: List<String>?,
            filterUUIDs: List<String>?,
            filterTargets: List<String>?
        ): NewsPagerFragment {
            return newInstance(
                authority,
                uuid,
                colorInt,
                subtitle,
                targetAuthorities?.let { ArrayList(it) },
                filterUUIDs?.let { ArrayList(it) },
                filterTargets?.let { ArrayList(it) }
            )
        }

        @JvmStatic
        fun newInstance(
            authority: String,
            uuid: String,
            colorInt: Int?,
            subtitle: String?,
            targetAuthorities: ArrayList<String>?,
            filterUUIDs: ArrayList<String>?,
            filterTargets: ArrayList<String>?
        ): NewsPagerFragment {
            return NewsPagerFragment().apply {
                arguments = bundleOf(
                    EXTRA_AUTHORITY to authority,
                    EXTRA_NEWS_UUID to uuid,
                    EXTRA_COLOR_INT to colorInt,
                    EXTRA_SUB_TITLE to subtitle,
                    EXTRA_FILTER_TARGET_AUTHORITIES to targetAuthorities,
                    EXTRA_FILTER_TARGETS to filterTargets,
                    EXTRA_FILTER_UUIDS to filterUUIDs
                )
            }
        }

        val LOG_TAG = NewsPagerFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "NewsViewer"
    }

    override fun getLogTag() = LOG_TAG

    override fun getScreenName() = TRACKING_SCREEN_NAME

    private var subTitle: String? = null
    private var colorInt: Int? = null

    override fun getABTitle(context: Context?) =
        context?.getString(R.string.news) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = subTitle ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?): Int = colorInt ?: super.getABBgColor(context)

    private lateinit var viewPager: ViewPager2

    private var viewBinding: FragmentNewsPagerBinding? = null

    private lateinit var adapter: NewsPagerAdapter

    private val viewModel: NewsListViewModel by activityViewModels {
        MTViewModelFactory(
            ServiceLocator.newsRepository,
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onPageSelected(
            BundleUtils.getString(EXTRA_AUTHORITY, savedInstanceState, arguments),
            BundleUtils.getString(EXTRA_NEWS_UUID, savedInstanceState, arguments)
        )
        viewModel.setFilter(
            BundleUtils.getStringArrayList(
                EXTRA_FILTER_TARGET_AUTHORITIES, savedInstanceState, arguments
            ),
            BundleUtils.getStringArrayList(
                EXTRA_FILTER_UUIDS, savedInstanceState, arguments
            ),
            BundleUtils.getStringArrayList(
                EXTRA_FILTER_TARGETS, savedInstanceState, arguments
            )
        )
        this.subTitle = BundleUtils.getString(EXTRA_SUB_TITLE, savedInstanceState, arguments)
        this.colorInt = BundleUtils.getInt(EXTRA_COLOR_INT, savedInstanceState, arguments)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.filteredNewsArticles.observe(viewLifecycleOwner, Observer {
            val oldSize = adapter.size
            adapter.submitList(it)
            if (oldSize == 0) {
                val lastSelectedArticleUUID = viewModel.getSavedCurrentNewsUUID()
                lastSelectedArticleUUID?.let { uuid ->
                    selectNewsArticle(uuid)
                }
            }
        })
        viewModel.currentNewsArticleUUID.observe(viewLifecycleOwner, Observer { uuid ->
            selectNewsArticle(uuid)
        })
    }

    private fun selectNewsArticle(uuid: String) {
        val newPosition = adapter.getItemPosition(uuid)
        if (newPosition < 0 || newPosition == viewPager.currentItem) {
            return
        }
        viewPager.setCurrentItem(
            newPosition, viewPager.currentItem != 0
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNewsPagerBinding.bind(view)
        viewBinding = binding
        setupView(binding)
    }

    private fun setupView(binding: FragmentNewsPagerBinding) {
        viewPager = binding.viewpager
        viewPager.offscreenPageLimit = 1
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onPageSelected(
                    adapter.getItem(position)
                )
            }
        })
        adapter = NewsPagerAdapter(this)
        viewPager.adapter = adapter
    }

    override fun onModulesUpdated() {
        viewModel.onModulesUpdated()
    }
}