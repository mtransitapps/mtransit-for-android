package org.mtransit.android.ui.view.poi.status

import android.content.Context
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import org.mtransit.android.R
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.POI.ItemStatusType
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.data.POIManager
import org.mtransit.android.ui.view.POIDataProvider

interface POICommonStatusViewHolder<VB : ViewBinding?, STATUS : POIStatus?> {

    @CallSuper
    fun bind(status: STATUS?, dataProvider: POIDataProvider) {
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

    fun fetchStatus(dataProvider: POIDataProvider, statusViewHolder: POICommonStatusViewHolder<*, *>?, poim: POIManager, context: Context): STATUS?

    fun updateStatus(context: Context, statusViewHolder: POICommonStatusViewHolder<*, *>?, status: POIStatus?, dataProvider: POIDataProvider)

    fun fetchAndUpdateStatus(context: Context, statusViewHolder: POICommonStatusViewHolder<*, *>?, poim: POIManager, dataProvider: POIDataProvider) {
        val status = fetchStatus(dataProvider, statusViewHolder, poim, context)
        updateStatus(context, statusViewHolder, status, dataProvider)
    }

    val statusV: View

    val binding: VB?

    companion object {

        private val LOG_TAG: String = POICommonStatusViewHolder::class.java.simpleName

        @JvmStatic
        fun bindStatusV(view: View): View = view.findViewById(R.id.status)

        @JvmStatic
        fun updateStatusView(context: Context, statusViewHolder: POICommonStatusViewHolder<*, *>?, poiStatus: POIStatus, dataProvider: POIDataProvider) {
            statusViewHolder?.updateStatus(context, statusViewHolder, poiStatus, dataProvider)
        }

        @JvmStatic
        fun fetchAndUpdateStatusView(context: Context, statusViewHolder: POICommonStatusViewHolder<*, *>?, poim: POIManager, dataProvider: POIDataProvider) {
            statusViewHolder?.fetchAndUpdateStatus(context, statusViewHolder, poim, dataProvider)
        }

        @JvmStatic
        fun initPOIStatusViewHolder(@ItemStatusType statusType: Int, view: View): POICommonStatusViewHolder<out ViewBinding?, out POIStatus?>? {
            val statusView = view.findViewById<View?>(R.id.status) ?: return null
            return when (statusType) {
                POI.ITEM_STATUS_TYPE_NONE -> null
                POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT -> POIAvailabilityStatusViewHolder.fromStatusView(statusView)
                POI.ITEM_STATUS_TYPE_SCHEDULE -> POIScheduleViewHolder.fromStatusView(statusView)
                POI.ITEM_STATUS_TYPE_APP -> POIAppStatusViewHolder.fromStatusView(statusView)
                else -> {
                    MTLog.w(LOG_TAG, "Unexpected status '%s' (no view holder)!", statusType)
                    null
                }
            }
        }

    }
}