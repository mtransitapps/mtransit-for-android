package org.mtransit.android.ui.news.viewer

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import org.mtransit.android.R
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.databinding.FragmentNewsViewerBinding
import org.mtransit.android.di.ServiceLocator
import org.mtransit.android.ui.fragment.ABFragment
import org.mtransit.android.ui.news.viewer.page.NewsViewerPageFragment
import org.mtransit.android.ui.view.MTViewModelFactory

class NewsViewerFragment : ABFragment(R.layout.fragment_news_viewer) {

    companion object {

        private const val EXTRA_AUTHORITY = "extra_agency_authority"
        private const val EXTRA_NEWS_UUID = "extra_news_uuid"

        @JvmStatic
        fun newInstance(
            newsArticle: NewsArticle
        ): NewsViewerFragment {
            return newInstance(
                newsArticle.authority,
                newsArticle.uUID
            )
        }

        @JvmStatic
        fun newInstance(
            authority: String,
            uuid: String
        ): NewsViewerFragment {
            return NewsViewerFragment().apply {
                arguments = bundleOf(
                    EXTRA_AUTHORITY to authority,
                    EXTRA_NEWS_UUID to uuid
                )
            }
        }

        val LOG_TAG = NewsViewerPageFragment::class.java.simpleName

        private const val TRACKING_SCREEN_NAME = "NewsViewer"
    }

    override fun getLogTag() = LOG_TAG

    override fun getScreenName() = TRACKING_SCREEN_NAME

    private var subTitle: String? = null
    private var colorInt: Int? = null

    override fun getABTitle(context: Context?) =
        context?.getString(R.string.news) ?: super.getABTitle(context)

    override fun getABSubtitle(context: Context?) = subTitle ?: super.getABSubtitle(context)

    override fun getABBgColor(context: Context?): Int = colorInt ?: super.getABBgColor(context)


    private lateinit var viewPager: ViewPager2

    private var viewBinding: FragmentNewsViewerBinding? = null

    private lateinit var adapter: NewsViewerPagerAdapter

    private val viewModel: NewsViewerViewModel by viewModels {
        MTViewModelFactory(
            ServiceLocator.newsRepository,
            this
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.newsArticles.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })
        viewModel.currentNewsUUID.observe(viewLifecycleOwner, Observer {
            val position = adapter.getItemPosition(it)
            if (position >= 0) {
                viewPager.currentItem = position
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNewsViewerBinding.bind(view)
        viewBinding = binding
        setupView(binding)
    }

    private fun setupView(binding: FragmentNewsViewerBinding) {
        viewPager = binding.viewpager
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.onPageSelected(adapter.getItem(position).uUID)
            }
        })
        adapter = NewsViewerPagerAdapter(this)
        viewPager.adapter = adapter
    }

    override fun onModulesUpdated() {
        viewModel.onModulesUpdated()
    }
}