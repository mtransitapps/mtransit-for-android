package org.mtransit.android.ui.news.details

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
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.PairMediatorLiveData
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class NewsDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = NewsDetailsViewModel::class.java.simpleName

        internal const val EXTRA_AUTHORITY = "extra_agency_authority"
        internal const val EXTRA_NEWS_UUID = "extra_news_uuid"
    }

    override fun getLogTag(): String = LOG_TAG

    val uuid = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_NEWS_UUID)

    private val authority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_AUTHORITY)

    val dataSourceRemovedEvent = MutableLiveData<Event<Boolean>>()

    private val allNewsProviders = this.dataSourcesRepository.readingAllNewsProviders() // #onModulesUpdated

    private val thisNewsProvider: LiveData<NewsProviderProperties?> = PairMediatorLiveData(allNewsProviders, authority).map { (allNewsProviders, authority) ->
        if (allNewsProviders != null && authority != null) {
            allNewsProviders.firstOrNull { it.authority == authority }
        } else {
            null
        }
    }

    val newsArticle: LiveData<News?> = PairMediatorLiveData(uuid, thisNewsProvider).switchMap { (uuid, thisNewsProvider) ->
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            emit(getNewsArticle(uuid, thisNewsProvider))
        }
    }

    private fun getNewsArticle(uuid: String?, thisNewsProvider: NewsProviderProperties?): News? {
        if (uuid == null) {
            return null
        }
        if (thisNewsProvider == null) {
            if (newsArticle.value != null) {
                MTLog.d(this, "getNewsArticle() > data source removed (no more agency)")
                dataSourceRemovedEvent.postValue(Event(true))
            }
            return null
        }
        val newNewsArticle = this.dataSourceRequestManager.findANews(thisNewsProvider.authority, NewsProviderContract.Filter.getNewUUIDFilter(uuid))
        if (newNewsArticle == null) {
            MTLog.d(this, "getNewsArticle() > data source updated (no more news article)")
            dataSourceRemovedEvent.postValue(Event(true))
        }
        return newNewsArticle
    }
}