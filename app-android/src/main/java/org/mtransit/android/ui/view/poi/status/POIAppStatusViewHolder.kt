package org.mtransit.android.ui.view.poi.status

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import org.mtransit.android.commons.data.AppStatus
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.LayoutPoiStatusAppBinding
import org.mtransit.android.ui.view.POIDataProvider
import org.mtransit.android.ui.view.common.context

data class POIAppStatusViewHolder(
    override val statusV: View,
    override val binding: LayoutPoiStatusAppBinding,
) : POICommonStatusViewHolder<LayoutPoiStatusAppBinding, AppStatus> {

    override fun fetchStatus(
        dataProvider: POIDataProvider,
        statusViewHolder: POICommonStatusViewHolder<*, *>?,
        poim: POIManager,
        context: Context
    ) = if (dataProvider.isShowingStatus && statusViewHolder is POIAppStatusViewHolder) {
        poim.setStatusLoaderListener(dataProvider)
        poim.getStatus(context, dataProvider.providesStatusLoader()) as? AppStatus
    } else null

    override fun updateStatus(context: Context, statusViewHolder: POICommonStatusViewHolder<*, *>?, status: POIStatus?, dataProvider: POIDataProvider) {
        if (dataProvider.isShowingStatus && statusViewHolder is POIAppStatusViewHolder) {
            statusViewHolder.bind(status as? AppStatus, dataProvider)
        } else {
            hideStatus()
        }
    }

    override fun bind(status: AppStatus?, dataProvider: POIDataProvider) {
        super.bind(status, dataProvider)
        status?.let { appStatus ->
            binding.apply {
                textTv.setText(appStatus.getStatusMsg(context), TextView.BufferType.SPANNABLE)
                textTv.isVisible = true
            }
        }
    }

    companion object {
        @JvmStatic
        fun fromStatusView(view: View) = POIAppStatusViewHolder(
            statusV = POICommonStatusViewHolder.bindStatusV(view),
            binding = LayoutPoiStatusAppBinding.bind(view),
        )
    }
}