package org.mtransit.android.ad

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.google.ads.mediation.pangle.PangleMediationAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.banner.BannerAdManager
import org.mtransit.android.ad.rewarded.RewardedUserManager
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.dev.DemoModeManager
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import com.bytedance.sdk.openadsdk.api.PAGConstant as PanglePAGConstant
import com.google.android.ump.FormError as UMPFormError

@Singleton
class GlobalAdManager(
    private val dataSourcesRepository: DataSourcesRepository,
    private val crashReporter: CrashReporter,
    private val demoModeManager: DemoModeManager,
    private val consentManager: AdsConsentManager,
    private val rewardedUserManager: RewardedUserManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MTLog.Loggable {

    @Inject
    constructor(
        dataSourcesRepository: DataSourcesRepository,
        crashReporter: CrashReporter,
        demoModeManager: DemoModeManager,
        consentManager: AdsConsentManager,
        rewardedUserManager: RewardedUserManager,
    ) : this(
        dataSourcesRepository = dataSourcesRepository,
        crashReporter = crashReporter,
        demoModeManager = demoModeManager,
        consentManager = consentManager,
        rewardedUserManager = rewardedUserManager,
        ioDispatcher = Dispatchers.IO,
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${GlobalAdManager::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    private val initialized = AtomicBoolean(false)

    private var showingAds: Boolean? = null
    private var hasAgenciesEnabled: Boolean? = null

    init {
        this.dataSourcesRepository.readingHasAgenciesEnabled().observeForever { hasAgenciesEnabled ->
            this.hasAgenciesEnabled = hasAgenciesEnabled
        }
    }

    fun init(activity: IAdScreenActivity, bannerAdManager: BannerAdManager) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        val theActivity = activity.activity
        if (theActivity == null) {
            MTLog.w(this, "Trying to initialized w/o activity!")
            return // SKIP
        }
        consentManager.gatherConsent(theActivity) { formError: UMPFormError? ->
            formError?.let {
                logAdsD(this@GlobalAdManager, "Consent not obtained [${formError.errorCode}]: ${formError.message}.")
            }
            if (consentManager.canRequestAds) {
                initWithConsent(activity, bannerAdManager)
            }
            if (consentManager.isPrivacyOptionsRequired) {
                activity.onPrivacyOptionsRequiredChanged()
            }
        }
        if (consentManager.canRequestAds) { // IF consent already given in previous session DO
            initWithConsent(activity, bannerAdManager)
        }
    }

    private fun initWithConsent(activity: IAdScreenActivity, bannerAdManager: BannerAdManager) {
        if (initialized.getAndSet(true)) {
            logAdsD(this, "init() > SKIP (initialized: ${this.initialized.get()})")
            return // SKIP
        }
        try {
            CoroutineScope(ioDispatcher).launch {
                initOnBackgroundThread(activity, bannerAdManager)
            }
        } catch (e: Exception) {
            this.crashReporter.w(this, e, "Error while initializing Ads!")
        }
    }

    @WorkerThread
    private fun initOnBackgroundThread(activity: IAdScreenActivity, bannerAdManager: BannerAdManager) {
        if (AdConstants.DEBUG) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(
                        listOf(*activity.requireContext().resources.getStringArray(R.array.google_ads_test_devices_ids))
                                + listOf(AdRequest.DEVICE_ID_EMULATOR)
                    )
                    .build()
            )
        }
        PangleMediationAdapter.setGDPRConsent(PanglePAGConstant.PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_CONSENT) // EU user consent policy
        PangleMediationAdapter.setPAConsent(PanglePAGConstant.PAGPAConsentType.PAG_PA_CONSENT_TYPE_CONSENT) // US states privacy laws
        // https://developers.google.com/admob/android/quick-start#initialize_the_mobile_ads_sdk
        MobileAds.initialize(
            activity.requireActivity(), // some adapters require activity
        ) { initializationStatus ->
            initializationStatus.adapterStatusMap.forEach { (adapterClass, status) ->
                logAdsD(this, "Adapter name: $adapterClass, Description: ${status.description}, Latency: ${status.latency}")
            }
            bannerAdManager.refreshBannerAdStatus(activity, force = false)
        }
    }

    fun setShowingAds(showingAds: Boolean?) {
        this.showingAds = showingAds
    }

    @AnyThread
    fun isShowingAds(): Boolean {
        if (!AdConstants.AD_ENABLED) {
            return false
        }
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
        if (showingAds == null) { // paying status unknown
            logAdsD(this, "isShowingAds() > Not showing ads (paying status unknown).")
            return false // not showing ads
        }
        logAdsD(this, "isShowingAds() > Showing ads: '$showingAds'.")
        if (AdConstants.IGNORE_REWARD_HIDING_BANNER) {
            return showingAds == true
        }
        if (isRewardedNow()) { // rewarded status
            logAdsD(this, "isShowingAds() > Not showing banner ads (rewarded until: ${this.rewardedUserManager.getRewardedUntilInMs()}).")
            return false // not showing ads
        }
        return showingAds == true
    }

    fun onHasAgenciesEnabledUpdated(hasAgenciesEnabled: Boolean?) {
        this.hasAgenciesEnabled = hasAgenciesEnabled
    }

    // region Rewarded

    fun getRewardedUntilInMs(): Long {
        return this.rewardedUserManager.getRewardedUntilInMs()
    }

    fun resetRewarded() {
        this.rewardedUserManager.resetRewarded()
    }

    fun isRewardedNow(): Boolean {
        return this.rewardedUserManager.isRewardedNow()
    }

    fun rewardUser(newRewardInMs: Long, context: IContext?) {
        this.rewardedUserManager.rewardUser(newRewardInMs, context)
    }

    fun shouldSkipRewardedAd(): Boolean {
        return this.rewardedUserManager.shouldSkipRewardedAd()
    }

    fun getRewardedAdAmount(): Int {
        return this.rewardedUserManager.getRewardedAdAmount()
    }

    fun getRewardedAdAmountInMs(): Long {
        return this.rewardedUserManager.getRewardedAdAmountInMs()
    }

    // endregion Rewarded
}