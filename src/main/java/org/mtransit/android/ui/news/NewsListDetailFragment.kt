@file:JvmName("NewsFragment") // ANALYTIC
package org.mtransit.android.ui.news

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.analytics.AnalyticsManager
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.data.AuthorityAndUuid
import org.mtransit.android.data.authorityAndUuidT
import org.mtransit.android.databinding.FragmentNewsListDetailsBinding
import org.mtransit.android.ui.TwoPaneOnBackPressedCallback
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.main.MainViewModel
import org.mtransit.android.ui.news.pager.NewsPagerAdapter
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class NewsListDetailFragment : ABFragment(R.layout.fragment_news_list_details) {

    companion object {
        private val LOG_TAG = NewsListDetailFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "News"

        @JvmOverloads
        @JvmStatic
        fun newInstance(
            optColorInt: Int? = null,
            subtitle: String? = null,
            targetAuthorities: List<String>? = null,
            filterTargets: List<String>? = null,
            filterUUIDs: List<String>? = null, // always null
            selectedArticleAuthority: String? = null,
            selectedArticleUuid: String? = null,
        ) = newInstance(
            optColorInt?.let { ColorUtils.toRGBColor(it) },
            subtitle,
            targetAuthorities?.toTypedArray(),
            filterTargets?.toTypedArray(),
            filterUUIDs?.toTypedArray(),
            selectedArticleAuthority,
            selectedArticleUuid,
        )

        fun newInstance(
            color: String? = null,
            subtitle: String? = null,
            targetAuthorities: Array<String>? = null,
            filterTargets: Array<String>? = null,
            filterUUIDs: Array<String>? = null, // always null
            selectedArticleAuthority: String? = null,
            selectedArticleUuid: String? = null,
        ): NewsListDetailFragment {
            return NewsListDetailFragment().apply {
                arguments = newInstanceArgs(
                    color,
                    subtitle,
                    targetAuthorities,
                    filterTargets,
                    filterUUIDs,
                    selectedArticleAuthority,
                    selectedArticleUuid,
                )
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
            selectedArticleAuthority: String? = null,
            selectedArticleUuid: String? = null,
        ) = newInstanceArgs(
            optColorInt?.let { ColorUtils.toRGBColor(it) },
            subtitle,
            targetAuthorities?.toTypedArray(),
            filterTargets?.toTypedArray(),
            filterUUIDs?.toTypedArray(),
            selectedArticleAuthority,
            selectedArticleUuid,
        )

        @JvmStatic
        fun newInstanceArgs(
            optColor: String? = null,
            subtitle: String? = null,
            targetAuthorities: Array<String>? = null,
            filterTargets: Array<String>? = null,
            filterUUIDs: Array<String>? = null, // always null
            selectedArticleAuthority: String? = null,
            selectedArticleUuid: String? = null,
        ): Bundle {
            return bundleOf(
                NewsListViewModel.EXTRA_COLOR to (optColor ?: NewsListViewModel.EXTRA_COLOR_DEFAULT),
                NewsListViewModel.EXTRA_SUB_TITLE to subtitle,
                NewsListViewModel.EXTRA_FILTER_TARGET_AUTHORITIES to (targetAuthorities ?: NewsListViewModel.EXTRA_FILTER_TARGET_AUTHORITIES_DEFAULT),
                NewsListViewModel.EXTRA_FILTER_TARGETS to (filterTargets ?: NewsListViewModel.EXTRA_FILTER_TARGETS_DEFAULT),
                NewsListViewModel.EXTRA_FILTER_UUIDS to (filterUUIDs ?: NewsListViewModel.EXTRA_FILTER_UUIDS_DEFAULT),
                NewsListViewModel.EXTRA_SELECTED_ARTICLE_AUTHORITY to selectedArticleAuthority,
                NewsListViewModel.EXTRA_SELECTED_ARTICLE_UUID to selectedArticleUuid,
            )
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String =
        attachedViewModel?.selectedNewsArticleAuthorityAndUUID?.value?.getUuid()?.uuid?.let { "$TRACKING_SCREEN_NAME/$it" } ?: TRACKING_SCREEN_NAME

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    private val viewModel by viewModels<NewsListViewModel>()
    private val attachedViewModel
        get() = if (isAttached()) viewModel else null

    private val mainViewModel by activityViewModels<MainViewModel>()

    private var binding: FragmentNewsListDetailsBinding? = null

    private val listAdapter: NewsListAdapter by lazy { NewsListAdapter(this.imageManager, this::openNewsArticleSelected, null, false) }

    private var pagerAdapter: NewsPagerAdapter? = null

    private fun makePagerAdapter() = NewsPagerAdapter(this)

    private var onBackPressedCallback: TwoPaneOnBackPressedCallback? = null

    private var addedToBackStack: Boolean = false

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val pagerAdapter = pagerAdapter ?: return
            if (binding?.slidingPaneLayout?.isOpen == true) {
                attachedViewModel?.onNewsArticleSelected(
                    pagerAdapter.getItem(position)?.authorityAndUuidT
                )
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun openNewsArticleSelected(view: View, newsArticle: News) {
        viewModel.onNewsArticleSelected(newsArticle.authorityAndUuidT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNewsListDetailsBinding.bind(view).apply {
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
            viewPager.apply {
                offscreenPageLimit = 1
                registerOnPageChangeCallback(onPageChangeCallback)
                adapter = pagerAdapter ?: makePagerAdapter().also { pagerAdapter = it } // cannot re-use Adapter w/ ViewPager
            }
            mainActivity?.supportFragmentManager?.addOnBackStackChangedListener {
                binding?.slidingPaneLayout?.apply {
                    if (addedToBackStack) {
                        if (isOpen) {
                            closePane()
                            viewModel.cleanSelectedNewsArticle()
                        }
                        addedToBackStack = false
                    }
                }
            }
            slidingPaneLayout.let { slidingPaneLayoutNN ->
                onBackPressedCallback = TwoPaneOnBackPressedCallback(
                    slidingPaneLayoutNN,
                    onPanelHandledBackPressedCallback = {
                        viewModel.cleanSelectedNewsArticle()
                    },
                    onPanelOpenedCallback = {
                        mainActivity?.apply {
                            if (!addedToBackStack) {
                                supportFragmentManager.commit {
                                    addToBackStack("panel")
                                }
                                supportFragmentManager.executePendingTransactions()
                                addedToBackStack = true
                            }

                        }
                    },
                    onPanelClosedCallback = {
                        mainActivity?.apply {
                            supportFragmentManager.apply {
                                if (addedToBackStack) {
                                    if (popBackStackImmediate("panel", FragmentManager.POP_BACK_STACK_INCLUSIVE)) {
                                        addedToBackStack = false
                                    }
                                }
                            }
                        }
                    }
                ).also { onBackPressedCallbackNN ->
                    slidingPaneLayoutNN.doOnLayout {
                        onBackPressedCallbackNN.isEnabled = slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen
                    }
                    requireActivity().onBackPressedDispatcher.addCallback(
                        viewLifecycleOwner,
                        onBackPressedCallbackNN,
                    )
                }
                slidingPaneLayoutNN.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED // interference with view pager horizontal swipe
            }
        }
        viewModel.subTitle.observe(viewLifecycleOwner) {
            abController?.setABSubtitle(this, getABSubtitle(context), false)
            if (FeatureFlags.F_NAVIGATION) {
                mainViewModel.setABSubtitle(getABSubtitle(context))
            }
        }
        viewModel.colorInt.observe(viewLifecycleOwner) {
            abController?.setABBgColor(this, getABBgColor(context), false)
            if (FeatureFlags.F_NAVIGATION) {
                mainViewModel.setABBgColor(getABBgColor(context))
            }
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding?.refreshLayout?.isRefreshing = loading
        }
        viewModel.newsArticles.observe(viewLifecycleOwner) { newsArticles ->
            listAdapter.submitList(newsArticles)
            binding?.newsContainerLayout?.apply {
                newsLinearLayout.isVisible = !newsArticles.isNullOrEmpty()
                noNewsLayout.isVisible = newsArticles.isNullOrEmpty()
            }
            pagerAdapter?.let { newsPagerAdapter ->
                val oldSize = newsPagerAdapter.size
                newsPagerAdapter.submitList(newsArticles)
                if (oldSize == 0) {
                    viewModel.selectedNewsArticleAuthorityAndUUID.value?.let { authorityAndUuid ->
                        selectPagerNewsArticle(authorityAndUuid)
                    }
                    viewModel.lastReadArticleAuthorityAndUUID.value?.let {
                        val newsArticlePosition = listAdapter.getItemPosition(it)
                        newsArticlePosition?.let {
                            binding?.newsContainerLayout?.newsList?.scrollToPosition(
                                (newsArticlePosition - 1) // show 1 more stop on top of the list
                                    .coerceAtLeast(0)
                                    .coerceAtMost(listAdapter.itemCount - 1)
                            )
                        }
                    }
                }
            }
        }
        viewModel.lastReadArticleAuthorityAndUUID.observe(viewLifecycleOwner) { authorityAndUuid ->
            authorityAndUuid?.let {
                binding?.newsContainerLayout?.newsList?.let { recyclerView ->
                    val newsArticlePosition = listAdapter.getItemPosition(it)
                    newsArticlePosition?.let {
                        recyclerView.scrollToPosition(
                            newsArticlePosition
                                .coerceAtLeast(0)
                                .coerceAtMost(listAdapter.itemCount - 1)
                        )
                    }
                }
            }
        }
        viewModel.selectedNewsArticleAuthorityAndUUID.observe(viewLifecycleOwner) { _authorityAndUuid ->
            if (_authorityAndUuid?.isValid() == false) {
                return@observe
            }
            listAdapter.setSelectedArticle(_authorityAndUuid)
            analyticsManager.trackScreenView(this@NewsListDetailFragment, this@NewsListDetailFragment)
            val authorityAndUuid = _authorityAndUuid ?: return@observe
            selectPagerNewsArticle(authorityAndUuid)
        }
        if (FeatureFlags.F_NAVIGATION) {
            mainViewModel.scrollToTopEvent.observe(viewLifecycleOwner, EventObserver { scroll ->
                if (scroll) {
                    binding?.newsContainerLayout?.newsList?.scrollToPosition(0)
                }
            })
        }
    }

    private fun selectPagerNewsArticle(authorityAndUuid: AuthorityAndUuid) {
        val pagerAdapter = this.pagerAdapter ?: return
        binding?.apply {
            slidingPaneLayout.apply {
                if (!isOpen) {
                    open()
                }
            }
            val newPosition = pagerAdapter.getItemPosition(authorityAndUuid) ?: -1
            val oldPosition = viewPager.currentItem
            if (newPosition < 0 || newPosition == oldPosition) {
                return
            }
            val smoothScroll = false // always set from code (not the user)
            viewPager.setCurrentItem(newPosition, smoothScroll)
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

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding?.let { bindingNN ->
            onBackPressedCallback?.apply {
                isEnabled = bindingNN.slidingPaneLayout.isSlideable && bindingNN.slidingPaneLayout.isOpen
            }
        }
    }

    override fun onPause() {
        super.onPause()
        listAdapter.onPause(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.viewPager?.unregisterOnPageChangeCallback(this.onPageChangeCallback)
        binding?.viewPager?.adapter = null // cannot re-use Adapter w/ ViewPager
        pagerAdapter = null // cannot re-use Adapter w/ ViewPager
        binding?.refreshLayout?.setOnRefreshListener(null)
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        listAdapter.onDestroy(this)
    }
}