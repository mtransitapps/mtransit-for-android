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
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.viewbinding.ViewBinding;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.UISchedule;
import org.mtransit.android.databinding.LayoutPoiDetailStatusAppBinding;
import org.mtransit.android.databinding.LayoutPoiDetailStatusAvailabilityPercentBinding;
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleBinding;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "WeakerAccess"})
public class POIStatusDetailViewController implements MTLog.Loggable {

	private static final String LOG_TAG = POIStatusDetailViewController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	public static ViewBinding getLayoutViewBinding(int poiStatusType, @NonNull ViewStub viewStub) {
		final Integer layoutResId = getLayoutResId(poiStatusType);
		if (layoutResId == null) {
			return null;
		}
		viewStub.setLayoutResource(layoutResId);
		switch (poiStatusType) {
		case POI.ITEM_STATUS_TYPE_NONE:
			return null;
		case POI.ITEM_STATUS_TYPE_APP:
			return LayoutPoiDetailStatusAppBinding.bind(viewStub.inflate());
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			return LayoutPoiDetailStatusScheduleBinding.bind(viewStub.inflate());
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			return LayoutPoiDetailStatusAvailabilityPercentBinding.bind(viewStub.inflate());
		}
		MTLog.w(LOG_TAG, "getLayoutViewBinding() > Unknown view type for status %s!", poiStatusType);
		return null;
	}

	@Nullable
	public static Integer getLayoutResId(@NonNull POIManager poim) {
		return getLayoutResId(poim.getStatusType());
	}

	@Nullable
	public static Integer getLayoutResId(int poiStatusType) {
		switch (poiStatusType) {
		case POI.ITEM_STATUS_TYPE_NONE:
			return null;
		case POI.ITEM_STATUS_TYPE_APP:
			return R.layout.layout_poi_detail_status_app;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			return R.layout.layout_poi_detail_status_schedule;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			return R.layout.layout_poi_detail_status_availability_percent;
		}
		MTLog.w(LOG_TAG, "getLayoutResId() > Unknown view type for status %s!", poiStatusType);
		return null;
	}

