package org.mtransit.android.ui.view.poi.status

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.data.POIManager
import org.mtransit.android.data.UISchedule
import org.mtransit.android.databinding.LayoutPoiStatusScheduleBinding
import org.mtransit.android.ui.view.common.context
import java.util.concurrent.TimeUnit

data class POIScheduleViewHolder(
    override var uuid: String,
    override val statusV: View,
    override val binding: LayoutPoiStatusScheduleBinding,
) : POICommonStatusViewHolder<LayoutPoiStatusScheduleBinding, Schedule> {

    override fun fetch(
        statusViewHolder: POICommonStatusViewHolder<*, *>?,
        poim: POIManager,
        dataProvider: POIStatusDataProvider
    ) = if (dataProvider.isShowingStatus && statusViewHolder is POIScheduleViewHolder) {
        poim.setStatusLoaderListener(dataProvider)
        poim.getStatus(dataProvider.providesStatusLoader()) as? Schedule
    } else null

    override fun update(statusViewHolder: POICommonStatusViewHolder<*, *>?, status: POIStatus?, dataProvider: POIStatusDataProvider) {
        if (dataProvider.isShowingStatus && statusViewHolder is POIScheduleViewHolder) {
            statusViewHolder.bind(status as Schedule?, dataProvider)
        } else {
            hideStatus()
        }
    }

    override fun bind(status: Schedule?, dataProvider: POIStatusDataProvider) {
        super.bind(status, dataProvider)
        status?.let { schedule ->
            binding.apply {
                var line1CS: CharSequence? = null
                var line2CS: CharSequence? = null
                if (schedule is UISchedule) {
                    val lines = schedule.getStatus(
                        context,
                        dataProvider.nowToTheMinute,
                        TimeUnit.MINUTES.toMillis(30L),
                        null,
                        10,
                        null
                    )
                    if (!lines.isNullOrEmpty()) {
                        line1CS = lines[0].first
                        line2CS = lines[0].second
                    }
                }
                dataNextLine1.setText(line1CS, TextView.BufferType.SPANNABLE)
                dataNextLine2.setText(line2CS, TextView.BufferType.SPANNABLE)
                dataNextLine2.isVisible = !line2CS.isNullOrEmpty()
                super.setStatusVisible(visible = !line1CS.isNullOrEmpty())
            }
        }
    }

    companion object {
        @JvmStatic
        fun fromStatusView(view: View, uuid: String) = POIScheduleViewHolder(
            uuid = uuid,
            statusV = POICommonStatusViewHolder.bindStatusV(view),
            binding = LayoutPoiStatusScheduleBinding.bind(view),
        )
    }
}