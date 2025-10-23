package org.mtransit.android.task.serviceupdate

import org.mtransit.android.task.ServiceUpdateLoader

interface ServiceUpdateLoaderProvider : ServiceUpdateLoader.ServiceUpdateLoaderListener {
    val isShowingServiceUpdates: Boolean
    fun providesServiceUpdateLoader(): ServiceUpdateLoader
    val ignoredTargetUUIDs: Collection<String>?
}