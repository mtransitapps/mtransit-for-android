package org.mtransit.android.ad

import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import org.mtransit.android.ad.banner.BannerAdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.ui.view.common.IActivity
import java.lang.ref.WeakReference

internal class OnInitializationCompleteListener(
    private val bannerAdManager: BannerAdManager,
    private val activityWR: WeakReference<IActivity>,
    private val adScreenFragmentWR: WeakReference<IAdScreenFragment>,
) : OnInitializationCompleteListener, Loggable {

    constructor(
        bannerAdManager: BannerAdManager,
        activity: IActivity?,
        adScreenFragment: IAdScreenFragment?,
    ) : this(
        bannerAdManager,
        WeakReference<IActivity>(activity),
        WeakReference<IAdScreenFragment>(adScreenFragment),
    )

    companion object {
        private val LOG_TAG = AdManager::class.java.getSimpleName() + ">" + OnInitializationCompleteListener::class.java.getSimpleName()
    }

    override fun getLogTag() = LOG_TAG

    override fun onInitializationComplete(initializationStatus: InitializationStatus) {
        val activity = this.activityWR.get()
        if (activity != null) {
            this.bannerAdManager.refreshBannerAdStatus(activity, this.adScreenFragmentWR.get(), false)
        }
        val statusMap = initializationStatus.adapterStatusMap
        for (adapterClass in statusMap.keys) {
            val status = statusMap[adapterClass]
            MTLog.d(this, "Adapter name: $adapterClass, Description: ${status?.description}, Latency: ${status?.latency}")
        }
    }
}
