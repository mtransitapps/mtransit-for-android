package org.mtransit.android.provider.favorite

import org.mtransit.android.data.DataSourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("ComplexRedundantLet")
class FavoritesFolderDSTUtilsTest {

    @Test
    fun test_isFavoriteFolderDataSourceId() {
        FavoritesFolderDSTUtils.isFavoriteFolderDataSourceId(DataSourceType.MAX_ID + 1).let { result ->
            assertTrue(result)
        }
        FavoritesFolderDSTUtils.isFavoriteFolderDataSourceId(DataSourceType.MAX_ID).let { result ->
            assertFalse(result)
        }
        FavoritesFolderDSTUtils.isFavoriteFolderDataSourceId(0).let { result ->
            assertFalse(result)
        }
    }

    @Test
    fun test_extractFavoriteFolderId() {
        FavoritesFolderDSTUtils.extractFavoriteFolderId(DataSourceType.MAX_ID + 1).let { result ->
            assertEquals(1, result)
        }
        FavoritesFolderDSTUtils.extractFavoriteFolderId(DataSourceType.MAX_ID).let { result ->
            assertEquals(0, result)
        }
    }

    @Test
    fun test_generateDstFavoriteFolderId() {
        FavoritesFolderDSTUtils.generateDstFavoriteFolderId(1).let { result ->
            assertEquals(1001, result)
        }
        FavoritesFolderDSTUtils.generateDstFavoriteFolderId(0).let { result ->
            assertEquals(1000, result)
        }
    }

    @Test
    fun test_getFavoriteFolderIdOrNull() {
        FavoritesFolderDSTUtils.getFavoriteFolderIdOrNull(DataSourceType.MAX_ID + 1).let { result ->
            assertEquals(1, result)
        }
        FavoritesFolderDSTUtils.getFavoriteFolderIdOrNull(DataSourceType.MAX_ID).let { result ->
            assertEquals(null, result)
        }
        FavoritesFolderDSTUtils.getFavoriteFolderIdOrNull(0).let { result ->
            assertEquals(null, result)
        }
    }
}
