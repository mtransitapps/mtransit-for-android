package org.mtransit.android.data.source

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

var LOG_TAG = NewsRepository::class.java.simpleName

class NewsRepository(
    private val dataSourceProvider: DataSourceProvider,
    private val dataSourceRepository: DataSourceRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DataSourceProvider.ModulesUpdateListener, MTLog.Loggable {

    override fun getLogTag() = LOG_TAG

    private var refreshPending = false

    private val _newsArticles: MutableLiveData<List<NewsArticle>> = MutableLiveData()

    val newsArticles: LiveData<List<NewsArticle>> = _newsArticles

    init {
        DataSourceProvider.addModulesUpdateListener(this)
    }

    suspend fun refreshNews() {
        if (!dataSourceProvider.isInitialized) {
            refreshPending = true
            return
        }
        doRefreshNews()
    }

    private suspend fun doRefreshNews() {
        _newsArticles.value = withContext(ioDispatcher) {
            val result: MutableList<NewsArticle> = mutableListOf()
            val dsp = DataSourceProvider.getOrInit()
            val newsProviders = dsp.allNewsProvider
            if (newsProviders.isEmpty()) {
                MTLog.d(
                    this@NewsRepository,
                    "loadInBackground() > no News provider found"
                )
                return@withContext result
            }
            val newsUUIDs = mutableSetOf<String>()
            newsProviders.forEach { newsProvider ->
                val newsFilter = NewsProviderContract.Filter.getNewEmptyFilter()
                val providerNews =
                    dataSourceRepository.findNews(newsProvider.authority, newsFilter)
                providerNews?.forEach { providerNewsArticle ->
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