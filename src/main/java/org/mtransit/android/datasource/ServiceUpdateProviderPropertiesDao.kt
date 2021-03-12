package org.mtransit.android.datasource

import androidx.room.Dao
import androidx.room.Query
import org.mtransit.android.data.ServiceUpdateProviderProperties

@Dao
interface ServiceUpdateProviderPropertiesDao {

    @Query("SELECT * FROM service_update_provider_properties")
    fun getAllStatusProvider(): List<ServiceUpdateProviderProperties>

    @Query("SELECT * FROM service_update_provider_properties WHERE authority = :authority")
    fun getStatusProvider(authority: String): ServiceUpdateProviderProperties

    @Query("SELECT * FROM service_update_provider_properties WHERE target_authority = :targetAuthority")
    fun getTargetAuthorityServiceUpdateProvider(targetAuthority: String): List<ServiceUpdateProviderProperties>
}