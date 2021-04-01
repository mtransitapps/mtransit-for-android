package org.mtransit.android.datasource

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.mtransit.android.common.repository.BaseDao
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.commons.sql.SQLUtils.BOOLEAN_FALSE
import org.mtransit.commons.sql.SQLUtils.BOOLEAN_TRUE

@Dao
interface AgencyPropertiesDao : BaseDao<AgencyProperties> {

    @Query("SELECT * FROM agency_properties")
    fun getAllAgenciesInclNotInstalled(): List<AgencyProperties>

    @Query("SELECT * FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    fun getAllAgencies(): List<AgencyProperties>

    @Query("SELECT * FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    fun readingAllAgencies(): LiveData<List<AgencyProperties>>

    @Query("SELECT * FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE AND is_enabled = $BOOLEAN_TRUE")
    fun getAllEnabledAgencies(): List<AgencyProperties>

    @Query("SELECT * FROM agency_properties WHERE is_installed = $BOOLEAN_FALSE OR is_enabled = $BOOLEAN_FALSE")
    fun getAllNotInstalledOrNotEnabledAgencies(): List<AgencyProperties>

    @Query("SELECT COUNT(*) FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    fun getAllAgenciesCount(): Int

    @Query("SELECT COUNT(*) FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    fun readingAllAgenciesCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE AND is_enabled = $BOOLEAN_TRUE")
    fun getAllEnabledAgenciesCount(): Int

    @Query("SELECT DISTINCT type FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    fun getAllDataSourceTypes(): List<DataSourceType>

    @Query("SELECT DISTINCT type FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    fun readingAllDataSourceTypes(): LiveData<List<DataSourceType>>

    @Query("SELECT * FROM agency_properties WHERE id = :authority")
    fun getAgency(authority: String): AgencyProperties?

    @Query("SELECT * FROM agency_properties WHERE id = :authority AND is_installed = $BOOLEAN_TRUE")
    fun readingAgency(authority: String): LiveData<AgencyProperties?>

    @Query("SELECT pkg FROM agency_properties WHERE id = :authority")
    fun getAgencyPkg(authority: String): String

    @Query("SELECT color_int FROM agency_properties WHERE id = :authority")
    fun getAgencyColorInt(authority: String): Int?

    @Query("SELECT * FROM agency_properties WHERE type = :dst")
    fun getTypeDataSources(dst: DataSourceType): List<AgencyProperties>

    @Query("SELECT * FROM agency_properties WHERE type = :dst")
    fun readingTypeDataSources(dst: DataSourceType): LiveData<List<AgencyProperties>>
}