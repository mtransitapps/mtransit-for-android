package org.mtransit.android.ui.view;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.JPaths;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.RTSRouteFragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class POIViewController implements MTLog.Loggable {

	private static final String TAG = POIViewController.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static int getLayoutResId(POIManager poim) {
		if (poim == null) {
			MTLog.w(TAG, "getLayoutResId() > Unknown view type for poim null!");
			return getBasicPOILayout(-1);
		}
		if (poim.poi == null) {
			MTLog.w(TAG, "getLayoutResId() > Unknown view type for poi null!");
			return getBasicPOILayout(poim.getStatusType());
		}
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleLayout(poim.getStatusType());
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return getRTSLayout(poim.getStatusType());
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOILayout(poim.getStatusType());
		default:
			MTLog.w(TAG, "getLayoutResId() > Unknown view type '%s' for poi %s!", poim.poi.getType(), poim);
			return getBasicPOILayout(poim.getStatusType());
		}
	}

	private static int getRTSLayout(int status) {
		int layoutRes = R.layout.layout_poi_rts;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			layoutRes = R.layout.layout_poi_rts_with_schedule;
			break;
		default:
			MTLog.w(TAG, "Unexpected status '%s' (rts view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	private static int getBasicPOILayout(int status) {
		int layoutRes = R.layout.layout_poi_basic;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			layoutRes = R.layout.layout_poi_basic_with_availability_percent;
			break;
		default:
			MTLog.w(TAG, "Unexpected status '%s' (basic view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	private static int getModuleLayout(int status) {
		int layoutRes = R.layout.layout_poi_module;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			layoutRes = R.layout.layout_poi_module_with_app_status;
			break;
		default:
			MTLog.w(TAG, "Unexpected status '%s' (module view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	public static void initViewHolder(POIManager poim, View view) {
		CommonViewHolder holder;
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_MODULE:
			holder = initModuleViewHolder(poim, view);
			break;
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			holder = initRTSViewHolder(poim, view);
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			holder = initBasicViewHolder(poim, view);
			break;
		default:
			MTLog.w(TAG, "initViewHolder() > Unknow view type for poi %s!", poim);
			holder = initBasicViewHolder(poim, view);
		}
		initCommonViewHolder(holder, view, poim.poi.getUUID());
		holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), view);
		holder.serviceUpdateViewHolder = initServiceUpdateViewHolder(view);
		view.setTag(holder);
	}

	private static ServiceUpdateViewHolder initServiceUpdateViewHolder(View view) {
		ServiceUpdateViewHolder holder = new ServiceUpdateViewHolder();
		holder.warningImg = (ImageView) view.findViewById(R.id.service_update_warning);
		return holder;
	}

	private static CommonViewHolder initModuleViewHolder(POIManager poim, View view) {
		return new ModuleViewHolder();
	}

	private static CommonViewHolder initBasicViewHolder(POIManager poim, View view) {
		return new BasicPOIViewHolder();
	}

	private static CommonViewHolder initRTSViewHolder(POIManager poim, View view) {
		RouteTripStopViewHolder holder = new RouteTripStopViewHolder();
		initRTSExtra(view, holder);
		return holder;
	}

	private static void initRTSExtra(View view, RouteTripStopViewHolder holder) {
		holder.rtsExtraV = view.findViewById(R.id.rts_extra);
		holder.routeFL = view.findViewById(R.id.route);
		holder.routeShortNameTv = (TextView) view.findViewById(R.id.route_short_name);
		holder.routeTypeImg = (MTJPathsView) view.findViewById(R.id.route_type_img);
		holder.tripHeadingTv = (TextView) view.findViewById(R.id.trip_heading);
		holder.tripHeadingBg = view.findViewById(R.id.trip_heading_bg);
	}

	private static CommonStatusViewHolder initPOIStatusViewHolder(int status, View view) {
		CommonStatusViewHolder statusViewHolder = null;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			statusViewHolder = initAvailabilityPercentViewHolder(view);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			statusViewHolder = initScheduleViewHolder(view);
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			statusViewHolder = initAppStatusViewHolder(view);
			break;
		default:
			MTLog.w(TAG, "Unexpected status '%s' (no view holder)!", status);
			break;
		}
		if (statusViewHolder != null) {
			initCommonStatusViewHolderHolder(statusViewHolder, view);
		}
		return statusViewHolder;
	}

	private static CommonStatusViewHolder initScheduleViewHolder(View view) {
		ScheduleStatusViewHolder scheduleStatusViewHolder = new ScheduleStatusViewHolder();
		scheduleStatusViewHolder.dataNextLine1Tv = (TextView) view.findViewById(R.id.data_next_line_1);
		scheduleStatusViewHolder.dataNextLine2Tv = (TextView) view.findViewById(R.id.data_next_line_2);
		return scheduleStatusViewHolder;
	}

	private static CommonStatusViewHolder initAppStatusViewHolder(View view) {
		AppStatusViewHolder appStatusViewHolder = new AppStatusViewHolder();
		appStatusViewHolder.textTv = (TextView) view.findViewById(R.id.textTv);
		return appStatusViewHolder;
	}

	private static CommonStatusViewHolder initAvailabilityPercentViewHolder(View view) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = new AvailabilityPercentStatusViewHolder();
		availabilityPercentStatusViewHolder.textTv = (TextView) view.findViewById(R.id.textTv);
		availabilityPercentStatusViewHolder.piePercentV = (MTPieChartPercentView) view.findViewById(R.id.pie);
		return availabilityPercentStatusViewHolder;
	}

	private static void initCommonStatusViewHolderHolder(CommonStatusViewHolder holder, View view) {
		holder.statusV = view.findViewById(R.id.status);
	}

	private static void initCommonViewHolder(CommonViewHolder holder, View view, String poiUUID) {
		holder.nameTv = (TextView) view.findViewById(R.id.name);
		holder.favImg = (ImageView) view.findViewById(R.id.fav);
		holder.distanceTv = (TextView) view.findViewById(R.id.distance);
		holder.compassV = (MTCompassView) view.findViewById(R.id.compass);
	}

	public static void updateView(Context context, View view, POIManager poim, POIDataProvider dataProvider) {
		if (view == null || poim == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updateCommonView(holder, poim, dataProvider);
		updateExtra(context, holder, poim, dataProvider);
		updatePOIStatus(context, holder.statusViewHolder, poim, dataProvider);
		updatePOIServiceUpdate(context, holder.serviceUpdateViewHolder, poim, dataProvider);
	}

	private static void updateExtra(Context context, CommonViewHolder holder, POIManager poim, POIDataProvider dataProvider) {
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			updateRTSExtra(context, poim, (RouteTripStopViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_MODULE:
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			break;
		default:
			MTLog.w(TAG, "updateView() > Unknow view type for poi %s!", poim);
		}
	}

	private static void updateRTSExtra(Context context, POIManager poim, RouteTripStopViewHolder holder, final POIDataProvider dataProvider) {
		if (poim.poi instanceof RouteTripStop) {
			RouteTripStop rts = (RouteTripStop) poim.poi;
			if (dataProvider != null && dataProvider.isShowingExtra() && rts.getRoute() == null) {
				holder.rtsExtraV.setVisibility(View.GONE);
				holder.routeFL.setVisibility(View.GONE);
				holder.tripHeadingBg.setVisibility(View.GONE);
			} else {
				final String authority = rts.getAuthority();
				final Route route = rts.getRoute();
				if (TextUtils.isEmpty(route.getShortName())) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
					if (holder.routeTypeImg.hasPaths() && poim.poi.getAuthority().equals(holder.routeTypeImg.getTag())) {
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						AgencyProperties agency = DataSourceProvider.get(context).getAgency(context, poim.poi.getAuthority());
						JPaths rtsRouteLogo = agency == null ? null : agency.getLogo(context);
						if (rtsRouteLogo != null) {
							holder.routeTypeImg.setJSON(rtsRouteLogo);
							holder.routeTypeImg.setTag(poim.poi.getAuthority());
							holder.routeTypeImg.setVisibility(View.VISIBLE);
						} else {
							holder.routeTypeImg.setVisibility(View.GONE);
						}
					}
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					SpannableStringBuilder ssb = new SpannableStringBuilder(route.getShortName());
					if (ssb.length() > 3) {
						SpanUtils.set(ssb, ssb.length() > 7 ? SpanUtils.TWENTY_FIVE_PERCENT_SIZE_SPAN : SpanUtils.FIFTY_PERCENT_SIZE_SPAN);
					}
					holder.routeShortNameTv.setText(ssb);
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				holder.routeFL.setVisibility(View.VISIBLE);
				holder.rtsExtraV.setVisibility(View.VISIBLE);
				final Long tripId;
				if (rts.getTrip() == null) {
					holder.tripHeadingBg.setVisibility(View.GONE);
					tripId = null;
				} else {
					tripId = rts.getTrip().getId();
					holder.tripHeadingTv.setText(rts.getTrip().getHeading(context).toUpperCase(Locale.getDefault()));
					holder.tripHeadingBg.setVisibility(View.VISIBLE);
				}
				holder.rtsExtraV.setBackgroundColor(poim.getColor(context));
				final Integer stopId = rts.getStop() == null ? null : rts.getStop().getId();
				holder.rtsExtraV.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (dataProvider != null) {
							MainActivity mainActivity = (MainActivity) dataProvider.getActivity();
							mainActivity.addFragmentToStack(RTSRouteFragment.newInstance(authority, route.getId(), tripId, stopId, route));
						}
					}
				});
			}
		}
	}

	public static void updatePOIStatus(Context context, View view, POIStatus status, POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIStatus(context, holder.statusViewHolder, status, dataProvider);
	}

	private static void updatePOIStatus(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status, POIDataProvider dataProvider) {
		if (dataProvider == null || !dataProvider.isShowingStatus() || status == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			}
			return;
		}
		switch (status.getType()) {
		case POI.ITEM_STATUS_TYPE_NONE:
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			updateAvailabilityPercent(context, statusViewHolder, status);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			updateRTSSchedule(context, statusViewHolder, status, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			updateAppStatus(context, statusViewHolder, status);
			break;
		default:
			MTLog.w(TAG, "Unexpected status type '%s'!", status.getType());
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	public static void updatePOIStatus(Context context, View view, POIManager poim, POIDataProvider dataProvider) {
		if (view == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIStatus(context, holder.statusViewHolder, poim, dataProvider);
	}

	private static void updatePOIStatus(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim, POIDataProvider dataProvider) {
		if (dataProvider == null || !dataProvider.isShowingStatus() || poim == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			}
			return;
		}
		switch (poim.getStatusType()) {
		case POI.ITEM_STATUS_TYPE_NONE:
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			updateAvailabilityPercent(context, statusViewHolder, poim, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			updateRTSSchedule(context, statusViewHolder, poim, dataProvider);
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			updateAppStatus(context, statusViewHolder, poim, dataProvider);
			break;
		default:
			MTLog.w(TAG, "Unexpected status type '%s'!", poim.getStatusType());
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAppStatus(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim, POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof AppStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAppStatus(context, statusViewHolder, poim.getStatus(context));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAppStatus(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status) {
		AppStatusViewHolder appStatusViewHolder = (AppStatusViewHolder) statusViewHolder;
		if (status != null && status instanceof AppStatus) {
			AppStatus appStatus = (AppStatus) status;
			appStatusViewHolder.textTv.setText(appStatus.getStatusMsg(context));
			appStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateRTSSchedule(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim, POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof ScheduleStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateRTSSchedule(context, statusViewHolder, poim.getStatus(context), dataProvider);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateRTSSchedule(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status, POIDataProvider dataProvider) {
		CharSequence line1CS = null;
		CharSequence line2CS = null;
		if (dataProvider != null && status != null && status instanceof Schedule) {
			Schedule schedule = (Schedule) status;
			ArrayList<Pair<CharSequence, CharSequence>> lines = schedule.getStatus(context, dataProvider.getNowToTheMinute(), TimeUnit.MINUTES.toMillis(30),
					null, 10, null);
			if (lines != null && lines.size() >= 1) {
				line1CS = lines.get(0).first;
				line2CS = lines.get(0).second;
			}
		}
		ScheduleStatusViewHolder scheduleStatusViewHolder = (ScheduleStatusViewHolder) statusViewHolder;
		scheduleStatusViewHolder.dataNextLine1Tv.setText(line1CS);
		scheduleStatusViewHolder.dataNextLine2Tv.setText(line2CS);
		scheduleStatusViewHolder.dataNextLine2Tv.setVisibility(line2CS != null && line2CS.length() > 0 ? View.VISIBLE : View.GONE);
		statusViewHolder.statusV.setVisibility(line1CS != null && line1CS.length() > 0 ? View.VISIBLE : View.INVISIBLE);
	}

	private static void updateAvailabilityPercent(Context context, CommonStatusViewHolder statusViewHolder, POIManager poim, POIDataProvider dataProvider) {
		if (dataProvider != null && dataProvider.isShowingStatus() && poim != null && statusViewHolder instanceof AvailabilityPercentStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAvailabilityPercent(context, statusViewHolder, poim.getStatus(context));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAvailabilityPercent(Context context, CommonStatusViewHolder statusViewHolder, POIStatus status) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = (AvailabilityPercentStatusViewHolder) statusViewHolder;
		if (status != null && status instanceof AvailabilityPercent) {
			AvailabilityPercent availabilityPercent = (AvailabilityPercent) status;
			if (!availabilityPercent.isStatusOK()) {
				availabilityPercentStatusViewHolder.textTv.setText(availabilityPercent.getStatusMsg(context));
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.VISIBLE);
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.GONE);
			} else if (availabilityPercent.isShowingLowerValue()) {
				availabilityPercentStatusViewHolder.textTv.setText(availabilityPercent.getLowerValueText(context));
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.VISIBLE);
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.GONE);
			} else {
				availabilityPercentStatusViewHolder.piePercentV.setValueColors( //
						availabilityPercent.getValue1Color(), //
						availabilityPercent.getValue1ColorBg(), //
						availabilityPercent.getValue2Color(), //
						availabilityPercent.getValue2ColorBg() //
						);
				availabilityPercentStatusViewHolder.piePercentV.setValues(availabilityPercent.getValue1(), availabilityPercent.getValue2());

				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.VISIBLE);
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.GONE);
			}
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	public static void updateServiceUpdatesView(View view, ArrayList<ServiceUpdate> serviceUpdates, POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updateServiceUpdateViewHolder(holder.serviceUpdateViewHolder, ServiceUpdate.isSeverityWarning(serviceUpdates), dataProvider);
	}

	public static void updatePOIServiceUpdate(Context context, View view, POIManager poim, POIDataProvider dataProvider) {
		if (view == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIServiceUpdate(context, holder.serviceUpdateViewHolder, poim, dataProvider);
	}

	private static void updatePOIServiceUpdate(Context context, ServiceUpdateViewHolder serviceUpdateViewHolder, POIManager poim, POIDataProvider dataProvider) {
		if (serviceUpdateViewHolder != null) {
			if (dataProvider != null && dataProvider.isShowingServiceUpdates() && poim != null) {
				poim.setServiceUpdateLoaderListener(dataProvider);
				updateServiceUpdateViewHolder(serviceUpdateViewHolder, poim.isServiceUpdateWarning(context), dataProvider);
			} else {
				serviceUpdateViewHolder.warningImg.setVisibility(View.GONE);
			}
		}
	}

	private static void updateServiceUpdateViewHolder(ServiceUpdateViewHolder serviceUpdateViewHolder, Boolean isServiceUpdateWarning,
			POIDataProvider dataProvider) {
		if (serviceUpdateViewHolder.warningImg == null) {
			return;
		}
		if (dataProvider != null && dataProvider.isShowingServiceUpdates() && isServiceUpdateWarning != null) {
			serviceUpdateViewHolder.warningImg.setVisibility(isServiceUpdateWarning ? View.VISIBLE : View.GONE);
		} else {
			serviceUpdateViewHolder.warningImg.setVisibility(View.GONE);
		}
	}

	public static void updatePOIDistanceAndCompass(View view, POIManager poim, POIDataProvider dataProvider) {
		if (view == null) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIDistanceAndCompass(holder, poim, dataProvider);
	}

	private static void updatePOIDistanceAndCompass(CommonViewHolder holder, POIManager poim, POIDataProvider dataProvider) {
		if (poim == null || poim.poi == null || holder == null) {
			return;
		}
		holder.compassV.setLatLng(poim.getLat(), poim.getLng());
		if (!TextUtils.isEmpty(poim.getDistanceString())) {
			if (!poim.getDistanceString().equals(holder.distanceTv.getText())) {
				holder.distanceTv.setText(poim.getDistanceString());
			}
			holder.distanceTv.setVisibility(View.VISIBLE);
		} else {
			holder.distanceTv.setVisibility(View.GONE);
			holder.distanceTv.setText(null);
		}
		if (holder.distanceTv.getVisibility() == View.VISIBLE) {
			if (dataProvider != null && dataProvider.hasLocation() && dataProvider.hasLastCompassInDegree()
					&& dataProvider.getLocation().getAccuracy() <= poim.getDistance()) {
				holder.compassV.generateAndSetHeading(dataProvider.getLocation(), dataProvider.getLastCompassInDegree(), dataProvider.getLocationDeclination());
			} else {
				holder.compassV.resetHeading();
			}
			holder.compassV.setVisibility(View.VISIBLE);
		} else {
			holder.compassV.resetHeading();
			holder.compassV.setVisibility(View.GONE);
		}
	}

	private static void updateCommonView(CommonViewHolder holder, POIManager poim, POIDataProvider dataProvider) {
		if (poim == null || poim.poi == null || holder == null) {
			return;
		}
		POI poi = poim.poi;
		holder.nameTv.setText(poi.getName());
		updatePOIDistanceAndCompass(holder, poim, dataProvider);
		if (dataProvider != null && dataProvider.isShowingFavorite() && dataProvider.isFavorite(poi.getUUID())) {
			holder.favImg.setVisibility(View.VISIBLE);
		} else {
			holder.favImg.setVisibility(View.GONE);
		}
		int index;
		if (dataProvider != null && dataProvider.isClosestPOI(poi.getUUID())) {
			index = 0;
		} else {
			index = -1;
		}
		switch (index) {
		case 0:
			holder.nameTv.setTypeface(Typeface.DEFAULT_BOLD);
			holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
			break;
		default:
			holder.nameTv.setTypeface(Typeface.DEFAULT);
			holder.distanceTv.setTypeface(Typeface.DEFAULT);
			break;
		}
	}

	public static interface POIDataProvider extends StatusLoader.StatusLoaderListener, ServiceUpdateLoader.ServiceUpdateLoaderListener {

		public boolean isShowingStatus();

		public Activity getActivity();

		public boolean isShowingExtra();

		public long getNowToTheMinute();

		public boolean isClosestPOI(String uuid);

		public boolean isFavorite(String uuid);

		public boolean isShowingFavorite();

		public float getLocationDeclination();

		public int getLastCompassInDegree();

		public Location getLocation();

		public boolean hasLastCompassInDegree();

		public boolean hasLocation();

		public boolean isShowingServiceUpdates();
	}

	private static class CommonViewHolder {
		TextView nameTv;
		TextView distanceTv;
		ImageView favImg;
		MTCompassView compassV;
		CommonStatusViewHolder statusViewHolder;
		ServiceUpdateViewHolder serviceUpdateViewHolder;
	}

	private static class ModuleViewHolder extends CommonViewHolder {
	}

	private static class RouteTripStopViewHolder extends CommonViewHolder {
		TextView routeShortNameTv;
		View routeFL;
		View rtsExtraV;
		MTJPathsView routeTypeImg;
		TextView tripHeadingTv;
		View tripHeadingBg;
	}

	private static class BasicPOIViewHolder extends CommonViewHolder {
	}

	private static class ServiceUpdateViewHolder {
		ImageView warningImg;
	}

	private static class CommonStatusViewHolder {
		View statusV;
	}

	private static class AvailabilityPercentStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv;
		MTPieChartPercentView piePercentV;
	}

	private static class AppStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv;
	}

	private static class ScheduleStatusViewHolder extends CommonStatusViewHolder {
		TextView dataNextLine1Tv;
		TextView dataNextLine2Tv;
	}
}
