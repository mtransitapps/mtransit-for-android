package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.analytics.AnalyticsUserProperties
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_APP_OPEN_COUNTS
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_APP_OPEN_COUNTS_DEFAULT
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_APP_OPEN_LAST
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_APP_OPEN_LAST_DEFAULT
import org.mtransit.android.common.repository.DefaultPreferenceRepository.Companion.PREF_USER_DAILY
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.toMillis
import org.mtransit.android.data.AgencyProperties
import org.mtransit.android.data.IAgencyProperties
import org.mtransit.android.datasource.DataSourceRequestManager
import org.mtransit.android.datasource.DataSourcesCache
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.toDateTimeLog
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.toEvent
import org.mtransit.android.util.NightModeUtils
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SuppressLint("CustomSplashScreen")
@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val analyticsManager: IAnalyticsManager,
    private val savedStateHandle: SavedStateHandle,
    private val demoModeManager: DemoModeManager,
    private val dataSourcesCache: DataSourcesCache,
    private val dataSourcesRepository: DataSourcesRepository,
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val adManager: IAdManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = SplashScreenViewModel::class.java.simpleName

        private val STATUS_DELAY = 500.milliseconds

        private val DEPLOY_DATA_MAX_DURATION = 13.seconds

        private val DEPLOYING_FOR_REPEAT = 2.seconds
    }

    override fun getLogTag() = LOG_TAG

    private val deploying = AtomicBoolean(false)
    private val adInitComplete = AtomicBoolean(false)
    private val loadAdTriggered = AtomicBoolean(false)

    private val _deployingData = MutableLiveData(Event<Boolean?>(null))
    val deployingData: LiveData<Event<Boolean?>> = _deployingData

    private val _deployingDataFor = MutableLiveData(Event<IAgencyProperties?>(null))
    val deployingDataFor: LiveData<Event<IAgencyProperties?>> = _deployingDataFor

    private val _showAppOpenAd = MutableLiveData(Event(false))
    val showAppOpenAd: LiveData<Event<Boolean>> = _showAppOpenAd

    private var _appOpenAdShown = AtomicBoolean(false)

    private val _appOpenAdShowing = MutableLiveData<Boolean>(null)

    private val _readyForNextScreen = MutableLiveData(false)

    val shouldKeepSplashScreenOn: LiveData<Boolean> = MediatorLiveData2(_readyForNextScreen, _appOpenAdShowing)
        .map { (readyForNextScreen, appOpenAdShowing) ->
            readyForNextScreen != true // not ready
                    && appOpenAdShowing != true // not currently showing ad
        }.distinctUntilChanged()

    val showNextScreen: LiveData<Boolean> = MediatorLiveData2(_readyForNextScreen, _appOpenAdShowing)
        .map { (readyForNextScreen, appOpenAdShowing) ->
            appOpenAdShowing == false // ad was dismissed by user -> show next screen
                    || readyForNextScreen == true && appOpenAdShowing != true // ready and not currently showing ad
        }.distinctUntilChanged()

    fun initShowingAdsFromCache() {
        viewModelScope.launch(Dispatchers.IO) {
            adManager.initShowingAdsFromCache()
        }
    }

    @MainThread
    fun onAdInitCompleted() {
        adInitComplete.set(true)
        triggerLoadAd()
    }

    private fun triggerLoadAd() {
        if (!deploying.get()) {
            MTLog.d(this, "triggerLoadAd() > SKIP (not deploying)")
            return // not deploying
        }
        if (loadAdTriggered.get()) {
            MTLog.d(this, "triggerLoadAd() > SKIP (already triggered)")
            return // already triggered
        }
        if (adManager.isAppOpenAdAvailable()) {
            MTLog.d(this, "triggerLoadAd() > SKIP (app open already available)")
            return // already available // 1st
        }
        if (adManager.loadAppOpenAd()) {
            MTLog.d(this, "triggerLoadAd() > adManager.loadAppOpenAd()... TRIGGERED")
            loadAdTriggered.set(true)
        }
    }

    fun onAppOpen() {
        viewModelScope.launch {
            demoModeManager.read(savedStateHandle, dataSourcesCache)
            demoModeManager.init()
            if (demoModeManager.isFullDemo()) {
                NightModeUtils.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // light for screenshots (demo mode ON)
            }
            analyticsManager.setUserProperty(AnalyticsUserProperties.OPEN_APP_COUNTS, getAndUpdateAppOpenCounts())
            deployIfNecessary()
            _readyForNextScreen.postValue(true)
        }
    }

    private suspend fun deployIfNecessary() {
        dataSourcesRepository.refreshSetupRequired(forcePkg = null, skipTimeCheck = false) // refresh status
        val agenciesWithSetupRequired = dataSourcesRepository.getAllAgenciesEnabled()
            .filter { agency ->
                agency.pkg != appContext.packageName  // not module / place providers
                        && agency.isInstalled
                        && agency.setupRequired
            }
        if (agenciesWithSetupRequired.isEmpty()) return
        deploying.set(true)
        if (checkState()) return // BREAK
        _deployingData.postValue(true.toEvent())
        deployAgencyData(agenciesWithSetupRequired)
        // TODO later prefetch free/useful real-time data / news?
        _deployingData.postValue(false.toEvent())
        deploying.set(false)
    }

    fun onAppOpenAdShown() {
        _appOpenAdShown.set(true)
        _appOpenAdShowing.postValue(false)
    }

    private suspend fun deployAgencyData(agenciesWithSetupRequired: List<AgencyProperties>) = withContext(Dispatchers.IO) {
        agenciesWithSetupRequired.forEach { agency ->
            if (checkState()) return@withContext // BREAK
            var deployingForTime = TimeUtilsK.currentInstant()
            _deployingDataFor.postValue(agency.toEvent())
            val start = TimeUtilsK.currentInstant()
            dataSourceRequestManager.ping(agency.authority) // ASYNC (uses WorkManager)
            if (checkState()) return@withContext // BREAK
            var setupRequired = true
            do {
                delay(STATUS_DELAY)
                if (checkState()) return@withContext // BREAK
                if (deployingForTime + DEPLOYING_FOR_REPEAT < TimeUtilsK.currentInstant()) {
                    deployingForTime = TimeUtilsK.currentInstant()
                    _deployingDataFor.postValue(agency.toEvent())
                }
                val updated = dataSourcesRepository.refreshSetupRequired(forcePkg = agency.pkg, skipTimeCheck = true)
                dataSourcesCache.getAgency(agency.authority)?.let { updatedAgency ->
                    setupRequired = updatedAgency.setupRequired
                }
            } while (setupRequired && TimeUtilsK.currentInstant() < start + DEPLOY_DATA_MAX_DURATION)
        }
    }

    private fun checkState(): Boolean {
        if (deploying.get() && adManager.isAppOpenAdAvailable()) {
            MTLog.d(this, "checkState() > app open available")
            _showAppOpenAd.postValue(true.toEvent())
            _appOpenAdShowing.postValue(true) // [trying to] show
        } else if (!_appOpenAdShown.get()) {
            triggerLoadAd() // trying to load
        }
        if (_appOpenAdShown.get()) return true
        return false
    }

    private suspend fun getAndUpdateAppOpenCounts(): Int = withContext(Dispatchers.IO) {
        var appOpenCounts = defaultPrefRepository.pref.getInt(PREF_USER_APP_OPEN_COUNTS, PREF_USER_APP_OPEN_COUNTS_DEFAULT)
        appOpenCounts++
        var appOpenLast = defaultPrefRepository.pref.getLong(PREF_USER_APP_OPEN_LAST, PREF_USER_APP_OPEN_LAST_DEFAULT).millisToInstant()
        val sevenDaysAgo = TimeUtilsK.currentInstant() - 7.days
        val dailyUser = sevenDaysAgo < appOpenLast && appOpenCounts > 10 // opened in the last 7 days
        appOpenLast = TimeUtilsK.currentInstant()
        defaultPrefRepository.pref.edit {
            putInt(PREF_USER_APP_OPEN_COUNTS, appOpenCounts)
            putLong(PREF_USER_APP_OPEN_LAST, appOpenLast.toMillis())
            putBoolean(PREF_USER_DAILY, dailyUser)
        }
        appOpenCounts
    }
}
