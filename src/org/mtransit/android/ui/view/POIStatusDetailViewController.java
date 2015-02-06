package org.mtransit.android.ui.view;

import java.util.concurrent.TimeUnit;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.POIManager;

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
		if (poim != null) {
			switch (poim.getStatusType()) {
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

	public static void initViewHolder(AgencyProperties agency, POIManager poim, View view) {
		switch (poim.getStatusType()) {
		case POI.ITEM_STATUS_TYPE_APP:
			initAppStatusViewHolder(poim, view);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			initScheduleViewHolder(poim, view);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			initAvailabilityPercentViewHolder(agency, poim, view);
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

	private static void initAvailabilityPercentViewHolder(AgencyProperties agency, POIManager poim, View view) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = new AvailabilityPercentStatusViewHolder();
		initCommonStatusViewHolderHolder(availabilityPercentStatusViewHolder, view);
		availabilityPercentStatusViewHolder.textTv1 = (TextView) view.findViewById(R.id.progress_text1);
		availabilityPercentStatusViewHolder.textTv2 = (TextView) view.findViewById(R.id.progress_text2);
		availabilityPercentStatusViewHolder.progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		if (poim != null) {
			availabilityPercentStatusViewHolder.progressBar.getProgressDrawable().setColorFilter(poim.getColor(view.getContext()), PorterDuff.Mode.SRC_IN);
		}
		view.setTag(availabilityPercentStatusViewHolder);
	}

	private static void initScheduleViewHolder(POIManager poim, View view) {
		ScheduleStatusViewHolder scheduleStatusViewHolder = new ScheduleStatusViewHolder();
		initCommonStatusViewHolderHolder(scheduleStatusViewHolder, view);
		scheduleStatusViewHolder.nextDeparturesTimesTv = (TextView) view.findViewById(R.id.next_departures_times);
		view.setTag(scheduleStatusViewHolder);
	}


	public static void updatePOIStatus(Context context, View view, POIStatus status, POIViewController.POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			return;
		}
		CommonStatusViewHolder holder = (CommonStatusViewHolder) view.getTag();
		updatePOIStatus(context, holder, status, dataProvider);
	}

	private static void updatePOIStatus(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status,
			POIViewController.POIDataProvider dataProvider) {
		if (dataProvider == null || !dataProvider.isShowingStatus() || status == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				setStatusView(statusViewHolder, false);
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
			setStatusView(statusViewHolder, false);
		}
	}

	public static void updateView(Context context, View view, AgencyProperties agency, POIManager poim, POIViewController.POIDataProvider dataProvider) {
		if (view == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonStatusViewHolder)) {
			initViewHolder(agency, poim, view);
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
		if (status != null && status instanceof AppStatus) {
			AppStatus appStatus = (AppStatus) status;
			appStatusViewHolder.textTv.setText(appStatus.getStatusMsg(context));
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
			updateScheduleView(context, statusViewHolder, poim.getStatus(context), dataProvider);
		} else {
			setStatusView(statusViewHolder, false);
		}
	}

	private static void updateScheduleView(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status,
			POIViewController.POIDataProvider dataProvider) {
		CharSequence line1CS = null;
		if (dataProvider != null && status != null && status instanceof Schedule) {
			Schedule schedule = (Schedule) status;
			long nowToTheMinute = dataProvider.getNowToTheMinute();
			line1CS = schedule.getSchedule(context, nowToTheMinute, TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(12), 10, 50);
		}
		ScheduleStatusViewHolder scheduleStatusViewHolder = (ScheduleStatusViewHolder) statusViewHolder;
		scheduleStatusViewHolder.nextDeparturesTimesTv.setText(line1CS);
		setStatusView(statusViewHolder, line1CS != null && line1CS.length() > 0);
	}

	private static void setStatusView(CommonStatusViewHolder statusViewHolder, boolean loaded) {
		if (loaded) {
			setStatusAsLoaded(statusViewHolder);
		} else {
			setStatusAsLoading(statusViewHolder);
		}
	}

	private static void setStatusAsLoading(CommonStatusViewHolder statusViewHolder) {
		if (statusViewHolder.loadingV != null) {
			statusViewHolder.loadingV.setVisibility(View.VISIBLE);
		}
	}

	private static void setStatusAsLoaded(CommonStatusViewHolder statusViewHolder) {
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
