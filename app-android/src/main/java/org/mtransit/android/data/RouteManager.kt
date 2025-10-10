package org.mtransit.android.data

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.provider.ServiceUpdateProviderContract
import org.mtransit.android.task.ServiceUpdateLoader
import org.mtransit.android.task.ServiceUpdateLoader.ServiceUpdateLoaderListener
import org.mtransit.android.util.UITimeUtils
import org.mtransit.commons.CollectionUtils
import java.lang.ref.WeakReference
import kotlin.collections.orEmpty

data class RouteManager(
    val authority: Authority,
    val route: Route,
    private val serviceUpdates: MutableList<ServiceUpdate> = mutableListOf(),
    private var lastFindServiceUpdateTimestampMs: Long = -1L,
    private var inFocus: Boolean = false, // TODO?
) : ServiceUpdateLoaderListener, MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RouteManager::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private var serviceUpdateLoaderListenerWR: WeakReference<ServiceUpdateLoaderListener>? = null

    @Suppress("unused")
    fun setServiceUpdateLoaderListener(serviceUpdateLoaderListener: ServiceUpdateLoaderListener) {
        this.serviceUpdateLoaderListenerWR = WeakReference<ServiceUpdateLoaderListener>(serviceUpdateLoaderListener)
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

    fun getServiceUpdates(serviceUpdateLoader: ServiceUpdateLoader): List<ServiceUpdate> {
        if (this.serviceUpdates.isNotEmpty() || this.lastFindServiceUpdateTimestampMs < 0L || this.inFocus || !areServiceUpdatesUseful()) {
            findServiceUpdates(serviceUpdateLoader, skipIfBusy = false)
        }
        return this.serviceUpdates
    }

    private fun areServiceUpdatesUseful(): Boolean {
        for (serviceUpdate in this.serviceUpdates) {
            if (serviceUpdate.isUseful) {
                return true
            }
        }
        return false
    }

    private fun findServiceUpdates(
        serviceUpdateLoader: ServiceUpdateLoader,
        skipIfBusy: Boolean
    ): Boolean {
        val findServiceUpdateTimestampMs = UITimeUtils.currentTimeToTheMinuteMillis()
        var isNotSkipped = false
        if (this.lastFindServiceUpdateTimestampMs != findServiceUpdateTimestampMs) { // IF not same minute as last findStatus() call DO
            isNotSkipped = serviceUpdateLoader.findServiceUpdate(
                this,
                ServiceUpdateProviderContract.Filter(this.route).apply {
                    setInFocus(inFocus)
                },
                this.serviceUpdateLoaderListenerWR?.get(),
                skipIfBusy
            )
            if (isNotSkipped) {
                this.lastFindServiceUpdateTimestampMs = findServiceUpdateTimestampMs
            }
        }
        return isNotSkipped
    }
}

fun Route.toRouteM(authority: Authority, serviceUpdates: List<ServiceUpdate>? = null) =
    RouteManager(authority, this, serviceUpdates.orEmpty().toMutableList())
