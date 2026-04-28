package org.mtransit.android.provider

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.data.Favorite
import org.mtransit.android.data.Favorite.Companion.fromCursor
import org.mtransit.android.data.FavoriteFolder
import org.mtransit.android.datasource.ContentProviderLiveData
import org.mtransit.android.dev.DemoModeManager
import javax.inject.Inject
import javax.inject.Singleton

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
        appContext = appContext,
        favoriteManager = favoriteManager,
        demoModeManager = demoModeManager,
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private const val FAVORITE_CONTENT_DIRECTORY = "favorite"
        private const val FOLDER_CONTENT_DIRECTORY = "folder"
    }

    private var _authority: String? = null
    private val authority: String get() = _authority ?: appContext.getString(R.string.favorite_authority).also { _authority = it }

    private val authorityUri get() = UriUtils.newContentUri(authority)

    private val favoriteContentDirectoryUri get() = Uri.withAppendedPath(authorityUri, FAVORITE_CONTENT_DIRECTORY)
    private val folderContentDirectoryUri get() = Uri.withAppendedPath(favoriteContentDirectoryUri, FOLDER_CONTENT_DIRECTORY)

    fun getReadingFavoriteById(id: Int): LiveData<Favorite?> =
        makeFavoriteLiveData(Uri.withAppendedPath(favoriteContentDirectoryUri, id.toString())).map {
            it?.firstOrNull()
        }

    val readingAllFavorites: LiveData<List<Favorite>?> = makeFavoriteLiveData(favoriteContentDirectoryUri)

    val readingAllFavoriteFkIds: LiveData<List<String>?> = readingAllFavorites.map {
        it?.map { favorite -> favorite.fkId }
    }

    private fun makeFavoriteLiveData(uri: Uri) = ContentProviderLiveData(
        contentResolver = appContext.contentResolver,
        uri = uri
    ) {
        appContext.contentResolver.query(uri, FavoriteProvider.PROJECTION_FAVORITE, null, null, null)
            ?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.fromCursor())
                    }
                }
            }
    }

    val readingAllFavoriteFolders: LiveData<List<FavoriteFolder>?> = ContentProviderLiveData(
        contentResolver = appContext.contentResolver,
        uri = folderContentDirectoryUri,
    ) {
        appContext.contentResolver.query(folderContentDirectoryUri, FavoriteProvider.PROJECTION_FOLDER, null, null, null)
            ?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(FavoriteFolder.fromCursor(cursor))
                    }
                }
            }
    }

    val isUsingFavoriteFolders: LiveData<Boolean> =
        readingAllFavoriteFolders.map { it?.any { folder -> folder.id != FavoriteFolder.DEFAULT_FOLDER_ID } ?: false }

    @Suppress("unused")
    suspend fun isUsingFavoriteFolders() = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        appContext.contentResolver.query(folderContentDirectoryUri, FavoriteProvider.PROJECTION_FOLDER, null, null, null)
            ?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(FavoriteFolder.fromCursor(cursor))
                    }
                }.any { it.id != FavoriteFolder.DEFAULT_FOLDER_ID }
            }
    }

    suspend fun hasFavorites() = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        favoriteManager.hasFavorites(appContext)
    }

    suspend fun findFavoriteUUIDs() = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext emptySet()
        favoriteManager.findFavoriteUUIDs(appContext)
    }

    @Suppress("unused")
    suspend fun findFavorites() = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext emptyList()
        favoriteManager.findFavorites(appContext)
    }

    suspend fun findFoldersList() = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext emptyList()
        favoriteManager.findFoldersList(appContext)
    }

    suspend fun addFolder(name: String) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext null
        favoriteManager.addFolder(appContext, name, null)
    }

    suspend fun updateFolder(updateFavoriteFolder: FavoriteFolder) = updateFolder(updateFavoriteFolder.id, updateFavoriteFolder.name)

    suspend fun updateFolder(folderId: Int, newFolderName: String) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        favoriteManager.updateFolder(appContext, folderId, newFolderName, null)
    }

    suspend fun deleteFolder(folder: FavoriteFolder) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        favoriteManager.deleteFolder(appContext, folder, null)
    }

    suspend fun isFavorite(fkId: String) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        favoriteManager.isFavorite(appContext, fkId)
    }

    suspend fun getFavorite(fkId: String) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext null
        favoriteManager.findFavorite(appContext, fkId)
    }

    suspend fun addFavorite(fkId: String, folderId: Int = FavoriteFolder.DEFAULT_FOLDER_ID) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        favoriteManager.addFavorite(appContext, fkId, folderId, null)
    }
    suspend fun updateFavoriteFolder(favoriteFkId: String, folderId: Int = FavoriteFolder.DEFAULT_FOLDER_ID) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        favoriteManager.updateFavoriteFolder(appContext, favoriteFkId, folderId, null)
    }
    suspend fun deleteFavorite(fkId: String, folderId: Int = FavoriteFolder.DEFAULT_FOLDER_ID): Boolean = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        favoriteManager.deleteFavorite(appContext, fkId, folderId, null)
    }
}