	private static void initViewHolder(@NonNull POIManager poim,
									   @NonNull View view,
									   @NonNull POIDataProvider dataProvider) {
		final int statusType = poim.getStatusType();
		switch (statusType) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			initAppStatusViewHolder(view);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			initScheduleViewHolder(view);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			initAvailabilityPercentViewHolder(poim, view, dataProvider);
			break;
		default:
			MTLog.w(LOG_TAG, "initViewHolder() > Unknown view status type for status %s!", statusType);
		}
	}

	private static void initAppStatusViewHolder(@NonNull View view) {
		AppStatusViewHolder appStatusViewHolder = new AppStatusViewHolder();
		initCommonStatusViewHolderHolder(appStatusViewHolder, view);
		appStatusViewHolder.textTv = view.findViewById(R.id.textTv);
		view.setTag(appStatusViewHolder);
	}

	private static void initAvailabilityPercentViewHolder(@NonNull View view,
														  @NonNull POIDataProvider dataProvider) {
		initAvailabilityPercentViewHolder(null, view, dataProvider);
	}

	private static void initAvailabilityPercentViewHolder(@Nullable POIManager poim,
														  @NonNull View view,
														  @NonNull POIDataProvider dataProvider) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = new AvailabilityPercentStatusViewHolder();
		initCommonStatusViewHolderHolder(availabilityPercentStatusViewHolder, view);
		availabilityPercentStatusViewHolder.textTv1 = view.findViewById(R.id.progress_text1);
		availabilityPercentStatusViewHolder.textTv1SubValue1 = view.findViewById(R.id.progress_text1_sub_value1);
		availabilityPercentStatusViewHolder.textTv2 = view.findViewById(R.id.progress_text2);
		availabilityPercentStatusViewHolder.progressBar = view.findViewById(R.id.progress_bar);
		if (poim != null) {
			availabilityPercentStatusViewHolder.progressBar.getProgressDrawable().setColorFilter(
					poim.getColor(dataProvider.providesDataSourcesRepository()),
					PorterDuff.Mode.SRC_IN
			);
		}
		view.setTag(availabilityPercentStatusViewHolder);
	}

	private static void initScheduleViewHolder(@NonNull View view) {
		ScheduleStatusViewHolder scheduleStatusViewHolder = new ScheduleStatusViewHolder();
		initCommonStatusViewHolderHolder(scheduleStatusViewHolder, view);
		scheduleStatusViewHolder.nextDeparturesLL = view.findViewById(R.id.next_departures_layout);
		view.setTag(scheduleStatusViewHolder);
	}

	public static void updatePOIStatus(@Nullable View view,
									   @Nullable POIStatus status,
									   @NonNull POIDataProvider dataProvider,
									   @Nullable POIManager optPOI) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			MTLog.d(LOG_TAG, "updatePOIStatus() > SKIP (no view holder)");
			return;
		}
		CommonStatusViewHolder holder = (CommonStatusViewHolder) view.getTag();
		updatePOIStatus(view.getContext(), holder, status, dataProvider, optPOI);
	}

	private static void updatePOIStatus(@NonNull Context context,
										@Nullable CommonStatusViewHolder statusViewHolder,
										@Nullable POIStatus status,
										@NonNull POIDataProvider dataProvider,
										@Nullable POIManager optPOI) {
		if (!dataProvider.isShowingStatus() || status == null || statusViewHolder == null) {
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
			MTLog.w(LOG_TAG, "Unexpected status type '%s'!", status.getType());
			setStatusView(statusViewHolder, false);
		}
	}

	public static void updateColorView(@Nullable View view,
									   int statusType,
									   @Nullable Integer color,
									   @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updateColorView() > SKIP (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			initViewHolder(statusType, view, dataProvider);
		}
		switch (statusType) {
		case POI.ITEM_STATUS_TYPE_NONE:
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
		case POI.ITEM_STATUS_TYPE_APP:
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = (AvailabilityPercentStatusViewHolder) view.getTag();
			if (availabilityPercentStatusViewHolder != null && color != null) {
				availabilityPercentStatusViewHolder.progressBar.getProgressDrawable().setColorFilter(
						color,
						PorterDuff.Mode.SRC_IN
				);
			}
			break;
		default:
			MTLog.w(LOG_TAG, "initViewHolder() > Unknown view status type for status %s!", statusType);
		}
	}

	private static void initViewHolder(int statusType,
									   @NonNull View view,
									   @NonNull POIDataProvider dataProvider) {
		switch (statusType) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			initAppStatusViewHolder(view);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			initScheduleViewHolder(view);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			initAvailabilityPercentViewHolder(view, dataProvider);
			break;
		default:
			MTLog.w(LOG_TAG, "initViewHolder() > Unknown view status type for status %s!", statusType);
		}
	}

	public static void updateView(@Nullable View view,
								  @NonNull POIManager poim,
								  @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			initViewHolder(poim, view, dataProvider);
		}
		CommonStatusViewHolder holder = (CommonStatusViewHolder) view.getTag();
		updateView(view.getContext(), holder, poim, dataProvider);
	}

	public static void updateView(@Nullable View view,
								  int poiStatusType,
								  @Nullable POIStatus poiStatus,
								  @Nullable POI poi,
								  @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			initViewHolder(poiStatusType, view, dataProvider);
		}
		CommonStatusViewHolder holder = (CommonStatusViewHolder) view.getTag();
		updateView(view.getContext(), holder, poiStatusType, poiStatus, poi, dataProvider);
	}

	public static void updateView(@NonNull Context context,
								  @Nullable CommonStatusViewHolder statusViewHolder,
								  int poiStatusType,
								  @Nullable POIStatus poiStatus,
								  @Nullable POI poi,
								  @NonNull POIDataProvider dataProvider) {
		if (!dataProvider.isShowingStatus() || statusViewHolder == null) {
			if (statusViewHolder != null) {
				setStatusView(statusViewHolder, false);
			}
			return;
		}
		switch (poiStatusType) {
		case POI.ITEM_STATUS_TYPE_NONE:
			setStatusView(statusViewHolder, false);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			updateAvailabilityPercentView(context, statusViewHolder, poiStatus, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			updateScheduleView(context, statusViewHolder, poiStatus, poi, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			updateAppStatusView(context, statusViewHolder, poiStatus, dataProvider);
			break;
		default:
			MTLog.w(LOG_TAG, "Unexpected status type '%s'!", poiStatusType);
			setStatusView(statusViewHolder, false);
		}
	}

	public static void updateView(@NonNull Context context,
								  @Nullable CommonStatusViewHolder statusViewHolder,
								  @NonNull POIManager poim,
								  @NonNull POIDataProvider dataProvider) {
		if (!dataProvider.isShowingStatus() || statusViewHolder == null) {
			if (statusViewHolder != null) {
				setStatusView(statusViewHolder, false);
			}
			return;
		}
		final int poiStatusType = poim.getStatusType();
		switch (poiStatusType) {
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
			MTLog.w(LOG_TAG, "Unexpected status type '%s'!", poiStatusType);
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAppStatusView(@NonNull Context context,
											CommonStatusViewHolder statusViewHolder,
											POIManager poim,
											POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof AppStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAppStatusView(context, statusViewHolder, poim.getStatus(context, dataProvider.providesStatusLoader()));
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAppStatusView(@NonNull Context context,
											@NonNull CommonStatusViewHolder statusViewHolder,
											@Nullable POIStatus poiStatus,
											@NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof AppStatusViewHolder) {
			updateAppStatusView(context, statusViewHolder, poiStatus);
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAppStatusView(@NonNull Context context, CommonStatusViewHolder statusViewHolder, POIStatus status) {
		final AppStatusViewHolder appStatusViewHolder = (AppStatusViewHolder) statusViewHolder;
		if (status instanceof AppStatus) {
			final AppStatus appStatus = (AppStatus) status;
			appStatusViewHolder.textTv.setText(appStatus.getStatusMsg(context), TextView.BufferType.SPANNABLE);
			appStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			setStatusView(statusViewHolder, true);
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAvailabilityPercentView(@NonNull Context context,
													  @NonNull CommonStatusViewHolder statusViewHolder,
													  @NonNull POIManager poim,
													  @NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof AvailabilityPercentStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAvailabilityPercentView(context, statusViewHolder, poim.getStatus(context, dataProvider.providesStatusLoader()));
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAvailabilityPercentView(@NonNull Context context,
													  @NonNull CommonStatusViewHolder statusViewHolder,
													  @Nullable POIStatus poiStatus,
													  @NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof AvailabilityPercentStatusViewHolder) {
			updateAvailabilityPercentView(context, statusViewHolder, poiStatus);
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateAvailabilityPercentView(@NonNull Context context,
													  @NonNull CommonStatusViewHolder statusViewHolder,
													  @Nullable POIStatus status) {
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

	private static void updateScheduleView(Context context,
										   CommonStatusViewHolder statusViewHolder,
										   POIManager poim,
										   POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof ScheduleStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateScheduleView(context, statusViewHolder, poim.getStatus(context, dataProvider.providesStatusLoader()), dataProvider, poim);
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateScheduleView(@NonNull Context context,
										   @NonNull CommonStatusViewHolder statusViewHolder,
										   @Nullable POIStatus poiStatus,
										   @Nullable POI poi,
										   @NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof ScheduleStatusViewHolder) {
			updateScheduleView(context, statusViewHolder, poiStatus, dataProvider, poi);
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static final long MIN_COVERAGE_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long MAX_COVERAGE_IN_MS = TimeUnit.HOURS.toMillis(12L);

	private static void updateScheduleView(@NonNull Context context,
										   @NonNull CommonStatusViewHolder statusViewHolder,
										   @Nullable POIStatus status,
										   @NonNull POIDataProvider dataProvider,
										   @Nullable POIManager optPOIM) {
		updateScheduleView(context, statusViewHolder, status, dataProvider, optPOIM == null ? null : optPOIM.poi);

	}

	private static void updateScheduleView(@NonNull Context context,
										   @NonNull CommonStatusViewHolder statusViewHolder,
										   @Nullable POIStatus status,
										   @NonNull POIDataProvider dataProvider,
										   @Nullable POI optPOI) {
		ArrayList<Pair<CharSequence, CharSequence>> nextDeparturesList = null;
		if (status instanceof UISchedule) {
			UISchedule schedule = (UISchedule) status;
			final String defaultHeadSign = optPOI instanceof RouteTripStop ? ((RouteTripStop) optPOI).getTrip().getHeading(context) : null;
			nextDeparturesList = schedule.getScheduleList(context,
					dataProvider.getNowToTheMinute(),
					MIN_COVERAGE_IN_MS,
					MAX_COVERAGE_IN_MS,
					10,
					20,
					defaultHeadSign);
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
					headSignTv.setOnClickListener(null);
					headSignTv.setVisibility(View.INVISIBLE);
				} else {
					headSignTv.setText(nextDeparture.second, TextView.BufferType.SPANNABLE);
					headSignTv.setVisibility(View.VISIBLE);
					headSignTv.setOnClickListener(v -> {
						if (headSignTv.isSelected()) {
							headSignTv.setSelected(false);
						}
						headSignTv.setSelected(true); // marquee forever
					});
				}
				scheduleStatusViewHolder.nextDeparturesLL.addView(view);
			}
		}
		scheduleStatusViewHolder.nextDeparturesLL.setVisibility(View.VISIBLE);
		setStatusView(statusViewHolder, nextDeparturesList != null && nextDeparturesList.size() > 0);
	}

	private static final RelativeSizeSpan SCHEDULE_SPACE_SIZE = SpanUtils.getNew200PercentSizeSpan();
	private static final StyleSpan SCHEDULE_SPACE_STYLE = SpanUtils.getNewBoldStyleSpan();

	@Nullable
	private static TextAppearanceSpan scheduleSpaceTextAppearance = null;

	@NonNull
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
			baselineSSB = SpanUtils.setAll(
					new SpannableStringBuilder(StringUtils.SPACE_STRING), //
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
		View loadingV; // unused
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
