package org.mtransit.android.ad.banner

import android.content.res.Configuration
import android.os.Build
import android.view.ViewGroup
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TaskUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.isVisibleOnce
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.secToMs
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

// Anchored adaptive banner
@Singleton
class BannerAdManager @Inject constructor(
    private val globalAdManager: GlobalAdManager,
    private val crashReporter: CrashReporter,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${BannerAdManager::class.java.simpleName}"

        private const val LOADED_UNKNOWN = -1L

        private const val LOAD_ON_SCREEN_RESUMED_MIN_DURATION_SEC = 1
    }

    override fun getLogTag() = LOG_TAG

    private val adBannerLoadedLastInMs = AtomicLong(LOADED_UNKNOWN)

    val adBannerLoaded: Boolean
        get() = this.adBannerLoadedLastInMs.get() > 0L

    private var setupBannerAdTask: SetupBannerAdTask? = null

    fun setAdBannerLoaded(loaded: Boolean?) {
        this.adBannerLoadedLastInMs.set(
            when (loaded) {
                true -> TimeUtils.currentTimeMillis()
                false -> 0L
                else -> LOADED_UNKNOWN
            }
        )
    }

    fun onResumeScreen(activity: IAdScreenActivity) {
        refreshBannerAdStatus(activity, force = true)
    }

    @JvmOverloads
    fun refreshBannerAdStatus(activity: IAdScreenActivity, force: Boolean = false) {
        if (this.globalAdManager.isShowingAds() // showing ads across the app
            && activity.currentAdFragment?.hasAds() == false // this specific screen doesn't include ads already
        ) {
            if (!this.adBannerLoaded || force) { // IF ad was not loaded DO
                setupBannerAd(activity, force)
            }
        } else { // ELSE IF not showing ads DO
            if (this.adBannerLoaded) { // IF ad was loaded DO
                hideBannerAd(activity)
                pauseAd(activity)
            }
        }
    }

    fun adaptToScreenSize(activity: IAdScreenActivity, configuration: Configuration? = activity.context?.resources?.configuration) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        if (!this.globalAdManager.isShowingAds()) {
            return
        }
        if (activity.currentAdFragment?.hasAds() == true) {
            return
        }
        if (isEnoughSpaceForBanner(configuration)) {
            if (this.adBannerLoaded) {
                resumeAd(activity)
                showBannerAd(activity)
            }
        } else {
            hideBannerAd(activity)
            pauseAd(activity)
        }
    }

    fun isEnoughSpaceForBanner(configuration: Configuration?): Boolean {
        if (FeatureFlags.F_NAVIGATION) {
            return true // always show
        }
        if (configuration == null) {
            return false
        }
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true
        }
        val sizeMask = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val smallScreen = sizeMask == Configuration.SCREENLAYOUT_SIZE_SMALL || sizeMask == Configuration.SCREENLAYOUT_SIZE_NORMAL
        return !smallScreen
    }

    private fun setupBannerAd(activity: IAdScreenActivity, force: Boolean) {
        MTLog.d(this, "setupAd($force)")
        if (!AdConstants.AD_ENABLED) {
            MTLog.d(this, "setupAd() > SKIP (AD not enabled)")
            return
        }
        if (!this.globalAdManager.isShowingAds()) {
            MTLog.d(this, "setupAd() > SKIP (not showing ads)")
            return
        }
        if (force
            && setupBannerAdTask != null // task to cancel or not cancel
            && this.adBannerLoadedLastInMs.get() != LOADED_UNKNOWN // state unknown
        ) {
            MTLog.d(this, "setupAd() > should we cancel?")
            val minDurationMs = LOAD_ON_SCREEN_RESUMED_MIN_DURATION_SEC.secToMs()
            if (this.adBannerLoadedLastInMs.get() + minDurationMs < TimeUtils.currentTimeMillis()) { // force refresh if ad loaded only
                MTLog.d(this, "setupAd() > cancelling previous setup ad task...")
                TaskUtils.cancelQuietly(setupBannerAdTask, true)
                setupBannerAdTask = null
            }
        }
        if (setupBannerAdTask == null) {
            MTLog.d(this, "setupAd() > starting setup ad task...")
            setupBannerAdTask = SetupBannerAdTask(this.globalAdManager, this, this.crashReporter, activity)
            TaskUtils.execute(setupBannerAdTask)
            setAdBannerLoaded(null) // loading
        }
        MTLog.d(this, "setupAd() > DONE")
    }

    private fun showBannerAd(activity: IAdScreenActivity) {
        val adLayout = getAdLayout(activity)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adView?.isVisibleOnce = true
            adLayout.isVisibleOnce = true
        }
    }

    fun hideBannerAd(activity: IAdScreenActivity) {
        val adLayout = getAdLayout(activity)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adLayout.isVisibleOnce = false
            adView?.isVisibleOnce = false
        }
    }

    fun resumeAd(activity: IAdScreenActivity) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        val adLayout = getAdLayout(activity)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adView?.resume()
        }
    }

    fun pauseAd(activity: IAdScreenActivity) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        val adLayout = getAdLayout(activity)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adView?.pause()
        }
    }

    fun getAdLayout(activity: IAdScreenActivity): ViewGroup? =
        activity.requireActivity().findViewById(R.id.ad_layout)

    @Suppress("unused")
    fun getAdView(activity: IAdScreenActivity) = getAdLayout(activity)?.let { getAdView(it) }

    fun getAdView(adLayout: ViewGroup): AdView? =
        adLayout.findViewById(R.id.ad)

    fun destroyAd(activity: IAdScreenActivity) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
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
        setAdBannerLoaded(false)
        TaskUtils.cancelQuietly(setupBannerAdTask, true)
        setupBannerAdTask = null
    }

    fun getBannerHeightInPx(activity: IAdScreenActivity?): Int {
        if (!this.adBannerLoaded) {
            return 0 // ad not loaded
        }
        if (!this.globalAdManager.isShowingAds()) {
            return 0 // not showing ads (0 agency installed, paying user...)
        }
        if (activity == null) {
            return 0 // can't measure w/o context
        }
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
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }
}