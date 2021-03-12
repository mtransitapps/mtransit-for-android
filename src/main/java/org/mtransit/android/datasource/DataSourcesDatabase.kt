package org.mtransit.android.datasource

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.mtransit.android.common.IApplication
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
    version = 1,
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

        @Volatile
        private var instance: DataSourcesDatabase? = null

        @JvmStatic
        fun getInstance(app: IApplication): DataSourcesDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(app).also { instance = it }
            }
        }

        private fun buildDatabase(app: IApplication): DataSourcesDatabase {
            return Room
                .databaseBuilder(
                    app.requireApplication(),
                    DataSourcesDatabase::class.java,
                    DB_NAME
                )
                .build()
        }
    }
}