package org.mtransit.android.ui.news.viewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.data.source.NewsRepository

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
        setCurrentNewsUUID(getSavedCurrentNewsUUID())
    }

    private fun setCurrentNewsUUID(uuid: String?) {
        savedStateHandle.set(CURRENT_NEWS_UUID_SAVED_STATE_KEY, uuid)
    }

    private fun getSavedCurrentNewsUUID(): String? {
        return savedStateHandle.get(CURRENT_NEWS_UUID_SAVED_STATE_KEY)
    }

    private fun getCurrentNewsUUID(): String {
        return getSavedCurrentNewsUUID() ?: pickCurrentNewsUUID()
    }

    val currentNewsUUID: LiveData<String> =
        savedStateHandle.getLiveData(CURRENT_NEWS_UUID_SAVED_STATE_KEY, pickCurrentNewsUUID())

    private fun pickCurrentNewsUUID(): String {
        return newsRepository.newsArticles.value?.first()?.uUID ?: ""
    }

    fun onModulesUpdated() {
    }

    fun onPageSelected(uuid: String?) {
        setCurrentNewsUUID(uuid)
    }
}