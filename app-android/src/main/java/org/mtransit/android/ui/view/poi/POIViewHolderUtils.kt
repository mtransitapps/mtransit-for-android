package org.mtransit.android.ui.view.poi

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import org.mtransit.android.commons.data.Route
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.ui.view.common.textAndVisibility
import org.mtransit.android.util.UIRouteUtils

object POIViewHolderUtils {

    private const val DEBUG_LAYOUT = false

    @JvmStatic
    fun RouteDirectionStopViewHolder.setupRoute(route: Route, getAgency: () -> IAgencyUIProperties?) {
        routeShortNameTv.textAndVisibility = route.shortName.takeIf { it.isNotBlank() }?.let { UIRouteUtils.decorateRouteShortName(context, it) }
        routeTypeImg.apply {
            if (hasPaths() && route.authority == tag) {
                isVisible = true // logo already set for this agency authority
            } else {
                val agency: IAgencyUIProperties? = getAgency()
                agency?.logo?.let { rdsRouteLogo ->
                    setJSON(rdsRouteLogo)
                    tag = route.authority
                    isVisible = true
                } ?: run {
                    isVisible = false
                }
            }
        }
        if (routeShortNameTv.isVisible && routeTypeImg.isVisible) {
            routeFL.updateLayoutParams {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            routeTypeImg.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 2f
                gravity = Gravity.END
            }
            routeShortNameTv.updateLayoutParams<LinearLayout.LayoutParams> {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        } else {
            routeFL.updateLayoutParams {
                width = ViewGroup.LayoutParams.MATCH_PARENT
            }
            routeTypeImg.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 4f
                gravity = Gravity.NO_GRAVITY
            }
            routeShortNameTv.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 4f
                width = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        if (DEBUG_LAYOUT) {
            routeTypeImg.setBackgroundColor(Color.CYAN)
            routeShortNameTv.setBackgroundColor(Color.MAGENTA)
            routeFL.setBackgroundColor(Color.GREEN)
        }
    }
}
