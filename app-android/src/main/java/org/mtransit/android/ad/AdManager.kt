package org.mtransit.android.ad

import android.content.Context
import android.content.res.Configuration
import androidx.core.os.bundleOf
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdInspectorError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnAdInspectorClosedListener
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.BuildConfig
import org.mtransit.android.ad.banner.BannerAdManager
import org.mtransit.android.ad.rewarded.RewardedAdManager
import org.mtransit.android.common.IContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
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
    @ApplicationContext private val appContext: Context,
    private val globalAdManager: GlobalAdManager,
    private val bannerAdManager: BannerAdManager,
    private val rewardedAdManager: RewardedAdManager,
) : IAdManager, Loggable {

    companion object {

        val LOG_TAG: String = AdManager::class.java.simpleName

        fun getAdRequest(context: IContext, collapsible: Boolean = false) = AdRequest.Builder().apply {
            for (keyword in AdConstants.KEYWORDS) {
                addKeyword(keyword)
            }
            if (collapsible) {
                addNetworkExtrasBundle(
                    AdMobAdapter::class.java, bundleOf(
                        "collapsible" to "bottom"
                    )
                )
            }
        }.build()
            .also {
                if (BuildConfig.DEBUG) {
                    MTLog.d(LOG_TAG, "getAdRequest() > test device? %s.", it.isTestDevice(context.requireContext()))
                }
            }
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
        this.bannerAdManager.refreshBannerAdStatus(activity)
        refreshRewardedAdStatus(activity)
    }

    // region Banner Ads

    override fun getBannerHeightInPx(activity: IAdScreenActivity?) = this.bannerAdManager.getBannerHeightInPx(activity)

    override fun adaptToScreenSize(activity: IAdScreenActivity, configuration: Configuration?) = this.bannerAdManager.adaptToScreenSize(activity, configuration)

    override fun pauseAd(activity: IAdScreenActivity) = this.bannerAdManager.pauseAd(activity)

    override fun resumeAd(activity: IAdScreenActivity) = this.bannerAdManager.resumeAd(activity)

    override fun destroyAd(activity: IAdScreenActivity) = this.bannerAdManager.destroyAd(activity)

    override fun onResumeScreen(activity: IAdScreenActivity) = this.bannerAdManager.refreshBannerAdStatus(activity, force = true)

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

    override fun openAdInspector() {
        MobileAds.openAdInspector(this.appContext, OnAdInspectorClosedListener { error: AdInspectorError? ->
            if (error == null) {
                MTLog.d(this@AdManager, "Ad inspector closed.")
            } else {
                MTLog.w(this@AdManager, "Ad inspector closed: ${error.code} > $error!")
            }
        })
    }
}
