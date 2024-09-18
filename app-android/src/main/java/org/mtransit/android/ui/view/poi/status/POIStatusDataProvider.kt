package org.mtransit.android.ui.view.poi.status

import org.mtransit.android.task.StatusLoader

interface POIStatusDataProvider : StatusLoader.StatusLoaderListener {
    val isShowingStatus: Boolean
    val nowToTheMinute: Long
    fun providesStatusLoader(): StatusLoader
}