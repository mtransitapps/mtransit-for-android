package org.mtransit.android.ad.banner

import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import org.mtransit.android.ui.setNavigationBarColor
import org.mtransit.android.ui.view.common.IActivity
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TaskUtils
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.setUpEdgeToEdgeBottom
import org.mtransit.commons.FeatureFlags

class BannerAdManager @Inject constructor(
    private val globalAdManager: GlobalAdManager,
    private val crashReporter: CrashReporter,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${BannerAdManager::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    val adBannerLoaded = AtomicBoolean(false)

    private var setupBannerAdTask: SetupBannerAdTask? = null

    fun refreshBannerAdStatus(activity: IActivity) {
        if (this.globalAdManager.isShowingAds()) {
            if (!this.adBannerLoaded.get()) { // IF ad was not loaded DO
                setupBannerAd(activity, force = false)
            }
        } else { // ELSE IF not showing ads DO
            if (this.adBannerLoaded.get()) { // IF ad was loaded DO
                hideBannerAd(activity)
                pauseAd(activity)
            }
        }
    }

    fun adaptToScreenSize(activity: IActivity, configuration: Configuration?) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        if (!this.globalAdManager.isShowingAds()) {
            return
        }
        if (isEnoughSpaceForBanner(configuration)) {
            if (this.adBannerLoaded.get()) {
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


    fun setupBannerAd(activity: IActivity, force: Boolean) {
        MTLog.d(this, "setupAd(%s)", force)
        if (!AdConstants.AD_ENABLED) {
            MTLog.d(this, "setupAd() > SKIP (AD not enabled)")
            return
        }
        if (!this.globalAdManager.isShowingAds()) {
            MTLog.d(this, "setupAd() > SKIP (not showing ads)")
            return
        }
        if (force) {
            MTLog.d(this, "setupAd() > should we cancel?")
            if (this.adBannerLoaded.get()) { // force refresh if ad loaded only
                MTLog.d(this, "setupAd() > cancelling previous setup ad task...")
                TaskUtils.cancelQuietly(setupBannerAdTask, true)
                setupBannerAdTask = null
            }
        }
        if (setupBannerAdTask == null) {
            MTLog.d(this, "setupAd() > starting setup ad task...")
            setupBannerAdTask = SetupBannerAdTask(this.globalAdManager, this, this.crashReporter, activity)
            TaskUtils.execute(setupBannerAdTask)
        }
        MTLog.d(this, "setupAd() > DONE")
    }

    fun showBannerAd(activity: IActivity) {
        val adLayout = getAdLayout(activity)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            if (adView != null && adView.visibility != View.VISIBLE) {
                adView.visibility = View.VISIBLE
            }
            if (adLayout.visibility != View.VISIBLE) {
                adLayout.visibility = View.VISIBLE
            }
            adLayout.setUpEdgeToEdgeBottom()
            activity.getActivity().setNavigationBarColor(true)
        }
    }

    fun hideBannerAd(activity: IActivity) {
        val adLayout = getAdLayout(activity)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            if (adLayout.isVisible != false) {
                adLayout.isVisible = false
            }
            if (adView?.isVisible != false) {
                adView?.isVisible = false
            }
            activity.getActivity().setNavigationBarColor(true)
        }
    }

    fun resumeAd(activity: IActivity) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        val adLayout = getAdLayout(activity)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adView?.resume()
        }
    }

    fun pauseAd(activity: IActivity) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        val adLayout = getAdLayout(activity)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adView?.pause()
        }
    }

    fun getAdLayout(activity: IActivity): ViewGroup? =
        activity.requireActivity().findViewById(R.id.ad_layout)

    @Suppress("unused")
    fun getAdView(activity: IActivity) = getAdLayout(activity)?.let { getAdView(it) }

    fun getAdView(adLayout: ViewGroup): AdView? =
        adLayout.findViewById(R.id.ad)

    fun destroyAd(activity: IActivity) {
        MTLog.d(this, "setupAd()")
        if (!AdConstants.AD_ENABLED) {
            MTLog.d(this, "setupAd() > SKIP (AD not enabled)")
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
        adBannerLoaded.set(false)
        TaskUtils.cancelQuietly(setupBannerAdTask, true)
        setupBannerAdTask = null
    }

    fun getBannerHeightInPx(activity: IActivity?): Int {
        if (!this.adBannerLoaded.get()) {
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

    @Suppress("DEPRECATION")
    fun getAdSize(activity: IActivity): AdSize {
        val display = activity.requireActivity().windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        val adLayout = getAdLayout(activity)
        val adWidthPixels = adLayout?.width?.toFloat() ?: 0f

        val widthPixels = outMetrics.widthPixels.toFloat()

        val adWidth = (if (adWidthPixels == 0f) widthPixels else adWidthPixels) / density

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity.requireContext(), adWidth.toInt())
    }


}