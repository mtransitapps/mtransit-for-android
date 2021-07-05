package org.mtransit.android.ui.splash

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.mtransit.android.analytics.AnalyticsUserProperties
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PreferenceUtils
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val analyticsManager: IAnalyticsManager
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = SplashScreenViewModel::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    fun onAppOpen() {
        var appOpenCounts = defaultPrefRepository.getValue(PreferenceUtils.PREF_USER_APP_OPEN_COUNTS, PreferenceUtils.PREF_USER_APP_OPEN_COUNTS_DEFAULT)
        appOpenCounts++
        defaultPrefRepository.pref.edit {
            putInt(PreferenceUtils.PREF_USER_APP_OPEN_COUNTS, appOpenCounts)
        }
        analyticsManager.setUserProperty(AnalyticsUserProperties.OPEN_APP_COUNTS, appOpenCounts)
    }
}