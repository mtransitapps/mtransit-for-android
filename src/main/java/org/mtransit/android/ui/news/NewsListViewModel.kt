package org.mtransit.android.ui.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.di.Injection
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import java.util.ArrayList

class NewsListViewModel(savedStateHandle: SavedStateHandle) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = NewsListViewModel::class.java.simpleName

        const val EXTRA_COLOR_INT = "extra_color_int"
        const val EXTRA_SUB_TITLE = "extra_subtitle"
        const val EXTRA_FILTER_TARGET_AUTHORITIES = "extra_filter_target_authorities"
        const val EXTRA_FILTER_TARGETS = "extra_filter_targets"
        const val EXTRA_FILTER_UUIDS = "extra_filter_uuids"
    }

    override fun getLogTag(): String = LOG_TAG

    private val dataSourcesRepository: DataSourcesRepository by lazy { Injection.providesDataSourcesRepository() }

    private val dataSourceRequestManager: DataSourceRequestManager by lazy { Injection.providesDataSourceRequestManager() }

    val colorInt: LiveData<Int?> = savedStateHandle.getLiveData<Int?>(EXTRA_COLOR_INT).distinctUntilChanged()

    val subTitle: LiveData<String?> = savedStateHandle.getLiveData<String?>(EXTRA_SUB_TITLE).distinctUntilChanged()

    private val _targetAuthorities: LiveData<ArrayList<String>?> =
        savedStateHandle.getLiveData<ArrayList<String>?>(EXTRA_FILTER_TARGET_AUTHORITIES).distinctUntilChanged()

    private val _filterTargets: LiveData<ArrayList<String>?> =
        savedStateHandle.getLiveData<ArrayList<String>?>(EXTRA_FILTER_TARGETS).distinctUntilChanged()

    private val _filterUUIDs: LiveData<ArrayList<String>?> =
        savedStateHandle.getLiveData<ArrayList<String>?>(EXTRA_FILTER_UUIDS).distinctUntilChanged()

    private val _filters = TripleMediatorLiveData(_targetAuthorities, _filterTargets, _filterUUIDs)

    private val _allNewsProviders = this.dataSourcesRepository.readingAllNewsProvidersDistinct()

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
        targetAuthorities: ArrayList<String>?,
        filterTargets: ArrayList<String>?,
        filterUUIDs: ArrayList<String>?
    ): List<News>? {
        if (allNewsProviders == null) {
            MTLog.d(this, "getNewsArticles() > SKIP (news providers missing)")
            return null // loading
        }
        val newsArticles = mutableListOf<News>()

        val filter = when {
            !filterUUIDs.isNullOrEmpty() -> {
                NewsProviderContract.Filter.getNewUUIDsFilter(filterUUIDs)
            }
            !filterTargets.isNullOrEmpty() -> {
                NewsProviderContract.Filter.getNewTargetsFilter(filterTargets)
            }
            else -> {
                NewsProviderContract.Filter.getNewEmptyFilter()
            }
        }

        val newsUUIDs = mutableSetOf<String>() // found articles (duplicate can occurs between similar providers in big cities)

        allNewsProviders.filter {
            targetAuthorities == null || targetAuthorities.contains(it.targetAuthority)
        }.forEach { newsProvider ->
            dataSourceRequestManager.findNews(newsProvider.authority, filter)?.forEach { providerNewsArticle ->
                if (!newsUUIDs.contains(providerNewsArticle.uuid)) {
                    newsArticles.add(providerNewsArticle)
                    newsUUIDs.add(providerNewsArticle.uuid)
                }
            }
        }
        newsArticles.sortWith(News.NEWS_COMPARATOR)
        _loading.postValue(false)
        MTLog.d(this, "getNewsArticles() > ${newsArticles.size}")
        return newsArticles
    }
}