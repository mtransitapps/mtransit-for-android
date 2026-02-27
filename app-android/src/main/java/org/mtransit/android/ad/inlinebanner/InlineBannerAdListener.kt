package org.mtransit.android.ad.inlinebanner

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.IFragment
import java.lang.ref.WeakReference

class InlineBannerAdListener(
    private val inlineBannerAdManager: InlineBannerAdManager,
    private val crashReporter: CrashReporter,
    private val fragmentWR: WeakReference<IFragment>,
    private val adViewWR: WeakReference<AdView>,
) : AdListener(), Loggable {

    constructor(
        inlineBannerAdManager: InlineBannerAdManager,
        crashReporter: CrashReporter,
        fragment: IFragment,
        adView: AdView,
    ) : this(
        inlineBannerAdManager,
        crashReporter,
        WeakReference<IFragment>(fragment),
        WeakReference<AdView>(adView)
    )

    companion object {
        private val LOG_TAG: String = "${AdManager.LOG_TAG}>${InlineBannerAdListener::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
        when (loadAdError.code) {
            AdRequest.ERROR_CODE_APP_ID_MISSING -> this.crashReporter.w(
                this,
                "Failed to received ad! App ID missing: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            AdRequest.ERROR_CODE_INTERNAL_ERROR -> this.crashReporter.w(
                this,
                "Failed to received ad! Internal error code: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            AdRequest.ERROR_CODE_INVALID_REQUEST -> this.crashReporter.w(
                this,
                "Failed to received ad! Invalid request error code: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            AdRequest.ERROR_CODE_REQUEST_ID_MISMATCH -> this.crashReporter.w(
                this,
                "Failed to received ad! Request ID mismatch error code: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            AdRequest.ERROR_CODE_NETWORK_ERROR -> MTLog.w(this, "Failed to received ad! Network error code: '%s' (%s).", loadAdError.code, loadAdError)
            AdRequest.ERROR_CODE_MEDIATION_NO_FILL -> MTLog.w(
                this,
                "Failed to received ad! Mediation no fill error code: '%s' (%s).",
                loadAdError.code,
                loadAdError
            )

            AdRequest.ERROR_CODE_NO_FILL -> MTLog.w(this, "Failed to received ad! No fill error code: '%s' (%s).", loadAdError.code, loadAdError)
            else -> this.crashReporter.w(this, "Failed to received ad! Error code: '%s' (%s).", loadAdError.code, loadAdError)
        }
        val fragment = this.fragmentWR.get()
        if (fragment == null) {
            logAdsD(this, "onAdFailedToLoad() > SKIP (no fragment)")
            return
        }
        this.inlineBannerAdManager.setAdBannerLoaded(fragment, false)
        this.inlineBannerAdManager.hideBannerAd(fragment) // hiding ads until next AUTOMATIC ad refresh
    }

    override fun onAdLoaded() {
        val adView = this.adViewWR.get()
        val responseInfo = adView?.responseInfo
        logAdsD(this, "onAdLoaded() > ad loaded from ${responseInfo?.mediationAdapterClassName}")
        val fragment = this.fragmentWR.get()
        if (fragment == null) {
            logAdsD(this, "onAdLoaded() > SKIP (no activity)")
            return
        }
        this.inlineBannerAdManager.setAdBannerLoaded(fragment, true)
        this.inlineBannerAdManager.adaptToScreenSize(
            fragment,
        ) // showing ads if hidden because of no-fill/network error
    }
}