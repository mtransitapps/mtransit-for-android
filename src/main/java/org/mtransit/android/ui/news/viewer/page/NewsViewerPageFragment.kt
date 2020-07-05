package org.mtransit.android.ui.news.viewer.page

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import org.mtransit.android.R
import org.mtransit.android.commons.BundleUtils
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.databinding.FragmentNewsViewerPageBinding
import org.mtransit.android.di.ServiceLocator
import org.mtransit.android.ui.view.MTViewModelFactory
import org.mtransit.android.util.LinkUtils
import org.mtransit.android.util.UITimeUtils

class NewsViewerPageFragment : Fragment(R.layout.fragment_news_viewer_page) {

    companion object {

        private const val EXTRA_AUTHORITY = "extra_agency_authority"
        private const val EXTRA_NEWS_UUID = "extra_news_uuid"

        @JvmStatic
        fun newInstance(
            newsArticle: NewsArticle
        ): NewsViewerPageFragment {
            return newInstance(
                newsArticle.authority,
                newsArticle.uUID
            )
        }

        @JvmStatic
        fun newInstance(
            authority: String,
            uuid: String
        ): NewsViewerPageFragment {
            return NewsViewerPageFragment()
                .apply {
                    arguments = bundleOf(
                        EXTRA_AUTHORITY to authority,
                        EXTRA_NEWS_UUID to uuid
                    )
                }
        }

        val LOG_TAG = NewsViewerPageFragment::class.java.simpleName
    }

    private val viewModel: NewsViewerPageViewModel by viewModels {
        MTViewModelFactory(
            ServiceLocator.newsRepository,
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.start(
            BundleUtils.getString(EXTRA_AUTHORITY, savedInstanceState, arguments),
            BundleUtils.getString(EXTRA_NEWS_UUID, savedInstanceState, arguments)
        )
    }

    private var viewBinding: FragmentNewsViewerPageBinding? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.newsArticle.observe(this.viewLifecycleOwner, Observer {
            it?.let { newsArticle ->
                viewBinding?.apply {
                    val context = thisNews.context
                    thumbnail.apply {
                        isVisible = if (newsArticle.hasValidImageUrls) {
                            noThumbnailSpace.isVisible = false
                            Glide.with(context)
                                .load(newsArticle.firstValidImageUrl)
                                .into(this)
                            true
                        } else {
                            Glide.with(context)
                                .clear(this)
                            noThumbnailSpace.isVisible = true
                            false
                        }
                    }
                    authorIcon.apply {
                        isVisible = if (newsArticle.hasAuthorPictureURL) {
                            noAuthorIconSpace.isVisible = false
                            Glide.with(context)
                                .load(newsArticle.authorPictureURL)
                                .into(this)
                            true
                        } else {
                            noAuthorIconSpace.isVisible = true
                            Glide.with(context)
                                .clear(this)
                            false
                        }
                    }
                    author.apply {
                        text = newsArticle.authorOneLine
                        setTextColor(
                            if (newsArticle.hasColor()) {
                                ColorUtils.adaptColorToTheme(
                                    context,
                                    newsArticle.colorInt
                                )
                            } else {
                                ColorUtils.getTextColorSecondary(
                                    context
                                )
                            }
                        )
                    }
                    source.apply {
                        text = newsArticle.sourceLabel
                    }
                    newsText.apply {
                        setText(
                            LinkUtils.linkifyHtml(newsArticle.textHTML, true),
                            TextView.BufferType.SPANNABLE
                        )
                        movementMethod =
                            LinkUtils.LinkMovementMethodInterceptop.getInstance { url ->
                                LinkUtils.open(
                                    requireActivity(),
                                    url,
                                    getString(R.string.web_browser),
                                    true
                                )
                            }
                        setLinkTextColor(
                            if (newsArticle.hasColor()) {
                                ColorUtils.adaptColorToTheme(
                                    context,
                                    newsArticle.colorInt
                                )
                            } else {
                                ColorUtils.getTextColorPrimary(context)
                            }
                        )
                    }
                    date.apply {
                        setText(
                            UITimeUtils.formatRelativeTime(newsArticle.createdAtInMs),
                            TextView.BufferType.SPANNABLE
                        )
                        val newWebURL = if (newsArticle.webURL.isEmpty()) {
                            newsArticle.authorProfileURL
                        } else {
                            newsArticle.webURL
                        }
                        setOnClickListener {
                            LinkUtils.open(
                                requireActivity(),
                                newWebURL,
                                getString(R.string.web_browser),
                                true
                            )
                        }
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNewsViewerPageBinding.bind(view)
        viewBinding = binding
    }
}