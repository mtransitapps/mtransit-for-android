package org.mtransit.android.rate

import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.mtransit.android.analytics.IAnalyticsManager
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.pref.liveData
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.provider.FavoriteRepository
import org.mtransit.android.ui.favorites.FavoritesFragment
import org.mtransit.android.ui.home.HomeFragment
import org.mtransit.android.ui.news.NewsListDetailFragment
import org.mtransit.android.ui.schedule.ScheduleFragment
import org.mtransit.android.ui.view.common.QuadrupleMediatorLiveData
import org.mtransit.android.util.BatteryOptimizationIssueUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRatingsManager @Inject constructor(
    private val defaultPrefRepository: DefaultPreferenceRepository,
    dataSourcesRepository: DataSourcesRepository,
    private val favoriteRepository: FavoriteRepository,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG = AppRatingsManager::class.java.simpleName

        const val ALWAYS_SHOW_APP_RATING_REQUEST = false
        // const val ALWAYS_SHOW_APP_RATING_REQUEST = true // DEBUG

        private const val INCREASED_APP_OPEN_COUNT = 50

        private const val SAMSUNG_INCREASED_APP_OPEN_COUNT = INCREASED_APP_OPEN_COUNT

        private const val AVOIDED_SCREENS_INCREASED_APP_OPEN_COUNT = 1000 + INCREASED_APP_OPEN_COUNT // TODO remove 1000 later?

        private val AVOIDED_SCREENS_TRACKING_NAME = listOf(
            HomeFragment.TRACKING_SCREEN_NAME, // just opened the app
        )

        private const val REDUCED_APP_OPEN_COUNT = 30

        private const val FIRST_REQUEST_APP_OPEN_COUNT = 70
        private const val NEXT_REQUEST_APP_OPEN_COUNT = 100

        private const val HAS_FAVORITES_REDUCED_APP_OPEN_COUNT = REDUCED_APP_OPEN_COUNT
        private const val PREFERRED_SCREENS_REDUCED_APP_OPEN_COUNT = REDUCED_APP_OPEN_COUNT

        private val PREFERRED_SCREENS_TRACKING_NAME = listOf(
            FavoritesFragment.TRACKING_SCREEN_NAME, // user has committed to re-use the app
            NewsListDetailFragment.TRACKING_SCREEN_NAME, // news is a distinctive app feature
            ScheduleFragment.TRACKING_SCREEN_NAME, // full schedule accessible offline is distinctive app feature
        )
    }

    override fun getLogTag(): String = LOG_TAG

    private val hasAgenciesEnabled = dataSourcesRepository.readingHasAgenciesEnabled()

    private val dailyUser by lazy {
        defaultPrefRepository.pref.liveData(
            DefaultPreferenceRepository.PREF_USER_DAILY,
            DefaultPreferenceRepository.PREF_USER_DAILY_DEFAULT
        )
    }

    private val appOpenCounts by lazy {
        defaultPrefRepository.pref.liveData(
            DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS,
            DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS_DEFAULT
        )
    }

    private val lastRequestAppOpenCount by lazy {
        defaultPrefRepository.pref.liveData(
            DefaultPreferenceRepository.PREF_USER_RATING_REQUEST_OPEN_COUNTS,
            DefaultPreferenceRepository.PREF_USER_RATING_REQUEST_OPEN_COUNTS_DEFAULT
        )
    }

    private suspend fun hasFavorites() = favoriteRepository.hasFavorites()

    @WorkerThread
    fun onAppRequestDisplayedSync() = runBlocking { onAppRequestDisplayed() }

    suspend fun onAppRequestDisplayed() = withContext(Dispatchers.IO) {
        val currentAppOpenCount = defaultPrefRepository.getValue(
            DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS,
            DefaultPreferenceRepository.PREF_USER_APP_OPEN_COUNTS_DEFAULT
        )
        // if (true) return@withContext // DEBUG do not persist for now
        defaultPrefRepository.pref.edit {
            putInt(DefaultPreferenceRepository.PREF_USER_RATING_REQUEST_OPEN_COUNTS, currentAppOpenCount)
        }
    }

    @JvmOverloads
    fun getShouldShowAppRatingRequest(trackingScreen: IAnalyticsManager.Trackable? = null): LiveData<Boolean> = QuadrupleMediatorLiveData(
        hasAgenciesEnabled,
        lastRequestAppOpenCount,
        appOpenCounts,
        dailyUser,
    ).switchMap { (hasAgenciesEnabled, lastRequestAppOpenCount, appOpenCounts, dailyUser) ->
        liveData {
            val hasFavorites = hasFavorites()
            val isSamsungDevice = BatteryOptimizationIssueUtils.isSamsungDevice()
            val trackingScreenName = trackingScreen?.screenName
            emit(
                shouldShowAppRatingRequest(
                    trackingScreenName = trackingScreenName,
                    hasFavorites = hasFavorites,
                    isSamsungDevice = isSamsungDevice,
                    hasAgenciesEnabled = hasAgenciesEnabled,
                    dailyUser = dailyUser,
                    appOpenCounts = appOpenCounts,
                    lastRequestAppOpenCount = lastRequestAppOpenCount,
                )
            )
        }
    }

    @Suppress("RedundantIf")
    @VisibleForTesting
    fun shouldShowAppRatingRequest(
        trackingScreenName: String? = null,
        hasFavorites: Boolean? = null,
        isSamsungDevice: Boolean? = null,
        hasAgenciesEnabled: Boolean? = null,
        dailyUser: Boolean? = null,
        appOpenCounts: Int? = null,
        lastRequestAppOpenCount: Int? = null,
    ): Boolean {
        if (Constants.DEBUG && ALWAYS_SHOW_APP_RATING_REQUEST) return true
        val theHasAgenciesEnabled = hasAgenciesEnabled ?: return false
        var theAppOpenCounts = appOpenCounts ?: return false
        val theLastRequestAppOpenCount = lastRequestAppOpenCount ?: return false
        val theDailyUser = dailyUser ?: return false
        if (!theHasAgenciesEnabled) {
            return false
        }
        if (!theDailyUser) {
            return false
        }
        // Good
        if (PREFERRED_SCREENS_TRACKING_NAME.contains(trackingScreenName)) {
            theAppOpenCounts += PREFERRED_SCREENS_REDUCED_APP_OPEN_COUNT
        } else if (hasFavorites == true) {
            theAppOpenCounts += HAS_FAVORITES_REDUCED_APP_OPEN_COUNT
        }
        // Bad
        if (isSamsungDevice == true) {
            theAppOpenCounts -= SAMSUNG_INCREASED_APP_OPEN_COUNT
        }
        if (AVOIDED_SCREENS_TRACKING_NAME.contains(trackingScreenName)) {
            theAppOpenCounts -= AVOIDED_SCREENS_INCREASED_APP_OPEN_COUNT
        }
        if (theAppOpenCounts < FIRST_REQUEST_APP_OPEN_COUNT) {
            return false
        }
        if (theLastRequestAppOpenCount != DefaultPreferenceRepository.PREF_USER_RATING_REQUEST_OPEN_COUNTS_DEFAULT
            && theLastRequestAppOpenCount - FIRST_REQUEST_APP_OPEN_COUNT + NEXT_REQUEST_APP_OPEN_COUNT > appOpenCounts
        ) {
            return false
        }
        return true
    }
}