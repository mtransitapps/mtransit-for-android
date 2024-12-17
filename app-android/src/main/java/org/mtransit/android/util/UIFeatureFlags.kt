package org.mtransit.android.util

object UIFeatureFlags {

    const val F_HIDE_ONE_AGENCY_TYPE_TABS = false

    const val F_HOME_SCREEN_BROWSE_COLORS_COUNT = 1

    // const val F_EDGE_TO_EDGE = false
    const val F_EDGE_TO_EDGE = true // WIP

    // !!! CAN NOT DRAW BEHIND NAVIGATION BAR AS LONG AS ANCHORED BOTTOM BANNER ADS IN ACTIVITY !!!
    const val F_EDGE_TO_EDGE_NAV_BAR_BELOW = false
    // const val F_EDGE_TO_EDGE_NAV_BAR_BELOW = true // WIP

    // const val F_POI_STATUS_AVAILABILITY_DOTS = false
    const val F_POI_STATUS_AVAILABILITY_DOTS = true // WIP

    const val F_CUSTOM_ADS_IN_NEWS = false
    // const val F_CUSTOM_ADS_IN_NEWS = true // WIP

    const val F_ADS_BANNER_COLLAPSIBLE = false
    // const val F_ADS_BANNER_COLLAPSIBLE = true // WIP

}