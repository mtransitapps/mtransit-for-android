package org.mtransit.android.ad.banner

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.view.ViewGroup
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TaskUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.view.common.isVisibleOnce
import org.mtransit.commons.FeatureFlags
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

// Anchored adaptive banner
@Singleton
class BannerAdManager @Inject constructor(
    private val globalAdManager: GlobalAdManager,
    private val crashReporter: CrashReporter,
    private val remoteConfigProvider: RemoteConfigProvider,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${BannerAdManager::class.java.simpleName}"

        private const val LOADED_UNKNOWN = -1L
    }

    override fun getLogTag() = LOG_TAG

    private val _adBannerLoadedLastInMs = AtomicLong(LOADED_UNKNOWN)

    private var adBannerLoadedLastInMs: Long
        get() = _adBannerLoadedLastInMs.get()
        set(value) = _adBannerLoadedLastInMs.set(value)

    var adBannerLoaded: Boolean? = null
        private set

    private var setupBannerAdTask: SetupBannerAdTask? = null

    private val loadOnScreenResumeMinDurationSec: Long by lazy {
        remoteConfigProvider.get(
            RemoteConfigProvider.AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC,
            RemoteConfigProvider.AD_BANNER_LOAD_ON_SCREEN_RESUME_MIN_DURATION_SEC_DEFAULT
        )
    }

    internal val loadOnScreenResume: Boolean by lazy {
        loadOnScreenResumeMinDurationSec > 0L
    }

    fun setAdBannerLoaded(lastInMs: Long, loaded: Boolean?) {
        this.adBannerLoadedLastInMs = lastInMs
        this.adBannerLoaded = loaded
    }

    @MainThread
    fun onResumeScreen(activity: IAdScreenActivity) {
        logAdsD(this, "onTimeChanged($activity)")
        refreshBannerAdStatus(activity, force = loadOnScreenResume)
    }

    @MainThread
    fun onTimeChanged(activity: IAdScreenActivity) {
        logAdsD(this, "onTimeChanged($activity)")
        refreshBannerAdStatus(activity, force = loadOnScreenResume)
    }

    @MainThread
    @JvmOverloads
    fun refreshBannerAdStatus(activity: IAdScreenActivity, force: Boolean = false) {
        logAdsD(this, "refreshBannerAdStatus($force)")
        if (this.globalAdManager.isShowingAds() // showing ads across the app
            && activity.currentAdFragment?.hasAds() == false // this specific screen doesn't include ads already
        ) {
            if (this.adBannerLoaded != true || force) { // IF ad was not loaded DO
                setupBannerAd(activity, force)
            }
        } else { // ELSE IF not showing ads DO
            if (this.adBannerLoaded == true) { // IF ad was loaded DO
                hideBannerAd(activity)
                pauseAd(activity)
            }
        }
    }

    @MainThread
    fun adaptToScreenSize(activity: IAdScreenActivity, configuration: Configuration? = activity.context?.resources?.configuration) {
        if (!AdConstants.AD_ENABLED) return
        if (!this.globalAdManager.isShowingAds()) return
        if (activity.currentAdFragment?.hasAds() == true) return
        if (isEnoughSpaceForBanner(configuration)) {
            if (this.adBannerLoaded == true) {
                resumeAd(activity)
                showBannerAd(activity)
            }
        } else {
            hideBannerAd(activity)
            pauseAd(activity)
        }
    }

    fun isEnoughSpaceForBanner(configuration: Configuration?): Boolean {
        if (FeatureFlags.F_NAVIGATION) return true // always show
        if (configuration == null) return false
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) return true
        val sizeMask = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val smallScreen = sizeMask == Configuration.SCREENLAYOUT_SIZE_SMALL || sizeMask == Configuration.SCREENLAYOUT_SIZE_NORMAL
        return !smallScreen
    }

    private fun setupBannerAd(activity: IAdScreenActivity, force: Boolean) {
        logAdsD(this, "setupAd($force) --------------------")
        if (!AdConstants.AD_ENABLED) {
            logAdsD(this, "setupAd() > SKIP (AD not enabled) --------------------")
            return
        }
        if (!this.globalAdManager.isShowingAds()) {
            logAdsD(this, "setupAd() > SKIP (not showing ads) --------------------")
            return
        }
        if (force
            && setupBannerAdTask != null // task to cancel or not cancel
            && this.adBannerLoaded != null // state unknown/loading
        ) {
            logAdsD(this, "setupAd() > should we cancel?")
            val minDurationMs = loadOnScreenResumeMinDurationSec.seconds.inWholeMilliseconds
            if (this.adBannerLoadedLastInMs + minDurationMs < TimeUtils.currentTimeMillis()) { // force refresh if ad loaded only
                logAdsD(this, "setupAd() > CANCELLING previous setup ad task...")
                TaskUtils.cancelQuietly(setupBannerAdTask, true)
                setupBannerAdTask = null
            } else {
                logAdsD(this, "setupAd() > not cancelling previous setup ad task...")
            }
        } else {
            logAdsD(this, "setupAd() > SKIP (force?$force|setupBannerAdTask?${setupBannerAdTask != null}|adBannerLoaded:$adBannerLoaded)")
        }
        if (setupBannerAdTask == null) {
            logAdsD(this, "setupAd() > STARTING setup ad task...")
            setupBannerAdTask = SetupBannerAdTask(this.globalAdManager, this, this.crashReporter, this.remoteConfigProvider, activity)
            TaskUtils.execute(setupBannerAdTask)
            this.adBannerLoaded = null // loading
        } else {
            logAdsD(this, "setupAd() > SKIP (task already running)")
        }
        logAdsD(this, "setupAd() > DONE --------------------")
    }

    @AnyThread
    private fun showBannerAd(activity: IAdScreenActivity) {
        val adLayout = getAdLayout(activity) ?: return
        adLayout.post { adLayout.isVisibleOnce = true }
        val adView = getAdView(adLayout) ?: return
        adView.post { adView.isVisibleOnce = true }
    }

    @AnyThread
    fun hideBannerAd(activity: IAdScreenActivity) {
        val adLayout = getAdLayout(activity) ?: return
        adLayout.post { adLayout.isVisibleOnce = false }
        val adView = getAdView(adLayout) ?: return
        adView.post { adView.isVisibleOnce = false }
    }

    fun resumeAd(@Suppress("unused") activity: IAdScreenActivity) {
        // DO NOTHING
    }

    fun pauseAd(@Suppress("unused") activity: IAdScreenActivity) {
        // DO NOTHING
    }

    fun getAdLayout(activity: IAdScreenActivity): ViewGroup? =
        activity.requireActivity().findViewById(R.id.ad_layout)

    @Suppress("unused")
    fun getAdView(activity: IAdScreenActivity) = getAdLayout(activity)?.let { getAdView(it) }

    fun getAdView(adLayout: ViewGroup): AdView? =
        adLayout.findViewById(R.id.ad)

    fun destroyAd(activity: IAdScreenActivity) {
        if (!AdConstants.AD_ENABLED) return
        val adLayout = getAdLayout(activity)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            if (adView != null) {
                try {
                    adView.removeAllViews()
                    adView.destroy()
                } catch (t: Throwable) {
                    this.crashReporter.w(this, t, "Error while destroying ad view!")
                }
            }
        }
        setAdBannerLoaded(LOADED_UNKNOWN, false) // reset
        TaskUtils.cancelQuietly(setupBannerAdTask, true)
        setupBannerAdTask = null
    }

    fun getBannerHeightInPx(activity: IAdScreenActivity?): Int {
        if (this.adBannerLoaded != true) return 0 // ad not loaded
        if (!this.globalAdManager.isShowingAds()) return 0 // not showing ads (0 agency installed, paying user...)
        if (activity == null) return 0 // can't measure w/o context
        val adSize = getAdSize(activity)
        return adSize.getHeightInPixels(activity.requireContext())
    }

    fun getAdSize(activity: IAdScreenActivity): AdSize = with(activity.requireActivity()) {
        val displayMetrics = resources.displayMetrics
        val adWidthPixels =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                this.windowManager.currentWindowMetrics.bounds.width()
            } else {
                displayMetrics.widthPixels
            }
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()
        if (remoteConfigProvider.get(RemoteConfigProvider.AD_BANNER_LARGE, RemoteConfigProvider.AD_BANNER_LARGE_DEFAULT)) {
            return AdSize.getLargeAnchoredAdaptiveBannerAdSize(this, adWidth)
        }
        @SuppressLint("DeprecatedCall")
        @Suppress("DEPRECATION") // recommended replacement don't show the same ad size!
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }
}
