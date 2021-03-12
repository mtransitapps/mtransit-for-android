package org.mtransit.android.datasource

import androidx.room.Dao
import androidx.room.Query
import org.mtransit.android.data.ScheduleProviderProperties

@Dao
interface ScheduleProviderPropertiesDao {

    @Query("SELECT * FROM schedule_provider_properties")
    fun getAllStatusProvider(): List<ScheduleProviderProperties>

    @Query("SELECT * FROM schedule_provider_properties WHERE authority = :authority")
    fun getStatusProvider(authority: String): ScheduleProviderProperties

    @Query("SELECT * FROM schedule_provider_properties WHERE target_authority = :targetAuthority")
    fun getTargetAuthorityScheduleProvider(targetAuthority: String): List<ScheduleProviderProperties>
}