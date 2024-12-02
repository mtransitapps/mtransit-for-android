package org.mtransit.android.ad

import androidx.annotation.WorkerThread
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import org.mtransit.android.R
import org.mtransit.android.ad.banner.BannerAdManager
import org.mtransit.android.ui.view.common.IActivity
import java.lang.ref.WeakReference
import java.util.ArrayList

@Suppress("DEPRECATION")
internal class InitTask(
    private val globalAdManager: GlobalAdManager,
    private val bannerAdManager: BannerAdManager,
    private val activityWR: WeakReference<IActivity>,
    private val adScreenFragmentWR: WeakReference<IAdScreenFragment>,
) : org.mtransit.android.commons.task.MTCancellableAsyncTask<Void?, Void?, Boolean?>() {

    constructor(
        globalAdManager: GlobalAdManager,
        bannerAdManager: BannerAdManager,
        activity: IActivity?,
        adScreenFragment: IAdScreenFragment?,
    ) : this(
        globalAdManager,
        bannerAdManager,
        WeakReference(activity),
        WeakReference(adScreenFragment)
    )

    override fun getLogTag() = LOG_TAG

    @WorkerThread
    override fun doInBackgroundNotCancelledMT(vararg params: Void?): Boolean? {
        if (!AdConstants.AD_ENABLED) {
            return false
        }
        val activity = this.activityWR.get()
        if (activity == null) {
            return false
        }
        if (this.globalAdManager.getAndSetInitialized(true)) {
            return false
        }
        initOnBackgroundThread(activity)
        return true
    }

    @WorkerThread
    private fun initOnBackgroundThread(activity: IActivity) {
        if (AdConstants.DEBUG) {
            val testDeviceIds: MutableList<String?> = ArrayList<String?>()
            testDeviceIds.add(AdRequest.DEVICE_ID_EMULATOR)
            testDeviceIds.addAll(listOf<String?>(*activity.requireContext().resources.getStringArray(R.array.google_ads_test_devices_ids)))
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
            )
        }
        // https://developers.google.com/admob/android/quick-start#initialize_the_mobile_ads_sdk
        MobileAds.initialize( // doing I/O #StrictMode
            activity.requireActivity(),  // some adapters require activity
            OnInitializationCompleteListener(this.bannerAdManager, activity, this.adScreenFragmentWR.get())
        )
    }

    companion object {
        private val LOG_TAG = AdManager::class.java.getSimpleName() + ">" + InitTask::class.java.getSimpleName()
    }
}
