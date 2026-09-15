package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import androidx.core.content.edit
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mtransit.android.common.repository.DefaultPreferenceRepository
import org.mtransit.android.common.repository.LocalPreferenceRepository
import org.mtransit.android.commons.FakeSharedPreferences
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.user.UserManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.days

class SplashScreenViewModelTest {

    companion object {
        private const val NOW_MS = 1234567890_000L // Friday, February 13, 2009 at 11:31:30 p.m. UTC
    }

    private val defaultPrefRepoSharedPref = FakeSharedPreferences()
    private val userManager = UserManager(
        defaultPrefRepository = mock {
            on { pref } doReturn defaultPrefRepoSharedPref
        }
    )

    private val lclPrefRepoSharedPref = FakeSharedPreferences()

    @SuppressLint("DoNotMockPlatformTypes")
    private val subject = SplashScreenViewModel(
        appContext = mock {},
        lclPrefRepository = mock {
            on { pref } doReturn lclPrefRepoSharedPref
        },
        userManager = userManager,
        analyticsManager = mock {},
        savedStateHandle = mock {},
        demoModeManager = mock {},
        dataSourcesStorage = mock {},
        dataSourcesReader = mock {},
        dataSourceRequestManager = mock {},
        remoteConfigProvider = mock {},
        adManager = mock {},
        pm = mock {}
    )

    @BeforeTest
    fun setUp() {
        TimeUtils.setOverrideCurrentTimeMillis(NOW_MS)
    }

    @AfterTest
    fun tearDown() {
        defaultPrefRepoSharedPref.reset()
        lclPrefRepoSharedPref.reset()
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_AppOpenFirst() = runTest {
        userManager.set(appOpenLast = NOW_MS - 1.days.inWholeMilliseconds)

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(1, result)
        assertEquals(1, userManager.getAppOpenCount())
        assertEquals(NOW_MS, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(true, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_AppOpenFirst_AppOpenLastFallback() = runTest {
        userManager.setUserLearnedDrawer(false)

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(1, result)
        assertEquals(1, userManager.getAppOpenCount())
        assertEquals(NOW_MS, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(true, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_AppOpenFirst_AppOpenCountTooHigh() = runTest {
        userManager.set(appOpenCounts = 34)

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(35, result)
        assertEquals(35, userManager.getAppOpenCount())
        assertEquals(DefaultPreferenceRepository.PREF_USER_APP_OPEN_FIRST_DEFAULT, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(false, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_DailyUser() = runTest {
        userManager.set(
            appOpenCounts = 11,
            appOpenLast = NOW_MS - 6.days.inWholeMilliseconds,
        )

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(12, result)
        assertEquals(12, userManager.getAppOpenCount())
        assertEquals(DefaultPreferenceRepository.PREF_USER_APP_OPEN_FIRST_DEFAULT, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(true, userManager.getDailyUserNow())
        assertEquals(false, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_ResetUxAfterOneMonth() = runTest {
        userManager.set(appOpenLast = NOW_MS - 30.days.inWholeMilliseconds)
        lclPrefRepoSharedPref.edit { putString(LocalPreferenceRepository.PREFS_LCL_ROOT_SCREEN_ITEM_ID, "static-2") }

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(1, result)
        assertFalse(lclPrefRepoSharedPref.contains(LocalPreferenceRepository.PREFS_LCL_ROOT_SCREEN_ITEM_ID))
        assertEquals(false, userManager.getUserLearnedDrawerNow())
        assertEquals(1, userManager.getAppOpenCount())
        assertEquals(NOW_MS, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(true, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_DoNotResetUxBeforeOneMonth() = runTest {
        userManager.set(appOpenLast = NOW_MS - 29.days.inWholeMilliseconds)
        userManager.setUserLearnedDrawer(true)
        lclPrefRepoSharedPref.edit { putString(LocalPreferenceRepository.PREFS_LCL_ROOT_SCREEN_ITEM_ID, "static-2") }

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(1, result)
        assertEquals("static-2", lclPrefRepoSharedPref.getString(LocalPreferenceRepository.PREFS_LCL_ROOT_SCREEN_ITEM_ID, null))
        assertEquals(true, userManager.getUserLearnedDrawerNow())
        assertEquals(1, userManager.getAppOpenCount())
        assertEquals(NOW_MS, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(true, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenFirst_FrequentUser_FirstDays() = runTest {
        userManager.set(
            appOpenCounts = 10,
            appOpenFirst = NOW_MS - 4.days.inWholeMilliseconds,
        )

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(11, result)
        assertEquals(11, userManager.getAppOpenCount())
        assertEquals(NOW_MS - 4.days.inWholeMilliseconds, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(true, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenFirst_FrequentUser_NextDays() = runTest {
        userManager.set(
            appOpenCounts = 10,
            appOpenFirst = NOW_MS - 6.days.inWholeMilliseconds,
        )

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(11, result)
        assertEquals(11, userManager.getAppOpenCount())
        assertEquals(NOW_MS - 6.days.inWholeMilliseconds, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(false, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenFirst_CasualUser_FirstDays() = runTest {
        userManager.set(
            appOpenCounts = 9,
            appOpenFirst = NOW_MS - 9.days.inWholeMilliseconds,
        )

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(10, result)
        assertEquals(10, userManager.getAppOpenCount())
        assertEquals(NOW_MS - 9.days.inWholeMilliseconds, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(true, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenFirst_CasualUser_NextDays() = runTest {
        userManager.set(
            appOpenCounts = 9,
            appOpenFirst = NOW_MS - 11.days.inWholeMilliseconds,
        )

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(10, result)
        assertEquals(10, userManager.getAppOpenCount())
        assertEquals(NOW_MS - 11.days.inWholeMilliseconds, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(false, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenLast_LongTime_FewAppOpens() = runTest {
        userManager.set(
            appOpenCounts = 32,
            appOpenLast = NOW_MS - 100.days.inWholeMilliseconds,
        )

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(33, result)
        assertEquals(33, userManager.getAppOpenCount())
        assertEquals(DefaultPreferenceRepository.PREF_USER_APP_OPEN_FIRST_DEFAULT, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(true, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenLast_LongTime_ManyAppOpens() = runTest {
        userManager.set(
            appOpenCounts = 34,
            appOpenLast = NOW_MS - 100.days.inWholeMilliseconds,
        )

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(35, result)
        assertEquals(35, userManager.getAppOpenCount())
        assertEquals(DefaultPreferenceRepository.PREF_USER_APP_OPEN_FIRST_DEFAULT, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(false, userManager.getNewUser())
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenLast_ShortTime_FewAppOpens() = runTest {
        userManager.set(
            appOpenCounts = 32,
            appOpenLast = NOW_MS - 98.days.inWholeMilliseconds,
        )

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(33, result)
        assertEquals(33, userManager.getAppOpenCount())
        assertEquals(DefaultPreferenceRepository.PREF_USER_APP_OPEN_FIRST_DEFAULT, userManager.getAppOpenFirst())
        assertEquals(NOW_MS, userManager.getAppOpenLast())
        assertEquals(false, userManager.getDailyUserNow())
        assertEquals(false, userManager.getNewUser())
    }
}
