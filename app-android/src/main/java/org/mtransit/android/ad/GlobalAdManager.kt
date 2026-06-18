package org.mtransit.android.ad

// import com.google.android.libraries.ads.mobile.sdk.MobileAds #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig #gmaNextGen
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.rewarded.RewardedUserManager
import org.mtransit.android.billing.IBillingManager
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.toDateTimeLog
import org.mtransit.android.ui.view.common.IActivity
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Instant
import com.google.android.ump.FormError as UMPFormError

@Singleton
class GlobalAdManager(
    private val dataSourcesRepository: DataSourcesRepository,
    private val crashReporter: CrashReporter,
    private val demoModeManager: DemoModeManager,
    private val consentManager: AdsConsentManager,
    private val rewardedUserManager: RewardedUserManager,
    private val billingManager: IBillingManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MTLog.Loggable {

    @Inject
    constructor(
        dataSourcesRepository: DataSourcesRepository,
        crashReporter: CrashReporter,
        demoModeManager: DemoModeManager,
        consentManager: AdsConsentManager,
        rewardedUserManager: RewardedUserManager,
        billingManager: IBillingManager,
    ) : this(
        dataSourcesRepository = dataSourcesRepository,
        crashReporter = crashReporter,
        demoModeManager = demoModeManager,
        consentManager = consentManager,
        rewardedUserManager = rewardedUserManager,
        billingManager = billingManager,
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${GlobalAdManager::class.java.simpleName}"

        private const val GOOGLE_ADS_TEST_IDS_START_WITH = "ca-app-pub-3940256099942544"
    }

    override fun getLogTag() = LOG_TAG

    private val initialized = AtomicBoolean(false)
    private val initializing = AtomicBoolean(false)

    private var hasSubscription: Boolean? = null
    private var hasAgenciesEnabled: Boolean? = null

    @Volatile
    private var _rewardedUntil: Instant? = null

    @Volatile
    private var _rewardedNow: Boolean? = null

    init {
        this.dataSourcesRepository.readingHasAgenciesEnabled().observeForever { hasAgenciesEnabled ->
            this.hasAgenciesEnabled = hasAgenciesEnabled
        }
        this.rewardedUserManager.rewardedUntilLive.observeForever { rewardedUntilInMs ->
            this._rewardedUntil = rewardedUntilInMs
        }
        this.rewardedUserManager.rewardedNowLive.observeForever { rewardedNow ->
            this._rewardedNow = rewardedNow
        }
    }

    val rewardedUntil: LiveData<Instant> get() = this.rewardedUserManager.rewardedUntilLive
    val rewardedNow: LiveData<Boolean> get() = this.rewardedUserManager.rewardedNowLive

    fun init(activity: IAdScreenActivity, onInitCompleteListener: () -> Unit, withConsentOnly: Boolean = false) {
        if (!AdConstants.AD_ENABLED) return
        val theActivity = activity.activity
        if (theActivity == null) {
            MTLog.w(this, "Trying to initialized w/o activity!")
            return // SKIP
        }
        consentManager.gatherConsent(
            theActivity,
            noUI = withConsentOnly,
            onConsentGatheringComplete = { error: UMPFormError? ->
                error?.let { logAdsD(this@GlobalAdManager, "Consent not obtained [${it.errorCode}]: ${it.message}.") }
                if (consentManager.canRequestAds) {
                    initWithConsent(activity, onInitCompleteListener)
                }
                if (consentManager.isPrivacyOptionsRequired) {
                    activity.onPrivacyOptionsRequiredChanged()
                }
            }
        )
        if (consentManager.canRequestAds) { // IF consent already given in previous session DO
            initWithConsent(activity, onInitCompleteListener)
        }
    }

    private fun initWithConsent(activity: IAdScreenActivity, onInitCompleteListener: () -> Unit) {
        if (initialized.get()) {
            logAdsD(this, "initWithConsent() > SKIP (initialized: ${this.initialized.get()})")
            onInitCompleteListener()
            return // SKIP
        }
        if (initializing.getAndSet(true)) {
            logAdsD(this, "initWithConsent() > SKIP (initializing: ${this.initializing.get()})")
            return // SKIP
        }
        try {
            CoroutineScope(ioDispatcher).launch {
                initOnBackgroundThread(activity, onInitCompleteListener)
            }
        } catch (e: Exception) {
            this.crashReporter.w(this, e, "Error while initializing Ads!")
        }
    }

    private fun makeAdsRequestConfig(context: Context) =
        RequestConfiguration.Builder()
            .setTestDeviceIds(
                listOf(*context.resources.getStringArray(R.array.google_ads_test_devices_ids))
                        + listOf(AdRequest.DEVICE_ID_EMULATOR)
                // Android emulators are automatically configured as test devices. #gmaNextGen
            )
            .build()

    // private fun InitializationConfig.Builder.disableMediationAdapterInit(@Suppress("unused") context: Context, appId: String) { #gmaNextGen
    private fun disableMediationAdapterInit(context: Context, appId: String) {
        if (appId.startsWith(GOOGLE_ADS_TEST_IDS_START_WITH)) {
            // disableMediationAdapterInitialization() // all will fail/timeout #gmaNextGen
            MobileAds.disableMediationAdapterInitialization(context) // all will fail/timeout
        }
    }

    @WorkerThread
    private fun initOnBackgroundThread(activity: IAdScreenActivity, onInitCompleteListener: () -> Unit) {
        // https://developers.google.com/admob/android/next-gen/quick-start
        val context = activity.requireContext()
        val appId = context.getString(R.string.google_ads_app_id)
        // val initConfig = InitializationConfig.Builder(applicationId = appId) #gmaNextGen
        //     .apply { #gmaNextGen
        if (Constants.DEBUG && Constants.IS_DEBUG_BUILD) {
            // setRequestConfiguration(makeAdsRequestConfig(context)) #gmaNextGen
            MobileAds.setRequestConfiguration(makeAdsRequestConfig(context))
            disableMediationAdapterInit(context, appId)
        }
        //     } #gmaNextGen
        // } #gmaNextGen
        // .build() #gmaNextGen
        MobileAds.initialize(
            activity.requireActivity(), // some adapters require activity
            // initConfig, #gmaNextGen
        ) { initializationStatus ->
            this.initialized.set(true)
            this.initializing.set(false)
            initializationStatus.adapterStatusMap.forEach { (adapterClass, status) ->
                logAdsD(
                    this@GlobalAdManager,
                    "onAdapterInitializationComplete() > Adapter name: $adapterClass, Status: ${status.initializationState}, Description: ${status.description}, Latency: ${status.latency}"
                )
            }
            onInitCompleteListener()
        }
    }

    suspend fun initHasSubscriptionFromCache() = withContext(ioDispatcher) {
        billingManager.getCachedHasSubscription()?.let { hasSubscription ->
            setHasSubscription(hasSubscription)
        }
    }

    fun setHasSubscription(hasSubscription: Boolean?) {
        this.hasSubscription = hasSubscription
    }

    fun canShowAds(): Boolean? {
        if (!AdConstants.AD_ENABLED) return false
        if (demoModeManager.enabled) return false
        return this.hasSubscription?.not()
    }

    fun isShowingAds(): Boolean {
        if (!AdConstants.AD_ENABLED) return false
        if (hasAgenciesEnabled == null) {
            hasAgenciesEnabled = this.dataSourcesRepository.hasAgenciesEnabled()
        }
        if (!this.initialized.get()) {
            logAdsD(this, "isShowingAds() > Not showing ads (not initialized yet).")
            return false // not showing ads
        }
        // number of agency unknown
        if (hasAgenciesEnabled == false) { // no (real) agency installed
            logAdsD(this, "isShowingAds() > Not showing ads (no agency added).")
            return false // not showing ads
        } else if (demoModeManager.enabled) {
            logAdsD(this, "isShowingAds() > Not showing ads (demo mode).")
            return false // not showing ads
        }
        if (hasSubscription == null) { // subscriptions unknown
            logAdsD(this, "isShowingAds() > Not showing ads (subscriptions unknown).")
            return false // not showing ads
        }
        logAdsD(this, "isShowingAds() > has subscriptions: '$hasSubscription'.")
        if (AdConstants.IGNORE_REWARD_HIDING_BANNER) {
            return hasSubscription == false
        }
        if (this._rewardedNow != false) { // rewarded status
            logAdsD(this, "isShowingAds() > Not showing banner ads (rewarded until: ${this._rewardedUntil?.toDateTimeLog()}).")
            return false // not showing ads
        }
        return hasSubscription == false
    }

    fun onHasAgenciesEnabledUpdated(hasAgenciesEnabled: Boolean?) {
        this.hasAgenciesEnabled = hasAgenciesEnabled
    }

    // region Rewarded

    @WorkerThread
    fun getRewardedUntil() = this.rewardedUserManager.getRewardedUntil()

    fun resetRewarded() {
        this.rewardedUserManager.resetRewarded()
    }

    @WorkerThread
    fun isRewardedNow() = this.rewardedUserManager.isRewardedNow()

    @WorkerThread
    fun rewardUser(newReward: Duration, activity: IActivity?) {
        this.rewardedUserManager.rewardUser(newReward, activity)
    }

    @WorkerThread
    fun shouldSkipLoadingRewardedAd() = this.rewardedUserManager.shouldSkipLoadingRewardedAd()

    val rewardedAdAmountInDays get() = this.rewardedUserManager.rewardedAdAmountInDays

    // endregion Rewarded
}
