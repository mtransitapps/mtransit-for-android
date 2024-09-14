package org.mtransit.android.ui.view.poi.status

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import org.mtransit.android.commons.data.AvailabilityPercent
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.data.POIManager
import org.mtransit.android.databinding.LayoutPoiStatusAvailabilityPercentBinding
import org.mtransit.android.ui.view.POIDataProvider
import org.mtransit.android.ui.view.common.context
import org.mtransit.android.util.UIFeatureFlags

import androidx.core.util.Pair as androidXPair

data class POIAvailabilityStatusViewHolder(
    override val statusV: View,
    override val binding: LayoutPoiStatusAvailabilityPercentBinding,
) : POICommonStatusViewHolder<LayoutPoiStatusAvailabilityPercentBinding, AvailabilityPercent> {

    override fun fetchStatus(
        dataProvider: POIDataProvider,
        statusViewHolder: POICommonStatusViewHolder<*, *>?,
        poim: POIManager,
        context: Context
    ) = if (dataProvider.isShowingStatus && statusViewHolder is POIAvailabilityStatusViewHolder) {
        poim.setStatusLoaderListener(dataProvider)
        poim.getStatus(context, dataProvider.providesStatusLoader()) as? AvailabilityPercent
    } else null

    override fun updateStatus(
        context: Context,
        statusViewHolder: POICommonStatusViewHolder<*, *>?,
        status: POIStatus?,
        dataProvider: POIDataProvider
    ) {
        if (dataProvider.isShowingStatus && statusViewHolder is POIAvailabilityStatusViewHolder) {
            statusViewHolder.bind(status as? AvailabilityPercent, dataProvider)
        } else {
            hideStatus()
        }
    }

    @SuppressLint("KotlinPairNotCreated") // MTPieChartPercentView in Java
    override fun bind(status: AvailabilityPercent?, dataProvider: POIDataProvider) {
        super.bind(status, dataProvider)
        status?.let { availabilityPercent ->
            binding.apply {
                bindAvailabilityLayout(availabilityPercent)
                if (!availabilityPercent.isStatusOK) {
                    pie.isVisible = false
                    textTv.setText(availabilityPercent.getStatusMsg(context), TextView.BufferType.SPANNABLE)
                    textTv.isVisible = true
                } else {
                    if (!UIFeatureFlags.F_POI_STATUS_AVAILABILITY_STACK_BAR) {
                        if (availabilityPercent.isShowingLowerValue) {
                            pie.isVisible = false
                            textTv.setText(availabilityPercent.getLowerValueText(context), TextView.BufferType.SPANNABLE)
                            textTv.isVisible = true
                        } else {
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
                    } else {
                        textTv.isVisible = false
                    }
                }
            }
        }
    }

    private fun bindAvailabilityLayout(availabilityPercent: AvailabilityPercent) = with(binding.availabilityLayout) {
        if (!UIFeatureFlags.F_POI_STATUS_AVAILABILITY_STACK_BAR) {
            root.isVisible = false
            return
        }
        if (!availabilityPercent.isStatusOK) {
            root.isVisible = false
        } else {
            availabilityBikesLow.setTextColor(availabilityPercent.value1SubValueDefaultColorBg)
            availabilityBikesPrimary.setBackgroundColor(availabilityPercent.value1SubValueDefaultColorBg)
            availabilityPercent.value1SubValue1ColorBg?.let { availabilityBikesSecondary.setBackgroundColor(it) }
            availabilityDocksLow.setTextColor(availabilityPercent.value2Color)
            availabilityDocks.setBackgroundColor(availabilityPercent.value2ColorBg)
            availabilityBikesPrimary.setLayoutParams(
                LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT,
                    availabilityPercent.value1SubValueDefault.toFloat()
                )
            )
            availabilityBikesSecondary.setLayoutParams(
                LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT,
                    (if (availabilityPercent.value1SubValue1 == null) 0 else availabilityPercent.value1SubValue1)!!.toFloat()
                )
            )
            availabilityDocks.setLayoutParams(
                LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT,
                    availabilityPercent.value2.toFloat()
                )
            )
            if (availabilityPercent.isShowingLowerValue) {
                if (availabilityPercent.value2 == availabilityPercent.lowerValue) {
                    availabilityBikesLow.isVisible = false

                    availabilityDocksLow.text = availabilityPercent.getLowerValueText(context)
                    availabilityDocksLow.isVisible = true
                } else {
                    availabilityDocksLow.isVisible = false

                    availabilityBikesLow.text = availabilityPercent.getLowerValueText(context)
                    availabilityBikesLow.isVisible = true
                }
            }
            root.isVisible = true
        }
    }

    companion object {
        @JvmStatic
        fun fromStatusView(view: View) = POIAvailabilityStatusViewHolder(
            statusV = POICommonStatusViewHolder.bindStatusV(view),
            binding = LayoutPoiStatusAvailabilityPercentBinding.bind(view),
        )
    }
}