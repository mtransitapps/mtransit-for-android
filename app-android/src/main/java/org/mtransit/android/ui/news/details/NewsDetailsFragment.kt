@file:JvmName("NewsDetailsFragment") // ANALYTICS
package org.mtransit.android.ui.news.details

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.webkit.WebViewClientCompat
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.ad.IAdScreenFragment
import org.mtransit.android.ad.inlinebanner.InlineBannerAdManager
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.data.AuthorityAndUuid
import org.mtransit.android.data.NewsImage
import org.mtransit.android.data.getAuthority
import org.mtransit.android.data.getTwitterVideoId
import org.mtransit.android.data.getUuid
import org.mtransit.android.data.getYouTubeVideoId
import org.mtransit.android.data.hasImagesOrVideoThumbnail
import org.mtransit.android.data.imageUrls
import org.mtransit.android.data.isAuthorityAndUuidValid
import org.mtransit.android.data.isTwitterVideo
import org.mtransit.android.data.isYouTubeVideo
import org.mtransit.android.data.makeTwitterEmbedVideoPlayerUrl
import org.mtransit.android.data.makeYouTubeEmbedVideoPlayerUrl
import org.mtransit.android.databinding.FragmentNewsDetailsBinding
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.news.NewsListViewModel
import org.mtransit.android.ui.news.image.NewsImagesAdapter
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.util.LinkUtils
import org.mtransit.android.util.UIFeatureFlags
import org.mtransit.android.util.UITimeUtils
import javax.inject.Inject
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class NewsDetailsFragment : MTFragmentX(R.layout.fragment_news_details) {

    companion object {
        private val LOG_TAG = NewsDetailsFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(newsArticle: News): NewsDetailsFragment = newInstance(newsArticle.authority, newsArticle.uuid)

        @JvmStatic
        fun newInstance(authorityAndUuid: AuthorityAndUuid) = newInstance(authorityAndUuid.getAuthority(), authorityAndUuid.getUuid())

        @JvmStatic
        fun newInstance(
            authority: String,
            uuid: String,
        ): NewsDetailsFragment {
            return NewsDetailsFragment().apply {
                arguments = newInstanceArgs(authority, uuid)
            }
        }

        @JvmStatic
        fun newInstanceArgs(newsArticle: News) = newInstanceArgs(newsArticle.authority, newsArticle.uuid)

        @JvmStatic
        fun newInstanceArgs(authority: String, uuid: String) = bundleOf(
            NewsDetailsViewModel.EXTRA_AUTHORITY to authority,
            NewsDetailsViewModel.EXTRA_NEWS_UUID to uuid,
        )

        @JvmStatic
        fun newInstanceArgs(authorityAndUuid: AuthorityAndUuid) = newInstanceArgs(authorityAndUuid.getAuthority(), authorityAndUuid.getUuid())

        val SPLIT_ARTICLE_REGEX = Regex(HtmlUtils.BRS_REGEX, RegexOption.IGNORE_CASE)
    }

    private var _logTag: String = LOG_TAG

    override fun getLogTag(): String = _logTag

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var inlineBannerAdManager: InlineBannerAdManager

    private val viewModel by viewModels<NewsDetailsViewModel>()

    private val parentViewModel by viewModels<NewsListViewModel>({ requireParentFragment() })

    private var binding: FragmentNewsDetailsBinding? = null

    private val thumbnailsListAdapter: NewsImagesAdapter by lazy { NewsImagesAdapter(this.imageManager, this::onImageClicked, horizontal = true) }

    private var timeChangedReceiverEnabled = false

    private val timeChangedReceiver = UITimeUtils.TimeChangedReceiver { updateNewsView() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MTTransitions.setContainerTransformTransition(this)
    }

    private fun onImageClicked(view: View, newsImage: NewsImage) {
        val titleStatic = viewModel.newsArticle.value?.text?.take(33)
        LinkUtils.open(view, requireActivity(), newsImage.imageUrl, getString(commonsR.string.web_browser), titleStatic, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MTTransitions.postponeEnterTransition(this)
        binding = FragmentNewsDetailsBinding.bind(view).apply {
            thumbnailsList.adapter = thumbnailsListAdapter
        }
        viewModel.newsArticle.observe(viewLifecycleOwner) { newsArticle ->
            updateNewsView(newsArticle)
            MTTransitions.startPostponedEnterTransitionOnPreDraw(view.parent as? ViewGroup, this)
        }
        viewModel.dataSourceRemovedEvent.observe(viewLifecycleOwner, EventObserver { removed ->
            if (removed) {
                (activity as MainActivity?)?.popFragmentFromStack(this) // close this fragment
            }
        })
        parentViewModel.selectedNewsArticleAuthorityAndUUID.observe(viewLifecycleOwner) { newAuthorityAndUuid ->
            if (newAuthorityAndUuid?.isAuthorityAndUuidValid() == false) {
                return@observe
            }
            if (newAuthorityAndUuid == null) { // navigate back to list on phone
                //noinspection DeprecatedCall
                binding?.thumbnailWebView?.onPause()
                inlineBannerAdManager.onPause(this)
            } else if (isResumed) {
                //noinspection DeprecatedCall
                binding?.thumbnailWebView?.onResume()
                inlineBannerAdManager.onResume(this)
            }
        }
    }

    @SuppressLint("DeprecatedCall")
    private fun updateNewsView(newsArticle: News? = viewModel.newsArticle.value) = binding?.apply {
        _logTag = LOG_TAG + "-" + newsArticle?.uuid
        newsArticle?.let { newsArticle ->
            MTTransitions.setTransitionName(root, "news_" + newsArticle.uuid)
            updateThumbnails(newsArticle)
            updateNewsArticleText(newsArticle)
            authorIcon.apply {
                isVisible = if (newsArticle.hasAuthorPictureURL()) {
                    noAuthorIconSpace.isVisible = false
                    imageManager.loadInto(context, newsArticle.authorPictureURL, this)
                    true
                } else {
                    noAuthorIconSpace.isVisible = true
                    imageManager.clear(context, this)
                    false
                }
            }
            author.apply {
                text = newsArticle.authorName
                setTextColor(
                    newsArticle.colorIntOrNull?.let {
                        ColorUtils.adaptColorToTheme(context, it)
                    } ?: run {
                        ColorUtils.getTextColorSecondary(context)
                    }
                )
            }
            source.apply {
                text = newsArticle.authorUsername?.let { authorUsername ->
                    context.getString(
                        R.string.news_shared_on_and_author_and_source,
                        authorUsername,
                        newsArticle.sourceLabel
                    )
                } ?: newsArticle.sourceLabel
            }
            date.apply {
                setText(UITimeUtils.formatRelativeTime(newsArticle.createdAtInMs), TextView.BufferType.SPANNABLE)
                val newsArticleWebURL = newsArticle.webURL.ifBlank { newsArticle.authorProfileURL }
                setOnClickListener { view ->
                    LinkUtils.open(view, requireActivity(), newsArticleWebURL, getString(commonsR.string.web_browser), true)
                }
            }
            dateLong.apply {
                val formattedDate = DateUtils.formatDateTime(
                    context,
                    newsArticle.createdAtInMs,
                    DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY
                )
                setText(formattedDate, TextView.BufferType.SPANNABLE)
                val newsArticleWebURL = newsArticle.webURL.ifBlank { newsArticle.authorProfileURL }
                setOnClickListener { view ->
                    LinkUtils.open(view, requireActivity(), newsArticleWebURL, getString(commonsR.string.web_browser), true)
                }
            }
        }
    }

    private fun updateNewsArticleText(newsArticle: News) = binding?.apply {
        val paragraphs = newsArticle.textHTML.split(SPLIT_ARTICLE_REGEX)
        val limit = when {
            newsArticle.hasImagesOrVideoThumbnail -> 150
            else -> 300
        }
        val textHTML1Sb = StringBuilder()
        val textHTML2Sb = StringBuilder()
        var text1Lines = 0
        var text2Lines = 0
        paragraphs.forEachIndexed { index, paragraph ->
            if (textHTML1Sb.length < limit
                && index != paragraphs.size - 1 && paragraphs.size > 1 // keep last paragraph for after ad break
            ) {
                if (text1Lines > 0) {
                    textHTML1Sb.append(HtmlUtils.BR)
                }
                textHTML1Sb.append(paragraph)
                text1Lines++
            } else {
                if (text2Lines > 0) {
                    textHTML2Sb.append(HtmlUtils.BR)
                }
                textHTML2Sb.append(paragraph)
                text2Lines++
            }
        }
        val textHTML1 = textHTML1Sb.toString()
        val textHTML2 = textHTML2Sb.toString()
        newsText1.apply {
            setText(LinkUtils.linkifyHtml(textHTML1, true), TextView.BufferType.SPANNABLE)
            movementMethod = LinkUtils.LinkMovementMethodInterceptor.getInstance { view, newTextUrl ->
                LinkUtils.open(view, requireActivity(), newTextUrl, getString(commonsR.string.web_browser), true)
            }
            setLinkTextColor(
                newsArticle.colorIntOrNull?.let {
                    ColorUtils.adaptColorToTheme(context, it)
                } ?: run {
                    ColorUtils.getTextColorPrimary(context)
                }
            )
        }
        inlineBannerAdManager.refreshBannerAdStatus(this@NewsDetailsFragment, parentFragment as? IAdScreenFragment)
        newsText2.apply {
            setText(LinkUtils.linkifyHtml(textHTML2, true), TextView.BufferType.SPANNABLE)
            movementMethod = LinkUtils.LinkMovementMethodInterceptor.getInstance { view, newTextUrl ->
                LinkUtils.open(view, requireActivity(), newTextUrl, getString(commonsR.string.web_browser), true)
            }
            setLinkTextColor(
                newsArticle.colorIntOrNull?.let {
                    ColorUtils.adaptColorToTheme(context, it)
                } ?: run {
                    ColorUtils.getTextColorPrimary(context)
                }
            )
        }
    }

    @SuppressLint("DeprecatedCall")
    private fun updateThumbnails(newsArticle: News) = binding?.apply {
        when {
            newsArticle.isTwitterVideo -> {
                if (!UIFeatureFlags.F_NEWS_THUMBNAIL_PLAY_BUTTON) {
                    thumbnail.apply {
                        isVisible = false
                        imageManager.clear(context, this)
                    }
                }
                thumbnailsListContainer.isVisible = false
                noThumbnailSpace.isVisible = false

                if (UIFeatureFlags.F_NEWS_THUMBNAIL_PLAY_BUTTON) {
                    thumbnail.apply {
                        imageManager.loadInto(context, newsArticle.firstValidImageUrl, this)
                        isVisible = true
                        setOnClickListener {}
                    }
                    thumbnailPlay.apply {
                        isVisible = true
                        setOnClickListener { view ->
                            isVisible = false
                            thumbnail.isVisible = false

                            thumbnailWebView.apply {
                                setupWebView(this)
                                newsArticle.getTwitterVideoId()?.let { videoId ->
                                    makeTwitterEmbedVideoPlayerUrl(videoId).let { newUrl ->
                                        if (url != newUrl) {
                                            loadUrl(newUrl)
                                        }
                                    }
                                }
                                isVisible = true
                            }
                        }
                    }
                } else {
                    thumbnailWebView.apply {
                        setupWebView(this)
                        newsArticle.getTwitterVideoId()?.let { videoId ->
                            makeTwitterEmbedVideoPlayerUrl(videoId).let { newUrl ->
                                if (url != newUrl) {
                                    loadUrl(newUrl)
                                }
                            }
                        }
                        isVisible = true
                    }
                }
            }

            newsArticle.isYouTubeVideo -> {
                if (!UIFeatureFlags.F_NEWS_THUMBNAIL_PLAY_BUTTON) {
                    thumbnail.apply {
                        isVisible = false
                        imageManager.clear(context, this)
                    }
                }
                thumbnailsListContainer.isVisible = false
                noThumbnailSpace.isVisible = false

                if (UIFeatureFlags.F_NEWS_THUMBNAIL_PLAY_BUTTON) {
                    thumbnail.apply {
                        imageManager.loadInto(context, newsArticle.firstValidImageUrl, this)
                        isVisible = true
                        setOnClickListener {}
                    }
                    thumbnailPlay.apply {
                        isVisible = true

                        setOnClickListener { view ->
                            isVisible = false
                            thumbnail.isVisible = false

                            thumbnailWebView.apply {
                                setupWebView(this)
                                newsArticle.getYouTubeVideoId()?.let { videoId ->
                                    makeYouTubeEmbedVideoPlayerUrl(videoId, autoPlay = true, mute = true).let { newUrl ->
                                        if (url != newUrl) {
                                            loadUrl(newUrl)
                                        }
                                    }
                                }
                                isVisible = true
                            }
                        }
                    }
                } else {
                    thumbnailWebView.apply {
                        setupWebView(this)
                        newsArticle.getYouTubeVideoId()?.let { videoId ->
                            makeYouTubeEmbedVideoPlayerUrl(videoId, autoPlay = false).let { newUrl ->
                                if (url != newUrl) {
                                    loadUrl(newUrl)
                                }
                            }
                        }
                        isVisible = true
                    }
                }
            }

            newsArticle.imageURLsCount == 0 -> {
                thumbnail.apply {
                    isVisible = false
                    imageManager.clear(context, this)
                }
                thumbnailsListContainer.isVisible = false
                thumbnailWebView.isVisible = false

                noThumbnailSpace.isVisible = true
            }

            newsArticle.imageURLsCount == 1 -> {
                noThumbnailSpace.isVisible = false
                thumbnailsListContainer.isVisible = false
                thumbnailWebView.isVisible = false
                thumbnail.apply {
                    imageManager.loadInto(context, newsArticle.firstValidImageUrl, this)
                    isVisible = true
                    setOnClickListener { view ->
                        val titleStatic = viewModel.newsArticle.value?.text?.take(33)
                        LinkUtils.open(view, requireActivity(), newsArticle.firstValidImageUrl, getString(commonsR.string.web_browser), titleStatic, true)
                    }
                }
            }

            else -> { // newsArticle.imageURLsCount > 1
                noThumbnailSpace.isVisible = false
                thumbnail.apply {
                    isVisible = false
                    imageManager.clear(context, this)
                }
                thumbnailWebView.isVisible = false

                thumbnailsListAdapter.submitList(newsArticle.imageUrls)
                thumbnailsListContainer.isVisible = true
            }
        }
    }

    private val customWebViewClient = object : WebViewClientCompat() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val webViewVideoPlayerUrl = request.url.toString()
            LinkUtils.open(view, requireActivity(), webViewVideoPlayerUrl, getString(commonsR.string.web_browser), true)
            return true // handled
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "DeprecatedCall")
    private fun setupWebView(webView: WebView) {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }
            webViewClient = customWebViewClient
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun onResume() {
        super.onResume()
        updateNewsView()
        enableTimeChangedReceiver()
        //noinspection DeprecatedCall
        binding?.thumbnailWebView?.onResume()
        if (UIFeatureFlags.F_APP_BAR_SCROLL_BEHAVIOR) {
            binding?.scrollView?.fullScroll(View.FOCUS_UP)
        }
        inlineBannerAdManager.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        disableTimeChangedReceiver()
        //noinspection DeprecatedCall
        binding?.thumbnailWebView?.onPause()
        inlineBannerAdManager.onPause(this)
    }

    private fun enableTimeChangedReceiver() {
        if (!timeChangedReceiverEnabled) {
            context?.let {
                ContextCompat.registerReceiver(it, timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER, ContextCompat.RECEIVER_NOT_EXPORTED)
                timeChangedReceiverEnabled = true
                updateNewsView() // force update to current time before next change
            }
        }
    }

    private fun disableTimeChangedReceiver() {
        if (timeChangedReceiverEnabled) {
            context?.let {
                it.unregisterReceiver(timeChangedReceiver)
                timeChangedReceiverEnabled = false
            }
            updateNewsView() // mark time as not updating anymore
        }
    }

    override fun onDetach() {
        super.onDetach()
        disableTimeChangedReceiver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //noinspection DeprecatedCall
        binding?.thumbnailWebView?.destroy()
        binding = null
        inlineBannerAdManager.destroyAd(this)
    }

    override fun <T : View?> findViewById(id: Int) = this.view?.findViewById<T>(id)
}