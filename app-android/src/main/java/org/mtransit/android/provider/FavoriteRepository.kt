package org.mtransit.android.provider

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
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
    private val demoModeManager: DemoModeManager,
    private val ioDispatcher: CoroutineDispatcher,
) : MTLog.Loggable {

    @Inject
    constructor(
        @ApplicationContext appContext: Context,
        demoModeManager: DemoModeManager,
    ) : this(
        appContext = appContext,
        demoModeManager = demoModeManager,
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG: String = FavoriteRepository::class.java.simpleName

        private const val FAVORITE_CONTENT_DIRECTORY = "favorite"
        private const val FOLDER_CONTENT_DIRECTORY = "folder"
    }

    override fun getLogTag() = LOG_TAG

    private var _authority: String? = null
    private val authority: String get() = _authority ?: appContext.getString(R.string.favorite_authority).also { _authority = it }

    private val authorityUri get() = UriUtils.newContentUri(authority)

    // region Favorites

    private val favoriteContentDirectoryUri get() = Uri.withAppendedPath(authorityUri, FAVORITE_CONTENT_DIRECTORY)

    fun getReadingFavoriteById(id: Int): LiveData<Favorite?> =
        makeFavoriteLiveData(Uri.withAppendedPath(favoriteContentDirectoryUri, id.toString())).map {
            it.firstOrNull()
        }

    val readingAllFavorites: LiveData<Collection<Favorite>> = makeFavoriteLiveData()

    val readingHasFavorites: LiveData<Boolean> = readingAllFavorites.map { it.isNotEmpty() }

    val readingAllFavoriteFkIds: LiveData<List<String>?> = readingAllFavorites.map {
        it.map { favorite -> favorite.fkId }
    }

    val readingAllFavoritesChange: LiveData<Any> = readingAllFavorites.distinctUntilChanged().map { Any() }

    fun makeFavoriteLiveData(uri: Uri = favoriteContentDirectoryUri) = ContentProviderLiveData(
        contentResolver = appContext.contentResolver,
        uri = uri
    ) {
        findFavorites(uri)
    }

    private fun findFavorites(uri: Uri = favoriteContentDirectoryUri, selection: String? = null): Collection<Favorite> =
        appContext.contentResolver.query(uri, FavoriteProvider.PROJECTION_FAVORITE, selection, null, null)
            ?.use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) {
                        add(cursor.fromCursor())
                    }
                }
            }.orEmpty()

    private fun findFavorite(uri: Uri = favoriteContentDirectoryUri, selection: String? = null) =
        findFavorites(uri, selection).firstOrNull()

    suspend fun hasFavorites() = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        findFavorites().isNotEmpty()
    }

    suspend fun findFavoriteUUIDs() = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext emptySet()
        findFavorites().map { it.fkId }
    }

    suspend fun isFavorite(fkId: String) = getFavorite(fkId) != null

    suspend fun getFavorite(fkId: String) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext null
        findFavorite(selection = SqlUtils.getWhereEqualsString(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_FK_ID, fkId))
    }

    suspend fun addFavorite(fkId: String, folderId: Int = FavoriteFolder.DEFAULT_FOLDER_ID) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        appContext.contentResolver.insert(favoriteContentDirectoryUri, Favorite.makeFavorite(fkId, folderId).toContentValues())
            ?.let { uri ->
                findFavorite(uri) != null
            } ?: false
    }

    suspend fun updateFavoriteFolder(favoriteFkId: String, folderId: Int = FavoriteFolder.DEFAULT_FOLDER_ID) =
        updateFavoriteFolders(folderId, setOf(favoriteFkId))

    suspend fun updateFavoriteFolders(
        folderId: Int = FavoriteFolder.DEFAULT_FOLDER_ID,
        favoriteFkIds: Collection<String> = emptySet(),
        selection: String? = SqlUtils.getWhereInString(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_FK_ID, favoriteFkIds),
    ) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        appContext.contentResolver.update(
            favoriteContentDirectoryUri,
            ContentValues().apply {
                put(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_FOLDER_ID, folderId)
            },
            selection,
            null
        ) > 0
    }

    suspend fun deleteFavorite(fkId: String) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        appContext.contentResolver.delete(
            favoriteContentDirectoryUri,
            SqlUtils.getWhereEqualsString(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_FK_ID, fkId),
            null
        ) > 0
    }

    // endregion

    // region Folders

    private val folderContentDirectoryUri get() = Uri.withAppendedPath(favoriteContentDirectoryUri, FOLDER_CONTENT_DIRECTORY)

    val readingAllFolders: LiveData<Collection<FavoriteFolder>> = ContentProviderLiveData(
        contentResolver = appContext.contentResolver,
        uri = folderContentDirectoryUri,
    ) {
        findFolders(folderContentDirectoryUri)
    }

    private fun findFolders(uri: Uri = folderContentDirectoryUri): Collection<FavoriteFolder> =
        appContext.contentResolver.query(uri, FavoriteProvider.PROJECTION_FOLDER, null, null, null)
            ?.use { cursor ->
                buildSet {
                    while (cursor.moveToNext()) {
                        add(FavoriteFolder.fromCursor(cursor))
                    }
                }
            }.orEmpty()

    private fun findFolder(uri: Uri) = findFolders(uri).firstOrNull()

    val isUsingFolders: LiveData<Boolean> =
        readingAllFolders.map { it.any { folder -> folder.id != FavoriteFolder.DEFAULT_FOLDER_ID } }

    suspend fun findFoldersList() = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext emptyList()
        findFolders()
    }

    suspend fun findFolder(id: Int) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext null
        findFolder(Uri.withAppendedPath(folderContentDirectoryUri, id.toString()))
    }

    suspend fun addFolder(name: String) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext null
        appContext.contentResolver.insert(folderContentDirectoryUri, FavoriteFolder(name = name).toContentValues())
            ?.let { uri ->
                findFolder(uri)
            }
    }

    suspend fun updateFolder(updatedFolder: FavoriteFolder) = updateFolder(updatedFolder.id, updatedFolder.name)

    suspend fun updateFolder(folderId: Int, newFolderName: String) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        appContext.contentResolver.update(
            Uri.withAppendedPath(folderContentDirectoryUri, folderId.toString()),
            FavoriteFolder(id = folderId, name = newFolderName).toContentValues(),
            null,
            null
        ) > 0
    }

    suspend fun deleteFolder(folder: FavoriteFolder) = withContext(ioDispatcher) {
        if (demoModeManager.isFullDemo()) return@withContext false
        if (folder.id == FavoriteFolder.DEFAULT_FOLDER_ID) return@withContext false
        updateFavoriteFolders(
            folderId = FavoriteFolder.DEFAULT_FOLDER_ID,
            selection = SqlUtils.getWhereEquals(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_FOLDER_ID, folder.id),
        )
        appContext.contentResolver.delete(Uri.withAppendedPath(folderContentDirectoryUri, folder.id.toString()), null, null) > 0
    }

    // endregion
}
