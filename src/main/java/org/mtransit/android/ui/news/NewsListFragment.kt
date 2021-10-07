@file:JvmName("NewsFragment") // ANALYTICS
package org.mtransit.android.ui.news

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.databinding.FragmentNewsListBinding
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.main.MainViewModel
import org.mtransit.android.ui.news.details.NewsDetailsFragment
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class NewsListFragment : ABFragment(R.layout.fragment_news_list) {

    companion object {
        private val LOG_TAG = NewsListFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "News"

        @JvmOverloads
        @JvmStatic
        fun newInstance(
            optColorInt: Int? = null,
            subtitle: String? = null,
            targetAuthorities: List<String>? = null,
            filterTargets: List<String>? = null,
            filterUUIDs: List<String>? = null, // always null
        ) = newInstance(
            optColorInt?.let { ColorUtils.toRGBColor(it) },
            subtitle,
            targetAuthorities?.toTypedArray(),
            filterTargets?.toTypedArray(),
            filterUUIDs?.toTypedArray(),
        )

        fun newInstance(
            color: String? = null,
            subtitle: String? = null,
            targetAuthorities: Array<String>? = null,
            filterTargets: Array<String>? = null,
            filterUUIDs: Array<String>? = null, // always null
        ): NewsListFragment {
            return NewsListFragment().apply {
                arguments = newInstanceArgs(color, subtitle, targetAuthorities, filterTargets, filterUUIDs)
            }
        }

        @JvmOverloads
        @JvmStatic
        fun newInstanceArgs(
            optColorInt: Int? = null,
            subtitle: String? = null,
            targetAuthorities: List<String>? = null,
            filterTargets: List<String>? = null,
            filterUUIDs: List<String>? = null, // always null
        ) = newInstanceArgs(
            optColorInt?.let { ColorUtils.toRGBColor(it) },
            subtitle,
            targetAuthorities?.toTypedArray(),
            filterTargets?.toTypedArray(),
            filterUUIDs?.toTypedArray(),
        )

        @JvmStatic
        fun newInstanceArgs(
            optColor: String? = null,
            subtitle: String? = null,
            targetAuthorities: Array<String>? = null,
            filterTargets: Array<String>? = null,
            filterUUIDs: Array<String>? = null, // always null
        ): Bundle {
            return bundleOf(
                NewsListViewModel.EXTRA_COLOR to (optColor ?: NewsListViewModel.EXTRA_COLOR_DEFAULT),
                NewsListViewModel.EXTRA_SUB_TITLE to subtitle,
                NewsListViewModel.EXTRA_FILTER_TARGET_AUTHORITIES to (targetAuthorities ?: NewsListViewModel.EXTRA_FILTER_TARGET_AUTHORITIES_DEFAULT),
                NewsListViewModel.EXTRA_FILTER_TARGETS to (filterTargets ?: NewsListViewModel.EXTRA_FILTER_TARGETS_DEFAULT),
                NewsListViewModel.EXTRA_FILTER_UUIDS to (filterUUIDs ?: NewsListViewModel.EXTRA_FILTER_UUIDS_DEFAULT),
            )
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = TRACKING_SCREEN_NAME

    @Inject
    lateinit var imageManager: ImageManager

    private val viewModel by viewModels<NewsListViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null
    private val mainViewModel by activityViewModels<MainViewModel>()

    private var binding: FragmentNewsListBinding? = null

    private val listAdapter: NewsListAdapter by lazy { NewsListAdapter(this.imageManager, this::openNewsDetails, null, false) }

    private fun openNewsDetails(view: View, newsArticle: News) {
        if (FeatureFlags.F_NAVIGATION) {
            var extras: FragmentNavigator.Extras? = null
            if (FeatureFlags.F_TRANSITION) {
                extras = FragmentNavigatorExtras(view to view.transitionName)
            }
            findNavController().navigate(
                R.id.nav_to_news_detail_screen,
                NewsDetailsFragment.newInstanceArgs(newsArticle),
                null,
                extras
            )
        } else {
            (activity as? MainActivity)?.addFragmentToStack(
                NewsDetailsFragment.newInstance(newsArticle)
            )
        }
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
            if (FeatureFlags.F_NAVIGATION) {
                mainViewModel.setABSubtitle(getABSubtitle(context))
            }
        })
        viewModel.colorInt.observe(viewLifecycleOwner, {
            abController?.setABBgColor(this, getABBgColor(context), false)
            if (FeatureFlags.F_NAVIGATION) {
                mainViewModel.setABBgColor(getABBgColor(context))
            }
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
        if (FeatureFlags.F_NAVIGATION) {
            mainViewModel.scrollToTopEvent.observe(viewLifecycleOwner, EventObserver { scroll ->
                if (scroll) {
                    binding?.newsContainerLayout?.newsList?.scrollToPosition(0)
                }
            })
        }
    }

    override fun getABTitle(context: Context?) = context?.getString(R.string.news) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = attachedViewModel?.subTitle?.value ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?) = attachedViewModel?.colorInt?.value ?: super.getABBgColor(context)

    override fun onResume() {
        super.onResume()
        listAdapter.onResume(this)
        if (FeatureFlags.F_NAVIGATION) {
            mainViewModel.setABTitle(getABTitle(context))
            mainViewModel.setABSubtitle(getABSubtitle(context))
            mainViewModel.setABBgColor(getABBgColor(context))
        }
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