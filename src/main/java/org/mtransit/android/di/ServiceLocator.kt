package org.mtransit.android.di

import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.analytics.AnalyticsManager
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.IApplication
import org.mtransit.android.data.DataSourceProvider
import org.mtransit.android.data.source.DataSourceRepository
import org.mtransit.android.data.source.NewsRepository
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.dev.CrashlyticsCrashReporter
import org.mtransit.android.dev.IStrictMode
import org.mtransit.android.dev.LeakCanaryDetector
import org.mtransit.android.dev.LeakDetector
import org.mtransit.android.dev.StrictModeImpl
import org.mtransit.android.provider.location.GoogleLocationProvider
import org.mtransit.android.provider.location.MTLocationProvider
import org.mtransit.android.provider.permission.LocationPermissionProvider
import org.mtransit.android.provider.sensor.MTSensorManager
import org.mtransit.android.provider.sensor.SensorManagerImpl
import org.mtransit.android.ui.MTApplication


object ServiceLocator {

    @JvmStatic
    val application: IApplication by lazy {
        MTApplication.getIApplication()
    }

    @JvmStatic
    val leakDetector: LeakDetector by lazy {
        LeakCanaryDetector()
    }

    @JvmStatic
    val strictMode: IStrictMode by lazy {
        StrictModeImpl()
    }

    @JvmStatic
    val crashReporter: CrashReporter by lazy {
        CrashlyticsCrashReporter()
    }

    @JvmStatic
    val locationPermissionProvider: LocationPermissionProvider by lazy {
        LocationPermissionProvider()
    }

    @JvmStatic
    val locationProvider: MTLocationProvider by lazy {
        GoogleLocationProvider(
            application,
            locationPermissionProvider,
            crashReporter
        )
    }

    @JvmStatic
    val sensorManager: MTSensorManager by lazy {
        SensorManagerImpl(
            application
        )
    }

    @JvmStatic
    val adManager: IAdManager by lazy {
        AdManager(
            application,
            crashReporter,
            locationProvider
        )
    }

    @JvmStatic
    val analyticsManager: IAnalyticsManager by lazy {
        AnalyticsManager(
            application
        )
    }

    @JvmStatic
    val dataSourceProvider: DataSourceProvider by lazy {
        DataSourceProvider(
            application,
            analyticsManager
        )
    }

    @JvmStatic
    val dataSourceRepository: DataSourceRepository by lazy {
        DataSourceRepository(
            application
        )
    }

    @JvmStatic
    val newsRepository: NewsRepository by lazy {
        NewsRepository(
            dataSourceProvider,
            dataSourceRepository
        )
    }
}