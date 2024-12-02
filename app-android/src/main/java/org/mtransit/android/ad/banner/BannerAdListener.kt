package org.mtransit.android.ad.banner

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.IActivity
import java.lang.ref.WeakReference

class BannerAdListener(
    private val bannerAdManager: BannerAdManager,
    private val crashReporter: CrashReporter,
    private val activityWR: WeakReference<IActivity>,
    private val adViewWR: WeakReference<AdView>,
) : AdListener(), Loggable {

    constructor(
        bannerAdManager: BannerAdManager,
        crashReporter: CrashReporter,
        activity: IActivity,
        adView: AdView,
    ) : this(
        bannerAdManager,
        crashReporter,
        WeakReference<IActivity>(activity),
        WeakReference<AdView>(adView)
    )

    companion object {
        private val LOG_TAG: String = "${AdManager.LOG_TAG}>${BannerAdListener::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
        MTLog.d(this, "onAdFailedToLoad(%s)", loadAdError)
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
        this.bannerAdManager.adBannerLoaded.set(false)
        val activity = this.activityWR.get()
        if (activity == null) {
            MTLog.d(this, "onAdFailedToLoad() > SKIP (no activity)")
            return
        }
        this.bannerAdManager.hideBannerAd(activity) // hiding ads until next AUTOMATIC ad refresh
    }

    override fun onAdLoaded() {
        MTLog.d(this, "onAdLoaded()")
        val adView = this.adViewWR.get()
        val responseInfo = adView?.responseInfo
        MTLog.d(this, "onAdLoaded() > ad loaded from %s", responseInfo?.mediationAdapterClassName)
        this.bannerAdManager.adBannerLoaded.set(true)
        val activity = this.activityWR.get()
        if (activity == null) {
            MTLog.d(this, "onAdLoaded() > SKIP (no activity)")
            return
        }
        this.bannerAdManager.adaptToScreenSize(
            activity,
            activity.requireContext().resources.configuration,
        ) // showing ads if hidden because of no-fill/network error
    }
}