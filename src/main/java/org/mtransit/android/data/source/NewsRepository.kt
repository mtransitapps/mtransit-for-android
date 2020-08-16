package org.mtransit.android.data.source

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.CollectionUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.data.DataSourceProvider
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.ui.view.common.MTLoading

var LOG_TAG = NewsRepository::class.java.simpleName

class NewsRepository(
    private val dataSourceProvider: DataSourceProvider,
    private val dataSourceRepository: DataSourceRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DataSourceProvider.ModulesUpdateListener, MTLog.Loggable {

    override fun getLogTag() = LOG_TAG

    private var refreshPending = false

    private val _filter = MutableLiveData<Triple<List<String>?, List<String>?, List<String>?>>()

    val filter: LiveData<Triple<List<String>?, List<String>?, List<String>?>> = _filter

    private val _filteredNewsArticles = MutableLiveData<List<NewsArticle>>()

    val filteredNewsArticles: LiveData<List<NewsArticle>> = _filteredNewsArticles

    private val _loadingNewsArticles = MutableLiveData<MTLoading>(MTLoading(false))

    val loadingNewsArticles: LiveData<MTLoading> = _loadingNewsArticles

    init {
        DataSourceProvider.addModulesUpdateListener(this)
    }

    fun setFilter(
        targetAuthorities: List<String>,
        filterUUIDs: List<String>,
        filterTargets: List<String>
    ): Boolean {
        if (targetAuthorities == _filter.value?.first
            && filterUUIDs == _filter.value?.second
            && filterTargets == _filter.value?.third
        ) {
            MTLog.d(this, "setFilter() > SKIP")
            return false
        }
        _filteredNewsArticles.value = null // clear
        _filter.value = Triple(targetAuthorities, filterUUIDs, filterTargets)
        return true
    }

    suspend fun refreshFilteredNews() {
        if (!dataSourceProvider.isInitialized) {
            refreshPending = true
            return
        }
        doRefreshFilteredNews()
    }

    private suspend fun doRefreshFilteredNews() {
        _loadingNewsArticles.value = MTLoading(true)
        withContext(ioDispatcher) {
            val loadedNews = loadNews(
                _filter.value?.first,
                _filter.value?.second,
                _filter.value?.third
            )
            ensureActive() // do not set wrong loaded news
            _filteredNewsArticles.postValue(loadedNews)
            _loadingNewsArticles.postValue(MTLoading(false))
        }
    }

    private suspend fun loadNews(
        targetAuthorities: List<String>? = null,
        filterUUIDs: List<String>? = null,
        filterTargets: List<String>? = null
    ): List<NewsArticle> {
        return loadNews(
            targetAuthorities,
            if (!filterUUIDs.isNullOrEmpty()) {
                NewsProviderContract.Filter.getNewUUIDsFilter(filterUUIDs)
            } else if (!filterTargets.isNullOrEmpty()) {
                NewsProviderContract.Filter.getNewTargetsFilter(filterTargets)
            } else {
                NewsProviderContract.Filter.getNewEmptyFilter()
            }
        )
    }

    private suspend fun loadNews(
        targetAuthorities: List<String>? = null,
        filter: NewsProviderContract.Filter = NewsProviderContract.Filter.getNewEmptyFilter()
    ): MutableList<NewsArticle> {
        return withContext(ioDispatcher) {
            val doLoadNews = doLoadNews(targetAuthorities, filter)
            ensureActive() // do not set wrong loaded news
            return@withContext doLoadNews
        }
    }

    @WorkerThread
    fun doLoadNews(
        targetAuthorities: List<String>? = null,
        filter: NewsProviderContract.Filter = NewsProviderContract.Filter.getNewEmptyFilter()
    ): MutableList<NewsArticle> {
        val result: MutableList<NewsArticle> = mutableListOf()
        val dsp = DataSourceProvider.getOrInit()
        val newsProviders: MutableList<NewsProviderProperties> = mutableListOf()
        if (targetAuthorities.isNullOrEmpty()) {
            newsProviders.addAll(dsp.allNewsProvider)
        } else {
            targetAuthorities.forEach { targetAuthority ->
                dsp.getTargetAuthorityNewsProviders(targetAuthority)
                    ?.apply {
                        newsProviders.addAll(this)
                    }
            }
        }
        if (newsProviders.isEmpty()) {
            MTLog.d(this, "doLoadNews() > no News provider found (target:$targetAuthorities|filter:$filter)")
            return result
        }
        val newsUUIDs = mutableSetOf<String>()
        newsProviders.forEach { newsProvider ->
            val providerNewsArticles =
                dataSourceRepository.findNews(newsProvider.authority, filter)
            providerNewsArticles?.forEach { providerNewsArticle ->
                if (!newsUUIDs.contains(providerNewsArticle.uUID)) {
                    result.add(providerNewsArticle)
                    newsUUIDs.add(providerNewsArticle.uUID)
                }
            }
        }
        CollectionUtils.sort<NewsArticle>(result, NewsArticle.NEWS_COMPARATOR)
        return result
    }

    suspend fun loadNewsArticle(authority: String?, uuid: String?): NewsArticle? {
        return withContext(ioDispatcher) {
            var result: NewsArticle? = null
            if (authority == null || uuid == null) {
                MTLog.w(this@NewsRepository, "loadNewsArticle() > Unexpected authority '$authority' & uuid '$uuid'!")
                return@withContext result
            }
            val newsFilter = NewsProviderContract.Filter.getNewUUIDFilter(uuid)
            result = dataSourceRepository.findNewsArticle(authority, newsFilter)
            return@withContext result
        }
    }

    override fun onModulesUpdated() {
        if (refreshPending) {
            refreshPending = false
        }
    }

    fun onViewModelCleared() {
        refreshPending = false
    }
}