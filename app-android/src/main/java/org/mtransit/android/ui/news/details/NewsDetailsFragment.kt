@file:JvmName("NewsDetailsFragment") // ANALYTICS
package org.mtransit.android.ui.news.details

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
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
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.registerReceiverCompat
import org.mtransit.android.data.AuthorityAndUuid
import org.mtransit.android.data.NewsImage
import org.mtransit.android.data.imageUrls
import org.mtransit.android.data.isYouTubeVideo
import org.mtransit.android.data.makeYouTubeEmbedVideoPlayerUrl
import org.mtransit.android.data.youTubeVideoId
import org.mtransit.android.databinding.FragmentNewsDetailsBinding
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.MTFragmentX
import org.mtransit.android.ui.news.image.NewsImagesAdapter
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.ui.view.common.ImageManager
import org.mtransit.android.ui.view.common.MTTransitions
import org.mtransit.android.util.LinkUtils
import org.mtransit.android.util.UITimeUtils
import java.util.Locale
import javax.inject.Inject
import org.mtransit.android.commons.R as commonsR

@AndroidEntryPoint
class NewsDetailsFragment : MTFragmentX(R.layout.fragment_news_details) {

    companion object {
        private val LOG_TAG = NewsDetailsFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(newsArticle: News): NewsDetailsFragment = newInstance(newsArticle.authority, newsArticle.uuid)

        @JvmStatic
        fun newInstance(authorityAndUuid: AuthorityAndUuid) = newInstance(authorityAndUuid.getAuthority().authority, authorityAndUuid.getUuid().uuid)

        @JvmStatic
        fun newInstance(
            authority: String,
            uuid: String
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
        fun newInstanceArgs(authorityAndUuid: AuthorityAndUuid) = newInstanceArgs(authorityAndUuid.getAuthority().authority, authorityAndUuid.getUuid().uuid)
    }

    override fun getLogTag(): String = LOG_TAG

    @Inject
    lateinit var imageManager: ImageManager

    private val viewModel by viewModels<NewsDetailsViewModel>()

    private var binding: FragmentNewsDetailsBinding? = null

    private val thumbnailsListAdapter: NewsImagesAdapter by lazy { NewsImagesAdapter(this.imageManager, this::onImageClicked, horizontal = true) }

    private var timeChangedReceiverEnabled = false

    private val timeChangedReceiver = UITimeUtils.TimeChangedReceiver { updateNewsView() }

    private var dateTimeFormat: ThreadSafeDateFormatter? = null

    private fun getDateTimeFormatter(context: Context): ThreadSafeDateFormatter {
        return dateTimeFormat ?: ThreadSafeDateFormatter(
            (if (UITimeUtils.is24HourFormat(context)) "HH:mm" else "h:mm a") + " - EEEE, MMMM d, yyyy",
            Locale.getDefault()
        ).also { dateTimeFormat = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MTTransitions.setContainerTransformTransition(this)
    }

    private fun onImageClicked(view: View, newsImage: NewsImage) {
        LinkUtils.open(view, requireActivity(), newsImage.imageUrl, getString(commonsR.string.web_browser), true)
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
    }

    private fun updateNewsView(newsArticle: News? = viewModel.newsArticle.value) {
        binding?.apply {
            newsArticle?.let { newsArticle ->
                MTTransitions.setTransitionName(root, "news_" + newsArticle.uuid)
                when {
                    newsArticle.isYouTubeVideo -> {
                        thumbnail.apply {
                            isVisible = false
                            imageManager.clear(context, this)
                        }
                        thumbnailsListContainer.isVisible = false
                        noThumbnailSpace.isVisible = false

                        thumbnailWebView.apply {
                            setupWebView(this)
                            newsArticle.youTubeVideoId?.let { videoId ->
                                makeYouTubeEmbedVideoPlayerUrl(videoId, false).let { newUrl ->
                                    if (this.url != newUrl) {
                                        // TODO ? loadUrl("about:blank")
                                        loadUrl(newUrl)
                                    }
                                }
                            }
                            isVisible = true
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
                                LinkUtils.open(view, requireActivity(), newsArticle.firstValidImageUrl, getString(commonsR.string.web_browser), true)
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
                    text = newsArticle.authorOneLine
                    setTextColor(
                        newsArticle.colorIntOrNull?.let {
                            ColorUtils.adaptColorToTheme(context, it)
                        } ?: run {
                            ColorUtils.getTextColorSecondary(context)
                        }
                    )
                }
                source.apply {
                    text = newsArticle.sourceLabel
                }
                newsText.apply {
                    setText(LinkUtils.linkifyHtml(newsArticle.textHTML, true), TextView.BufferType.SPANNABLE)
                    movementMethod = LinkUtils.LinkMovementMethodInterceptor.getInstance { view, url ->
                        LinkUtils.open(view, requireActivity(), url, getString(commonsR.string.web_browser), true)
                    }
                    setLinkTextColor(
                        newsArticle.colorIntOrNull?.let {
                            ColorUtils.adaptColorToTheme(context, it)
                        } ?: run {
                            ColorUtils.getTextColorPrimary(context)
                        }
                    )
                }
                date.apply {
                    setText(UITimeUtils.formatRelativeTime(newsArticle.createdAtInMs), TextView.BufferType.SPANNABLE)
                    val newWebURL = newsArticle.webURL.ifBlank { newsArticle.authorProfileURL }
                    setOnClickListener { view ->
                        LinkUtils.open(view, requireActivity(), newWebURL, getString(commonsR.string.web_browser), true)
                    }
                }
                dateLong.apply {
                    setText(getDateTimeFormatter(context).formatThreadSafe(newsArticle.createdAtInMs), TextView.BufferType.SPANNABLE)
                    val newWebURL = newsArticle.webURL.ifBlank { newsArticle.authorProfileURL }
                    setOnClickListener { view ->
                        LinkUtils.open(view, requireActivity(), newWebURL, getString(commonsR.string.web_browser), true)
                    }
                }
            }
        }
    }

    private var webViewSetup = false

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        if (webViewSetup) return
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }
            webViewClient = object : WebViewClientCompat() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    LinkUtils.open(view, requireActivity(), request.url.toString(), getString(commonsR.string.web_browser), true)
                    return true // handled
                }
            }
        }
        webViewSetup = true
    }

    override fun onResume() {
        super.onResume()
        updateNewsView()
        enableTimeChangedReceiver()
        binding?.thumbnailWebView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        disableTimeChangedReceiver()
        binding?.thumbnailWebView?.onPause()
    }

    private fun enableTimeChangedReceiver() {
        if (!timeChangedReceiverEnabled) {
            activity?.registerReceiverCompat(timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER, ContextCompat.RECEIVER_NOT_EXPORTED)
            timeChangedReceiverEnabled = true
            updateNewsView() // force update to current time before next change
        }
    }

    private fun disableTimeChangedReceiver() {
        if (timeChangedReceiverEnabled) {
            activity?.unregisterReceiver(timeChangedReceiver)
            timeChangedReceiverEnabled = false
            updateNewsView() // mark time as not updating anymore
        }
    }

    override fun onDetach() {
        super.onDetach()
        disableTimeChangedReceiver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.thumbnailWebView?.destroy()
        binding = null
    }
}