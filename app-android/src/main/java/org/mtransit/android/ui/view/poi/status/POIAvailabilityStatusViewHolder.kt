package org.mtransit.android.ui.view.poi.status

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import org.mtransit.android.commons.data.AvailabilityPercent
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.LayoutPoiStatusAvailabilityPercentBinding
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.util.UIFeatureFlags
import androidx.core.util.Pair as androidXPair

data class POIAvailabilityStatusViewHolder(
    override var uuid: String,
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
    } else null

    override fun update(
        statusViewHolder: POICommonStatusViewHolder<*, *>?,
        status: POIStatus?,
        dataProvider: POIStatusDataProvider
    ) {
        if (dataProvider.isShowingStatus && statusViewHolder is POIAvailabilityStatusViewHolder) {
            statusViewHolder.bind(status as? AvailabilityPercent, dataProvider)
        } else {
            hideStatus()
        }
    }

    @SuppressLint("KotlinPairNotCreated") // MTPieChartPercentView in Java
    override fun bind(status: AvailabilityPercent?, dataProvider: POIStatusDataProvider) {
        super.bind(status, dataProvider)
        status?.let { availabilityPercent ->
            binding.apply {
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
                            List(availabilityPercent.value1SubValueDefault) { availabilityPercent.value1SubValueDefaultColorBg } +
                                    List(availabilityPercent.value1SubValue1 ?: 0) { availabilityPercent.value1SubValue1ColorBg }
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
                                        availabilityPercent.value1SubValueDefaultColor,
                                        availabilityPercent.value1SubValueDefaultColorBg
                                    ),
                                    androidXPair(
                                        availabilityPercent.value1SubValue1Color,
                                        availabilityPercent.value1SubValue1ColorBg
                                    ),
                                    androidXPair(
                                        availabilityPercent.value2Color,
                                        availabilityPercent.value2ColorBg
                                    )
                                )
                            )
                            pie.setPieces(
                                listOf(
                                    availabilityPercent.value1SubValueDefault,
                                    availabilityPercent.value1SubValue1,
                                    availabilityPercent.value2
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
        fun fromStatusView(view: View, uuid: String) = POIAvailabilityStatusViewHolder(
            uuid = uuid,
            statusV = POICommonStatusViewHolder.bindStatusV(view),
            binding = LayoutPoiStatusAvailabilityPercentBinding.bind(view),
        )
    }
}