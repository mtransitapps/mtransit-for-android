package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import org.mtransit.android.datasource.DataSourcesReader
import org.mtransit.android.datasource.DataSourcesStorage
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
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
/**
 * Not suing [org.mtransit.android.datasource.DataSourcesRepository] because memory cache might not be available yet
 */
class SplashScreenViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val analyticsManager: IAnalyticsManager,
    private val savedStateHandle: SavedStateHandle,
    private val demoModeManager: DemoModeManager,
    private val dataSourcesStorage: DataSourcesStorage, // not using [DataSourcesRepository]
    private val dataSourcesReader: DataSourcesReader, // not using [DataSourcesRepository]
    private val dataSourceRequestManager: DataSourceRequestManager,
    private val remoteConfigProvider: RemoteConfigProvider,
    private val adManager: IAdManager,
    private val pm: PackageManager,
) : ViewModel(), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = SplashScreenViewModel::class.java.simpleName

        private const val KEEP_SPLASH_SCREEN_BELOW_AD = true

        private val STATUS_DELAY = 500.milliseconds

        private val DEPLOY_DATA_MAX_DURATION = 13.seconds

        private val DEPLOYING_FOR_REPEAT = 3.seconds
    }

    override fun getLogTag() = LOG_TAG

    private val deploying = AtomicBoolean(false)
    private val adInitComplete = AtomicBoolean(false)
    private val loadAdTriggered = AtomicBoolean(false)

    private val _deployingData = MutableLiveData<Event<Boolean>>()
    val deployingData: LiveData<Event<Boolean>> = _deployingData

    private val _deployingDataFor = MutableLiveData<Event<IAgencyProperties>>()
    val deployingDataFor: LiveData<Event<IAgencyProperties>> = _deployingDataFor

    private val _showAppOpenAd = MutableLiveData<Event<Boolean>>()
    val showAppOpenAd: LiveData<Event<Boolean>> = _showAppOpenAd

    private var _appOpenAdShowComplete = AtomicBoolean(false)

    private val _appOpenAdShowing = MutableLiveData<Boolean>(null)

    private val _readyForNextScreen = MutableLiveData(false)

    val shouldKeepSplashScreenOn: LiveData<Boolean> = MediatorLiveData2(_readyForNextScreen, _appOpenAdShowing)
        .map { (readyForNextScreen, appOpenAdShowing) ->
            if (KEEP_SPLASH_SCREEN_BELOW_AD) {
                readyForNextScreen != true // not ready
                        || appOpenAdShowing == true // currently showing ad
            } else {
                readyForNextScreen != true // not ready
                        && appOpenAdShowing != true // not currently showing ad
            }
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

    @MainThread
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
        val loadTriggered = adManager.loadAppOpenAd()
        if (loadTriggered) {
            MTLog.d(this, "triggerLoadAd() > adManager.loadAppOpenAd()... TRIGGERED")
            loadAdTriggered.set(true)
        }
    }

    fun onAppOpen() {
        viewModelScope.launch {
            demoModeManager.read(savedStateHandle, dataSourcesStorage)
            demoModeManager.init()
            if (demoModeManager.isFullDemo()) {
                NightModeUtils.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // light for screenshots (demo mode ON)
            }
            analyticsManager.setUserProperty(AnalyticsUserProperties.OPEN_APP_COUNTS, getAndUpdateAppOpenCounts())
            val appOpenAdEnabled = remoteConfigProvider.get(
                RemoteConfigProvider.AD_APP_OPEN_ENABLED, RemoteConfigProvider.AD_APP_OPEN_ENABLED_DEFAULT
            )
            if (appOpenAdEnabled) {
                deployIfNecessary()
            }
            _readyForNextScreen.postValue(true)
        }
    }

    private suspend fun deployIfNecessary() = withContext(Dispatchers.IO) {
        dataSourcesReader.refreshSetupRequired(forcePkg = null, skipTimeCheck = false, markUpdated = {})
        val agenciesWithSetupRequired = dataSourcesStorage.getAllAgencies()
            .filter { agency ->
                agency.pkg != appContext.packageName  // not module / place providers
                        && agency.isInstalled
                        && agency.setupRequired
                        && agency.isEnabled(pm)
            }
        if (agenciesWithSetupRequired.isEmpty()) return@withContext // NOT NECESSARY TO DEPLOY
        deploying.set(true)
        if (checkState()) return@withContext // BREAK
        deployAgencyData(agenciesWithSetupRequired)
        // TODO later prefetch free/useful real-time data / news?
        deploying.set(false)
    }

    fun onShowAppOpenAdComplete() {
        _appOpenAdShowComplete.set(true)
        _appOpenAdShowing.postValue(false)
    }

    private suspend fun deployAgencyData(agenciesWithSetupRequired: List<AgencyProperties>) = withContext(Dispatchers.IO) {
        _deployingData.postValue(true.toEvent())
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
                dataSourcesReader.refreshSetupRequired(forcePkg = agency.pkg, skipTimeCheck = true, markUpdated = {})
                dataSourcesStorage.getAgency(agency.authority)?.let { updatedAgency ->
                    setupRequired = updatedAgency.setupRequired
                }
            } while (setupRequired && TimeUtilsK.currentInstant() < start + DEPLOY_DATA_MAX_DURATION)
        }
        _deployingData.postValue(false.toEvent())
    }

    private suspend fun checkState(): Boolean {
        if (deploying.get() && adManager.isAppOpenAdAvailable()) {
            if (_appOpenAdShowing.value != true) {
                _showAppOpenAd.postValue(true.toEvent()) // trigger show app open ad
                _appOpenAdShowing.postValue(true) // set status == [trying to] showing
            }
        } else if (!_appOpenAdShowComplete.get()) {
            withContext(Dispatchers.Main) {
                triggerLoadAd() // trying to load
            }
        }
        if (_appOpenAdShowComplete.get()) return true
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
