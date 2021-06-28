@file:JvmName("NewsDetailsFragment") // ANALYTICS
package org.mtransit.android.ui.news.details

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.databinding.FragmentNewsDetailsBinding
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.view.common.EventObserver
import org.mtransit.android.util.LinkUtils
import org.mtransit.android.util.UITimeUtils

@AndroidEntryPoint
class NewsDetailsFragment : ABFragment(R.layout.fragment_news_details) {

    companion object {
        private val LOG_TAG = NewsDetailsFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "News"

        @JvmStatic
        fun newInstance(
            newsArticle: News,
        ): NewsDetailsFragment {
            return newInstance(
                newsArticle.authority,
                newsArticle.uuid,
            )
        }

        @JvmStatic
        fun newInstance(
            authority: String,
            uuid: String
        ): NewsDetailsFragment {
            return NewsDetailsFragment().apply {
                arguments = bundleOf(
                    NewsDetailsViewModel.EXTRA_AUTHORITY to authority,
                    NewsDetailsViewModel.EXTRA_NEWS_UUID to uuid,
                )
            }
        }
    }

    override fun getLogTag(): String = LOG_TAG

    override fun getScreenName(): String = addedViewModel?.uuid?.value?.let { "$TRACKING_SCREEN_NAME/$it" } ?: TRACKING_SCREEN_NAME

    private val viewModel by viewModels<NewsDetailsViewModel>()
    private val addedViewModel: NewsDetailsViewModel?
        get() = if (isAdded) viewModel else null

    private var binding: FragmentNewsDetailsBinding? = null

    private var timeChangedReceiverEnabled = false

    private val timeChangedReceiver = UITimeUtils.TimeChangedReceiver { updateNewsView() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNewsDetailsBinding.bind(view).apply {
            // DO NOTHING
        }
        viewModel.newsArticle.observe(viewLifecycleOwner, { newsArticle ->
            updateNewsView(newsArticle)
            abController?.apply {
                setABBgColor(this@NewsDetailsFragment, getABBgColor(context), false)
                setABTitle(this@NewsDetailsFragment, getABTitle(context), false)
                setABSubtitle(this@NewsDetailsFragment, getABSubtitle(context), false)
                setABReady(this@NewsDetailsFragment, isABReady, true)
            }
        })
        viewModel.dataSourceRemovedEvent.observe(viewLifecycleOwner, EventObserver { removed ->
            if (removed) {
                (activity as MainActivity?)?.popFragmentFromStack(this) // close this fragment
            }
        })
    }

    private fun updateNewsView(newsArticle: News? = viewModel.newsArticle.value) {
        binding?.apply {
            newsArticle?.let { newsArticle ->
                newsText.apply {
                    setText(LinkUtils.linkifyHtml(newsArticle.textHTML, true), TextView.BufferType.SPANNABLE)
                    movementMethod = LinkUtils.LinkMovementMethodInterceptor.getInstance { url ->
                        LinkUtils.open(requireActivity(), url, getString(R.string.web_browser), true)
                    }
                    setLinkTextColor(
                        if (newsArticle.hasColor()) {
                            ColorUtils.adaptColorToTheme(context, newsArticle.colorInt)
                        } else {
                            ColorUtils.getTextColorPrimary(context)
                        }
                    )
                }
                date.apply {
                    setText(UITimeUtils.formatRelativeTime(newsArticle.createdAtInMs), TextView.BufferType.SPANNABLE)
                    val newWebURL = if (newsArticle.webURL.isBlank()) newsArticle.authorProfileURL else newsArticle.webURL
                    setOnClickListener {
                        LinkUtils.open(requireActivity(), newWebURL, getString(R.string.web_browser), true)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNewsView()
        enableTimeChangedReceiver()
    }

    override fun onPause() {
        super.onPause()
        disableTimeChangedReceiver()
    }

    private fun enableTimeChangedReceiver() {
        if (!timeChangedReceiverEnabled) {
            activity?.registerReceiver(timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER)
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

    override fun getABBgColor(context: Context?) = addedViewModel?.newsArticle?.value?.colorIntOrNull ?: super.getABBgColor(context)

    override fun getABTitle(context: Context?) = addedViewModel?.newsArticle?.value?.authorOneLine ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = addedViewModel?.newsArticle?.value?.sourceLabel ?: super.getABTitle(context)

    override fun isABReady() = addedViewModel?.newsArticle?.value != null

    override fun onDetach() {
        super.onDetach()
        disableTimeChangedReceiver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


}