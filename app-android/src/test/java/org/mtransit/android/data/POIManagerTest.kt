package org.mtransit.android.data

import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.makeRDS
import org.mtransit.android.commons.data.makeSchedule
import org.mtransit.android.commons.data.toScheduleTimestamp
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.toMillis
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
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

    private val rds get() = makeRDS()

    @Test
    fun test_setStatus_fromNothing_toNoData() {
        rds.toPOIM(status = null).apply {
            setStatus(rds.mkSchedule(noData = true))
        }.statusOrNull.let { result ->
            assertNotNull(result)
            assertIs<Schedule>(result)
            assertTrue(result.isNoData)
        }
    }

    @Test
    fun test_setStatus_fromNoData_toUseful() {
        rds.toPOIM(status = rds.mkSchedule(noData = true)).apply {
            setStatus(rds.mkSchedule(timestamps = mkTimestampsUseful()))
        }.statusOrNull.let { result ->
            assertNotNull(result)
            assertIs<Schedule>(result)
            assertFalse(result.isNoData)
            assertTrue(result.isUseful)
        }
    }

    @Test
    fun test_setStatus_fromUseful_toNoData() {
        rds.toPOIM(status = rds.mkSchedule(timestamps = mkTimestampsUseful())).apply {
            setStatus(rds.mkSchedule(noData = true))
        }.statusOrNull.let { result ->
            assertNotNull(result)
            assertIs<Schedule>(result)
            assertFalse(result.isNoData)
            assertTrue(result.isUseful)
        }
    }

    @Test
    fun test_setStatus_fromNoService_toNoData() {
        rds.toPOIM(status = rds.mkSchedule()).apply {
            setStatus(rds.mkSchedule(noData = true))
        }.statusOrNull.let { result ->
            assertNotNull(result)
            assertIs<Schedule>(result)
            assertFalse(result.isNoData)
        }
    }

    @Test
    fun test_setStatus_fromNoData_toNoService() {
        rds.toPOIM(status = rds.mkSchedule(noData = true)).apply {
            setStatus(rds.mkSchedule())
        }.statusOrNull.let { result ->
            assertNotNull(result)
            assertIs<Schedule>(result)
            assertFalse(result.isNoData)
        }
    }

    @Test
    fun test_setStatus_fromMoreRecent_toOldUseful() {
        rds.toPOIM(status = rds.mkSchedule(readFromSourceAt = NOW - 1.minutes, timestamps = listOf(mkTime(NOW - 2.minutes)))).apply {
            setStatus(rds.mkSchedule(readFromSourceAtInMs = 0L, timestamps = mkTimestampsUseful()))
        }.statusOrNull.let { result ->
            assertNotNull(result)
            assertIs<Schedule>(result)
            assertTrue(result.isUseful)
            assertEquals(0L, result.readFromSourceAtInMs)
        }
    }

    private fun mkTimestampsUseful() = listOf(mkTime(NOW + 1.minutes))

    private fun RouteDirectionStop.mkSchedule(
        lastUpdate: Instant = NOW,
        lastUpdateInMs: Long = lastUpdate.toMillis(),
        maxValidityInMs: Long = 1.hours.inWholeMilliseconds,
        readFromSourceAt: Instant = NOW,
        readFromSourceAtInMs: Long = readFromSourceAt.toMillis(),
        providerPrecision: Duration = 1.minutes,
        providerPrecisionInMs: Long = providerPrecision.inWholeMilliseconds,
        sourceLabel: String = "test_source",
        noData: Boolean = false,
        timestamps: List<Schedule.Timestamp>? = null,
    ) = makeSchedule(
        lastUpdateInMs = lastUpdateInMs,
        maxValidityInMs = maxValidityInMs,
        readFromSourceAtInMs = readFromSourceAtInMs,
        providerPrecisionInMs = providerPrecisionInMs,
        localTimeZoneId = LOCAL_TZ_ID,
        sourceLabel = sourceLabel,
        noData = noData
    ).apply {
        timestamps?.let { setTimestampsAndSort(it) }
    }.toUI()

    private fun mkTime(time: Instant, tripId: String? = null, stopSeq: Int? = null, arrival: Instant? = null) =
        time.toScheduleTimestamp(LOCAL_TZ_ID, arrival, tripId, stopSeq)
}
