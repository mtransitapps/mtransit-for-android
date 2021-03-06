package org.mtransit.android.datasource

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.NewsProviderProperties
import org.mtransit.android.data.ScheduleProviderProperties
import org.mtransit.android.data.ServiceUpdateProviderProperties
import org.mtransit.android.data.StatusProviderProperties

@Database(
    entities = [
        AgencyProperties::class,
        StatusProviderProperties::class,
        ScheduleProviderProperties::class,
        ServiceUpdateProviderProperties::class,
        NewsProviderProperties::class,
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(DataSourcesConverters::class)
abstract class DataSourcesDatabase : RoomDatabase() {

    abstract fun agencyPropertiesDao(): AgencyPropertiesDao

    abstract fun statusProviderPropertiesDao(): StatusProviderPropertiesDao

    abstract fun scheduleProviderPropertiesDao(): ScheduleProviderPropertiesDao

    abstract fun serviceUpdateProviderPropertiesDao(): ServiceUpdateProviderPropertiesDao

    abstract fun newsProviderPropertiesDao(): NewsProviderPropertiesDao

    companion object {

        private const val DB_NAME = "data_sources.db"

        val LOG_TAG = DataSourcesDatabase::class.simpleName

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                MTLog.i(LOG_TAG, "DB migration from version 1 to 2...")
                database.execSQL(
                    "ALTER TABLE agency_properties ADD COLUMN available_version_code INTEGER NOT NULL DEFAULT -1"
                )
                MTLog.i(LOG_TAG, "DB migration from version 1 to 2... DONE")
            }
        }

        @Volatile
        private var instance: DataSourcesDatabase? = null

        @JvmStatic
        fun getInstance(appContext: Context): DataSourcesDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(appContext).also { instance = it }
            }
        }

        private fun buildDatabase(appContext: Context): DataSourcesDatabase {
            return Room
                .databaseBuilder(
                    appContext,
                    DataSourcesDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}