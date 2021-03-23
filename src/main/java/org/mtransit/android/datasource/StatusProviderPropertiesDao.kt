package org.mtransit.android.datasource

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.data.StatusProviderProperties

@Dao
interface StatusProviderPropertiesDao {

    @Query("SELECT * FROM status_provider_properties")
    fun getAllStatusProvider(): List<StatusProviderProperties>

    @Query("SELECT * FROM status_provider_properties")
    fun readingAllStatusProviders(): LiveData<List<StatusProviderProperties>>

    @Query("SELECT * FROM status_provider_properties WHERE authority = :authority")
    fun getStatusProvider(authority: String): StatusProviderProperties

    @Query("SELECT * FROM status_provider_properties WHERE target_authority = :targetAuthority")
    fun getTargetAuthorityStatusProvider(targetAuthority: String): List<StatusProviderProperties>

    @Insert
    fun insert(statusProviderProperties: StatusProviderProperties)

    @Insert
    fun insertAll(vararg statusProviderProperties: StatusProviderProperties)

    @Update
    fun update(statusProviderProperties: StatusProviderProperties)

    @Update
    fun updateAll(vararg statusProviderProperties: StatusProviderProperties)

    @Delete
    fun delete(statusProviderProperties: StatusProviderProperties)
}