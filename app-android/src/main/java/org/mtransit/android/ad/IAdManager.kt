package org.mtransit.android.ad

import android.content.res.Configuration
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import org.mtransit.android.ui.view.common.IActivity
import kotlin.time.Instant

interface IAdManager {

    fun init(activity: IAdScreenActivity, onInitCompleteListener: () -> Unit = {}, withConsentOnly: Boolean = false)
    fun initForScreens(activity: IAdScreenActivity)

    suspend fun onHasAgenciesEnabledUpdated(hasAgenciesEnabled: Boolean?, activity: IAdScreenActivity)

    suspend fun initHasSubscriptionFromCache()

    fun canShowAds(): Boolean?

    suspend fun setHasSubscription(hasSubscription: Boolean?, activity: IAdScreenActivity)

    // region App open ad

    fun isAppOpenAdAvailable(): Boolean

    fun isShowingAppOpenAd(): Boolean

    @MainThread
    fun loadAppOpenAd(): Boolean

    fun showAppOpenAdIfAvailable(activity: IActivity, onShowAdComplete: () -> Unit)

    // endregion App open ad

    // region Rewarded ad

    val rewardedAdAmountInDays: Int

    fun linkRewardedAd(activity: IActivity)

    fun unlinkRewardedAd(activity: IActivity)

    suspend fun refreshRewardedAdStatus(activity: IActivity)

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

    val rewardedUntilLive: LiveData<Instant>
    val rewardedNowLive: LiveData<Boolean>

    @WorkerThread
    fun getRewardedUntil(): Instant

    fun resetRewarded()

    @WorkerThread
    fun isRewardedNow(): Boolean

    fun setRewardedAdListener(rewardedAdListener: RewardedAdListener?)

    fun openAdInspector(activity: IActivity)

    @WorkerThread
    fun shouldSkipLoadingRewardedAd(): Boolean

    interface RewardedAdListener {
        fun onRewardedAdStatusChanged()

        @WorkerThread
        fun skipLoadingRewardedAd(): Boolean
    }
}
