package org.mtransit.android.ad.inlinebanner

import android.os.Build
import android.view.ViewGroup
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.ad.IAdScreenFragment
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TaskUtils
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.IFragment
import org.mtransit.android.ui.view.common.IViewFinder
import org.mtransit.android.ui.view.common.isVisibleOnce
import org.mtransit.android.util.UIFeatureFlags
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InlineBannerAdManager @Inject constructor(
    private val globalAdManager: GlobalAdManager,
    private val crashReporter: CrashReporter,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${InlineBannerAdManager::class.java.simpleName}"

        // https://developers.google.com/admob/android/banner/fixed-size
        private const val USE_MEDIUM_RECTANGLE = true
    }

    override fun getLogTag() = LOG_TAG

    private val inlineAdBannerLoaded = mutableMapOf<Int, AtomicBoolean>()

    private val setupInlineBannerAdTasks = mutableMapOf<Int, SetupInlineBannerAdTask>()

    @JvmOverloads
    fun refreshBannerAdStatus(fragment: IFragment, adScreenFragment: IAdScreenFragment?, force: Boolean = false) {
        if (this.globalAdManager.isShowingAds() // showing ads across the app
            && (UIFeatureFlags.F_CUSTOM_ADS_IN_NEWS && adScreenFragment?.hasAds() == true) // this specific screen does include ads already
        ) {
            if (!isAdBannerLoaded(fragment) || force) { // IF ad was not loaded DO
                setupBannerAd(fragment, force)
            }
        } else { // ELSE IF not showing ads DO
            if (isAdBannerLoaded(fragment)) { // IF ad was loaded DO
                hideBannerAd(fragment)
                pauseAd(fragment)
            }
        }
    }

    fun isAdBannerLoaded(fragment: IFragment): Boolean {
        return this.inlineAdBannerLoaded[fragment.hashCode()]?.get() == true
    }

    fun setAdBannerLoaded(fragment: IFragment, loaded: Boolean?) {
        loaded?.let {
            this.inlineAdBannerLoaded[fragment.hashCode()] = AtomicBoolean(it)
        } ?: run {
            this.inlineAdBannerLoaded.remove(fragment.hashCode())
        }
    }

    fun getSetupInlineBannerAdTask(fragment: IFragment): SetupInlineBannerAdTask? {
        return this.setupInlineBannerAdTasks[fragment.hashCode()]
    }

    fun setSetupInlineBannerAdTask(fragment: IFragment, task: SetupInlineBannerAdTask?) {
        task?.let {
            this.setupInlineBannerAdTasks[fragment.hashCode()] = it
        } ?: run {
            this.setupInlineBannerAdTasks.remove(fragment.hashCode())
        }
    }

    private fun setupBannerAd(fragment: IFragment, force: Boolean) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        if (!UIFeatureFlags.F_CUSTOM_ADS_IN_NEWS) {
            return
        }
        if (!this.globalAdManager.isShowingAds()) {
            MTLog.d(this, "setupBannerAd() > SKIP (not showing ads)")
            return
        }
        if (force) {
            if (isAdBannerLoaded(fragment)) { // force refresh if ad loaded only
                TaskUtils.cancelQuietly(getSetupInlineBannerAdTask(fragment), true)
                setSetupInlineBannerAdTask(fragment, null)
            }
        }
        if (getSetupInlineBannerAdTask(fragment) == null) {
            setSetupInlineBannerAdTask(fragment, SetupInlineBannerAdTask(this.globalAdManager, this, this.crashReporter, fragment))
            TaskUtils.execute(getSetupInlineBannerAdTask(fragment))
        }
    }

    fun getAdLayout(viewFinder: IViewFinder): ViewGroup? =
        viewFinder.findViewById(R.id.inline_ad_layout)

    @Suppress("unused")
    fun getAdView(viewFinder: IViewFinder) = getAdLayout(viewFinder)?.let { getAdView(it) }

    fun getAdView(adLayout: ViewGroup): AdView? =
        adLayout.findViewById(R.id.inline_banner_ad)

    fun onResume(fragment: IFragment) {
        resumeAd(fragment)
    }

    fun adaptToScreenSize(fragment: IFragment) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        if (!UIFeatureFlags.F_CUSTOM_ADS_IN_NEWS) {
            return
        }
        if (!this.globalAdManager.isShowingAds()) {
            return
        }
        if (isAdBannerLoaded(fragment)) {
            showBannerAd(fragment)
        } else {
            hideBannerAd(fragment)
        }
    }

    private fun resumeAd(viewFinder: IViewFinder) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        if (!UIFeatureFlags.F_CUSTOM_ADS_IN_NEWS) {
            return
        }
        val adLayout = getAdLayout(viewFinder)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adView?.resume()
        }
    }

    fun onPause(viewFinder: IViewFinder) {
        pauseAd(viewFinder)
    }

    private fun pauseAd(viewFinder: IViewFinder) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        if (!UIFeatureFlags.F_CUSTOM_ADS_IN_NEWS) {
            return
        }
        val adLayout = getAdLayout(viewFinder)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adView?.pause()
        }
    }

    private fun showBannerAd(viewFinder: IViewFinder) {
        val adLayout = getAdLayout(viewFinder)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adView?.isVisibleOnce = true
            adLayout.isVisibleOnce = true
        }
    }

    internal fun hideBannerAd(viewFinder: IViewFinder) {
        val adLayout = getAdLayout(viewFinder)
        if (adLayout != null) {
            val adView = getAdView(adLayout)
            adLayout.isVisibleOnce = false
            adView?.isVisibleOnce = false
        }
    }

    internal fun getAdSize(fragment: IFragment): AdSize = with(fragment.requireActivity()) {
        if (USE_MEDIUM_RECTANGLE) {
            return@with AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(this, AdSize.MEDIUM_RECTANGLE.width) // width = 300
        }
        val padding = resources.getDimensionPixelSize(R.dimen.ad_banner_inline_horizontal_margin)
        val displayMetrics = resources.displayMetrics
        val screenWidth =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                this.windowManager.currentWindowMetrics.bounds.width()
            } else {
                displayMetrics.widthPixels
            }
        val density = displayMetrics.density
        val adWidth = screenWidth - padding - padding
        val adWidthDp = (adWidth / density).toInt()
        // https://developers.google.com/admob/android/banner/fixed-size
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(this, adWidthDp)
    }

    fun destroyAd(fragment: IFragment) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        if (!UIFeatureFlags.F_CUSTOM_ADS_IN_NEWS) {
            return
        }
        val adLayout = getAdLayout(fragment)
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
        setAdBannerLoaded(fragment, null)
        TaskUtils.cancelQuietly(getSetupInlineBannerAdTask(fragment), true)
        setSetupInlineBannerAdTask(fragment, null)
    }
}