package org.mtransit.android.ad.rewarded

import android.content.Context
import androidx.annotation.AnyThread
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.google.android.gms.ads.rewarded.RewardedAd
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd #gmaNextGen
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdConstants.logAdsD
import org.mtransit.android.ad.AdManager
import org.mtransit.android.ad.GlobalAdManager
import org.mtransit.android.ad.IAdManager.RewardedAdListener
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.MTLog
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.view.common.IActivity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardedAdManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val globalAdManager: GlobalAdManager,
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val demoModeManager: DemoModeManager,
    private val crashReporter: CrashReporter,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${RewardedAdManager::class.java.simpleName}"
    }

    override fun getLogTag() = LOG_TAG

    var rewardedAdListener: RewardedAdListener?
        get() = rewardedAdListenerWR?.get()
        set(value) {
            if (value == null) {
                rewardedAdListenerWR?.let {
                    logAdsD(this, "setRewardedAdListener() > clearing ${it.get()}...")
                    it.clear()
                }
                rewardedAdListenerWR = null
            } else {
                logAdsD(this, "setRewardedAdListener() > setting $value...")
                rewardedAdListenerWR = WeakReference(value)
            }
        }

    private var rewardedAdListenerWR: WeakReference<RewardedAdListener?>? = null

    private var rewardedAd: RewardedAd? = null
    private var rewardedAdActivityHashCode: Int? = null

    private suspend fun loadRewardedAdForActivity(activity: IActivity) = withContext(Dispatchers.Main) {
        val activityHashCode = activity.requireActivity().hashCode()
        if (rewardedAd != null && (rewardedAdActivityHashCode == null || rewardedAdActivityHashCode == activityHashCode)) {
            logAdsD(this@RewardedAdManager, "loadRewardedAdForActivity() > NOT Loading rewarded ad for ${activity::class.java.simpleName}...")
            return@withContext
        }
        rewardedAdActivityHashCode = activityHashCode
        logAdsD(this@RewardedAdManager, "loadRewardedAdForActivity() > Loading rewarded ad for ${activity::class.java.simpleName}...")
        RewardedAd.load( // Must be called on the main UI thread
            appContext,
            appContext.getString(adUnitStringResId),
            AdManager.getAdRequest(
                adUnitId = appContext.getString(adUnitStringResId)
            ),
            RewardedAdLoadCallback(this@RewardedAdManager, crashReporter)
        )
        withContext(Dispatchers.IO) {
            val loadCounts = defaultPrefRepository.pref.getInt(
                DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS_DEFAULT
            )
            defaultPrefRepository.pref.edit { putInt(DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS, loadCounts + 1) }
        }
    }

    @get:StringRes
    private val adUnitStringResId: Int get() = R.string.google_ads_rewarded_ad_unit_id

    fun setRewardedAd(rewardedAd: RewardedAd?) {
        if (this.rewardedAdActivityHashCode == null) {
            logAdsD(this, "setRewardedAd() > SKIP rewarded ad (no activity) $rewardedAd.")
            return // too late
        }
        this.rewardedAd = rewardedAd
    }

    fun linkRewardedAd(activity: IActivity) {
        val theActivity = activity.requireActivity()
        if (this.rewardedAdActivityHashCode != null && this.rewardedAdActivityHashCode == theActivity.hashCode()) {
            logAdsD(this, "linkRewardedAd() > SKIP (same activity)")
            return // same activity
        }
        this.rewardedAd = null
        this.rewardedAdActivityHashCode = null
    }

    fun unlinkRewardedAd(activity: IActivity) {
        val theActivity = activity.requireActivity()
        if (this.rewardedAdActivityHashCode != null && this.rewardedAdActivityHashCode == theActivity.hashCode()) {
            this.rewardedAd = null
            this.rewardedAdActivityHashCode = null
        } else {
            logAdsD(this, "unlinkRewardedAd() > SKIP (not this activity)")
        }
    }

    suspend fun refreshRewardedAdStatus(activity: IActivity) = withContext(Dispatchers.IO) {
        if (!AdConstants.AD_ENABLED) return@withContext
        val isNotPayingUser = globalAdManager.isShowingAds()
        if (!isNotPayingUser) {
            logAdsD(this@RewardedAdManager, "refreshRewardedAdStatus() > SKIP (paying user or unknown)")
            return@withContext
        }
        val listener = rewardedAdListener
        if (listener == null) {
            logAdsD(this@RewardedAdManager, "refreshRewardedAdStatus() > SKIP (unknown screen)")
            return@withContext
        }
        if (listener.skipRewardedAd()) {
            logAdsD(this@RewardedAdManager, "refreshRewardedAdStatus() > SKIP (not in this screen)")
            return@withContext
        }
        logAdsD(this@RewardedAdManager, "refreshRewardedAdStatus() > Load if necessary...")
        loadRewardedAdForActivity(activity)
    }

    @AnyThread
    fun isRewardedAdAvailableToShow(): Boolean {
        if (!AdConstants.AD_ENABLED) return false
        if (this.demoModeManager.enabled) return false
        if (this.rewardedAd == null) return false // do not trigger creation + loading
        return true
    }

    fun showRewardedAd(activity: IActivity): Boolean {
        if (!AdConstants.AD_ENABLED) return false
        if (this.rewardedAd == null) return false // do not trigger creation + loading

        val theActivity = activity.requireActivity()
        logAdsD(this, "showRewardedAd() > Showing rewarded ad for ${theActivity::class.java.simpleName}...")
        // this.rewardedAd?.adEventCallback = RewardedAdFullScreenContentCallback(this, this.crashReporter, activity) #gmaNextGen
        this.rewardedAd?.fullScreenContentCallback = RewardedAdFullScreenContentCallback(this, this.crashReporter, activity)
        this.rewardedAd?.show(theActivity, RewardedAdOnUserEarnedRewardListener(this.globalAdManager, activity))
        val showCounts = this.defaultPrefRepository.pref.getInt(
            DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS_DEFAULT
        )
        this.defaultPrefRepository.pref.edit { putInt(DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, showCounts + 1) }
        return true
    }
}