package org.mtransit.android.ui.splash

import android.annotation.SuppressLint
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.user.UserManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class SplashScreenViewModelTest {

    companion object {
        private const val NOW_MS = 123456789_000L
    }

    private val userManager: UserManager = mock {
        on { getAppOpenCount() } doReturn 0
    }

    @SuppressLint("DoNotMockPlatformTypes")
    private val subject = SplashScreenViewModel(
        appContext = mock {},
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
        reset(userManager)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_AppOpenFirst() = runTest {
        whenever { userManager.getAppOpenLastOrNull() } doReturn NOW_MS - 1.days.inWholeMilliseconds

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(1, result)
        verify(userManager).set(appOpenCounts = 1, appOpenFirst = NOW_MS, appOpenLast = NOW_MS, dailyUser = false, newUser = true)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_AppOpenFirst_AppOpenLastFallback() = runTest {
        whenever { userManager.getAppOpenLastOrNull() } doReturn null

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(1, result)
        verify(userManager).set(appOpenCounts = 1, appOpenFirst = NOW_MS, appOpenLast = NOW_MS, dailyUser = false, newUser = true)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_AppOpenFirst_AppOpenCountTooHigh() = runTest {
        whenever { userManager.getAppOpenCount() } doReturn 34

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(35, result)
        verify(userManager).set(appOpenCounts = 35, appOpenFirst = null, appOpenLast = NOW_MS, dailyUser = false, newUser = false)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_DailyUser() = runTest {
        whenever { userManager.getAppOpenCount() } doReturn 11
        whenever { userManager.getAppOpenLastOrNull() } doReturn NOW_MS - 6.days.inWholeMilliseconds

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(12, result)
        verify(userManager).set(appOpenCounts = 12, appOpenFirst = null, appOpenLast = NOW_MS, dailyUser = true, newUser = false)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenFirst_FrequentUser_FirstDays() = runTest {
        whenever { userManager.getAppOpenCount() } doReturn 10
        whenever { userManager.getAppOpenFirstOrNull() } doReturn NOW_MS - 4.days.inWholeMilliseconds

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(11, result)
        verify(userManager).set(appOpenCounts = 11, appOpenFirst = null, appOpenLast = NOW_MS, dailyUser = false, newUser = true)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenFirst_FrequentUser_NextDays() = runTest {
        whenever { userManager.getAppOpenCount() } doReturn 10
        whenever { userManager.getAppOpenFirstOrNull() } doReturn NOW_MS - 6.days.inWholeMilliseconds

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(11, result)
        verify(userManager).set(appOpenCounts = 11, appOpenFirst = null, appOpenLast = NOW_MS, dailyUser = false, newUser = false)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenFirst_CasualUser_FirstDays() = runTest {
        whenever { userManager.getAppOpenCount() } doReturn 9
        whenever { userManager.getAppOpenFirstOrNull() } doReturn NOW_MS - 9.days.inWholeMilliseconds

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(10, result)
        verify(userManager).set(appOpenCounts = 10, appOpenFirst = null, appOpenLast = NOW_MS, dailyUser = false, newUser = true)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenFirst_CasualUser_NextDays() = runTest {
        whenever { userManager.getAppOpenCount() } doReturn 9
        whenever { userManager.getAppOpenFirstOrNull() } doReturn NOW_MS - 11.days.inWholeMilliseconds

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(10, result)
        verify(userManager).set(appOpenCounts = 10, appOpenFirst = null, appOpenLast = NOW_MS, dailyUser = false, newUser = false)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenLast_LongTime_FewAppOpens() = runTest {
        whenever { userManager.getAppOpenCount() } doReturn 32
        whenever { userManager.getAppOpenFirstOrNull() } doReturn null
        whenever { userManager.getAppOpenLastOrNull() } doReturn NOW_MS - 100.days.inWholeMilliseconds

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(33, result)
        verify(userManager).set(appOpenCounts = 33, appOpenFirst = null, appOpenLast = NOW_MS, dailyUser = false, newUser = true)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenLast_LongTime_ManyAppOpens() = runTest {
        whenever { userManager.getAppOpenCount() } doReturn 34
        whenever { userManager.getAppOpenFirstOrNull() } doReturn null
        whenever { userManager.getAppOpenLastOrNull() } doReturn NOW_MS - 100.days.inWholeMilliseconds

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(35, result)
        verify(userManager).set(appOpenCounts = 35, appOpenFirst = null, appOpenLast = NOW_MS, dailyUser = false, newUser = false)
    }

    @Test
    fun test_getAndUpdateAppOpenCounts_NewUser_fromAppOpenLast_ShortTime_FewAppOpens() = runTest {
        whenever { userManager.getAppOpenCount() } doReturn 32
        whenever { userManager.getAppOpenFirstOrNull() } doReturn null
        whenever { userManager.getAppOpenLastOrNull() } doReturn NOW_MS - 98.days.inWholeMilliseconds

        val result = subject.getAndUpdateAppOpenCounts()

        assertEquals(33, result)
        verify(userManager).set(appOpenCounts = 33, appOpenFirst = null, appOpenLast = NOW_MS, dailyUser = false, newUser = false)
    }
}
