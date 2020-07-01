package org.mtransit.android.ui.news.viewer.page

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.commons.data.NewsArticle
import org.mtransit.android.data.source.NewsRepository
import org.mtransit.android.ui.view.common.PairMediatorLiveData

class NewsViewerPageViewModel(
    newsRepository: NewsRepository
) : ViewModel() {

    private val _authority = MutableLiveData<String>()
    private val _newsUUID = MutableLiveData<String>()

    private val _authorityAndUUID = PairMediatorLiveData(_authority, _newsUUID)

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
        TODO("Not yet implemented")
    }

    fun start(authority: String?, uuid: String?) {
        if (authority == _authority.value || uuid == _newsUUID.value) {
            return
        }
        _authority.value = authority
        _newsUUID.value = uuid
    }
}