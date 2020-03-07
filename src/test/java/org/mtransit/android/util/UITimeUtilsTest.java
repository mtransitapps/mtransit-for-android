package org.mtransit.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;

import androidx.core.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mtransit.android.R;

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
		UITimeUtils.cleanTimes(input, output);
		verify(output).setSpan(any(RelativeSizeSpan.class), eq(5), eq(9), anyInt());
	}

	@Test
	public void testGetShortTimeSpanNumber() {
		//
		Context context = mock(Context.class);
		Resources resources = mock(Resources.class);
		when(context.getResources()).thenReturn(resources);
		SpannableStringBuilder shortTimeSpan1SSB = mock(SpannableStringBuilder.class);
		SpannableStringBuilder shortTimeSpan2SSB = mock(SpannableStringBuilder.class);
		long diffInMs = TimeUnit.MINUTES.toMillis(9L);
		long precisionInMs = TimeUnit.MINUTES.toMillis(1L);
		when(resources.getQuantityString(R.plurals.minutes_capitalized, 9)).thenReturn("Minutes");
		//
		Pair<CharSequence, CharSequence> result = UITimeUtils.getShortTimeSpanNumber(context, diffInMs, precisionInMs, shortTimeSpan1SSB, shortTimeSpan2SSB);
		//
		verify(shortTimeSpan1SSB).append(eq("9"));
		verify(shortTimeSpan2SSB).append(eq("Minutes"));
	}

}