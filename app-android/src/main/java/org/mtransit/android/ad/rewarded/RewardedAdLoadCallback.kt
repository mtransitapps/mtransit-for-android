package org.mtransit.android.ad.rewarded

import androidx.annotation.AnyThread
// import com.google.android.gms.ads.AdRequest // #gmaLegacy
// import com.google.android.gms.ads.LoadAdError // #gmaLegacy
// import com.google.android.gms.ads.rewarded.RewardedAd // #gmaLegacy
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback // #gmaNextGen
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError // #gmaNextGen
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd // #gmaNextGen
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.dev.CrashReporter

// import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback as GRewardedAdLoadCallback  // #gmaLegacy

class RewardedAdLoadCallback(
    private val rewardedAdManager: RewardedAdManager,
    private val crashReporter: CrashReporter,
    private val activityHashCode: Int,
) : AdLoadCallback<RewardedAd>, // #gmaNextGen
    // ) : GRewardedAdLoadCallback(), // #gmaLegacy
    MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${RewardedAdLoadCallback::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @AnyThread
    override fun onAdLoaded(ad: RewardedAd) {
        super.onAdLoaded(ad)
        val adapterClassName = ad.getResponseInfo().adapterClassName // #gmaNextGen
        // val adapterClassName = ad.responseInfo.mediationAdapterClassName // #gmaLegacy
        // val adRewardedItem = ad.rewardItem // #gmaLegacy
        val adRewardedItem = ad.getRewardItem() // #gmaNextGen
        logAdsD(this, "onAdLoaded() > Rewarded ad '${adRewardedItem.toStringPlus()}' loaded from '$adapterClassName'.")
        this.rewardedAdManager.onRewardedAdLoadingComplete(ad, activityHashCode)
    }

    @AnyThread
    override fun onAdFailedToLoad(adError: LoadAdError) {
        super.onAdFailedToLoad(adError)
        this.rewardedAdManager.onRewardedAdLoadingComplete(null, activityHashCode)
        when (adError.code) {
            // AdRequest.ERROR_CODE_APP_ID_MISSING -> // #gmaLegacy
            LoadAdError.ErrorCode.APP_ID_MISSING -> // #gmaNextGen
                this.crashReporter.w(this, "Failed to receive rewarded ad! App ID missing: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_INTERNAL_ERROR -> // #gmaLegacy
            LoadAdError.ErrorCode.INTERNAL_ERROR -> // #gmaNextGen
                this.crashReporter.w(this, "Failed to receive rewarded ad! Internal error code: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_INVALID_REQUEST -> // #gmaLegacy
            LoadAdError.ErrorCode.INVALID_REQUEST -> // #gmaNextGen
                this.crashReporter.w(this, "Failed to receive rewarded ad! Invalid request error code: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_REQUEST_ID_MISMATCH -> // #gmaLegacy
            LoadAdError.ErrorCode.REQUEST_ID_MISMATCH -> // #gmaNextGen
                this.crashReporter.w(this, "Failed to receive rewarded ad! Request ID mismatch error code: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_NETWORK_ERROR -> // #gmaLegacy
            LoadAdError.ErrorCode.NETWORK_ERROR -> // #gmaNextGen
                MTLog.w(this, "Failed to receive rewarded ad! Network error code: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_MEDIATION_NO_FILL, // #gmaLegacy
            // AdRequest.ERROR_CODE_NO_FILL -> // #gmaLegacy
            LoadAdError.ErrorCode.NO_FILL -> // #gmaNextGen
                this.crashReporter.w(this, "Failed to receive rewarded ad! No fill error code: '${adError.code}' ($adError).")

            LoadAdError.ErrorCode.TIMEOUT, // #gmaNextGen
            LoadAdError.ErrorCode.CANCELLED, // #gmaNextGen
            LoadAdError.ErrorCode.NOT_FOUND, // #gmaNextGen
            LoadAdError.ErrorCode.INVALID_AD_RESPONSE, // #gmaNextGen
            LoadAdError.ErrorCode.AD_RESPONSE_ALREADY_USED, // #gmaNextGen
                // else // #gmaLegacy
                -> this.crashReporter.w(this, "Failed to receive rewarded ad! Error code: '${adError.code}' ($adError).")
        }
    }
}
