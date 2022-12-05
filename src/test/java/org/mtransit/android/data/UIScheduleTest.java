package org.mtransit.android.data;

import org.junit.Test;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.Schedule.Timestamp;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.data.UISchedule.TimeSections;
import org.mtransit.commons.FeatureFlags;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

public class UIScheduleTest {

	private static final String TARGET_UUID = POI.POIUtils.getUUID("authority", 1);
	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long NOW_IN_MS = TimeUnit.SECONDS.toMillis(1534681140L); // Aug 19, 2018 8:19:00 AM EST
	// private static final long NOW_IN_MS = TimeUnit.SECONDS.toMillis(1000000000L); // Sept 9, 2001 9:46:40 PM EST
	private static final long NOW_TO_THE_MINUTE = TimeUtils.timeToTheMinuteMillis(NOW_IN_MS); // data & UI is 99% precise to the minute (if not less precise)
	private static final long AFTER_IN_MS = TimeUtils.timeToTheMinuteMillis(NOW_IN_MS);

	private static final Long MIN_COVERAGE_IN_MS = TimeUnit.MINUTES.toMillis(30L);
	private static final Long MAX_COVERAGE_IN_MS = null;
	private static final Integer MIN_COUNT = 10;
	private static final Integer MAX_COUNT = null;

	@Test
	public void testFindTimesSectionsStartEnd_Simple_All() {
		// Arrange
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-90L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(90L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(150L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(210L)));
		// Act
		TimeSections result = UISchedule.findTimesSectionsStartEnd(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, timestamps);
		// Assert
		assertEquals(0, result.previousTimesStartIdx);
		assertEquals(1, result.previousTimesEndIdx);
		assertEquals(1, result.previousTimeStartIdx);
		assertEquals(2, result.previousTimeEndIdx);
		assertEquals(2, result.nextTimeStartIdx);
		assertEquals(3, result.nextTimeEndIdx);
		assertEquals(3, result.nextNextTimeStartIdx);
		assertEquals(4, result.nextNextTimeEndIdx);
		assertEquals(4, result.afterNextTimesStartIdx);
		assertEquals(5 + 1, result.afterNextTimesEndIdx);
	}

	@Test
	public void testFindTimesSectionsStartEnd_Simple_NoPreviousList() {
		// Arrange
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(90L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(150L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(210L)));
		// Act
		TimeSections result = UISchedule.findTimesSectionsStartEnd(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, timestamps);
		// Assert
		assertEquals(0, result.previousTimesEndIdx - result.previousTimesStartIdx);
		assertEquals(0, result.previousTimeStartIdx);
		assertEquals(1, result.previousTimeEndIdx);
		assertEquals(1, result.nextTimeStartIdx);
		assertEquals(2, result.nextTimeEndIdx);
		assertEquals(2, result.nextNextTimeStartIdx);
		assertEquals(3, result.nextNextTimeEndIdx);
		assertEquals(3, result.afterNextTimesStartIdx);
		assertEquals(4 + 1, result.afterNextTimesEndIdx);
	}

	@Test
	public void testFindTimesSectionsStartEnd_Simple_NoPrevious() {
		// Arrange
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(90L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(150L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(210L)));
		// Act
		TimeSections result = UISchedule.findTimesSectionsStartEnd(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, timestamps);
		// Assert
		assertEquals(0, result.previousTimesEndIdx - result.previousTimesStartIdx);
		assertEquals(0, result.previousTimeEndIdx - result.previousTimeStartIdx);
		assertEquals(0, result.nextTimeStartIdx);
		assertEquals(1, result.nextTimeEndIdx);
		assertEquals(1, result.nextNextTimeStartIdx);
		assertEquals(2, result.nextNextTimeEndIdx);
		assertEquals(2, result.afterNextTimesStartIdx);
		assertEquals(3 + 1, result.afterNextTimesEndIdx);
	}

