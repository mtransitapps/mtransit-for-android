package org.mtransit.android.ui.view.poi

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import org.mtransit.android.R
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.data.IAgencyUIProperties
import org.mtransit.android.data.POIManager
import org.mtransit.android.ui.MainActivity
import org.mtransit.android.ui.rds.route.RDSRouteFragment.Companion.newInstance
import org.mtransit.android.ui.rds.route.RDSRouteFragment.Companion.newInstanceArgs
import org.mtransit.android.ui.view.POIViewUtils
import org.mtransit.android.ui.view.common.MTTransitions.setTransitionName
import org.mtransit.android.ui.view.common.navigateF
import org.mtransit.android.ui.view.common.textAndVisibility
import org.mtransit.android.ui.view.setJSONAndVisibility
import org.mtransit.android.util.UIDirectionUtils
import org.mtransit.android.util.UIRouteUtils
import org.mtransit.commons.FeatureFlags

object POIViewHolderUtils {

    private const val DEBUG_LAYOUT = false
    // private const val DEBUG_LAYOUT = true // DEBUG

    @JvmOverloads
    @JvmStatic
    fun RouteDirectionStopViewHolder.updateExtra(
        poim: POIManager,
        showingExtra: Boolean,
        getAgency: () -> IAgencyUIProperties?,
        @ColorInt poimColorInt: Int,
        getMainActivity: () -> MainActivity?,
        directionSingleLine: Boolean = false,
        directionSelected: Boolean = false,
        onClick: View.OnClickListener = {},
    ) {
        if (poim.poi !is RouteDirectionStop) return
        val rds: RouteDirectionStop = poim.poi
        if (!showingExtra) {
            rdsExtraV.isVisible = false
            rsnImg.isVisible = false
            directionHeadingBg.isVisible = false
            noExtra.isVisible = true
            return
        }
        setupRoute(rds.route, getAgency)
        noExtra.isVisible = false
        rsnImg.isVisible = true
        rdsExtraV.isVisible = true
        directionHeadingTv.apply {
            text = UIDirectionUtils.decorateDirection(context, rds.direction.getUIHeading(context, true), true)
            if (directionSingleLine) {
                setSingleLine(true) // marquee forever
            }
            if (directionSelected) {
                setSelected(true) // marquee forever
            }
        }
        directionHeadingBg.isVisible = true
        POIViewUtils.setupPOIExtraLayoutBackground(rdsExtraV, poim.poi.type, poimColorInt)
        rdsExtraV.setOnClickListener { view ->
            onClick.onClick(view)
            setTransitionName(view, "r_" + rds.authority + "_" + rds.route.id)
            if (FeatureFlags.F_NAVIGATION) {
                val navController = findNavController(view)
                var extras: FragmentNavigator.Extras? = null
                if (FeatureFlags.F_TRANSITION) {
                    extras = FragmentNavigator.Extras.Builder()
                        .addSharedElement(view, view.transitionName)
                        .build()
                }
                navController.navigateF(
                    R.id.nav_to_rds_route_screen,
                    newInstanceArgs(rds),
                    null,
                    extras
                )
            } else {
                getMainActivity()?.addFragmentToStack(newInstance(rds), view)
            }
        }
    }

    @JvmStatic
    fun RouteDirectionStopViewHolder.setupRoute(route: Route, getAgency: () -> IAgencyUIProperties?) {
        routeShortName.textAndVisibility = route.shortName.takeIf { it.isNotBlank() }?.let { UIRouteUtils.decorateRouteShortName(context, it) }
        routeTypeImg.setJSONAndVisibility(getAgency())
        if (routeShortName.isVisible && routeTypeImg.isVisible) {
            routeSpaceStart.isVisible = true
            routeTypeImg.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 2f
            }
            routeShortName.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 2f
            }
            routeShortName.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        } else {
            routeSpaceStart.isVisible = false
            routeTypeImg.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 4f
            }
            routeShortName.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 4f
            }
            routeShortName.gravity = Gravity.CENTER
        }
        if (DEBUG_LAYOUT) {
            rdsExtraV.setBackgroundColor(Color.YELLOW)
            view.findViewById<View>(R.id.route_direction).setBackgroundColor(Color.BLUE)
            routeTypeImg.setBackgroundColor(Color.CYAN)
            routeShortName.setBackgroundColor(Color.MAGENTA)
            rsnImg.setBackgroundColor(Color.GREEN)
        }
    }
}
