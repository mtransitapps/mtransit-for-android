package org.mtransit.android.ui.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.NewsRepository
import org.mtransit.android.ui.view.common.TripleMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class NewsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newsRepository: NewsRepository,
    private val dataSourcesRepository: DataSourcesRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = NewsListViewModel::class.java.simpleName

        internal const val EXTRA_COLOR = "extra_color"
        internal val EXTRA_COLOR_DEFAULT: String? = null
        internal const val EXTRA_SUB_TITLE = "extra_subtitle"
        internal const val EXTRA_FILTER_TARGET_AUTHORITIES = "extra_filter_target_authorities"
        internal val EXTRA_FILTER_TARGET_AUTHORITIES_DEFAULT: Array<String> = emptyArray()
        internal const val EXTRA_FILTER_TARGETS = "extra_filter_targets"
        internal val EXTRA_FILTER_TARGETS_DEFAULT: Array<String> = emptyArray()
        internal const val EXTRA_FILTER_UUIDS = "extra_filter_uuids"
        internal val EXTRA_FILTER_UUIDS_DEFAULT: Array<String> = emptyArray()
    }

    override fun getLogTag(): String = LOG_TAG

    val colorInt = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_COLOR, EXTRA_COLOR_DEFAULT).map { it?.let { ColorUtils.parseColor(it) } }

    val subTitle = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_SUB_TITLE)

    private val _targetAuthorities = savedStateHandle.getLiveDataDistinct(EXTRA_FILTER_TARGET_AUTHORITIES, EXTRA_FILTER_TARGET_AUTHORITIES_DEFAULT)

    private val _filterTargets = savedStateHandle.getLiveDataDistinct(EXTRA_FILTER_TARGETS, EXTRA_FILTER_TARGETS_DEFAULT)

    private val _filterUUIDs = savedStateHandle.getLiveDataDistinct(EXTRA_FILTER_UUIDS, EXTRA_FILTER_UUIDS_DEFAULT)

    private val _filters = TripleMediatorLiveData(_targetAuthorities, _filterTargets, _filterUUIDs)

    private val _allNewsProviders = this.dataSourcesRepository.readingAllNewsProviders() // #onModulesUpdated

    private val _refreshRequestedTrigger = MutableLiveData<Int>() // no initial value to avoid triggering onChanged()

    fun onRefreshRequested() {
        MTLog.d(this, "onRefreshRequested() > trigger refresh")
        _refreshRequestedTrigger.value = (_refreshRequestedTrigger.value ?: 0) + 1
    }

    private val _loading = MutableLiveData(true)

    val loading: LiveData<Boolean> = _loading

    val newsArticles: LiveData<List<News>?> =
        TripleMediatorLiveData(_allNewsProviders, _filters, _refreshRequestedTrigger).switchMap { (allNewsProviders, filters) ->
            _loading.value = true
            newsRepository.loadingNewsArticles(
                allNewsProviders,
                filters?.first,
                filters?.second,
                filters?.third,
                News.NEWS_COMPARATOR,
                onSuccess = {
                    _loading.postValue(false)
                },
                context = viewModelScope.coroutineContext + Dispatchers.IO,
            )
        }
}