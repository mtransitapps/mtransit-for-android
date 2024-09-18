package org.mtransit.android.ui.view.poi.serviceupdate

import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.shortUUID

data class POIServiceUpdateViewHolder(
    var uuid: String,
    val serviceUpdateImg: ImageView?
) {

    fun hideServiceUpdate() {
        serviceUpdateImg?.isVisible = false
    }

    fun fetchAndUpdate(poim: POIManager, dataProvider: POIServiceUpdateProvider) {
        val serviceUpdates = fetch(dataProvider, poim)
        update(serviceUpdates, dataProvider)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun fetch(
        dataProvider: POIServiceUpdateProvider,
        poim: POIManager
    ): List<ServiceUpdate>? {
        return if (dataProvider.isShowingServiceUpdates && serviceUpdateImg != null) {
            poim.setServiceUpdateLoaderListener(dataProvider)
            poim.getServiceUpdates(dataProvider.providesServiceUpdateLoader())
        } else null
    }

    fun update(serviceUpdates: List<ServiceUpdate>?, dataProvider: POIServiceUpdateProvider) {
        if (serviceUpdateImg == null) {
            return
        }
        if (!dataProvider.isShowingServiceUpdates) {
            serviceUpdateImg.isVisible = false
            return
        }
        val (isWarning, isInfo) = serviceUpdates
            .let {
                ServiceUpdate.isSeverityWarning(it) to ServiceUpdate.isSeverityInfo(it)
            }
        if (isWarning) {
            serviceUpdateImg.setImageResource(R.drawable.ic_warning_on_surface_16dp)
            serviceUpdateImg.isVisible = true
        } else if (isInfo) {
            serviceUpdateImg.setImageResource(R.drawable.ic_info_outline_on_surface_16dp)
            serviceUpdateImg.isVisible = true
        } else {
            serviceUpdateImg.setImageDrawable(null)
            serviceUpdateImg.isVisible = false
        }
    }

    companion object {
        private val LOG_TAG: String = POIServiceUpdateViewHolder::class.java.simpleName

        @JvmStatic
        fun init(poi: POI, view: View) = POIServiceUpdateViewHolder(
            uuid = poi.uuid,
            serviceUpdateImg = view.findViewById(R.id.service_update_img)
        )

        @JvmStatic
        fun updateView(serviceUpdateViewHolder: POIServiceUpdateViewHolder?, serviceUpdates: List<ServiceUpdate>?, dataProvider: POIServiceUpdateProvider) {
            serviceUpdateViewHolder?.update(serviceUpdates, dataProvider)
        }

        @JvmStatic
        fun fetchAndUpdateView(serviceUpdateViewHolder: POIServiceUpdateViewHolder?, poim: POIManager, dataProvider: POIServiceUpdateProvider) {
            serviceUpdateViewHolder?.fetchAndUpdate(poim, dataProvider)
        }
    }
}