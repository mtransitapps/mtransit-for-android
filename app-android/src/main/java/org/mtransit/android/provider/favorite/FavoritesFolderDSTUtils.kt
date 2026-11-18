package org.mtransit.android.provider.favorite

import org.mtransit.android.commons.data.DataSourceTypeId
import org.mtransit.android.data.DataSourceType

object FavoritesFolderDSTUtils {

    @JvmStatic
    fun isFavoriteFolderDataSourceId(@DataSourceTypeId.DataSourceType dstFavFolderId: Int) =
        dstFavFolderId > DataSourceType.MAX_ID

    @JvmStatic
    fun extractFavoriteFolderId(@DataSourceTypeId.DataSourceType dstFavFolderId: Int) =
        dstFavFolderId - DataSourceType.MAX_ID

    @JvmStatic
    fun generateDstFavoriteFolderId(favoriteFolderId: Int) =
        DataSourceType.MAX_ID + favoriteFolderId

    @JvmStatic
    fun getFavoriteFolderIdOrNull(dstFavFolderIdOrDstId: Int) =
        dstFavFolderIdOrDstId.takeIf { isFavoriteFolderDataSourceId(it) }
            ?.let { extractFavoriteFolderId(it) }
}
