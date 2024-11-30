package org.mtransit.android.ad.rewarded

import android.widget.Toast
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.view.common.IActivity
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardedUserManager @Inject constructor(
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val demoModeManager: DemoModeManager,
) {

    val dailyUser: Boolean by lazy {
        this.defaultPrefRepository.getValue(
            DefaultPreferenceRepository.PREF_USER_DAILY,
            DefaultPreferenceRepository.PREF_USER_DAILY_DEFAULT
        )
    }

    val hasLowLoadShowRatio: Boolean by lazy {
        val showCounts = this.defaultPrefRepository.getValue(
            DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS,
            DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS_DEFAULT
        )
        val loadCounts = this.defaultPrefRepository.getValue(
            DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS,
            DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS_DEFAULT
        )
        val newHasLowLoadShowRatio = when {
            loadCounts <= 0 -> false
            showCounts <= 0 -> loadCounts >= 10
            else -> (showCounts.toFloat() / loadCounts) < 0.10f
        }
        newHasLowLoadShowRatio
    }

    private var rewardedUntilInMs: Long? = null

    fun getRewardedUntilInMs(): Long {
        if (!AdConstants.AD_ENABLED) {
            return Long.MAX_VALUE // forever
        }
        if (this.rewardedUntilInMs == null) {
            this.rewardedUntilInMs = this.defaultPrefRepository.getValue(
                DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL,
                DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT
            )
        }
        return this.rewardedUntilInMs ?: Long.MAX_VALUE
    }

    fun setRewardedUntilInMs(newRewardedUntilInMs: Long) {
        this.rewardedUntilInMs = newRewardedUntilInMs
        this.defaultPrefRepository.saveAsync(
            DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL,
            newRewardedUntilInMs
        )
    }

    fun resetRewarded() {
        setRewardedUntilInMs(DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT)
    }

    fun isRewardedNow(): Boolean {
        if (!AdConstants.AD_ENABLED) {
            return true
        }
        if (this.demoModeManager.enabled) {
            return true
        }
        return getRewardedUntilInMs() > TimeUtils.currentTimeMillis()
    }

    fun rewardUser(newRewardInMs: Long, activity: IActivity?) {
        val currentRewardedUntilOrNow = maxOf(getRewardedUntilInMs(), TimeUtils.currentTimeMillis())
        setRewardedUntilInMs(currentRewardedUntilOrNow + newRewardInMs)
        activity?.let {
            ToastUtils.makeTextAndShowCentered(
                it.context,
                R.string.support_watch_rewarded_ad_successful_message,
                Toast.LENGTH_LONG
            )
            // refreshBannerAdStatus(it)
        }
    }

    fun shouldSkipRewardedAd(): Boolean {
        if (!this.dailyUser) {
            return true // always skip for non-daily users
        }
        if (this.hasLowLoadShowRatio) {
            return true // too much loads for too less shows
        }
        if (!isRewardedNow()) {
            return false // never skip for non-rewarded users
        }
        val rewardedUntilInMs = getRewardedUntilInMs()
        val skipRewardedAdUntilInMs = TimeUtils.currentTimeMillis() -
                TimeUnit.HOURS.toMillis(1L) + // accounts for "recent" rewards
                2L * getRewardedAdAmountInMs()
        return rewardedUntilInMs > skipRewardedAdUntilInMs
    }

    fun getRewardedAdAmount(): Int {
        return 7 // 1 week
    }

    fun getRewardedAdAmountInMs(): Long {
        val rewardAmount = getRewardedAdAmount() // TODO custom amount? rewardItem.getAmount()
        val rewardType = TimeUnit.DAYS // TODO custom type? rewardItem.getType()
        return rewardType.toMillis(rewardAmount.toLong())
    }

}