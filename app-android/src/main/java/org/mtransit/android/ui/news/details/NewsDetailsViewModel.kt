package org.mtransit.android.ui.news.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.NewsRepository
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class NewsDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val newsRepository: NewsRepository,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = NewsDetailsViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_NEWS_UUID = "extra_news_uuid"
    }

    override fun getLogTag() = LOG_TAG

    val uuid = savedStateHandle.getLiveDataDistinct<String>(EXTRA_NEWS_UUID)

    val authority = savedStateHandle.getLiveDataDistinct<String>(EXTRA_AUTHORITY)

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    private val allNewsProviders = this.dataSourcesRepository.readingAllNewsProviders() // #onModulesUpdated

    private val thisNewsProvider: LiveData<NewsProviderProperties?> = MediatorLiveData2(allNewsProviders, authority).map { (allNewsProviders, authority) ->
        if (allNewsProviders != null && authority != null) {
            allNewsProviders.firstOrNull { it.authority == authority }
        } else {
            null
        }
    }

    val newsArticle: LiveData<News?> = MediatorLiveData2(uuid, thisNewsProvider).switchMap { (uuid, thisNewsProvider) ->
        newsRepository.loadingNewsArticle(
            uuid,
            thisNewsProvider,
            onMissingProvider = { oldNews ->
                if (oldNews != null) {
                    MTLog.d(this, "getNewsArticle() > data source removed (no more agency)")
                    dataSourceRemovedEvent.postValue(Event(true))
                }
            },
            onNewsLoaded = { loadedNews ->
                if (loadedNews == null) {
                    MTLog.d(this, "getNewsArticle() > data source updated (no more news article)")
                    dataSourceRemovedEvent.postValue(Event(true))
                }
            },
            coroutineContext = viewModelScope.coroutineContext + Dispatchers.IO,
        )
    }
}