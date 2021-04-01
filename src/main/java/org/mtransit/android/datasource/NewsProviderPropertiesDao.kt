package org.mtransit.android.datasource

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.mtransit.android.common.repository.BaseDao
import org.mtransit.android.data.NewsProviderProperties

@Dao
interface NewsProviderPropertiesDao : BaseDao<NewsProviderProperties> {

    @Query("SELECT * FROM news_provider_properties")
    fun getAllNewsProvider(): List<NewsProviderProperties>

    @Query("SELECT * FROM news_provider_properties")
    fun readingAllNewsProvider(): LiveData<List<NewsProviderProperties>>

    @Query("SELECT * FROM news_provider_properties WHERE authority = :authority")
    fun getNewsProvider(authority: String): NewsProviderProperties

    @Query("SELECT * FROM news_provider_properties WHERE target_authority = :targetAuthority")
    fun getTargetAuthorityNewsProviders(targetAuthority: String): List<NewsProviderProperties>

    @Query("SELECT * FROM news_provider_properties WHERE target_authority = :targetAuthority")
    fun readingTargetAuthorityNewsProviders(targetAuthority: String): LiveData<List<NewsProviderProperties>>
}