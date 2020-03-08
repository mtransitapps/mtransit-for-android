package org.mtransit.android.data;

import org.junit.Test;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.Schedule.Timestamp;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

public class UIScheduleTest {

	private static final String TARGET_UUID = POI.POIUtils.getUUID("authority", 1);
	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long NOW_IN_MS = TimeUnit.SECONDS.toMillis(1534681140L); // August 19, 2018 8:19 EST
	private static final long AFTER_IN_MS = TimeUtils.timeToTheMinuteMillis(NOW_IN_MS);

	private static final Long MIN_COVERAGE_IN_MS = TimeUnit.MINUTES.toMillis(30L);
	private static final Long MAX_COVERAGE_IN_MS = null;
	private static final Integer MIN_COUNT = 10;
	private static final Integer MAX_COUNT = null;

	@Test
	public void testGetNextTimestamps() {
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-5L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = schedule.getNextTimestamps(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		//
		assertEquals(3, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).t);
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).t);
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulLast() {
		//
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-2L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		//
		assertEquals(4, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-2L), result.get(0).getT());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUselessLast() {
		//
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-10L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		//
		assertEquals(3, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(0).getT());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithMultipleUsefulLast() {
		//
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-3L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		//
		assertEquals(4, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).getT());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulLastDuplicates() {
		//
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-2L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		// schedule.addTimestampWithoutSort(new Schedule.Timestamp(now + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		//
		assertEquals(4, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).getT());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulNextDuplicates() {
		//
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-20L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		//
		assertEquals(3, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(0).getT());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulNearDuplicates() {
		//
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L) - 1L));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L) + 1L));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		//
		assertEquals(4, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).getT());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulNextNearDuplicates() {
		//
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-10L) - 1L));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-10L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L) + 1L));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		//
		// TODO FIX TEST
		assertEquals(3, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L), result.get(0).getT());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithBusAtStop() {
		//
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(0L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(3L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		//
		assertEquals(5, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(0L), result.get(0).getT());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(3L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithNearDuplicates() {
		//
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-2L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(2L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(3L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(4L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_IN_MS + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		//
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		//
		assertEquals(7, result.size());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(-1L), result.get(0).getT());
		assertEquals(NOW_IN_MS + TimeUnit.MINUTES.toMillis(2L), result.get(1).getT());
	}
}