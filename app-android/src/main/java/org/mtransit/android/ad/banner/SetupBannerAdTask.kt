package org.mtransit.android.ad.banner

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdView
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.util.UIFeatureFlags
import java.lang.ref.WeakReference

@Suppress("DEPRECATION")
class SetupBannerAdTask(
    private val globalAdManager: GlobalAdManager,
    private val bannerAdManager: BannerAdManager,
    private val crashReporter: CrashReporter,
    private val activityWR: WeakReference<IAdScreenActivity>,
) : org.mtransit.android.commons.task.MTCancellableAsyncTask<Void?, Void?, Boolean?>() {

    constructor(
        globalAdManager: GlobalAdManager,
        bannerAdManager: BannerAdManager,
        crashReporter: CrashReporter,
        activity: IAdScreenActivity,
    ) : this(
        globalAdManager,
        bannerAdManager,
        crashReporter,
        WeakReference<IAdScreenActivity>(activity),
    )

    companion object {
        private val LOG_TAG: String = "${AdManager.LOG_TAG}>${SetupBannerAdTask::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @WorkerThread
    override fun doInBackgroundNotCancelledMT(vararg params: Void?): Boolean {
        if (!AdConstants.AD_ENABLED) return false
        return !isCancelled && this.globalAdManager.isShowingAds() // TODO can be called from any thread
    }

    @MainThread
    override fun onPostExecuteNotCancelledMT(result: Boolean?) {
        val activity = this.activityWR.get() ?: return
        val isShowingAds = result == true
        if (isShowingAds && !isCancelled) { // show ads
            val adLayout = this.bannerAdManager.getAdLayout(activity)
            if (adLayout != null) {
                var adView = this.bannerAdManager.getAdView(adLayout)
                if (adView == null) {
                    adView = makeNewAdView(activity, adLayout)
                }
                adView.loadAd(AdManager.getAdRequest(activity, collapsible = UIFeatureFlags.F_ADS_BANNER_COLLAPSIBLE))
            }
        } else { // hide ads
            this.bannerAdManager.hideBannerAd(activity)
        }
    }

    private fun makeNewAdView(activity: IAdScreenActivity, adLayout: ViewGroup): AdView {
        val adView = AdView(activity.requireActivity()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isVisible = false
            id = R.id.ad
            adUnitId = activity.requireContext().getString(R.string.google_ads_banner_ad_unit_id)
        }
        adLayout.removeAllViews()
        adLayout.addView(adView)

        adView.apply {
            setAdSize(bannerAdManager.getAdSize(activity)) // ad size can only be set once
            adListener = BannerAdListener(bannerAdManager, crashReporter, activity, adView)
        }
        return adView
    }
}