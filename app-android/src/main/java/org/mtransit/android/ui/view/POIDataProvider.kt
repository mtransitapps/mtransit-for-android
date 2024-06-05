package org.mtransit.android.ui.view

import android.app.Activity
import android.location.Location
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.ServiceUpdateLoader.ServiceUpdateLoaderListener
import org.mtransit.android.task.StatusLoader
import org.mtransit.android.task.StatusLoader.StatusLoaderListener
import org.mtransit.android.util.LinkUtils.OnUrlClickListener

interface POIDataProvider : StatusLoaderListener, ServiceUpdateLoaderListener, OnUrlClickListener {
    val isShowingStatus: Boolean
    val activity: Activity?
    val isShowingExtra: Boolean
    val nowToTheMinute: Long
    fun isClosestPOI(uuid: String): Boolean
    fun isFavorite(uuid: String): Boolean
    val isShowingFavorite: Boolean
    val locationDeclination: Float?
    val lastCompassInDegree: Int?
    val location: Location?
    fun hasLastCompassInDegree(): Boolean
    fun hasLocation(): Boolean
    val isShowingServiceUpdates: Boolean
    fun providesDataSourcesRepository(): DataSourcesRepository
    fun providesStatusLoader(): StatusLoader
    fun providesServiceUpdateLoader(): ServiceUpdateLoader
    fun providesDemoModeManager(): DemoModeManager
    val isShowingAccessibilityInfo: Boolean
}