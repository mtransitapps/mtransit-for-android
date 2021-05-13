@file:JvmName("NewsFragment") // ANALYTICS
package org.mtransit.android.ui.news

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import org.mtransit.android.R
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.databinding.FragmentNewsListBinding
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.fragment.NewsDetailsFragment

class NewsListFragment : ABFragment(R.layout.fragment_news_list) {

    companion object {
        private val LOG_TAG = NewsListFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "News"

        @JvmOverloads
        @JvmStatic
        fun newInstance(
            colorInt: Int? = null,
            subtitle: String? = null,
            targetAuthorities: List<String>? = null,
            filterUUIDs: List<String>? = null, // always null
            filterTargets: List<String>? = null
        ): NewsListFragment {
            return newInstance(
                colorInt,
                subtitle,
                targetAuthorities?.let { ArrayList(it) },
                filterUUIDs?.let { ArrayList(it) },
                filterTargets?.let { ArrayList(it) }
            )
        }

        fun newInstance(
            colorInt: Int? = null,
            subtitle: String? = null,
            targetAuthorities: ArrayList<String>? = null,
            filterUUIDs: ArrayList<String>? = null, // always null
            filterTargets: ArrayList<String>? = null,
        ): NewsListFragment {
            return NewsListFragment().apply {
                arguments = bundleOf(
                    NewsListViewModel.EXTRA_COLOR_INT to colorInt,
                    NewsListViewModel.EXTRA_SUB_TITLE to subtitle,
                    NewsListViewModel.EXTRA_FILTER_TARGET_AUTHORITIES to targetAuthorities,
                    NewsListViewModel.EXTRA_FILTER_TARGETS to filterTargets,
                    NewsListViewModel.EXTRA_FILTER_UUIDS to filterUUIDs,
                )
            }
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    private val viewModel by viewModels<NewsListViewModel>()

    private var binding: FragmentNewsListBinding? = null

    private val listAdapter: NewsListAdapter by lazy { NewsListAdapter { newsArticle -> openNewsDetails(newsArticle) } }

    private fun openNewsDetails(newsArticle: News) {
        (activity as? MainActivity)?.addFragmentToStack(
            NewsDetailsFragment.newInstance(
                newsArticle
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNewsListBinding.bind(view).apply {
            refreshLayout.apply {
                setColorSchemeColors(
                    ThemeUtils.resolveColorAttribute(rootView.context, R.attr.colorAccent)
                )
                setOnRefreshListener(viewModel::onRefreshRequested)
            }
            newsContainerLayout.apply {
                newsList.adapter = listAdapter
                newsList.addItemDecoration(
                    DividerItemDecoration(
                        newsList.context,
                        DividerItemDecoration.VERTICAL
                    )
                )
            }
        }
        viewModel.subTitle.observe(viewLifecycleOwner, {
            abController?.setABSubtitle(this, getABSubtitle(context), false)
        })
        viewModel.colorInt.observe(viewLifecycleOwner, {
            abController?.setABBgColor(this, getABBgColor(context), false)
        })
        viewModel.loading.observe(viewLifecycleOwner, { loading ->
            binding?.refreshLayout?.isRefreshing = loading
        })
        viewModel.newsArticles.observe(viewLifecycleOwner, { newsArticles ->
            listAdapter.submitList(newsArticles)
            binding?.newsContainerLayout?.apply {
                newsLinearLayout.isVisible = !newsArticles.isNullOrEmpty()
                noNewsLayout.isVisible = newsArticles.isNullOrEmpty()
            }
        })
    }

    override fun getABTitle(context: Context?) = context?.getString(R.string.news) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = viewModel.subTitle.value ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?) = viewModel.colorInt.value ?: super.getABBgColor(context)

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
        binding?.refreshLayout?.setOnRefreshListener(null)
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        listAdapter.onDestroy(this)
    }
}