package org.mtransit.android.ad.inlinebanner

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
) : AdLoadCallback<BannerAd>, MTLog.Loggable {

    constructor(
        inlineBannerAdManager: InlineBannerAdManager,
        crashReporter: CrashReporter,
        fragment: IFragment,
    ) : this(
        inlineBannerAdManager = inlineBannerAdManager,
        crashReporter = crashReporter,
        fragmentWR = WeakReference(fragment),
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${InlineBannerAdListener::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    override fun onAdFailedToLoad(adError: LoadAdError) {
        super.onAdFailedToLoad(adError)
        when (adError.code) {
            LoadAdError.ErrorCode.APP_ID_MISSING -> this.crashReporter.w(
                this,
                "Failed to received ad! App ID missing: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.INTERNAL_ERROR -> this.crashReporter.w(
                this,
                "Failed to received ad! Internal error code: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.INVALID_REQUEST -> this.crashReporter.w(
                this,
                "Failed to received ad! Invalid request error code: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.REQUEST_ID_MISMATCH -> this.crashReporter.w(
                this,
                "Failed to received ad! Request ID mismatch error code: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.NETWORK_ERROR -> MTLog.w(this, "Failed to received ad! Network error code: '%s' (%s).", adError.code, adError)

            LoadAdError.ErrorCode.NO_FILL -> MTLog.w(this, "Failed to received ad! No fill error code: '%s' (%s).", adError.code, adError)
            LoadAdError.ErrorCode.TIMEOUT,
            LoadAdError.ErrorCode.CANCELLED,
            LoadAdError.ErrorCode.NOT_FOUND,
            LoadAdError.ErrorCode.INVALID_AD_RESPONSE,
            LoadAdError.ErrorCode.AD_RESPONSE_ALREADY_USED,
                -> this.crashReporter.w(this, "Failed to received ad! Error code: '%s' (%s).", adError.code, adError)
        }
        val fragment = this.fragmentWR.get()
        if (fragment == null) {
            logAdsD(this, "onAdFailedToLoad() > SKIP (no fragment)")
            return
        }
        this.inlineBannerAdManager.setAdBannerLoaded(fragment, false)
        CoroutineScope(Dispatchers.Main).launch {
            inlineBannerAdManager.hideBannerAd(fragment) // hiding ads until next AUTOMATIC ad refresh
        }
    }

    override fun onAdLoaded(ad: BannerAd) {
        super.onAdLoaded(ad)
        val responseInfo = ad.getResponseInfo()
        logAdsD(this, "onAdLoaded() > ad loaded from ${responseInfo.adapterClassName}")
        val fragment = this.fragmentWR.get()
        if (fragment == null) {
            logAdsD(this, "onAdLoaded() > SKIP (no activity)")
            return
        }
        this.inlineBannerAdManager.setAdBannerLoaded(fragment, true)
        CoroutineScope(Dispatchers.Main).launch {
            inlineBannerAdManager.adaptToScreenSize(
                fragment,
            ) // showing ads if hidden because of no-fill/network error
        }
    }
}