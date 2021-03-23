package org.mtransit.android.ui.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.location.Location;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.JPaths;
import org.mtransit.android.data.Module;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.UISchedule;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.RTSRouteFragment;
import org.mtransit.android.util.LinkUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class POIViewController implements MTLog.Loggable {

	private static final String LOG_TAG = POIViewController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@LayoutRes
	public static int getLayoutResId(@NonNull POIManager poim) {
		//noinspection ConstantConditions // poi always non-null?
		if (poim.poi == null) {
			MTLog.w(LOG_TAG, "getLayoutResId() > Unknown view type for poi null!");
			return getBasicPOILayout(poim.getStatusType());
		}
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return R.layout.layout_poi_basic;
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleLayout(poim.getStatusType());
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return getRTSLayout(poim.getStatusType());
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOILayout(poim.getStatusType());
		default:
			MTLog.w(LOG_TAG, "getLayoutResId() > Unknown view type '%s' for poi %s!", poim.poi.getType(), poim);
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
			MTLog.w(LOG_TAG, "Unexpected status '%s' (rts view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	@LayoutRes
	private static int getBasicPOILayout(int status) {
		int layoutRes = R.layout.layout_poi_basic;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			layoutRes = R.layout.layout_poi_basic_with_availability_percent;
			break;
		default:
			MTLog.w(LOG_TAG, "Unexpected status '%s' (basic view w/o status)!", status);
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
			MTLog.w(LOG_TAG, "Unexpected status '%s' (module view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	public static void initViewHolder(@NonNull POIManager poim, @NonNull View view) {
		CommonViewHolder holder;
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			holder = initTextMessageViewHolder(poim, view);
			break;
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
			MTLog.w(LOG_TAG, "initViewHolder() > Unknow view type for poi %s!", poim);
			holder = initBasicViewHolder(poim, view);
		}
		initCommonViewHolder(holder, view, poim.poi.getUUID());
		holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), view);
		holder.serviceUpdateViewHolder = initServiceUpdateViewHolder(view);
		view.setTag(holder);
	}

	private static ServiceUpdateViewHolder initServiceUpdateViewHolder(@NonNull View view) {
		ServiceUpdateViewHolder holder = new ServiceUpdateViewHolder();
		holder.warningImg = view.findViewById(R.id.service_update_warning);
		return holder;
	}

	private static CommonViewHolder initModuleViewHolder(@NonNull POIManager poim, @NonNull View view) {
		ModuleViewHolder holder = new ModuleViewHolder();
		holder.moduleExtraTypeImg = view.findViewById(R.id.extra);
		return holder;
	}

	private static CommonViewHolder initTextMessageViewHolder(@NonNull POIManager poim, @NonNull View view) {
		return new TextMessageViewHolder();
	}

	private static CommonViewHolder initBasicViewHolder(@NonNull POIManager poim, @NonNull View view) {
		return new BasicPOIViewHolder();
	}

	private static CommonViewHolder initRTSViewHolder(@NonNull POIManager poim, @NonNull View view) {
		RouteTripStopViewHolder holder = new RouteTripStopViewHolder();
		initRTSExtra(view, holder);
		return holder;
	}

	private static void initRTSExtra(@NonNull View view, @NonNull RouteTripStopViewHolder holder) {
		holder.rtsExtraV = view.findViewById(R.id.extra);
		holder.routeFL = view.findViewById(R.id.route);
		holder.routeShortNameTv = view.findViewById(R.id.route_short_name);
		holder.routeTypeImg = view.findViewById(R.id.route_type_img);
		holder.tripHeadingTv = view.findViewById(R.id.trip_heading);
		holder.tripHeadingBg = view.findViewById(R.id.trip_heading_bg);
	}

	@Nullable
	private static CommonStatusViewHolder initPOIStatusViewHolder(int status, @NonNull View view) {
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
			MTLog.w(LOG_TAG, "Unexpected status '%s' (no view holder)!", status);
			break;
		}
		if (statusViewHolder != null) {
			initCommonStatusViewHolderHolder(statusViewHolder, view);
		}
		return statusViewHolder;
	}

	private static CommonStatusViewHolder initScheduleViewHolder(@NonNull View view) {
		ScheduleStatusViewHolder scheduleStatusViewHolder = new ScheduleStatusViewHolder();
		scheduleStatusViewHolder.dataNextLine1Tv = view.findViewById(R.id.data_next_line_1);
		scheduleStatusViewHolder.dataNextLine2Tv = view.findViewById(R.id.data_next_line_2);
		return scheduleStatusViewHolder;
	}

	@NonNull
	private static CommonStatusViewHolder initAppStatusViewHolder(@NonNull View view) {
		AppStatusViewHolder appStatusViewHolder = new AppStatusViewHolder();
		appStatusViewHolder.textTv = view.findViewById(R.id.textTv);
		return appStatusViewHolder;
	}

	@NonNull
	private static CommonStatusViewHolder initAvailabilityPercentViewHolder(@NonNull View view) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = new AvailabilityPercentStatusViewHolder();
		availabilityPercentStatusViewHolder.textTv = view.findViewById(R.id.textTv);
		availabilityPercentStatusViewHolder.piePercentV = view.findViewById(R.id.pie);
		return availabilityPercentStatusViewHolder;
	}

	private static void initCommonStatusViewHolderHolder(@NonNull CommonStatusViewHolder holder, @NonNull View view) {
		holder.statusV = view.findViewById(R.id.status);
	}

	private static void initCommonViewHolder(@NonNull CommonViewHolder holder, @NonNull View view, String poiUUID) {
		holder.nameTv = view.findViewById(R.id.name);
		holder.favImg = view.findViewById(R.id.fav);
		holder.distanceTv = view.findViewById(R.id.distance);
		holder.compassV = view.findViewById(R.id.compass);
	}

	public static void updateView(@NonNull Context context, @Nullable View view, @Nullable POIManager poim, @NonNull POIDataProvider dataProvider) {
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

	private static void updateExtra(Context context, @NonNull CommonViewHolder holder, @NonNull POIManager poim, @NonNull POIDataProvider dataProvider) {
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			updateRTSExtra(context, poim, (RouteTripStopViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_MODULE:
			updateModuleExtra(context, poim, (ModuleViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			break;
		default:
			MTLog.w(LOG_TAG, "updateView() > Unknown view type for poi %s!", poim);
		}
	}

	private static void updateModuleExtra(Context context, @NonNull POIManager poim, @NonNull ModuleViewHolder holder, @NonNull POIDataProvider dataProvider) {
		if (poim.poi instanceof Module) {
			Module module = (Module) poim.poi;
			holder.moduleExtraTypeImg.setBackgroundColor(poim.getColor(context));
			DataSourceType moduleType = DataSourceType.parseId(module.getTargetTypeId());
			if (moduleType != null) {
				holder.moduleExtraTypeImg.setImageResource(moduleType.getIconResId());
			} else {
				holder.moduleExtraTypeImg.setImageResource(0);
			}
			holder.moduleExtraTypeImg.setVisibility(View.VISIBLE);
		} else {
			holder.moduleExtraTypeImg.setVisibility(View.GONE);
		}
	}

	private static void updateRTSExtra(Context context, @NonNull POIManager poim, @NonNull RouteTripStopViewHolder holder,
									   @NonNull final POIDataProvider dataProvider) {
		if (poim.poi instanceof RouteTripStop) {
			RouteTripStop rts = (RouteTripStop) poim.poi;
			//noinspection ConstantConditions // route always non-null?
			if (dataProvider.isShowingExtra() && rts.getRoute() == null) {
				if (holder.rtsExtraV != null) {
					holder.rtsExtraV.setVisibility(View.GONE);
				}
				if (holder.routeFL != null) {
					holder.routeFL.setVisibility(View.GONE);
				}
				if (holder.tripHeadingBg != null) {
					holder.tripHeadingBg.setVisibility(View.GONE);
				}
			} else {
				final String authority = rts.getAuthority();
				final Route route = rts.getRoute();
				if (TextUtils.isEmpty(route.getShortName())) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
					if (holder.routeTypeImg.hasPaths() && poim.poi.getAuthority().equals(holder.routeTypeImg.getTag())) {
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						AgencyProperties agency = DataSourceProvider.get(context).getAgency(context, poim.poi.getAuthority());
						JPaths rtsRouteLogo = agency == null ? null : agency.getLogo();
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
					holder.routeShortNameTv.setText(route.getShortName());
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				holder.routeFL.setVisibility(View.VISIBLE);
				holder.rtsExtraV.setVisibility(View.VISIBLE);
				final Long tripId;
				//noinspection ConstantConditions // trip always non-null?
				if (rts.getTrip() == null) {
					holder.tripHeadingBg.setVisibility(View.GONE);
					tripId = null;
				} else {
					tripId = rts.getTrip().getId();
					holder.tripHeadingTv.setText(rts.getTrip().getHeading(context).toUpperCase(Locale.getDefault()));
					holder.tripHeadingTv.setSingleLine(true); // marquee forever
					holder.tripHeadingTv.setSelected(true); // marquee forever
					holder.tripHeadingBg.setVisibility(View.VISIBLE);
				}
				holder.rtsExtraV.setBackgroundColor(poim.getColor(context));
				//noinspection ConstantConditions // stop always non-null?
				final Integer stopId = rts.getStop() == null ? null : rts.getStop().getId();
				holder.rtsExtraV.setOnClickListener(new MTOnClickListener() {
					@Override
					public void onClickMT(@NonNull View view) {
						MainActivity mainActivity = (MainActivity) dataProvider.getActivity();
						if (mainActivity == null) {
							return;
						}
						mainActivity.addFragmentToStack(RTSRouteFragment.newInstance(authority, route.getId(), tripId, stopId, route));
					}
				});
			}
		}
	}

	public static void updatePOIStatus(Context context, @Nullable View view, @NonNull POIStatus status, @NonNull POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIStatus(context, holder.statusViewHolder, status, dataProvider);
	}

	private static void updatePOIStatus(Context context, @Nullable CommonStatusViewHolder statusViewHolder, @NonNull POIStatus status,
										@NonNull POIDataProvider dataProvider) {
		if (statusViewHolder == null) {
			return;
		}
		if (!dataProvider.isShowingStatus()) {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
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
			MTLog.w(LOG_TAG, "Unexpected status type '%s'!", status.getType());
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	public static void updatePOIStatus(@NonNull Context context, @Nullable View view, @NonNull POIManager poim, @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updatePOIStatus() > SKIP (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIStatus(context, holder.statusViewHolder, poim, dataProvider);
	}

	private static void updatePOIStatus(@NonNull Context context, @Nullable CommonStatusViewHolder statusViewHolder, @NonNull POIManager poim,
										@NonNull POIDataProvider dataProvider) {
		if (statusViewHolder == null) {
			return;
		}
		if (!dataProvider.isShowingStatus()) {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
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
			MTLog.w(LOG_TAG, "Unexpected status type '%s'!", poim.getStatusType());
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAppStatus(Context context, @NonNull CommonStatusViewHolder statusViewHolder, @NonNull POIManager poim,
										@NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof AppStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAppStatus(context, statusViewHolder, poim.getStatus(context));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAppStatus(@NonNull Context context, @NonNull CommonStatusViewHolder statusViewHolder, @Nullable POIStatus status) {
		AppStatusViewHolder appStatusViewHolder = (AppStatusViewHolder) statusViewHolder;
		if (status instanceof AppStatus) {
			AppStatus appStatus = (AppStatus) status;
			appStatusViewHolder.textTv.setText(appStatus.getStatusMsg(context), TextView.BufferType.SPANNABLE);
			appStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateRTSSchedule(Context context, @NonNull CommonStatusViewHolder statusViewHolder, @NonNull POIManager poim,
										  @NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof ScheduleStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateRTSSchedule(context, statusViewHolder, poim.getStatus(context), dataProvider);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateRTSSchedule(Context context, @NonNull CommonStatusViewHolder statusViewHolder, @Nullable POIStatus status,
										  @NonNull POIDataProvider dataProvider) {
		CharSequence line1CS = null;
		CharSequence line2CS = null;
		if (status instanceof UISchedule) {
			UISchedule schedule = (UISchedule) status;
			ArrayList<Pair<CharSequence, CharSequence>> lines = schedule.getStatus( //
					context, dataProvider.getNowToTheMinute(), TimeUnit.MINUTES.toMillis(30L), null, 10, null);
			if (lines != null && lines.size() >= 1) {
				line1CS = lines.get(0).first;
				line2CS = lines.get(0).second;
			}
		}
		ScheduleStatusViewHolder scheduleStatusViewHolder = (ScheduleStatusViewHolder) statusViewHolder;
		scheduleStatusViewHolder.dataNextLine1Tv.setText(line1CS, TextView.BufferType.SPANNABLE);
		scheduleStatusViewHolder.dataNextLine2Tv.setText(line2CS, TextView.BufferType.SPANNABLE);
		scheduleStatusViewHolder.dataNextLine2Tv.setVisibility(line2CS != null && line2CS.length() > 0 ? View.VISIBLE : View.GONE);
		statusViewHolder.statusV.setVisibility(line1CS != null && line1CS.length() > 0 ? View.VISIBLE : View.INVISIBLE);
	}

	private static void updateAvailabilityPercent(@NonNull Context context, @NonNull CommonStatusViewHolder statusViewHolder, @NonNull POIManager poim,
												  @NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof AvailabilityPercentStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAvailabilityPercent(context, statusViewHolder, poim.getStatus(context));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAvailabilityPercent(@NonNull Context context, @NonNull CommonStatusViewHolder statusViewHolder, @Nullable POIStatus status) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = (AvailabilityPercentStatusViewHolder) statusViewHolder;
		if (status instanceof AvailabilityPercent) {
			AvailabilityPercent availabilityPercent = (AvailabilityPercent) status;
			if (!availabilityPercent.isStatusOK()) {
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.GONE);
				availabilityPercentStatusViewHolder.textTv.setText(availabilityPercent.getStatusMsg(context), TextView.BufferType.SPANNABLE);
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			} else if (availabilityPercent.isShowingLowerValue()) {
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.GONE);
				availabilityPercentStatusViewHolder.textTv.setText(availabilityPercent.getLowerValueText(context), TextView.BufferType.SPANNABLE);
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			} else {
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.GONE);
				availabilityPercentStatusViewHolder.piePercentV.setPiecesColors( //
						Arrays.asList(
								new Pair<>(
										availabilityPercent.getValue1SubValueDefaultColor(), //
										availabilityPercent.getValue1SubValueDefaultColorBg()), //
								new Pair<>(
										availabilityPercent.getValue1SubValue1Color(), //
										availabilityPercent.getValue1SubValue1ColorBg()), //
								new Pair<>(
										availabilityPercent.getValue2Color(), //
										availabilityPercent.getValue2ColorBg()) //
						)
				);
				availabilityPercentStatusViewHolder.piePercentV.setPieces(
						Arrays.asList(
								availabilityPercent.getValue1SubValueDefault(),
								availabilityPercent.getValue1SubValue1(),
								availabilityPercent.getValue2()
						)
				);
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.VISIBLE);
			}
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	public static void updateServiceUpdatesView(@Nullable View view, ArrayList<ServiceUpdate> serviceUpdates, @NonNull POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updateServiceUpdateViewHolder(holder.serviceUpdateViewHolder, ServiceUpdate.isSeverityWarning(serviceUpdates), dataProvider);
	}

	public static void updatePOIServiceUpdate(Context context, @Nullable View view, @NonNull POIManager poim, @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIServiceUpdate(context, holder.serviceUpdateViewHolder, poim, dataProvider);
	}

	private static void updatePOIServiceUpdate(Context context, @Nullable ServiceUpdateViewHolder serviceUpdateViewHolder, @NonNull POIManager poim,
											   @NonNull POIDataProvider dataProvider) {
		if (serviceUpdateViewHolder != null) {
			if (dataProvider.isShowingServiceUpdates()) {
				poim.setServiceUpdateLoaderListener(dataProvider);
				updateServiceUpdateViewHolder(serviceUpdateViewHolder, poim.isServiceUpdateWarning(context), dataProvider);
			} else {
				serviceUpdateViewHolder.warningImg.setVisibility(View.GONE);
			}
		}
	}

	private static void updateServiceUpdateViewHolder(@NonNull ServiceUpdateViewHolder serviceUpdateViewHolder, Boolean isServiceUpdateWarning,
													  @NonNull POIDataProvider dataProvider) {
		if (serviceUpdateViewHolder.warningImg == null) {
			return;
		}
		if (dataProvider.isShowingServiceUpdates() && isServiceUpdateWarning != null) {
			serviceUpdateViewHolder.warningImg.setVisibility(isServiceUpdateWarning ? View.VISIBLE : View.GONE);
		} else {
			serviceUpdateViewHolder.warningImg.setVisibility(View.GONE);
		}
	}

	public static void updatePOIDistanceAndCompass(@Nullable View view, POIManager poim, @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updatePOIDistanceAndCompass() > skip (no view)");
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIDistanceAndCompass(holder, poim, dataProvider);
	}

	private static void updatePOIDistanceAndCompass(CommonViewHolder holder, POIManager poim, @NonNull POIDataProvider dataProvider) {
		//noinspection ConstantConditions // poi always non-null?
		if (poim == null || poim.poi == null || holder == null) {
			MTLog.d(LOG_TAG, "updatePOIDistanceAndCompass() > skip (no poi or view holder)");
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
			if (dataProvider.hasLocation() //
					&& dataProvider.hasLastCompassInDegree() //
					&& dataProvider.getLocation().getAccuracy() <= poim.getDistance()) {
				holder.compassV.generateAndSetHeading( //
						dataProvider.getLocation(), dataProvider.getLastCompassInDegree(), dataProvider.getLocationDeclination());
			} else {
				holder.compassV.resetHeading();
			}
			holder.compassV.setVisibility(View.VISIBLE);
		} else {
			holder.compassV.resetHeading();
			holder.compassV.setVisibility(View.GONE);
		}
	}

	private static void updateCommonView(@NonNull CommonViewHolder holder, @NonNull POIManager poim, @NonNull POIDataProvider dataProvider) {
		//noinspection ConstantConditions // poi always non-null?
		if (poim.poi == null) {
			return;
		}
		POI poi = poim.poi;
		holder.nameTv.setText(poi.getLabel());
		holder.nameTv.setSingleLine(true); // marquee forever
		holder.nameTv.setSelected(true); // marquee forever
		updatePOIDistanceAndCompass(holder, poim, dataProvider);
		if (dataProvider.isShowingFavorite() && dataProvider.isFavorite(poi.getUUID())) {
			holder.favImg.setVisibility(View.VISIBLE);
		} else {
			holder.favImg.setVisibility(View.GONE);
		}
		int index;
		if (dataProvider.isClosestPOI(poi.getUUID())) {
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

	public interface POIDataProvider extends StatusLoader.StatusLoaderListener, ServiceUpdateLoader.ServiceUpdateLoaderListener, LinkUtils.OnUrlClickListener {

		boolean isShowingStatus();

		Activity getActivity();

		boolean isShowingExtra();

		long getNowToTheMinute();

		boolean isClosestPOI(String uuid);

		boolean isFavorite(String uuid);

		boolean isShowingFavorite();

		float getLocationDeclination();

		int getLastCompassInDegree();

		Location getLocation();

		boolean hasLastCompassInDegree();

		boolean hasLocation();

		boolean isShowingServiceUpdates();
	}

	private static class CommonViewHolder {
		TextView nameTv;
		TextView distanceTv;
		ImageView favImg;
		MTCompassView compassV;
		CommonStatusViewHolder statusViewHolder;
		ServiceUpdateViewHolder serviceUpdateViewHolder;
	}

	private static class TextMessageViewHolder extends CommonViewHolder {
	}

	private static class ModuleViewHolder extends CommonViewHolder {
		ImageView moduleExtraTypeImg;
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
