package org.mtransit.android.ad.rewarded

import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.ui.view.common.IActivity
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardedUserManager @Inject constructor(
    private val defaultPrefRepository: DefaultPreferenceRepository,
    private val demoModeManager: DemoModeManager,
) {

    companion object {
        private const val REWARDED_UNTIL_NO_VALUE = -1L
    }

    @get:WorkerThread
    private val _dailyUser: Boolean by lazy {
        this.defaultPrefRepository.getValue(
            DefaultPreferenceRepository.PREF_USER_DAILY,
            DefaultPreferenceRepository.PREF_USER_DAILY_DEFAULT
        )
    }

    @get:WorkerThread
    private val _hasLowLoadShowRatio: Boolean by lazy {
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

    private var _rewardedUntilInMs = AtomicLong(REWARDED_UNTIL_NO_VALUE)

    @WorkerThread
    fun getRewardedUntilInMs(): Long {
        if (!AdConstants.AD_ENABLED) return Long.MAX_VALUE // forever
        return this._rewardedUntilInMs.updateAndGet { cached ->
            if (cached != REWARDED_UNTIL_NO_VALUE) cached
            else this.defaultPrefRepository.getValue(
                DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL,
                DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT,
            )
        }.takeUnless { it < 0L } ?: DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT
    }

    val rewardedUntilInMsLive: LiveData<Long> = defaultPrefRepository.pref.liveData(
        DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL,
        DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT,
    ).distinctUntilChanged()

    @AnyThread
    fun setRewardedUntilInMs(newRewardedUntilInMs: Long) {
        this._rewardedUntilInMs.set(newRewardedUntilInMs)
        defaultPrefRepository.pref.edit {
            putLong(DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL, newRewardedUntilInMs)
        }
    }

    @AnyThread
    fun resetRewarded() {
        setRewardedUntilInMs(DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT)
    }

    val rewardedNowLive: LiveData<Boolean> = this.rewardedUntilInMsLive
        .map { rewardedUntilInMs ->
            if (!AdConstants.AD_ENABLED) return@map true
            if (this.demoModeManager.enabled) return@map true
            rewardedUntilInMs > TimeUtils.currentTimeMillis()
        }

    @WorkerThread
    fun isRewardedNow(): Boolean {
        return isRewardedNow(getRewardedUntilInMs())
    }

    @AnyThread
    fun isRewardedNow(rewardedUntilInMs: Long): Boolean {
        if (!AdConstants.AD_ENABLED) return true
        if (this.demoModeManager.enabled) return true
        return rewardedUntilInMs > TimeUtils.currentTimeMillis()
    }

    @WorkerThread
    fun rewardUser(newRewardInMs: Long, activity: IActivity?) {
        rewardUser(newRewardInMs, getRewardedUntilInMs(), activity)
    }

    @AnyThread
    fun rewardUser(newRewardInMs: Long, rewardedUntilInMs: Long, activity: IActivity?) {
        val currentRewardedUntilOrNow = maxOf(rewardedUntilInMs, TimeUtils.currentTimeMillis())
        setRewardedUntilInMs(currentRewardedUntilOrNow + newRewardInMs)
        activity?.activity?.let { activity ->
            activity.runOnUiThread {
                ToastUtils.makeTextAndShowCentered(
                    activity,
                    R.string.support_watch_rewarded_ad_successful_message,
                    Toast.LENGTH_LONG
                )
            }
        }
    }

    @WorkerThread
    fun shouldSkipRewardedAd(): Boolean {
        return shouldSkipRewardedAd(this._dailyUser, this._hasLowLoadShowRatio, getRewardedUntilInMs())
    }

    @AnyThread
    fun shouldSkipRewardedAd(dailyUser: Boolean, hasLowLoadShowRatio: Boolean, rewardedUntilInMs: Long): Boolean {
        if (!dailyUser) return true // always skip for non-daily users
        if (hasLowLoadShowRatio) return true // too much loads for not enough shows
        if (!isRewardedNow(rewardedUntilInMs)) return false // never skip for non-rewarded users
        val skipRewardedAdUntilInMs = TimeUtils.currentTimeMillis() -
                TimeUnit.HOURS.toMillis(1L) + // accounts for "recent" rewards
                2L * getRewardedAdAmountInMs()
        return rewardedUntilInMs > skipRewardedAdUntilInMs
    }

    @AnyThread
    fun getRewardedAdAmount() = 7 // 1 week

    @AnyThread
    fun getRewardedAdAmountInMs(): Long {
        val rewardAmount = getRewardedAdAmount() // TODO custom amount? rewardItem.getAmount()
        val rewardType = TimeUnit.DAYS // TODO custom type? rewardItem.getType()
        return rewardType.toMillis(rewardAmount.toLong())
    }
}
