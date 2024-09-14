package org.mtransit.android.ui.view.poi.serviceupdate

import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import org.mtransit.android.R
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.data.POIManager
import org.mtransit.android.ui.view.POIDataProvider

data class POIServiceUpdateViewHolder(
    val serviceUpdateImg: ImageView?
) {

    fun hideServiceUpdate() {
        serviceUpdateImg?.isVisible = false
    }

    fun fetchAndUpdate(poim: POIManager, dataProvider: POIDataProvider) {
        val serviceUpdates = fetch(dataProvider, poim)
        update(serviceUpdates, dataProvider)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun fetch(
        dataProvider: POIDataProvider,
        poim: POIManager
    ) = if (dataProvider.isShowingServiceUpdates && serviceUpdateImg != null) {
        poim.setServiceUpdateLoaderListener(dataProvider)
        poim.getServiceUpdates(dataProvider.providesServiceUpdateLoader())
    } else null

    fun update(serviceUpdates: List<ServiceUpdate>?, dataProvider: POIDataProvider) {
        if (serviceUpdateImg == null) {
            return
        }
        if (!dataProvider.isShowingServiceUpdates) {
            serviceUpdateImg.isVisible = false
            return
        }
        val isServiceUpdateWarning = ServiceUpdate.isSeverityWarning(serviceUpdates)
        val isServiceUpdateInfo = ServiceUpdate.isSeverityInfo(serviceUpdates)
        if (isServiceUpdateWarning) {
            serviceUpdateImg.setImageResource(R.drawable.ic_warning_on_surface_16dp)
            serviceUpdateImg.isVisible = true
        } else if (isServiceUpdateInfo) {
            serviceUpdateImg.setImageResource(R.drawable.ic_info_outline_on_surface_16dp)
            serviceUpdateImg.isVisible = true
        } else {
            serviceUpdateImg.setImageDrawable(null)
            serviceUpdateImg.isVisible = false
        }
    }

    companion object {
        @JvmStatic
        fun init(view: View) = POIServiceUpdateViewHolder(
            serviceUpdateImg = view.findViewById(R.id.service_update_img)
        )

        @JvmStatic
        fun updateView(statusViewHolder: POIServiceUpdateViewHolder?, serviceUpdates: List<ServiceUpdate>?, dataProvider: POIDataProvider) {
            statusViewHolder?.update(serviceUpdates, dataProvider)
        }

        @JvmStatic
        fun fetchAndUpdateView(serviceUpdateViewHolder: POIServiceUpdateViewHolder?, poim: POIManager, dataProvider: POIDataProvider) {
            serviceUpdateViewHolder?.fetchAndUpdate(poim, dataProvider)
        }
    }
}