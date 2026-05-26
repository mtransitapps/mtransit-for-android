package org.mtransit.android.ad

// import com.google.android.libraries.ads.mobile.sdk.MobileAds #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.banner.AdSize #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.common.AdInspectorError #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.common.AdRequest #gmaNextGen
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdInspectorError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.appopen.AppOpenAdManager
import org.mtransit.android.ad.banner.BannerAdManager
import org.mtransit.android.ad.rewarded.RewardedAdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.view.common.IActivity
import javax.inject.Inject
import kotlin.time.Instant

/**
 * TESTING:
 * Add tools:replace="android:supportsRtl" in AndroidManifest.xml
 * Requires real app ID & unit IDs in keys.xml
 * AdMob test devices:
 * - https://support.google.com/admob/answer/9691433
 * - https://apps.admob.com/v2/settings/test-devices/list
 * Audience Network test devices:
 * - https://developers.facebook.com/docs/audience-network/guides/test
 * - https://business.facebook.com/pub/testing
 * MORE:
 * - https://developers.google.com/admob/android/test-ads
 */
class AdManager @Inject internal constructor(
    private val globalAdManager: GlobalAdManager,
    private val bannerAdManager: BannerAdManager,
    private val rewardedAdManager: RewardedAdManager,
    private val appOpenAdManager: AppOpenAdManager,
) : IAdManager,
    MTLog.Loggable {

    companion object {

        val LOG_TAG: String = AdManager::class.java.simpleName

        fun getAdRequest(
            @Suppress("unused") adUnitId: String,
            // ) = AdRequest.Builder(adUnitId).apply { #gmaNextGen
        ) = AdRequest.Builder().apply {
            AdConstants.KEYWORDS.forEach { addKeyword(it) }
        }.build()

        fun getBannerAdRequest(
            @Suppress("unused") adUnitId: String,
            @Suppress("unused") adSize: AdSize,
            collapsible: Boolean = false,
            // ) = BannerAdRequest.Builder(adUnitId, adSize).apply { #gmaNextGen
        ) = AdRequest.Builder().apply {
            AdConstants.KEYWORDS.forEach { addKeyword(it) }
            if (collapsible) {
                // setGoogleExtrasBundle( #gmaNextGen
                addNetworkExtrasBundle(
                    AdMobAdapter::class.java,
                    Bundle().apply {
                        putString("collapsible", "bottom")
                    }
                )
            }
        }.build()
    }

    override fun getLogTag() = LOG_TAG

    override fun init(activity: IAdScreenActivity, onInitCompleteListener: () -> Unit, withConsentOnly: Boolean) {
        this.globalAdManager.init(
            activity,
            onInitCompleteListener = onInitCompleteListener,
            withConsentOnly = withConsentOnly
        )
    }

    override fun initForScreens(activity: IAdScreenActivity) {
        init(
            activity,
            onInitCompleteListener = {
                activity.lifecycleScope.launch {
                    onShowingAdsUpdated(activity)
                }
            }
        )
    }

    override suspend fun onHasAgenciesEnabledUpdated(hasAgenciesEnabled: Boolean?, activity: IAdScreenActivity) {
        this.globalAdManager.onHasAgenciesEnabledUpdated(hasAgenciesEnabled)
        onShowingAdsUpdated(activity)
    }

    override suspend fun initHasSubscriptionFromCache() {
        this.globalAdManager.initHasSubscriptionFromCache()
    }

    override fun canShowAds() = this.globalAdManager.canShowAds()

    override suspend fun setHasSubscription(hasSubscription: Boolean?, activity: IAdScreenActivity) {
        this.globalAdManager.setHasSubscription(hasSubscription)
        onShowingAdsUpdated(activity)
    }

    private suspend fun onShowingAdsUpdated(activity: IAdScreenActivity) {
        withContext(Dispatchers.Main) {
            bannerAdManager.refreshBannerAdStatus(activity, force = false)
        }
        refreshRewardedAdStatus(activity)
    }

    // region Banner ads

    @MainThread
    override fun getBannerHeightInPx(activity: IAdScreenActivity?) = this.bannerAdManager.getBannerHeightInPx(activity)

    @MainThread
    override fun adaptToScreenSize(activity: IAdScreenActivity, configuration: Configuration?) = this.bannerAdManager.adaptToScreenSize(activity, configuration)

    @MainThread
    override fun pauseAd(activity: IAdScreenActivity) = this.bannerAdManager.pauseAd(activity)

    @MainThread
    override fun resumeAd(activity: IAdScreenActivity) = this.bannerAdManager.resumeAd(activity)

    @MainThread
    override fun destroyAd(activity: IAdScreenActivity) = this.bannerAdManager.destroyAd(activity)

    @MainThread
    override fun onResumeScreen(activity: IAdScreenActivity) = this.bannerAdManager.onResumeScreen(activity)

    @MainThread
    override fun onTimeChanged(activity: IAdScreenActivity) = this.bannerAdManager.onTimeChanged(activity)

    // endregion Banner ads

    // region Rewarded ads

    override val rewardedUntilLive: LiveData<Instant> get() = this.globalAdManager.rewardedUntil
    override val rewardedNowLive: LiveData<Boolean> get() = this.globalAdManager.rewardedNow

    @WorkerThread
    override fun getRewardedUntil() = this.globalAdManager.getRewardedUntil()

    override fun resetRewarded() = this.globalAdManager.resetRewarded()

    @WorkerThread
    override fun isRewardedNow() = this.globalAdManager.isRewardedNow()

    override fun setRewardedAdListener(rewardedAdListener: IAdManager.RewardedAdListener?) {
        this.rewardedAdManager.rewardedAdListener = rewardedAdListener
    }

    @WorkerThread
    override fun shouldSkipLoadingRewardedAd() = this.globalAdManager.shouldSkipLoadingRewardedAd()

    override fun linkRewardedAd(activity: IActivity) = this.rewardedAdManager.linkRewardedAd(activity)

    override fun unlinkRewardedAd(activity: IActivity) = this.rewardedAdManager.unlinkRewardedAd(activity)

    override suspend fun refreshRewardedAdStatus(activity: IActivity) = this.rewardedAdManager.refreshRewardedAdStatus(activity)

    override fun isRewardedAdAvailableToShow() = this.rewardedAdManager.isRewardedAdAvailableToShow()

    override fun showRewardedAd(activity: IActivity) = this.rewardedAdManager.showRewardedAd(activity)

    override val rewardedAdAmountInDays: Int get() = this.globalAdManager.rewardedAdAmountInDays

    // endregion Rewarded ads

    // region App open ads

    override fun isAppOpenAdAvailable() = this.appOpenAdManager.isAdAvailable()

    override fun isShowingAppOpenAd() = this.appOpenAdManager.isShowingAd()

    @MainThread
    override fun loadAppOpenAd() = this.appOpenAdManager.loadAd()

    override fun showAppOpenAdIfAvailable(activity: IActivity, onShowAdComplete: () -> Unit) =
        this.appOpenAdManager.showAdIfAvailable(activity, onShowAdComplete)

    // endregion App open ads

    override fun openAdInspector(activity: IActivity) {
        // MobileAds.openAdInspector { error: AdInspectorError? -> #gmaNextGen
        MobileAds.openAdInspector(activity.requireActivity()) { error: AdInspectorError? ->
            if (error == null) {
                logAdsD(this@AdManager, "Ad inspector closed.")
            } else {
                MTLog.w(this@AdManager, "Ad inspector closed: ${error.code} > $error!")
            }
        }
    }
}
