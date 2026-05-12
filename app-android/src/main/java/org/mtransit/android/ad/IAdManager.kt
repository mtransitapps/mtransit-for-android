package org.mtransit.android.ad

import android.content.res.Configuration
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import org.mtransit.android.ui.view.common.IActivity

interface IAdManager {

    fun init(activity: IAdScreenActivity, onInitCompleteListener: () -> Unit = {}, withConsentOnly: Boolean = false)
    fun initForBanner(activity: IAdScreenActivity)

    suspend fun onHasAgenciesEnabledUpdated(hasAgenciesEnabled: Boolean?, activity: IAdScreenActivity)

    suspend fun initShowingAdsFromCache()

    suspend fun setShowingAds(newShowingAds: Boolean?, activity: IAdScreenActivity)

    // region App open ad

    fun isAppOpenAdAvailable(): Boolean

    fun isShowingAppOpenAd(): Boolean

    @MainThread
    fun loadAppOpenAd(): Boolean

    fun showAppOpenAdIfAvailable(activity: IActivity, onShowAdCompleteListener: () -> Unit)

    // endregion App open ad

    // region Rewarded ad

    @AnyThread
    fun getRewardedAdAmount(): Int

    fun getRewardedAdAmountInMs(): Long

    fun linkRewardedAd(activity: IActivity)

    fun unlinkRewardedAd(activity: IActivity)

    suspend fun refreshRewardedAdStatus(activity: IActivity)

    @AnyThread
    fun isRewardedAdAvailableToShow(): Boolean

    fun showRewardedAd(activity: IActivity): Boolean

    // endregion Rewarded ad

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
