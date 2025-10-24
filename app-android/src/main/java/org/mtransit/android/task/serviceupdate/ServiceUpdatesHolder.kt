package org.mtransit.android.task.serviceupdate

import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.task.ServiceUpdateLoader

interface ServiceUpdatesHolder {
    fun getServiceUpdates(serviceUpdateLoader: ServiceUpdateLoader, ignoredUUIDsOrUnknown: Collection<String>?): List<ServiceUpdate>
    fun setServiceUpdateLoaderListener(serviceUpdateLoaderListener: ServiceUpdateLoader.ServiceUpdateLoaderListener)
}
