package org.mtransit.android.ui.news

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.core.view.MenuProvider
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.news.NewsProviderContract.Filter.Companion.toTargetsUUIDs
import org.mtransit.android.data.AuthorityAndUuid
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.authorityAndUuidT
import org.mtransit.android.data.getNewOneLineDescriptionForNews
import org.mtransit.android.data.uuid
import org.mtransit.android.databinding.FragmentNewsListDetailsBinding
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.ListDetailOnBackPressedCallback
import org.mtransit.android.ui.applyStatusBarsInsetsEdgeToEdge
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareFragment
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledUI
import org.mtransit.android.ui.main.NextMainViewModel
import org.mtransit.android.ui.news.pager.NewsPagerAdapter
import org.mtransit.android.ui.setUpListEdgeToEdge
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.ui.view.common.StickyHeaderItemDecorator
import org.mtransit.android.ui.view.common.isAttached
import org.mtransit.android.ui.view.common.observeEvent
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.commons.FeatureFlags
import javax.inject.Inject

@AndroidEntryPoint
class NewsListDetailFragment : ABFragment(R.layout.fragment_news_list_details),
    MenuProvider,
    ModuleDisabledAwareFragment {

    companion object {
        private val LOG_TAG: String = NewsListDetailFragment::class.java.simpleName

        const val TRACKING_SCREEN_NAME = "News"

        @JvmStatic
        fun newInstance() = NewsListDetailFragment().apply {
            arguments = newInstanceArgs()
        }

        @JvmStatic
        fun newInstanceArgs() = Bundle()

        @JvmStatic
        fun newInstance(
            poim: POIManager,
            dataSourcesRepository: DataSourcesRepository,
            selectedArticle: News? = null,
        ) = NewsListDetailFragment().apply {
            arguments = newInstanceArgs(
                poim = poim,
                dataSourcesRepository = dataSourcesRepository,
                selectedArticle = selectedArticle
            )
        }

        @JvmOverloads
        @JvmStatic
        fun newInstanceArgs(
            poim: POIManager,
            dataSourcesRepository: DataSourcesRepository,
            selectedArticle: News? = null,
        ) = newInstanceArgs(
            optColorInt = poim.getColor(dataSourcesRepository),
            subtitle = poim.poi.getNewOneLineDescriptionForNews(dataSourcesRepository),
            targetAuthorities = listOf(poim.poi.authority),
            filterTargetUUIDs = poim.poi.toTargetsUUIDs(),
            filterArticleUUIDs = null,
            selectedArticleAuthority = selectedArticle?.authority,
            selectedArticleUuid = selectedArticle?.uuid,
        )

        private fun newInstanceArgs(
            @ColorInt optColorInt: Int? = null,
            subtitle: String? = null,
            targetAuthorities: List<String>?,
            filterTargetUUIDs: List<String>?,
            @Suppress("SameParameterValue") filterArticleUUIDs: List<String>?, // always null
            selectedArticleAuthority: String? = null,
            selectedArticleUuid: String? = null,
        ) = Bundle().apply {
            putString(NewsListViewModel.EXTRA_COLOR, optColorInt?.let { ColorUtils.toRGBColor(it) } ?: NewsListViewModel.EXTRA_COLOR_DEFAULT)
            putString(NewsListViewModel.EXTRA_SUB_TITLE, subtitle)
            putStringArray(
                NewsListViewModel.EXTRA_FILTER_TARGET_AUTHORITIES,
                targetAuthorities?.toTypedArray() ?: NewsListViewModel.EXTRA_FILTER_TARGET_AUTHORITIES_DEFAULT
            )
            putStringArray(
                NewsListViewModel.EXTRA_FILTER_TARGETS_UUIDS,
                filterTargetUUIDs?.toTypedArray() ?: NewsListViewModel.EXTRA_FILTER_TARGETS_UUIDS_DEFAULT
            )
            putStringArray(
                NewsListViewModel.EXTRA_FILTER_ARTICLE_UUIDS,
                filterArticleUUIDs?.toTypedArray() ?: NewsListViewModel.EXTRA_FILTER_ARTICLE_UUIDS_DEFAULT
            )
            putString(NewsListViewModel.EXTRA_SELECTED_ARTICLE_AUTHORITY, selectedArticleAuthority)
            putString(NewsListViewModel.EXTRA_SELECTED_ARTICLE_UUID, selectedArticleUuid)
        }
    }

    override fun getLogTag() = LOG_TAG

    override val screenName: String
        get() = attachedViewModel?.validSelectedNewsArticleAuthorityAndUUID?.value?.uuid?.let { "$TRACKING_SCREEN_NAME/$it" }
            ?: TRACKING_SCREEN_NAME

    override val screenClass = "NewsFragment" // ANALYTICS // do not change

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var adManager: IAdManager

    @Inject
    lateinit var analyticsManager: IAnalyticsManager

    override val viewModel by viewModels<NewsListViewModel>()
    override val attachedViewModel
        get() = if (isAttached()) viewModel else null

    override fun getContextView(): View? = this.binding?.newsContainerLayout?.newsContainerLayout ?: this.view

    private val nextMainViewModel by activityViewModels<NextMainViewModel>()

    private var binding: FragmentNewsListDetailsBinding? = null

    private val listAdapter: NewsListAdapter by lazy {
        NewsListAdapter(
            imageManager = this.imageManager,
            onClick = { _: View, newsArticle: News ->
                attachedViewModel?.onNewsArticleSelected(newsArticle.authorityAndUuidT)
            },
            minLines = null,
            horizontal = false
        )
    }

    private var pagerAdapter: NewsPagerAdapter? = null

    private fun makePagerAdapter() = NewsPagerAdapter(this)

    private val fullscreenBackPressedCallback = object : OnBackPressedCallback(enabled = false) {
        override fun handleOnBackPressed() {
            handleExitFullscreen()
        }
    }

    /** like [androidx.navigation.fragment.AbstractListDetailFragment] **/
    private var listDetailOnBackPressedCallback: ListDetailOnBackPressedCallback? = null

    private val panelSlideListener = object : SlidingPaneLayout.PanelSlideListener {
        override fun onPanelSlide(panel: View, slideOffset: Float) {
            // DO NOTHING
        }

        override fun onPanelOpened(panel: View) {
            MTLog.d(this@NewsListDetailFragment, "onPanelOpened()")
        }

        override fun onPanelClosed(panel: View) {
            MTLog.d(this@NewsListDetailFragment, "onPanelClosed()")
            attachedViewModel?.cleanSelectedNewsArticle()
        }
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val pagerAdapter = pagerAdapter ?: return
            binding?.apply {
                if (slidingPaneLayout.isOpen) {
                    attachedViewModel?.onNewsArticleSelected(
                        pagerAdapter.getItem(position)?.authorityAndUuidT
                    )
                }
            }
        }
    }

    private var onBackStackChangedListener: FragmentManager.OnBackStackChangedListener? = null

    private fun makeOnBackStackChangedListener() = FragmentManager.OnBackStackChangedListener {
        binding?.apply {
            screenToolbarLayout.apply { updateScreenToolbarNavigationIcon(screenToolbar) }
        }
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
                lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED // interference with view pager horizontal swipe
                /** like [androidx.navigation.fragment.AbstractListDetailFragment.onCreateView] */
                listDetailOnBackPressedCallback = ListDetailOnBackPressedCallback(slidingPaneLayout = this)
                    .also { listDetailOnBackPressedCallback ->
                        listDetailOnBackPressedCallback.panelSlideListener = panelSlideListener
                        doOnLayout {
                            it.isEnabled = isSlideable && isOpen
                        }
                        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, listDetailOnBackPressedCallback) // 1st added = less priority
                    }
            }
            if (UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, fullscreenBackPressedCallback) // last added = top priority
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
        viewModel.abColorString.observe(viewLifecycleOwner) {
            updateABColor()
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
                    viewModel.validSelectedNewsArticleAuthorityAndUUID.value?.let { authorityAndUuid ->
                        selectPagerNewsArticle(authorityAndUuid)
                        viewModel.onNewsArticleSelected(authorityAndUuid) // was selected before list had data
                    }
                    viewModel.lastReadArticleAuthorityAndUUID.value?.let { lastReadArticleAuthorityAndUUID ->
                        binding?.apply {
                            listAdapter.getNewsItemPosition(lastReadArticleAuthorityAndUUID)?.let { newsArticlePosition ->
                                newsContainerLayout.newsList.scrollToPosition(
                                    (newsArticlePosition - 1) // show 1 more stop on top of the list
                                        .coerceAtLeast(0)
                                        .coerceAtMost(listAdapter.itemCount - 1)
                                )
                            }
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
            authorityAndUuid ?: return@observe
            binding?.apply {
                listAdapter.getNewsItemPosition(authorityAndUuid)?.let { newsArticlePosition ->
                    newsContainerLayout.newsList.scrollToPosition(
                        newsArticlePosition
                            .coerceAtLeast(0)
                            .coerceAtMost(listAdapter.itemCount - 1)
                    )
                }
            }
        }
        viewModel.validSelectedNewsArticleAuthorityAndUUID.observe(viewLifecycleOwner) { newAuthorityAndUuid ->
            listAdapter.setSelectedArticle(newAuthorityAndUuid)
            if (UIFeatureFlags.F_APP_BAR_SCROLL_BEHAVIOR) {
                if (newAuthorityAndUuid != null) {
                    binding?.screenToolbarLayout?.screenToolbarLayout?.setExpanded(true, false)
                }
            }
            if (binding?.slidingPaneLayout?.isSlideable == true) {
                (activity as? IAdScreenActivity)?.let { adManager.onResumeScreen(it) }
                analyticsManager.trackScreenView(this@NewsListDetailFragment)
            }
            newAuthorityAndUuid?.let { selectPagerNewsArticle(it) }
        }
        viewModel.fullscreenAvailable.observe(viewLifecycleOwner) {
            if (it == false) {
                viewModel.setFullscreenMode(false)
            }
            updateMenuItemsVisibility(fullscreenAvailable = it)
        }
        viewModel.fullscreen.observe(viewLifecycleOwner) { fullscreen ->
            updateMenuItemsVisibility(fullscreen = fullscreen)
        }
        viewModel.fullscreenAndAvailable.observe(viewLifecycleOwner) { fullscreenAndAvailable ->
            updateMenuItemsVisibility(fullscreenAndAvailable = fullscreenAndAvailable)
            if (UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
                fullscreenBackPressedCallback.isEnabled = fullscreenAndAvailable
            }
        }
        ModuleDisabledUI.onViewCreated(this)
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.scrollToTopEvent.observeEvent(viewLifecycleOwner) { scroll ->
                if (scroll) {
                    binding?.newsContainerLayout?.newsList?.scrollToPosition(0)
                }
            }
        }
    }

    private fun updateABColor() {
        abController?.setABBgColor(this, getABBgColor(context), true)
        updateScreenToolbarBgColor()
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.setABBgColor(getABBgColor(context))
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
            if (!slidingPaneLayout.isOpen) {
                slidingPaneLayout.openPane()
            }
        }
    }

    override fun hasAds() = UIFeatureFlags.F_CUSTOM_ADS_IN_NEWS

    override fun hasToolbar() = true

    override fun getABTitle(context: Context?) = context?.getString(R.string.news) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = attachedViewModel?.subTitle?.value ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?) =
        attachedViewModel?.abColorString?.value?.let { ColorUtils.parseColor(it) }
            ?: super.getABBgColor(context)

    override fun onResume() {
        super.onResume()
        binding?.apply { onResumeToolbar(screenToolbarLayout) }
        listAdapter.onVisible(this)
        if (FeatureFlags.F_NAVIGATION) {
            nextMainViewModel.setABTitle(getABTitle(context))
            nextMainViewModel.setABSubtitle(getABSubtitle(context))
            nextMainViewModel.setABBgColor(getABBgColor(context))
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        /** like [androidx.navigation.fragment.AbstractListDetailFragment.onViewStateRestored] */
        listDetailOnBackPressedCallback?.isEnabled = binding?.let { it.slidingPaneLayout.isSlideable && it.slidingPaneLayout.isOpen } == true
    }

    override fun onPause() {
        super.onPause()
        listAdapter.onInvisible(this)
    }

    private var fullscreenMenuItem: MenuItem? = null
    private var mainMenuSearchMenuItem: MenuItem? = null

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_news_details, menu)
        this.fullscreenMenuItem = menu.findItem(R.id.menu_fullscreen)
        this.mainMenuSearchMenuItem = menu.findItem(R.id.nav_search)
        updateMenuItemsVisibility()
    }

    private fun updateMenuItemsVisibility(
        fullscreen: Boolean? = attachedViewModel?.fullscreen?.value,
        fullscreenAvailable: Boolean? = attachedViewModel?.fullscreenAvailable?.value,
        fullscreenAndAvailable: Boolean? = attachedViewModel?.fullscreenAndAvailable?.value,
    ) {
        val fullscreenAndAvailable = fullscreenAndAvailable == true
        fullscreenMenuItem?.apply {
            setIcon(if (fullscreenAndAvailable) R.drawable.ic_baseline_fullscreen_exit_black_24dp else R.drawable.ic_baseline_fullscreen_black_24dp)
            setTitle(if (fullscreenAndAvailable) R.string.menu_action_fullscreen_exit else R.string.menu_action_fullscreen)
            isVisible = fullscreenAvailable == true && fullscreen != null
        }
        mainMenuSearchMenuItem?.isVisible = !fullscreenAndAvailable
        binding?.apply {
            screenToolbarLayout.screenToolbar.alpha = if (fullscreenAndAvailable) 0.3f else 1f
            refreshLayout.isVisible = !fullscreenAndAvailable
        }
        @SuppressLint("DeprecatedCall")
        @Suppress("DEPRECATION") // deprecated in API Level 30 (Android R) // no [easy] alternative found
        activity?.window?.decorView?.systemUiVisibility = if (fullscreenAndAvailable) View.SYSTEM_UI_FLAG_LOW_PROFILE else 0
    }

    override fun onMenuItemSelected(menuItem: MenuItem) =
        when (menuItem.itemId) {
            R.id.menu_fullscreen -> {
                analyticsManager.trackButtonClick("toolbar_fullscreen", attachedViewModel?.fullscreen?.value?.toString(), this)
                viewModel.setFullscreenMode(viewModel.fullscreen.value == false) // flip
                true // handled
            }

            else -> false // not handled
        }

    private fun handleExitFullscreen(): Boolean {
        if (viewModel.fullscreen.value != true) return false // not in fullscreen mode
        viewModel.setFullscreenMode(false)
        // return handled if fullscreen was actually available/visible // ELSE handle back/up navigation as usual
        return viewModel.fullscreenAvailable.value == true
    }

    override fun onScreenToolbarNavigationClick(v: View) {
        analyticsManager.trackButtonClick("up_icon", this)
        activity?.apply {
            if (onBackPressedDispatcher.hasEnabledCallbacks()) {
                onBackPressedDispatcher.onBackPressed()
                return // handled
            }
        }
        super.onScreenToolbarNavigationClick(v)
    }

    override fun onBackPressed(): Boolean {
        if (UIFeatureFlags.F_PREDICTIVE_BACK_GESTURE) {
            return super.onBackPressed()
        }
        activity?.apply {
            if (onBackPressedDispatcher.hasEnabledCallbacks()) {
                onBackPressedDispatcher.onBackPressed()
                return true // handled
            }
        }
        return super.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackStackChangedListener?.let { activity?.supportFragmentManager?.removeOnBackStackChangedListener(it) }
        onBackStackChangedListener = null
        pagerAdapter = null // cannot re-use Adapter w/ ViewPager
        binding?.apply {
            viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
            viewPager.adapter = null // cannot re-use Adapter w/ ViewPager
            refreshLayout.setOnRefreshListener(null)
        }
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        listAdapter.onDestroy(this)
    }
}
