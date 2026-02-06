package org.mtransit.android.data

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.ServiceUpdateLoader.ServiceUpdateLoaderListener
import org.mtransit.android.task.serviceupdate.ServiceUpdatesHolder
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.CollectionUtils
import java.util.WeakHashMap

data class RouteManager(
    val authority: String,
    val route: Route,
    private val tripIds: Collection<String>? = null,
    private val serviceUpdates: MutableList<ServiceUpdate> = mutableListOf(),
    private var lastFindServiceUpdateTimestampMs: Long = -1L,
    private var inFocus: Boolean = false, // TODO?
) : ServiceUpdateLoaderListener, ServiceUpdatesHolder, MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RouteManager::class.java.simpleName
    }

    @Suppress("SENSELESS_COMPARISON")
    override fun getLogTag(): String {
        if (this.route != null) {
            return LOG_TAG + "-" + this.route.uuid.removePrefix(IAgencyProperties.PKG_COMMON)
        }
        return LOG_TAG
    }

    private val serviceUpdateLoaderListenersWR = WeakHashMap<ServiceUpdateLoaderListener, Void?>()

    override fun addServiceUpdateLoaderListener(serviceUpdateLoaderListener: ServiceUpdateLoaderListener) {
        this.serviceUpdateLoaderListenersWR[serviceUpdateLoaderListener] = null
    }

    override fun onServiceUpdatesLoaded(targetUUID: String, serviceUpdates: List<ServiceUpdate>?) {
        setServiceUpdates(serviceUpdates)
    }

    fun setServiceUpdates(newServiceUpdates: Collection<ServiceUpdate>?) {
        if (this.serviceUpdates.isNotEmpty()) {
            this.serviceUpdates.clear()
        }
        if (newServiceUpdates != null) {
            this.serviceUpdates.addAll(newServiceUpdates)
            CollectionUtils.sort(this.serviceUpdates, ServiceUpdate.HIGHER_SEVERITY_FIRST_COMPARATOR)
        }
    }

    override fun getServiceUpdates(serviceUpdateLoader: ServiceUpdateLoader, ignoredUUIDsOrUnknown: Collection<String>?): List<ServiceUpdate> {
        if (this.serviceUpdates.isEmpty() || this.lastFindServiceUpdateTimestampMs < 0L || this.inFocus || !areServiceUpdatesUseful) {
            findServiceUpdates(serviceUpdateLoader, skipIfBusy = false)
        }
        if (ignoredUUIDsOrUnknown == null) return emptyList() // IF filter not ready DO wait for filter
        return this.serviceUpdates
            .filter { !ignoredUUIDsOrUnknown.contains(it.targetUUID) }
    }

    private val areServiceUpdatesUseful: Boolean
        get() = this.serviceUpdates.any { it.isUseful }

    private fun findServiceUpdates(
        serviceUpdateLoader: ServiceUpdateLoader,
        @Suppress("SameParameterValue") skipIfBusy: Boolean
    ): Boolean {
        val findServiceUpdateTimestampMs = UITimeUtils.currentTimeToTheMinuteMillis()
        var isNotSkipped = false
        if (this.lastFindServiceUpdateTimestampMs != findServiceUpdateTimestampMs) { // IF not same minute as last findStatus() call DO
            isNotSkipped = serviceUpdateLoader.findServiceUpdate(
                this,
                ServiceUpdateProviderContract.Filter(this.authority, this.route, tripIds).apply {
                    setInFocus(inFocus)
                },
                this.serviceUpdateLoaderListenersWR.keys,
                skipIfBusy
            )
            if (isNotSkipped) {
                this.lastFindServiceUpdateTimestampMs = findServiceUpdateTimestampMs
            }
        }
        return isNotSkipped
    }
}

fun Route.toRouteM(authority: String, tripIds: Collection<String>? = null, serviceUpdates: List<ServiceUpdate>? = null) =
    RouteManager(authority, this, tripIds, serviceUpdates.orEmpty().toMutableList())
