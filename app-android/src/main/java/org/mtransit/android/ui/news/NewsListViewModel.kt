package org.mtransit.android.ui.news

import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.isAppEnabled
import org.mtransit.android.data.AUTHORITY_INVALID
import org.mtransit.android.data.AuthorityAndUuid
import org.mtransit.android.data.UUID_INVALID
import org.mtransit.android.data.authorityAndUuidT
import org.mtransit.android.data.getAuthority
import org.mtransit.android.data.getUuid
import org.mtransit.android.data.hasVideo
import org.mtransit.android.data.isAuthorityAndUuidValid
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.datasource.NewsRepository
import org.mtransit.android.ui.inappnotification.moduledisabled.ModuleDisabledAwareViewModel
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.MediatorLiveData3
import org.mtransit.android.ui.view.common.getLiveDataDistinct
import javax.inject.Inject

@HiltViewModel
class NewsListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val newsRepository: NewsRepository,
    private val dataSourcesRepository: DataSourcesRepository,
    private val adManager: IAdManager,
    private val pm: PackageManager,
) : ViewModel(),
    ModuleDisabledAwareViewModel,
    MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = NewsListViewModel::class.java.simpleName

        internal const val EXTRA_COLOR = "extra_color"
        internal val EXTRA_COLOR_DEFAULT: String? = null
        internal const val EXTRA_SUB_TITLE = "extra_subtitle"
        internal const val EXTRA_FILTER_TARGET_AUTHORITIES = "extra_filter_target_authorities"
        internal val EXTRA_FILTER_TARGET_AUTHORITIES_DEFAULT: Array<String> = emptyArray()
        internal const val EXTRA_FILTER_TARGETS = "extra_filter_targets"
        internal val EXTRA_FILTER_TARGETS_DEFAULT: Array<String> = emptyArray()
        internal const val EXTRA_FILTER_UUIDS = "extra_filter_uuids"
        internal val EXTRA_FILTER_UUIDS_DEFAULT: Array<String> = emptyArray()

        internal const val EXTRA_SELECTED_ARTICLE_AUTHORITY = "extra_selected_article_agency_authority"
        internal const val EXTRA_SELECTED_ARTICLE_UUID = "extra_selected_article_uuid"
        internal const val EXTRA_FULL_SCREEN_MODE = "extra_full_screen"
        internal const val EXTRA_FULL_SCREEN_MODE_DEFAULT = false
    }

    override fun getLogTag() = LOG_TAG

    val colorInt = savedStateHandle.getLiveDataDistinct(EXTRA_COLOR, EXTRA_COLOR_DEFAULT)
        .map { it?.let { ColorUtils.parseColor(it) } }

    val subTitle = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_SUB_TITLE)

    private val _targetAuthorities = savedStateHandle.getLiveDataDistinct(EXTRA_FILTER_TARGET_AUTHORITIES, EXTRA_FILTER_TARGET_AUTHORITIES_DEFAULT)

    private val _filterTargets = savedStateHandle.getLiveDataDistinct(EXTRA_FILTER_TARGETS, EXTRA_FILTER_TARGETS_DEFAULT)

    private val _filterUUIDs = savedStateHandle.getLiveDataDistinct(EXTRA_FILTER_UUIDS, EXTRA_FILTER_UUIDS_DEFAULT)

    private val _filters = MediatorLiveData3(_targetAuthorities, _filterTargets, _filterUUIDs).map {
        Filters(it.first?.toList(), it.second?.toList(), it.third?.toList())
    }.distinctUntilChanged()

    private val _allNewsProviders = this.dataSourcesRepository.readingAllNewsProviders() // #onModulesUpdated

    private val _refreshRequestedTrigger = MutableLiveData<Int>() // no initial value to avoid triggering onChanged()

    private val _selectedNewsArticleAgencyAuthority = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_SELECTED_ARTICLE_AUTHORITY)

    private val _selectedNewsArticleUUID = savedStateHandle.getLiveDataDistinct<String?>(EXTRA_SELECTED_ARTICLE_UUID)

    val selectedNewsArticleAuthorityAndUUID = MediatorLiveData2(_selectedNewsArticleAgencyAuthority, _selectedNewsArticleUUID).map { (authority, uuid) ->
        authority?.let {
            uuid?.let {
                AuthorityAndUuid(authority, uuid)
            }
        }
    }.distinctUntilChanged()

    private var _lastReadArticleAuthorityAndUUID = MutableLiveData<AuthorityAndUuid?>(null)

    val lastReadArticleAuthorityAndUUID: LiveData<AuthorityAndUuid?> = _lastReadArticleAuthorityAndUUID.distinctUntilChanged()

    fun onRefreshRequested() {
        MTLog.d(this, "onRefreshRequested() > trigger refresh")
        _refreshRequestedTrigger.value = (_refreshRequestedTrigger.value ?: 0) + 1
    }

    private val _loading = MutableLiveData(true)

    val loading: LiveData<Boolean> = _loading

    val newsArticles: LiveData<List<News>?> =
        MediatorLiveData3(_allNewsProviders, _filters, _refreshRequestedTrigger).switchMap { (allNewsProviders, filters, trigger) ->
            _loading.value = true
            newsRepository.loadingNewsArticles(
                allProviders = allNewsProviders,
                targetProviderAuthorities = filters?.targetAuthorities,
                filterTargets = filters?.filterTargets,
                filterUUIDs = filters?.filterUUIDs,
                comparator = News.NEWS_COMPARATOR,
                firstLoad = newsArticles.value == null,
                onSuccess = {
                    _loading.postValue(false)
                },
                coroutineContext = viewModelScope.coroutineContext + Dispatchers.IO,
            )
        }

    fun cleanSelectedNewsArticle() = onNewsArticleSelected(null)

    fun onNewsArticleSelected(newAuthorityAndUuid: AuthorityAndUuid?) {
        // 1st: make sure it's null to avoid INVALID authority+uuid pair
        val newAuthority = newAuthorityAndUuid?.getAuthority()
        val newUuid = newAuthorityAndUuid?.getUuid()
        if (newAuthority != null && newAuthority != savedStateHandle[EXTRA_SELECTED_ARTICLE_AUTHORITY]) {
            savedStateHandle[EXTRA_SELECTED_ARTICLE_AUTHORITY] = AUTHORITY_INVALID
        }
        if (newUuid != null && newUuid != savedStateHandle[EXTRA_SELECTED_ARTICLE_UUID]) {
            savedStateHandle[EXTRA_SELECTED_ARTICLE_UUID] = UUID_INVALID
        }
        // 2nd: set the new value
        savedStateHandle[EXTRA_SELECTED_ARTICLE_AUTHORITY] = newAuthority
        savedStateHandle[EXTRA_SELECTED_ARTICLE_UUID] = newUuid
        if (newAuthorityAndUuid != null) {
            this._lastReadArticleAuthorityAndUUID.value = newAuthorityAndUuid
        }
    }

    val fullscreenModeAvailable: LiveData<Boolean?> =
        MediatorLiveData2(newsArticles, selectedNewsArticleAuthorityAndUUID).map { (newsArticles, selectedNewsArticleAuthorityAndUUID) ->
            newsArticles ?: return@map null
            selectedNewsArticleAuthorityAndUUID ?: return@map null
            if (!selectedNewsArticleAuthorityAndUUID.isAuthorityAndUuidValid()) return@map null
            newsArticles.singleOrNull { it.authorityAndUuidT == selectedNewsArticleAuthorityAndUUID }?.hasVideo == true
        }

    val fullscreenMode = savedStateHandle.getLiveDataDistinct(EXTRA_FULL_SCREEN_MODE, EXTRA_FULL_SCREEN_MODE_DEFAULT)

    fun setFullscreenMode(newFullscreenMode: Boolean) {
        savedStateHandle[EXTRA_FULL_SCREEN_MODE] = newFullscreenMode
    }

    override fun getAdBannerHeightInPx(activity: IAdScreenActivity?) = this.adManager.getBannerHeightInPx(activity)

    override val moduleDisabled = this.dataSourcesRepository.readingAllAgenciesBase().map {
        it.filter { agency -> !agency.isEnabled }
    }.distinctUntilChanged()

    override val hasDisabledModule = moduleDisabled.map {
        it.any { agency -> !pm.isAppEnabled(agency.pkg) }
    }

    private data class Filters(
        val targetAuthorities: List<String>?,
        val filterTargets: List<String>?,
        val filterUUIDs: List<String>?,
    )
}