package org.mtransit.android.ui.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class NewsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = NewsListViewModel::class.java.simpleName

        internal const val EXTRA_COLOR = "extra_color"
        internal val EXTRA_COLOR_DEFAULT: String? = null
        internal const val EXTRA_SUB_TITLE = "extra_subtitle"
        internal const val EXTRA_FILTER_TARGET_AUTHORITIES = "extra_filter_target_authorities"
        internal const val EXTRA_FILTER_TARGETS = "extra_filter_targets"
        internal const val EXTRA_FILTER_UUIDS = "extra_filter_uuids"
    }

    override fun getLogTag(): String = LOG_TAG

    val colorInt = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_COLOR, EXTRA_COLOR_DEFAULT).map { it?.let { ColorUtils.parseColor(it) } }

    val subTitle = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_SUB_TITLE)

    private val _targetAuthorities = savedStateHandle.getLiveDataDistinct<Array<String>?>(EXTRA_FILTER_TARGET_AUTHORITIES)

    private val _filterTargets = savedStateHandle.getLiveDataDistinct<Array<String>?>(EXTRA_FILTER_TARGETS)

    private val _filterUUIDs = savedStateHandle.getLiveDataDistinct<Array<String>?>(EXTRA_FILTER_UUIDS)

    private val _filters = TripleMediatorLiveData(_targetAuthorities, _filterTargets, _filterUUIDs)

    private val _allNewsProviders = this.dataSourcesRepository.readingAllNewsProviders() // #onModulesUpdated

    private val _refreshRequestedTrigger = MutableLiveData(0)

    fun onRefreshRequested() {
        MTLog.d(this, "onRefreshRequested() > trigger refresh")
        _refreshRequestedTrigger.value = (_refreshRequestedTrigger.value ?: 0) + 1
    }

    private val _loading = MutableLiveData(true)

    val loading: LiveData<Boolean> = _loading

    val newsArticles: LiveData<List<News>?> =
        TripleMediatorLiveData(_allNewsProviders, _filters, _refreshRequestedTrigger).switchMap { (allNewsProviders, filters) ->
            _loading.value = true
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(
                    getNewsArticles(
                        allNewsProviders,
                        filters?.first,
                        filters?.second,
                        filters?.third
                    )
                )
            }
        }

    private fun getNewsArticles(
        allNewsProviders: List<NewsProviderProperties>?,
        targetAuthorities: Array<String>?,
        filterTargets: Array<String>?,
        filterUUIDs: Array<String>?
    ): List<News>? {
        if (allNewsProviders == null) {
            MTLog.d(this, "getNewsArticles() > SKIP (news providers missing)")
            return null // loading
        }
        val newsArticles = mutableListOf<News>()

        val filter = when {
            !filterUUIDs.isNullOrEmpty() -> {
                NewsProviderContract.Filter.getNewUUIDsFilter(filterUUIDs.toList())
            }
            !filterTargets.isNullOrEmpty() -> {
                NewsProviderContract.Filter.getNewTargetsFilter(filterTargets.toList())
            }
            else -> {
                NewsProviderContract.Filter.getNewEmptyFilter()
            }
        }

        val newsUUIDs = mutableSetOf<String>() // found articles (duplicate can occurs between similar providers in big cities)

        allNewsProviders.filter {
            targetAuthorities == null || targetAuthorities.contains(it.targetAuthority)
        }.forEach { newsProvider ->
            dataSourceRequestManager.findNews(newsProvider, filter)?.forEach { providerNewsArticle ->
                if (!newsUUIDs.contains(providerNewsArticle.uuid)) {
                    newsArticles.add(providerNewsArticle)
                    newsUUIDs.add(providerNewsArticle.uuid)
                }
            }
        }
        newsArticles.sortWith(News.NEWS_COMPARATOR)
        _loading.postValue(false)
        return newsArticles
    }
}