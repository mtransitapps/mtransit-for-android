package org.mtransit.android.datasource

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import org.mtransit.android.common.repository.BaseDao
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.data.AgencyBaseProperties
import org.mtransit.commons.sql.SQLUtils.BOOLEAN_FALSE
import org.mtransit.commons.sql.SQLUtils.BOOLEAN_TRUE

@Dao
interface AgencyPropertiesDao : BaseDao<AgencyProperties> {

    @Query("SELECT * FROM agency_properties")
    suspend fun getAllAgenciesInclNotInstalled(): List<AgencyProperties>

    @Query("SELECT * FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    suspend fun getAllAgencies(): List<AgencyProperties>

    @Query("SELECT * FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    fun readingAllAgencies(): LiveData<List<AgencyProperties>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    fun readingAllAgenciesBase(): LiveData<List<AgencyBaseProperties>>

    @Query("SELECT * FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE AND is_enabled = $BOOLEAN_TRUE")
    suspend fun getAllEnabledAgencies(): List<AgencyProperties>

    @Query("SELECT * FROM agency_properties WHERE is_installed = $BOOLEAN_FALSE OR is_enabled = $BOOLEAN_FALSE")
    suspend fun getAllNotInstalledOrNotEnabledAgencies(): List<AgencyProperties>

    @Query("SELECT COUNT(*) FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    suspend fun getAllAgenciesCount(): Int

    @Query("SELECT COUNT(*) FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE")
    fun readingAllAgenciesCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE AND is_enabled = $BOOLEAN_TRUE")
    suspend fun getAllEnabledAgenciesCount(): Int

    @Query("SELECT DISTINCT type FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE AND extended_type IS NULL")
    suspend fun getAllNotExtendedDataSourceTypes(): List<DataSourceType>

    @Query("SELECT DISTINCT type FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE AND extended_type IS NULL")
    fun readingAllNotExtendedDataSourceTypes(): LiveData<List<DataSourceType>>

    @Query("SELECT DISTINCT extended_type FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE AND extended_type IS NOT NULL")
    suspend fun getAllExtendedDataSourceTypes(): List<DataSourceType?>

    @Query("SELECT DISTINCT extended_type FROM agency_properties WHERE is_installed = $BOOLEAN_TRUE AND extended_type IS NOT NULL")
    fun readingAllExtendedDataSourceTypes(): LiveData<List<DataSourceType?>>

    @Query("SELECT * FROM agency_properties WHERE id = :authority")
    suspend fun getAgency(authority: String): AgencyProperties?

    @Query("SELECT * FROM agency_properties WHERE id = :authority AND is_installed = $BOOLEAN_TRUE")
    fun readingAgency(authority: String): LiveData<AgencyProperties?>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM agency_properties WHERE id = :authority AND is_installed = $BOOLEAN_TRUE")
    fun readingAgencyBase(authority: String): LiveData<AgencyBaseProperties?>

    @Query("SELECT pkg FROM agency_properties WHERE id = :authority")
    suspend fun getAgencyPkg(authority: String): String

    @Query("SELECT color_int FROM agency_properties WHERE id = :authority")
    suspend fun getAgencyColorInt(authority: String): Int?
}