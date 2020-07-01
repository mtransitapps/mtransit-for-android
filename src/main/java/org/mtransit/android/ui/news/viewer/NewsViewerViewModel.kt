package org.mtransit.android.ui.news.viewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.data.source.NewsRepository
import org.mtransit.android.ui.view.common.PairMediatorLiveData

const val CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY = "CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY"
const val CURRENT_NEWS_UUID_SAVED_STATE_KEY = "CURRENT_NEWS_UUID_SAVED_STATE_KEY"

class NewsViewerViewModel(
    private val newsRepository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _newsArticles: LiveData<List<NewsArticle>> =
        newsRepository
            .newsArticles
            .distinctUntilChanged()

    val newsArticles: LiveData<List<NewsArticle>> = _newsArticles

    init {
        setCurrentNews(
            getSavedCurrentNewsUUID(),
            getSavedCurrentNewsAuthority()
        )
    }

    private fun setCurrentNews(authority: String?, uuid: String?) {
        savedStateHandle.set(CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY, authority)
        savedStateHandle.set(CURRENT_NEWS_UUID_SAVED_STATE_KEY, uuid)
    }

    private fun getSavedCurrentNewsAuthority(): String? {
        return savedStateHandle.get(CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY)
    }

    private fun loadSavedCurrentNewsAuthority(): LiveData<String> {
        return savedStateHandle.getLiveData<String>(CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY)
    }

    private fun getSavedCurrentNewsUUID(): String? {
        return savedStateHandle.get(CURRENT_NEWS_UUID_SAVED_STATE_KEY)
    }

    private fun loadSavedCurrentNewsUUID(): LiveData<String> {
        return savedStateHandle.getLiveData<String>(CURRENT_NEWS_UUID_SAVED_STATE_KEY)
    }



    private val _authorityAndUUID =
        PairMediatorLiveData(loadSavedCurrentNewsAuthority(), loadSavedCurrentNewsUUID())
    val currentNewsAuthorityAndUUID = _authorityAndUUID
    fun onModulesUpdated() {
    }

    fun start(authority: String?, uuid: String?) {
        if (authority == getSavedCurrentNewsAuthority()
            && uuid == getSavedCurrentNewsUUID()
        ) {
            return
        }
        setCurrentNews(authority, uuid)
    }

    fun onPageSelected(newsArticle: NewsArticle) {
        onPageSelected(newsArticle.authority, newsArticle.uUID)
    }

    fun onPageSelected(authority: String?, uuid: String?) {
        if (authority == getSavedCurrentNewsAuthority()
            && uuid == getSavedCurrentNewsUUID()
        ) {
            return
        }
        setCurrentNews(authority, uuid)
    }
}