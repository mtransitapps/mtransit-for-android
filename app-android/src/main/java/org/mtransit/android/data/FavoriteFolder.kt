package org.mtransit.android.data

import android.content.ContentValues
import android.database.Cursor
import org.mtransit.android.provider.FavoriteProvider

data class FavoriteFolder @JvmOverloads constructor(
    val id: Int = INVALID_FOLDER_ID,
    val name: String,
) {
    companion object {

        const val INVALID_FOLDER_ID = -1
        const val DEFAULT_FOLDER_ID = 0

        @JvmStatic
        fun fromCursor(cursor: Cursor) = FavoriteFolder(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME)),
        )

        @JvmField
        val NAME_COMPARATOR: Comparator<FavoriteFolder> = compareBy { it.name }
    }

    fun toContentValues() = ContentValues().apply {
        id.takeIf { it != INVALID_FOLDER_ID }?.let { put(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_ID, it) }
        put(FavoriteProvider.FavoriteFolderColumns.T_FAVORITE_FOLDER_K_NAME, name)
    }
}
