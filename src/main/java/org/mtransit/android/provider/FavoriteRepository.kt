package org.mtransit.android.provider

import android.content.Context
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.R
import org.mtransit.android.commons.data.POI
import org.mtransit.android.data.Favorite
import org.mtransit.android.data.TextMessage
import org.mtransit.android.dev.DemoModeManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val favoriteManager: FavoriteManager,
    private val demoModeManager: DemoModeManager,
) {

    companion object {
        const val DEFAULT_FOLDER_ID = FavoriteManager.DEFAULT_FOLDER_ID
    }

    fun findFavoriteUUIDs(): Set<String> {
        if (demoModeManager.enabled) {
            return emptySet()
        }
        return favoriteManager.findFavoriteUUIDs(appContext)
    }

    fun findFavorites(): List<Favorite> {
        if (demoModeManager.enabled) {
            return emptyList()
        }
        return favoriteManager.findFavorites(appContext)
    }

    fun findFoldersList(): List<Favorite.Folder> {
        if (demoModeManager.enabled) {
            return emptyList()
        }
        return favoriteManager.findFoldersList(appContext)
    }

    @WorkerThread
    fun isFavorite(fkId: String): Boolean {
        if (demoModeManager.enabled) {
            return false
        }
        return favoriteManager.isFavorite(appContext, fkId)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun isFavoriteDataSourceId(favoriteFolderId: Int): Boolean {
        if (demoModeManager.enabled) {
            return false
        }
        return favoriteManager.isFavoriteDataSourceId(favoriteFolderId)
    }

    fun getFavoriteDataSourceIdOrNull(favoriteFolderId: Int): Int? {
        return if (isFavoriteDataSourceId(favoriteFolderId)) {
            extractFavoriteFolderId(favoriteFolderId)
        } else {
            null
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun extractFavoriteFolderId(favoriteFolderId: Int): Int {
        return favoriteManager.extractFavoriteFolderId(favoriteFolderId)
    }

    fun generateFavoriteFolderId(favoriteFolderId: Int): Int {
        return favoriteManager.generateFavoriteFolderId(favoriteFolderId)
    }

    fun generateFavEmptyFavPOI(textMessageId: Long): POI {
        return TextMessage(textMessageId, appContext.getString(R.string.favorite_folder_empty))
    }
}