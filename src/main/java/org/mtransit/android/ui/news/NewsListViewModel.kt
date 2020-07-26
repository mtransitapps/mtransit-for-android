package org.mtransit.android.ui.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.data.source.NewsRepository
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData

class NewsListViewModel(
    private val newsRepository: NewsRepository
) : ViewModel(), NewsListAdapter.OnNewsArticleSelectedListener {

    private val _forceUpdate = MutableLiveData<Boolean>(false)

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    val filter: LiveData<Triple<List<String>?, List<String>?, List<String>?>> =
        newsRepository.filter

    private val _trigger = PairMediatorLiveData(filter, _forceUpdate)
    private val _filteredNewsArticles: LiveData<List<NewsArticle>> =
        _trigger.switchMap { trigger ->
            if (trigger.second == true) {
                _dataLoading.value = true
                viewModelScope.launch {
                    newsRepository.refreshFilteredNews()
                    _dataLoading.value = false
                }
            }
            newsRepository
                .filteredNewsArticles
                .distinctUntilChanged()
        }

    val filteredNewsArticles: LiveData<List<NewsArticle>> = _filteredNewsArticles

    fun start(
        targetAuthorities: List<String>?,
        filterUUIDs: List<String>?,
        filterTargets: List<String>?
    ) {
        newsRepository.setFilter(
            targetAuthorities ?: emptyList(),
            filterUUIDs ?: emptyList(),
            filterTargets ?: emptyList()
        )
    }

    val empty: LiveData<Boolean> =
        Transformations.map(_filteredNewsArticles) {
            it.isEmpty()
        }

    private val _openNewsArticleEvent = MutableLiveData<Event<NewsArticle>>()
    val openNewsArticleEvent: LiveData<Event<NewsArticle>> = _openNewsArticleEvent

    init {
        loadNews(true)
    }

    override fun onNewsArticleSelected(newsArticle: NewsArticle) {
        openNews(newsArticle)
    }

    fun openNews(newsArticle: NewsArticle) {
        _openNewsArticleEvent.value = Event(newsArticle)
    }

    override fun onCleared() {
        newsRepository.onViewModelCleared()
    }

    fun refresh() {
        loadNews(true)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun loadNews(forceUpdate: Boolean) {
        _forceUpdate.value = forceUpdate
    }

    fun onModulesUpdated() {
        refresh()
    }
}