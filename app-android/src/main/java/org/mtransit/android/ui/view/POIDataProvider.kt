package org.mtransit.android.ui.view

import android.app.Activity
import android.location.Location
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.task.serviceupdate.ServiceUpdateLoaderProvider
import org.mtransit.android.ui.view.poi.status.POIStatusDataProvider
import org.mtransit.android.util.LinkUtils.OnUrlClickListener

interface POIDataProvider : POIStatusDataProvider, ServiceUpdateLoaderProvider, OnUrlClickListener {
    val activity: Activity?
    val isShowingExtra: Boolean
    fun isClosestPOI(uuid: String): Boolean
    fun isFavorite(uuid: String): Boolean
    val isShowingFavorite: Boolean
    val locationDeclination: Float?
    val lastCompassInDegree: Int?
    val deviceLocation: Location?
    fun hasLastCompassInDegree(): Boolean
    fun hasLocation(): Boolean
    fun providesDataSourcesRepository(): DataSourcesRepository
    fun providesDemoModeManager(): DemoModeManager
    val isShowingAccessibilityInfo: Boolean
}