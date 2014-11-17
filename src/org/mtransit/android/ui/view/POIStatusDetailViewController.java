package org.mtransit.android.ui.view;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.ui.view.POIViewController.POIDataProvider;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class POIStatusDetailViewController implements MTLog.Loggable {

	private static final String TAG = POIStatusDetailViewController.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static Integer getLayoutResId(POIManager poim) {
		switch (poim.getStatusType()) {
		case POI.ITEM_STATUS_TYPE_APP:
			return R.layout.layout_poi_status_detail_app;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			return R.layout.layout_poi_status_detail_schedule;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			return R.layout.layout_poi_status_detail_availability_percent;
		default:
			MTLog.w(TAG, "getLayoutResId() > Unknow view type for poi %s!", poim);
			return null;
		}
	}

	public static void initViewHolder(POIManager poim, View view) {
		switch (poim.getStatusType()) {
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
		appStatusViewHolder.textTv = (TextView) view.findViewById(R.id.textTv);
		view.setTag(appStatusViewHolder);
	}

	private static void initAvailabilityPercentViewHolder(POIManager poim, View view) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = new AvailabilityPercentStatusViewHolder();
		initCommonStatusViewHolderHolder(availabilityPercentStatusViewHolder, view);
		availabilityPercentStatusViewHolder.textTv1 = (TextView) view.findViewById(R.id.progress_text1);
		availabilityPercentStatusViewHolder.textTv2 = (TextView) view.findViewById(R.id.progress_text2);
		availabilityPercentStatusViewHolder.progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		view.setTag(availabilityPercentStatusViewHolder);
	}

	private static void initScheduleViewHolder(POIManager poim, View view) {
		ScheduleStatusViewHolder scheduleStatusViewHolder = new ScheduleStatusViewHolder();
		initCommonStatusViewHolderHolder(scheduleStatusViewHolder, view);
		scheduleStatusViewHolder.nextDeparturesTimesTv = (TextView) view.findViewById(R.id.next_departures_times);
		view.setTag(scheduleStatusViewHolder);
	}

	public static void updateView(POIManager poim, View view, POIDataProvider dataProvider) {
		if (view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonStatusViewHolder holder = (CommonStatusViewHolder) view.getTag();
		switch (poim.getStatusType()) {
		case POI.ITEM_STATUS_TYPE_APP:
			updateAppStatusView(view.getContext(), holder, poim, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			updateScheduleView(view.getContext(), holder, poim, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			updateAvailabilityPercentView(view.getContext(), holder, poim, dataProvider);
			break;
		default:
			MTLog.w(TAG, "updateView() > Unknow view status type for poi %s!", poim);
		}
	}

	public static void updatePOIStatus(Context context, POIDataProvider dataProvider, View view, POIStatus status) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			return;
		}
		CommonStatusViewHolder holder = (CommonStatusViewHolder) view.getTag();
		updatePOIStatus(view.getContext(), dataProvider, holder, status);
	}

	public static void updatePOIStatus(Context context, POIDataProvider dataProvider, CommonStatusViewHolder statusViewHolder, POIStatus status) {
		if (dataProvider == null || !dataProvider.isShowingStatus() || status == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			}
			return;
		}
		switch (status.getType()) {
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			updateAvailabilityPercentView(context, statusViewHolder, status);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			updateScheduleView(context, statusViewHolder, status, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			updateAppStatusView(context, statusViewHolder, status);
			break;
		default:
			MTLog.w(TAG, "Unexpected status type '%s'!", status.getType());
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	public static void updatePOIStatus(View view, POIManager poim, POIDataProvider dataProvider) {
		if (view == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonStatusViewHolder holder = (CommonStatusViewHolder) view.getTag();
		updatePOIStatus(view.getContext(), holder, poim, dataProvider);
	}

	public static void updatePOIStatus(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim, POIDataProvider dataProvider) {
		if (dataProvider == null || !dataProvider.isShowingStatus() || poim == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			}
			return;
		}
		switch (poim.getStatusType()) {
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
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAppStatusView(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim, POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof AppStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAppStatusView(context, statusViewHolder, poim.getStatus(context));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAppStatusView(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status) {
		final AppStatusViewHolder appStatusViewHolder = (AppStatusViewHolder) statusViewHolder;
		if (status != null && status instanceof AppStatus) {
			AppStatus appStatus = (AppStatus) status;
			appStatusViewHolder.textTv.setText(appStatus.getStatusMsg(context));
			appStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAvailabilityPercentView(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim, POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof AvailabilityPercentStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAvailabilityPercentView(context, statusViewHolder, poim.getStatus(context));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAvailabilityPercentView(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = (AvailabilityPercentStatusViewHolder) statusViewHolder;
		if (status != null && status instanceof AvailabilityPercent) {
			AvailabilityPercent availabilityPercent = (AvailabilityPercent) status;
			if (!availabilityPercent.isStatusOK()) {
				availabilityPercentStatusViewHolder.textTv2.setVisibility(View.GONE);
				availabilityPercentStatusViewHolder.textTv1.setText(availabilityPercent.getStatusMsg(context));
				availabilityPercentStatusViewHolder.textTv1.setVisibility(View.VISIBLE);
			} else {
				availabilityPercentStatusViewHolder.textTv1.setText(availabilityPercent.getValue1Text(context));
				availabilityPercentStatusViewHolder.textTv1.setVisibility(View.VISIBLE);
				availabilityPercentStatusViewHolder.textTv2.setText(availabilityPercent.getValue2Text(context));
				availabilityPercentStatusViewHolder.textTv2.setVisibility(View.VISIBLE);
			}
			availabilityPercentStatusViewHolder.progressBar.setIndeterminate(false);
			availabilityPercentStatusViewHolder.progressBar.setMax(availabilityPercent.getTotalValue());
			availabilityPercentStatusViewHolder.progressBar.setProgress(availabilityPercent.getValue1());
			availabilityPercentStatusViewHolder.progressBar.getProgressDrawable().setColorFilter(//
					availabilityPercent.getValue1Color(), PorterDuff.Mode.SRC_IN);
			availabilityPercentStatusViewHolder.progressBar.setVisibility(View.VISIBLE);
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateScheduleView(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim, POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof ScheduleStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateScheduleView(context, statusViewHolder, poim.getStatus(context), dataProvider);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateScheduleView(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status, POIDataProvider dataProvider) {
		CharSequence line1CS = null;
		if (dataProvider != null && status != null && status instanceof Schedule) {
			Schedule schedule = (Schedule) status;
			final long nowToTheMinute = dataProvider.getNowToTheMinute();
			line1CS = schedule.getTimesListString(context, nowToTheMinute, nowToTheMinute + TimeUtils.ONE_DAY_IN_MS, 50);
		}
		ScheduleStatusViewHolder scheduleStatusViewHolder = (ScheduleStatusViewHolder) statusViewHolder;
		scheduleStatusViewHolder.nextDeparturesTimesTv.setText(line1CS);
		scheduleStatusViewHolder.nextDeparturesTimesTv.setVisibility(line1CS != null && line1CS.length() > 0 ? View.VISIBLE : View.GONE);
		statusViewHolder.statusV.setVisibility(line1CS != null && line1CS.length() > 0 ? View.VISIBLE : View.INVISIBLE);
	}

	private static void initCommonStatusViewHolderHolder(CommonStatusViewHolder holder, View view) {
		holder.statusV = view;
	}

	private static class CommonStatusViewHolder {
		View statusV;
	}

	private static class AvailabilityPercentStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv1;
		TextView textTv2;
		ProgressBar progressBar;
	}

	private static class AppStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv;
	}

	private static class ScheduleStatusViewHolder extends CommonStatusViewHolder {
		TextView nextDeparturesTimesTv;
	}
}
