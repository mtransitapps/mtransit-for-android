package org.mtransit.android.ui.news.viewer.page

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.data.source.NewsRepository

class NewsArticleViewModel(
    newsRepository: NewsRepository
) : ViewModel(), MTLog.Loggable {

    companion object {
        val LOG_TAG: String = NewsArticleViewModel::class.java.simpleName
    }

    override fun getLogTag() = "${LOG_TAG}-${_authorityAndUUID.value?.second}"

    private val _authorityAndUUID = MutableLiveData<Pair<String, String>>()

    private val _newsArticle: LiveData<NewsArticle?> =
        _authorityAndUUID.switchMap { authorityAndUUID ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(
                    newsRepository.loadNewsArticle(
                        authorityAndUUID.first,
                        authorityAndUUID.second
                    )
                )
            }
        }

    val newsArticle: LiveData<NewsArticle?> = _newsArticle

    fun onModulesUpdated() {
    }

    fun start(authority: String?, uuid: String?) {
        if (authority.isNullOrEmpty() || uuid.isNullOrEmpty()) {
            MTLog.w(this, "start() > Unexpected authority '$authority' & uuid '$uuid'!")
            return // SKIP
        }
        if (authority == _authorityAndUUID.value?.first
            && uuid == _authorityAndUUID.value?.second
        ) {
            MTLog.d(this, "start() > SKIP (same UUID '$uuid' & authority '$authority')")
            return // SKIP
        }
        _authorityAndUUID.value = Pair(authority, uuid)
    }
}