package org.mtransit.android.ui.view;

import java.util.List;
import java.util.Locale;

import org.mtransit.android.R;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.JPaths;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.RTSRouteFragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
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
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleLayout(poim.getStatusType());
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return getRTSLayout(poim.getStatusType());
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOILayout(poim.getStatusType());
		default:
			MTLog.w(TAG, "getLayoutResId() > Unknow view type for poi %s!", poim);
			return getBasicPOILayout(poim.getStatusType());
		}
	}

	private static int getRTSLayout(int status) {
		int layoutRes = R.layout.layout_poi_rts;
		switch (status) {
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
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_MODULE:
			initModuleViewHolder(poim, view);
			break;
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			initRTSViewHolder(poim, view);
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			initBasicViewHolder(poim, view);
			break;
		default:
			MTLog.w(TAG, "initViewHolder() > Unknow view type for poi %s!", poim);
			initBasicViewHolder(poim, view);
		}
	}
	
	private static void initModuleViewHolder(POIManager poim, View view) {
		ModuleViewHolder holder = new ModuleViewHolder();
		initCommonViewHolder(holder, view, poim.poi.getUUID());
		holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), view);
		view.setTag(holder);
	}

	private static void initBasicViewHolder(POIManager poim, View view) {
		BasicPOIViewHolder holder = new BasicPOIViewHolder();
		initCommonViewHolder(holder, view, poim.poi.getUUID());
		holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), view);
		view.setTag(holder);
	}

	private static void initRTSViewHolder(POIManager poim, View view) {
		RouteTripStopViewHolder holder = new RouteTripStopViewHolder();
		initCommonViewHolder(holder, view, poim.poi.getUUID());
		initRTSExtra(view, holder);
		holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), view);
		view.setTag(holder);
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
		switch (status) {
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			return initAvailabilityPercentViewHolder(view);
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			return initScheduleViewHolder(view);
		case POI.ITEM_STATUS_TYPE_APP:
			return initAppStatusViewHolder(view);
		default:
			MTLog.w(TAG, "Unexpected status '%s' (no view holder)!", status);
			return null;
		}
	}

	private static CommonStatusViewHolder initScheduleViewHolder(View view) {
		ScheduleStatusViewHolder scheduleStatusViewHolder = new ScheduleStatusViewHolder();
		initCommonStatusViewHolderHolder(scheduleStatusViewHolder, view);
		scheduleStatusViewHolder.dataNextLine1Tv = (TextView) view.findViewById(R.id.data_next_line_1);
		scheduleStatusViewHolder.dataNextLine2Tv = (TextView) view.findViewById(R.id.data_next_line_2);
		return scheduleStatusViewHolder;
	}

	private static CommonStatusViewHolder initAppStatusViewHolder(View view) {
		AppStatusViewHolder appStatusViewHolder = new AppStatusViewHolder();
		initCommonStatusViewHolderHolder(appStatusViewHolder, view);
		appStatusViewHolder.textTv = (TextView) view.findViewById(R.id.textTv);
		return appStatusViewHolder;
	}

	private static CommonStatusViewHolder initAvailabilityPercentViewHolder(View view) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = new AvailabilityPercentStatusViewHolder();
		initCommonStatusViewHolderHolder(availabilityPercentStatusViewHolder, view);
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

	public static void updateView(POIManager poim, View view, POIDataProvider dataProvider) {
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_MODULE:
			updateModuleView(dataProvider, poim, view);
			break;
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			updateRTSView(dataProvider, poim, view);
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			updateBasicPOIView(dataProvider, poim, view);
			break;
		default:
			MTLog.w(TAG, "updateView() > Unknow view type for poi %s!", poim);
			updateBasicPOIView(dataProvider, poim, view);
		}
	}

	private static View updateRTSView(POIDataProvider dataProvider, POIManager poim, View view) {
		if (view == null || poim == null) {
			return view;
		}
		RouteTripStopViewHolder holder = (RouteTripStopViewHolder) view.getTag();
		updateCommonView(view.getContext(), dataProvider, holder, poim);
		updateRTSExtra(view.getContext(), poim, holder, dataProvider);
		updatePOIStatus(view.getContext(), holder.statusViewHolder, poim, dataProvider);
		return view;
	}

	private static void updateRTSExtra(Context context, POIManager poim, RouteTripStopViewHolder holder, final POIDataProvider dataProvider) {
		if (poim.poi instanceof RouteTripStop) {
			final RouteTripStop rts = (RouteTripStop) poim.poi;
			if (dataProvider != null && dataProvider.isShowingExtra() && rts.route == null) {
				holder.rtsExtraV.setVisibility(View.GONE);
				holder.routeFL.setVisibility(View.GONE);
				holder.tripHeadingBg.setVisibility(View.GONE);
			} else {
				final int routeTextColor = ColorUtils.parseColor(rts.route.textColor);
				final int routeColor = ColorUtils.parseColor(rts.route.color);
				if (TextUtils.isEmpty(rts.route.shortName)) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
					final JPaths rtsRouteLogo = DataSourceProvider.get().getRTSRouteLogo(context, poim.poi.getAuthority());
					if (rtsRouteLogo != null) {
						holder.routeTypeImg.setJSON(rtsRouteLogo);
						holder.routeTypeImg.setColor(routeTextColor);
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						holder.routeTypeImg.setVisibility(View.GONE);
					}
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					holder.routeShortNameTv.setText(rts.route.shortName);
					holder.routeShortNameTv.setTextColor(routeTextColor);
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				holder.rtsExtraV.setBackgroundColor(routeColor);
				holder.rtsExtraV.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						((MainActivity) dataProvider.getActivity()).addFragmentToStack(RTSRouteFragment.newInstance(rts));
					}
				});
				holder.routeFL.setVisibility(View.VISIBLE);
				holder.rtsExtraV.setVisibility(View.VISIBLE);
				if (rts.trip == null) {
					holder.tripHeadingBg.setVisibility(View.GONE);
				} else {
					holder.tripHeadingTv.setTextColor(routeColor);
					holder.tripHeadingBg.setBackgroundColor(routeTextColor);
					holder.tripHeadingTv.setText(rts.trip.getHeading(context).toUpperCase(Locale.getDefault()));
					holder.tripHeadingBg.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private static View updateModuleView(POIDataProvider dataProvider, POIManager poim, View view) {
		if (view == null || poim == null) {
			return view;
		}
		ModuleViewHolder holder = (ModuleViewHolder) view.getTag();
		updateCommonView(view.getContext(), dataProvider, holder, poim);
		updatePOIStatus(view.getContext(), holder.statusViewHolder, poim, dataProvider);
		return view;
	}

	private static View updateBasicPOIView(POIDataProvider dataProvider, POIManager poim, View view) {
		if (view == null || poim == null) {
			return view;
		}
		BasicPOIViewHolder holder = (BasicPOIViewHolder) view.getTag();
		updateCommonView(view.getContext(), dataProvider, holder, poim);
		updatePOIStatus(view.getContext(), holder.statusViewHolder, poim, dataProvider);
		return view;
	}

	public static void updatePOIStatus(Context context, POIDataProvider dataProvider, View view, POIStatus status) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIStatus(view.getContext(), dataProvider, holder.statusViewHolder, status);
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

	public static void updatePOIStatus(View view, POIManager poim, POIDataProvider dataProvider) {
		if (view == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIStatus(view.getContext(), holder.statusViewHolder, poim, dataProvider);
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
			final int count = 20; // needs enough to check if service is frequent (every 5 minutes or less for at least 30 minutes)
			List<Pair<CharSequence, CharSequence>> lines = schedule.getNextTimesStrings(context, dataProvider.getNowToTheMinute(), null, count);
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

	public static void updatePOIDistanceAndCompass(POIManager poim, View view, POIDataProvider dataProvider) {
		if (poim == null || poim.poi == null || view == null || view.getTag() == null) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
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
		if (holder.distanceTv.getVisibility() == View.VISIBLE && dataProvider != null && dataProvider.hasLocation() && dataProvider.hasLastCompassInDegree()
				&& dataProvider.getLocation().getAccuracy() <= poim.getDistance()) {
			holder.compassV.generateAndSetHeading(dataProvider.getLocation(), dataProvider.getLastCompassInDegree(), dataProvider.getLocationDeclination());
			holder.compassV.setVisibility(View.VISIBLE);
		} else {
			holder.compassV.setVisibility(View.GONE);
		}
	}

	private static void updateCommonView(Context context, POIDataProvider dataProvider, CommonViewHolder holder, POIManager poim) {
		if (poim == null || poim.poi == null || holder == null) {
			return;
		}
		final POI poi = poim.poi;
		holder.compassV.setLatLng(poim.getLat(), poim.getLng());
		holder.nameTv.setText(poi.getName());
		if (!TextUtils.isEmpty(poim.getDistanceString())) {
			if (!poim.getDistanceString().equals(holder.distanceTv.getText())) {
				holder.distanceTv.setText(poim.getDistanceString());
			}
			holder.distanceTv.setVisibility(View.VISIBLE);
		} else {
			holder.distanceTv.setVisibility(View.GONE);
			holder.distanceTv.setText(null);
		}
		if (holder.distanceTv.getVisibility() == View.VISIBLE && dataProvider != null && dataProvider.hasLocation() && dataProvider.hasLastCompassInDegree()
				&& dataProvider.getLocation().getAccuracy() <= poim.getDistance()) {
			holder.compassV.generateAndSetHeading(dataProvider.getLocation(), dataProvider.getLastCompassInDegree(), dataProvider.getLocationDeclination());
			holder.compassV.setVisibility(View.VISIBLE);
		} else {
			holder.compassV.setVisibility(View.GONE);
		}
		if (dataProvider != null && dataProvider.isShowingFavorite() && dataProvider.isFavorite(poi.getUUID())) {
			holder.favImg.setVisibility(View.VISIBLE);
		} else {
			holder.favImg.setVisibility(View.GONE);
		}
		final int index;
		if (dataProvider != null && dataProvider.isClosestPOI(poi.getUUID())) {
			index = 0;
		} else {
			index = -1;
		}
		switch (index) {
		case 0:
			holder.nameTv.setTypeface(Typeface.DEFAULT_BOLD);
			holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
			final int textColorPrimary = ColorUtils.getTextColorPrimary(context);
			holder.distanceTv.setTextColor(textColorPrimary);
			holder.compassV.setColor(textColorPrimary);
			break;
		default:
			holder.nameTv.setTypeface(Typeface.DEFAULT);
			holder.distanceTv.setTypeface(Typeface.DEFAULT);
			final int defaultDistanceAndCompassColor = POIManager.getDefaultDistanceAndCompassColor(context);
			holder.distanceTv.setTextColor(defaultDistanceAndCompassColor);
			holder.compassV.setColor(defaultDistanceAndCompassColor);
			break;
		}
	}

	public static interface POIDataProvider extends StatusLoader.StatusLoaderListener {

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
	}

	private static class CommonViewHolder {
		TextView nameTv;
		TextView distanceTv;
		ImageView favImg;
		MTCompassView compassV;
		CommonStatusViewHolder statusViewHolder;
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
