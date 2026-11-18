package org.mtransit.android.ad.banner

// import com.google.android.gms.ads.AdListener // #gmaLegacy
// import com.google.android.gms.ads.AdRequest // #gmaLegacy
// import com.google.android.gms.ads.AdView // #gmaLegacy
// import com.google.android.gms.ads.LoadAdError // #gmaLegacy
import androidx.annotation.AnyThread
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd // #gmaNextGen
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback // #gmaNextGen
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback // #gmaNextGen
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError // #gmaNextGen
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.dev.CrashReporter
import java.lang.ref.WeakReference

class BannerAdListener(
    private val bannerAdManager: BannerAdManager,
    private val crashReporter: CrashReporter,
    private val adRequestHashCode: Int,
    private val activityWR: WeakReference<IAdScreenActivity>,
    // private val adViewWR: WeakReference<AdView>, // #gmaLegacy
) : AdLoadCallback<BannerAd>, // #gmaNextGen
    BannerAdRefreshCallback, // #gmaNextGen
    // ) : AdListener(), // #gmaLegacy
    MTLog.Loggable {

    constructor(
        bannerAdManager: BannerAdManager,
        crashReporter: CrashReporter,
        adRequestHashCode: Int,
        adScreenActivity: IAdScreenActivity,
        // adView: AdView, // #gmaLegacy
    ) : this(
        bannerAdManager = bannerAdManager,
        crashReporter = crashReporter,
        adRequestHashCode = adRequestHashCode,
        activityWR = WeakReference(adScreenActivity),
        // adViewWR = WeakReference(adView) // #gmaLegacy
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${BannerAdListener::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @AnyThread
    override fun onAdFailedToLoad(adError: LoadAdError) {
        super.onAdFailedToLoad(adError)
        logAdsD(this, "onAdFailedToLoad($adError)")
        when (adError.code) {
            // AdRequest.ERROR_CODE_APP_ID_MISSING -> // #gmaLegacy
            LoadAdError.ErrorCode.APP_ID_MISSING -> // #gmaNextGen
                this.crashReporter.w(this, "Failed to receive ad! App ID missing: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_INTERNAL_ERROR -> // #gmaLegacy
            LoadAdError.ErrorCode.INTERNAL_ERROR -> // #gmaNextGen
                this.crashReporter.w(this, "Failed to receive ad! Internal error code: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_INVALID_REQUEST -> // #gmaLegacy
            LoadAdError.ErrorCode.INVALID_REQUEST -> // #gmaNextGen
                this.crashReporter.w(this, "Failed to receive ad! Invalid request error code: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_REQUEST_ID_MISMATCH -> // #gmaLegacy
            LoadAdError.ErrorCode.REQUEST_ID_MISMATCH -> // #gmaNextGen
                this.crashReporter.w(this, "Failed to receive ad! Request ID mismatch error code: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_NETWORK_ERROR -> // #gmaLegacy
            LoadAdError.ErrorCode.NETWORK_ERROR -> // #gmaNextGen
                MTLog.w(this, "Failed to receive ad! Network error code: '${adError.code}' ($adError).")

            // AdRequest.ERROR_CODE_MEDIATION_NO_FILL, // #gmaLegacy
            // AdRequest.ERROR_CODE_NO_FILL -> // #gmaLegacy
            LoadAdError.ErrorCode.NO_FILL -> // #gmaNextGen
                MTLog.w(this, "Failed to receive ad! No fill error code: '${adError.code}' ($adError).")

            LoadAdError.ErrorCode.TIMEOUT, // #gmaNextGen
            LoadAdError.ErrorCode.CANCELLED, // #gmaNextGen
            LoadAdError.ErrorCode.NOT_FOUND, // #gmaNextGen
            LoadAdError.ErrorCode.INVALID_AD_RESPONSE, // #gmaNextGen
            LoadAdError.ErrorCode.AD_RESPONSE_ALREADY_USED, // #gmaNextGen
                -> this.crashReporter.w(this, "Failed to receive ad! Error code: '${adError.code}' ($adError).")
            // else // #gmaLegacy
        }
        this.activityWR.get()?.let { activity ->
            activity.activity?.runOnUiThread {
                val previouslyLoadedAdToShow = this.bannerAdManager.adBannerLoaded
                // wait until next try, even if failed
                this.bannerAdManager.setAdBannerLoaded(adRequestHashCode, TimeUtils.currentTimeMillis(), previouslyLoadedAdToShow)
                if (previouslyLoadedAdToShow) {
                    logAdsD(this@BannerAdListener, "onAdFailedToLoad() > keep old ad visible")
                    return@runOnUiThread // keep old ad visible
                }
                this.bannerAdManager.hideBannerAd(activity) // hiding ads until next AUTOMATIC ad refresh
            }
        }
    }

    override fun onAdFailedToRefresh(adError: LoadAdError) { // #gmaNextGen
        super.onAdFailedToRefresh(adError) // #gmaNextGen
        logAdsD(this, "onAdFailedToRefresh($adError)") // #gmaNextGen
        onAdFailedToLoad(adError) // #gmaNextGen
    } // #gmaNextGen

    override fun onAdRefreshed() { // #gmaNextGen
        super.onAdRefreshed() // #gmaNextGen
        logAdsD(this, "onAdRefreshed()") // #gmaNextGen
        this.activityWR.get()?.let { activity -> // #gmaNextGen
            activity.activity?.runOnUiThread {
                this.bannerAdManager.setAdBannerLoaded(adRequestHashCode, TimeUtils.currentTimeMillis(), true) // success // #gmaNextGen
                this.bannerAdManager.adaptToScreenSize(activity) // showing ads if hidden because of no-fill/network error // #gmaNextGen
            }
        } // #gmaNextGen
    } // #gmaNextGen

    @AnyThread
    override fun onAdLoaded(ad: BannerAd) { // #gmaNextGen
        super.onAdLoaded(ad) // #gmaNextGen
        logAdsD(this, "onAdLoaded($ad)") // #gmaNextGen
        // override fun onAdLoaded() { // #gmaLegacy
        // super.onAdLoaded() // #gmaLegacy
        // logAdsD(this, "onAdLoaded()") // #gmaLegacy
        // // Called when an ad has loaded.
        // ad.adEventCallback =
        //     object : BannerAdEventCallback {}
        ad.bannerAdRefreshCallback = this // #gmaNextGen
        this.activityWR.get()?.let { activity ->
            activity.activity?.runOnUiThread {
                this.bannerAdManager.setAdBannerLoaded(adRequestHashCode, TimeUtils.currentTimeMillis(), true) // success
                // val adapterClassName = this.adViewWR.get()?.responseInfo?.mediationAdapterClassName // #gmaLegacy
                val adapterClassName = ad.getResponseInfo().adapterClassName // #gmaNextGen
                logAdsD(this, "onAdLoaded() > ad loaded from $adapterClassName ")
                this.bannerAdManager.adaptToScreenSize(activity) // showing ads if hidden because of no-fill/network error // #gmaNextGen
            }
        }
    }
}
