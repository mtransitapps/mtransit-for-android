package org.mtransit.android.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.commons.data.POI
import org.mtransit.android.data.Favorite
import org.mtransit.android.data.TextMessage
import org.mtransit.android.dev.DemoModeManager
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("MemberVisibilityCanBePrivate", "unused")
@Singleton
class FavoriteRepository(
    @param:ApplicationContext private val appContext: Context,
    private val favoriteManager: FavoriteManager,
    private val demoModeManager: DemoModeManager,
    private val ioDispatcher: CoroutineDispatcher,
) {

    @Inject
    constructor(
        @ApplicationContext appContext: Context,
        favoriteManager: FavoriteManager,
        demoModeManager: DemoModeManager,
    ) : this(
        appContext,
        favoriteManager,
        demoModeManager,
        Dispatchers.IO,
    )

    companion object {
        const val DEFAULT_FOLDER_ID = FavoriteManager.DEFAULT_FOLDER_ID
    }

    suspend fun hasFavorites(): Boolean = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) {
            return@withContext false
        }
        favoriteManager.hasFavorites(appContext)
    }

    suspend fun findFavoriteUUIDs(): Set<String> = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) {
            return@withContext emptySet()
        }
        favoriteManager.findFavoriteUUIDs(appContext)
    }

    suspend fun findFavorites(): List<Favorite> = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) {
            return@withContext emptyList<Favorite>()
        }
        favoriteManager.findFavorites(appContext)
    }

    suspend fun findFoldersList(): List<Favorite.Folder> = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) {
            return@withContext emptyList()
        }
        favoriteManager.findFoldersList(appContext)
    }

    suspend fun isFavorite(fkId: String): Boolean = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) {
            return@withContext false
        }
        favoriteManager.isFavorite(appContext, fkId)
    }

    fun isFavoriteDataSourceId(favoriteFolderId: Int): Boolean {
        if (demoModeManager.isFullDemo()) {
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

    fun extractFavoriteFolderId(favoriteFolderId: Int): Int {
        return favoriteManager.extractFavoriteFolderId(favoriteFolderId)
    }

    fun generateFavoriteFolderId(favoriteFolderId: Int): Int {
        return favoriteManager.generateFavoriteFolderId(favoriteFolderId)
    }

    fun generateFavEmptyFavPOI(textMessageId: Long, @DataSourceTypeId.DataSourceType dataSourceTypeId: Int): POI {
        return TextMessage(textMessageId, dataSourceTypeId, appContext.getString(R.string.favorite_folder_empty))
    }
}