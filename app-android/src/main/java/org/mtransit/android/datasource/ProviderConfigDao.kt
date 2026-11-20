package org.mtransit.android.datasource

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.mtransit.android.data.ProviderConfigEntity

@Dao
interface ProviderConfigDao {

    @Query("SELECT * FROM provider_config WHERE authority = :authority")
    suspend fun get(authority: String): ProviderConfigEntity?

    @Upsert
    suspend fun upsert(config: ProviderConfigEntity)

    @Query("DELETE FROM provider_config WHERE authority = :authority")
    suspend fun delete(authority: String)

    @Query("DELETE FROM provider_config WHERE authority NOT IN (:authorities)")
    suspend fun deleteExcept(authorities: List<String>)
}
