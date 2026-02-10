package org.mtransit.android.util;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
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
import org.mtransit.android.ui.MTTopSuperscriptSpan;
import org.mtransit.commons.StringUtils;

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

	public static final long RECENT_IN_MILLIS = TimeUnit.HOURS.toMillis(1L);

	public static final long MAX_DURATION_DISPLAYED_IN_MS = TimeUnit.HOURS.toMillis(6L);
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
	private static final String A__M_ = "a. m.";
	private static final String PM = "pm";
	private static final String P_M_ = "p.m.";
	private static final String P__M_ = "p. m.";
	private static final List<String> AM_PM_LIST = Arrays.asList( //
			AM, A_M_, A__M_, //
			PM, P_M_, P__M_ //
	);

	private static final long MILLIS_IN_SEC = 1000L;
	private static final int SEC_IN_MIN = 60;
	private static final int MIN_IN_HOUR = 60;
	private static final int HOUR_IN_DAY = 24;

	private static final long MAX_MINUTES_SHOWED = 99L;
	private static final int MAX_HOURS_SHOWED = 99;
	public static final long MAX_DURATION_SHOW_NUMBER_IN_MS = TimeUnit.MINUTES.toMillis(MAX_MINUTES_SHOWED);

	private static final RelativeSizeSpan TIME_UNIT_SIZE = SpanUtils.getNew50PercentSizeSpan();

	private static final TypefaceSpan TIME_UNIT_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

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

	static void resetColorCache() {
		realTimeImage = null;
	}

	@NonNull
	public static ThreadSafeDateFormatter getNewHourFormat(@NonNull Context context, boolean fixedLength) {
		if (is24HourFormat(context)) {
			return new ThreadSafeDateFormatter(FORMAT_HOUR_24_PATTERN, Locale.getDefault());
		} else {
			return new ThreadSafeDateFormatter(fixedLength ? FORMAT_HOUR_12_FIXED_LENGTH_PATTERN : FORMAT_HOUR_12_PATTERN, Locale.getDefault());
		}
	}

	@NonNull
	public static CharSequence formatNearDate(@NonNull Context context, long dateInMs) {
		return DateUtils.formatDateTime(context, dateInMs, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL);
	}

	@SuppressWarnings("unused")
	@NonNull
	public static CharSequence formatVeryRecentTime(long timeInMs) {
		return formatVeryRecentTime(timeInMs, currentTimeMillis());
	}

	@NonNull
	private static CharSequence formatVeryRecentTime(long timeInMs, long nowInMs) {
		return DateUtils.getRelativeTimeSpanString(timeInMs, nowInMs, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
	}

	@SuppressWarnings("unused")
	@NonNull
	public static CharSequence formatNearTime(long timeInMs) {
		return formatNearTime(timeInMs, currentTimeMillis());
	}

	@NonNull
	private static CharSequence formatNearTime(long timeInMs, long nowInMs) {
		return DateUtils.getRelativeTimeSpanString(timeInMs, nowInMs, DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
	}

	@NonNull
	public static CharSequence formatRelativeTime(long timeInThePastInMs) {
		return formatRelativeTime(timeInThePastInMs, currentTimeMillis());
	}

	@NonNull
	private static CharSequence formatRelativeTime(long timeInThePastInMs, long nowInMs) {
		return DateUtils.getRelativeTimeSpanString(timeInThePastInMs, nowInMs, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
	}

	@SuppressWarnings("unused")
	@NonNull
	public static String formatTime(@NonNull Context context, long timeInMs, @NonNull String timeZone) {
		return formatTime(context, timeInMs, TimeZone.getTimeZone(timeZone));
	}

	@NonNull
	public static String formatTimestamp(@NonNull Context context, @NonNull Timestamp timestamp) {
		return formatTimestamp(context, timestamp, timestamp.getT());
	}

	@NonNull
	public static String formatTimestamp(@NonNull Context context, @NonNull Timestamp timestamp, long timestampInMs) {
		final String localTimeZone = timestamp.getLocalTimeZone();
		if (localTimeZone != null) {
			return cleanNoRealTime(timestamp.isRealTime(),
					formatTime(context, timestampInMs, TimeZone.getTimeZone(localTimeZone))
			);
		} else {
			return cleanNoRealTime(timestamp.isRealTime(),
					formatTime(context, timestampInMs)
			);
		}
	}

	@SuppressWarnings("WeakerAccess")
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
	private static final WeakHashMap<String, ThreadSafeDateFormatter> formatTimePreciseTZ = new WeakHashMap<>();

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
	private static final WeakHashMap<String, ThreadSafeDateFormatter> formatTimeTZ = new WeakHashMap<>();

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
	private static final WeakHashMap<String, ThreadSafeDateFormatter> formatTimePreciseWithTZ = new WeakHashMap<>();

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
	private static final WeakHashMap<String, ThreadSafeDateFormatter> formatTimeWithTZ = new WeakHashMap<>();

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

	@NonNull
	public static String getNearRelativeDay(@NonNull Context context, long timeInMs, @NonNull String formattedDate) {
		if (isYesterday(timeInMs)) {
			return context.getString(R.string.yesterday_and, formattedDate);
		} else if (isToday(timeInMs)) {
			return context.getString(R.string.today_and, formattedDate);
		} else if (isTomorrow(timeInMs)) {
			return context.getString(R.string.tomorrow_and, formattedDate);
		} else {
			return formattedDate;
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

	@SuppressWarnings("unused")
	public static long getBeginningOfTodayInMs() {
		return getBeginningOfTodayInMs(TimeZone.getDefault());
	}

	public static long getBeginningOfTodayInMs(@NonNull TimeZone timeZone) {
		return getBeginningOfTodayCal(timeZone).getTimeInMillis();
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

	@SuppressWarnings("unused")
	@NonNull
	public static Calendar getBeginningOfMonthCalRelativeToThisMonth(int nbMonths) {
		Calendar today = getBeginningOfMonthCal();
		today.add(Calendar.MONTH, nbMonths);
		return today;
	}

	@SuppressWarnings("unused")
	@NonNull
	public static Calendar getBeginningOfYearCalRelativeToThisYear(int nbYears) {
		Calendar today = getBeginningOfYearCal();
		today.add(Calendar.YEAR, nbYears);
		return today;
	}

	@SuppressWarnings("unused")
	public static int getHourOfTheDay(long timeInMs) {
		Calendar time = getNewCalendar(timeInMs);
		return getHourOfTheDay(time);
	}

	@SuppressWarnings("unused")
	public static int getHourOfTheDay(@NonNull Calendar cal, long timeInMs) {
		cal.setTimeInMillis(timeInMs);
		return getHourOfTheDay(cal);
	}

	public static int getHourOfTheDay(@NonNull Calendar cal) {
		return cal.get(Calendar.HOUR_OF_DAY);
	}

	@NonNull
	public static Calendar getBeginningOfYearCal() {
		return setBeginningOfYear(
				getNewCalendarInstance(currentTimeMillis())
		);
	}

	@NonNull
	public static Calendar getBeginningOfMonthCal() {
		return setBeginningOfMonth(
				getNewCalendarInstance(currentTimeMillis())
		);
	}

	@NonNull
	public static Calendar getBeginningOfTodayCal() {
		return getBeginningOfTodayCal(TimeZone.getDefault());
	}

	@NonNull
	public static Calendar getBeginningOfTodayCal(@NonNull TimeZone timeZone) {
		return setBeginningOfDay(
				getNewCalendarInstance(timeZone, currentTimeMillis())
		);
	}

	@SuppressWarnings("UnusedReturnValue")
	@NonNull
	public static Calendar setBeginningOfHour(@NonNull Calendar day) {
		day.set(Calendar.MINUTE, 0);
		day.set(Calendar.SECOND, 0);
		day.set(Calendar.MILLISECOND, 0);
		return day;
	}

	@NonNull
	public static Calendar setBeginningOfDay(@NonNull Calendar day) {
		day.set(Calendar.HOUR_OF_DAY, 0);
		setBeginningOfHour(day);
		return day;
	}

	@NonNull
	public static Calendar setBeginningOfMonth(@NonNull Calendar day) {
		day.set(Calendar.DAY_OF_MONTH, 1); // The first day of the month has value 1
		setBeginningOfDay(day);
		return day;
	}

	@NonNull
	public static Calendar setBeginningOfYear(@NonNull Calendar day) {
		day.set(Calendar.MONTH, Calendar.JANUARY);
		setBeginningOfMonth(day);
		return day;
	}

	@NonNull
	public static Calendar getNewCalendarInstance(long timeInMs) {
		return getNewCalendarInstance(TimeZone.getDefault(), timeInMs);
	}

	@NonNull
	public static Calendar getNewCalendarInstance(@NonNull TimeZone timeZone, long timeInMs) {
		Calendar cal = Calendar.getInstance(timeZone);
		cal.setTimeInMillis(timeInMs);
		return cal;
	}

	public static void cleanTimes(@NonNull SpannableStringBuilder ssb) {
		cleanTimes(ssb.toString(), ssb);
	}

	public static void cleanTimes(@NonNull String input, @NonNull SpannableStringBuilder output) {
		cleanTimes(input, output, 1.0);
	}

	public static void cleanTimes(@NonNull String input, @NonNull SpannableStringBuilder output, double superScriptRadio) {
		String word = input.toLowerCase(Locale.ENGLISH);
		for (String amPm : AM_PM_LIST) {
			for (int index = word.indexOf(amPm); index >= 0; index = word.indexOf(amPm, index + 1)) { // TODO i18n
				if (index == 0) {
					break;
				}
				if (AM.equals(amPm) || A_M_.equals(amPm) || A__M_.equals(amPm)) {
					output = SpanUtils.set(output, index, index + amPm.length(), new MTTopSuperscriptSpan(superScriptRadio));
				}
				output = SpanUtils.set(output, index - 1, index, SpanUtils.getNew10PercentSizeSpan()); // remove space hack - before
				if (A_M_.equals(amPm) || P_M_.equals(amPm)) {
					output = SpanUtils.set(output, index + 1, index + 2, SpanUtils.getNew10PercentSizeSpan()); // remove space hack - after a/p
					output = SpanUtils.set(output, index + 3, index + 4, SpanUtils.getNew10PercentSizeSpan()); // remove space hack - after m
				} else if (A__M_.equals(amPm) || P__M_.equals(amPm)) {
					output = SpanUtils.set(output, index + 1, index + 3, SpanUtils.getNew10PercentSizeSpan()); // remove space hack - after a/p
					output = SpanUtils.set(output, index + 4, index + 5, SpanUtils.getNew10PercentSizeSpan()); // remove space hack - after m
				}
				output = SpanUtils.set(output, index, index + amPm.length(), SpanUtils.getNew25PercentSizeSpan());
			}
		}
		final Matcher rMatcher = TIME_W_SECONDS.matcher(word);
		while (rMatcher.find()) {
			int end = rMatcher.end();
			output = SpanUtils.set(output, end - 3, end, SpanUtils.getNew50PercentSizeSpan());
		}
	}

	public static boolean isFrequentService(@NonNull ArrayList<Timestamp> timestamps, long providerFSMinDurationInMs, long providerFSTimeSpanInMs) {
		final int timestampsSize = timestamps.size();
		if (timestampsSize < FREQUENT_SERVICE_MIN_SERVICE) {
			return false; // NOT FREQUENT (no service at all)
		}
		long fsMinDurationMs = providerFSMinDurationInMs > 0 ? providerFSMinDurationInMs : FREQUENT_SERVICE_MIN_DURATION_IN_MS_DEFAULT;
		long fsTimeSpanMs = providerFSTimeSpanInMs > 0 ? providerFSTimeSpanInMs : FREQUENT_SERVICE_TIME_SPAN_IN_MS_DEFAULT;
		int i = 0;
		Timestamp timestamp;
		long firstTimestamp = -1L;
		for (; i < timestampsSize; i++) {
			timestamp = timestamps.get(i);
			if (timestamp.isNoPickup()) {
				continue; // skip descent only
			}
			firstTimestamp = timestamp.getT();
			break;
		}
		if (firstTimestamp < 0) {
			return false; // NOT FREQUENT (no real service)
		}
		long previousTimestamp = firstTimestamp;
		long currentTimestamp;
		long diffInMs;
		for (; i < timestampsSize; i++) {
			timestamp = timestamps.get(i);
			if (timestamp.isNoPickup()) {
				continue; // skip descent only
			}
			currentTimestamp = timestamp.getT();
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
			return getShortTimeSpanNumber(context, diffInMs, precisionInMs, targetedTimestamp.isRealTime(), targetedTimestamp.isOldSchedule());
		} else {
			Pair<CharSequence, CharSequence> shortTimeSpanString = getShortTimeSpanString(context, diffInMs, targetedTimestamp.getT());
			return new Pair<>( //
					getShortTimeSpanStringStyle(context, shortTimeSpanString.first, targetedTimestamp.isOldSchedule()),  //
					getShortTimeSpanStringStyle(context, shortTimeSpanString.second, targetedTimestamp.isOldSchedule()));
		}
	}

	@NonNull
	private static Pair<CharSequence, CharSequence> getShortTimeSpanNumber(@NonNull Context context,
																		   long diffInMs, long precisionInMs,
																		   boolean isRealTime, boolean isOldSchedule) {
		SpannableStringBuilder shortTimeSpan1SSB = new SpannableStringBuilder();
		SpannableStringBuilder shortTimeSpan2SSB = new SpannableStringBuilder();
		return getShortTimeSpanNumber(context,
				diffInMs, precisionInMs,
				isRealTime, isOldSchedule,
				shortTimeSpan1SSB, shortTimeSpan2SSB);
	}

	@NonNull
	static Pair<CharSequence, CharSequence> getShortTimeSpanNumber(@NonNull Context context, long diffInMs, long precisionInMs,
																   boolean isRealTime, boolean isOldSchedule,
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
			shortTimeSpan1SSB.append(UITimeUtils.REAL_TIME_CHAR);
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
					getShortTimeSpanStringStyle(context, shortTimeSpan1SSB, isOldSchedule), //
					getShortTimeSpanStringStyle(context, shortTimeSpan2SSB, isOldSchedule));
		} else { // < 99 minutes
			if (isOldSchedule) {
				SpanUtils.setAllNN(shortTimeSpan1SSB, SpanUtils.getNewItalicStyleSpan());
				SpanUtils.setAllNN(shortTimeSpan2SSB, SpanUtils.getNewItalicStyleSpan());
			}
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
			return String.valueOf(number); // 2 characters number almost equal word
		}
	}

	@NonNull
	public static Pair<CharSequence, CharSequence> getShortTimeSpanString(@NonNull Context context, long diffInMs, long targetedTimestamp) {
		long now = targetedTimestamp - diffInMs;
		Calendar today = setBeginningOfDay(
				getNewCalendarInstance(now)
		);
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

	private static CharSequence getShortTimeSpanStringStyle(@NonNull Context context, @Nullable CharSequence timeSpan, boolean isOldSchedule) {
		if (StringUtils.isEmpty(timeSpan)) {
			return timeSpan;
		}
		if (isOldSchedule) {
			timeSpan = SpanUtils.setAll(timeSpan,
					SpanUtils.getNewItalicStyleSpan()); // can be concatenated
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
			final String action = intent == null ? null : intent.getAction();
			if (Intent.ACTION_TIME_TICK.equals(action) //
					|| Intent.ACTION_TIME_CHANGED.equals(action) //
					|| Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
				final TimeChangedListener listener = this.listenerWR.get();
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
