package org.mtransit.android.ad.appopen

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import dagger.hilt.android.qualifiers.ApplicationContext
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.TimeUtilsK.EPOCH_TIME_0
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.toMillis
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.toDateTimeLog
import org.mtransit.android.ui.view.common.IActivity
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.takeIf
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
    private val adUnitStringResId: Int get() = R.string.google_ads_app_open_ad_unit_it

    @MainThread
    fun loadAd(): Boolean {
        if (!AdConstants.AD_ENABLED) return false
        if (!globalAdManager.isShowingAds()) {
            logAdsD(LOG_TAG, "loadAd() > SKIP (not showing ads).")
            return false
        }
        if (isLoadingAd.get() || isAdAvailable()) {
            logAdsD(LOG_TAG, "App open ad is either loading or has already loaded.")
            return false
        }
        isLoadingAd.set(true)
        AppOpenAd.load(
            appContext,
            appContext.getString(adUnitStringResId),
            AdRequest.Builder()
                .build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd.set(false)
                    loadTimeK = TimeUtilsK.currentInstant()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    logAdsD(LOG_TAG, "App open ad failed to load with error: ${loadAdError.message}")
                    isLoadingAd.set(false)
                }
            },
        )
        return true
    }

    fun showAdIfAvailable(activity: IActivity, onShowAdCompleteListener: () -> Unit) {
        if (!AdConstants.AD_ENABLED) return
        if (isShowingAd) {
            logAdsD(LOG_TAG, "The app open ad is already showing.")
            return // do not show the ad again.
        }
        if (appOpenAd == null) {
            logAdsD(LOG_TAG, "The app open ad is not ready yet.")
            onShowAdCompleteListener()
            return // Load an ad.
        }
        isShowingAd = true
        appOpenAd?.fullScreenContentCallback = AppOpenAdFullScreenContentCallback(this, crashReporter, onShowAdCompleteListener)
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
                && globalAdManager.isShowingAds()
                && (TimeUtilsK.currentInstant() < loadTimeK + 4.hours)
    }
}
