package org.mtransit.android.util;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;

import androidx.core.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mtransit.android.R;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.commons.FeatureFlags;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UITimeUtilsTest {

	@Test
	public void testCleanTimes() {
		// Arrange
		String input;
		SpannableStringBuilder output;
		//
		input = "2:06 am";
		output = mock(SpannableStringBuilder.class);
		when(output.length()).thenReturn(input.length());
		UITimeUtils.cleanTimes(input, output);
		verify(output).setSpan(any(RelativeSizeSpan.class), eq(5), eq(7), anyInt());
		//
		input = "2:06 pm";
		output = mock(SpannableStringBuilder.class);
		when(output.length()).thenReturn(input.length());
		UITimeUtils.cleanTimes(input, output);
		verify(output).setSpan(any(RelativeSizeSpan.class), eq(5), eq(7), anyInt());
		//
		input = "2:06 a.m.";
		output = mock(SpannableStringBuilder.class);
		when(output.length()).thenReturn(input.length());
		UITimeUtils.cleanTimes(input, output);
		verify(output).setSpan(any(RelativeSizeSpan.class), eq(5), eq(9), anyInt());
		//
		input = "2:06 p.m.";
		output = mock(SpannableStringBuilder.class);
		when(output.length()).thenReturn(input.length());
		// Act
		UITimeUtils.cleanTimes(input, output);
		// Assert
		verify(output).setSpan(any(RelativeSizeSpan.class), eq(5), eq(9), anyInt());
	}

	@SuppressWarnings({"ConstantConditions", "unused"})
	@Test
	public void testGetShortTimeSpanNumber() {
		// Arrange
		Context context = mock(Context.class);
		Resources resources = mock(Resources.class);
		when(context.getResources()).thenReturn(resources);
		SpannableStringBuilder shortTimeSpan1SSB = mock(SpannableStringBuilder.class);
		SpannableStringBuilder shortTimeSpan2SSB = mock(SpannableStringBuilder.class);
		long diffInMs = TimeUnit.MINUTES.toMillis(9L);
		long precisionInMs = TimeUnit.MINUTES.toMillis(1L);
		boolean isRealTime = false;
		boolean isOldSchedule = false;
		when(resources.getQuantityString(R.plurals.minutes_capitalized, 9)).thenReturn("Minutes");
		// Act
		Pair<CharSequence, CharSequence> result = UITimeUtils.getShortTimeSpanNumber(context,
				diffInMs, precisionInMs,
				isRealTime, isOldSchedule,
				shortTimeSpan1SSB, shortTimeSpan2SSB);
		// Assert
		verify(shortTimeSpan1SSB).append(eq("9"));
		verify(shortTimeSpan2SSB).append(eq("Minutes"));
	}

	@Test
	public void test_isFrequentService() {
		// Arrange
		long now = System.currentTimeMillis();
		ArrayList<Schedule.Timestamp> timestamps = new ArrayList<>();
		long providerFSMinDurationInMs = TimeUnit.MINUTES.toMillis(15L);
		long providerFSTimeSpanInMs = TimeUnit.MINUTES.toMillis(3L);
		timestamps.add(new Schedule.Timestamp(now + -1L * providerFSTimeSpanInMs));
		//noinspection ConstantConditions,PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs));
		//noinspection PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 1L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 2L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 3L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 4L * providerFSTimeSpanInMs));
		// Act
		boolean result = UITimeUtils.isFrequentService(timestamps, providerFSMinDurationInMs, providerFSTimeSpanInMs);
		// Assert
		assertTrue(result);
	}

	@Test
	public void test_isFrequentService_DropOffOnly_First() {
		// Arrange
		long now = System.currentTimeMillis();
		ArrayList<Schedule.Timestamp> timestamps = new ArrayList<>();
		long providerFSMinDurationInMs = TimeUnit.MINUTES.toMillis(15L);
		long providerFSTimeSpanInMs = TimeUnit.MINUTES.toMillis(3L);
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			timestamps.add(new Schedule.Timestamp(now + -1L * providerFSTimeSpanInMs).setHeadsign(Trip.HEADSIGN_TYPE_DESCENT_ONLY, null));
		} else {
			timestamps.add(new Schedule.Timestamp(now + -1L * providerFSTimeSpanInMs));
		}
		//noinspection ConstantConditions,PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs));
		//noinspection PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 1L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 2L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 3L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 4L * providerFSTimeSpanInMs));
		// Act
		boolean result = UITimeUtils.isFrequentService(timestamps, providerFSMinDurationInMs, providerFSTimeSpanInMs);
		// Assert
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			assertFalse(result);
		} else {
			assertTrue(result);
		}
	}

	@Test
	public void test_isFrequentService_DropOffOnly_Second() {
		// Arrange
		long now = System.currentTimeMillis();
		ArrayList<Schedule.Timestamp> timestamps = new ArrayList<>();
		long providerFSMinDurationInMs = TimeUnit.MINUTES.toMillis(15L);
		long providerFSTimeSpanInMs = TimeUnit.MINUTES.toMillis(3L);
		timestamps.add(new Schedule.Timestamp(now + -1L * providerFSTimeSpanInMs));
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			//noinspection ConstantConditions,PointlessArithmeticExpression
			timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs).setHeadsign(Trip.HEADSIGN_TYPE_DESCENT_ONLY, null));
		} else {
			//noinspection ConstantConditions,PointlessArithmeticExpression
			timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs));
		}
		//noinspection PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 1L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 2L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 3L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 4L * providerFSTimeSpanInMs));
		// Act
		boolean result = UITimeUtils.isFrequentService(timestamps, providerFSMinDurationInMs, providerFSTimeSpanInMs);
		// Assert
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			assertFalse(result);
		} else {
			assertTrue(result);
		}
	}

	@Test
	public void test_isFrequentService_DropOffOnly_Second_CloseNext() {
		// Arrange
		long now = System.currentTimeMillis();
		ArrayList<Schedule.Timestamp> timestamps = new ArrayList<>();
		long providerFSMinDurationInMs = TimeUnit.MINUTES.toMillis(15L);
		long providerFSTimeSpanInMs = TimeUnit.MINUTES.toMillis(3L);
		timestamps.add(new Schedule.Timestamp(now + -1L * providerFSTimeSpanInMs));
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			//noinspection ConstantConditions,PointlessArithmeticExpression
			timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs - 1L));
			//noinspection ConstantConditions,PointlessArithmeticExpression
			timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs).setHeadsign(Trip.HEADSIGN_TYPE_DESCENT_ONLY, null));
			//noinspection ConstantConditions,PointlessArithmeticExpression
			timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs + 1L));
		} else {
			//noinspection ConstantConditions,PointlessArithmeticExpression
			timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs));
		}
		//noinspection PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 1L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 2L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 3L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 4L * providerFSTimeSpanInMs));
		// Act
		boolean result = UITimeUtils.isFrequentService(timestamps, providerFSMinDurationInMs, providerFSTimeSpanInMs);
		// Assert
		assertTrue(result);
	}

	@Test
	public void test_isFrequentService_DropOffOnly_Last() {
		// Arrange
		long now = System.currentTimeMillis();
		ArrayList<Schedule.Timestamp> timestamps = new ArrayList<>();
		long providerFSMinDurationInMs = TimeUnit.MINUTES.toMillis(15L);
		long providerFSTimeSpanInMs = TimeUnit.MINUTES.toMillis(3L);
		timestamps.add(new Schedule.Timestamp(now + -1L * providerFSTimeSpanInMs));
		//noinspection ConstantConditions,PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs));
		//noinspection PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 1L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 2L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 3L * providerFSTimeSpanInMs));
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			timestamps.add(new Schedule.Timestamp(now + 4L * providerFSTimeSpanInMs).setHeadsign(Trip.HEADSIGN_TYPE_DESCENT_ONLY, null));
		} else {
			timestamps.add(new Schedule.Timestamp(now + 4L * providerFSTimeSpanInMs));
		}
		// Act
		boolean result = UITimeUtils.isFrequentService(timestamps, providerFSMinDurationInMs, providerFSTimeSpanInMs);
		// Assert
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			assertFalse(result);
		} else {
			assertTrue(result);
		}
	}

	@Test
	public void test_isFrequentService_DropOffOnly_After() {
		// Arrange
		long now = System.currentTimeMillis();
		ArrayList<Schedule.Timestamp> timestamps = new ArrayList<>();
		long providerFSMinDurationInMs = TimeUnit.MINUTES.toMillis(15L);
		long providerFSTimeSpanInMs = TimeUnit.MINUTES.toMillis(3L);
		timestamps.add(new Schedule.Timestamp(now + -1L * providerFSTimeSpanInMs));
		//noinspection ConstantConditions,PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 0L * providerFSTimeSpanInMs));
		//noinspection PointlessArithmeticExpression
		timestamps.add(new Schedule.Timestamp(now + 1L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 2L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 3L * providerFSTimeSpanInMs));
		timestamps.add(new Schedule.Timestamp(now + 4L * providerFSTimeSpanInMs));
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			timestamps.add(new Schedule.Timestamp(now + 5L * providerFSTimeSpanInMs).setHeadsign(Trip.HEADSIGN_TYPE_DESCENT_ONLY, null));
		} else {
			timestamps.add(new Schedule.Timestamp(now + 5L * providerFSTimeSpanInMs));
		}
		// Act
		boolean result = UITimeUtils.isFrequentService(timestamps, providerFSMinDurationInMs, providerFSTimeSpanInMs);
		// Assert
		assertTrue(result);
	}
}