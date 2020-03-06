package org.mtransit.android.ui.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.data.POIManager;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class POIStatusDetailViewController implements MTLog.Loggable {

	private static final String TAG = POIStatusDetailViewController.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	@Nullable
	public static Integer getLayoutResId(POIManager poim) {
		if (poim != null) {
			switch (poim.getStatusType()) {
			case POI.ITEM_STATUS_TYPE_NONE:
				return null;
			case POI.ITEM_STATUS_TYPE_APP:
				return R.layout.layout_poi_detail_status_app;
			case POI.ITEM_STATUS_TYPE_SCHEDULE:
				return R.layout.layout_poi_detail_status_schedule;
			case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
				return R.layout.layout_poi_detail_status_availability_percent;
			}
		}
		MTLog.w(TAG, "getLayoutResId() > Unknow view type for poi %s!", poim);
		return null;
	}

	public static void initViewHolder(POIManager poim, View view) {
		switch (poim.getStatusType()) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			initAppStatusViewHolder(poim, view);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			initScheduleViewHolder(poim, view);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			initAvailabilityPercentViewHolder(poim, view);
			break;
		default:
			MTLog.w(TAG, "initViewHolder() > Unknow view status type for poi %s!", poim);
		}
	}

	private static void initAppStatusViewHolder(POIManager poim, View view) {
		AppStatusViewHolder appStatusViewHolder = new AppStatusViewHolder();
		initCommonStatusViewHolderHolder(appStatusViewHolder, view);
		appStatusViewHolder.textTv = view.findViewById(R.id.textTv);
		view.setTag(appStatusViewHolder);
	}

	private static void initAvailabilityPercentViewHolder(POIManager poim, View view) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = new AvailabilityPercentStatusViewHolder();
		initCommonStatusViewHolderHolder(availabilityPercentStatusViewHolder, view);
		availabilityPercentStatusViewHolder.textTv1 = view.findViewById(R.id.progress_text1);
		availabilityPercentStatusViewHolder.textTv1SubValue1 = view.findViewById(R.id.progress_text1_sub_value1);
		availabilityPercentStatusViewHolder.textTv2 = view.findViewById(R.id.progress_text2);
		availabilityPercentStatusViewHolder.progressBar = view.findViewById(R.id.progress_bar);
		if (poim != null) {
			availabilityPercentStatusViewHolder.progressBar.getProgressDrawable().setColorFilter(poim.getColor(view.getContext()), PorterDuff.Mode.SRC_IN);
		}
		view.setTag(availabilityPercentStatusViewHolder);
	}

	private static void initScheduleViewHolder(POIManager poim, View view) {
		ScheduleStatusViewHolder scheduleStatusViewHolder = new ScheduleStatusViewHolder();
		initCommonStatusViewHolderHolder(scheduleStatusViewHolder, view);
		scheduleStatusViewHolder.nextDeparturesLL = view.findViewById(R.id.next_departures_layout);
		view.setTag(scheduleStatusViewHolder);
	}

	public static void updatePOIStatus(Context context, View view, POIStatus status, POIViewController.POIDataProvider dataProvider, POIManager optPOI) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			return;
		}
		CommonStatusViewHolder holder = (CommonStatusViewHolder) view.getTag();
		updatePOIStatus(context, holder, status, dataProvider, optPOI);
	}

	private static void updatePOIStatus(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status,
										POIViewController.POIDataProvider dataProvider, POIManager optPOI) {
		if (dataProvider == null || !dataProvider.isShowingStatus() || status == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				setStatusView(statusViewHolder, false);
			}
			return;
		}
		switch (status.getType()) {
		case POI.ITEM_STATUS_TYPE_NONE:
			setStatusView(statusViewHolder, false);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			updateAvailabilityPercentView(context, statusViewHolder, status);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			updateScheduleView(context, statusViewHolder, status, dataProvider, optPOI);
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			updateAppStatusView(context, statusViewHolder, status);
			break;
		default:
			MTLog.w(TAG, "Unexpected status type '%s'!", status.getType());
			setStatusView(statusViewHolder, false);
		}
	}

	public static void updateView(Context context, View view, POIManager poim, POIViewController.POIDataProvider dataProvider) {
		if (view == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonStatusViewHolder holder = (CommonStatusViewHolder) view.getTag();
		updateView(context, holder, poim, dataProvider);
	}

	private static void updateView(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim, POIViewController.POIDataProvider dataProvider) {
		if (dataProvider == null || !dataProvider.isShowingStatus() || poim == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				setStatusView(statusViewHolder, false);
			}
			return;
		}
		switch (poim.getStatusType()) {
		case POI.ITEM_STATUS_TYPE_NONE:
			setStatusView(statusViewHolder, false);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			updateAvailabilityPercentView(context, statusViewHolder, poim, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			updateScheduleView(context, statusViewHolder, poim, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			updateAppStatusView(context, statusViewHolder, poim, dataProvider);
			break;
		default:
			MTLog.w(TAG, "Unexpected status type '%s'!", poim.getStatusType());
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAppStatusView(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim,
											POIViewController.POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof AppStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAppStatusView(context, statusViewHolder, poim.getStatus(context));
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAppStatusView(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status) {
		AppStatusViewHolder appStatusViewHolder = (AppStatusViewHolder) statusViewHolder;
		if (status instanceof AppStatus) {
			AppStatus appStatus = (AppStatus) status;
			appStatusViewHolder.textTv.setText(appStatus.getStatusMsg(context), TextView.BufferType.SPANNABLE);
			appStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			setStatusView(statusViewHolder, true);
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAvailabilityPercentView(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim,
													  POIViewController.POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof AvailabilityPercentStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAvailabilityPercentView(context, statusViewHolder, poim.getStatus(context));
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAvailabilityPercentView(@NonNull Context context, @NonNull CommonStatusViewHolder statusViewHolder, @Nullable POIStatus status) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = (AvailabilityPercentStatusViewHolder) statusViewHolder;
		if (status instanceof AvailabilityPercent) {
			AvailabilityPercent availabilityPercent = (AvailabilityPercent) status;
			if (!availabilityPercent.isStatusOK()) {
				availabilityPercentStatusViewHolder.textTv2.setVisibility(View.GONE);
				availabilityPercentStatusViewHolder.textTv1.setText(availabilityPercent.getStatusMsg(context), TextView.BufferType.SPANNABLE);
				availabilityPercentStatusViewHolder.textTv1.setVisibility(View.VISIBLE);
			} else {
				final CharSequence value1SubValue1Text = availabilityPercent.getValue1SubValue1Text(context);
				if (value1SubValue1Text == null) { // NO SUB-VALUE
					availabilityPercentStatusViewHolder.textTv1.setText(availabilityPercent.getValue1Text(context, false), TextView.BufferType.SPANNABLE);
					availabilityPercentStatusViewHolder.textTv1.setVisibility(View.VISIBLE);
					availabilityPercentStatusViewHolder.textTv1SubValue1.setVisibility(View.GONE);
				} else { // WITH SUB-VALUE
					availabilityPercentStatusViewHolder.textTv1.setText(availabilityPercent.getValue1SubValueDefaultText(context), TextView.BufferType.SPANNABLE);
					availabilityPercentStatusViewHolder.textTv1.setVisibility(View.VISIBLE);
					availabilityPercentStatusViewHolder.textTv1SubValue1.setText(value1SubValue1Text, TextView.BufferType.SPANNABLE);
					availabilityPercentStatusViewHolder.textTv1SubValue1.setVisibility(View.VISIBLE);
				}
				availabilityPercentStatusViewHolder.textTv2.setText(availabilityPercent.getValue2Text(context), TextView.BufferType.SPANNABLE);
				availabilityPercentStatusViewHolder.textTv2.setVisibility(View.VISIBLE);
			}
			availabilityPercentStatusViewHolder.progressBar.setIndeterminate(false);
			availabilityPercentStatusViewHolder.progressBar.setMax(availabilityPercent.getTotalValue());
			availabilityPercentStatusViewHolder.progressBar.setProgress(availabilityPercent.getValue1());
			availabilityPercentStatusViewHolder.progressBar.setVisibility(View.VISIBLE);
			setStatusView(statusViewHolder, true);
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateScheduleView(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim,
										   POIViewController.POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof ScheduleStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateScheduleView(context, statusViewHolder, poim.getStatus(context), dataProvider, poim);
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateScheduleView(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status,
										   POIViewController.POIDataProvider dataProvider, POIManager optPOI) {
		ArrayList<Pair<CharSequence, CharSequence>> nextDeparturesList = null;
		if (dataProvider != null && status instanceof Schedule) {
			Schedule schedule = (Schedule) status;
			String defaultHeadSign = (optPOI != null && optPOI.poi instanceof RouteTripStop) ? ((RouteTripStop) optPOI.poi).getTrip()
					.getHeading(context) : null;
			nextDeparturesList = schedule.getScheduleList(context, dataProvider.getNowToTheMinute(), TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(12),
					10, 20, defaultHeadSign);
		}
		ScheduleStatusViewHolder scheduleStatusViewHolder = (ScheduleStatusViewHolder) statusViewHolder;
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		scheduleStatusViewHolder.nextDeparturesLL.removeAllViews();
		scheduleStatusViewHolder.nextDeparturesLL.addView(layoutInflater.inflate(R.layout.layout_poi_detail_status_schedule_space,
				scheduleStatusViewHolder.nextDeparturesLL, false));
		SpannableStringBuilder baselineSSB = getScheduleSpace(context);
		if (nextDeparturesList != null) {
			for (Pair<CharSequence, CharSequence> nextDeparture : nextDeparturesList) {
				View view = layoutInflater.inflate(R.layout.layout_poi_detail_status_schedule_departure, scheduleStatusViewHolder.nextDeparturesLL, false);
				((TextView) view.findViewById(R.id.next_departure_time_baseline)).setText(baselineSSB, TextView.BufferType.SPANNABLE);
				((TextView) view.findViewById(R.id.next_departure_time)).setText(nextDeparture.first);
				TextView headSignTv = view.findViewById(R.id.next_departures_head_sign);
				if (TextUtils.isEmpty(nextDeparture.second)) {
					headSignTv.setText(null);
					headSignTv.setVisibility(View.INVISIBLE);
				} else {
					headSignTv.setText(nextDeparture.second, TextView.BufferType.SPANNABLE);
					headSignTv.setVisibility(View.VISIBLE);
				}
				scheduleStatusViewHolder.nextDeparturesLL.addView(view);
			}
		}
		scheduleStatusViewHolder.nextDeparturesLL.setVisibility(View.VISIBLE);
		setStatusView(statusViewHolder, nextDeparturesList != null && nextDeparturesList.size() > 0);
	}

	private static final RelativeSizeSpan SCHEDULE_SPACE_SIZE = SpanUtils.getNew200PercentSizeSpan();
	private static final StyleSpan SCHEDULE_SPACE_STYLE = SpanUtils.getNewBoldStyleSpan();

	private static TextAppearanceSpan scheduleSpaceTextAppearance = null;

	private static TextAppearanceSpan getScheduleSpaceTextAppearance(Context context) {
		if (scheduleSpaceTextAppearance == null) {
			scheduleSpaceTextAppearance = SpanUtils.getNewLargeTextAppearance(context);
		}
		return scheduleSpaceTextAppearance;
	}

	@Nullable
	private static SpannableStringBuilder baselineSSB;

	@NonNull
	private static SpannableStringBuilder getScheduleSpace(@NonNull Context context) {
		if (baselineSSB == null) {
			baselineSSB = SpanUtils.setAll(new SpannableStringBuilder(StringUtils.SPACE_STRING), //
					getScheduleSpaceTextAppearance(context), SCHEDULE_SPACE_STYLE, SCHEDULE_SPACE_SIZE);
		}
		return baselineSSB;
	}

	private static void setStatusView(@NonNull CommonStatusViewHolder statusViewHolder, boolean loaded) {
		if (loaded) {
			setStatusAsLoaded(statusViewHolder);
		} else {
			setStatusAsLoading(statusViewHolder);
		}
	}

	private static void setStatusAsLoading(@NonNull CommonStatusViewHolder statusViewHolder) {
		if (statusViewHolder.loadingV != null) {
			statusViewHolder.loadingV.setVisibility(View.VISIBLE);
		}
	}

	private static void setStatusAsLoaded(@NonNull CommonStatusViewHolder statusViewHolder) {
		if (statusViewHolder.loadingV != null) {
			statusViewHolder.loadingV.setVisibility(View.GONE);
		}
	}

	private static void initCommonStatusViewHolderHolder(CommonStatusViewHolder holder, View view) {
		holder.statusV = view;
	}

	private static class CommonStatusViewHolder {
		@SuppressWarnings("unused")
		View statusV;
		View loadingV;
	}

	private static class AvailabilityPercentStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv1;
		TextView textTv1SubValue1;
		TextView textTv2;
		ProgressBar progressBar;
	}

	private static class AppStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv;
	}

	private static class ScheduleStatusViewHolder extends CommonStatusViewHolder {
		LinearLayout nextDeparturesLL;
	}
}