	@Test
	public void testFindTimesSectionsStartEnd_Simple_NoNextNext() {
		// Arrange
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-90L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)));
		// Act
		TimeSections result = UISchedule.findTimesSectionsStartEnd(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, timestamps);
		// Assert
		assertEquals(0, result.previousTimesStartIdx);
		assertEquals(1, result.previousTimesEndIdx);
		assertEquals(1, result.previousTimeStartIdx);
		assertEquals(2, result.previousTimeEndIdx);
		assertEquals(2, result.nextTimeStartIdx);
		assertEquals(3, result.nextTimeEndIdx);
		assertEquals(0, result.nextNextTimeEndIdx - result.nextNextTimeStartIdx);
		assertEquals(0, result.afterNextTimesEndIdx - result.afterNextTimesStartIdx);
	}

	@Test
	public void testFindTimesSectionsStartEnd_Simple_NoNextList() {
		// Arrange
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-90L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(90L)));
		// Act
		TimeSections result = UISchedule.findTimesSectionsStartEnd(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, timestamps);
		// Assert
		assertEquals(0, result.previousTimesStartIdx);
		assertEquals(1, result.previousTimesEndIdx);
		assertEquals(1, result.previousTimeStartIdx);
		assertEquals(2, result.previousTimeEndIdx);
		assertEquals(2, result.nextTimeStartIdx);
		assertEquals(3, result.nextTimeEndIdx);
		assertEquals(3, result.nextNextTimeStartIdx);
		assertEquals(4, result.nextNextTimeEndIdx);
		assertEquals(0, result.afterNextTimesEndIdx - result.afterNextTimesStartIdx);
	}

	@Test
	public void testFindTimesSectionsStartEnd_Simple_1NextList() {
		// Arrange
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-90L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(90L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(150L)));
		// Act
		TimeSections result = UISchedule.findTimesSectionsStartEnd(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, timestamps);
		// Assert
		assertEquals(0, result.previousTimesStartIdx);
		assertEquals(1, result.previousTimesEndIdx);
		assertEquals(1, result.previousTimeStartIdx);
		assertEquals(2, result.previousTimeEndIdx);
		assertEquals(2, result.nextTimeStartIdx);
		assertEquals(3, result.nextTimeEndIdx);
		assertEquals(3, result.nextNextTimeStartIdx);
		assertEquals(4, result.nextNextTimeEndIdx);
		assertEquals(4, result.afterNextTimesStartIdx);
		assertEquals(4 + 1, result.afterNextTimesEndIdx);
	}

	@Test
	public void testFindTimesSectionsStartEnd_DropOffOnly_Next() {
		// Arrange
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-30L)));
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)).setHeadsign(Trip.HEADSIGN_TYPE_NO_PICKUP, null));
		} else {
			timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)));
		}
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(90L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(150L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(210L)));
		// Act
		TimeSections result = UISchedule.findTimesSectionsStartEnd(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, timestamps);
		// Assert
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			assertEquals(2, result.nextTimeStartIdx);
			assertEquals(3, result.nextTimeEndIdx);
		} else {
			assertEquals(1, result.nextTimeStartIdx);
			assertEquals(2, result.nextTimeEndIdx);
		}
	}

	@Test
	public void testFindTimesSectionsStartEnd_DropOffOnly_NextNext() {
		// Arrange
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-30L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)));
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(90L)).setHeadsign(Trip.HEADSIGN_TYPE_NO_PICKUP, null));
		} else {
			timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(90L)));
		}
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(150L)));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(210L)));
		// Act
		TimeSections result = UISchedule.findTimesSectionsStartEnd(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, timestamps);
		// Assert
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			assertEquals(3, result.nextNextTimeStartIdx);
			assertEquals(4, result.nextNextTimeEndIdx);
		} else {
			assertEquals(2, result.nextNextTimeStartIdx);
			assertEquals(3, result.nextNextTimeEndIdx);
		}
	}

	@Test
	public void testGetNextTimestamps() {
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-5L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = schedule.getNextTimestamps(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		// Assert
		assertEquals(3, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L), result.get(0).t);
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L), result.get(1).t);
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulLast() {
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-2L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		// Assert
		assertEquals(4, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-2L), result.get(0).getT());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUselessLast() {
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-10L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		// Assert
		assertEquals(3, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L), result.get(0).getT());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithMultipleUsefulLast() {
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-3L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT);
		// Assert
		assertEquals(4, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L), result.get(0).getT());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulLastDuplicates() {
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-2L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		// Assert
		assertEquals(4, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L), result.get(0).getT());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulNextDuplicates() {
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-20L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		// Assert
		assertEquals(3, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L), result.get(0).getT());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulNearDuplicates() {
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L) - 1L));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L) + 1L));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		// Assert
		assertEquals(4, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L), result.get(0).getT());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithUsefulNextNearDuplicates() {
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-10L) - 1L));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-10L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L) + 1L));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		// Assert
		// TODO FIX TEST: code NOT yet compatible with time NOT the minute
		assertEquals(3, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L), result.get(0).getT());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithBusAtStop() { // fails if timestamp times are NOT to the minute
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(0L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(3L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		// Assert
		assertEquals(5, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(0L), result.get(0).getT());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(3L), result.get(1).getT());
	}

	@Test
	public void testGetStatusNextTimestampsWithNearDuplicates() {
		// Arrange
		UISchedule schedule = new UISchedule(null, TARGET_UUID, -1L, -1L, -1L, PROVIDER_PRECISION_IN_MS, false, false);
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-2L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(2L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(3L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(4L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(7L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(13L)));
		schedule.addTimestampWithoutSort(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(24L)));
		schedule.sortTimestamps();
		// Act
		ArrayList<Timestamp> result = UISchedule.filterStatusNextTimestampsTimes( //
				schedule.getStatusNextTimestamps(AFTER_IN_MS, MIN_COVERAGE_IN_MS, MAX_COVERAGE_IN_MS, MIN_COUNT, MAX_COUNT));
		// Assert
		assertEquals(7, result.size());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-1L), result.get(0).getT());
		assertEquals(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(2L), result.get(1).getT());
	}

	// Kelowna 1 -> Downtown | Stop Queensway Exch
	@Test
	public void testFindTimesSectionsStartEnd_All_DropOffOnly() {
		// Arrange
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(-30L)).setHeadsign(Trip.HEADSIGN_TYPE_NO_PICKUP, null));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(30L)).setHeadsign(Trip.HEADSIGN_TYPE_NO_PICKUP, null));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(90L)).setHeadsign(Trip.HEADSIGN_TYPE_NO_PICKUP, null));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(150L)).setHeadsign(Trip.HEADSIGN_TYPE_NO_PICKUP, null));
		timestamps.add(new Timestamp(NOW_TO_THE_MINUTE + TimeUnit.MINUTES.toMillis(210L)).setHeadsign(Trip.HEADSIGN_TYPE_NO_PICKUP, null));
		// Act
		TimeSections result = UISchedule.findTimesSectionsStartEnd(AFTER_IN_MS - PROVIDER_PRECISION_IN_MS, timestamps);
		// Assert
		assertEquals(0, result.previousTimesStartIdx);
		assertEquals(0, result.previousTimesEndIdx);
		assertEquals(0, result.previousTimeStartIdx);
		assertEquals(1, result.previousTimeEndIdx);
		assertEquals(1, result.nextTimeStartIdx);
		assertEquals(2, result.nextTimeEndIdx);
		assertEquals(2, result.nextNextTimeStartIdx);
		assertEquals(3, result.nextNextTimeEndIdx);
		assertEquals(3, result.afterNextTimesStartIdx);
		assertEquals(5, result.afterNextTimesEndIdx);
	}
}