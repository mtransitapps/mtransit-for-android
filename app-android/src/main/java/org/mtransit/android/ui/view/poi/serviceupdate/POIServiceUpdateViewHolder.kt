package org.mtransit.android.ui.view.poi.serviceupdate

import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.data.POIManager
import org.mtransit.android.task.serviceupdate.ServiceUpdateLoaderProvider

data class POIServiceUpdateViewHolder @JvmOverloads constructor(
    private var _uuid: String,
    val serviceUpdateImg: ImageView?,
    val otherServiceUpdateImg: ImageView? = null,
    var ignoredOtherTargetUUIDsOrUnknown: Collection<String>? = null,
) : MTLog.Loggable {

    override fun getLogTag() = LOG_TAG

    val uuid: String get() = _uuid

    fun setTarget(poi: POI) {
        _uuid = poi.uuid
        ignoredOtherTargetUUIDsOrUnknown = (poi as? RouteDirectionStop)?.routeDirectionAllUUIDs
    }

    fun fetchAndUpdate(poim: POIManager, dataProvider: ServiceUpdateLoaderProvider) {
        val allServiceUpdates = fetch(dataProvider, poim)
        serviceUpdateImg?.update(allServiceUpdates, dataProvider, other = false)
        otherServiceUpdateImg?.update(allServiceUpdates, dataProvider, other = true)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun fetch(
        dataProvider: ServiceUpdateLoaderProvider,
        poim: POIManager
    ): List<ServiceUpdate>? {
        return if (dataProvider.isShowingServiceUpdates && serviceUpdateImg != null) {
            poim.addServiceUpdateLoaderListener(dataProvider)
            poim.getServiceUpdates(
                dataProvider.providesServiceUpdateLoader(),
                emptyList() // filter later
            )
        } else null
    }

    fun update(
        allServiceUpdates: List<ServiceUpdate>?,
        dataProvider: ServiceUpdateLoaderProvider,
    ) {
        serviceUpdateImg?.update(allServiceUpdates, dataProvider, other = false)
        otherServiceUpdateImg?.update(allServiceUpdates, dataProvider, other = true)
    }

    private fun ImageView.update(
        allServiceUpdates: List<ServiceUpdate>?,
        dataProvider: ServiceUpdateLoaderProvider,
        other: Boolean,
    ) {
        if (!dataProvider.isShowingServiceUpdates) {
            this.isVisible = false
            return
        }
        val filteredServiceUpdates = allServiceUpdates
            ?.filter { !dataProvider.ignoredTargetUUIDsOrUnknown.orEmpty().contains(it.targetUUID) }
            ?.filter {
                if (other) ignoredOtherTargetUUIDsOrUnknown.orEmpty().contains(it.targetUUID)
                else !ignoredOtherTargetUUIDsOrUnknown.orEmpty().contains(it.targetUUID)
            }
        val (isWarning, isInfo) = filteredServiceUpdates
            .let {
                ServiceUpdate.isSeverityWarning(it) to ServiceUpdate.isSeverityInfo(it)
            }
        if (isWarning) {
            this.setImageResource(R.drawable.ic_warning_on_surface_16dp)
            this.isVisible = true
        } else if (isInfo) {
            this.setImageResource(R.drawable.ic_info_outline_on_surface_16dp)
            this.isVisible = true
        } else {
            this.setImageDrawable(null)
            this.isVisible = false
        }
    }

    companion object {

        private val LOG_TAG: String = POIServiceUpdateViewHolder::class.java.simpleName

        @JvmOverloads
        @JvmStatic
        fun init(poi: POI, view: View, otherView: ImageView? = null) = POIServiceUpdateViewHolder(
            _uuid = poi.uuid,
            serviceUpdateImg = view.findViewById(R.id.poi_status_service_update_img),
            otherServiceUpdateImg = otherView,
        ).apply {
            setTarget(poi)
        }

        @JvmStatic
        fun updateView(
            serviceUpdateViewHolder: POIServiceUpdateViewHolder?,
            allServiceUpdates: List<ServiceUpdate>?,
            dataProvider: ServiceUpdateLoaderProvider
        ) {
            serviceUpdateViewHolder?.update(allServiceUpdates, dataProvider)
        }

        @JvmStatic
        fun fetchAndUpdateView(serviceUpdateViewHolder: POIServiceUpdateViewHolder?, poim: POIManager, dataProvider: ServiceUpdateLoaderProvider) {
            serviceUpdateViewHolder?.fetchAndUpdate(poim, dataProvider)
        }
    }
}