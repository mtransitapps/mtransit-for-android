package org.mtransit.android.ad

import android.content.res.Configuration
import org.mtransit.android.ui.view.common.IActivity

interface IAdManager {
    fun init(activity: IActivity, adScreenFragment: IAdScreenFragment?)

    fun onHasAgenciesEnabledUpdated(hasAgenciesEnabled: Boolean?, activity: IActivity, adScreenFragment: IAdScreenFragment?)

    fun setShowingAds(newShowingAds: Boolean?, activity: IActivity, adScreenFragment: IAdScreenFragment?)

    fun getRewardedAdAmount(): Int

    fun getRewardedAdAmountInMs(): Long

    fun linkRewardedAd(activity: IActivity)

    fun unlinkRewardedAd(activity: IActivity)

    fun refreshRewardedAdStatus(activity: IActivity)

    fun isRewardedAdAvailableToShow(): Boolean

    fun showRewardedAd(activity: IActivity): Boolean

    fun getBannerHeightInPx(activity: IActivity?): Int

    fun adaptToScreenSize(activity: IActivity, configuration: Configuration?)

    fun onResumeScreen(activity: IActivity, adScreenFragment: IAdScreenFragment)

    @Suppress("unused")
    fun resumeAd(activity: IActivity)

    fun pauseAd(activity: IActivity)

    fun destroyAd(activity: IActivity)

    fun getRewardedUntilInMs(): Long

    fun resetRewarded()

    fun isRewardedNow(): Boolean

    fun setRewardedAdListener(rewardedAdListener: RewardedAdListener?)

    fun openAdInspector()

    fun shouldSkipRewardedAd(): Boolean

    interface RewardedAdListener {
        fun onRewardedAdStatusChanged()

        fun skipRewardedAd(): Boolean
    }
}
