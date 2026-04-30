package org.mtransit.android.ui

import androidx.core.content.edit
import org.mtransit.android.common.repository.DefaultPreferenceRepository

internal fun NavigationDrawerController.setUserLearnedDrawer() {
    if (this.demoModeManager.isFullDemo()) return
    this.userLearnedDrawer = true
    this.defaultPrefRepository.pref.edit {
        putBoolean(DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER, true)
    }
}
