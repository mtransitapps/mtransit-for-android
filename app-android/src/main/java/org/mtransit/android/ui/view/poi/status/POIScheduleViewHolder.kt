package org.mtransit.android.ui.view.poi.status

import android.view.View
import android.widget.TextView
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.isVisible
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.ServiceUpdates
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.UISchedule
import org.mtransit.android.data.getStatusK
import org.mtransit.android.databinding.LayoutPoiStatusScheduleBinding
import org.mtransit.android.ui.view.common.context
import java.util.concurrent.TimeUnit
import androidx.core.util.Pair as androidXPair

data class POIScheduleViewHolder(
    override var targetUUID: String,
    override val statusV: View,
    override val binding: LayoutPoiStatusScheduleBinding,
) : POICommonStatusViewHolder<LayoutPoiStatusScheduleBinding, Schedule>, MTLog.Loggable {

    override fun getLogTag() = "$LOG_TAG-$targetUUID"

    override fun fetch(
        statusViewHolder: POICommonStatusViewHolder<*, *>?,
        poim: POIManager,
        dataProvider: POIStatusDataProvider
    ) = if (dataProvider.isShowingStatus && statusViewHolder is POIScheduleViewHolder) {
        poim.setStatusLoaderListener(dataProvider)
        poim.getStatus(dataProvider.providesStatusLoader()) as? Schedule
    } else null

    override fun update(
        statusViewHolder: POICommonStatusViewHolder<*, *>?,
        status: POIStatus?,
        dataProvider: POIStatusDataProvider,
        serviceUpdates: ServiceUpdates?,
    ) {
        if (dataProvider.isShowingStatus && statusViewHolder is POIScheduleViewHolder) {
            statusViewHolder.bind(status as Schedule?, dataProvider, serviceUpdates)
        } else {
            hideStatus()
        }
    }

    override fun bind(status: Schedule?, dataProvider: POIStatusDataProvider, serviceUpdates: ServiceUpdates?) {
        val scheduleWithData = (status as? UISchedule)?.takeIf { !it.isNoData } // 'no data' status is never displayed
        super.bind(scheduleWithData, dataProvider, serviceUpdates)
        scheduleWithData ?: return
        binding.apply {
            //noinspection KotlinPairNotCreated
            val (line1CS, line2CS) = scheduleWithData.getStatusK(
                context = context,
                after = dataProvider.nowToTheMinute,
                minCoverageInMs = TimeUnit.MINUTES.toMillis(30L),
                minCount = 10,
                serviceUpdates = serviceUpdates,
            ) ?: androidXPair<CharSequence?, CharSequence?>(null, null)
            dataNextLine1.setText(line1CS, TextView.BufferType.SPANNABLE)
            dataNextLine2.setText(line2CS, TextView.BufferType.SPANNABLE)
            dataNextLine2.isVisible = !line2CS.isNullOrEmpty()
            super.setStatusVisible(visible = !line1CS.isNullOrEmpty())
        }
    }

    companion object {
        private val LOG_TAG: String = POIScheduleViewHolder::class.java.simpleName

        @JvmStatic
        fun fromStatusView(view: View, targetUUID: String) = POIScheduleViewHolder(
            targetUUID = targetUUID,
            statusV = POICommonStatusViewHolder.bindStatusV(view),
            binding = LayoutPoiStatusScheduleBinding.bind(view),
        )
    }
}