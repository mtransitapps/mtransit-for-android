package org.mtransit.android.task.serviceupdate

import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.data.POIManager
import org.mtransit.android.task.ServiceUpdateLoader

class POIManagerHolder(
    private val poim: POIManager,
) : ServiceUpdatesHolder {

    override fun getServiceUpdates(serviceUpdateLoader: ServiceUpdateLoader, ignoredUUIDsOrUnknown: Collection<String>?): List<ServiceUpdate> {
        return poim.getServiceUpdates(serviceUpdateLoader, ignoredUUIDsOrUnknown) ?: emptyList()
    }

    override fun addServiceUpdateLoaderListener(serviceUpdateLoaderListener: ServiceUpdateLoader.ServiceUpdateLoaderListener) {
        poim.addServiceUpdateLoaderListener(serviceUpdateLoaderListener)
    }
}
