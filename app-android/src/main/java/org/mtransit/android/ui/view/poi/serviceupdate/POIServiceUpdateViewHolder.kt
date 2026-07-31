package org.mtransit.android.ui.view.poi.serviceupdate

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdates
import org.mtransit.android.data.POIManager
import org.mtransit.android.task.serviceupdate.ServiceUpdateLoaderProvider
import org.mtransit.android.ui.view.common.setImageResourceAndVisibility

data class POIServiceUpdateViewHolder @JvmOverloads constructor(
    private val initialTargetUUID: String,
    val serviceUpdateImg: ImageView?,
    val otherServiceUpdateImg: ImageView? = null,
    var ignoredOtherTargetUUIDsOrUnknown: Collection<String>? = null,
) : MTLog.Loggable {

    var targetUUID: String = initialTargetUUID
        private set

    constructor(
        poi: POI,
        serviceUpdateImg: ImageView?,
        otherServiceUpdateImg: ImageView? = null,
    ) : this(
        initialTargetUUID = poi.uuid,
        serviceUpdateImg = serviceUpdateImg,
        otherServiceUpdateImg = otherServiceUpdateImg,
        ignoredOtherTargetUUIDsOrUnknown = (poi as? RouteDirectionStop)?.routeDirectionAllUUIDs
    )

    override fun getLogTag() = LOG_TAG

    fun setTarget(poi: POI) {
        targetUUID = poi.uuid
        ignoredOtherTargetUUIDsOrUnknown = (poi as? RouteDirectionStop)?.routeDirectionAllUUIDs
    }

    fun fetchAndUpdate(poim: POIManager, dataProvider: ServiceUpdateLoaderProvider) {
        update(
            allServiceUpdates = fetch(dataProvider, poim),
            dataProvider,
        )
    }

    fun fetch(
        dataProvider: ServiceUpdateLoaderProvider,
        poim: POIManager
    ): ServiceUpdates {
        return if (dataProvider.isShowingServiceUpdates && serviceUpdateImg != null) {
            poim.addServiceUpdateLoaderListener(dataProvider)
            poim.getServiceUpdates(
                dataProvider.providesServiceUpdateLoader(),
                emptyList() // filter later
            )
        } else ServiceUpdates.newEmpty()
    }

    fun update(
        allServiceUpdates: ServiceUpdates,
        dataProvider: ServiceUpdateLoaderProvider,
    ) {
        serviceUpdateImg?.update(allServiceUpdates, dataProvider, other = false)
        otherServiceUpdateImg?.update(allServiceUpdates, dataProvider, other = true)
    }

    private fun ImageView.update(
        allServiceUpdates: ServiceUpdates,
        dataProvider: ServiceUpdateLoaderProvider,
        other: Boolean,
    ) {
        if (!dataProvider.isShowingServiceUpdates) {
            this.isVisible = false
            return
        }
        val filteredServiceUpdates = allServiceUpdates
            .filter { !dataProvider.ignoredTargetUUIDsOrUnknown.orEmpty().contains(it.targetUUID) }
            .filter {
                if (other) ignoredOtherTargetUUIDsOrUnknown.orEmpty().contains(it.targetUUID)
                else !ignoredOtherTargetUUIDsOrUnknown.orEmpty().contains(it.targetUUID)
            }
            .distinctByOriginalId()
        val (isWarning, isInfo) = filteredServiceUpdates.isSeverityWarningXorInfo()
        if (isWarning) {
            this.setImageResourceAndVisibility(R.drawable.ic_warning_on_surface_16dp)
        } else if (isInfo) {
            this.setImageResourceAndVisibility(R.drawable.ic_info_outline_on_surface_16dp)
        } else {
            this.setImageResourceAndVisibility(null)
        }
        if (DEBUG_LAYOUT) {
            this.setImageResourceAndVisibility(R.drawable.ic_warning_on_surface_16dp)
            this.setBackgroundColor(Color.RED)
        }
    }

    companion object {

        private val LOG_TAG: String = POIServiceUpdateViewHolder::class.java.simpleName

        private const val DEBUG_LAYOUT = false
        // private const val DEBUG_LAYOUT = true // DEBUG

        @JvmOverloads
        @JvmStatic
        fun init(poi: POI, view: View, otherView: ImageView? = null) = POIServiceUpdateViewHolder(
            poi = poi,
            serviceUpdateImg = view.findViewById(R.id.poi_status_service_update_img),
            otherServiceUpdateImg = otherView,
        )

        @JvmStatic
        fun updateView(
            serviceUpdateViewHolder: POIServiceUpdateViewHolder?,
            allServiceUpdates: ServiceUpdates,
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