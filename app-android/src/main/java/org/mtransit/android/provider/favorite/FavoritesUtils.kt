package org.mtransit.android.provider.favorite

import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.data.DataSourceType

object FavoritesUtils {

    @JvmStatic
    fun isFavoriteDataSourceId(@DataSourceTypeId.DataSourceType dataSourceId: Int): Boolean {
        return dataSourceId > DataSourceType.MAX_ID
    }

    @JvmStatic
    fun extractFavoriteFolderId(@DataSourceTypeId.DataSourceType dataSourceId: Int): Int {
        return dataSourceId - DataSourceType.MAX_ID
    }

    @JvmStatic
    fun generateFavoriteFolderId(favoriteFolderId: Int): Int {
        return favoriteFolderId + DataSourceType.MAX_ID
    }

    @JvmStatic
    fun getFavoriteDataSourceIdOrNull(favoriteFolderId: Int): Int? {
        if (!isFavoriteDataSourceId(favoriteFolderId)) return null
        return extractFavoriteFolderId(favoriteFolderId)
    }
}
