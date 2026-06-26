package org.mtransit.android.task.serviceupdate

import org.mtransit.android.commons.data.ServiceUpdates
import org.mtransit.android.task.ServiceUpdateLoader

interface ServiceUpdatesHolder {
    fun getServiceUpdates(serviceUpdateLoader: ServiceUpdateLoader, ignoredUUIDsOrUnknown: Collection<String>?): ServiceUpdates
    fun addServiceUpdateLoaderListener(serviceUpdateLoaderListener: ServiceUpdateLoader.ServiceUpdateLoaderListener)
}
