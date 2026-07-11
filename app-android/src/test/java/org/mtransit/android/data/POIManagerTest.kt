package org.mtransit.android.data

import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.makeRDS
import org.mtransit.android.commons.data.makeSchedule
import org.mtransit.android.commons.data.toScheduleTimestamp
import org.mtransit.android.commons.millisToInstant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class POIManagerTest {

    companion object {
        private const val LOCAL_TZ_ID: String = "America/Montreal"

        private const val NOW_MS = 123456789_000L
        private val NOW = NOW_MS.millisToInstant()
    }

    @BeforeTest
    fun setUp() {
        TimeUtils.setOverrideCurrentTimeMillis(NOW_MS)
    }

    @Test
    fun test_setStatus() {
        val rds = makeRDS()
        // from no status -> status w/ no data
        rds.toPOIM(status = null).apply {
            setStatus(rds.mkSchedule(hasData = false))
        }.statusOrNull?.let { result ->
            assertNotNull(result)
            assertIs<Schedule>(result)
            assertFalse(result.hasData())
        }
        // from staus no data -> status useful
        rds.toPOIM(status = rds.mkSchedule(hasData = false)).apply {
            setStatus(rds.mkSchedule(hasData = true, timestamps = mkTimestampsUseful()))
        }.statusOrNull?.let { result ->
            assertNotNull(result)
            assertIs<Schedule>(result)
            assertTrue(result.hasData())
            assertTrue(result.isUseful)
        }
        // from status useful -> ignore new no data
        rds.toPOIM(status = rds.mkSchedule(hasData = true, timestamps = mkTimestampsUseful())).apply {
            setStatus(rds.mkSchedule(hasData = false))
        }.statusOrNull?.let { result ->
            assertNotNull(result)
            assertIs<Schedule>(result)
            assertTrue(result.hasData())
            assertTrue(result.isUseful)
        }
    }

    private fun mkTimestampsUseful() = listOf(mkTime(NOW + 1.minutes))

    private fun RouteDirectionStop.mkSchedule(
        lastUpdateInMs: Long = NOW_MS,
        validityInMs: Long = 1.hours.inWholeMilliseconds,
        readFromSourceAtInMs: Long = NOW_MS,
        providerPrecisionInMs: Long = 1.minutes.inWholeMilliseconds,
        sourceLabel: String = "test_source",
        hasData: Boolean = true,
        timestamps: List<Schedule.Timestamp>? = null,
    ) = makeSchedule(
        lastUpdateInMs = lastUpdateInMs,
        validityInMs = validityInMs,
        readFromSourceAtInMs = readFromSourceAtInMs,
        providerPrecisionInMs = providerPrecisionInMs,
        sourceLabel = sourceLabel,
        hasData = hasData
    ).apply {
        timestamps?.let { setTimestampsAndSort(it) }
    }

    private fun mkTime(time: Instant, tripId: String? = null, stopSeq: Int? = null, arrival: Instant? = null) =
        time.toScheduleTimestamp(LOCAL_TZ_ID, arrival, tripId, stopSeq)
}
