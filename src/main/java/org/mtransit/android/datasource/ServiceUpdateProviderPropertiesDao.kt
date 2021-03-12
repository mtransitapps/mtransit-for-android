package org.mtransit.android.datasource

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.mtransit.android.data.ServiceUpdateProviderProperties

@Dao
interface ServiceUpdateProviderPropertiesDao {

    @Query("SELECT * FROM service_update_provider_properties")
    fun getAllServiceUpdateProvider(): List<ServiceUpdateProviderProperties>

    @Query("SELECT * FROM service_update_provider_properties WHERE authority = :authority")
    fun getServiceUpdateProvider(authority: String): ServiceUpdateProviderProperties

    @Query("SELECT * FROM service_update_provider_properties WHERE target_authority = :targetAuthority")
    fun getTargetAuthorityServiceUpdateProvider(targetAuthority: String): List<ServiceUpdateProviderProperties>

    @Insert
    fun insert(serviceUpdateProviderProperties: ServiceUpdateProviderProperties)

    @Insert
    fun insertAll(vararg serviceUpdateProviderProperties: ServiceUpdateProviderProperties)

    @Update
    fun update(serviceUpdateProviderProperties: ServiceUpdateProviderProperties)

    @Update
    fun updateAll(vararg serviceUpdateProviderProperties: ServiceUpdateProviderProperties)

    @Delete
    fun delete(serviceUpdateProviderProperties: ServiceUpdateProviderProperties)
}