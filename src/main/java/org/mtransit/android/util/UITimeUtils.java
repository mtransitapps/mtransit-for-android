package org.mtransit.android.util;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Schedule.Timestamp;
import org.mtransit.android.data.UISchedule;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UITimeUtils extends org.mtransit.android.commons.TimeUtils implements MTLog.Loggable {

	private static final String LOG_TAG = UITimeUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final long RECENT_IN_MILLIS = DateUtils.HOUR_IN_MILLIS;

	private static final long MAX_DURATION_DISPLAYED_IN_MS = TimeUnit.HOURS.toMillis(6L);
	private static final long URGENT_SCHEDULE_IN_MIN = 10L;
	public static final long URGENT_SCHEDULE_IN_MS = TimeUnit.MINUTES.toMillis(URGENT_SCHEDULE_IN_MIN);

	public static final long FREQUENT_SERVICE_TIME_SPAN_IN_MS_DEFAULT = TimeUnit.MINUTES.toMillis(5L);
	private static final long FREQUENT_SERVICE_MIN_DURATION_IN_MS_DEFAULT = TimeUnit.MINUTES.toMillis(30L);
	private static final long FREQUENT_SERVICE_MIN_SERVICE = 2;

	@NonNull
	public static IntentFilter TIME_CHANGED_INTENT_FILTER;

	static {
		TIME_CHANGED_INTENT_FILTER = new IntentFilter();
		TIME_CHANGED_INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
		TIME_CHANGED_INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		TIME_CHANGED_INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
	}

	private static final Pattern TIME_W_SECONDS = Pattern.compile("([0-9]{1,2}:[0-9]{2}:[0-9]{2})", Pattern.CASE_INSENSITIVE);

	@SuppressLint("ConstantLocale")
	// will only be an issue if user switch language w/o re-starting app
	private static final ThreadSafeDateFormatter STANDALONE_DAY_OF_THE_WEEK_LONG = new ThreadSafeDateFormatter("EEEE", Locale.getDefault());

	@SuppressLint("ConstantLocale")
	// will only be an issue if user switch language w/o re-starting app
	private static final ThreadSafeDateFormatter STANDALONE_MONTH_LONG = new ThreadSafeDateFormatter("LLLL", Locale.getDefault());

	private static final String AM = "am";
	private static final String A_M_ = "a.m.";
	private static final String PM = "pm";
	private static final String P_M_ = "p.m.";
	private static final List<String> AM_PM_LIST = Arrays.asList( //
			AM, A_M_, //
			PM, P_M_ //
	);

	private static final long MILLIS_IN_SEC = 1000L;
	private static final int SEC_IN_MIN = 60;
	private static final int MIN_IN_HOUR = 60;
	private static final int HOUR_IN_DAY = 24;

	private static final long MAX_MINUTES_SHOWED = 99L;
	private static final int MAX_HOURS_SHOWED = 99;
	public static final long MAX_DURATION_SHOW_NUMBER_IN_MS = TimeUnit.MINUTES.toMillis(MAX_MINUTES_SHOWED);

	private static RelativeSizeSpan TIME_UNIT_SIZE = SpanUtils.getNew50PercentSizeSpan();

	private static TypefaceSpan TIME_UNIT_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	@Nullable
	private static TextAppearanceSpan urgentTime1TextAppearance = null;

	@NonNull
	private static TextAppearanceSpan getUrgentTime1TextAppearance(@NonNull Context context) {
		if (urgentTime1TextAppearance == null) {
			urgentTime1TextAppearance = SpanUtils.getNewLargeTextAppearance(context);
		}
		return urgentTime1TextAppearance;
	}

	@Nullable
	private static TextAppearanceSpan urgentTime2TextAppearance = null;

	private static TextAppearanceSpan getUrgentTime2TextAppearance(@NonNull Context context) {
		if (urgentTime2TextAppearance == null) {
			urgentTime2TextAppearance = SpanUtils.getNewLargeTextAppearance(context);
		}
		return urgentTime2TextAppearance;
	}

	@Nullable
	private static ImageSpan realTimeImage = null;

	@Nullable
	private static ImageSpan getRealTimeImage(@NonNull Context context) {
		if (realTimeImage == null) {
			realTimeImage = UISchedule.getNewRealTimeImage(context, true);
		}
		return realTimeImage;
	}

	public static void resetColorCache() {
		realTimeImage = null;
	}

	@NonNull
	public static ThreadSafeDateFormatter getNewHourFormat(@NonNull Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_HOUR_24_PATTERN, Locale.getDefault());
		} else {
			return new ThreadSafeDateFormatter(FORMAT_HOUR_12_PATTERN, Locale.getDefault());
		}
	}

	@NonNull
	public static CharSequence formatRelativeTime(@NonNull Context context, long timeInThePastInMs) {
		return formatRelativeTime(context, timeInThePastInMs, currentTimeMillis());
	}

	@NonNull
	private static CharSequence formatRelativeTime(@SuppressWarnings("unused") @NonNull Context context,
												   long timeInThePastInMs,
												   long nowInMs) {
		return DateUtils.getRelativeTimeSpanString(timeInThePastInMs, nowInMs, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
	}

	@NonNull
	public static String formatTime(@NonNull Context context, long timeInMs) {
		return getFormatTime(context, timeInMs).formatThreadSafe(timeInMs);
	}

	@SuppressWarnings("unused")
	@NonNull
	public static String formatTime(@NonNull Context context, long timeInMs, @NonNull String timeZone) {
		return formatTime(context, timeInMs, TimeZone.getTimeZone(timeZone));
	}

	@NonNull
	public static String formatTime(@NonNull Context context, long timeInMs, @NonNull TimeZone timeZone) {
		return getFormatTimeTZ(context, timeInMs, timeZone).formatThreadSafe(timeInMs);
	}

	@NonNull
	private static ThreadSafeDateFormatter getFormatTimeTZ(@NonNull Context context, long timeInMs, @NonNull TimeZone timeZone) {
		if (isMorePreciseThanMinute(timeInMs)) {
			return getFormatTimePreciseTZ(context, timeZone);
		}
		return getFormatTimeTZ(context, timeZone);
	}

	@NonNull
	private static WeakHashMap<String, ThreadSafeDateFormatter> formatTimePreciseTZ = new WeakHashMap<>();

	@NonNull
	private static ThreadSafeDateFormatter getFormatTimePreciseTZ(@NonNull Context context, @NonNull TimeZone timeZone) {
		ThreadSafeDateFormatter formatTimePrecise = formatTimePreciseTZ.get(timeZone.getID());
		if (formatTimePrecise == null) {
			formatTimePrecise = getNewFormatTimePrecise(context);
			formatTimePrecise.setTimeZone(timeZone);
			formatTimePreciseTZ.put(timeZone.getID(), formatTimePrecise);
		}
		return formatTimePrecise;
	}

	@NonNull
	private static WeakHashMap<String, ThreadSafeDateFormatter> formatTimeTZ = new WeakHashMap<>();

	@NonNull
	private static ThreadSafeDateFormatter getFormatTimeTZ(@NonNull Context context, @NonNull TimeZone timeZone) {
		ThreadSafeDateFormatter formatTime = formatTimeTZ.get(timeZone.getID());
		if (formatTime == null) {
			formatTime = getNewFormatTime(context);
			formatTime.setTimeZone(timeZone);
			formatTimeTZ.put(timeZone.getID(), formatTime);
		}
		return formatTime;
	}

	@SuppressWarnings("unused")
	@NonNull
	public static String formatTimeWithTZ(@NonNull Context context, long timeInMs, @NonNull TimeZone timeZone) {
		return getFormatTimeWithTZ(context, timeInMs, timeZone).formatThreadSafe(timeInMs);
	}

	@NonNull
	private static ThreadSafeDateFormatter getFormatTimeWithTZ(@NonNull Context context, long timeInMs, @NonNull TimeZone timeZone) {
		if (isMorePreciseThanMinute(timeInMs)) {
			return getFormatTimePreciseWithTZ(context, timeZone);
		}
		return getFormatTimeWithTZ(context, timeZone);
	}

	@NonNull
	private static WeakHashMap<String, ThreadSafeDateFormatter> formatTimePreciseWithTZ = new WeakHashMap<>();

	@NonNull
	private static ThreadSafeDateFormatter getFormatTimePreciseWithTZ(@NonNull Context context, @NonNull TimeZone timeZone) {
		ThreadSafeDateFormatter formatTimePrecise = formatTimePreciseWithTZ.get(timeZone.getID());
		if (formatTimePrecise == null) {
			formatTimePrecise = getNewFormatTimePreciseWithTZ(context);
			formatTimePrecise.setTimeZone(timeZone);
			formatTimePreciseWithTZ.put(timeZone.getID(), formatTimePrecise);
		}
		return formatTimePrecise;
	}

	@NonNull
	private static ThreadSafeDateFormatter getNewFormatTimePreciseWithTZ(@NonNull Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_TIME_24_PRECISE_W_TZ_PATTERN, Locale.getDefault());
		} else {
			return new ThreadSafeDateFormatter(FORMAT_TIME_12_PRECISE_W_TZ_PATTERN, Locale.getDefault());
		}
	}

	@NonNull
	private static WeakHashMap<String, ThreadSafeDateFormatter> formatTimeWithTZ = new WeakHashMap<>();

	@NonNull
	private static ThreadSafeDateFormatter getFormatTimeWithTZ(@NonNull Context context, @NonNull TimeZone timeZone) {
		ThreadSafeDateFormatter formatTime = formatTimeWithTZ.get(timeZone.getID());
		if (formatTime == null) {
			formatTime = getNewFormatTimeWithTZ(context);
			formatTime.setTimeZone(timeZone);
			formatTimeWithTZ.put(timeZone.getID(), formatTime);
		}
		return formatTime;
	}

	@NonNull
	private static ThreadSafeDateFormatter getNewFormatTimeWithTZ(@NonNull Context context) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_TIME_24_W_TZ_PATTERN, Locale.getDefault());
		} else {
			return new ThreadSafeDateFormatter(FORMAT_TIME_12_W_TZ_PATTERN, Locale.getDefault());
		}
	}

	public static boolean isToday(long timeInMs) {
		return timeInMs >= getBeginningOfTodayCal().getTimeInMillis() && timeInMs < getBeginningOfTomorrowCal().getTimeInMillis();
	}

	public static boolean isTomorrow(long timeInMs) {
		return timeInMs >= getBeginningOfTomorrowCal().getTimeInMillis() && timeInMs < getBeginningOfDayRelativeToTodayCal(+2).getTimeInMillis();
	}

	public static boolean isYesterday(long timeInMs) {
		return timeInMs >= getBeginningOfYesterdayCal().getTimeInMillis() && timeInMs < getBeginningOfTodayCal().getTimeInMillis();
	}

	public static long getBeginningOfTodayInMs() {
		return getBeginningOfTodayCal().getTimeInMillis();
	}

	@NonNull
	private static Calendar getBeginningOfYesterdayCal() {
		return getBeginningOfDayRelativeToTodayCal(-1);
	}

	@NonNull
	public static Calendar getBeginningOfTomorrowCal() {
		return getBeginningOfDayRelativeToTodayCal(+1);
	}

	@NonNull
	private static Calendar getBeginningOfDayRelativeToTodayCal(int nbDays) {
		Calendar today = getBeginningOfTodayCal();
		today.add(Calendar.DATE, nbDays);
		return today;
	}

	public static int getHourOfTheDay(long timeInMs) {
		Calendar time = getNewCalendar(timeInMs);
		return time.get(Calendar.HOUR_OF_DAY);
	}

	@NonNull
	public static Calendar getBeginningOfTodayCal() {
		Calendar today = getNewCalendarInstance(currentTimeMillis());
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		return today;
	}

	@NonNull
	public static Calendar getNewCalendarInstance(long timeInMs) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timeInMs);
		return cal;
	}

	public static void cleanTimes(@NonNull SpannableStringBuilder ssb) {
		cleanTimes(ssb.toString(), ssb);
	}

	public static void cleanTimes(@NonNull String input, @NonNull SpannableStringBuilder output) {
		String word = input.toLowerCase(Locale.ENGLISH);
		for (String amPm : AM_PM_LIST) {
			for (int index = word.indexOf(amPm); index >= 0; index = word.indexOf(amPm, index + 1)) { // TODO i18n
				if (index <= 0) {
					break;
				}
				output = SpanUtils.set(output, index - 1, index, SpanUtils.getNew10PercentSizeSpan()); // remove space hack
				if (amPm.length() == 4) {
					output = SpanUtils.set(output, index + 1, index + 2, SpanUtils.getNew10PercentSizeSpan()); // remove space hack
					output = SpanUtils.set(output, index + 3, index + 4, SpanUtils.getNew10PercentSizeSpan()); // remove space hack
				}
				output = SpanUtils.set(output, index, index + amPm.length(), SpanUtils.getNew25PercentSizeSpan());
			}
		}
		Matcher rMatcher = TIME_W_SECONDS.matcher(word);
		while (rMatcher.find()) {
			int end = rMatcher.end();
			output = SpanUtils.set(output, end - 3, end, SpanUtils.getNew50PercentSizeSpan());
		}
	}

	public static boolean isFrequentService(@NonNull ArrayList<Timestamp> timestamps, long providerFSMinDurationInMs, long providerFSTimeSpanInMs) {
		if (timestamps.size() < FREQUENT_SERVICE_MIN_SERVICE) {
			return false; // NOT FREQUENT (no service at all)
		}
		long fsMinDurationMs = providerFSMinDurationInMs > 0 ? providerFSMinDurationInMs : FREQUENT_SERVICE_MIN_DURATION_IN_MS_DEFAULT;
		long fsTimeSpanMs = providerFSTimeSpanInMs > 0 ? providerFSTimeSpanInMs : FREQUENT_SERVICE_TIME_SPAN_IN_MS_DEFAULT;
		long firstTimestamp = timestamps.get(0).getT();
		long previousTimestamp = firstTimestamp;
		Long currentTimestamp;
		long diffInMs;
		for (int i = 1; i < timestamps.size(); i++) {
			currentTimestamp = timestamps.get(i).getT();
			diffInMs = currentTimestamp - previousTimestamp;
			if (diffInMs > fsTimeSpanMs) {
				return false; // NOT FREQUENT
			}
			previousTimestamp = currentTimestamp;
			if (previousTimestamp - firstTimestamp >= fsMinDurationMs) {
				return true; // FREQUENT (for long enough)
			}
		}
		//noinspection RedundantIfStatement
		if (previousTimestamp - firstTimestamp < fsMinDurationMs) {
			return false; // NOT FREQUENT (for long enough)
		}
		return true; // FREQUENT
	}

	@NonNull
	public static Pair<CharSequence, CharSequence> getShortTimeSpan(@NonNull Context context,
																	long diffInMs,
																	@NonNull Timestamp targetedTimestamp,
																	long precisionInMs) {
		if (diffInMs < MAX_DURATION_DISPLAYED_IN_MS) {
			return getShortTimeSpanNumber(context, diffInMs, precisionInMs, targetedTimestamp.getRealTime());
		} else {
			Pair<CharSequence, CharSequence> shortTimeSpanString = getShortTimeSpanString(context, diffInMs, targetedTimestamp.getT());
			return new Pair<>( //
					getShortTimeSpanStringStyle(context, shortTimeSpanString.first),  //
					getShortTimeSpanStringStyle(context, shortTimeSpanString.second));
		}
	}

	@NonNull
	private static Pair<CharSequence, CharSequence> getShortTimeSpanNumber(@NonNull Context context,
																		   long diffInMs, long precisionInMs,
																		   @Nullable Boolean realTime) {
		SpannableStringBuilder shortTimeSpan1SSB = new SpannableStringBuilder();
		SpannableStringBuilder shortTimeSpan2SSB = new SpannableStringBuilder();
		return getShortTimeSpanNumber(context,
				diffInMs, precisionInMs,
				realTime,
				shortTimeSpan1SSB, shortTimeSpan2SSB);
	}

	@NonNull
	static Pair<CharSequence, CharSequence> getShortTimeSpanNumber(@NonNull Context context, long diffInMs, long precisionInMs,
																   @Nullable Boolean realTime,
																   @NonNull SpannableStringBuilder shortTimeSpan1SSB,
																   @NonNull SpannableStringBuilder shortTimeSpan2SSB) {
		int diffInSec = (int) Math.floor(TimeUnit.MILLISECONDS.toSeconds(diffInMs));
		if (diffInMs - (diffInSec * MILLIS_IN_SEC) > (MILLIS_IN_SEC / 2)) {
			diffInSec++;
		}
		int diffInMin = (int) Math.floor(TimeUnit.SECONDS.toMinutes(diffInSec));
		if (diffInSec - (diffInMin * SEC_IN_MIN) > (SEC_IN_MIN / 2)) {
			diffInMin++;
		}
		int diffInHour = (int) Math.floor(TimeUnit.MINUTES.toHours(diffInMin));
		if (diffInMin - (diffInHour * MIN_IN_HOUR) > (MIN_IN_HOUR / 2)) {
			diffInHour++;
		}
		int diffInDay = (int) Math.floor(TimeUnit.HOURS.toDays(diffInHour));
		if (diffInHour - (diffInDay * HOUR_IN_DAY) > (HOUR_IN_DAY / 2)) {
			diffInDay++;
		}
		int realTimeStart = -1;
		int realTimeEnd = -1;
		int urgentTime1Start = -1;
		int urgentTime1End = -1;
		int timeUnit2Start = -1;
		int timeUnit2End = -1;
		int urgentTime2Start = -1;
		int urgentTime2End = -1;
		boolean isShortTimeSpanString = false;
		final boolean isRealTime = Boolean.TRUE.equals(realTime);
		Resources resources = context.getResources();
		if (diffInDay > 0 && diffInHour > MAX_HOURS_SHOWED) {
			shortTimeSpan1SSB.append(getNumberInLetter(context, diffInDay));
			isShortTimeSpanString = true;
			shortTimeSpan2SSB.append(resources.getQuantityText(R.plurals.days_capitalized, diffInDay));
		} else if (diffInHour > 0 && diffInMin > MAX_MINUTES_SHOWED) {
			shortTimeSpan1SSB.append(getNumberInLetter(context, diffInHour));
			isShortTimeSpanString = true;
			shortTimeSpan2SSB.append(resources.getQuantityText(R.plurals.hours_capitalized, diffInHour));
		} else if (-precisionInMs <= diffInMs && diffInMs <= precisionInMs) {
			urgentTime1Start = shortTimeSpan1SSB.length();
			shortTimeSpan1SSB.append(String.valueOf(diffInMin));
			urgentTime1End = shortTimeSpan1SSB.length();
			urgentTime2Start = shortTimeSpan2SSB.length();
			timeUnit2Start = shortTimeSpan2SSB.length();
			shortTimeSpan2SSB.append(resources.getQuantityString(R.plurals.minutes_capitalized, Math.abs(diffInMin)));
			timeUnit2End = shortTimeSpan2SSB.length();
			urgentTime2End = shortTimeSpan2SSB.length();
		} else {
			boolean isUrgent = diffInMin < URGENT_SCHEDULE_IN_MIN;
			if (isUrgent) {
				urgentTime1Start = shortTimeSpan1SSB.length();
			}
			shortTimeSpan1SSB.append(String.valueOf(diffInMin));
			if (isUrgent) {
				urgentTime1End = shortTimeSpan1SSB.length();
			}
			if (isUrgent) {
				urgentTime2Start = shortTimeSpan2SSB.length();
			}
			timeUnit2Start = shortTimeSpan2SSB.length();
			shortTimeSpan2SSB.append(resources.getQuantityString(R.plurals.minutes_capitalized, diffInMin));
			timeUnit2End = shortTimeSpan2SSB.length();
			if (isUrgent) {
				urgentTime2End = shortTimeSpan2SSB.length();
			}
		}
		if (isRealTime) {
			realTimeStart = shortTimeSpan1SSB.length();
			shortTimeSpan1SSB.append(UISchedule.REAL_TIME_CHAR);
			realTimeEnd = shortTimeSpan1SSB.length();
		}
		// set spans
		if (realTimeStart < realTimeEnd) {
			shortTimeSpan1SSB = SpanUtils.set(shortTimeSpan1SSB, realTimeStart, realTimeEnd, getRealTimeImage(context));
		}
		if (urgentTime1Start < urgentTime1End) {
			shortTimeSpan1SSB = SpanUtils.set(shortTimeSpan1SSB, urgentTime1Start, urgentTime1End, getUrgentTime1TextAppearance(context));
		}
		if (urgentTime2Start < urgentTime2End) {
			shortTimeSpan2SSB = SpanUtils.set(shortTimeSpan2SSB, urgentTime2Start, urgentTime2End, getUrgentTime2TextAppearance(context));
		}
		if (timeUnit2Start < timeUnit2End) {
			shortTimeSpan2SSB = SpanUtils.set(shortTimeSpan2SSB, timeUnit2Start, timeUnit2End, //
					TIME_UNIT_SIZE, TIME_UNIT_FONT);
		}
		if (isShortTimeSpanString) { // > 99 minutes
			return new Pair<>( //
					getShortTimeSpanStringStyle(context, shortTimeSpan1SSB), //
					getShortTimeSpanStringStyle(context, shortTimeSpan2SSB));
		} else { // < 99 minutes
			return new Pair<>(shortTimeSpan1SSB, shortTimeSpan2SSB);
		}
	}

	@NonNull
	public static CharSequence getNumberInLetter(@NonNull Context context, int number) {
		switch (number) {
		case 0:
			return context.getString(R.string.zero_capitalized);
		case 1:
			return context.getString(R.string.one_capitalized);
		case 2:
			return context.getString(R.string.two_capitalized);
		case 3:
			return context.getString(R.string.three_capitalized);
		case 4:
			return context.getString(R.string.four_capitalized);
		case 5:
			return context.getString(R.string.five_capitalized);
		case 6:
			return context.getString(R.string.six_capitalized);
		case 7:
			return context.getString(R.string.seven_capitalized);
		case 8:
			return context.getString(R.string.eight_capitalized);
		case 9:
			return context.getString(R.string.nine_capitalized);
		case 10:
			return context.getString(R.string.ten_capitalized);
		case 11:
			return context.getString(R.string.eleven_capitalized);
		case 12:
			return context.getString(R.string.twelve_capitalized);
		case 13:
			return context.getString(R.string.thirteen_capitalized);
		case 14:
			return context.getString(R.string.fourteen_capitalized);
		case 15:
			return context.getString(R.string.fifteen_capitalized);
		case 16:
			return context.getString(R.string.sixteen_capitalized);
		case 17:
			return context.getString(R.string.seventeen_capitalized);
		case 18:
			return context.getString(R.string.eighteen_capitalized);
		case 19:
			return context.getString(R.string.nineteen_capitalized);
		default:
			return String.valueOf(number); // 2 characters number almost equal world
		}
	}

	@NonNull
	private static Pair<CharSequence, CharSequence> getShortTimeSpanString(@NonNull Context context, long diffInMs, long targetedTimestamp) {
		long now = targetedTimestamp - diffInMs;
		Calendar today = Calendar.getInstance();
		today.setTimeInMillis(now);
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		Calendar todayMorningStarts = (Calendar) today.clone();
		todayMorningStarts.set(Calendar.HOUR_OF_DAY, 6);
		Calendar todayAfterNoonStarts = (Calendar) today.clone();
		todayAfterNoonStarts.set(Calendar.HOUR_OF_DAY, 12);
		if (targetedTimestamp >= todayMorningStarts.getTimeInMillis() && targetedTimestamp < todayAfterNoonStarts.getTimeInMillis()) {
			return new Pair<>( //
					context.getString(R.string.this_morning_part_1), context.getString(R.string.this_morning_part_2)); // MORNING
		}
		Calendar todayEveningStarts = (Calendar) today.clone();
		todayEveningStarts.set(Calendar.HOUR_OF_DAY, 18);
		if (targetedTimestamp >= todayAfterNoonStarts.getTimeInMillis() && targetedTimestamp < todayEveningStarts.getTimeInMillis()) {
			return new Pair<>( //
					context.getString(R.string.this_afternoon_part_1), context.getString(R.string.this_afternoon_part_2)); // AFTERNOON
		}
		Calendar tonightStarts = (Calendar) today.clone();
		tonightStarts.set(Calendar.HOUR_OF_DAY, 22);
		if (targetedTimestamp >= todayEveningStarts.getTimeInMillis() && targetedTimestamp < tonightStarts.getTimeInMillis()) {
			return new Pair<>( //
					context.getString(R.string.this_evening_part_1), context.getString(R.string.this_evening_part_2)); // EVENING
		}
		Calendar tomorrow = (Calendar) today.clone();
		tomorrow.add(Calendar.DATE, +1);
		Calendar tomorrowStarts = (Calendar) tomorrow.clone();
		tomorrowStarts.set(Calendar.HOUR_OF_DAY, 5);
		if (targetedTimestamp >= tonightStarts.getTimeInMillis() && targetedTimestamp < tomorrowStarts.getTimeInMillis()) {
			return new Pair<>( //
					context.getString(R.string.tonight_part_1), context.getString(R.string.tonight_part_2)); // NIGHT
		}
		Calendar afterTomorrow = (Calendar) today.clone();
		afterTomorrow.add(Calendar.DATE, +2);
		if (targetedTimestamp >= tomorrowStarts.getTimeInMillis() && targetedTimestamp < afterTomorrow.getTimeInMillis()) {
			return new Pair<>( //
					context.getString(R.string.tomorrow_part_1), context.getString(R.string.tomorrow_part_2)); // TOMORROW
		}
		Calendar nextWeekStarts = (Calendar) today.clone();
		nextWeekStarts.add(Calendar.DATE, +7);
		if (targetedTimestamp >= afterTomorrow.getTimeInMillis() && targetedTimestamp < nextWeekStarts.getTimeInMillis()) {
			return new Pair<>( //
					STANDALONE_DAY_OF_THE_WEEK_LONG.formatThreadSafe(targetedTimestamp), null); // THIS WEEK (Monday-Sunday)
		}
		Calendar nextWeekEnds = (Calendar) today.clone();
		nextWeekEnds.add(Calendar.DATE, +14);
		if (targetedTimestamp >= nextWeekStarts.getTimeInMillis() && targetedTimestamp < nextWeekEnds.getTimeInMillis()) {
			return new Pair<>( //
					context.getString(R.string.next_week_part_1), context.getString(R.string.next_week_part_2)); // NEXT WEEK
		}
		Calendar thisMonthStarts = (Calendar) today.clone();
		thisMonthStarts.set(Calendar.DAY_OF_MONTH, 1);
		Calendar nextMonthStarts = (Calendar) thisMonthStarts.clone();
		nextMonthStarts.add(Calendar.MONTH, +1);
		if (targetedTimestamp >= thisMonthStarts.getTimeInMillis() && targetedTimestamp < nextMonthStarts.getTimeInMillis()) {
			return new Pair<>( //
					context.getString(R.string.this_month_part_1), context.getString(R.string.this_month_part_2)); // THIS MONTH
		}
		Calendar nextNextMonthStarts = (Calendar) nextMonthStarts.clone();
		nextNextMonthStarts.add(Calendar.MONTH, +1);
		if (targetedTimestamp >= nextMonthStarts.getTimeInMillis() && targetedTimestamp < nextNextMonthStarts.getTimeInMillis()) {
			return new Pair<>( //
					context.getString(R.string.next_month_part_1), context.getString(R.string.next_month_part_2)); // NEXT MONTH
		}
		Calendar next12MonthsStart = (Calendar) today.clone();
		next12MonthsStart.add(Calendar.MONTH, +1);
		Calendar next12MonthsEnd = (Calendar) today.clone();
		next12MonthsEnd.add(Calendar.MONTH, +6);
		if (targetedTimestamp >= next12MonthsStart.getTimeInMillis() && targetedTimestamp < next12MonthsEnd.getTimeInMillis()) {
			return new Pair<>( //
					STANDALONE_MONTH_LONG.formatThreadSafe(targetedTimestamp), null); // LESS THAN 12 MONTHS (January-December)
		}
		Calendar thisYearStarts = (Calendar) thisMonthStarts.clone();
		thisYearStarts.set(Calendar.MONTH, Calendar.JANUARY);
		Calendar nextYearStarts = (Calendar) thisYearStarts.clone();
		nextYearStarts.add(Calendar.YEAR, +1);
		Calendar nextNextYearStarts = (Calendar) nextYearStarts.clone();
		nextNextYearStarts.add(Calendar.YEAR, +1);
		if (targetedTimestamp >= nextYearStarts.getTimeInMillis() && targetedTimestamp < nextNextYearStarts.getTimeInMillis()) {
			return new Pair<>( //
					context.getString(R.string.next_year_part_1), context.getString(R.string.next_year_part_2)); // NEXT YEAR
		}
		return new Pair<>( //
				DateUtils.formatSameDayTime(targetedTimestamp, now, ThreadSafeDateFormatter.MEDIUM, ThreadSafeDateFormatter.SHORT), null); // DEFAULT
	}

	private static CharSequence getShortTimeSpanStringStyle(@NonNull Context context, @Nullable CharSequence timeSpan) {
		if (TextUtils.isEmpty(timeSpan)) {
			return timeSpan;
		}
		return SpanUtils.setAll(timeSpan, //
				SpanUtils.getNewSmallTextAppearance(context), // can be concatenated
				SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont())); // can be concatenated
	}

	public static boolean isSameDay(@NonNull Long timeInMillis1, @NonNull Long timeInMillis2) {
		SupportFactory.get().requireNonNull(timeInMillis1, "The date must not be null");
		SupportFactory.get().requireNonNull(timeInMillis2, "The date must not be null");
		Calendar cal1 = Calendar.getInstance();
		cal1.setTimeInMillis(timeInMillis1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTimeInMillis(timeInMillis2);
		return isSameDay(cal1, cal2);
	}

	private static boolean isSameDay(@NonNull Calendar cal1, @NonNull Calendar cal2) {
		SupportFactory.get().requireNonNull(cal1, "The date must not be null");
		SupportFactory.get().requireNonNull(cal2, "The date must not be null");
		return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) //
				&& cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) //
				&& cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
	}

	@SuppressWarnings("unused")
	public static boolean isSameDay(@NonNull Date date1, @NonNull Date date2) {
		SupportFactory.get().requireNonNull(date1, "The date must not be null");
		SupportFactory.get().requireNonNull(date2, "The date must not be null");
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		return isSameDay(cal1, cal2);
	}

	public static class TimeChangedReceiver extends BroadcastReceiver {

		@NonNull
		private final WeakReference<TimeChangedListener> listenerWR;

		public TimeChangedReceiver(@NonNull TimeChangedListener listener) {
			this.listenerWR = new WeakReference<>(listener);
		}

		@Override
		public void onReceive(@Nullable Context context, @Nullable Intent intent) {
			String action = intent == null ? null : intent.getAction();
			if (Intent.ACTION_TIME_TICK.equals(action) //
					|| Intent.ACTION_TIME_CHANGED.equals(action) //
					|| Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
				TimeChangedListener listener = this.listenerWR.get();
				if (listener != null) {
					listener.onTimeChanged();
				}
			}
		}

		public interface TimeChangedListener {
			void onTimeChanged();
		}
	}
}
