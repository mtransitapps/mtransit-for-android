package org.mtransit.android.ui.news

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import org.mtransit.android.R
import org.mtransit.android.commons.BundleUtils
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.databinding.FragmentNewsListBinding
import org.mtransit.android.di.ServiceLocator
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.news.viewer.NewsViewerFragment
import org.mtransit.android.ui.view.MTViewModelFactory
import org.mtransit.android.ui.view.common.EventObserver

class NewsListFragment : ABFragment(R.layout.fragment_news_list) {

    companion object {

        private const val EXTRA_COLOR_INT = "extra_color_int"
        private const val EXTRA_SUB_TITLE = "extra_subtitle"
        private const val EXTRA_FILTER_TARGET_AUTHORITIES =
            "extra_filter_target_authorities"
        private const val EXTRA_FILTER_TARGETS = "extra_filter_targets"
        private const val EXTRA_FILTER_UUIDS = "extra_filter_uuids"

        @JvmStatic
        fun newInstance(
            colorInt: Int?,
            subtitle: String?,
            targetAuthorities: List<String>?,
            filterUUIDs: List<String>?,
            filterTargets: List<String>?
        ): NewsListFragment {
            return NewsListFragment().apply {
                arguments = bundleOf(
                    EXTRA_COLOR_INT to colorInt,
                    EXTRA_SUB_TITLE to subtitle,
                    EXTRA_FILTER_TARGET_AUTHORITIES to targetAuthorities,
                    EXTRA_FILTER_TARGETS to filterTargets,
                    EXTRA_FILTER_UUIDS to filterUUIDs
                )
            }
        }

        val LOG_TAG = NewsListFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "NewsList"
    }

    override fun getLogTag() = LOG_TAG

    override fun getScreenName() = TRACKING_SCREEN_NAME

    private val viewModel: NewsListViewModel by viewModels {
        MTViewModelFactory(
            ServiceLocator.newsRepository,
            this
        )
    }

    private val listAdapter: NewsListAdapter by lazy { NewsListAdapter(viewModel) }

    private var subTitle: String? = null
    private var colorInt: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.start(
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

    override fun getABTitle(context: Context?) =
        context?.getString(R.string.news) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = subTitle ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?): Int = colorInt ?: super.getABBgColor(context)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.empty.observe(viewLifecycleOwner, Observer {
            viewBinding?.apply {
                noNewsLayout.isVisible = it
                newsLinearLayout.isVisible = !it
            }
        })
        viewModel.dataLoading.observe(viewLifecycleOwner, Observer {
            viewBinding?.refreshLayout?.isRefreshing = it
        })
        viewModel.filteredNewsArticles.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
        })
        viewModel.openNewsArticleEvent.observe(viewLifecycleOwner, EventObserver {
            openNewsDetails(it)
        })
    }

    private fun openNewsDetails(newsArticle: NewsArticle) {
        (activity as MainActivity).addFragmentToStack(
            NewsViewerFragment.newInstance(
                newsArticle,
                this.colorInt,
                this.subTitle,
                viewModel.filter.value?.first,
                viewModel.filter.value?.second,
                viewModel.filter.value?.third
            )
        )
    }

    private var viewBinding: FragmentNewsListBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNewsListBinding.bind(view)
        viewBinding = binding
        setupView(binding)
    }

    private fun setupView(binding: FragmentNewsListBinding) {
        binding.apply {
            refreshLayout.apply {
                setColorSchemeColors(
                    ThemeUtils.resolveColorAttribute(
                        rootView.context,
                        R.attr.colorAccent
                    )
                )
                setOnRefreshListener(viewModel::refresh)
            }
            newsList.adapter = listAdapter
            newsList.addItemDecoration(
                DividerItemDecoration(
                    newsList.context,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        listAdapter.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        listAdapter.onPause(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        listAdapter.onDestroy(this)
    }

    override fun onModulesUpdated() {
        viewModel.onModulesUpdated()
    }
}