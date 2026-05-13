package org.mtransit.android.ad.appopen

// import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback #gmaNextGen
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.dev.CrashReporter

class AppOpenAdFullScreenContentCallback(
    private val appOpenAdManager: AppOpenAdManager,
    private val crashReporter: CrashReporter,
    private val onShowAdCompleteListener: () -> Unit
    // ) : RewardedAdEventCallback, #gmaNextGen
) : FullScreenContentCallback(),
    MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${AppOpenAdFullScreenContentCallback::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    override fun onAdDismissedFullScreenContent() {
        logAdsD(this, "onAdDismissedFullScreenContent() > App open ad dismissed.")
        this.appOpenAdManager.appOpenAd = null // clear dismissed ad
        this.appOpenAdManager.isShowingAd = false
        this.onShowAdCompleteListener()
        // TODO ? this.appOpenAdManager.loadAd()
    }

    // override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) { #gmaNextGen
    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: AdError) {
        logAdsD(this, "onAdImpression() > App open ad failed to show fullscreen content.")
        this.appOpenAdManager.appOpenAd = null // clear dismissed ad
        this.appOpenAdManager.isShowingAd = false
        this.onShowAdCompleteListener()
        // TODO ? this.appOpenAdManager.loadAd()
        this.crashReporter.w(
            this,
            "Failed to show app open ad! ${fullScreenContentError.code}: " +
                    "'${fullScreenContentError.message}' " +
                    // "(${fullScreenContentError.mediationAdError})." #gmaNextGen
                    "(${fullScreenContentError.domain})."
        )
    }

    override fun onAdShowedFullScreenContent() { // Ad was shown
        logAdsD(this, "onAdShowedFullScreenContent() > App open ad showed fullscreen content.")
    }

    override fun onAdImpression() {
        logAdsD(this, "onAdImpression() > App open ad recorded an impression.")
    }

    override fun onAdClicked() {
        logAdsD(this, "onAdClicked() > App open ad was clicked.")
    }
}
