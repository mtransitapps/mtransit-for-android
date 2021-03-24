package org.mtransit.android.datasource

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.mtransit.android.data.ScheduleProviderProperties

@Dao
interface ScheduleProviderPropertiesDao {

    @Query("SELECT * FROM schedule_provider_properties")
    fun getAllScheduleProvider(): List<ScheduleProviderProperties>

    @Query("SELECT * FROM schedule_provider_properties")
    fun readingAllScheduleProvider(): LiveData<List<ScheduleProviderProperties>>

    @Query("SELECT * FROM schedule_provider_properties WHERE authority = :authority")
    fun getScheduleProvider(authority: String): ScheduleProviderProperties

    @Query("SELECT * FROM schedule_provider_properties WHERE target_authority = :targetAuthority")
    fun getTargetAuthorityScheduleProvider(targetAuthority: String): List<ScheduleProviderProperties>

    @Insert
    fun insert(scheduleProviderProperties: ScheduleProviderProperties)

    @Insert
    fun insertAll(vararg scheduleProviderProperties: ScheduleProviderProperties)

    @Update
    fun update(scheduleProviderProperties: ScheduleProviderProperties)

    @Update
    fun updateAll(vararg scheduleProviderProperties: ScheduleProviderProperties)

    @Delete
    fun delete(scheduleProviderProperties: ScheduleProviderProperties)
}