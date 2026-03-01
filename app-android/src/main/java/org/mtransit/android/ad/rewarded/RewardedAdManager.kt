package org.mtransit.android.ad.rewarded

import com.google.android.gms.ads.rewarded.RewardedAd
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
    private val globalAdManager: GlobalAdManager,
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val demoModeManager: DemoModeManager,
    private val crashReporter: CrashReporter,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = "${AdManager.LOG_TAG}>${RewardedAdManager::class.java.simpleName}"
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

    fun loadRewardedAdForActivity(activity: IActivity) {
        val theActivity = activity.requireActivity()
        if (this.rewardedAd == null || (this.rewardedAdActivityHashCode != null && this.rewardedAdActivityHashCode != theActivity.hashCode())) {
            this.rewardedAdActivityHashCode = theActivity.hashCode()
            logAdsD(this, "loadRewardedAdForActivity() > Loading rewarded ad for ${theActivity::class.java.simpleName}...")
            RewardedAd.load(
                theActivity,
                theActivity.getString(R.string.google_ads_rewarded_ad_unit_id),
                AdManager.getAdRequest(activity),
                RewardedAdLoadCallback(this, this.crashReporter)
            )
            val loadCounts = this.defaultPrefRepository.getValue(
                DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS,
                DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS_DEFAULT
            )
            this.defaultPrefRepository.saveAsync(DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS, loadCounts + 1)
        } else {
            logAdsD(this, "loadRewardedAdForActivity() > NOT Loading rewarded ad for ${theActivity::class.java.simpleName}...")
        }
    }

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

    fun refreshRewardedAdStatus(activity: IActivity) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        val isNotPayingUser = this.globalAdManager.isShowingAds()
        if (!isNotPayingUser) {
            logAdsD(this, "refreshRewardedAdStatus() > SKIP (paying user or unknown)")
            return
        }
        val listener = this.rewardedAdListener
        if (listener == null) {
            logAdsD(this, "refreshRewardedAdStatus() > SKIP (unknown screen)")
            return
        }
        if (listener.skipRewardedAd()) {
            logAdsD(this, "refreshRewardedAdStatus() > SKIP (not in this screen)")
            return
        }
        logAdsD(this, "refreshRewardedAdStatus() > Load if necessary...")
        loadRewardedAdForActivity(activity)
    }

    fun isRewardedAdAvailableToShow(): Boolean {
        if (!AdConstants.AD_ENABLED) {
            return false
        }
        if (this.demoModeManager.enabled) {
            return false
        }
        if (this.rewardedAd == null) { // do not trigger creation + loading
            return false
        }
        return true
    }

    fun showRewardedAd(activity: IActivity): Boolean {
        if (!AdConstants.AD_ENABLED) {
            return false
        }
        if (this.rewardedAd == null) { // do not trigger creation + loading
            return false
        }
        val theActivity = activity.requireActivity()
        logAdsD(this, "showRewardedAd() > Showing rewarded ad for ${theActivity::class.java.simpleName}...")
        this.rewardedAd?.fullScreenContentCallback = RewardedAdFullScreenContentCallback(this, this.crashReporter, activity)
        this.rewardedAd?.show(theActivity, RewardedAdOnUserEarnedRewardListener(this.globalAdManager, activity))
        val showCounts = this.defaultPrefRepository.getValue(
            DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS,
            DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS_DEFAULT
        )
        this.defaultPrefRepository.saveAsync(DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, showCounts + 1)
        return true
    }
}