package org.mtransit.android.datasource

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.mtransit.android.common.repository.BaseDao
import org.mtransit.android.data.ServiceUpdateProviderProperties

@Dao
interface ServiceUpdateProviderPropertiesDao : BaseDao<ServiceUpdateProviderProperties> {

    @Query("SELECT * FROM service_update_provider_properties")
    fun getAllServiceUpdateProvider(): List<ServiceUpdateProviderProperties>

    @Query("SELECT * FROM service_update_provider_properties")
    fun readingAllServiceUpdateProvider(): LiveData<List<ServiceUpdateProviderProperties>>

    @Query("SELECT * FROM service_update_provider_properties WHERE authority = :authority")
    fun getServiceUpdateProvider(authority: String): ServiceUpdateProviderProperties

    @Query("SELECT * FROM service_update_provider_properties WHERE target_authority = :targetAuthority")
    fun getTargetAuthorityServiceUpdateProvider(targetAuthority: String): List<ServiceUpdateProviderProperties>
}