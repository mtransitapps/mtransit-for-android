package org.mtransit.android.data.source

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.CollectionUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.data.DataSourceProvider
import org.mtransit.android.data.NewsProviderProperties

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

    init {
        DataSourceProvider.addModulesUpdateListener(this)
    }

    fun setFilter(
        targetAuthorities: List<String>,
        filterUUIDs: List<String>,
        filterTargets: List<String>
    ) {
        if (targetAuthorities == _filter.value?.first
            && filterUUIDs == filter.value?.second
            && filterTargets == filter.value?.third
        ) {
            MTLog.d(
                this,
                "setFilter() > SKIP"
            )
            return
        }
        _filter.value = Triple(targetAuthorities, filterUUIDs, filterTargets)
    }

    suspend fun refreshFilteredNewsIfEmpty() {
        if (_filteredNewsArticles.value.isNullOrEmpty()) {
            refreshFilteredNews()
        }
    }

    suspend fun refreshFilteredNews() {
        if (!dataSourceProvider.isInitialized) {
            refreshPending = true
            return
        }
        doRefreshFilteredNews()
    }

    private suspend fun doRefreshFilteredNews() {
        _filteredNewsArticles.value = loadNews(
            _filter.value?.first,
            _filter.value?.second,
            _filter.value?.third
        )
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
            return@withContext doLoadNews(targetAuthorities, filter)
        }
    }

    @WorkerThread
    fun doLoadNews(
        targetAuthority: String? = null,
        filter: NewsProviderContract.Filter = NewsProviderContract.Filter.getNewEmptyFilter()
    ): MutableList<NewsArticle> {
        return doLoadNews(
            if (targetAuthority.isNullOrEmpty()) null else listOf(targetAuthority),
            filter
        )
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
            MTLog.d(
                this,
                "doLoadNews() > no News provider found (target:$targetAuthorities|filter:$filter)"
            )
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
        CollectionUtils.sort<NewsArticle>(
            result,
            NewsArticle.NEWS_COMPARATOR
        )
        return result
    }

    suspend fun loadNewsArticle(authority: String?, uuid: String?): NewsArticle? {
        return withContext(ioDispatcher) {
            var result: NewsArticle? = null
            if (authority == null || uuid == null) {
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