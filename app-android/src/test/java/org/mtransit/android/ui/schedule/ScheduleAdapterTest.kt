package org.mtransit.android.ui.schedule

import org.mockito.kotlin.mock
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.toScheduleTimestamp
import org.mtransit.android.commons.data.updateDepartureForRealTime
import org.mtransit.android.commons.millisToInstant
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

        private const val NOW_IN_MS = 1772727133_000L // Thursday, March 5, 2026 at 11:12:13 a.m. UTC-05:00
        private val DEPARTURE = 1772722800_000L.millisToInstant() // Thursday, March 5, 2026 at 10:00:00 a.m. UTC-05:00

        private const val TRIP_ID = "123456789"
    }

    private val subject = ScheduleAdapter(
        serviceUpdateLoader = mock {},
    ).apply {
        this.callNotifyDataSetChanged = {}
    }

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(isAndroidPlatform = false)
        TimeUtils.setOverrideCurrentTimeMillis(NOW_IN_MS)
        subject.onTimeChanged(NOW_IN_MS)
    }

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
            add(mkTime(startsAt + 150.minutes, "trip5").apply { updateDepartureForRealTime(startsAt + 200.minutes) })
        }.sortedWith(Schedule.TIMESTAMPS_COMPARATOR)
        subject.localTimeZone = TimeZone.getTimeZone(LOCAL_TZ_ID) // calls subject.updateTimes()

        assertEquals(1, notifyDataSetChangedCalled)
        assertNotNull(subject.dayToHourToTimestamps.firstOrNull { it.first == 1772686800000L }) { (_, hourToTimes) ->
            assertNotNull(hourToTimes[10]) {
                assertEquals(2, it.size)
                assertEquals("trip0", it[0].tripId) // 10:20 am
                assertEquals("trip1", it[1].tripId) // 10:30 am
            }
            assertNotNull(hourToTimes[11]) {
                assertEquals(2, it.size)
                assertEquals("trip3", it[0].tripId) // 11:30 am
                assertEquals("trip2", it[1].tripId) // 11:20 am late by 20 minutes
            }
            assertNotNull(hourToTimes[12]) {
                assertEquals(1, it.size)
                assertEquals("trip4", it[0].tripId) // 12:20 pm
            }
            assertNotNull(hourToTimes[13]) {
                assertEquals(1, it.size)
                assertEquals("trip5", it[0].tripId) // 12:30 pm late by 50 minutes
            }
        }
        assertEquals("trip3", subject.nextTimestamp?.tripId)

        subject.hideRealTime = true // triggers subject.updateTimes()
        assertEquals(2, notifyDataSetChangedCalled)
        assertNotNull(subject.dayToHourToTimestamps.firstOrNull { it.first == 1772686800000L }) { (_, hourToTimes) ->
            assertNotNull(hourToTimes[10]) {
                assertEquals(2, it.size)
                assertEquals("trip0", it[0].tripId) // 10:20 am
                assertEquals("trip1", it[1].tripId) // 10:30 am
            }
            assertNotNull(hourToTimes[11]) {
                assertEquals(2, it.size)
                assertEquals("trip2", it[0].tripId) // 11:20 am late by 20 minutes
                assertEquals("trip3", it[1].tripId) // 11:30 am
            }
            assertNotNull(hourToTimes[12]) {
                assertEquals(2, it.size)
                assertEquals("trip4", it[0].tripId) // 12:20 pm
                assertEquals("trip5", it[1].tripId) // 12:30 pm late by 50 minutes
            }
        }
        assertEquals("trip2", subject.nextTimestamp?.tripId)
    }

    private fun mkTime(time: Instant, tripId: String? = TRIP_ID, stopSeq: Int? = null, arrival: Instant? = null) =
        time.toScheduleTimestamp(LOCAL_TZ_ID, arrival, tripId, stopSeq)
}
