package org.mtransit.android.ui

import android.view.MenuItem
import androidx.core.content.edit
import org.mtransit.android.R
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.fragment.ABFragment

internal fun NavigationDrawerController.setUserLearnedDrawer() {
    if (this.demoModeManager.isFullDemo()) return
    this.defaultPrefRepository.pref.edit {
        putBoolean(DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER, true)
    }
}

private val NavigationDrawerController.currentFragment: ABFragment? get() = mainActivityWR.get()?.currentABFragment

internal fun NavigationDrawerController.trackNavigationItemSelected(menuItem: MenuItem) {
    when (menuItem.itemId) {
        R.id.root_nav_home -> this.analyticsManager.trackButtonClick("drawer_nav_home", currentFragment)
        R.id.root_nav_favorites -> this.analyticsManager.trackButtonClick("drawer_nav_favorites", currentFragment)
        R.id.root_nav_nearby -> this.analyticsManager.trackButtonClick("drawer_nav_nearby", currentFragment)
        R.id.root_nav_map -> this.analyticsManager.trackButtonClick("drawer_nav_map", currentFragment)
        R.id.nav_trip_planner -> this.analyticsManager.trackButtonClick("drawer_nav_trip_planner", currentFragment)
        R.id.root_nav_news -> this.analyticsManager.trackButtonClick("drawer_nav_news", currentFragment)
        R.id.root_nav_light_rail -> this.analyticsManager.trackButtonClick("drawer_nav_light_rail", currentFragment)
        R.id.root_nav_tram -> this.analyticsManager.trackButtonClick("drawer_nav_tram", currentFragment)
        R.id.root_nav_subway -> this.analyticsManager.trackButtonClick("drawer_nav_subway", currentFragment)
        R.id.root_nav_rail -> this.analyticsManager.trackButtonClick("drawer_nav_rail", currentFragment)
        R.id.root_nav_bus -> this.analyticsManager.trackButtonClick("drawer_nav_bus", currentFragment)
        R.id.root_nav_ferry -> this.analyticsManager.trackButtonClick("drawer_nav_ferry", currentFragment)
        R.id.root_nav_bike -> this.analyticsManager.trackButtonClick("drawer_nav_bike", currentFragment)
        R.id.root_nav_module -> this.analyticsManager.trackButtonClick("drawer_nav_module", currentFragment)
        R.id.nav_settings -> this.analyticsManager.trackButtonClick("drawer_nav_settings", currentFragment)
        R.id.nav_support -> this.analyticsManager.trackButtonClick("drawer_nav_support", currentFragment)
        R.id.nav_privacy_setting -> this.analyticsManager.trackButtonClick("drawer_nav_privacy_setting", currentFragment)
        R.id.nav_rate_review -> this.analyticsManager.trackButtonClick("drawer_nav_rate_review", currentFragment)
        R.id.nav_fares -> this.analyticsManager.trackButtonClick("drawer_nav_fares", currentFragment)
        R.id.nav_send_feedback -> this.analyticsManager.trackButtonClick("drawer_nav_send_feedback", currentFragment)
        else -> {
            MTLog.w(this, "trackNavigationItemSelected() > Unexpected screen nav item ID '%s'!", menuItem.itemId)
            this.analyticsManager.trackButtonClick("drawer_nav_unknown", currentFragment)
        }
    }
}
