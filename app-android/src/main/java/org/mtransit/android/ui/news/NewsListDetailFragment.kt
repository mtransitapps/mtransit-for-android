@file:JvmName("NewsFragment") // ANALYTIC
package org.mtransit.android.ui.news

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.analytics.AnalyticsManager
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.data.AuthorityAndUuid
import org.mtransit.android.data.authorityAndUuidT
import org.mtransit.android.data.getUuid
import org.mtransit.android.data.isAuthorityAndUuidValid
import org.mtransit.android.databinding.FragmentNewsListDetailsBinding
import org.mtransit.android.ui.TwoPaneOnBackPressedCallback
import org.mtransit.android.ui.applyStatusBarsInsetsEdgeToEdge
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledUI
import org.mtransit.android.ui.main.NextMainViewModel
import org.mtransit.android.ui.news.pager.NewsPagerAdapter
import org.mtransit.android.ui.setUpListEdgeToEdge
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.ui.view.common.StickyHeaderItemDecorator
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.util.FragmentUtils
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class NewsListDetailFragment : ABFragment(R.layout.fragment_news_list_details),
    ModuleDisabledAwareFragment {

    companion object {
        private val LOG_TAG = NewsListDetailFragment::class.java.simpleName

        const val TRACKING_SCREEN_NAME = "News"

        private const val BACK_STACK_NAME = "panel"

        @JvmOverloads
        @JvmStatic
        fun newInstance(
            @ColorInt optColorInt: Int? = null,
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
            @ColorInt optColorInt: Int? = null,
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
        attachedViewModel?.selectedNewsArticleAuthorityAndUUID?.value?.getUuid()?.let { "$TRACKING_SCREEN_NAME/$it" } ?: TRACKING_SCREEN_NAME

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override val viewModel by viewModels<NewsListViewModel>()
    override val attachedViewModel
        get() = if (isAttached()) viewModel else null

    override fun getContextView(): View? = this.binding?.newsContainerLayout?.newsContainerLayout ?: this.view

    private val nextMainViewModel by activityViewModels<NextMainViewModel>()

    private var binding: FragmentNewsListDetailsBinding? = null

    private val listAdapter: NewsListAdapter by lazy { NewsListAdapter(this.imageManager, this::openNewsArticleSelected, null, false) }

    private var pagerAdapter: NewsPagerAdapter? = null

    private fun makePagerAdapter() = NewsPagerAdapter(this)

    private var onBackPressedCallback: TwoPaneOnBackPressedCallback? = null

    private var initialBackStackEntryCount = 0

    private var addToBackStackCalled: Boolean? = null

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

    private var onBackStackChangedListener: FragmentManager.OnBackStackChangedListener? = null

    private fun makeOnBackStackChangedListener() = object : FragmentManager.OnBackStackChangedListener {
        override fun onBackStackChanged() {
            binding?.apply {
                activity?.apply {
                    if (addToBackStackCalled == true
                        && supportFragmentManager.backStackEntryCount == initialBackStackEntryCount
                    ) {
                        if (slidingPaneLayout.isOpen) {
                            slidingPaneLayout.closePane()
                            viewModel.cleanSelectedNewsArticle()
                        }
                    }
                }
                screenToolbarLayout.apply {
                    updateScreenToolbarNavigationIcon(screenToolbar)
                }
            }
        }
    }

    private fun openNewsArticleSelected(@Suppress("unused") view: View, newsArticle: News) {
        viewModel.onNewsArticleSelected(newsArticle.authorityAndUuidT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNewsListDetailsBinding.bind(view).apply {
            applyStatusBarsInsetsEdgeToEdge() // not drawing behind status bar
            refreshLayout.apply {
                setColorSchemeColors(
                    ThemeUtils.resolveColorAttribute(rootView.context, android.R.attr.colorAccent)
                )
                setOnRefreshListener(viewModel::onRefreshRequested)
            }
            newsContainerLayout.apply {
                newsList.apply {
                    adapter = listAdapter
                    addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                    addItemDecoration(StickyHeaderItemDecorator(listAdapter, this))
                    setUpListEdgeToEdge()
                }
            }
            viewPager.apply {
                offscreenPageLimit = 1 // only one because pre-fetching ads // TODO really? try ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
                registerOnPageChangeCallback(onPageChangeCallback)
                adapter = pagerAdapter ?: makePagerAdapter().also { pagerAdapter = it } // cannot re-use Adapter w/ ViewPager
            }
            activity?.supportFragmentManager?.addOnBackStackChangedListener(
                onBackStackChangedListener ?: makeOnBackStackChangedListener().also { onBackStackChangedListener = it }
            )
            slidingPaneLayout.apply {
                onBackPressedCallback = TwoPaneOnBackPressedCallback(
                    this,
                    onPanelHandledBackPressedCallback = { // #onBackPressed()
                        viewModel.cleanSelectedNewsArticle()
                    },
                    onPanelOpenedCallback = {
                        activity?.apply {
                            if (supportFragmentManager.backStackEntryCount <= initialBackStackEntryCount) {
                                if (FragmentUtils.isReady(this, this@NewsListDetailFragment)) {
                                    supportFragmentManager.commit {
                                        addToBackStack(BACK_STACK_NAME)
                                        addToBackStackCalled = true
                                    }
                                }
                            }
                        }
                    },
                    onPanelClosedCallback = {
                        activity?.apply {
                            if (supportFragmentManager.backStackEntryCount >= initialBackStackEntryCount) {
                                if (FragmentUtils.isReady(this, this@NewsListDetailFragment)) {
                                    supportFragmentManager.popBackStack(BACK_STACK_NAME, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                                    addToBackStackCalled = null
                                }
                            }
                        }
                    }
                ).also { onBackPressedCallbackNN ->
                    doOnLayout {
                        onBackPressedCallbackNN.isEnabled = slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen
                    }
                    requireActivity().onBackPressedDispatcher.addCallback(
                        viewLifecycleOwner,
                        onBackPressedCallbackNN,
                    )
                }
                lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED // interference with view pager horizontal swipe
            }
            setupScreenToolbar(screenToolbarLayout)
            if (UIFeatureFlags.F_APP_BAR_SCROLL_BEHAVIOR) {
                viewPager.children.find { it is RecyclerView }?.let {
                    it.isNestedScrollingEnabled = false
                }
            }
        }
        viewModel.subTitle.observe(viewLifecycleOwner) {
            abController?.setABSubtitle(this, getABSubtitle(context), false)
            binding?.screenToolbarLayout?.screenToolbar?.let { updateScreenToolbarSubtitle(it) }
            if (FeatureFlags.F_NAVIGATION) {
                nextMainViewModel.setABSubtitle(getABSubtitle(context))
            }
        }
        viewModel.colorInt.observe(viewLifecycleOwner) {
            abController?.setABBgColor(this, getABBgColor(context), true)
            binding?.screenToolbarLayout?.let { updateScreenToolbarBgColor(it) }
            if (FeatureFlags.F_NAVIGATION) {
                nextMainViewModel.setABBgColor(getABBgColor(context))
            }
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding?.refreshLayout?.isRefreshing = loading
        }
        viewModel.newsArticles.observe(viewLifecycleOwner) { newsArticles ->
            listAdapter.submitList(newsArticles)
            pagerAdapter?.let { newsPagerAdapter ->
                val oldSize = newsPagerAdapter.size
                newsPagerAdapter.submitList(newsArticles)
                if (oldSize == 0) {
                    viewModel.selectedNewsArticleAuthorityAndUUID.value?.let { authorityAndUuid ->
                        selectPagerNewsArticle(authorityAndUuid)
                        viewModel.onNewsArticleSelected(authorityAndUuid) // was selected before list had data
                    }
                    viewModel.lastReadArticleAuthorityAndUUID.value?.let {
                        val newsArticlePosition = listAdapter.getNewsItemPosition(it)
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
            binding?.newsContainerLayout?.apply {
                newsList.isVisible = !newsArticles.isNullOrEmpty()
                noNewsText.isVisible = newsArticles.isNullOrEmpty()
            }
        }
        viewModel.lastReadArticleAuthorityAndUUID.observe(viewLifecycleOwner) { authorityAndUuid ->
            authorityAndUuid?.let {
                binding?.newsContainerLayout?.newsList?.let { recyclerView ->
                    val newsArticlePosition = listAdapter.getNewsItemPosition(it)
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
        viewModel.selectedNewsArticleAuthorityAndUUID.observe(viewLifecycleOwner) { newAuthorityAndUuid ->
            if (newAuthorityAndUuid?.isAuthorityAndUuidValid() == false) {
                return@observe
            }
            (activity as? IAdScreenActivity)?.let { adManager.onResumeScreen(it) }
            listAdapter.setSelectedArticle(newAuthorityAndUuid)
            if (UIFeatureFlags.F_APP_BAR_SCROLL_BEHAVIOR) {
                if (newAuthorityAndUuid != null) {
                    binding?.screenToolbarLayout?.screenToolbarLayout?.setExpanded(true, false)
                }
            }
            analyticsManager.trackScreenView(this@NewsListDetailFragment)
            val authorityAndUuid = newAuthorityAndUuid ?: return@observe
            selectPagerNewsArticle(authorityAndUuid)
        }
        ModuleDisabledUI.onViewCreated(this)
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.scrollToTopEvent.observe(viewLifecycleOwner, EventObserver { scroll ->
                if (scroll) {
                    binding?.newsContainerLayout?.newsList?.scrollToPosition(0)
                }
            })
        }
    }

    private fun selectPagerNewsArticle(authorityAndUuid: AuthorityAndUuid) {
        val pagerAdapter = this.pagerAdapter ?: return
        binding?.apply {
            val newPosition = pagerAdapter.getItemPosition(authorityAndUuid) ?: -1
            val oldPosition = viewPager.currentItem
            if (newPosition >= 0 && newPosition != oldPosition) {
                val smoothScroll = false // always set from code (not the user)
                viewPager.setCurrentItem(newPosition, smoothScroll)
            }
            slidingPaneLayout.apply {
                if (!isOpen) {
                    openPane()
                }
            }
        }
    }

    override fun hasAds() = UIFeatureFlags.F_CUSTOM_ADS_IN_NEWS

    override fun hasToolbar() = true

    override fun getABTitle(context: Context?) = context?.getString(R.string.news) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = attachedViewModel?.subTitle?.value ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?) = attachedViewModel?.colorInt?.value ?: super.getABBgColor(context)

    override fun onResume() {
        super.onResume()
        listAdapter.onResume(this)
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.setABTitle(getABTitle(context))
            nextMainViewModel.setABSubtitle(getABSubtitle(context))
            nextMainViewModel.setABBgColor(getABBgColor(context))
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding?.apply {
            onBackPressedCallback?.isEnabled = slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen
        }
    }

    override fun onPause() {
        super.onPause()
        listAdapter.onPause(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.apply {
            initialBackStackEntryCount = supportFragmentManager.backStackEntryCount
        }
    }

    override fun onBackPressed(): Boolean {
        if (UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
            return super.onBackPressed()
        }
        binding?.apply {
            activity?.apply {
                if (supportFragmentManager.backStackEntryCount == (initialBackStackEntryCount + 1)) {
                    if (slidingPaneLayout.isOpen) {
                        slidingPaneLayout.closePane()
                        viewModel.cleanSelectedNewsArticle()
                    }
                }
            }
        }
        return super.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.apply {
            viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
            viewPager.adapter = null // cannot re-use Adapter w/ ViewPager
            onBackStackChangedListener?.let {
                activity?.supportFragmentManager?.removeOnBackStackChangedListener(it)
            }
            onBackStackChangedListener = null
            pagerAdapter = null // cannot re-use Adapter w/ ViewPager
            refreshLayout.setOnRefreshListener(null)
        }
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        listAdapter.onDestroy(this)
    }
}