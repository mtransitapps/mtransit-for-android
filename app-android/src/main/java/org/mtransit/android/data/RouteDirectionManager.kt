package org.mtransit.android.data

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.ServiceUpdates
import org.mtransit.android.commons.data.orNewEmpty
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.ServiceUpdateLoader.ServiceUpdateLoaderListener
import org.mtransit.android.task.serviceupdate.ServiceUpdatesHolder
import org.mtransit.android.util.UITimeUtils
import java.util.WeakHashMap

data class RouteDirectionManager(
    val authority: String,
    val routeDirection: RouteDirection,
    private var serviceUpdates: ServiceUpdates? = null,
    private var lastTriggerServiceUpdateRefreshMinTimestampMs: Long = -1L,
    private var inFocus: Boolean = false, // TODO?
) : ServiceUpdateLoaderListener, ServiceUpdatesHolder, MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RouteDirectionManager::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG + "-" + this.routeDirection.uuid.removePrefix(IAgencyProperties.PKG_COMMON)

    private val serviceUpdateLoaderListenersWR = WeakHashMap<ServiceUpdateLoaderListener, Void?>()

    override fun addServiceUpdateLoaderListener(serviceUpdateLoaderListener: ServiceUpdateLoaderListener) {
        this.serviceUpdateLoaderListenersWR[serviceUpdateLoaderListener] = null
    }

    override fun removeServiceUpdateLoaderListener(serviceUpdateLoaderListener: ServiceUpdateLoaderListener) {
        this.serviceUpdateLoaderListenersWR.remove(serviceUpdateLoaderListener)
    }

    override fun onServiceUpdatesLoaded(targetUUID: String, serviceUpdates: ServiceUpdates) {
        setServiceUpdates(serviceUpdates)
    }

    fun setServiceUpdates(newServiceUpdates: ServiceUpdates) {
        if (newServiceUpdates == this.serviceUpdates) return
        this.serviceUpdates = newServiceUpdates.apply {
            sortWith(ServiceUpdate.HIGHER_SEVERITY_FIRST_COMPARATOR)
        }
    }

    override fun getServiceUpdates(serviceUpdateLoader: ServiceUpdateLoader, ignoredUUIDsOrUnknown: Collection<String>?): ServiceUpdates {
        if (this.lastTriggerServiceUpdateRefreshMinTimestampMs != UITimeUtils.currentTimeToTheMinuteMillis()) { // fetch NOT already triggered
            triggerServiceUpdatesRefresh(serviceUpdateLoader, skipIfBusy = false)
        }
        ignoredUUIDsOrUnknown ?: return ServiceUpdates.newEmpty() // IF filter not ready DO wait for filter
        return this.serviceUpdates
            ?.filter { !ignoredUUIDsOrUnknown.contains(it.targetUUID) }
            ?: ServiceUpdates.newEmpty()
    }

    fun allowTriggerServiceUpdatesRefresh() {
        this.lastTriggerServiceUpdateRefreshMinTimestampMs = -1L
    }

    private fun triggerServiceUpdatesRefresh(
        serviceUpdateLoader: ServiceUpdateLoader,
        @Suppress("SameParameterValue") skipIfBusy: Boolean
    ): Boolean {
        // IF not same minute as last triggerRefresh() call DO
        val filter = ServiceUpdateProviderContract.Filter(this.authority, this.routeDirection)
        val isNotSkipped = serviceUpdateLoader.triggerRefresh(this, filter, this.serviceUpdateLoaderListenersWR.keys, skipIfBusy)
        if (isNotSkipped) {
            this.lastTriggerServiceUpdateRefreshMinTimestampMs = UITimeUtils.currentTimeToTheMinuteMillis() // rounded to MINUTES
        }
        return isNotSkipped
    }
}

fun RouteDirection.toRouteDirectionM(authority: String, serviceUpdates: ServiceUpdates? = null) =
    RouteDirectionManager(authority, this, serviceUpdates.orNewEmpty())
