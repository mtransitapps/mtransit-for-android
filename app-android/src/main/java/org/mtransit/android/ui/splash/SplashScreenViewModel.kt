package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.analytics.AnalyticsUserProperties
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_APP_OPEN_COUNTS
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_APP_OPEN_COUNTS_DEFAULT
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_APP_OPEN_LAST
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_APP_OPEN_LAST_DEFAULT
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_DAILY
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.datasource.DataSourcesCache
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.util.NightModeUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val analyticsManager: IAnalyticsManager,
    private val savedStateHandle: SavedStateHandle,
    private val demoModeManager: DemoModeManager,
    private val dataSourcesCache: DataSourcesCache,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG = SplashScreenViewModel::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    fun onAppOpen() {
        viewModelScope.launch {
            demoModeManager.read(savedStateHandle, dataSourcesCache)
            demoModeManager.init()
            if (demoModeManager.isFullDemo()) {
                NightModeUtils.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // light for screenshots (demo mode ON)
            }
            analyticsManager.setUserProperty(AnalyticsUserProperties.OPEN_APP_COUNTS, getAndUpdateAppOpenCounts())
        }
    }

    private suspend fun getAndUpdateAppOpenCounts(): Int = withContext(Dispatchers.IO) {
        var appOpenCounts = defaultPrefRepository.getValue(PREF_USER_APP_OPEN_COUNTS, PREF_USER_APP_OPEN_COUNTS_DEFAULT)
        appOpenCounts++
        var appOpenLast = defaultPrefRepository.getValue(PREF_USER_APP_OPEN_LAST, PREF_USER_APP_OPEN_LAST_DEFAULT)
        val sevenDaysAgo = TimeUtils.currentTimeMillis() - TimeUnit.DAYS.toMillis(7L)
        val dailyUser = sevenDaysAgo < appOpenLast && appOpenCounts > 10 // opened in the last 7 days
        appOpenLast = TimeUtils.currentTimeMillis()
        defaultPrefRepository.pref.edit {
            putInt(PREF_USER_APP_OPEN_COUNTS, appOpenCounts)
            putLong(PREF_USER_APP_OPEN_LAST, appOpenLast)
            putBoolean(PREF_USER_DAILY, dailyUser)
        }
        appOpenCounts
    }
}