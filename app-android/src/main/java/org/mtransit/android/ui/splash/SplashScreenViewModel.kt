package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.mtransit.android.ad.IAdManager
import org.mtransit.android.analytics.AnalyticsUserProperties
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
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
import org.mtransit.android.ui.view.common.Event
import org.mtransit.android.ui.view.common.MediatorLiveData2
import org.mtransit.android.ui.view.common.toEvent
import org.mtransit.android.user.UserManager
import org.mtransit.android.util.NightModeUtils
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@SuppressLint("CustomSplashScreen")
@HiltViewModel
/**
 * Not using [org.mtransit.android.datasource.DataSourcesRepository] because memory cache might not be available yet
 */
class SplashScreenViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val userManager: UserManager,
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

        private val DEPLOY_DATA_STATUS_DELAY = 500.milliseconds

        private val DEPLOY_DATA_UI_REPEATED = 3.seconds

        private val DEPLOY_DATA_MAX_DURATION = 7.seconds

        private val DEPLOY_DATA_OVERALL_MAX_DURATION = 13.seconds

        private val FIRST_REFRESH_SETUP_REQUIRED_TIMEOUT = 3.seconds
    }

    override fun getLogTag() = LOG_TAG

    private val deploying = AtomicBoolean(false)
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

    fun initHasSubscriptionFromCache() {
        viewModelScope.launch(Dispatchers.IO) {
            adManager.initHasSubscriptionFromCache()
        }
    }

    @MainThread
    fun onAdInitCompleted() {
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
            val canShowAds = adManager.canShowAds() != false
            val appOpenAdEnabled = remoteConfigProvider.get(
                RemoteConfigProvider.AD_APP_OPEN_ENABLED, RemoteConfigProvider.AD_APP_OPEN_ENABLED_DEFAULT
            )
            if (appOpenAdEnabled && canShowAds) {
                deployIfNecessary()
            }
            _readyForNextScreen.postValue(true)
        }
    }

    private suspend fun deployIfNecessary() = withContext(Dispatchers.IO) {
        val setupRefreshResult = withTimeoutOrNull(FIRST_REFRESH_SETUP_REQUIRED_TIMEOUT) {
            dataSourcesReader.refreshSetupRequired(forcePkg = null, skipTimeCheck = false, markUpdated = {})
        }
        if (setupRefreshResult == null) {
            MTLog.d(this@SplashScreenViewModel, "deployIfNecessary() > refreshSetupRequired() timed out after $FIRST_REFRESH_SETUP_REQUIRED_TIMEOUT.")
        }
        val agenciesWithSetupRequired = dataSourcesStorage.getAllAgencies()
            .filter { agency ->
                agency.pkg != appContext.packageName  // not module / place providers
                        && agency.isInstalled
                        && agency.setupRequired
                        && agency.isEnabled(pm)
            }
        if (agenciesWithSetupRequired.isEmpty()) return@withContext // NOT NECESSARY TO DEPLOY
        deploying.set(true)
        deployAgencyData(agenciesWithSetupRequired)
        // TODO later prefetch free/useful real-time data / news?
        deploying.set(false)
    }

    fun onShowAppOpenAdComplete() {
        _appOpenAdShowComplete.set(true)
        _appOpenAdShowing.postValue(false)
    }

    private suspend fun deployAgencyData(agenciesWithSetupRequired: List<AgencyProperties>) = withContext(Dispatchers.IO) {
        withTimeoutOrNull(DEPLOY_DATA_OVERALL_MAX_DURATION) {
            agenciesWithSetupRequired.forEach { agency ->
                if (checkState()) return@withTimeoutOrNull // BREAK
                ensureActive()
                var deployingForTime = TimeUtilsK.currentInstant()
                _deployingDataFor.postValue(agency.toEvent())
                val start = TimeUtilsK.currentInstant()
                dataSourceRequestManager.ping(agency) // ASYNC (uses WorkManager)
                if (checkState()) return@withTimeoutOrNull // BREAK
                ensureActive()
                var setupRequired = true
                do {
                    delay(DEPLOY_DATA_STATUS_DELAY)
                    if (checkState()) return@withTimeoutOrNull // BREAK
                    ensureActive()
                    if (deployingForTime + DEPLOY_DATA_UI_REPEATED < TimeUtilsK.currentInstant()) {
                        deployingForTime = TimeUtilsK.currentInstant()
                        _deployingDataFor.postValue(agency.toEvent())
                    }
                    dataSourcesReader.refreshSetupRequired(forcePkg = agency.pkg, skipTimeCheck = true, markUpdated = {})
                    dataSourcesStorage.getAgency(agency.authority)?.let { updatedAgency ->
                        setupRequired = updatedAgency.setupRequired
                    }
                } while (setupRequired && TimeUtilsK.currentInstant() < start + DEPLOY_DATA_MAX_DURATION)
            }
        }
        _deployingData.postValue(false.toEvent()) // maybe not fully done (if longer than max duration) but good enough
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
        if (_appOpenAdShowComplete.get()) return true // app open ad has been dismissed by user (or failed to display)
        return false
    }

    @VisibleForTesting
    internal suspend fun getAndUpdateAppOpenCounts(): Int = withContext(Dispatchers.IO) {
        val appOpenCounts = userManager.getAppOpenCount()
        val appOpenFirst = userManager.getAppOpenFirstOrNull()?.millisToInstant()
        val appOpenLast = userManager.getAppOpenLastOrNull()?.millisToInstant()
        val dailyUser = appOpenLast?.let {
            val sevenDaysAgo = TimeUtilsK.currentInstant() - 7.days
            sevenDaysAgo < it && appOpenCounts > 10 // opened in the last 7 days
        } ?: false
        var newAppOpenFirst: Instant? = null
        if (appOpenFirst == null) {
            if (appOpenCounts <= 0) {
                newAppOpenFirst = TimeUtilsK.currentInstant()
            } else if (appOpenLast == null) { // added a while ago, very likely first time
                newAppOpenFirst = TimeUtilsK.currentInstant()
            }
            if (appOpenCounts > 33) {
                newAppOpenFirst = null // never reset app open first with so many app opens
            }
        }
        val newUser = appOpenFirst?.let { firstAppOpen ->
            if (appOpenCounts >= 10) { // frequent user
                if (TimeUtilsK.currentInstant() <= firstAppOpen + 5.days) {
                    return@let true // frequent user but less than 5 days
                }
            } else { // casual user
                if (TimeUtilsK.currentInstant() <= firstAppOpen + 10.days) {
                    return@let true // casual user but less than 10 days
                }
            }
            null
        } ?: appOpenLast?.let {
            if (it + 99.days < TimeUtilsK.currentInstant()  // a long time ago
                && appOpenCounts < 33 // "few" app opens
            ) {
                return@let true // old user coming back with "few" app opens
            }
            null
        } ?: (appOpenCounts <= 7) // probably new-ish user
        userManager.set(
            appOpenCounts = appOpenCounts + 1,
            appOpenFirst = newAppOpenFirst?.toMillis(),
            appOpenLast = TimeUtils.currentTimeMillis(),
            dailyUser = dailyUser,
            newUser = newUser,
        )
        appOpenCounts + 1
    }
}
