package org.mtransit.android.datasource

import androidx.lifecycle.liveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.util.UITimeUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Singleton
class NewsRepository(
    val dataSourceRequestManager: DataSourceRequestManager,
    val demoModeManager: DemoModeManager,
    private val ioDispatcher: CoroutineDispatcher,
) : MTLog.Loggable {

    @Inject
    constructor(
        dataSourceRequestManager: DataSourceRequestManager,
        demoModeManager: DemoModeManager,
    ) : this(
        dataSourceRequestManager,
        demoModeManager,
        Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG = NewsRepository::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    fun loadingNewsArticles(
        providers: List<NewsProviderProperties>?,
        poi: POI?,
        comparator: Comparator<News>,
        let: ((List<News>) -> List<News>?) = { it },
        onSuccess: (() -> Unit)? = null,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
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
        let,
        onSuccess,
        coroutineContext,
    )

    fun loadingNewsArticles(
        allProviders: Iterable<NewsProviderProperties>?,
        targetProviderAuthorities: Array<String>?,
        filterTargets: Array<String>?,
        filterUUIDs: Array<String>?,
        comparator: Comparator<News>,
        let: ((List<News>) -> List<News>?) = { it },
        onSuccess: (() -> Unit)? = null,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
    ) = loadingNewsArticles(
        providers = allProviders?.filter {
            targetProviderAuthorities != null // SKIP
                    && (targetProviderAuthorities.isEmpty() || targetProviderAuthorities.contains(it.targetAuthority))
        },
        filter = when {
            filterUUIDs == null || filterTargets == null -> null // SKIP
            filterUUIDs.isNotEmpty() -> NewsProviderContract.Filter.getNewUUIDsFilter(filterUUIDs.toList())
            filterTargets.isNotEmpty() -> NewsProviderContract.Filter.getNewTargetsFilter(filterTargets.toList())
            else -> NewsProviderContract.Filter.getNewEmptyFilter()
        },
        comparator,
        let,
        onSuccess,
        coroutineContext,
    )

    private fun loadingNewsArticles(
        providers: List<NewsProviderProperties>?,
        filter: NewsProviderContract.Filter?,
        comparator: Comparator<News>,
        let: ((List<News>) -> List<News>?) = { it },
        onSuccess: (() -> Unit)? = null,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
    ) = liveData(coroutineContext) {
        if (providers == null || filter == null) {
            return@liveData // SKIP
        }
        emit(loadNewsArticles(providers, filter, comparator, let, coroutineContext))
        onSuccess?.invoke()
    }

    private suspend fun loadNewsArticles(
        providers: List<NewsProviderProperties>,
        filter: NewsProviderContract.Filter,
        comparator: Comparator<News>,
        let: ((List<News>) -> List<News>?) = { it },
        coroutineContext: CoroutineContext = ioDispatcher,
    ) = withContext(coroutineContext) {
        providers
            .map { provider ->
                async {
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
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
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
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
    ) = liveData(coroutineContext) {
        if (filter == null) {
            return@liveData // SKIP
        }
        if (provider == null) {
            onMissingProvider(this.latestValue)
            return@liveData // SKIP
        }
        val loadedNewsArticle = loadNewsArticle(provider, filter, coroutineContext)
        onNewsLoaded(loadedNewsArticle)
        emit(loadedNewsArticle)
    }

    private suspend fun loadNewsArticle(
        provider: NewsProviderProperties,
        filter: NewsProviderContract.Filter,
        context: CoroutineContext = ioDispatcher,
    ) = withContext(context) {
        dataSourceRequestManager.findNews(provider, filter)?.firstOrNull()
    }
}