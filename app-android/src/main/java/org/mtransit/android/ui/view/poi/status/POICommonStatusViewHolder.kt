package org.mtransit.android.ui.view.poi.status

import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.data.POIManager

interface POICommonStatusViewHolder<VB : ViewBinding?, STATUS : POIStatus?> {

    @CallSuper
    fun bind(status: STATUS?, dataProvider: POIStatusDataProvider) {
        statusV.isVisible = status != null
    }

    fun setStatusVisible(visible: Boolean) {
        if (visible) {
            showStatus()
        } else {
            hideStatus()
        }
    }

    fun showStatus() {
        statusV.visibility = View.VISIBLE // NOT GONE!
    }

    fun hideStatus() {
        statusV.visibility = View.INVISIBLE // NOT GONE!
    }

    fun fetch(statusViewHolder: POICommonStatusViewHolder<*, *>?, poim: POIManager, dataProvider: POIStatusDataProvider): STATUS?

    fun update(statusViewHolder: POICommonStatusViewHolder<*, *>?, status: POIStatus?, dataProvider: POIStatusDataProvider)

    fun fetchAndUpdate(statusViewHolder: POICommonStatusViewHolder<*, *>?, poim: POIManager, dataProvider: POIStatusDataProvider) {
        val status = fetch(statusViewHolder, poim, dataProvider)
        update(statusViewHolder, status, dataProvider)
    }

    var uuid: String

    val statusV: View

    val binding: VB?

    companion object {

        private val LOG_TAG: String = POICommonStatusViewHolder::class.java.simpleName

        @JvmStatic
        fun bindStatusV(view: View): View = view.findViewById(R.id.status)

        @JvmStatic
        fun updateView(statusViewHolder: POICommonStatusViewHolder<*, *>?, poiStatus: POIStatus, dataProvider: POIStatusDataProvider) {
            statusViewHolder?.update(statusViewHolder, poiStatus, dataProvider)
        }

        @JvmStatic
        fun fetchAndUpdateView(statusViewHolder: POICommonStatusViewHolder<*, *>?, poim: POIManager, dataProvider: POIStatusDataProvider) {
            statusViewHolder?.fetchAndUpdate(statusViewHolder, poim, dataProvider)
        }

        @JvmStatic
        fun init(poi: POI, view: View): POICommonStatusViewHolder<out ViewBinding?, out POIStatus?>? {
            val statusView = view.findViewById<View?>(R.id.status) ?: return null
            return when (poi.statusType) {
                POI.ITEM_STATUS_TYPE_NONE -> null
                POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT -> POIAvailabilityStatusViewHolder.fromStatusView(statusView, poi.uuid)
                POI.ITEM_STATUS_TYPE_SCHEDULE -> POIScheduleViewHolder.fromStatusView(statusView, poi.uuid)
                POI.ITEM_STATUS_TYPE_APP -> POIAppStatusViewHolder.fromStatusView(statusView, poi.uuid)
                else -> {
                    MTLog.w(LOG_TAG, "Unexpected status '%s' (no view holder)!", poi.statusType)
                    null
                }
            }
        }

    }
}