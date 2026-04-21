package org.mtransit.android.ad.rewarded

import androidx.annotation.AnyThread
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.dev.CrashReporter

class RewardedAdLoadCallback(
    private val rewardedAdManager: RewardedAdManager,
    private val crashReporter: CrashReporter,
) : AdLoadCallback<RewardedAd>, MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${RewardedAdLoadCallback::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @AnyThread
    override fun onAdLoaded(ad: RewardedAd) {
        super.onAdLoaded(ad)
        logAdsD(this, "onAdLoaded() > Rewarded ad loaded from ${ad.getResponseInfo().adapterClassName}.")
        this.rewardedAdManager.setRewardedAd(ad)
        this.rewardedAdManager.rewardedAdListener?.onRewardedAdStatusChanged()
    }

    @AnyThread
    override fun onAdFailedToLoad(adError: LoadAdError) {
        super.onAdFailedToLoad(adError)
        this.rewardedAdManager.setRewardedAd(null)
        this.rewardedAdManager.rewardedAdListener?.onRewardedAdStatusChanged()
        when (adError.code) {
            LoadAdError.ErrorCode.APP_ID_MISSING -> this.crashReporter.w(
                this,
                "Failed to received rewarded ad! App ID missing: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.INTERNAL_ERROR -> this.crashReporter.w(
                this,
                "Failed to received rewarded ad! Internal error code: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.INVALID_REQUEST -> this.crashReporter.w(
                this,
                "Failed to received rewarded ad! Invalid request error code: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.REQUEST_ID_MISMATCH -> this.crashReporter.w(
                this,
                "Failed to received rewarded ad! Request ID mismatch error code: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.NETWORK_ERROR -> MTLog.w(
                this,
                "Failed to received rewarded ad! Network error code: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.NO_FILL -> this.crashReporter.w(
                this,
                "Failed to received rewarded ad! No fill error code: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.TIMEOUT,
            LoadAdError.ErrorCode.CANCELLED,
            LoadAdError.ErrorCode.NOT_FOUND,
            LoadAdError.ErrorCode.INVALID_AD_RESPONSE,
            LoadAdError.ErrorCode.AD_RESPONSE_ALREADY_USED,
                -> this.crashReporter.w(this, "Failed to received rewarded ad! Error code: '%s' (%s).", adError.code, adError)
        }
    }
}
