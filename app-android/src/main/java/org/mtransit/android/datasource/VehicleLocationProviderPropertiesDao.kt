package org.mtransit.android.datasource

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.mtransit.android.common.repository.BaseDao
import org.mtransit.android.data.VehicleLocationProviderProperties

@Dao
interface VehicleLocationProviderPropertiesDao : BaseDao<VehicleLocationProviderProperties> {

    @Query("SELECT * FROM service_update_provider_properties")
    suspend fun getAllVehicleLocationProvider(): List<VehicleLocationProviderProperties>

    @Query("SELECT * FROM service_update_provider_properties")
    fun readingAllVehicleLocationProvider(): LiveData<List<VehicleLocationProviderProperties>>

    @Query("SELECT * FROM service_update_provider_properties WHERE authority = :authority")
    suspend fun getVehicleLocationProvider(authority: String): VehicleLocationProviderProperties

    @Query("SELECT * FROM service_update_provider_properties WHERE target_authority = :targetAuthority")
    suspend fun getTargetAuthorityVehicleLocationProvider(targetAuthority: String): List<VehicleLocationProviderProperties>

    @Query("SELECT * FROM service_update_provider_properties WHERE target_authority = :targetAuthority")
    fun readingTargetAuthorityVehicleLocationProviders(targetAuthority: String?): LiveData<List<VehicleLocationProviderProperties>>
}
