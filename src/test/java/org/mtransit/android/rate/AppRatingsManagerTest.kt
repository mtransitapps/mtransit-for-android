package org.mtransit.android.rate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.datasource.DataSourcesRepository
import org.mtransit.android.provider.FavoriteRepository


class AppRatingsManagerTest {

    private val defaultPrefRepository: DefaultPreferenceRepository = mock()
    private val dataSourcesRepository: DataSourcesRepository = mock()
    private val favoriteRepository: FavoriteRepository = mock()

    private val subject = AppRatingsManager(
        defaultPrefRepository,
        dataSourcesRepository,
        favoriteRepository,
    )

    @Test
    fun verify_shouldShowAppRequest_noData() {
        val result = subject.shouldShowAppRatingRequest()

        assertFalse(result)
    }

    @Test
    fun verify_shouldShowAppRequest_hasAgenciesEnabled_Not() {
        val hasAgenciesEnabled = false

        val result = subject.shouldShowAppRatingRequest(
            hasAgenciesEnabled = hasAgenciesEnabled,
        )

        assertFalse(result)
    }

    @Test
    fun verify_shouldShowAppRequest_firstAppOpenCount_Not() {
        val hasAgenciesEnabled = true
        val appOpenCounts = 30

        val result = subject.shouldShowAppRatingRequest(
            hasAgenciesEnabled = hasAgenciesEnabled,
            appOpenCounts = appOpenCounts,
        )

        assertFalse(result)
    }

    @Test
    fun verify_shouldShowAppRequest_firstAppOpenCount_Yes_Daily_Not() {
        val hasAgenciesEnabled = true
        val appOpenCounts = 71

        val result = subject.shouldShowAppRatingRequest(
            hasAgenciesEnabled = hasAgenciesEnabled,
            appOpenCounts = appOpenCounts,
        )

        assertFalse(result)
    }

    @Test
    fun verify_shouldShowAppRequest_firstAppOpenCount_Yes_Daily_Yes_RequestedUnknown() {
        val hasAgenciesEnabled = true
        val appOpenCounts = 71
        val dailyUser = true

        val result = subject.shouldShowAppRatingRequest(
            hasAgenciesEnabled = hasAgenciesEnabled,
            dailyUser = dailyUser,
            appOpenCounts = appOpenCounts,
        )

        assertFalse(result)
    }

    @Test
    fun verify_shouldShowAppRequest_firstAppOpenCount_Yes_Daily_Yes_NeverRequested() {
        val hasAgenciesEnabled = true
        val lastRequestAppOpenCount = 0
        val appOpenCounts = 71
        val dailyUser = true

        val result = subject.shouldShowAppRatingRequest(
            hasAgenciesEnabled = hasAgenciesEnabled,
            dailyUser = dailyUser,
            appOpenCounts = appOpenCounts,
            lastRequestAppOpenCount = lastRequestAppOpenCount,
        )

        assertTrue(result)
    }

    @Test
    fun verify_shouldShowAppRequest_firstAppOpenCount_Yes_Daily_Yes_NeverRequested_Samsung() {
        val isSamsungDevice = true
        val hasAgenciesEnabled = true
        val lastRequestAppOpenCount = 0
        val appOpenCounts = 71
        val dailyUser = true

        val result = subject.shouldShowAppRatingRequest(
            isSamsungDevice = isSamsungDevice,
            hasAgenciesEnabled = hasAgenciesEnabled,
            dailyUser = dailyUser,
            appOpenCounts = appOpenCounts,
            lastRequestAppOpenCount = lastRequestAppOpenCount,
        )

        assertFalse(result)
    }

    @Test
    fun verify_shouldShowAppRequest_firstAppOpenCount_Yes_Daily_Yes_RequestAgain_No() {
        val hasAgenciesEnabled = true
        val lastRequestAppOpenCount = 80
        val appOpenCounts = 90
        val dailyUser = true

        val result = subject.shouldShowAppRatingRequest(
            hasAgenciesEnabled = hasAgenciesEnabled,
            dailyUser = dailyUser,
            appOpenCounts = appOpenCounts,
            lastRequestAppOpenCount = lastRequestAppOpenCount,
        )

        assertFalse(result)
    }

    @Test
    fun verify_shouldShowAppRequest_firstAppOpenCount_Yes_Daily_Yes_RequestAgain_Yes() {
        val hasAgenciesEnabled = true
        val lastRequestAppOpenCount = 80
        val appOpenCounts = 190
        val dailyUser = true

        val result = subject.shouldShowAppRatingRequest(
            hasAgenciesEnabled = hasAgenciesEnabled,
            dailyUser = dailyUser,
            appOpenCounts = appOpenCounts,
            lastRequestAppOpenCount = lastRequestAppOpenCount,
        )

        assertTrue(result)
    }

    @Test
    fun verify_shouldShowAppRequest_firstAppOpenCount_Yes_Daily_Yes_NeverRequested_PreferredScreen() {
        val screenName = "Favorites"
        val hasAgenciesEnabled = true
        val lastRequestAppOpenCount = 0
        val appOpenCounts = 41
        val dailyUser = true

        val result = subject.shouldShowAppRatingRequest(
            trackingScreenName = screenName,
            hasAgenciesEnabled = hasAgenciesEnabled,
            dailyUser = dailyUser,
            appOpenCounts = appOpenCounts,
            lastRequestAppOpenCount = lastRequestAppOpenCount,
        )

        assertTrue(result)
    }

    @Test
    fun verify_shouldShowAppRequest_firstAppOpenCount_Yes_Daily_Yes_NeverRequested_HasFavorites() {
        val hasFavorites = true
        val hasAgenciesEnabled = true
        val lastRequestAppOpenCount = 0
        val appOpenCounts = 41
        val dailyUser = true

        val result = subject.shouldShowAppRatingRequest(
            hasFavorites = hasFavorites,
            hasAgenciesEnabled = hasAgenciesEnabled,
            dailyUser = dailyUser,
            appOpenCounts = appOpenCounts,
            lastRequestAppOpenCount = lastRequestAppOpenCount,
        )

        assertTrue(result)
    }
}