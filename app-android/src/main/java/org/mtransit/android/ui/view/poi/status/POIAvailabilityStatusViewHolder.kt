package org.mtransit.android.ui.view.poi.status

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import org.mtransit.android.commons.data.AvailabilityPercent
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.ServiceUpdates
import org.mtransit.android.commons.data.value1Default
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.LayoutPoiStatusAvailabilityPercentBinding
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.util.UIFeatureFlags
import androidx.core.util.Pair as androidXPair

data class POIAvailabilityStatusViewHolder(
    override var targetUUID: String,
    override val statusV: View,
    override val binding: LayoutPoiStatusAvailabilityPercentBinding,
) : POICommonStatusViewHolder<LayoutPoiStatusAvailabilityPercentBinding, AvailabilityPercent> {

    override fun fetch(
        statusViewHolder: POICommonStatusViewHolder<*, *>?,
        poim: POIManager,
        dataProvider: POIStatusDataProvider
    ) = if (dataProvider.isShowingStatus && statusViewHolder is POIAvailabilityStatusViewHolder) {
        poim.setStatusLoaderListener(dataProvider)
        poim.getStatus(dataProvider.providesStatusLoader()) as? AvailabilityPercent
    } else {
        null
    }

    override fun update(
        statusViewHolder: POICommonStatusViewHolder<*, *>?,
        status: POIStatus?,
        dataProvider: POIStatusDataProvider,
        serviceUpdates: ServiceUpdates?,
    ) {
        if (dataProvider.isShowingStatus && statusViewHolder is POIAvailabilityStatusViewHolder) {
            statusViewHolder.bind(status as? AvailabilityPercent, dataProvider, serviceUpdates)
        } else {
            hideStatus()
        }
    }

    @SuppressLint("KotlinPairNotCreated") // MTPieChartPercentView in Java
    override fun bind(status: AvailabilityPercent?, dataProvider: POIStatusDataProvider, serviceUpdates: ServiceUpdates?) {
        super.bind(status, dataProvider, serviceUpdates)
        status?.let { availabilityPercent ->
            binding.apply {
                val value1SubValueDefault = availabilityPercent.value1Default
                if (!availabilityPercent.isStatusOK) {
                    if (UIFeatureFlags.F_POI_STATUS_AVAILABILITY_DOTS) {
                        dots.isVisible = false
                    } else {
                        pie.isVisible = false
                    }
                    textTv.setText(availabilityPercent.getStatusMsg(context), TextView.BufferType.SPANNABLE)
                    textTv.isVisible = true
                } else {
                    if (UIFeatureFlags.F_POI_STATUS_AVAILABILITY_DOTS) {
                        textTv.isVisible = false
                        dots.setColorDots(
                            availabilityPercent.totalValue,
                            List(value1SubValueDefault.value) { value1SubValueDefault.colorBg } +
                                availabilityPercent.value1SubValue1?.let { sv1 -> List(sv1.value) { sv1.colorBg } }.orEmpty()
                        )
                        dots.isVisible = true
                    } else {
                        if (availabilityPercent.isShowingLowerValue) {
                            pie.isVisible = false
                            textTv.setText(availabilityPercent.getLowerValueText(context), TextView.BufferType.SPANNABLE)
                            textTv.isVisible = true
                        } else {
                            textTv.isVisible = false
                            pie.setPiecesColors(
                                listOf(
                                    androidXPair(
                                        value1SubValueDefault.color,
                                        value1SubValueDefault.colorBg
                                    ),
                                    androidXPair(
                                        availabilityPercent.value1SubValue1?.color,
                                        availabilityPercent.value1SubValue1?.colorBg
                                    ),
                                    androidXPair(
                                        availabilityPercent.value2.color,
                                        availabilityPercent.value2.colorBg
                                    )
                                )
                            )
                            pie.setPieces(
                                listOf(
                                    value1SubValueDefault.value,
                                    availabilityPercent.value1SubValue1?.value,
                                    availabilityPercent.value2.value
                                )
                            )
                            pie.isVisible = true
                        }
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun fromStatusView(view: View, targetUUID: String) = POIAvailabilityStatusViewHolder(
            targetUUID = targetUUID,
            statusV = POICommonStatusViewHolder.bindStatusV(view),
            binding = LayoutPoiStatusAvailabilityPercentBinding.bind(view),
        )
    }
}
