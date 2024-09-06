package org.mtransit.android.ui.view

import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.annotation.ColorInt
import org.mtransit.android.R
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.POI.ItemViewType
import org.mtransit.android.data.POIManager
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.ui.common.UIColorUtils

object POIViewUtils {

    @JvmStatic
    fun setupPOIExtraLayoutBackground(
        view: View,
        poim: POIManager,
        dataSourcesRepository: DataSourcesRepository
    ) = setupPOIExtraLayoutBackground(
        view, poim.poi.type, poim.getColor(dataSourcesRepository)
    )

    @JvmStatic
    fun setupPOIExtraLayoutBackground(
        view: View,
        @ItemViewType poiType: Int,
        @ColorInt originalColorInt: Int
    ) {
        val colorInt: Int = UIColorUtils.adaptBackgroundColorToLightText(view.context, originalColorInt)
        when (poiType) {
            POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP -> {
                view.setBackgroundTintList(null)
                view.background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(colorInt, colorInt)
                ).apply {
                    shape = GradientDrawable.RECTANGLE
                    val topLeftRadius = view.resources.getDimension(R.dimen.extra_route_trip_radius_top_left)
                    val topRightRadius = view.resources.getDimension(R.dimen.extra_route_trip_radius_top_right)
                    val bottomLeftRadius = view.resources.getDimension(R.dimen.extra_route_trip_radius_bottom_left)
                    val bottomRightRadius = view.resources.getDimension(R.dimen.extra_route_trip_radius_bottom_right)
                    cornerRadii = floatArrayOf(
                        topLeftRadius, topLeftRadius,
                        topRightRadius, topRightRadius,
                        bottomRightRadius, bottomRightRadius,
                        bottomLeftRadius, bottomLeftRadius,
                    )
                }
            }

            else -> view.setBackgroundColor(colorInt)
        }
    }
}