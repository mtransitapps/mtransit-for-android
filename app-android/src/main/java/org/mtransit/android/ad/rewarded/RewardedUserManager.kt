package org.mtransit.android.ad.rewarded

import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import org.mtransit.android.R
import org.mtransit.android.ad.AdConstants
import org.mtransit.android.ad.AdManager
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.ToastUtils
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.toMillis
import org.mtransit.android.dev.DemoModeManager
import org.mtransit.android.provider.remoteconfig.RemoteConfigProvider
import org.mtransit.android.ui.view.common.IActivity
import org.mtransit.android.user.UserManager
import org.mtransit.commons.toIntOrNull
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Singleton
class RewardedUserManager @Inject constructor(
    private val userManager: UserManager,
    private val demoModeManager: DemoModeManager,
    private val remoteConfigProvider: RemoteConfigProvider,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = "${AdManager.LOG_TAG}>${RewardedUserManager::class.java.simpleName}"

        private const val REWARDED_UNTIL_NO_VALUE = -1L
        private const val MAX_LOAD_FOR_NO_SHOW = 333
        private const val MIN_SHOW_TO_LOAD_RATIO = 0.07f
    }

    override fun getLogTag() = LOG_TAG

    @get:WorkerThread
    private val _hasLowLoadShowRatio: Boolean
        get() {
            //noinspection DiscouragedApi
            val showCounts = userManager.getRewardedShowCountsNow()
            //noinspection DiscouragedApi
            val loadCounts = userManager.getRewardedLoadCountsNow()
            val newHasLowLoadShowRatio = when {
                loadCounts <= 0 -> false // never loaded
                showCounts <= 0 -> loadCounts >= MAX_LOAD_FOR_NO_SHOW // never showed
                else -> (showCounts.toFloat() / loadCounts) < MIN_SHOW_TO_LOAD_RATIO
            }
            return newHasLowLoadShowRatio
        }

    private var _rewardedUntilInMs = AtomicLong(REWARDED_UNTIL_NO_VALUE)

    @WorkerThread
    private fun getRewardedUntilInMs(): Long {
        if (!AdConstants.AD_ENABLED) return Long.MAX_VALUE // forever rewarded (no ads)
        return this._rewardedUntilInMs.updateAndGet { cached ->
            if (cached != REWARDED_UNTIL_NO_VALUE) cached
            //noinspection DiscouragedApi
            else userManager.getRewardedUntilNow()
        }.coerceAtLeast(0L)
    }

    @WorkerThread
    fun getRewardedUntil(): Instant {
        if (!AdConstants.AD_ENABLED) return Instant.DISTANT_FUTURE // forever rewarded (no ads)
        return getRewardedUntilInMs().millisToInstant()
    }

    val rewardedUntilLive: LiveData<Instant> = userManager.rewardedUntil.distinctUntilChanged().map {
        it.millisToInstant()
    }

    private fun setRewardedUntil(newRewardedUntil: Instant) = setRewardedUntilInMs(newRewardedUntil.toMillis())

    private fun setRewardedUntilInMs(newRewardedUntilInMs: Long) {
        this._rewardedUntilInMs.set(newRewardedUntilInMs)
        //noinspection DiscouragedApi
        userManager.setRewardedUntilNow(newRewardedUntilInMs)
    }

    fun resetRewarded() {
        setRewardedUntilInMs(0L)
    }

    val rewardedNowLive: LiveData<Boolean> = this.rewardedUntilLive
        .map { isRewardedNow(it) }

    @WorkerThread
    fun isRewardedNow() = isRewardedNow(getRewardedUntil())

    private fun isRewardedNow(rewardedUntil: Instant): Boolean {
        if (!AdConstants.AD_ENABLED) return true
        if (this.demoModeManager.enabled) return true
        return rewardedUntil > TimeUtilsK.currentInstant()
    }

    @WorkerThread
    fun rewardUser(newReward: Duration, activity: IActivity?) {
        rewardUser(newReward, getRewardedUntil(), activity)
    }

    fun rewardUser(newReward: Duration, rewardedUntil: Instant, activity: IActivity?) {
        val currentRewardedUntilOrNow = maxOf(rewardedUntil, TimeUtilsK.currentInstant())
        setRewardedUntil(currentRewardedUntilOrNow + newReward)
        activity?.activity?.let { activity ->
            activity.runOnUiThread {
                ToastUtils.makeTextAndShow(
                    activity,
                    R.string.support_watch_rewarded_ad_successful_message,
                    Toast.LENGTH_LONG
                )
            }
        }
    }

    @WorkerThread
    fun shouldSkipLoadingRewardedAd(): Boolean {
        //noinspection DiscouragedApi
        return shouldSkipLoadingRewardedAd(this.userManager.getDailyUserNow(), this._hasLowLoadShowRatio, getRewardedUntil())
    }

    fun shouldSkipLoadingRewardedAd(dailyUser: Boolean, hasLowLoadShowRatio: Boolean, rewardedUntil: Instant): Boolean {
        if (!dailyUser) return true // always skip for non-daily users
        if (hasLowLoadShowRatio) return true // too much loads for not enough shows
        if (!isRewardedNow(rewardedUntil)) return false // never skip for non-rewarded users
        val skipRewardedAdUntil = TimeUtilsK.currentInstant() -
                1.hours + // accounts for "recent" rewards
                _rewardedAdAmountInDays.days.times(2)
        return rewardedUntil > skipRewardedAdUntil
    }

    private val _rewardedAdAmountInDays: Long by lazy {
        remoteConfigProvider.get(
            RemoteConfigProvider.AD_REWARDED_AMOUNT_IN_DAYS,
            RemoteConfigProvider.AD_REWARDED_AMOUNT_IN_DAYS_DEFAULT.toLong(), // 1 week (not possible to have dynamic eCPM/RPM-based value)
        )
    }

    val rewardedAdAmountInDays get() = _rewardedAdAmountInDays.toIntOrNull() ?: RemoteConfigProvider.AD_REWARDED_AMOUNT_IN_DAYS_DEFAULT
}
