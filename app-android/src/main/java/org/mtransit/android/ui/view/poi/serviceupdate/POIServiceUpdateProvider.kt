package org.mtransit.android.ui.view.poi.serviceupdate

import org.mtransit.android.task.ServiceUpdateLoader

interface POIServiceUpdateProvider : ServiceUpdateLoader.ServiceUpdateLoaderListener {
    val isShowingServiceUpdates: Boolean
    fun providesServiceUpdateLoader(): ServiceUpdateLoader
}