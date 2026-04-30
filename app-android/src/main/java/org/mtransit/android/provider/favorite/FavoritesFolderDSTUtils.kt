package org.mtransit.android.provider.favorite

import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.data.DataSourceType

object FavoritesFolderDSTUtils {

    @JvmStatic
    fun isFavoriteFolderDataSourceId(@DataSourceTypeId.DataSourceType dataSourceId: Int) =
        dataSourceId > DataSourceType.MAX_ID

    @JvmStatic
    fun extractFavoriteFolderId(@DataSourceTypeId.DataSourceType dataSourceId: Int) =
        dataSourceId - DataSourceType.MAX_ID

    @JvmStatic
    fun generateFavoriteFolderDataSourceId(favoriteFolderId: Int) =
        DataSourceType.MAX_ID + favoriteFolderId

    @JvmStatic
    fun getFavoriteFolderDataSourceIdOrNull(favoriteFolderId: Int) =
        favoriteFolderId.takeIf { isFavoriteFolderDataSourceId(it) }
            ?.let { generateFavoriteFolderDataSourceId(it) }
}
