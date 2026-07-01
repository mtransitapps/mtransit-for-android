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
import kotlin.time.Duration.Companion.minutes

data class RouteDirectionManager(
    val authority: String,
    val routeDirection: RouteDirection,
    private val serviceUpdates: ServiceUpdates = ServiceUpdates(),
    private var lastTriggerServiceUpdateRefreshMinTimestampMs: Long = -1L,
) : ServiceUpdateLoaderListener, ServiceUpdatesHolder, MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RouteDirectionManager::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG + "-" + this.routeDirection.uuid.removePrefix(IAgencyProperties.PKG_COMMON)

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

    fun allowTriggerServiceUpdatesRefresh() {
        this.lastTriggerServiceUpdateRefreshMinTimestampMs -= 1.minutes.inWholeMilliseconds
    }

    private fun triggerServiceUpdatesRefresh(
        serviceUpdateLoader: ServiceUpdateLoader,
        @Suppress("SameParameterValue") skipIfBusy: Boolean
    ): Boolean {
        val triggerServiceUpdateRefreshMinTimestampMs = UITimeUtils.currentTimeToTheMinuteMillis() // rounded to MINUTES
        if (this.lastTriggerServiceUpdateRefreshMinTimestampMs == triggerServiceUpdateRefreshMinTimestampMs) return false
        // IF not same minute as last triggerRefresh() call DO
        val filter = ServiceUpdateProviderContract.Filter(this.authority, this.routeDirection)
        val isNotSkipped = serviceUpdateLoader.triggerRefresh(this, filter, this.serviceUpdateLoaderListenersWR.keys, skipIfBusy)
        if (isNotSkipped) {
            this.lastTriggerServiceUpdateRefreshMinTimestampMs = triggerServiceUpdateRefreshMinTimestampMs
        }
        return isNotSkipped
    }
}

fun RouteDirection.toRouteDirectionM(authority: String, serviceUpdates: ServiceUpdates? = null) =
    RouteDirectionManager(authority, this, serviceUpdates.orNewEmpty())
