package org.mtransit.android.datasource

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.mtransit.android.data.NewsProviderProperties

@Dao
interface NewsProviderPropertiesDao {

    @Query("SELECT * FROM news_provider_properties")
    fun getAllNewsProvider(): List<NewsProviderProperties>

    @Query("SELECT * FROM news_provider_properties WHERE authority = :authority")
    fun getNewsProvider(authority: String): NewsProviderProperties

    @Query("SELECT * FROM news_provider_properties WHERE target_authority = :targetAuthority")
    fun getTargetAuthorityNewsProviders(targetAuthority: String): List<NewsProviderProperties>

    @Insert
    fun insert(newsProviderProperties: NewsProviderProperties)

    @Insert
    fun insertAll(vararg newsProviderProperties: NewsProviderProperties)

    @Update
    fun update(newsProviderProperties: NewsProviderProperties)

    @Update
    fun updateAll(vararg newsProviderProperties: NewsProviderProperties)

    @Delete
    fun delete(newsProviderProperties: NewsProviderProperties)
}