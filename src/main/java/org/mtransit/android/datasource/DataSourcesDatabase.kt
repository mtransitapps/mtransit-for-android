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
import org.mtransit.commons.sql.SQLUtils

@Database(
    entities = [
        AgencyProperties::class,
        StatusProviderProperties::class,
        ScheduleProviderProperties::class,
        ServiceUpdateProviderProperties::class,
        NewsProviderProperties::class,
    ],
    version = 4,
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

        private val LOG_TAG: String = DataSourcesDatabase::class.java.simpleName

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MTLog.i(LOG_TAG, "DB migration from version 1 to 2...")
                db.execSQL(
                    "ALTER TABLE agency_properties ADD COLUMN available_version_code INTEGER NOT NULL DEFAULT -1"
                )
                MTLog.i(LOG_TAG, "DB migration from version 1 to 2... DONE")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MTLog.i(LOG_TAG, "DB migration from version 2 to 3...")
                db.execSQL(
                    "ALTER TABLE agency_properties ADD COLUMN contact_us_web ${SQLUtils.TXT} DEFAULT null"
                )
                db.execSQL(
                    "ALTER TABLE agency_properties ADD COLUMN contact_us_web_fr ${SQLUtils.TXT} DEFAULT null"
                )
                MTLog.i(LOG_TAG, "DB migration from version 2 to 3... DONE")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MTLog.i(LOG_TAG, "DB migration from version 3 to 4...")
                db.execSQL(
                    "ALTER TABLE agency_properties ADD COLUMN extended_type ${SQLUtils.INT} DEFAULT null"
                )
                MTLog.i(LOG_TAG, "DB migration from version 3 to 4... DONE")
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
            // if (true) { // DEBUG - force clear DB
            // appContext.deleteDatabase(DB_NAME)
            // } // DEBUG - force clear DB
            return Room
                .databaseBuilder(
                    appContext,
                    DataSourcesDatabase::class.java,
                    DB_NAME
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}