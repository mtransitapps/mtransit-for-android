package org.mtransit.android.ad

import androidx.annotation.AnyThread
import org.mtransit.android.ad.banner.BannerAdManager
import org.mtransit.android.ad.rewarded.RewardedUserManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TaskUtils
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.dev.CrashReporter
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.view.common.IActivity
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalAdManager @Inject constructor(
    private val dataSourcesRepository: DataSourcesRepository,
    private val crashReporter: CrashReporter,
    private val demoModeManager: DemoModeManager,
    private val rewardedUserManager: RewardedUserManager,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = AdManager.LOG_TAG + ">" + GlobalAdManager::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    private val initialized = AtomicBoolean(false)

    private var showingAds: Boolean? = null
    private var hasAgenciesEnabled: Boolean? = null

    init {
        this.dataSourcesRepository.readingHasAgenciesEnabled().observeForever { hasAgenciesEnabled ->
            this.hasAgenciesEnabled = hasAgenciesEnabled
        }
    }

    fun init(activity: IActivity, adScreenFragment: IAdScreenFragment?, bannerAdManager: BannerAdManager) {
        if (!AdConstants.AD_ENABLED) {
            return
        }
        val theActivity = activity.getActivity()
        if (theActivity == null) {
            MTLog.w(this, "Trying to initialized w/o activity!")
            return // SKIP
        }
        if (this.initialized.get()) {
            MTLog.d(this, "init() > SKIP (init: %s)", this.initialized.get())
            return // SKIP
        }
        try {
            TaskUtils.execute(InitTask(this, bannerAdManager, activity, adScreenFragment))
        } catch (e: Exception) {
            this.crashReporter.w(this, e, "Error while initializing Ads!")
        }
    }

    fun setShowingAds(showingAds: Boolean?) {
        this.showingAds = showingAds
    }

    @AnyThread
    fun isShowingAds(): Boolean {
        if (!AdConstants.AD_ENABLED) {
            return false
        }
        if (hasAgenciesEnabled == null) {
            hasAgenciesEnabled = this.dataSourcesRepository.hasAgenciesEnabled()
        }
        if (this.initialized.get() != true) {
            MTLog.d(this, "isShowingAds() > Not showing ads (not initialized yet).")
            return false // not showing ads
        }
        // number of agency unknown
        if (hasAgenciesEnabled == false) { // no (real) agency installed
            MTLog.d(this, "isShowingAds() > Not showing ads (no agency added).")
            return false // not showing ads
        } else if (demoModeManager.enabled) {
            MTLog.d(this, "isShowingAds() > Not showing ads (demo mode).")
            return false // not showing ads
        }
        if (showingAds == null) { // paying status unknown
            MTLog.d(this, "isShowingAds() > Not showing ads (paying status unknown).")
            return false // not showing ads
        }
        MTLog.d(this, "isShowingAds() > Showing ads: '$showingAds'.")
        if (AdConstants.IGNORE_REWARD_HIDING_BANNER) {
            return showingAds == true
        }
        if (isRewardedNow()) { // rewarded status
            MTLog.d(this, "isShowingAds() > Not showing banner ads (rewarded until: ${this.rewardedUserManager.getRewardedUntilInMs()}).")
            return false // not showing ads
        }
        return showingAds == true
    }

    fun onHasAgenciesEnabledUpdated(hasAgenciesEnabled: Boolean?) {
        this.hasAgenciesEnabled = hasAgenciesEnabled
    }

    fun getAndSetInitialized(newInitialized: Boolean) =
        initialized.getAndSet(newInitialized)

    // region Rewarded

    fun getRewardedUntilInMs(): Long {
        return this.rewardedUserManager.getRewardedUntilInMs()
    }

    fun resetRewarded() {
        this.rewardedUserManager.resetRewarded()
    }

    fun isRewardedNow(): Boolean {
        return this.rewardedUserManager.isRewardedNow()
    }

    fun rewardUser(newRewardInMs: Long, activity: IActivity?) {
        this.rewardedUserManager.rewardUser(newRewardInMs, activity)
    }

    fun shouldSkipRewardedAd(): Boolean {
        return this.rewardedUserManager.shouldSkipRewardedAd()
    }

    fun getRewardedAdAmount(): Int {
        return this.rewardedUserManager.getRewardedAdAmount()
    }

    fun getRewardedAdAmountInMs(): Long {
        return this.rewardedUserManager.getRewardedAdAmountInMs()
    }

    // endregion Rewarded
}