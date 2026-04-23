package org.mtransit.android.ad

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.MainThread
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdInspectorError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.qualifiers.ApplicationContext
// import com.google.android.libraries.ads.mobile.sdk.MobileAds #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.banner.AdSize #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.common.AdInspectorError #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.common.AdRequest #gmaNextGen
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.banner.BannerAdManager
import org.mtransit.android.ad.rewarded.RewardedAdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.ui.view.common.IActivity
import javax.inject.Inject

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
    @param:ApplicationContext private val appContext: Context,
    private val globalAdManager: GlobalAdManager,
    private val bannerAdManager: BannerAdManager,
    private val rewardedAdManager: RewardedAdManager,
) : IAdManager, MTLog.Loggable {

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

    override fun init(activity: IAdScreenActivity) {
        this.globalAdManager.init(activity, this.bannerAdManager)
    }

    override fun onHasAgenciesEnabledUpdated(hasAgenciesEnabled: Boolean?, activity: IAdScreenActivity) {
        this.globalAdManager.onHasAgenciesEnabledUpdated(hasAgenciesEnabled)
        onShowingAdsUpdated(activity)
    }

    override fun setShowingAds(newShowingAds: Boolean?, activity: IAdScreenActivity) {
        this.globalAdManager.setShowingAds(newShowingAds)
        onShowingAdsUpdated(activity)
    }

    private fun onShowingAdsUpdated(activity: IAdScreenActivity) {
        this.bannerAdManager.refreshBannerAdStatus(activity, force = false)
        refreshRewardedAdStatus(activity)
    }

    // region Banner Ads

    override fun getBannerHeightInPx(activity: IAdScreenActivity?) = this.bannerAdManager.getBannerHeightInPx(activity)

    @MainThread
    override fun adaptToScreenSize(activity: IAdScreenActivity, configuration: Configuration?) = this.bannerAdManager.adaptToScreenSize(activity, configuration)

    override fun pauseAd(activity: IAdScreenActivity) = this.bannerAdManager.pauseAd(activity)

    override fun resumeAd(activity: IAdScreenActivity) = this.bannerAdManager.resumeAd(activity)

    override fun destroyAd(activity: IAdScreenActivity) = this.bannerAdManager.destroyAd(activity)

    override fun onResumeScreen(activity: IAdScreenActivity) = this.bannerAdManager.onResumeScreen(activity)

    override fun onTimeChanged(activity: IAdScreenActivity) = this.bannerAdManager.onTimeChanged(activity)

    // endregion Banner Ads

    // region Rewarded Ads

    override fun getRewardedUntilInMs() = this.globalAdManager.getRewardedUntilInMs()

    override fun resetRewarded() = this.globalAdManager.resetRewarded()

    override fun isRewardedNow() = this.globalAdManager.isRewardedNow()

    override fun setRewardedAdListener(rewardedAdListener: IAdManager.RewardedAdListener?) {
        this.rewardedAdManager.rewardedAdListener = rewardedAdListener
    }

    override fun shouldSkipRewardedAd() = this.globalAdManager.shouldSkipRewardedAd()

    override fun linkRewardedAd(activity: IActivity) = this.rewardedAdManager.linkRewardedAd(activity)

    override fun unlinkRewardedAd(activity: IActivity) = this.rewardedAdManager.unlinkRewardedAd(activity)

    override fun refreshRewardedAdStatus(activity: IActivity) = this.rewardedAdManager.refreshRewardedAdStatus(activity)

    override fun isRewardedAdAvailableToShow() = this.rewardedAdManager.isRewardedAdAvailableToShow()

    override fun showRewardedAd(activity: IActivity) = this.rewardedAdManager.showRewardedAd(activity)

    override fun getRewardedAdAmount() = this.globalAdManager.getRewardedAdAmount()

    override fun getRewardedAdAmountInMs() = this.globalAdManager.getRewardedAdAmountInMs()

    // endregion Rewarded Ads

    override fun openAdInspector(activity:IActivity) {
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
