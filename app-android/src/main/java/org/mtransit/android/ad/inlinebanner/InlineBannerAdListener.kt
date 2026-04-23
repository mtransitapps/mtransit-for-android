package org.mtransit.android.ad.inlinebanner

import androidx.annotation.AnyThread
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
// import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError #gmaNextGen
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.IFragment
import java.lang.ref.WeakReference

class InlineBannerAdListener(
    private val inlineBannerAdManager: InlineBannerAdManager,
    private val crashReporter: CrashReporter,
    private val fragmentWR: WeakReference<IFragment>,
    private val adViewWR: WeakReference<AdView>,
    // ) : AdLoadCallback<BannerAd>, #gmaNextGen
) : AdListener(),
    MTLog.Loggable {

    constructor(
        inlineBannerAdManager: InlineBannerAdManager,
        crashReporter: CrashReporter,
        fragment: IFragment,
        adView: AdView,
    ) : this(
        inlineBannerAdManager = inlineBannerAdManager,
        crashReporter = crashReporter,
        fragmentWR = WeakReference(fragment),
        adViewWR = WeakReference(adView)
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${InlineBannerAdListener::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @AnyThread
    override fun onAdFailedToLoad(adError: LoadAdError) {
        super.onAdFailedToLoad(adError)
        when (adError.code) {
            AdRequest.ERROR_CODE_APP_ID_MISSING ->
                // LoadAdError.ErrorCode.APP_ID_MISSING -> #gmaNextGen
                this.crashReporter.w(this, "Failed to received ad! App ID missing: '${adError.code}' ($adError).")

            AdRequest.ERROR_CODE_INTERNAL_ERROR ->
                // LoadAdError.ErrorCode.INTERNAL_ERROR -> #gmaNextGen
                this.crashReporter.w(this, "Failed to received ad! Internal error code: '${adError.code}' ($adError).")

            AdRequest.ERROR_CODE_INVALID_REQUEST ->
                // LoadAdError.ErrorCode.INVALID_REQUEST -> #gmaNextGen
                this.crashReporter.w(this, "Failed to received ad! Invalid request error code: '${adError.code}' ($adError).")

            AdRequest.ERROR_CODE_REQUEST_ID_MISMATCH ->
                // LoadAdError.ErrorCode.REQUEST_ID_MISMATCH -> #gmaNextGen
                this.crashReporter.w(this, "Failed to received ad! Request ID mismatch error code: '${adError.code}' ($adError).")

            AdRequest.ERROR_CODE_NETWORK_ERROR ->
                // LoadAdError.ErrorCode.NETWORK_ERROR -> #gmaNextGen
                MTLog.w(this, "Failed to received ad! Network error code: '${adError.code}' ($adError).")

            AdRequest.ERROR_CODE_MEDIATION_NO_FILL,
            AdRequest.ERROR_CODE_NO_FILL ->
                // LoadAdError.ErrorCode.NO_FILL -> #gmaNextGen
                MTLog.w(this, "Failed to received ad! No fill error code: '${adError.code}' ($adError).")

            // LoadAdError.ErrorCode.TIMEOUT, #gmaNextGen
            // LoadAdError.ErrorCode.CANCELLED, #gmaNextGen
            // LoadAdError.ErrorCode.NOT_FOUND, #gmaNextGen
            // LoadAdError.ErrorCode.INVALID_AD_RESPONSE, #gmaNextGen
            // LoadAdError.ErrorCode.AD_RESPONSE_ALREADY_USED,#gmaNextGen
            else
                -> this.crashReporter.w(this, "Failed to received ad! Error code: '${adError.code}' ($adError).")
        }
        this.fragmentWR.get()?.let { fragment ->
            fragment.getActivity()?.runOnUiThread {
                this.inlineBannerAdManager.setAdBannerLoaded(fragment, false)
                this.inlineBannerAdManager.hideBannerAd(fragment) // hiding ads until next AUTOMATIC ad refresh
            }
        }
    }

    @AnyThread
    // override fun onAdLoaded(ad: BannerAd) { #gmaNextGen
    // super.onAdLoaded(ad) #gmaNextGen
    override fun onAdLoaded() {
        super.onAdLoaded()
        // val adapterClassName = ad.getResponseInfo().adapterClassName #gmaNextGen
        // logAdsD(this, "onAdLoaded() > ad loaded from $adapterClassName") #gmaNextGen
        this.fragmentWR.get()?.let { fragment ->
            fragment.getActivity()?.runOnUiThread {
                val adapterClassName = this.adViewWR.get()?.responseInfo?.mediationAdapterClassName
                logAdsD(this, "onAdLoaded() > ad loaded from $adapterClassName")
                this.inlineBannerAdManager.setAdBannerLoaded(fragment, true)
                this.inlineBannerAdManager.adaptToScreenSize(
                    fragment,
                ) // showing ads if hidden because of no-fill/network error
            }
        }
    }
}
