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
import org.mtransit.android.commons.MTLog
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.IActivity
import java.lang.ref.WeakReference

@Suppress("deprecation")
class SetupBannerAdTask(
    private val globalAdManager: GlobalAdManager,
    private val bannerAdManager: BannerAdManager,
    private val crashReporter: CrashReporter,
    private val activityWR: WeakReference<IActivity>,
) : org.mtransit.android.commons.task.MTCancellableAsyncTask<Void?, Void?, Boolean?>() {

    constructor(
        globalAdManager: GlobalAdManager,
        bannerAdManager: BannerAdManager,
        crashReporter: CrashReporter,
        activity: IActivity,
    ) : this(
        globalAdManager,
        bannerAdManager,
        crashReporter,
        WeakReference<IActivity>(activity),
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${SetupBannerAdTask::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @WorkerThread
    override fun doInBackgroundNotCancelledMT(vararg params: Void?): Boolean? {
        MTLog.d(this, "doInBackgroundNotCancelledMT()")
        if (!AdConstants.AD_ENABLED) {
            return false
        }
        return !isCancelled && this.globalAdManager.isShowingAds() // TODO can be called from any thread
    }

    @MainThread
    override fun onPostExecuteNotCancelledMT(isShowingAds: Boolean?) {
        MTLog.d(this, "onPostExecuteNotCancelledMT(%s)", isShowingAds)
        val activity = this.activityWR.get()
        // Activity activity = iActivity == null ? null : iActivity.getActivity();
        MTLog.d(this, "onPostExecuteNotCancelledMT() > activity: %s", activity)
        if (activity == null) {
            MTLog.d(this, "onPostExecuteNotCancelledMT() > SKIP (no activity)")
            return
        }
        MTLog.d(this, "onPostExecuteNotCancelledMT() > isCancelled(): %s", isCancelled)
        if (isShowingAds == true && !isCancelled) { // show ads
            MTLog.d(this, "onPostExecuteNotCancelledMT() > showing ads...")
            val adLayout = this.bannerAdManager.getAdLayout(activity)
            if (adLayout != null) {
                MTLog.d(this, "onPostExecuteNotCancelledMT() > adLayout found...")
                var adView = this.bannerAdManager.getAdView(adLayout)
                if (adView == null) {
                    adView = makeNewAdView(activity, adLayout)
                    // setupAdView(adView, activity);
                } else {
                    MTLog.d(this, "onPostExecuteNotCancelledMT() > adView found...")
                }
                MTLog.d(this, "onPostExecuteNotCancelledMT() > setAdListener()...")

                MTLog.d(this, "onPostExecuteNotCancelledMT() > loadAd()..")
                adView.loadAd(AdManager.getAdRequest(activity))
            } else {
                MTLog.d(this, "onPostExecuteNotCancelledMT() > SKIP (no adLayout)")
            }
        } else { // hide ads
            MTLog.d(this, "onPostExecuteNotCancelledMT() > hide ads...")
            this.bannerAdManager.hideBannerAd(activity)
        }
    }

    private fun makeNewAdView(activity: IActivity, adLayout: ViewGroup): AdView {
        MTLog.d(this, "makeNewAdView()")
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
            adListener = BannerAdLister(bannerAdManager, crashReporter, activity, adView)
        }
        return adView
    }
}