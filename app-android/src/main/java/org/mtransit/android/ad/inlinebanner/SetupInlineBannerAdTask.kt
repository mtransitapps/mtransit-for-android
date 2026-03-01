package org.mtransit.android.ad.inlinebanner

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdView
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.ui.view.common.IFragment
import java.lang.ref.WeakReference

@Suppress("DEPRECATION")
class SetupInlineBannerAdTask(
    private val globalAdManager: GlobalAdManager,
    private val inlineBannerAdManager: InlineBannerAdManager,
    private val crashReporter: CrashReporter,
    private val fragmentWR: WeakReference<IFragment>,
) : org.mtransit.android.commons.task.MTCancellableAsyncTask<Void?, Void?, Boolean?>() {

    constructor(
        globalAdManager: GlobalAdManager,
        inlineBannerAdManager: InlineBannerAdManager,
        crashReporter: CrashReporter,
        fragment: IFragment,
    ) : this(
        globalAdManager,
        inlineBannerAdManager,
        crashReporter,
        WeakReference<IFragment>(fragment),
    )

    companion object {
        private val LOG_TAG: String = "${AdManager.LOG_TAG}>${SetupInlineBannerAdTask::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @WorkerThread
    override fun doInBackgroundNotCancelledMT(vararg params: Void?): Boolean {
        if (!AdConstants.AD_ENABLED) {
            return false
        }
        return !isCancelled && this.globalAdManager.isShowingAds() // TODO can be called from any thread
    }

    @MainThread
    override fun onPostExecuteNotCancelledMT(result: Boolean?) {
        val isShowingAds = result == true
        val activity = this.fragmentWR.get()
        if (activity == null) {
            logAdsD(this, "onPostExecuteNotCancelledMT() > SKIP (no activity)")
            return
        }
        if (isShowingAds && !isCancelled) { // show ads
            val adLayout = this.inlineBannerAdManager.getAdLayout(activity)
            if (adLayout != null) {
                var adView = this.inlineBannerAdManager.getAdView(adLayout)
                if (adView == null) {
                    adView = makeNewAdView(activity, adLayout)
                }
                adView.loadAd(AdManager.getAdRequest(activity))
            }
        } else { // hide ads
            this.inlineBannerAdManager.hideBannerAd(activity)
        }
    }

    private fun makeNewAdView(fragment: IFragment, adLayout: ViewGroup): AdView {
        val adView = AdView(fragment.requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isVisible = false
            id = R.id.inline_banner_ad
            adUnitId = fragment.requireContext().getString(R.string.google_ads_banner_inline_ad_unit_id)
        }
        adLayout.removeAllViews()
        adLayout.addView(adView)

        adView.apply {
            setAdSize(inlineBannerAdManager.getAdSize(fragment)) // ad size can only be set once
            adListener = InlineBannerAdListener(inlineBannerAdManager, crashReporter, fragment, adView)
        }
        return adView
    }
}