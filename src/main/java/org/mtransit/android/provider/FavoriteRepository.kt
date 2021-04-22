package org.mtransit.android.provider

import org.mtransit.android.R
import org.mtransit.android.common.IApplication
import org.mtransit.android.commons.data.POI
import org.mtransit.android.data.Favorite
import org.mtransit.android.data.TextMessage

class FavoriteRepository(private val app: IApplication) {

    companion object {
        const val DEFAULT_FOLDER_ID = FavoriteManager.DEFAULT_FOLDER_ID
    }

    fun findFavoriteUUIDs(): Set<String> {
        return FavoriteManager.findFavoriteUUIDs(app.requireContext())
    }

    fun findFavorites(): List<Favorite> {
        return FavoriteManager.findFavorites(app.requireContext())
    }

    fun findFoldersList(): List<Favorite.Folder> {
        return FavoriteManager.findFoldersList(app.requireContext())
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun isFavoriteDataSourceId(favoriteFolderId: Int): Boolean {
        return FavoriteManager.isFavoriteDataSourceId(favoriteFolderId)
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
        return FavoriteManager.extractFavoriteFolderId(favoriteFolderId)
    }

    fun generateFavoriteFolderId(favoriteFolderId: Int): Int {
        return FavoriteManager.generateFavoriteFolderId(favoriteFolderId)
    }

    fun generateFavEmptyFavPOI(textMessageId: Long): POI {
        return TextMessage(textMessageId, app.requireContext().getString(R.string.favorite_folder_empty))
    }
}