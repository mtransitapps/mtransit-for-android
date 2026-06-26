package org.mtransit.android.datasource

import androidx.lifecycle.liveData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.provider.news.NewsProviderContract
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.util.UITimeUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class NewsRepository @Inject constructor(
    val dataSourceRequestManager: DataSourceRequestManager,
    val demoModeManager: DemoModeManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = NewsRepository::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    fun loadingNewsArticles(
        providers: List<NewsProviderProperties>?,
        poi: POI?,
        comparator: Comparator<News>,
        firstLoad: Boolean,
        let: ((List<News>) -> List<News>?) = { it },
        onSuccess: (() -> Unit)? = null,
        coroutineContext: CoroutineContext,
    ) = loadingNewsArticles(
        providers,
        poi?.let {
            NewsProviderContract.Filter.newPOIFilter(poi)
                .copy(
                    inFocus = true,  // focuses on POI
                    minCreatedAtInMs = UITimeUtils.currentTimeMillis() - TimeUnit.DAYS.toMillis(if (demoModeManager.enabled) 365L else 100L),
                )
        },
        comparator,
        firstLoad,
        let,
        onSuccess,
        coroutineContext,
    )

    fun loadingNewsArticles(
        allProviders: Iterable<NewsProviderProperties>?,
        targetProviderAuthorities: List<String>?,
        filterTargetUUIDs: List<String>?,
        filterArticleUUIDs: List<String>?,
        comparator: Comparator<News>,
        firstLoad: Boolean,
        inFocus: Boolean = false,
        let: ((List<News>) -> List<News>?) = { it },
        onSuccess: (() -> Unit)? = null,
        coroutineContext: CoroutineContext,
    ) = loadingNewsArticles(
        providers = allProviders?.filter {
            targetProviderAuthorities != null // SKIP (bundle extra not read yet)
                    && (targetProviderAuthorities.isEmpty() || targetProviderAuthorities.contains(it.targetAuthority))
        },
        filter = when {
            filterArticleUUIDs == null || filterTargetUUIDs == null -> null // SKIP (bundle extra not read yet)
            filterArticleUUIDs.isNotEmpty() -> NewsProviderContract.Filter(articlesUUIDs = filterArticleUUIDs)
            filterTargetUUIDs.isNotEmpty() -> NewsProviderContract.Filter(targetsUUIDs = filterTargetUUIDs)
            else -> NewsProviderContract.Filter.newEmptyFilter()
        }?.copy(inFocus = inFocus),
        comparator,
        firstLoad,
        let,
        onSuccess,
        coroutineContext,
    )

    private fun loadingNewsArticles(
        providers: List<NewsProviderProperties>?,
        filter: NewsProviderContract.Filter?,
        comparator: Comparator<News>,
        firstLoad: Boolean,
        let: ((List<News>) -> List<News>?) = { it },
        onSuccess: (() -> Unit)? = null,
        coroutineContext: CoroutineContext,
    ) = liveData(coroutineContext) {
        if (providers == null || filter == null) return@liveData // SKIP (bundle extra not read yet)
        // 1 - cache only
        if (firstLoad) {
            emit(loadNewsArticles(providers, filter.copy(cacheOnly = true), comparator, let))
        }
        // 2 - look for new news
        emit(loadNewsArticles(providers, filter.copy(cacheOnly = false), comparator, let))
        onSuccess?.invoke()
    }

    private suspend fun loadNewsArticles(
        providers: List<NewsProviderProperties>,
        filter: NewsProviderContract.Filter,
        comparator: Comparator<News>,
        let: ((List<News>) -> List<News>?) = { it },
    ): List<News>? = coroutineScope {
        providers
            .map { provider ->
                async<List<News>?> {
                    ensureActive()
                    dataSourceRequestManager.findNews(provider, filter)
                }
            }
            .awaitAll()
            .filterNotNull()
            .flatten()
            .distinctBy { it.uuid }
            .sortedWith(comparator)
            .let { let.invoke(it) }
    }

    fun loadingNewsArticle(
        articleUUID: String?,
        provider: NewsProviderProperties?,
        onMissingProvider: ((oldNews: News?) -> (Unit)) = {},
        onNewsLoaded: ((loadedNews: News?) -> (Unit)) = {},
        coroutineContext: CoroutineContext,
    ) = loadingNewsArticle(
        provider,
        articleUUID?.let { NewsProviderContract.Filter.newArticleUUIDFilter(articleUUID) },
        onMissingProvider,
        onNewsLoaded,
        coroutineContext,
    )

    private fun loadingNewsArticle(
        provider: NewsProviderProperties?,
        filter: NewsProviderContract.Filter?,
        onMissingProvider: ((oldNews: News?) -> (Unit)) = {},
        onNewsLoaded: ((loadedNews: News?) -> (Unit)) = {},
        coroutineContext: CoroutineContext,
    ) = liveData(coroutineContext) {
        if (filter == null) {
            return@liveData // SKIP
        }
        if (provider == null) {
            onMissingProvider(this.latestValue)
            return@liveData // SKIP
        }
        val loadedNewsArticle = loadNewsArticle(provider, filter)
        onNewsLoaded(loadedNewsArticle)
        emit(loadedNewsArticle)
    }

    private suspend fun loadNewsArticle(
        provider: NewsProviderProperties,
        filter: NewsProviderContract.Filter,
    ) = dataSourceRequestManager.findNews(provider, filter)?.firstOrNull()
}