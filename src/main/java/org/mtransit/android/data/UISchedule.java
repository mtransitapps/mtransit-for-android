package org.mtransit.android.data;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.util.UISpanUtils;
import org.mtransit.android.util.UITimeUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class UISchedule extends org.mtransit.android.commons.data.Schedule implements MTLog.Loggable {

	private static final String LOG_TAG = UISchedule.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final char REAL_TIME_CHAR = '_';

	private static final long MAX_LAST_STATUS_DIFF_IN_MS = TimeUnit.MINUTES.toMillis(5L);

	private static final long MAX_FREQUENCY_DISPLAYED_IN_SEC = TimeUnit.MINUTES.toSeconds(15L);

	private static final RelativeSizeSpan SCHEDULE_LIST_TIMES_SIZE = SpanUtils.getNew200PercentSizeSpan();

	private static final StyleSpan SCHEDULE_LIST_TIMES_STYLE = SpanUtils.getNewBoldStyleSpan();

	private static final StyleSpan SCHEDULE_OLD_SCHEDULE_STYLE = SpanUtils.getNewItalicStyleSpan();

	private static final RelativeSizeSpan NO_SERVICE_SIZE = SpanUtils.getNew200PercentSizeSpan();

	@ColorInt
	public static int getDefaultPastTextColor(@NonNull Context context) {
		return ColorUtils.getTextColorTertiary(context);
	}

	@Nullable
	private static Typeface defaultPastTypeface;

	@SuppressWarnings("unused")
	@NonNull
	public static Typeface getDefaultPastTypeface() {
		if (defaultPastTypeface == null) {
			defaultPastTypeface = Typeface.DEFAULT;
		}
		return defaultPastTypeface;
	}

	@ColorInt
	public static int getDefaultNowTextColor(@NonNull Context context) {
		return ColorUtils.getTextColorPrimary(context);
	}

	@Nullable
	private static Typeface defaultNowTypeface;

	@SuppressWarnings("unused")
	@NonNull
	public static Typeface getDefaultNowTypeface() {
		if (defaultNowTypeface == null) {
			defaultNowTypeface = Typeface.DEFAULT_BOLD;
		}
		return defaultNowTypeface;
	}

	@ColorInt
	public static int getDefaultFutureTextColor(@NonNull Context context) {
		return ColorUtils.getTextColorPrimary(context);
	}

	@Nullable
	private static Typeface defaultFutureTypeface;

	@SuppressWarnings("unused")
	@NonNull
	public static Typeface getDefaultFutureTypeface() {
		if (defaultFutureTypeface == null) {
			defaultFutureTypeface = Typeface.DEFAULT;
		}
		return defaultFutureTypeface;
	}

	@Nullable
	private static ForegroundColorSpan scheduleListTimesFutureTextColor1 = null;

	@NonNull
	private static ForegroundColorSpan getScheduleListTimesFutureTextColor1(Context context) {
		if (scheduleListTimesFutureTextColor1 == null) {
			scheduleListTimesFutureTextColor1 = SpanUtils.getNewTextColor(getDefaultFutureTextColor(context));
		}
		return scheduleListTimesFutureTextColor1;
	}

	@Nullable
	private static TextAppearanceSpan scheduleListTimesCloseTextAppearance1 = null;

	@NonNull
	private static TextAppearanceSpan getScheduleListTimesCloseTextAppearance1(Context context) {
		if (scheduleListTimesCloseTextAppearance1 == null) {
			scheduleListTimesCloseTextAppearance1 = SpanUtils.getNewMediumTextAppearance(context);
		}
		return scheduleListTimesCloseTextAppearance1;
	}

	@Nullable
	private static TextAppearanceSpan scheduleListTimesFarTextAppearance1 = null;

	@NonNull
	private static TextAppearanceSpan getScheduleListTimesFarTextAppearance1(@NonNull Context context) {
		if (scheduleListTimesFarTextAppearance1 == null) {
			scheduleListTimesFarTextAppearance1 = SpanUtils.getNewSmallTextAppearance(context);
		}
		return scheduleListTimesFarTextAppearance1;
	}

	@Nullable
	private static ForegroundColorSpan scheduleListTimesPastTextColor1 = null;

	@NonNull
	private static ForegroundColorSpan getScheduleListTimesPastTextColor1(@NonNull Context context) {
		if (scheduleListTimesPastTextColor1 == null) {
			scheduleListTimesPastTextColor1 = SpanUtils.getNewTextColor(getDefaultPastTextColor(context));
		}
		return scheduleListTimesPastTextColor1;
	}

	@Nullable
	private static ForegroundColorSpan statusStringsTextColor1 = null;

	@NonNull
	private static ForegroundColorSpan getStatusStringsTextColor1(@NonNull Context context) {
		if (statusStringsTextColor1 == null) {
			statusStringsTextColor1 = SpanUtils.getNewTextColor(POIStatus.getDefaultStatusTextColor(context));
		}
		return statusStringsTextColor1;
	}

	@Nullable
	private static ForegroundColorSpan statusStringsTextColor2 = null;

	@NonNull
	private static ForegroundColorSpan getStatusStringsTextColor2(@NonNull Context context) {
		if (statusStringsTextColor2 == null) {
			statusStringsTextColor2 = SpanUtils.getNewTextColor(POIStatus.getDefaultStatusTextColor(context));
		}
		return statusStringsTextColor2;
	}

	@Nullable
	private static ForegroundColorSpan statusStringsTextColor3 = null;

	@NonNull
	private static ForegroundColorSpan getStatusStringsTextColor3(@NonNull Context context) {
		if (statusStringsTextColor3 == null) {
			statusStringsTextColor3 = SpanUtils.getNewTextColor(POIStatus.getDefaultStatusTextColor(context));
		}
		return statusStringsTextColor3;
	}

	@Nullable
	private static TextAppearanceSpan noServiceTextAppearance = null;

	private static TextAppearanceSpan getNoServiceTextAppearance(@NonNull Context context) {
		if (noServiceTextAppearance == null) {
			noServiceTextAppearance = SpanUtils.getNewSmallTextAppearance(context);
		}
		return noServiceTextAppearance;
	}

	@Nullable
	private static ForegroundColorSpan noServiceTextColor = null;

	private static ForegroundColorSpan getNoServiceTextColor(@NonNull Context context) {
		if (noServiceTextColor == null) {
			noServiceTextColor = SpanUtils.getNewTextColor(ColorUtils.getTextColorTertiary(context));
		}
		return noServiceTextColor;
	}

	@Nullable
	private static TextAppearanceSpan scheduleListTimesFarTextAppearance = null;

	@NonNull
	private static TextAppearanceSpan getScheduleListTimesFarTextAppearance(@NonNull Context context) {
		if (scheduleListTimesFarTextAppearance == null) {
			scheduleListTimesFarTextAppearance = SpanUtils.getNewSmallTextAppearance(context);
		}
		return scheduleListTimesFarTextAppearance;
	}

	@Nullable
	private static TextAppearanceSpan scheduleListTimesCloseTextAppearance = null;

	@NonNull
	private static TextAppearanceSpan getScheduleListTimesCloseTextAppearance(@NonNull Context context) {
		if (scheduleListTimesCloseTextAppearance == null) {
			scheduleListTimesCloseTextAppearance = SpanUtils.getNewMediumTextAppearance(context);
		}
		return scheduleListTimesCloseTextAppearance;
	}

	@Nullable
	private static TextAppearanceSpan scheduleListTimesClosestTextAppearance = null;

	@NonNull
	private static TextAppearanceSpan getScheduleListTimesClosestTextAppearance(@NonNull Context context) {
		if (scheduleListTimesClosestTextAppearance == null) {
			scheduleListTimesClosestTextAppearance = SpanUtils.getNewLargeTextAppearance(context);
		}
		return scheduleListTimesClosestTextAppearance;
	}

	@Nullable
	private static ForegroundColorSpan scheduleListTimesPastTextColor = null;

	@NonNull
	private static ForegroundColorSpan getScheduleListTimesPastTextColor(@NonNull Context context) {
		if (scheduleListTimesPastTextColor == null) {
			scheduleListTimesPastTextColor = SpanUtils.getNewTextColor(getDefaultPastTextColor(context));
		}
		return scheduleListTimesPastTextColor;
	}

	@Nullable
	private static ForegroundColorSpan scheduleListTimesNowTextColor = null;

	@NonNull
	private static ForegroundColorSpan getScheduleListTimesNowTextColor(@NonNull Context context) {
		if (scheduleListTimesNowTextColor == null) {
			scheduleListTimesNowTextColor = SpanUtils.getNewTextColor(getDefaultNowTextColor(context));
		}
		return scheduleListTimesNowTextColor;
	}

	@Nullable
	private static ForegroundColorSpan scheduleListTimesFutureTextColor = null;

	@NonNull
	private static ForegroundColorSpan getScheduleListTimesFutureTextColor(@NonNull Context context) {
		if (scheduleListTimesFutureTextColor == null) {
			scheduleListTimesFutureTextColor = SpanUtils.getNewTextColor(getDefaultFutureTextColor(context));
		}
		return scheduleListTimesFutureTextColor;
	}

	@Nullable
	private static ImageSpan realTimeImage = null;

	@Nullable
	private static ImageSpan getRealTimeImage(@NonNull Context context) {
		if (realTimeImage == null) {
			realTimeImage = getNewRealTimeImage(context, false);
		}
		return realTimeImage;
	}

	@Nullable
	public static ImageSpan getNewRealTimeImage(@NonNull Context context, boolean countdown) {
		return UISpanUtils.getNewImage(context,
				(countdown ? R.drawable.ic_rss_feed_black_6dp : R.drawable.ic_rss_feed_black_12dp),
				ImageSpan.ALIGN_BASELINE);
	}

	@Nullable
	private static TextAppearanceSpan statusStringTextAppearance = null;

	@NonNull
	private static TextAppearanceSpan getStatusStringTextAppearance(@NonNull Context context) {
		if (statusStringTextAppearance == null) {
			statusStringTextAppearance = SpanUtils.getNewSmallTextAppearance(context);
		}
		return statusStringTextAppearance;
	}

	public static void resetColorCache() {
		noServiceTextColor = null;
		scheduleListTimesPastTextColor = null;
		scheduleListTimesPastTextColor1 = null;
		scheduleListTimesNowTextColor = null;
		scheduleListTimesFutureTextColor = null;
		scheduleListTimesFutureTextColor1 = null;
		statusStringsTextColor1 = null;
		statusStringsTextColor2 = null;
		statusStringsTextColor3 = null;
		realTimeImage = null;
	}

	@Nullable
	private ArrayList<Pair<CharSequence, CharSequence>> scheduleList = null;

	private long scheduleListTimestamp = -1L;

	@Nullable
	private CharSequence scheduleString = null;

	private long scheduleStringTimestamp = -1L;

	@Nullable
	private ArrayList<Pair<CharSequence, CharSequence>> statusStrings = null;

	private long statusStringsTimestamp = -1L;

	private UISchedule(@NonNull org.mtransit.android.commons.data.Schedule schedule) {
		this(
				schedule,
				schedule.getProviderPrecisionInMs(),
				schedule.isDescentOnly()
		);
		setTimestampsAndSort(schedule.getTimestamps());
		setFrequenciesAndSort(schedule.getFrequencies());
	}

	private UISchedule(@NonNull POIStatus status, long providerPrecisionInMs, boolean descentOnly) {
		super(status, providerPrecisionInMs, descentOnly);
	}

	UISchedule(@Nullable Integer id, @NonNull String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, long providerPrecisionInMs, boolean descentOnly, boolean noData) {
		super(id, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, providerPrecisionInMs, descentOnly, noData);
	}

	@Nullable
	public static UISchedule fromCursorWithExtra(@NonNull Cursor cursor) {
		org.mtransit.android.commons.data.Schedule schedule = org.mtransit.android.commons.data.Schedule.fromCursorWithExtra(cursor);
		if (schedule == null) {
			return null;
		}
		return new UISchedule(schedule);
	}

	@Nullable
	private Timestamp getLastTimestamp(long before, @Nullable Long optAfter) {
		Timestamp lastTimestamp = null;
		for (Timestamp timestamp : getTimestamps()) {
			if (timestamp.t >= before) {
				break;
			}
			if (optAfter != null && timestamp.t < optAfter) {
				continue; // skip
			}
			lastTimestamp = timestamp;
		}
		return lastTimestamp;
	}

	@Nullable
	private Timestamp getNextTimestamp(long after) {
		for (Timestamp timestamp : getTimestamps()) {
			if (timestamp.t >= after) {
				return timestamp;
			}
		}
		return null;
	}

	@Nullable
	private Frequency getCurrentFrequency(long after) {
		for (Frequency frequency : getFrequencies()) {
			if (frequency.startTimeInMs <= after && after <= frequency.endTimeInMs) {
				return frequency;
			}
		}
		return null;
	}

	@NonNull
	ArrayList<Timestamp> getNextTimestamps(long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
										   @Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		ArrayList<Timestamp> nextTimestamps = new ArrayList<>();
		boolean isAfter = false;
		int nbAfter = 0;
		Boolean minCoverageInMsCompleted = optMinCoverageInMs == null ? null : false;
		Boolean minCountCompleted = optMinCount == null ? null : false;
		for (Timestamp timestamp : getTimestamps()) {
			if (optMaxCoverageInMs != null && timestamp.t > after + optMaxCoverageInMs) {
				break;
			}
			if (minCoverageInMsCompleted != null && !minCoverageInMsCompleted && timestamp.t > after + optMinCoverageInMs) {
				if (minCountCompleted != null && minCountCompleted) {
					break;
				}
				minCoverageInMsCompleted = true;
			}
			if (!isAfter && timestamp.t >= after) {
				isAfter = true;
			}
			if (isAfter) {
				nextTimestamps.add(timestamp);
				nbAfter++;
				if (optMaxCount != null && nbAfter >= optMaxCount) {
					break; // enough time stamps found
				}
				if (minCountCompleted != null && !minCountCompleted && nbAfter >= optMinCount) {
					if (minCoverageInMsCompleted != null && minCoverageInMsCompleted) {
						break;
					}
					minCountCompleted = true;
				}
			}
		}
		return nextTimestamps;
	}

	// SCHEDULE LIST

	@Nullable
	public ArrayList<Pair<CharSequence, CharSequence>> getScheduleList(@NonNull Context context, long after,
																	   @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
																	   @Nullable Integer optMinCount, @Nullable Integer optMaxCount,
																	   @Nullable String optDefaultHeadSign) {
		if (this.scheduleList == null || this.scheduleListTimestamp != after) {
			generateScheduleList(context, after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount, optDefaultHeadSign);
		}
		return this.scheduleList;
	}

	private void generateScheduleList(@NonNull Context context, long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
									  @Nullable Integer optMinCount, @Nullable Integer optMaxCount, @Nullable String optDefaultHeadSign) {
		ArrayList<Timestamp> nextTimestamps =
				getNextTimestamps(after - getUIProviderPrecisionInMs(), optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		if (CollectionUtils.getSize(nextTimestamps) <= 0) { // NO SERVICE
			SpannableStringBuilder ssb = null;
			try {
				Timestamp timestamp = getNextTimestamp(after);
				if (timestamp != null && timestamp.t >= 0L) {
					ssb = new SpannableStringBuilder(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(timestamp.t)));
					ssb = decorateOldSchedule(timestamp, ssb);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing next timestamp date time!");
			}
			if (ssb == null) {
				ssb = new SpannableStringBuilder(context.getString(R.string.no_upcoming_departures));
			}
			ssb = SpanUtils.setAll(ssb, //
					getNoServiceTextAppearance(context), getNoServiceTextColor(context), NO_SERVICE_SIZE);
			this.scheduleList = new ArrayList<>();
			this.scheduleList.add(new Pair<>(ssb, null));
			this.scheduleListTimestamp = after;
			return;
		}
		Timestamp lastTimestamp = getLastTimestamp(after, after - TimeUnit.HOURS.toMillis(1L));
		if (lastTimestamp != null && !nextTimestamps.contains(lastTimestamp)) {
			nextTimestamps.add(0, lastTimestamp);
		}
		generateScheduleListTimes(context, after, nextTimestamps, optDefaultHeadSign);
		this.scheduleListTimestamp = after;
	}

	@SuppressWarnings("ConditionCoveredByFurtherCondition")
	private void generateScheduleListTimes(@NonNull Context context,
										   long after,
										   @NonNull ArrayList<Timestamp> nextTimestamps,
										   @Nullable String optDefaultHeadSign) {
		ArrayList<Pair<CharSequence, CharSequence>> list = new ArrayList<>();
		int startPreviousTimesIndex = -1, endPreviousTimesIndex = -1;
		int startPreviousTimeIndex = -1, endPreviousTimeIndex = -1;
		int startNextTimeIndex = -1, endNextTimeIndex = -1;
		int startNextNextTimeIndex = -1, endNextNextTimeIndex = -1;
		int startAfterNextTimesIndex = -1, endAfterNextTimesIndex = -1;
		int index = 0;
		for (Timestamp t : nextTimestamps) {
			if (endPreviousTimeIndex == -1) {
				if (t.t >= after) {
					if (startPreviousTimeIndex != -1) {
						endPreviousTimeIndex = index;
					}
				} else {
					endPreviousTimesIndex = index;
					startPreviousTimeIndex = endPreviousTimesIndex;
				}
			}
			if (t.t < after) {
				if (endPreviousTimeIndex == -1) {
					if (startPreviousTimesIndex == -1) {
						startPreviousTimesIndex = index;
					}
				}
			}
			if (t.t >= after) {
				if (startNextTimeIndex == -1) {
					startNextTimeIndex = index;
				}
			}
			index++;
			if (t.t >= after) {
				if (endNextTimeIndex == -1) {
					if (startNextTimeIndex != index) {
						endNextTimeIndex = index;
						startNextNextTimeIndex = endNextTimeIndex;
						endNextNextTimeIndex = startNextNextTimeIndex; // if was last, the same means empty
					}
				} else if (endNextNextTimeIndex != -1 && endNextNextTimeIndex == startNextNextTimeIndex) {
					endNextNextTimeIndex = index;
					startAfterNextTimesIndex = endNextNextTimeIndex;
					endAfterNextTimesIndex = startAfterNextTimesIndex; // if was last, the same means empty
				} else if (endAfterNextTimesIndex != -1 && startAfterNextTimesIndex != -1) {
					endAfterNextTimesIndex = index;
				}
			}
		}
		index = 0;
		final int nbSpaceBefore = 0;
		final int nbSpaceAfter = 0;
		for (Timestamp t : nextTimestamps) {
			index++;
			SpannableStringBuilder headSignSSB = null;
			String fTime = UITimeUtils.formatTime(context, t.t);
			fTime = cleanNoRealTime(t, fTime);
			SpannableStringBuilder timeSSB = new SpannableStringBuilder(fTime);
			if (t.hasHeadsign() && !Trip.isSameHeadsign(t.getHeading(context), optDefaultHeadSign)) {
				headSignSSB = new SpannableStringBuilder(t.getHeading(context).toUpperCase(Locale.ENGLISH));
			}
			if (startPreviousTimesIndex < endPreviousTimesIndex //
					&& index > startPreviousTimesIndex && index <= endPreviousTimesIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesFarTextAppearance(context), getScheduleListTimesPastTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesPastTextColor(context));
				}
			} else if (startPreviousTimeIndex < endPreviousTimeIndex //
					&& index > startPreviousTimeIndex && index <= endPreviousTimeIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesCloseTextAppearance(context), getScheduleListTimesPastTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesPastTextColor(context));
				}
			} else if (startNextTimeIndex < endNextTimeIndex //
					&& index > startNextTimeIndex && index <= endNextTimeIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesClosestTextAppearance(context), getScheduleListTimesNowTextColor(context), SCHEDULE_LIST_TIMES_STYLE);
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesNowTextColor(context));
				}
			} else if (startNextNextTimeIndex < endNextNextTimeIndex //
					&& index > startNextNextTimeIndex && index <= endNextNextTimeIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesCloseTextAppearance(context), getScheduleListTimesFutureTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesFutureTextColor(context));
				}
			} else if (startAfterNextTimesIndex < endAfterNextTimesIndex //
					&& index > startAfterNextTimesIndex && index <= endAfterNextTimesIndex) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesFarTextAppearance(context), getScheduleListTimesFutureTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesFutureTextColor(context));
				}
			}
			UITimeUtils.cleanTimes(timeSSB);
			timeSSB = SpanUtils.setAll(timeSSB, SCHEDULE_LIST_TIMES_SIZE);
			timeSSB = decorateRealTime(context, t, fTime, timeSSB);
			timeSSB = decorateOldSchedule(t, timeSSB);
			if (headSignSSB != null && headSignSSB.length() > 0) {
				headSignSSB = SpanUtils.setAll(headSignSSB, SCHEDULE_LIST_TIMES_STYLE);
			}
			list.add(new Pair<>(timeSSB, headSignSSB));
		}
		this.scheduleList = list;
	}

	@NonNull
	public static String cleanNoRealTime(@NonNull Timestamp t, @NonNull String fTime) {
		return cleanNoRealTime(t.isRealTime(), fTime);
	}

	@NonNull
	public static String cleanNoRealTime(boolean realTime, @NonNull String fTime) {
		if (!realTime) {
			fTime = fTime.replace(REAL_TIME_CHAR, StringUtils.EMPTY_CAR);
		}
		return fTime;
	}

	@NonNull
	public static SpannableStringBuilder decorateRealTime(@NonNull Context context,
														  @NonNull Timestamp t,
														  @NonNull String fTime,
														  @NonNull SpannableStringBuilder timeSSB) {
		if (t.isRealTime()) {
			int start = fTime.indexOf(REAL_TIME_CHAR);
			int end = start + 1;
			timeSSB = SpanUtils.set(timeSSB,
					start,
					end,
					getRealTimeImage(context)
			);
		}
		return timeSSB;
	}

	@NonNull
	public static SpannableStringBuilder decorateOldSchedule(@NonNull Timestamp t,
															 @NonNull SpannableStringBuilder timeSSB) {
		if (t.isOldSchedule()) {
			timeSSB = SpanUtils.setAllNN(timeSSB,
					SCHEDULE_OLD_SCHEDULE_STYLE
			);
		}
		return timeSSB;
	}

	// SCHEDULE

	@SuppressWarnings("unused")
	@Nullable
	public CharSequence getSchedule(@NonNull Context context, long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
									@Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		if (this.scheduleString == null || this.scheduleStringTimestamp != after) {
			generateSchedule(context, after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		}
		return this.scheduleString;
	}

	@SuppressWarnings("unused")
	private void generateSchedule(@NonNull Context context, long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
								  @Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		ArrayList<Timestamp> nextTimestamps =
				getNextTimestamps(after - getUIProviderPrecisionInMs(), optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		if (CollectionUtils.getSize(nextTimestamps) <= 0) { // NO SERVICE
			SpannableStringBuilder ssb = null;
			try {
				Timestamp timestamp = getNextTimestamp(after);
				if (timestamp != null && timestamp.t >= 0L) {
					ssb = new SpannableStringBuilder(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(timestamp.t)));
					ssb = decorateOldSchedule(timestamp, ssb);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing next timestamp date time!");
			}
			if (ssb == null) {
				ssb = new SpannableStringBuilder(context.getString(R.string.no_upcoming_departures));
			}
			ssb = SpanUtils.setAll(ssb, //
					getNoServiceTextAppearance(context), //
					getNoServiceTextColor(context), //
					NO_SERVICE_SIZE);
			this.scheduleString = ssb;
			this.scheduleStringTimestamp = after;
			return;
		}
		Timestamp lastTimestamp = getLastTimestamp(after, after - TimeUnit.HOURS.toMillis(1));
		if (lastTimestamp != null && !nextTimestamps.contains(lastTimestamp)) {
			nextTimestamps.add(0, lastTimestamp);
		}
		generateScheduleStringsTimes(context, after, nextTimestamps);
		this.scheduleStringTimestamp = after;
	}

	@SuppressWarnings("unused")
	private void generateScheduleStringsTimes(Context context, long after, ArrayList<Timestamp> nextTimestamps) {
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		int startPreviousTimes = -1, endPreviousTimes = -1;
		int startPreviousTime = -1, endPreviousTime = -1;
		int startNextTime = -1, endNextTime = -1;
		int startNextNextTime = -1, endNextNextTime = -1;
		int startAfterNextTimes = -1, endAfterNextTimes = -1;
		for (Timestamp t : nextTimestamps) {
			if (ssb.length() > 0) {
				ssb.append(StringUtils.SPACE_CAR).append(StringUtils.SPACE_CAR);
			}
			if (endPreviousTime == -1) {
				if (t.t >= after) {
					if (startPreviousTime != -1) {
						endPreviousTime = ssb.length();
					}
				} else {
					endPreviousTimes = ssb.length();
					startPreviousTime = endPreviousTimes;
				}
			}
			if (t.t < after) {
				if (endPreviousTime == -1) {
					if (startPreviousTimes == -1) {
						startPreviousTimes = ssb.length();
					}
				}
			}
			if (t.t >= after) {
				if (startNextTime == -1) {
					startNextTime = ssb.length();
				}
			}
			String fTime = UITimeUtils.formatTime(context, t.t);
			fTime = cleanNoRealTime(t, fTime);
			ssb.append(fTime);
			if (t.t >= after) {
				if (endNextTime == -1) {
					if (startNextTime != ssb.length()) {
						endNextTime = ssb.length();
						startNextNextTime = endNextTime;
						endNextNextTime = startNextNextTime; // if was last, the same means empty
					}
				} else if (endNextNextTime != -1 && endNextNextTime == startNextNextTime) {
					endNextNextTime = ssb.length();
					startAfterNextTimes = endNextNextTime;
					endAfterNextTimes = startAfterNextTimes; // if was last, the same means empty
				} else if (endAfterNextTimes != -1 && startAfterNextTimes != -1) {
					endAfterNextTimes = ssb.length();
				}
			}
		}
		if (startPreviousTimes < endPreviousTimes) {
			ssb = SpanUtils.set(ssb, startPreviousTimes, endPreviousTimes, //
					getScheduleListTimesFarTextAppearance(context), getScheduleListTimesPastTextColor(context));
		}
		if (startPreviousTime < endPreviousTime) {
			ssb = SpanUtils.set(ssb, startPreviousTime, endPreviousTime, //
					getScheduleListTimesCloseTextAppearance(context), getScheduleListTimesPastTextColor1(context));
		}
		if (startNextTime < endNextTime) {
			ssb = SpanUtils.set(ssb, startNextTime, endNextTime, //
					getScheduleListTimesClosestTextAppearance(context), getScheduleListTimesNowTextColor(context), SCHEDULE_LIST_TIMES_STYLE);
		}
		if (startNextNextTime < endNextNextTime) {
			ssb = SpanUtils.set(ssb, startNextNextTime, endNextNextTime, //
					getScheduleListTimesCloseTextAppearance1(context), getScheduleListTimesFutureTextColor(context));
		}
		if (startAfterNextTimes < endAfterNextTimes) {
			ssb = SpanUtils.set(ssb, startAfterNextTimes, endAfterNextTimes, //
					getScheduleListTimesFarTextAppearance1(context), getScheduleListTimesFutureTextColor1(context));
		}
		UITimeUtils.cleanTimes(ssb);
		ssb = SpanUtils.setAll(ssb, SpanUtils.getNew200PercentSizeSpan());
		this.scheduleString = ssb;
	}

	// STATUS

	@Nullable
	public ArrayList<Pair<CharSequence, CharSequence>> getStatus(@NonNull Context context, long after,
																 @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
																 @Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		if (this.statusStrings == null || this.statusStringsTimestamp != after) {
			generateStatus(context, after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		}
		return this.statusStrings;
	}

	private void generateStatus(@NonNull Context context, long after,
								@Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
								@Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		if (isNoData()) { // NO DATA
			generateStatusStringsNoService(context);
			this.statusStringsTimestamp = after;
			return;
		}
		if (isDescentOnly()) { // DESCENT ONLY
			if (this.statusStrings == null || this.statusStrings.size() == 0) {
				generateStatusStringsDescentOnly(context);
			} // ELSE descent only already set
			this.statusStringsTimestamp = after;
			return;
		}
		Frequency frequency = getCurrentFrequency(after);
		if (frequency != null && frequency.headwayInSec < MAX_FREQUENCY_DISPLAYED_IN_SEC) { // FREQUENCY
			generateStatusStringsFrequency(context, frequency);
			this.statusStringsTimestamp = after;
			return;
		}
		ArrayList<Timestamp> nextTimestamps = getStatusNextTimestamps(after, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		if (nextTimestamps.size() <= 0) { // NO SERVICE
			generateStatusStringsNoService(context);
			this.statusStringsTimestamp = after;
			return;
		}
		long diffInMs = nextTimestamps.get(0).getT() - after;
		// TODO diffInMs can be < 0 !! ?
		boolean isFrequentService = //
				!isDescentOnly() //
						&& diffInMs < UITimeUtils.FREQUENT_SERVICE_TIME_SPAN_IN_MS_DEFAULT //
						&& UITimeUtils.isFrequentService(nextTimestamps, -1, -1); // needs more than 3 services times!
		if (isFrequentService) { // FREQUENT SERVICE
			generateStatusStringsFrequentService(context);
			this.statusStringsTimestamp = after;
			return;
		}
		nextTimestamps = filterStatusNextTimestampsTimes(nextTimestamps);
		generateStatusStringsTimes(context, after, diffInMs, nextTimestamps);
		this.statusStringsTimestamp = after;
	}

	@NonNull
	static ArrayList<Timestamp> filterStatusNextTimestampsTimes(@NonNull ArrayList<Timestamp> nextTimestampList) {
		ArrayList<Timestamp> nextTimestampsT = new ArrayList<>();
		Long lastTimestamp = null;
		if (nextTimestampList.size() > 0) {
			for (Timestamp timestamp : nextTimestampList) {
				if (nextTimestampsT.contains(timestamp)) {
					continue; // skip duplicate time
				}
				if (lastTimestamp != null //
						&& (timestamp.getT() - lastTimestamp) < MIN_UI_PRECISION_IN_MS) {
					continue; // skip near duplicate time
				}
				nextTimestampsT.add(timestamp);
				lastTimestamp = timestamp.getT();
			}
		}
		return nextTimestampsT;
	}

	@NonNull
	ArrayList<Timestamp> getStatusNextTimestamps(long after, @Nullable Long optMinCoverageInMs, @Nullable Long optMaxCoverageInMs,
												 @Nullable Integer optMinCount, @Nullable Integer optMaxCount) {
		long usefulPastInMs = Math.max(MAX_LAST_STATUS_DIFF_IN_MS, getUIProviderPrecisionInMs());
		ArrayList<Timestamp> nextTimestampsT = getNextTimestamps(after - usefulPastInMs, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		if (nextTimestampsT.size() > 0) {
			Long theNextTimestamp = null;
			Long theLastTimestamp = null;
			for (Timestamp timestamp : nextTimestampsT) {
				if (timestamp.getT() >= after) {
					if (theNextTimestamp == null || timestamp.getT() < theNextTimestamp) {
						theNextTimestamp = timestamp.getT();
					}
				} else {
					if (theLastTimestamp == null || theLastTimestamp < timestamp.getT()) {
						theLastTimestamp = timestamp.getT();
					}
				}
			}
			Long oldestUsefulTimestamp = null;
			if (theNextTimestamp != null) {
				oldestUsefulTimestamp = after - ((theNextTimestamp - after) / 2L);
			}
			if (theLastTimestamp != null) {
				if (oldestUsefulTimestamp == null //
						|| oldestUsefulTimestamp < theLastTimestamp) {
					oldestUsefulTimestamp = theLastTimestamp;
				}
			}
			//noinspection ConstantConditions // TODO ?
			if (oldestUsefulTimestamp != null) {
				Iterator<Timestamp> it = nextTimestampsT.iterator();
				while (it.hasNext()) {
					Timestamp timestamp = it.next();
					if (timestamp.getT() < oldestUsefulTimestamp) {
						it.remove();
					}
				}
			}
		}
		return nextTimestampsT;
	}

	private void generateStatusStringsTimes(@NonNull Context context, long recentEnoughToBeNow, long diffInMs,
											@NonNull ArrayList<Timestamp> nextTimestamps) {
		Pair<CharSequence, CharSequence> statusCS = UITimeUtils.getShortTimeSpan(context,
				diffInMs,
				nextTimestamps.get(0),
				getUIProviderPrecisionInMs()
		);
		CharSequence line1CS;
		CharSequence line2CS;
		if (diffInMs < UITimeUtils.URGENT_SCHEDULE_IN_MS && nextTimestamps.size() > 1) { // URGENT & NEXT NEXT SCHEDULE
			if (statusCS.second == null || statusCS.second.length() == 0) {
				line1CS = SpanUtils.setAll(statusCS.first, getStatusStringsTextColor1(context));
			} else {
				line1CS = TextUtils.concat( //
						SpanUtils.setAll(statusCS.first, getStatusStringsTextColor1(context)), //
						SpanUtils.setAll(getNewStatusSpaceSSB(context), getStatusStringsTextColor2(context)), //
						SpanUtils.setAll(statusCS.second, getStatusStringsTextColor3(context))
				);
			}
			long diff2InMs = nextTimestamps.get(1).getT() - recentEnoughToBeNow;
			Pair<CharSequence, CharSequence> nextStatusCS = UITimeUtils.getShortTimeSpan(context,
					diff2InMs,
					nextTimestamps.get(1),
					getUIProviderPrecisionInMs()
			);
			if (nextStatusCS.second == null || nextStatusCS.second.length() == 0) {
				line2CS = SpanUtils.setAll(nextStatusCS.first, getStatusStringsTextColor1(context));
			} else {
				line2CS = TextUtils.concat( //
						SpanUtils.setAll(nextStatusCS.first, getStatusStringsTextColor1(context)), //
						SpanUtils.setAll(getNewStatusSpaceSSB(context), getStatusStringsTextColor2(context)), //
						SpanUtils.setAll(nextStatusCS.second, getStatusStringsTextColor3(context)) //
				);
			}
		} else { // NEXT SCHEDULE ONLY (large numbers)
			if (diffInMs < UITimeUtils.MAX_DURATION_SHOW_NUMBER_IN_MS) {
				line1CS = SpanUtils.setAll(statusCS.first, //
						getStatusStringsTimesNumberShownTextAppearance(context), //
						getStatusStringsTextColor1(context));
			} else {
				line1CS = SpanUtils.setAll(statusCS.first, //
						getStatusStringsTextColor1(context));
			}
			if (!TextUtils.isEmpty(statusCS.second)) {
				line2CS = SpanUtils.setAll(statusCS.second, getStatusStringsTextColor1(context));
			} else {
				line2CS = null;
			}
		}
		this.statusStrings = new ArrayList<>();
		this.statusStrings.add(new Pair<>(line1CS, line2CS));
	}

	@Nullable
	private static TextAppearanceSpan statusStringsTimesNumberShownTextAppearance = null;

	@NonNull
	private static TextAppearanceSpan getStatusStringsTimesNumberShownTextAppearance(@NonNull Context context) {
		if (statusStringsTimesNumberShownTextAppearance == null) {
			statusStringsTimesNumberShownTextAppearance = SpanUtils.getNewLargeTextAppearance(context);
		}
		return statusStringsTimesNumberShownTextAppearance;
	}

	private static final TypefaceSpan STATUS_FONT = SpanUtils.getNewTypefaceSpan(POIStatus.getStatusTextFont());

	@Nullable
	private static TextAppearanceSpan statusSpaceTextAppearance = null;

	private static TextAppearanceSpan getStatusSpaceTextAppearance(@NonNull Context context) {
		if (statusSpaceTextAppearance == null) {
			statusSpaceTextAppearance = SpanUtils.getNewSmallTextAppearance(context);
		}
		return statusSpaceTextAppearance;
	}

	@NonNull
	private SpannableStringBuilder getNewStatusSpaceSSB(@NonNull Context context) {
		return SpanUtils.setAll( //
				new SpannableStringBuilder(StringUtils.SPACE_STRING), //
				getStatusSpaceTextAppearance(context), STATUS_FONT);
	}

	private void generateStatusStringsFrequency(@NonNull Context context, @NonNull Frequency frequency) {
		int headwayInMin = frequency.headwayInSec / 60;
		CharSequence headway = UITimeUtils.getNumberInLetter(context, headwayInMin);
		final SpannableStringBuilder cs1 = new SpannableStringBuilder(context.getResources().getQuantityString(R.plurals.every_minutes_and_quantity_part_1, headwayInMin, headway));
		final SpannableStringBuilder cs2 = new SpannableStringBuilder(context.getResources().getQuantityString(R.plurals.every_minutes_and_quantity_part_2, headwayInMin, headway));
		if (frequency.isOldSchedule()) {
			SpanUtils.setAll(cs1, SCHEDULE_OLD_SCHEDULE_STYLE);
			SpanUtils.setAll(cs2, SCHEDULE_OLD_SCHEDULE_STYLE);
		}
		generateStatusStrings(context, cs1, cs2);
	}

	private void generateStatusStringsNoService(@NonNull Context context) {
		generateStatusStrings(context, context.getString(R.string.no_service_part_1), context.getString(R.string.no_service_part_2));
	}

	private void generateStatusStringsFrequentService(@NonNull Context context) {
		generateStatusStrings(context, context.getString(R.string.frequent_service_part_1), context.getString(R.string.frequent_service_part_2));
	}

	private void generateStatusStringsDescentOnly(@NonNull Context context) {
		generateStatusStrings(context, context.getString(R.string.descent_only_part_1), context.getString(R.string.descent_only_part_2));
	}

	private void generateStatusStrings(@NonNull Context context,
									   CharSequence cs1,
									   CharSequence cs2) {
		this.statusStrings = new ArrayList<>();
		this.statusStrings.add(new Pair<>(//
				SpanUtils.setAll(cs1, //
						getStatusStringTextAppearance(context), //
						STATUS_FONT, //
						getStatusStringsTextColor1(context)), //
				SpanUtils.setAll(cs2, //
						getStatusStringTextAppearance(context), //
						STATUS_FONT, //
						getStatusStringsTextColor2(context))));
	}
}
