package org.mtransit.android.ad.appopen

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd // #gmaNextGen
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback // #gmaNextGen
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError // #gmaNextGen
// import com.google.android.gms.ads.LoadAdError // #gmaLegacy
// import com.google.android.gms.ads.appopen.AppOpenAd // #gmaLegacy
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.toMillis
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.IActivity
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class AppOpenAdManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val globalAdManager: GlobalAdManager,
    private val crashReporter: CrashReporter,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${AppOpenAdManager::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @Volatile
    internal var appOpenAd: AppOpenAd? = null

    private var isLoadingAd = AtomicBoolean(false)
    private var _isShowingAd = AtomicBoolean(false)
    internal var isShowingAd: Boolean
        get() = _isShowingAd.get()
        set(value) = _isShowingAd.set(value)

    private var _loadTime = AtomicLong(0L)

    private var loadTimeK: Instant
        get() = _loadTime.get().millisToInstant()
        set(value) = _loadTime.set(value.toMillis())

    @get:StringRes
    private val adUnitStringResId: Int get() = R.string.google_ads_app_open_ad_unit_id

    @MainThread
    fun loadAd(): Boolean {
        if (!AdConstants.AD_ENABLED) return false
        if (!globalAdManager.adsAllowed()) {
            logAdsD(LOG_TAG, "loadAd() > SKIP (ads not allowed).")
            return false
        }
        if (isLoadingAd.get()) {
            logAdsD(LOG_TAG, "App open ad is already loading.")
            return false
        }
        if (isAdAvailable()) {
            logAdsD(LOG_TAG, "App open ad has already been loaded and is available to show.")
            return false
        }
        isLoadingAd.set(true)
        AppOpenAd.load(
            // Must be called on the main UI thread
            // appContext, // #gmaLegacy
            // appContext.getString(adUnitStringResId), // #gmaLegacy
            AdManager.getAdRequest(
                adUnitId = appContext.getString(adUnitStringResId)
            ),
            object : AdLoadCallback<AppOpenAd> { // #gmaNextGen
                // object : AppOpenAd.AppOpenAdLoadCallback() { // #gmaLegacy
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd.set(false)
                    loadTimeK = TimeUtilsK.currentInstant()
                }

                // override fun onAdFailedToLoad(loadAdError: LoadAdError) { // #gmaLegacy
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // val loadAdErrorMessage = loadAdError.message // #gmaLegacy
                    val loadAdErrorMessage = adError.message // #gmaNextGen
                    logAdsD(LOG_TAG, "App open ad failed to load with error: $loadAdErrorMessage")
                    isLoadingAd.set(false)
                }
            },
        )
        return true
    }

    fun showAdIfAvailable(activity: IActivity, onShowAdComplete: () -> Unit) {
        if (!AdConstants.AD_ENABLED) return
        if (isShowingAd) {
            logAdsD(LOG_TAG, "The app open ad is already showing.")
            return // do not show the ad again.
        }
        if (appOpenAd == null) {
            logAdsD(LOG_TAG, "The app open ad is not ready yet.")
            onShowAdComplete()
            return // Load an ad.
        }
        isShowingAd = true
        // appOpenAd?.fullScreenContentCallback = // #gmaLegacy
        appOpenAd?.adEventCallback = // #gmaNextGen
            AppOpenAdFullScreenContentCallback(this, crashReporter, onShowAdComplete)
        appOpenAd?.show(activity.requireActivity())
    }

    fun isShowingAd(): Boolean {
        return isShowingAd
    }

    // https://support.google.com/admob/answer/9341964
    fun isAdAvailable(): Boolean {
        if (!AdConstants.AD_ENABLED) return false
        return appOpenAd != null
                && !isShowingAd
                && globalAdManager.adsAllowed()
                && (TimeUtilsK.currentInstant() < loadTimeK + 4.hours)
    }
}
