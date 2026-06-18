package org.mtransit.android.ui

import android.view.MenuItem
import androidx.core.content.edit
import org.mtransit.android.R
import org.mtransit.android.analytics.AnalyticsScreen
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.MTLog

internal fun NavigationDrawerController.setUserLearnedDrawer() {
    if (this.demoModeManager.isFullDemo()) return
    this.defaultPrefRepository.pref.edit {
        putBoolean(DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER, true)
    }
}

private val NavigationDrawerController.currentAnalyticsScreen: AnalyticsScreen? get() = mainActivityWR.get()?.currentAnalyticsScreen

internal fun NavigationDrawerController.trackNavigationItemSelected(menuItem: MenuItem) {
    when (menuItem.itemId) {
        R.id.root_nav_home -> this.analyticsManager.trackButtonClick("drawer_nav_home", currentAnalyticsScreen)
        R.id.root_nav_favorites -> this.analyticsManager.trackButtonClick("drawer_nav_favorites", currentAnalyticsScreen)
        R.id.root_nav_nearby -> this.analyticsManager.trackButtonClick("drawer_nav_nearby", currentAnalyticsScreen)
        R.id.root_nav_map -> this.analyticsManager.trackButtonClick("drawer_nav_map", currentAnalyticsScreen)
        R.id.nav_trip_planner -> this.analyticsManager.trackButtonClick("drawer_nav_trip_planner", currentAnalyticsScreen)
        R.id.root_nav_news -> this.analyticsManager.trackButtonClick("drawer_nav_news", currentAnalyticsScreen)
        R.id.root_nav_light_rail -> this.analyticsManager.trackButtonClick("drawer_nav_light_rail", currentAnalyticsScreen)
        R.id.root_nav_tram -> this.analyticsManager.trackButtonClick("drawer_nav_tram", currentAnalyticsScreen)
        R.id.root_nav_subway -> this.analyticsManager.trackButtonClick("drawer_nav_subway", currentAnalyticsScreen)
        R.id.root_nav_rail -> this.analyticsManager.trackButtonClick("drawer_nav_rail", currentAnalyticsScreen)
        R.id.root_nav_bus -> this.analyticsManager.trackButtonClick("drawer_nav_bus", currentAnalyticsScreen)
        R.id.root_nav_ferry -> this.analyticsManager.trackButtonClick("drawer_nav_ferry", currentAnalyticsScreen)
        R.id.root_nav_bike -> this.analyticsManager.trackButtonClick("drawer_nav_bike", currentAnalyticsScreen)
        R.id.root_nav_module -> this.analyticsManager.trackButtonClick("drawer_nav_module", currentAnalyticsScreen)
        R.id.nav_settings -> this.analyticsManager.trackButtonClick("drawer_nav_settings", currentAnalyticsScreen)
        R.id.nav_support -> this.analyticsManager.trackButtonClick("drawer_nav_support", currentAnalyticsScreen)
        R.id.nav_privacy_setting -> this.analyticsManager.trackButtonClick("drawer_nav_privacy_setting", currentAnalyticsScreen)
        R.id.nav_rate_review -> this.analyticsManager.trackButtonClick("drawer_nav_rate_review", currentAnalyticsScreen)
        R.id.nav_fares -> this.analyticsManager.trackButtonClick("drawer_nav_fares", currentAnalyticsScreen)
        R.id.nav_send_feedback -> this.analyticsManager.trackButtonClick("drawer_nav_send_feedback", currentAnalyticsScreen)
        else -> {
            MTLog.w(this, "trackNavigationItemSelected() > Unexpected screen nav item ID '${menuItem.itemId}'!")
            this.analyticsManager.trackButtonClick("drawer_nav_unknown", currentAnalyticsScreen)
        }
    }
}
