package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.mtransit.android.analytics.AnalyticsUserProperties
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.datasource.DataSourcesCache
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.util.NightModeUtils
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
        var appOpenCounts = defaultPrefRepository.getValue(DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS, DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS_DEFAULT)
        appOpenCounts++
        defaultPrefRepository.pref.edit {
            putInt(DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS, appOpenCounts)
        }
        analyticsManager.setUserProperty(AnalyticsUserProperties.OPEN_APP_COUNTS, appOpenCounts)
        viewModelScope.launch {
            demoModeManager.read(savedStateHandle, dataSourcesCache)
            if (demoModeManager.enabled) {
                NightModeUtils.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // light for screenshots (demo mode ON)
            }
        }
    }
}