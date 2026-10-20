package org.mtransit.android.ui.schedule

import org.mockito.kotlin.mock
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.toScheduleTimestamp
import org.mtransit.android.commons.data.updateDepartureForRealTime
import org.mtransit.android.commons.secsToInstant
import org.mtransit.commons.CommonsApp
import java.util.TimeZone
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ScheduleAdapterTest {

    companion object {
        private const val LOCAL_TZ_ID = "America/Montreal"
        private const val NOW_IN_MS = 1772730794_000L // Thursday, March 5, 2026 at 12:13:14 p.m. UTC-05:00
        private val DEPARTURE = 1772722800L.secsToInstant() // 2026-03-06 10:00
        private const val TRIP_ID = "123456789"
    }

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(isAndroidPlatform = false)
        TimeUtils.setOverrideCurrentTimeMillis(NOW_IN_MS)
    }

    private val subject = ScheduleAdapter(
        serviceUpdateLoader = mock {},
    )

    @Test
    fun test_updateTimes() {
        var notifyDataSetChangedCalled = 0
        subject.callNotifyDataSetChanged = {
            notifyDataSetChangedCalled++
        }
        val startsAt = DEPARTURE
        subject.timestamps = buildList {
            add(mkTime(startsAt + 20.minutes, "trip0"))
            add(mkTime(startsAt + 30.minutes, "trip1"))
            add(mkTime(startsAt + 80.minutes, "trip2").apply { updateDepartureForRealTime(startsAt + 100.minutes) })
            add(mkTime(startsAt + 90.minutes, "trip3"))
            add(mkTime(startsAt + 140.minutes, "trip4"))
            add(mkTime(startsAt + 150.minutes, "trip5"))
        }.sortedWith(Schedule.TIMESTAMPS_COMPARATOR)
        subject.localTimeZone = TimeZone.getTimeZone(LOCAL_TZ_ID) // calls subject.updateTimes()

        assertEquals(1, notifyDataSetChangedCalled)
        assertNotNull(subject.dayToHourToTimestamps.firstOrNull { it.first == 1772686800000L }) { (_, hourToTimes) ->
            assertNotNull(hourToTimes[10]) {
                assertEquals(2, it.size)
                assertEquals("trip0", it[0].tripId)
                assertEquals("trip1", it[1].tripId)
            }
            assertNotNull(hourToTimes[11]) {
                assertEquals(2, it.size)
                assertEquals("trip3", it[0].tripId)
                assertEquals("trip2", it[1].tripId)
            }
            assertNotNull(hourToTimes[12]) {
                assertEquals(2, it.size)
                assertEquals("trip4", it[0].tripId)
                assertEquals("trip5", it[1].tripId)
            }
        }

        subject.hideRealTime = true // triggers subject.updateTimes()
        assertEquals(2, notifyDataSetChangedCalled)
        assertNotNull(subject.dayToHourToTimestamps.firstOrNull { it.first == 1772686800000L }) { (_, hourToTimes) ->
            assertNotNull(hourToTimes[10]) {
                assertEquals(2, it.size)
                assertEquals("trip0", it[0].tripId)
                assertEquals("trip1", it[1].tripId)
            }
            assertNotNull(hourToTimes[11]) {
                assertEquals(2, it.size)
                assertEquals("trip2", it[0].tripId)
                assertEquals("trip3", it[1].tripId)
            }
            assertNotNull(hourToTimes[12]) {
                assertEquals(2, it.size)
                assertEquals("trip4", it[0].tripId)
                assertEquals("trip5", it[1].tripId)
            }
        }
    }

    private fun mkTime(time: Instant, tripId: String? = TRIP_ID, stopSeq: Int? = null, arrival: Instant? = null) =
        time.toScheduleTimestamp(LOCAL_TZ_ID, arrival, tripId, stopSeq)
}
