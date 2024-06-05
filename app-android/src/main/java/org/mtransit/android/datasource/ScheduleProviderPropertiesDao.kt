package org.mtransit.android.datasource

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.mtransit.android.common.repository.BaseDao
import org.mtransit.android.data.ScheduleProviderProperties

@Dao
interface ScheduleProviderPropertiesDao : BaseDao<ScheduleProviderProperties> {

    @Query("SELECT * FROM schedule_provider_properties")
    suspend fun getAllScheduleProvider(): List<ScheduleProviderProperties>

    @Query("SELECT * FROM schedule_provider_properties")
    fun readingAllScheduleProvider(): LiveData<List<ScheduleProviderProperties>>

    @Query("SELECT * FROM schedule_provider_properties WHERE authority = :authority")
    suspend fun getScheduleProvider(authority: String): ScheduleProviderProperties

    @Query("SELECT * FROM schedule_provider_properties WHERE authority = :authority")
    fun readingScheduleProvider(authority: String): LiveData<ScheduleProviderProperties?>

    @Query("SELECT * FROM schedule_provider_properties WHERE target_authority = :targetAuthority")
    suspend fun getTargetAuthorityScheduleProviders(targetAuthority: String): List<ScheduleProviderProperties>

    @Query("SELECT * FROM schedule_provider_properties WHERE target_authority = :targetAuthority")
    fun readingTargetAuthorityScheduleProviders(targetAuthority: String?): LiveData<List<ScheduleProviderProperties>>
}