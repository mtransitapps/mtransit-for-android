package org.mtransit.android.di

import android.content.Context
import android.content.pm.PackageManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.analytics.AnalyticsManager
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.billing.MTBillingManager
import org.mtransit.android.datasource.DataSourcesDatabase
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.dev.CrashlyticsCrashReporter
import org.mtransit.android.dev.IStrictMode
import org.mtransit.android.dev.LeakCanaryDetector
import org.mtransit.android.dev.LeakDetector
import org.mtransit.android.dev.StrictModeImpl
import org.mtransit.android.provider.location.GoogleLocationProvider
import org.mtransit.android.provider.location.MTLocationProvider
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.provider.sensor.SensorManagerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Singleton
    @Binds
    abstract fun bindAdManager(adManager: AdManager): IAdManager

    @Singleton
    @Binds
    abstract fun bindAnalyticsService(analyticsManager: AnalyticsManager): IAnalyticsManager

    @Singleton
    @Binds
    abstract fun bindBillingManager(billingManager: MTBillingManager): IBillingManager

    @Singleton
    @Binds
    abstract fun bindCrashReporter(crashlyticsCrashReporter: CrashlyticsCrashReporter): CrashReporter

    @Singleton
    @Binds
    abstract fun bindLeakDetector(leakCanaryDetector: LeakCanaryDetector): LeakDetector

    @Singleton
    @Binds
    abstract fun bindStrictMode(strictModeImpl: StrictModeImpl): IStrictMode

    @Singleton
    @Binds
    abstract fun bindLocationProvider(googleLocationProvider: GoogleLocationProvider): MTLocationProvider

    @Singleton
    @Binds
    abstract fun bindSensorManager(sensorManager: SensorManagerImpl): MTSensorManager

    companion object {
        @Singleton
        @Provides
        fun provideDataSourcesDatabase(
            @ApplicationContext appContext: Context,
        ): DataSourcesDatabase {
            return DataSourcesDatabase.getInstance(appContext)
        }

        @Singleton
        @Provides
        fun providesPackageManager(
            @ApplicationContext appContext: Context,
        ): PackageManager {
            return appContext.packageManager
        }
    }
}