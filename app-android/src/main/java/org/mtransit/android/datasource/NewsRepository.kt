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
        private val LOG_TAG = NewsRepository::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

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
            NewsProviderContract.Filter
                .getNewTargetFilter(poi)
                .setMinCreatedAtInMs(
                    UITimeUtils.currentTimeMillis() -
                            if (demoModeManager.enabled) {
                                TimeUnit.DAYS.toMillis(365L)
                            } else {
                                TimeUnit.DAYS.toMillis(100L)
                            }
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
        filterTargets: List<String>?,
        filterUUIDs: List<String>?,
        comparator: Comparator<News>,
        firstLoad: Boolean,
        let: ((List<News>) -> List<News>?) = { it },
        onSuccess: (() -> Unit)? = null,
        coroutineContext: CoroutineContext,
    ) = loadingNewsArticles(
        providers = allProviders?.filter {
            targetProviderAuthorities != null // SKIP
                    && (targetProviderAuthorities.isEmpty() || targetProviderAuthorities.contains(it.targetAuthority))
        },
        filter = when {
            filterUUIDs == null || filterTargets == null -> null // SKIP
            filterUUIDs.isNotEmpty() -> NewsProviderContract.Filter.getNewUUIDsFilter(filterUUIDs)
            filterTargets.isNotEmpty() -> NewsProviderContract.Filter.getNewTargetsFilter(filterTargets)
            else -> NewsProviderContract.Filter.getNewEmptyFilter()
        },
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
        if (providers == null || filter == null) {
            return@liveData // SKIP
        }
        // 1 - cache only
        if (firstLoad) {
            emit(loadNewsArticles(providers, filter.setCacheOnly(true), comparator, let))
        }
        // 2 - look for new news
        emit(loadNewsArticles(providers, filter.setCacheOnly(false), comparator, let))
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
        uuid: String?,
        provider: NewsProviderProperties?,
        onMissingProvider: ((oldNews: News?) -> (Unit)) = {},
        onNewsLoaded: ((loadedNews: News?) -> (Unit)) = {},
        coroutineContext: CoroutineContext,
    ) = loadingNewsArticle(
        provider,
        uuid?.let { NewsProviderContract.Filter.getNewUUIDFilter(uuid) },
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