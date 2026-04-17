package org.mtransit.android.ad.rewarded

import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.IActivity
import java.lang.ref.WeakReference

class RewardedAdFullScreenContentCallback(
    private val rewardedAdManager: RewardedAdManager,
    private val crashReporter: CrashReporter,
    private val activityWR: WeakReference<IActivity>,
) : RewardedAdEventCallback, MTLog.Loggable {

    constructor(
        rewardedAdManager: RewardedAdManager,
        crashReporter: CrashReporter,
        activity: IActivity,
    ) : this(
        rewardedAdManager = rewardedAdManager,
        crashReporter = crashReporter,
        activityWR = WeakReference(activity),
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${RewardedAdFullScreenContentCallback::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    override fun onAdShowedFullScreenContent() { // Ad was shown
        this.rewardedAdManager.setRewardedAd(null) // clear showed ad
        val activity = this.activityWR.get()
        val theActivity = activity?.activity
        if (theActivity != null && !theActivity.isDestroyed && !theActivity.isFinishing) {
            this.rewardedAdManager.refreshRewardedAdStatus(activity)
        }
    }

    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
        super.onAdFailedToShowFullScreenContent(fullScreenContentError)
        this.crashReporter.w(this, "Failed to show rewarded ad! %s: '%s' (%s).", fullScreenContentError.code, fullScreenContentError.message, fullScreenContentError.mediationAdError)
    }

    override fun onAdDismissedFullScreenContent() {
        logAdsD(this, "onAdDismissedFullScreenContent() > Rewarded ad dismissed.")
        this.rewardedAdManager.setRewardedAd(null) // clear dismissed ad
    }
}
