package org.mtransit.android.ad.rewarded

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.dev.CrashReporter

class RewardedAdLoadCallback(
    private val rewardedAdManager: RewardedAdManager,
    private val crashReporter: CrashReporter
) : RewardedAdLoadCallback(), Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${RewardedAdLoadCallback::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    override fun onAdLoaded(rewardedAd: RewardedAd) {
        MTLog.d(this, "onAdLoaded() > Rewarded ad loaded from %s.", rewardedAd.responseInfo.mediationAdapterClassName)
        this.rewardedAdManager.setRewardedAd(rewardedAd)
        val listener = this.rewardedAdManager.rewardedAdListener
        listener?.onRewardedAdStatusChanged()
    }

    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
        this.rewardedAdManager.setRewardedAd(null)
        val listener = this.rewardedAdManager.rewardedAdListener
        listener?.onRewardedAdStatusChanged()
        when (loadAdError.code) {
            AdRequest.ERROR_CODE_APP_ID_MISSING -> this.crashReporter.w(
                this,
                "Failed to received rewarded ad! App ID missing: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            AdRequest.ERROR_CODE_INTERNAL_ERROR -> this.crashReporter.w(
                this,
                "Failed to received rewarded ad! Internal error code: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            AdRequest.ERROR_CODE_INVALID_REQUEST -> this.crashReporter.w(
                this,
                "Failed to received rewarded ad! Invalid request error code: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            AdRequest.ERROR_CODE_NETWORK_ERROR -> MTLog.w(
                this,
                "Failed to received rewarded ad! Network error code: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            AdRequest.ERROR_CODE_NO_FILL -> this.crashReporter.w(
                this,
                "Failed to received rewarded ad! No fill error code: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            else -> this.crashReporter.w(this, "Failed to received rewarded ad! Error code: '%s' (%s).", loadAdError.code, loadAdError)
        }
    }
}
