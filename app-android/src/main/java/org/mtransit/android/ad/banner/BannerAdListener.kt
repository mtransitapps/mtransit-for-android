package org.mtransit.android.ad.banner

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import java.lang.ref.WeakReference

class BannerAdListener(
    private val bannerAdManager: BannerAdManager,
    private val crashReporter: CrashReporter,
    private val remoteConfigProvider: RemoteConfigProvider,
    private val activityWR: WeakReference<IAdScreenActivity>,
) : AdLoadCallback<BannerAd>, MTLog.Loggable {

    constructor(
        bannerAdManager: BannerAdManager,
        crashReporter: CrashReporter,
        remoteConfigProvider: RemoteConfigProvider,
        adScreenActivity: IAdScreenActivity,
    ) : this(
        bannerAdManager = bannerAdManager,
        crashReporter = crashReporter,
        remoteConfigProvider = remoteConfigProvider,
        activityWR = WeakReference(adScreenActivity),
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${BannerAdListener::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    private val keepOldAdVisible: Boolean by lazy {
        remoteConfigProvider.get(
            RemoteConfigProvider.AD_BANNER_KEEP_OLD_AD_VISIBLE,
            RemoteConfigProvider.AD_BANNER_KEEP_OLD_AD_VISIBLE_DEFAULT
        )
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        super.onAdFailedToLoad(adError)
        logAdsD(this, "onAdFailedToLoad($adError)")
        when (adError.code) {
            LoadAdError.ErrorCode.APP_ID_MISSING -> this.crashReporter.w(
                this,
                "Failed to received ad! App ID missing: '%s' (%s).",
                adError.code,
                adError
            )

            LoadAdError.ErrorCode.INTERNAL_ERROR -> MTLog.w( // network error...
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
        if (keepOldAdVisible && this.bannerAdManager.adBannerLoaded == true) {
            logAdsD(this, "onAdFailedToLoad() > keep old ad visible")
            return // keep old ad visible
        }
        this.bannerAdManager.setAdBannerLoaded(TimeUtils.currentTimeMillis(), false) // wait until next try, even if failed
        val activity = this.activityWR.get()
        if (activity == null) {
            logAdsD(this, "onAdFailedToLoad() > SKIP (no activity)")
            return
        }
        this.bannerAdManager.hideBannerAd(activity) // hiding ads until next AUTOMATIC ad refresh
    }

    override fun onAdLoaded(ad: BannerAd) {
        super.onAdLoaded(ad)
        logAdsD(this, "onAdLoaded($ad)")
        val responseInfo = ad.getResponseInfo()
        logAdsD(this, "onAdLoaded() > ad loaded from ${responseInfo.adapterClassName} ")
        this.bannerAdManager.setAdBannerLoaded(TimeUtils.currentTimeMillis(), true) // success
        val activity = this.activityWR.get()
        if (activity == null) {
            logAdsD(this, "onAdLoaded() > SKIP (no activity)")
            return
        }
        this.bannerAdManager.adaptToScreenSize(
            activity,
        ) // showing ads if hidden because of no-fill/network error
    }
}