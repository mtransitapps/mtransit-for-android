package org.mtransit.android.ui.news.viewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.data.source.NewsRepository
import org.mtransit.android.ui.view.common.PairMediatorLiveData

const val CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY = "CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY"
const val CURRENT_NEWS_UUID_SAVED_STATE_KEY = "CURRENT_NEWS_UUID_SAVED_STATE_KEY"

class NewsViewerViewModel(
    private val newsRepository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _filteredNewsArticles: LiveData<List<NewsArticle>> =
        newsRepository.filter.switchMap { trigger ->
            viewModelScope.launch {
                newsRepository.refreshFilteredNewsIfEmpty()
            }
            newsRepository
                .filteredNewsArticles
                .distinctUntilChanged()
        }

    val filteredNewsArticles: LiveData<List<NewsArticle>> = _filteredNewsArticles

    init {
        setCurrentNews(
            getSavedCurrentNewsAuthority(),
            getSavedCurrentNewsUUID()
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

    fun getSavedCurrentNewsUUID(): String? {
        return savedStateHandle.get(CURRENT_NEWS_UUID_SAVED_STATE_KEY)
    }

    private fun loadSavedCurrentNewsUUID(): LiveData<String> {
        return savedStateHandle.getLiveData<String>(CURRENT_NEWS_UUID_SAVED_STATE_KEY)
    }

    private val _authorityAndUUID =
        PairMediatorLiveData(
            loadSavedCurrentNewsAuthority(),
            loadSavedCurrentNewsUUID()
        )
    val currentNewsArticleUUID: LiveData<String> = Transformations.map(_authorityAndUUID) {
        it.second
    }

    fun onModulesUpdated() {
    }

    fun start(
        authority: String?,
        uuid: String?,
        targetAuthorities: List<String>?,
        filterUUIDs: List<String>?,
        filterTargets: List<String>?
    ) {
        newsRepository.setFilter(
            targetAuthorities ?: emptyList(),
            filterUUIDs ?: emptyList(),
            filterTargets ?: emptyList()
        )
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

    private fun onPageSelected(authority: String?, uuid: String?) {
        if (authority == getSavedCurrentNewsAuthority()
            && uuid == getSavedCurrentNewsUUID()
        ) {
            return
        }
        setCurrentNews(authority, uuid)
    }
}