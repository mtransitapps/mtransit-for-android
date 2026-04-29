package org.mtransit.android.data

import android.content.ContentValues
import android.database.Cursor
import org.mtransit.android.provider.FavoriteProvider

data class Favorite @JvmOverloads constructor(
    val id: Int = -1,
    val fkId: String,
    val type: Int = KEY_TYPE_VALUE_AUTHORITY_POI,
    val folderId: Int = FavoriteFolder.DEFAULT_FOLDER_ID,
) {

    companion object {

        private const val KEY_TYPE_VALUE_AUTHORITY_POI = 1

        @JvmStatic
        fun makeFavorite(fkId: String, folderId: Int) = Favorite(
            fkId = fkId,
            folderId = folderId,
        )

        @JvmStatic
        fun Cursor.fromCursor() = Favorite(
            id = getInt(getColumnIndexOrThrow(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_ID)),
            fkId = getString(getColumnIndexOrThrow(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_FK_ID)),
            type = getInt(getColumnIndexOrThrow(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_TYPE)),
            folderId = getColumnIndexOrThrow(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_FOLDER_ID)
                .takeIf { it > FavoriteFolder.DEFAULT_FOLDER_ID }
                ?.let { getInt(it) }
                ?: FavoriteFolder.DEFAULT_FOLDER_ID,
        )
    }

    fun toContentValues() = ContentValues().apply {
        id.takeIf { it >= 0 }?.let { put(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_ID, it) }
        put(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_FK_ID, fkId)
        put(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_TYPE, type)
        if (folderId > FavoriteFolder.DEFAULT_FOLDER_ID) {
            put(FavoriteProvider.FavoriteColumns.T_FAVORITE_K_FOLDER_ID, folderId)
        }
    }
}
