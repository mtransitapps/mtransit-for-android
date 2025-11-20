package org.mtransit.android.util

import android.os.Build

object UIFeatureFlags {

    const val F_HIDE_ONE_AGENCY_TYPE_TABS = false

    const val F_HOME_SCREEN_BROWSE_COLORS_COUNT = 1

    @JvmField
    // val F_EDGE_TO_EDGE = false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R // WIP
    val F_EDGE_TO_EDGE = true && Build.VERSION.SDK_INT >= 30 // WIP

    // !!! CAN NOT DRAW BEHIND NAVIGATION BAR AS LONG AS ANCHORED BOTTOM BANNER ADS IN ACTIVITY !!!
    const val F_EDGE_TO_EDGE_NAV_BAR_BELOW = false
    // const val F_EDGE_TO_EDGE_NAV_BAR_BELOW = true // WIP

    const val F_APP_BAR_SCROLL_BEHAVIOR = false
    // const val F_APP_BAR_SCROLL_BEHAVIOR = true // WIP

    // see R.boolean.f_predictive_back_gesture
    const val F_PREDICTIVE_BACK_GESTURE = false
    // const val F_PREDICTIVE_BACK_GESTURE = true // WIP (need to move Toolbar to ABFragment)

    // const val F_POI_STATUS_AVAILABILITY_DOTS = false
    const val F_POI_STATUS_AVAILABILITY_DOTS = true // WIP

    const val F_CUSTOM_ADS_IN_NEWS = false
    // const val F_CUSTOM_ADS_IN_NEWS = true // WIP

    const val F_ADS_BANNER_COLLAPSIBLE = false
    // const val F_ADS_BANNER_COLLAPSIBLE = true // WIP

    const val F_NEW_IN_APP_SUBS = false
    // const val F_NEW_IN_APP_SUBS = true // WIP

    const val F_NEWS_THUMBNAIL_PLAY_BUTTON = false
    // const val F_NEWS_THUMBNAIL_PLAY_BUTTON = true // WIP

    const val F_SCHEDULE_HORIZONTAL_CALENDAR = false
    // const val F_SCHEDULE_HORIZONTAL_CALENDAR = true // WIP

}