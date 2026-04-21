package org.mtransit.android.ad.inlinebanner

import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.core.view.isVisible
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
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
        globalAdManager = globalAdManager,
        inlineBannerAdManager = inlineBannerAdManager,
        crashReporter = crashReporter,
        fragmentWR = WeakReference(fragment),
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${SetupInlineBannerAdTask::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @WorkerThread
    override fun doInBackgroundNotCancelledMT(vararg params: Void?): Boolean {
        if (!AdConstants.AD_ENABLED) return false
        return !isCancelled && this.globalAdManager.isShowingAds() // TODO can be called from any thread
    }

    @MainThread
    override fun onPostExecuteNotCancelledMT(result: Boolean?) {
        val fragment = this.fragmentWR.get() ?: return
        val isShowingAds = result == true
        if (isShowingAds && !isCancelled) { // show ads
            this.inlineBannerAdManager.getAdLayout(fragment)?.let { adLayout ->
                val adView = this.inlineBannerAdManager.getAdView(adLayout)
                    ?: makeNewAdView(fragment, adLayout)
                adView.loadAd(
                    adRequest = AdManager.getBannerAdRequest(
                        adUnitId = fragment.requireActivity().getString(adUnitStringResId),
                        adSize = inlineBannerAdManager.getAdSize(fragment),
                    ),
                    adLoadCallback = InlineBannerAdListener(inlineBannerAdManager, crashReporter, fragment)
                )
            }
        } else { // hide ads
            this.inlineBannerAdManager.hideBannerAd(fragment)
        }
    }

    @get:StringRes
    private val adUnitStringResId: Int get() = R.string.google_ads_banner_inline_ad_unit_id

    private fun makeNewAdView(fragment: IFragment, adLayout: ViewGroup) =
        AdView(fragment.requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isVisible = false
            id = R.id.inline_banner_ad
        }.also {
            adLayout.removeAllViews()
            adLayout.addView(it)
        }
}