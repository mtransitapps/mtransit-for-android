package org.mtransit.android.ad.rewarded

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.IActivity
import java.lang.ref.WeakReference

class RewardedAdFullScreenContentCallback(
    private val rewardedAdManager: RewardedAdManager,
    private val crashReporter: CrashReporter,
    private val activityWR: WeakReference<IActivity>,
) : FullScreenContentCallback(), Loggable {

    constructor(
        rewardedAdManager: RewardedAdManager,
        crashReporter: CrashReporter,
        activity: IActivity,
    ) : this(
        rewardedAdManager,
        crashReporter,
        WeakReference<IActivity>(activity),
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

    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
        this.crashReporter.w(this, "Failed to show rewarded ad! %s: '%s' (%s).", adError.code, adError.message, adError.domain)
    }

    override fun onAdDismissedFullScreenContent() {
        MTLog.d(this, "onAdDismissedFullScreenContent() > Rewarded ad dismissed.")
        this.rewardedAdManager.setRewardedAd(null) // clear dismissed ad
    }
}
