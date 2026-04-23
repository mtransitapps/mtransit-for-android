package org.mtransit.android.ad.banner

import androidx.annotation.AnyThread
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
// import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd // #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback // #gmaNextGen
// import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError // #gmaNextGen
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
    private val adViewWR: WeakReference<AdView>,
    // ) : AdLoadCallback<BannerAd>, #gmaNextGen
) : AdListener(),
    MTLog.Loggable {

    constructor(
        bannerAdManager: BannerAdManager,
        crashReporter: CrashReporter,
        remoteConfigProvider: RemoteConfigProvider,
        adScreenActivity: IAdScreenActivity,
        adView: AdView,
    ) : this(
        bannerAdManager = bannerAdManager,
        crashReporter = crashReporter,
        remoteConfigProvider = remoteConfigProvider,
        activityWR = WeakReference(adScreenActivity),
        adViewWR = WeakReference(adView)
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

    @AnyThread
    override fun onAdFailedToLoad(adError: LoadAdError) {
        super.onAdFailedToLoad(adError)
        logAdsD(this, "onAdFailedToLoad($adError)")
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

            AdRequest.ERROR_CODE_MEDIATION_NO_FILL ->
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
        if (keepOldAdVisible && this.bannerAdManager.adBannerLoaded == true) {
            logAdsD(this, "onAdFailedToLoad() > keep old ad visible")
            return // keep old ad visible
        }
        this.activityWR.get()?.let { activity ->
            activity.activity?.runOnUiThread {
                this.bannerAdManager.setAdBannerLoaded(TimeUtils.currentTimeMillis(), false) // wait until next try, even if failed
                this.bannerAdManager.hideBannerAd(activity) // hiding ads until next AUTOMATIC ad refresh
            }
        }
    }

    @AnyThread
    // override fun onAdLoaded(ad: BannerAd) { #gmaNextGen
    // super.onAdLoaded(ad) #gmaNextGen
    // logAdsD(this, "onAdLoaded($ad)") #gmaNextGen
    override fun onAdLoaded() {
        super.onAdLoaded()
        logAdsD(this, "onAdLoaded()")
        // val responseInfo = ad.getResponseInfo().adapterClassName #gmaNextGen
        val adapterClassName = this.adViewWR.get()?.responseInfo?.mediationAdapterClassName
        logAdsD(this, "onAdLoaded() > ad loaded from $adapterClassName ")
        this.activityWR.get()?.let { activity ->
            this.bannerAdManager.setAdBannerLoaded(TimeUtils.currentTimeMillis(), true) // success
            activity.activity?.runOnUiThread {
                this.bannerAdManager.adaptToScreenSize(
                    activity,
                ) // showing ads if hidden because of no-fill/network error
            }
        }
    }
}
