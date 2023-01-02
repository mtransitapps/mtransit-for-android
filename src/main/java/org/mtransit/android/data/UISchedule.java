package org.mtransit.android.data;

import static org.mtransit.commons.Constants.SPACE;

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
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.util.UIDirectionUtils;
import org.mtransit.android.util.UISpanUtils;
import org.mtransit.android.util.UITimeUtils;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UISchedule extends org.mtransit.android.commons.data.Schedule implements MTLog.Loggable {

	private static final String LOG_TAG = UISchedule.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

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
				true,
				true,
				true,
				ImageSpan.ALIGN_BASELINE
		);
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
	private ArrayList<DetailsNextDepartures> scheduleList = null;

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
				schedule.isNoPickup()
		);
		setTimestampsAndSort(schedule.getTimestamps());
		setFrequenciesAndSort(schedule.getFrequencies());
	}

	private UISchedule(@NonNull POIStatus status, long providerPrecisionInMs, boolean noPickup) {
		super(status, providerPrecisionInMs, noPickup);
	}

	UISchedule(@Nullable Integer id, @NonNull String targetUUID, long lastUpdateInMs, long maxValidityInMs, long readFromSourceAtInMs, long providerPrecisionInMs, boolean noPickup, boolean noData) {
		super(id, targetUUID, lastUpdateInMs, maxValidityInMs, readFromSourceAtInMs, providerPrecisionInMs, noPickup, noData);
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
				break; // max coverage date range completed
			}
			if (minCoverageInMsCompleted != null && !minCoverageInMsCompleted && timestamp.t > after + optMinCoverageInMs) {
				if (minCountCompleted != null && minCountCompleted) {
					break; // min coverage count (and min coverage date range) completed
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
					break; // max coverage count completed
				}
				if (minCountCompleted != null && !minCountCompleted && nbAfter >= optMinCount) {
					if (minCoverageInMsCompleted != null && minCoverageInMsCompleted) {
						break; // min coverage date range completed
					}
					minCountCompleted = true;
				}
			}
		}
		return nextTimestamps;
	}

	// SCHEDULE LIST

	@Nullable
	public ArrayList<DetailsNextDepartures> getScheduleList(@NonNull Context context, long after,
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
		ArrayList<Timestamp> timestamps =
				getNextTimestamps(after - getUIProviderPrecisionInMs(), optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		if (CollectionUtils.getSize(timestamps) <= 0) { // NO SERVICE
			SpannableStringBuilder ssb = null;
			try {
				Timestamp timestamp = getNextTimestamp(after);
				if (timestamp != null && timestamp.t >= 0L) {
					ssb = new SpannableStringBuilder(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(timestamp.t)));
					decorateOldSchedule(timestamp, ssb);
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
			this.scheduleList.add(new DetailsNextDepartures(ssb));
			this.scheduleListTimestamp = after;
			return;
		}
		final Timestamp lastTimestamp = getLastTimestamp(after, after - TimeUnit.MINUTES.toMillis(60L));
		if (lastTimestamp != null && !timestamps.contains(lastTimestamp)) {
			if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
				if (!lastTimestamp.isNoPickup()
						|| lastTimestamp.t > after - TimeUnit.MINUTES.toMillis(30L)) {
					timestamps.add(0, lastTimestamp);
				}
			} else {
				timestamps.add(0, lastTimestamp);
			}
		}
		generateScheduleListTimes(context, after, timestamps, optDefaultHeadSign);
		this.scheduleListTimestamp = after;
	}

	@SuppressWarnings("ConditionCoveredByFurtherCondition")
	private void generateScheduleListTimes(@NonNull Context context,
										   long after,
										   @NonNull List<Timestamp> timestamps,
										   @Nullable String optDefaultHeadSign) {
		// 1 - Find the start/end time sections
		final TimeSections ts = findTimesSectionsStartEnd(after, timestamps);
		// 2 - Set spannable with style
		int idx = 0;
		final int nbSpaceBefore = 0;
		final int nbSpaceAfter = 0;
		ArrayList<DetailsNextDepartures> list = new ArrayList<>();
		long lastTimestamp = -1L;
		for (Timestamp t : timestamps) {
			idx++;
			SpannableStringBuilder headSignSSB = null;
			SpannableStringBuilder dateSSB = null;
			String fTime = UITimeUtils.formatTime(context, t);
			SpannableStringBuilder timeSSB = new SpannableStringBuilder(fTime);
			if (t.hasHeadsign() && !Trip.isSameHeadsign(t.getHeading(context), optDefaultHeadSign)) {
				headSignSSB = new SpannableStringBuilder(
						UIDirectionUtils.decorateDirection(context,
								t.getUIHeading(context, true)
						)
				);
			}
			if (lastTimestamp > 0L) {
				if (!UITimeUtils.isSameDay(lastTimestamp, t.t)) {
					dateSSB = new SpannableStringBuilder(UITimeUtils.formatNearDate(context, t.t));
				}
			} else { // 1st timestamp
				long diffInMs = t.t - after;
				if (UITimeUtils.isSameDay(after, t.t)) {
					dateSSB = new SpannableStringBuilder(context.getString(R.string.today));
				} else if (diffInMs < TimeUnit.HOURS.toMillis(24L)) {
					Pair<CharSequence, CharSequence> shortTimeSpam;
					if (diffInMs < UITimeUtils.MAX_DURATION_DISPLAYED_IN_MS) {
						shortTimeSpam = UITimeUtils.getShortTimeSpanString(context, diffInMs, t.t); // avoid countdown
					} else {
						shortTimeSpam = UITimeUtils.getShortTimeSpan(context, diffInMs, t, getUIProviderPrecisionInMs());
					}
					dateSSB = new SpannableStringBuilder(shortTimeSpam.first);
					if (!TextUtils.isEmpty(shortTimeSpam.second)) {
						dateSSB.append(SPACE).append(shortTimeSpam.second);
					}
				} else {
					dateSSB = new SpannableStringBuilder(UITimeUtils.formatNearDate(context, t.t));
				}
			}
			if (ts.previousTimesStartIdx < ts.previousTimesEndIdx // IF previous times list DO
					&& idx > ts.previousTimesStartIdx && idx <= ts.previousTimesEndIdx) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesFarTextAppearance(context), getScheduleListTimesPastTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesPastTextColor(context));
				}
			} else if (ts.previousTimeStartIdx < ts.previousTimeEndIdx // ELSE IF the previous time DO
					&& idx > ts.previousTimeStartIdx && idx <= ts.previousTimeEndIdx) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesCloseTextAppearance(context), getScheduleListTimesPastTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesPastTextColor(context));
				}
			} else if (ts.nextTimeStartIdx < ts.nextTimeEndIdx // ELSE IF the next time DO
					&& idx > ts.nextTimeStartIdx && idx <= ts.nextTimeEndIdx) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesClosestTextAppearance(context), getScheduleListTimesNowTextColor(context), SCHEDULE_LIST_TIMES_STYLE);
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesNowTextColor(context));
				}
			} else if (ts.nextNextTimeStartIdx < ts.nextNextTimeEndIdx // ELSE IF the time after next DO
					&& idx > ts.nextNextTimeStartIdx && idx <= ts.nextNextTimeEndIdx) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesCloseTextAppearance(context), getScheduleListTimesFutureTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesFutureTextColor(context));
				}
			} else if (ts.afterNextTimesStartIdx < ts.afterNextTimesEndIdx // ELSE IF other next times list DO
					&& idx > ts.afterNextTimesStartIdx && idx <= ts.afterNextTimesEndIdx) {
				timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
						getScheduleListTimesFarTextAppearance(context), getScheduleListTimesFutureTextColor(context));
				if (headSignSSB != null && headSignSSB.length() > 0) {
					headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesFutureTextColor(context));
				}
			}
			if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
				if (idx > ts.nextTimeStartIdx && t.isNoPickup()) { // IF at least next time (not in the past) DO
					timeSSB = SpanUtils.set(timeSSB, nbSpaceBefore, timeSSB.length() - nbSpaceAfter, //
							getScheduleListTimesPastTextColor(context));
					if (headSignSSB != null && headSignSSB.length() > 0) {
						headSignSSB = SpanUtils.setAll(headSignSSB, getScheduleListTimesPastTextColor(context));
					}
				}
			}
			UITimeUtils.cleanTimes(timeSSB);
			timeSSB = SpanUtils.setAll(timeSSB, SCHEDULE_LIST_TIMES_SIZE);
			timeSSB = decorateRealTime(context, t, fTime, timeSSB);
			decorateOldSchedule(t, timeSSB);
			if (headSignSSB != null && headSignSSB.length() > 0) {
				headSignSSB = SpanUtils.setAll(headSignSSB, SCHEDULE_LIST_TIMES_STYLE);
			}
			list.add(new DetailsNextDepartures(timeSSB, headSignSSB, dateSSB));
			lastTimestamp = t.t;
		}
		this.scheduleList = list;
	}

	@NonNull
	static TimeSections findTimesSectionsStartEnd(long after, @NonNull List<Timestamp> timestamps) {
		long afterNext = after;
		boolean hasNoPickupOnly = CollectionUtils.count(timestamps, timestamp -> !timestamp.isNoPickup()) == 0;
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			for (Timestamp t : timestamps) {
				if (t.t >= afterNext
						&& !t.isNoPickup()) {
					if (afterNext < t.t) {
						afterNext = t.t;
					}
					break;
				}
			}
		}
		final TimeSections ts = new TimeSections();
		int idx = 0;
		for (Timestamp t : timestamps) {
			if (ts.previousTimeEndIdx == -1) { // IF the previous time end NOT found DO
				if (t.t >= afterNext) { // IF timestamp after now DO
					if (ts.previousTimeStartIdx != -1) { // IF the previous time start found DO
						ts.previousTimeEndIdx = idx; // mark the previous end
					}
				} else { // ELSE IF timestamp before now DO
					ts.previousTimesEndIdx = idx; // mark previous times list end
					ts.previousTimeStartIdx = ts.previousTimesEndIdx; // mark previous times list start
				}
			}
			if (t.t < afterNext) { // IF timestamp before now DO
				if (ts.previousTimeEndIdx == -1) { // IF the previous time end NOT found DO
					if (ts.previousTimesStartIdx == -1) { // IF previous times list start NOT found DO
						ts.previousTimesStartIdx = idx; // mark previous times list start
					}
				}
			}
			if (t.t >= afterNext) { // IF timestamp after now DO
				if (ts.nextTimeStartIdx == -1) { // IF the next time start NOT found DO
					ts.nextTimeStartIdx = idx; // mark the next time start
				} else {
					if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI && !hasNoPickupOnly) {
						if (!t.isNoPickup()) {
							if (ts.nextNextTimeStartIdx == -1) { // ELSE IF the time after next start NOT FOUND
								ts.nextNextTimeStartIdx = idx; // mark the time after next start
							}
						}
					}
				}
			}
			idx++;
			if (t.t >= afterNext) { // IF timestamp after now DO
				if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI && !hasNoPickupOnly) {
					if (t.isNoPickup()) {
						if (ts.afterNextTimesStartIdx == -1) { // IF other next times list start NOT found DO
							ts.afterNextTimesStartIdx = ts.nextTimeEndIdx; // mark other next times list start
						}
						ts.afterNextTimesEndIdx = idx; // mark other next times list end
					} else {
						if (ts.nextTimeEndIdx == -1) { // IF the next time end NOT found DO
							if (ts.nextTimeStartIdx != idx) { // IF the next time start NOT equal index DO
								ts.nextTimeEndIdx = idx; // mark the next time end
							}
						} else if (ts.nextNextTimeStartIdx != -1  // ELSE IF the time after next start found
								&& ts.nextNextTimeEndIdx == -1) { // AND IF the time after next end NOT found DO
							ts.nextNextTimeEndIdx = idx; // mark the time after next end
							if (ts.afterNextTimesStartIdx == -1) { // IF other next times list start found DO
								ts.afterNextTimesStartIdx = ts.nextNextTimeEndIdx; // mark other next times list start
								ts.afterNextTimesEndIdx = ts.afterNextTimesStartIdx; // mark other next times list end // if was last, the same means empty
							}
						} else if (ts.afterNextTimesEndIdx != -1  // ELSE IF other next times list end found
								&& ts.afterNextTimesStartIdx != -1) { // AND IF other next times list start found DO
							ts.afterNextTimesEndIdx = idx; // mark other next times list end
						}
					}
				} else {
					if (ts.nextTimeEndIdx == -1) { // IF the next time end NOT found DO
						if (ts.nextTimeStartIdx != idx) { // IF the next time start NOT equal index DO
							ts.nextTimeEndIdx = idx; // mark the next time end
							ts.nextNextTimeStartIdx = ts.nextTimeEndIdx; // mark the time after next start
							ts.nextNextTimeEndIdx = ts.nextNextTimeStartIdx; // mark the time after next end // if was last, the same means empty
						}
					} else if (ts.nextNextTimeEndIdx != -1 // ELSE IF the time after next end found
							&& ts.nextNextTimeEndIdx == ts.nextNextTimeStartIdx) { // AND IF the time after next end equals start DO
						ts.nextNextTimeEndIdx = idx; // mark the time after next end
						ts.afterNextTimesStartIdx = ts.nextNextTimeEndIdx; // mark other next times list start
						ts.afterNextTimesEndIdx = ts.afterNextTimesStartIdx; // mark other next times list end // if was last, the same means empty
					} else if (ts.afterNextTimesEndIdx != -1  // ELSE IF other next times list end found
							&& ts.afterNextTimesStartIdx != -1) { // AND IF other next times list start found DO
						ts.afterNextTimesEndIdx = idx; // mark other next times list end
					}
				}
			}
		}
		return ts;
	}

	@NonNull
	public static SpannableStringBuilder decorateRealTime(@NonNull Context context,
														  @NonNull Timestamp t,
														  @NonNull String fTime,
														  @NonNull SpannableStringBuilder timeSSB) {
		if (t.isRealTime()) {
			int start = fTime.indexOf(UITimeUtils.REAL_TIME_CHAR);
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
			SpanUtils.setAllNN(timeSSB,
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
					decorateOldSchedule(timestamp, ssb);
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
		Timestamp lastTimestamp = getLastTimestamp(after, after - TimeUnit.HOURS.toMillis(1L));
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
			String fTime = UITimeUtils.formatTime(context, t);
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
				} else //noinspection ConstantConditions
					if (endAfterNextTimes != -1 && startAfterNextTimes != -1) {
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
		if (isNoPickup()) { // DESCENT ONLY
			if (this.statusStrings == null || this.statusStrings.size() == 0) {
				generateStatusStringsNoPickup(context);
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
		if (FeatureFlags.F_SCHEDULE_DESCENT_ONLY_UI) {
			CollectionUtils.removeIfNN(nextTimestamps, Timestamp::isNoPickup);
			if (nextTimestamps.size() == 0) { // DESCENT ONLY SERVICE
				if (this.statusStrings == null || this.statusStrings.size() == 0) {
					generateStatusStringsNoPickup(context);
				} // ELSE descent only already set
				this.statusStringsTimestamp = after;
				return;
			}
		}
		long diffInMs = nextTimestamps.get(0).getT() - after;
		// TODO diffInMs can be < 0 !! ?
		boolean isFrequentService = //
				!isNoPickup() //
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
	ArrayList<Timestamp> getStatusNextTimestamps(long after, // truncated to the minute
												 @Nullable Long optMinCoverageInMs,
												 @Nullable Long optMaxCoverageInMs,
												 @Nullable Integer optMinCount,
												 @Nullable Integer optMaxCount) {
		final long usefulPastInMs = Math.max(MAX_LAST_STATUS_DIFF_IN_MS, getUIProviderPrecisionInMs());
		ArrayList<Timestamp> nextTimestampsT = getNextTimestamps(after - usefulPastInMs, optMinCoverageInMs, optMaxCoverageInMs, optMinCount, optMaxCount);
		if (nextTimestampsT.size() > 0) {
			Long theNextTimestamp = null;
			Long thePreviousTimestamp = null;
			for (Timestamp timestamp : nextTimestampsT) {
				if (timestamp.getT() < after) {
					if (thePreviousTimestamp == null || thePreviousTimestamp < timestamp.getT()) {
						thePreviousTimestamp = timestamp.getT();
					}
				} else {
					if (theNextTimestamp == null || timestamp.getT() <= theNextTimestamp) {
						theNextTimestamp = timestamp.getT();
					}
				}
			}
			Long oldestUsefulTimestamp = null;
			if (theNextTimestamp != null) {
				oldestUsefulTimestamp = after - ((theNextTimestamp - after) / 2L);
			}
			if (thePreviousTimestamp != null) {
				if (oldestUsefulTimestamp == null //
						|| oldestUsefulTimestamp < thePreviousTimestamp) {
					oldestUsefulTimestamp = thePreviousTimestamp;
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

	private void generateStatusStringsNoPickup(@NonNull Context context) {
		generateStatusStrings(context, context.getString(R.string.drop_off_only_part_1), context.getString(R.string.drop_off_only_part_2));
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

	static class TimeSections {
		int previousTimesStartIdx = -1, previousTimesEndIdx = -1; // previous times list
		int previousTimeStartIdx = -1, previousTimeEndIdx = -1; // the preview time
		int nextTimeStartIdx = -1, nextTimeEndIdx = -1; // the next time
		int nextNextTimeStartIdx = -1, nextNextTimeEndIdx = -1; // the time after next
		int afterNextTimesStartIdx = -1, afterNextTimesEndIdx = -1; // other next times list

		@NonNull
		@Override
		public String toString() {
			return TimeSections.class.getSimpleName() + "{" +
					"previousTimesStartIdx=" + previousTimesStartIdx +
					", previousTimesEndIdx=" + previousTimesEndIdx +
					", previousTimeStartIdx=" + previousTimeStartIdx +
					", previousTimeEndIdx=" + previousTimeEndIdx +
					", nextTimeStartIdx=" + nextTimeStartIdx +
					", nextTimeEndIdx=" + nextTimeEndIdx +
					", nextNextTimeStartIdx=" + nextNextTimeStartIdx +
					", nextNextTimeEndIdx=" + nextNextTimeEndIdx +
					", afterNextTimesStartIdx=" + afterNextTimesStartIdx +
					", afterNextTimesEndIdx=" + afterNextTimesEndIdx +
					'}';
		}
	}

	public static class DetailsNextDepartures {

		@NonNull
		public CharSequence time;
		@Nullable
		public CharSequence headSign;
		@Nullable
		public CharSequence date;

		DetailsNextDepartures(@NonNull CharSequence time) {
			this(time, null);
		}

		DetailsNextDepartures(@NonNull CharSequence time, @Nullable CharSequence headSign) {
			this(time, headSign, null);
		}

		DetailsNextDepartures(@NonNull CharSequence time, @Nullable CharSequence headSign, @Nullable CharSequence date) {
			this.time = time;
			this.headSign = headSign;
			this.date = date;
		}
	}
}
