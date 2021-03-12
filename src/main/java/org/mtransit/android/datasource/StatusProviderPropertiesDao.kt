package org.mtransit.android.datasource

import androidx.room.Dao
import androidx.room.Query
import org.mtransit.android.data.StatusProviderProperties

@Dao
interface StatusProviderPropertiesDao {

    @Query("SELECT * FROM status_provider_properties")
    fun getAllStatusProvider(): List<StatusProviderProperties>

    @Query("SELECT * FROM status_provider_properties WHERE authority = :authority")
    fun getStatusProvider(authority: String): StatusProviderProperties

    @Query("SELECT * FROM status_provider_properties WHERE target_authority = :targetAuthority")
    fun getTargetAuthorityStatusProvider(targetAuthority: String): List<StatusProviderProperties>
}