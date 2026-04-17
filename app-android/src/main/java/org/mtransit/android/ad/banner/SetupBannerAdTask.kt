package org.mtransit.android.ad.banner

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
import org.mtransit.android.ad.IAdScreenActivity
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.util.UIFeatureFlags
import java.lang.ref.WeakReference

@Suppress("DEPRECATION")
class SetupBannerAdTask(
    private val globalAdManager: GlobalAdManager,
    private val bannerAdManager: BannerAdManager,
    private val crashReporter: CrashReporter,
    private val remoteConfigProvider: RemoteConfigProvider,
    private val activityWR: WeakReference<IAdScreenActivity>,
) : org.mtransit.android.commons.task.MTCancellableAsyncTask<Void?, Void?, Boolean?>() {

    constructor(
        globalAdManager: GlobalAdManager,
        bannerAdManager: BannerAdManager,
        crashReporter: CrashReporter,
        remoteConfigProvider: RemoteConfigProvider,
        activity: IAdScreenActivity,
    ) : this(
        globalAdManager,
        bannerAdManager,
        crashReporter,
        remoteConfigProvider,
        WeakReference(activity),
    )

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${SetupBannerAdTask::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    @WorkerThread
    override fun doInBackgroundNotCancelledMT(vararg params: Void?): Boolean {
        if (!AdConstants.AD_ENABLED) return false
        return this.globalAdManager.isShowingAds() // TODO can be called from any thread
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
                adView.loadAd(
                    adRequest = AdManager.getBannerAdRequest(
                        adUnitId = activity.requireActivity().getString(adUnitStringResId),
                        adSize = bannerAdManager.getAdSize(activity),
                        collapsible = UIFeatureFlags.F_ADS_BANNER_COLLAPSIBLE
                    ),
                    adLoadCallback = BannerAdListener(bannerAdManager, crashReporter, remoteConfigProvider, activity)
                )
            }
        } else if (!isShowingAds) { // hide ads
            this.bannerAdManager.hideBannerAd(activity)
        }
    }

    @get:StringRes
    private val adUnitStringResId: Int
        get() = when {
            bannerAdManager.loadOnScreenResume -> R.string.google_ads_banner_manual_refresh_ad_unit_id
            else -> R.string.google_ads_banner_ad_unit_id
        }

    private fun makeNewAdView(activity: IAdScreenActivity, adLayout: ViewGroup) =
        AdView(activity.requireActivity()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isVisible = false
            id = R.id.ad
        }.also {
            adLayout.removeAllViews()
            adLayout.addView(it)
        }
}