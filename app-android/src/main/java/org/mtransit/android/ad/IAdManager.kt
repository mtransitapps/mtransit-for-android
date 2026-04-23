package org.mtransit.android.ad

import android.content.res.Configuration
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import org.mtransit.android.ui.view.common.IActivity

interface IAdManager {

    fun init(activity: IAdScreenActivity)

    fun onHasAgenciesEnabledUpdated(hasAgenciesEnabled: Boolean?, activity: IAdScreenActivity)

    fun setShowingAds(newShowingAds: Boolean?, activity: IAdScreenActivity)

    // region Rewarded Ad

    fun getRewardedAdAmount(): Int

    fun getRewardedAdAmountInMs(): Long

    fun linkRewardedAd(activity: IActivity)

    fun unlinkRewardedAd(activity: IActivity)

    fun refreshRewardedAdStatus(activity: IActivity)

    fun isRewardedAdAvailableToShow(): Boolean

    fun showRewardedAd(activity: IActivity): Boolean

    // endregion Rewarded Ad

    fun getBannerHeightInPx(activity: IAdScreenActivity?): Int

    fun adaptToScreenSize(activity: IAdScreenActivity, configuration: Configuration? = activity.activity?.resources?.configuration)

    fun onResumeScreen(activity: IAdScreenActivity)

    fun onTimeChanged(activity: IAdScreenActivity)

    @Suppress("unused")
    fun resumeAd(activity: IAdScreenActivity)

    fun pauseAd(activity: IAdScreenActivity)

    fun destroyAd(activity: IAdScreenActivity)

    val rewardedUntilInMsLive: LiveData<Long>
    val rewardedNowLive: LiveData<Boolean>

    @WorkerThread
    fun getRewardedUntilInMs(): Long

    fun resetRewarded()

    @WorkerThread
    fun isRewardedNow(): Boolean

    fun setRewardedAdListener(rewardedAdListener: RewardedAdListener?)

    fun openAdInspector(activity: IActivity)

    @WorkerThread
    fun shouldSkipRewardedAd(): Boolean

    interface RewardedAdListener {
        @AnyThread
        fun onRewardedAdStatusChanged()

        @WorkerThread
        fun skipRewardedAd(): Boolean
    }
}
