package org.mtransit.android.ui.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.data.source.NewsRepository

class NewsListViewModel(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _forceUpdate = MutableLiveData<Boolean>(false)

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _newsArticles: LiveData<List<NewsArticle>> = _forceUpdate.switchMap { forceUpdate ->
        if (forceUpdate) {
            _dataLoading.value = true
            viewModelScope.launch {
                newsRepository.refreshNews()
                _dataLoading.value = false
            }
        }
        newsRepository
            .newsArticles
            .distinctUntilChanged()
    }

    val newsArticles: LiveData<List<NewsArticle>> = _newsArticles

    val items = newsArticles

    val empty: LiveData<Boolean> =
        Transformations.map(_newsArticles) {
            it.isEmpty()
        }

    init {
        loadNews(true)
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