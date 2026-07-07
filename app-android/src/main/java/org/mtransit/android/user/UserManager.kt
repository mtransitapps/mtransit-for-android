package org.mtransit.android.user

import androidx.annotation.Discouraged
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.pref.liveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserManager @Inject constructor(
    private val defaultPrefRepository: DefaultPreferenceRepository,
) {

    fun set(
        appOpenCounts: Int? = null,
        appOpenFirst: Long? = null,
        appOpenLast: Long? = null,
        dailyUser: Boolean? = null,
    ) {
        defaultPrefRepository.pref.edit {
            appOpenCounts?.let { putInt(DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS, it) }
            appOpenFirst?.let { putLong(DefaultPreferenceRepository.PREF_USER_APP_OPEN_FIRST, it) }
            appOpenLast?.let { putLong(DefaultPreferenceRepository.PREF_USER_APP_OPEN_LAST, it) }
            dailyUser?.let { putBoolean(DefaultPreferenceRepository.PREF_USER_DAILY, it) }
        }
    }

    // region learned drawer

    val userLearnedDrawer: LiveData<Boolean>
        get() = defaultPrefRepository.pref.liveData(
            DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER, DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER_DEFAULT
        )

    @Suppress("unused")
    suspend fun getUserLearnedDrawer() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getBoolean(
            DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER, DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER_DEFAULT
        )
    }

    @WorkerThread
    @Discouraged("use suspend function or live data")
    fun getUserLearnedDrawerNow() = defaultPrefRepository.pref.getBoolean(
        DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER, DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER_DEFAULT
    )

    suspend fun setUserLearnedDrawer(learned: Boolean) = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.edit {
            putBoolean(DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER, learned)
        }
    }

    @WorkerThread
    @Discouraged("use suspend function")
    fun setUserLearnedDrawerNow(learned: Boolean) = defaultPrefRepository.pref.edit {
        putBoolean(DefaultPreferenceRepository.PREF_USER_LEARNED_DRAWER, learned)
    }

    // endregion

    // region daily user

    val dailyUser: LiveData<Boolean>
        get() = defaultPrefRepository.pref.liveData(
            DefaultPreferenceRepository.PREF_USER_DAILY, DefaultPreferenceRepository.PREF_USER_DAILY_DEFAULT
        )

    @Suppress("unused")
    suspend fun getDailyUser() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getBoolean(
            DefaultPreferenceRepository.PREF_USER_DAILY, DefaultPreferenceRepository.PREF_USER_DAILY_DEFAULT
        )
    }

    @WorkerThread
    @Discouraged("use suspend function or live data")
    fun getDailyUserNow() = defaultPrefRepository.pref.getBoolean(
        DefaultPreferenceRepository.PREF_USER_DAILY, DefaultPreferenceRepository.PREF_USER_DAILY_DEFAULT
    )

    // endregion

    // region app open counts

    val appOpenCounts: LiveData<Int>
        get() = defaultPrefRepository.pref.liveData(
            DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS, DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS_DEFAULT
        )

    suspend fun getAppOpenCount() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getInt(
            DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS, DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS_DEFAULT
        )
    }

    // endregion

    // region last rating request app open counts

    val lastRequestAppOpenCount: LiveData<Int>
        get() = defaultPrefRepository.pref.liveData(
            DefaultPreferenceRepository.PREF_USER_RATING_REQUEST_OPEN_COUNTS, DefaultPreferenceRepository.PREF_USER_RATING_REQUEST_OPEN_COUNTS_DEFAULT
        )

    suspend fun setRatingRequestOpenCount(count: Int) = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.edit {
            putInt(DefaultPreferenceRepository.PREF_USER_RATING_REQUEST_OPEN_COUNTS, count)
        }
    }

    // endregion

    // region app open first

    suspend fun getAppOpenFirst() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getLong(
            DefaultPreferenceRepository.PREF_USER_APP_OPEN_FIRST, DefaultPreferenceRepository.PREF_USER_APP_OPEN_FIRST_DEFAULT
        )
    }

    // endregion

    // region app open last

    suspend fun getAppOpenLast() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getLong(
            DefaultPreferenceRepository.PREF_USER_APP_OPEN_LAST, DefaultPreferenceRepository.PREF_USER_APP_OPEN_LAST_DEFAULT
        )
    }

    // endregion

    // region rewarded until

    val rewardedUntil: LiveData<Long>
        get() = defaultPrefRepository.pref.liveData(
            DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL, DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT
        )

    @Suppress("unused")
    suspend fun getRewardedUntil() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getLong(
            DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL, DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT
        )
    }

    @WorkerThread
    @Discouraged("use suspend function or live data")
    fun getRewardedUntilNow() = defaultPrefRepository.pref.getLong(
        DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL, DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT
    )

    @Suppress("unused")
    suspend fun setRewardedUntil(rewardedUntil: Long) = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.edit {
            putLong(DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL, rewardedUntil)
        }
    }

    @WorkerThread
    @Discouraged("use suspend function")
    fun setRewardedUntilNow(rewardedUntil: Long) = defaultPrefRepository.pref.edit {
        putLong(DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL, rewardedUntil)
    }

    // endregion

    // endregion rewarded load counts

    suspend fun getRewardedLoadCounts() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getInt(
            DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS_DEFAULT
        )
    }

    @WorkerThread
    @Discouraged("use suspend function or live data")
    fun getRewardedLoadCountsNow() = defaultPrefRepository.pref.getInt(
        DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS_DEFAULT
    )

    suspend fun setRewardedLoadCounts(rewardedLoadCounts: Int) = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.edit {
            putInt(DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS, rewardedLoadCounts)
        }
    }

    // endregion

    // region rewarded show counts

    @Suppress("unused")
    suspend fun getRewardedShowCounts() = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.getInt(
            DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS_DEFAULT
        )
    }

    @WorkerThread
    @Discouraged("use suspend function or live data")
    fun getRewardedShowCountsNow() = defaultPrefRepository.pref.getInt(
        DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS_DEFAULT
    )

    @Suppress("unused")
    suspend fun setRewardedShowCounts(rewardedShowCounts: Int) = withContext(Dispatchers.IO) {
        defaultPrefRepository.pref.edit {
            putInt(DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, rewardedShowCounts)
        }
    }

    @WorkerThread
    @Discouraged("use suspend function")
    fun setRewardedShowCountsNow(rewardedShowCounts: Int) = defaultPrefRepository.pref.edit {
        putInt(DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, rewardedShowCounts)
    }

    // endregion
}
