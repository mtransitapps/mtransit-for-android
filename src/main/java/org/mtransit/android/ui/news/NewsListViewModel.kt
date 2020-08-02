package org.mtransit.android.ui.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.util.UITimeUtils
import java.util.concurrent.TimeUnit

const val CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY = "CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY"
const val CURRENT_NEWS_UUID_SAVED_STATE_KEY = "CURRENT_NEWS_UUID_SAVED_STATE_KEY"

class NewsListViewModel(
    private val newsRepository: NewsRepository,
    private val savedStateHandle: SavedStateHandle
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

    var selectionFilterMaxNoteworthiness: Int = 7

    var selectionFilterOldestCreatedAtInMs: Long =
        UITimeUtils.currentTimeMillis() - TimeUnit.DAYS.toMillis(14L)

    val selectedNewsArticles: LiveData<List<NewsArticle>> =
        Transformations.map(_filteredNewsArticles) { newsArticles ->
            val sortedNewsArticles = newsArticles
                .filter {
                    it.createdAtInMs > selectionFilterOldestCreatedAtInMs
                }
                .sortedWith(NewsArticle.NEWS_SEVERITY_COMPARATOR)
            val minSelectedArticles =
                if (sortedNewsArticles.size > 1) 2 else 1  // encourage 2+ articles
            val nowInMs = UITimeUtils.currentTimeMillis()
            var noteworthiness = 1
            val newSelectedNewsArticle = mutableListOf<NewsArticle>()
            while (newSelectedNewsArticle.size < minSelectedArticles
                && noteworthiness < selectionFilterMaxNoteworthiness
            ) {
                sortedNewsArticles.forEach { newsArticle ->
                    val validityInMs: Long =
                        newsArticle.createdAtInMs + newsArticle.noteworthyInMs * noteworthiness
                    if (validityInMs >= nowInMs) {
                        if (!newSelectedNewsArticle.contains(newsArticle)) {
                            newSelectedNewsArticle.add(newsArticle)
                        }
                    }
                }
                noteworthiness++
            }
            newSelectedNewsArticle
        }

    init {
        setCurrentNews(
            getSavedCurrentNewsAuthority(),
            getSavedCurrentNewsUUID()
        )
    }

    private fun setCurrentNews(authority: String?, uuid: String?) {
        savedStateHandle.set(
            CURRENT_NEWS_AUTHORITY_SAVED_STATE_KEY,
            authority
        )
        savedStateHandle.set(
            CURRENT_NEWS_UUID_SAVED_STATE_KEY,
            uuid
        )
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

    fun onPageSelected(newsArticle: NewsArticle) {
        onPageSelected(newsArticle.authority, newsArticle.uUID)
    }

    fun onPageSelected(authority: String?, uuid: String?) {
        if (authority == getSavedCurrentNewsAuthority()
            && uuid == getSavedCurrentNewsUUID()
        ) {
            MTLog.d(this, "start() > SKIP")
            return
        }
        setCurrentNews(authority, uuid)
    }

    fun setFilter(
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

    private fun openNews(newsArticle: NewsArticle) {
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