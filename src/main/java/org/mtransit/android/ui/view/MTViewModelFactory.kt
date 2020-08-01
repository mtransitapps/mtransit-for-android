package org.mtransit.android.ui.view

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import org.mtransit.android.data.source.NewsRepository
import org.mtransit.android.ui.news.NewsListViewModel
import org.mtransit.android.ui.news.viewer.page.NewsArticleViewModel

/**
 * Factory for all ViewModels.
 */
@Suppress("UNCHECKED_CAST")
class MTViewModelFactory constructor(
    private val newsRepository: NewsRepository,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ) = with(modelClass) {
        when {
            isAssignableFrom(NewsListViewModel::class.java) ->
                NewsListViewModel(newsRepository, handle)
            isAssignableFrom(NewsArticleViewModel::class.java) ->
                NewsArticleViewModel(newsRepository)
            else ->
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } as T
}