package org.mtransit.android.data

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.ServiceUpdates
import org.mtransit.android.commons.data.orNewEmpty
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.ServiceUpdateLoader.ServiceUpdateLoaderListener
import org.mtransit.android.task.serviceupdate.ServiceUpdatesHolder
import org.mtransit.android.util.UITimeUtils
import java.util.WeakHashMap

data class RouteManager(
    val authority: String,
    val route: Route,
    private val serviceUpdates: ServiceUpdates = ServiceUpdates(),
    private var lastTriggerServiceUpdateRefreshMinTimestampMs: Long = -1L,
) : ServiceUpdateLoaderListener, ServiceUpdatesHolder, MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RouteManager::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG + "-" + this.route.uuid.removePrefix(IAgencyProperties.PKG_COMMON)

    private val serviceUpdateLoaderListenersWR = WeakHashMap<ServiceUpdateLoaderListener, Void?>()

    override fun addServiceUpdateLoaderListener(serviceUpdateLoaderListener: ServiceUpdateLoaderListener) {
        this.serviceUpdateLoaderListenersWR[serviceUpdateLoaderListener] = null
    }

    override fun onServiceUpdatesLoaded(targetUUID: String, serviceUpdates: ServiceUpdates) {
        setServiceUpdates(serviceUpdates)
    }

    fun setServiceUpdates(newServiceUpdates: ServiceUpdates) {
        if (this.serviceUpdates.isNotEmpty()) {
            this.serviceUpdates.clear()
        }
        if (newServiceUpdates.isNotEmpty()) {
            this.serviceUpdates.addAll(newServiceUpdates)
            this.serviceUpdates.sortWith(ServiceUpdate.HIGHER_SEVERITY_FIRST_COMPARATOR)
        }
    }

    override fun getServiceUpdates(serviceUpdateLoader: ServiceUpdateLoader, ignoredUUIDsOrUnknown: Collection<String>?): ServiceUpdates {
        if (this.lastTriggerServiceUpdateRefreshMinTimestampMs != UITimeUtils.currentTimeToTheMinuteMillis()) {
            triggerServiceUpdatesRefresh(serviceUpdateLoader, skipIfBusy = false)
        }
        ignoredUUIDsOrUnknown ?: return ServiceUpdates.newEmpty() // IF filter not ready DO wait for filter
        return this.serviceUpdates
            .filter { !ignoredUUIDsOrUnknown.contains(it.targetUUID) }
    }

    private fun triggerServiceUpdatesRefresh(
        serviceUpdateLoader: ServiceUpdateLoader,
        @Suppress("SameParameterValue") skipIfBusy: Boolean
    ): Boolean {
        // IF not same minute as last triggerRefresh() call DO
        val filter = ServiceUpdateProviderContract.Filter(this.authority, this.route)
        val isNotSkipped = serviceUpdateLoader.triggerRefresh(this, filter, this.serviceUpdateLoaderListenersWR.keys, skipIfBusy)
        if (isNotSkipped) {
            this.lastTriggerServiceUpdateRefreshMinTimestampMs = UITimeUtils.currentTimeToTheMinuteMillis() // rounded to MINUTES
        }
        return isNotSkipped
    }
}

fun Route.toRouteM(authority: String, serviceUpdates: ServiceUpdates? = null) =
    RouteManager(authority, this, serviceUpdates.orNewEmpty())
