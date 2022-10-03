package org.mtransit.android.ui.view;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.viewbinding.ViewBinding;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.IAgencyUIProperties;
import org.mtransit.android.data.JPaths;
import org.mtransit.android.data.Module;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.UISchedule;
import org.mtransit.android.databinding.LayoutPoiBasicBinding;
import org.mtransit.android.databinding.LayoutPoiBasicWithAvailabilityPercentBinding;
import org.mtransit.android.databinding.LayoutPoiModuleBinding;
import org.mtransit.android.databinding.LayoutPoiModuleWithAppStatusBinding;
import org.mtransit.android.databinding.LayoutPoiRtsBinding;
import org.mtransit.android.databinding.LayoutPoiRtsWithScheduleBinding;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.rts.route.RTSRouteFragment;
import org.mtransit.android.ui.view.common.MTTransitions;
import org.mtransit.android.util.UIDirectionUtils;
import org.mtransit.commons.FeatureFlags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "unused", "DuplicateBranchesInSwitch"})
public class POIViewController implements MTLog.Loggable {

	private static final String LOG_TAG = POIViewController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static ViewBinding getLayoutViewBinding(int poiType, int poiStatusType, @NonNull ViewStub viewStub) {
		viewStub.setLayoutResource(getLayoutResId(poiType, poiStatusType));
		switch (poiType) {
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return LayoutPoiBasicBinding.bind(viewStub.inflate());
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleLayoutViewBinding(poiStatusType, viewStub);
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return getRTSLayoutViewBinding(poiStatusType, viewStub);
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOILayoutViewBinding(poiStatusType, viewStub);
		default:
			MTLog.w(LOG_TAG, "getLayoutViewBinding() > Unknown view type '%s' for status %s!", poiType, poiStatusType);
			return getBasicPOILayoutViewBinding(poiStatusType, viewStub);
		}
	}

	@LayoutRes
	public static int getLayoutResId(@NonNull POIManager poim) {
		return getLayoutResId(poim.poi.getType(), poim.getStatusType());
	}

	@LayoutRes
	public static int getLayoutResId(int poiType, int poiStatusType) {
		switch (poiType) {
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return R.layout.layout_poi_basic;
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleLayout(poiStatusType);
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return getRTSLayout(poiStatusType);
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOILayout(poiStatusType);
		default:
			MTLog.w(LOG_TAG, "getLayoutResId() > Unknown view type '%s' for status %s!", poiType, poiStatusType);
			return getBasicPOILayout(poiStatusType);
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

	@NonNull
	private static ViewBinding getRTSLayoutViewBinding(int status, @NonNull ViewStub viewStub) {
		switch (status) {
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			return LayoutPoiRtsWithScheduleBinding.bind(viewStub.inflate());
		case POI.ITEM_STATUS_TYPE_NONE:
			return LayoutPoiRtsBinding.bind(viewStub.inflate());
		default:
			MTLog.w(LOG_TAG, "Unexpected status '%s' (rts view w/o status)!", status);
			return LayoutPoiRtsBinding.bind(viewStub.inflate());
		}
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

	@NonNull
	private static ViewBinding getBasicPOILayoutViewBinding(int status, @NonNull ViewStub viewStub) {
		switch (status) {
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			return LayoutPoiBasicWithAvailabilityPercentBinding.bind(viewStub.inflate());
		case POI.ITEM_STATUS_TYPE_NONE:
			return LayoutPoiBasicBinding.bind(viewStub.inflate());
		default:
			MTLog.w(LOG_TAG, "Unexpected status '%s' (basic view w/o status)!", status);
			return LayoutPoiBasicBinding.bind(viewStub.inflate());
		}
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

	@NonNull
	private static ViewBinding getModuleLayoutViewBinding(int status, @NonNull ViewStub viewStub) {
		switch (status) {
		case POI.ITEM_STATUS_TYPE_APP:
			return LayoutPoiModuleWithAppStatusBinding.bind(viewStub.inflate());
		case POI.ITEM_STATUS_TYPE_NONE:
			return LayoutPoiModuleBinding.bind(viewStub.inflate());
		default:
			MTLog.w(LOG_TAG, "Unexpected status '%s' (module view w/o status)!", status);
			return LayoutPoiModuleBinding.bind(viewStub.inflate());
		}
	}

	private static void initViewHolder(@NonNull POIManager poim, @NonNull View view) {
		initViewHolder(poim.poi, view);
	}

	private static void initViewHolder(@NonNull POI poi, @NonNull View view) {
		initViewHolder(poi.getType(), poi.getStatusType(), view);
	}

	private static void initViewHolder(int poiType, int poiStatusType, @NonNull View view) {
		CommonViewHolder holder;
		switch (poiType) {
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			holder = initTextMessageViewHolder(view);
			break;
		case POI.ITEM_VIEW_TYPE_MODULE:
			holder = initModuleViewHolder(view);
			break;
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			holder = initRTSViewHolder(view);
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			holder = initBasicViewHolder(view);
			break;
		default:
			MTLog.w(LOG_TAG, "initViewHolder() > Unknown view type for poi type %s!", poiType);
			holder = initBasicViewHolder(view);
		}
		initCommonViewHolder(holder, view);
		holder.statusViewHolder = initPOIStatusViewHolder(poiStatusType, view);
		holder.serviceUpdateViewHolder = initServiceUpdateViewHolder(view);
		view.setTag(holder);
	}

	private static ServiceUpdateViewHolder initServiceUpdateViewHolder(@NonNull View view) {
		ServiceUpdateViewHolder holder = new ServiceUpdateViewHolder();
		holder.warningImg = view.findViewById(R.id.service_update_warning);
		return holder;
	}

	private static CommonViewHolder initModuleViewHolder(@NonNull View view) {
		ModuleViewHolder holder = new ModuleViewHolder();
		holder.moduleExtraTypeImg = view.findViewById(R.id.extra);
		return holder;
	}

	private static CommonViewHolder initTextMessageViewHolder(@NonNull View view) {
		return new TextMessageViewHolder();
	}

	private static CommonViewHolder initBasicViewHolder(@NonNull View view) {
		return new BasicPOIViewHolder();
	}

	private static CommonViewHolder initRTSViewHolder(@NonNull View view) {
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

	private static void initCommonViewHolder(@NonNull CommonViewHolder holder, @NonNull View view) {
		holder.uuid = null;
		holder.view = view;
		holder.nameTv = view.findViewById(R.id.name);
		holder.favImg = view.findViewById(R.id.fav);
		holder.distanceTv = view.findViewById(R.id.distance);
		holder.compassV = view.findViewById(R.id.compass);
	}

	public static void updatePOIView(@Nullable View view, @Nullable POI poi, @NonNull POIDataProvider dataProvider) {
		if (view == null || poi == null) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view or poi)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			final int poiType = poi.getType();
			final int poiStatusType = poi.getStatusType();
			initViewHolder(poiType, poiStatusType, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOICommonView(holder, poi, dataProvider);
		updateExtra(view.getContext(), holder, poi, dataProvider);
		if (holder.statusViewHolder != null && !dataProvider.isShowingStatus()) {
			holder.statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			return;
		}
		if (holder.serviceUpdateViewHolder != null && !dataProvider.isShowingServiceUpdates()) {
			holder.serviceUpdateViewHolder.warningImg.setVisibility(View.GONE);
		}
	}

	private static void updatePOICommonView(@NonNull CommonViewHolder holder, @NonNull POI poi, @NonNull POIDataProvider dataProvider) {
		//noinspection ConstantConditions // poi always non-null?
		if (poi == null) {
			MTLog.d(LOG_TAG, "updateCommonView() > SKIP (no poi)");
			return;
		}
		holder.nameTv.setText(poi.getLabel());
		final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
		holder.nameTv.setSingleLine(true); // marquee forever
		holder.nameTv.setSelected(!demoModeManager.getEnabled()); // marquee forever
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
		holder.compassV.setLatLng(poi.getLat(), poi.getLng());
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

	public static void updateView(@Nullable View view, @Nullable POIManager poim, @NonNull POIDataProvider dataProvider) {
		if (view == null || poim == null) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view or poi)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			final int poiType = poim.poi.getType();
			final int poiStatusType = poim.poi.getStatusType();
			initViewHolder(poiType, poiStatusType, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updateCommonView(holder, poim, dataProvider);
		updateExtra(view.getContext(), holder, poim, dataProvider);
		updatePOIStatus(view.getContext(), holder.statusViewHolder, poim, dataProvider);
		updatePOIServiceUpdate(view.getContext(), holder.serviceUpdateViewHolder, poim, dataProvider);
	}

	public static void updatePOIColorView(@Nullable View view,
										  int poiType,
										  int poiStatusType,
										  @Nullable Integer poiColor,
										  @NonNull final POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updatePOIColorView() > SKIP (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poiType, poiStatusType, view);
		}
		switch (poiType) {
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			if (dataProvider.isShowingExtra() && poiColor != null) {
				RouteTripStopViewHolder holder = (RouteTripStopViewHolder) view.getTag();
				holder.rtsExtraV.setBackgroundColor(poiColor);
			}
			break;
		case POI.ITEM_VIEW_TYPE_MODULE:
			if (dataProvider.isShowingExtra() && poiColor != null) {
				ModuleViewHolder holder = (ModuleViewHolder) view.getTag();
				holder.moduleExtraTypeImg.setBackgroundColor(poiColor);
			}
			break;
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			break;
		default:
			MTLog.w(LOG_TAG, "updatePOIColorView() > Unknown view type for poi type %s!", poiType);
		}
	}

	private static void updateExtra(@NonNull Context context,
									@NonNull CommonViewHolder holder,
									@NonNull POIManager poim,
									@NonNull POIDataProvider dataProvider) {
		final int poiType = poim.poi.getType();
		switch (poiType) {
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			updateRTSExtra(context, poim, (RouteTripStopViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_MODULE:
			updateModuleExtra(poim, (ModuleViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			break;
		default:
			MTLog.w(LOG_TAG, "updateView() > Unknown view type for poi type %s!", poiType);
		}
	}

	private static void updateModuleExtra(@NonNull POIManager poim,
										  @NonNull ModuleViewHolder holder,
										  @NonNull POIDataProvider dataProvider) {
		if (poim.poi instanceof Module) {
			Module module = (Module) poim.poi;
			holder.moduleExtraTypeImg.setBackgroundColor(poim.getColor(dataProvider.providesDataSourcesRepository()));
			final DataSourceType moduleType = DataSourceType.parseId(module.getTargetTypeId());
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

	private static void updateExtra(@NonNull Context context,
									@NonNull CommonViewHolder holder,
									@NonNull POI poi,
									@NonNull POIDataProvider dataProvider) {
		final int poiType = poi.getType();
		switch (poiType) {
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			updateRTSExtra(context, poi, (RouteTripStopViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_MODULE:
			updateModuleExtra(poi, (ModuleViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			break;
		default:
			MTLog.w(LOG_TAG, "updateView() > Unknown view type for poi type %s!", poiType);
		}
	}

	private static void updateRTSExtra(@NonNull Context context,
									   @NonNull POI poi,
									   @NonNull RouteTripStopViewHolder holder,
									   @NonNull final POIDataProvider dataProvider) {
		if (poi instanceof RouteTripStop) {
			RouteTripStop rts = (RouteTripStop) poi;
			//noinspection ConstantConditions // route is always non-null?
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
					if (holder.routeTypeImg.hasPaths() && poi.getAuthority().equals(holder.routeTypeImg.getTag())) {
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						final IAgencyUIProperties agency = dataProvider.providesDataSourcesRepository().getAgency(poi.getAuthority());
						JPaths rtsRouteLogo = agency == null ? null : agency.getLogo();
						if (rtsRouteLogo != null) {
							holder.routeTypeImg.setJSON(rtsRouteLogo);
							holder.routeTypeImg.setTag(poi.getAuthority());
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
				//noinspection ConstantConditions // trip always non-null?
				if (rts.getTrip() == null) {
					holder.tripHeadingBg.setVisibility(View.GONE);
				} else {
					holder.tripHeadingTv.setText(
							UIDirectionUtils.decorateDirection(context,
									rts.getTrip().getUIHeading(context, true)
							)
					);
					final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
					holder.tripHeadingTv.setSingleLine(true); // marquee forever
					holder.tripHeadingTv.setSelected(!demoModeManager.getEnabled()); // marquee forever
					holder.tripHeadingBg.setVisibility(View.VISIBLE);
				}
				//noinspection ConstantConditions // stop always non-null?
				final Integer stopId = rts.getStop() == null ? null : rts.getStop().getId();
				holder.rtsExtraV.setOnClickListener(view -> {
					MTTransitions.setTransitionName(view, "r_" + rts.getAuthority() + "_" + rts.getRoute().getId());
					if (FeatureFlags.F_NAVIGATION) {
						final NavController navController = Navigation.findNavController(view);
						FragmentNavigator.Extras extras = null;
						if (FeatureFlags.F_TRANSITION) {
							extras = new FragmentNavigator.Extras.Builder()
									.addSharedElement(view, view.getTransitionName())
									.build();
						}
						navController.navigate(
								R.id.nav_to_rts_route_screen,
								RTSRouteFragment.newInstanceArgs(rts),
								null,
								extras
						);
					} else {
						final MainActivity mainActivity = (MainActivity) dataProvider.getActivity();
						if (mainActivity == null) {
							return;
						}
						mainActivity.addFragmentToStack(
								RTSRouteFragment.newInstance(rts),
								view
						);
					}
				});
			}
		}
	}

	private static void updateModuleExtra(@NonNull POI poi,
										  @NonNull ModuleViewHolder holder,
										  @NonNull POIDataProvider dataProvider) {
		if (poi instanceof Module) {
			Module module = (Module) poi;
			final DataSourceType moduleType = DataSourceType.parseId(module.getTargetTypeId());
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

	private static void updateRTSExtra(@NonNull Context context,
									   @NonNull POIManager poim,
									   @NonNull RouteTripStopViewHolder holder,
									   @NonNull final POIDataProvider dataProvider) {
		final POI poi = poim.poi;
		if (poi instanceof RouteTripStop) {
			RouteTripStop rts = (RouteTripStop) poi;
			//noinspection ConstantConditions // route is always non-null?
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
					if (holder.routeTypeImg.hasPaths() && poi.getAuthority().equals(holder.routeTypeImg.getTag())) {
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						final IAgencyUIProperties agency = dataProvider.providesDataSourcesRepository().getAgency(poi.getAuthority());
						JPaths rtsRouteLogo = agency == null ? null : agency.getLogo();
						if (rtsRouteLogo != null) {
							holder.routeTypeImg.setJSON(rtsRouteLogo);
							holder.routeTypeImg.setTag(poi.getAuthority());
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
				//noinspection ConstantConditions // trip always non-null?
				if (rts.getTrip() == null) {
					holder.tripHeadingBg.setVisibility(View.GONE);
				} else {
					holder.tripHeadingTv.setText(
							UIDirectionUtils.decorateDirection(context,
									rts.getTrip().getUIHeading(context, true)
							)
					);
					final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
					holder.tripHeadingTv.setSingleLine(true); // marquee forever
					holder.tripHeadingTv.setSelected(!demoModeManager.getEnabled()); // marquee forever
					holder.tripHeadingBg.setVisibility(View.VISIBLE);
				}
				holder.rtsExtraV.setBackgroundColor(poim.getColor(dataProvider.providesDataSourcesRepository()));
				//noinspection ConstantConditions // stop always non-null?
				final Integer stopId = rts.getStop() == null ? null : rts.getStop().getId();
				holder.rtsExtraV.setOnClickListener(view -> {
					MTTransitions.setTransitionName(view, "r_" + rts.getAuthority() + "_" + rts.getRoute().getId());
					if (FeatureFlags.F_NAVIGATION) {
						final NavController navController = Navigation.findNavController(view);
						FragmentNavigator.Extras extras = null;
						if (FeatureFlags.F_TRANSITION) {
							extras = new FragmentNavigator.Extras.Builder()
									.addSharedElement(view, view.getTransitionName())
									.build();
						}
						navController.navigate(
								R.id.nav_to_rts_route_screen,
								RTSRouteFragment.newInstanceArgs(rts),
								null,
								extras
						);
					} else {
						final MainActivity mainActivity = (MainActivity) dataProvider.getActivity();
						if (mainActivity == null) {
							return;
						}
						mainActivity.addFragmentToStack(
								RTSRouteFragment.newInstance(rts),
								view
						);
					}
				});
			}
		}
	}

	public static void updatePOIStatus(@Nullable View view, @NonNull POIStatus status, @NonNull POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			MTLog.d(LOG_TAG, "updatePOIStatus() > SKIP (no view or view holder)");
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIStatus(view.getContext(), holder.statusViewHolder, status, dataProvider);
	}

	private static void updatePOIStatus(@NonNull Context context,
										@Nullable CommonStatusViewHolder statusViewHolder,
										@NonNull POIStatus status,
										@NonNull POIDataProvider dataProvider) {
		if (statusViewHolder == null) {
			MTLog.d(LOG_TAG, "updatePOIStatus() > SKIP (no view holder)");
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

	public static void updatePOIStatus(@Nullable View view, @NonNull POIManager poim, @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updatePOIStatus() > SKIP (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIStatus(view.getContext(), holder.statusViewHolder, poim, dataProvider);
	}

	private static void updatePOIStatus(@NonNull Context context,
										@Nullable CommonStatusViewHolder statusViewHolder,
										@NonNull POIManager poim,
										@NonNull POIDataProvider dataProvider) {
		if (statusViewHolder == null) {
			MTLog.d(LOG_TAG, "updatePOIStatus() > SKIP (no view holder)");
			return;
		}
		if (!dataProvider.isShowingStatus()) {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			return;
		}
		final POI poi = poim.poi;
		final int statusType = poi.getStatusType();
		switch (statusType) {
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
			MTLog.w(LOG_TAG, "Unexpected status type '%s'!", statusType);
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAppStatus(@NonNull Context context,
										@NonNull CommonStatusViewHolder statusViewHolder,
										@NonNull POIManager poim,
										@NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof AppStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAppStatus(context, statusViewHolder, poim.getStatus(context, dataProvider.providesStatusLoader()));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAppStatus(@NonNull Context context, @NonNull CommonStatusViewHolder statusViewHolder, @Nullable POIStatus status) {
		AppStatusViewHolder appStatusViewHolder = (AppStatusViewHolder) statusViewHolder;
		if (status instanceof AppStatus) {
			final AppStatus appStatus = (AppStatus) status;
			appStatusViewHolder.textTv.setText(appStatus.getStatusMsg(context), TextView.BufferType.SPANNABLE);
			appStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateRTSSchedule(@NonNull Context context,
										  @NonNull CommonStatusViewHolder statusViewHolder,
										  @NonNull POIManager poim,
										  @NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof ScheduleStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateRTSSchedule(context, statusViewHolder, poim.getStatus(context, dataProvider.providesStatusLoader()), dataProvider);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateRTSSchedule(@NonNull Context context,
										  @NonNull CommonStatusViewHolder statusViewHolder,
										  @Nullable POIStatus status,
										  @NonNull POIDataProvider dataProvider) {
		CharSequence line1CS = null;
		CharSequence line2CS = null;
		if (status instanceof UISchedule) {
			UISchedule schedule = (UISchedule) status;
			ArrayList<Pair<CharSequence, CharSequence>> lines = schedule.getStatus(
					context,
					dataProvider.getNowToTheMinute(),
					TimeUnit.MINUTES.toMillis(30L),
					null,
					10,
					null);
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

	private static void updateAvailabilityPercent(@NonNull Context context,
												  @NonNull CommonStatusViewHolder statusViewHolder,
												  @NonNull POIManager poim,
												  @NonNull POIDataProvider dataProvider) {
		if (dataProvider.isShowingStatus() && statusViewHolder instanceof AvailabilityPercentStatusViewHolder) {
			poim.setStatusLoaderListener(dataProvider);
			updateAvailabilityPercent(context, statusViewHolder, poim.getStatus(context, dataProvider.providesStatusLoader()));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private static void updateAvailabilityPercent(@NonNull Context context,
												  @NonNull CommonStatusViewHolder statusViewHolder,
												  @Nullable POIStatus status) {
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

	public static void updateServiceUpdatesView(@Nullable View view,
												@Nullable List<ServiceUpdate> serviceUpdates,
												@NonNull POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			MTLog.d(LOG_TAG, "updateServiceUpdatesView() > SKIP (no view or view holder)");
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updateServiceUpdateViewHolder(holder.serviceUpdateViewHolder, ServiceUpdate.isSeverityWarning(serviceUpdates), dataProvider);
	}

	public static void updatePOIServiceUpdate(@Nullable View view,
											  @NonNull POIManager poim,
											  @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updatePOIServiceUpdate() > SKIP (no view");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			final int poiType = poim.poi.getType();
			final int poiStatusType = poim.poi.getStatusType();
			initViewHolder(poiType, poiStatusType, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIServiceUpdate(view.getContext(), holder.serviceUpdateViewHolder, poim, dataProvider);
	}

	private static void updatePOIServiceUpdate(@NonNull Context context,
											   @Nullable ServiceUpdateViewHolder serviceUpdateViewHolder,
											   @NonNull POIManager poim,
											   @NonNull POIDataProvider dataProvider) {
		if (serviceUpdateViewHolder != null) {
			if (dataProvider.isShowingServiceUpdates()) {
				poim.setServiceUpdateLoaderListener(dataProvider);
				updateServiceUpdateViewHolder(serviceUpdateViewHolder, poim.isServiceUpdateWarning(context, dataProvider.providesServiceUpdateLoader()), dataProvider);
			} else {
				serviceUpdateViewHolder.warningImg.setVisibility(View.GONE);
			}
		}
	}

	private static void updateServiceUpdateViewHolder(@NonNull ServiceUpdateViewHolder serviceUpdateViewHolder,
													  @Nullable Boolean isServiceUpdateWarning,
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

	public static void updatePOIDistanceAndCompass(@Nullable View view,
												   int poiType,
												   int poiStatusType,
												   @Nullable Float distance,
												   @Nullable CharSequence distanceString,
												   @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updatePOIDistanceAndCompass() > skip (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poiType, poiStatusType, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIDistanceAndCompass(holder, distance, distanceString, dataProvider);
	}

	private static void updatePOIDistanceAndCompass(@Nullable CommonViewHolder holder,
													@Nullable Float distance,
													@Nullable CharSequence distanceString,
													@NonNull POIDataProvider dataProvider) {
		if (holder == null) {
			MTLog.d(LOG_TAG, "updatePOIDistanceAndCompass() > skip (no view holder)");
			return;
		}
		if (distanceString != null) {
			if (!distanceString.equals(holder.distanceTv.getText())) {
				holder.distanceTv.setText(distanceString);
			}
			holder.distanceTv.setVisibility(View.VISIBLE);
		} else {
			holder.distanceTv.setVisibility(View.GONE);
			holder.distanceTv.setText(null);
		}
		if (holder.distanceTv.getVisibility() == View.VISIBLE) {
			if (dataProvider.getLocation() != null
					&& dataProvider.getLastCompassInDegree() != null
					&& dataProvider.getLocationDeclination() != null
					&& distance != null
					&& dataProvider.getLocation().getAccuracy() <= distance) {
				holder.compassV.generateAndSetHeadingN(
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

	public static void updatePOIDistanceAndCompass(@Nullable View view, @NonNull POIManager poim, @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updatePOIDistanceAndCompass() > skip (no view)");
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOIDistanceAndCompass(holder, poim, dataProvider);
	}

	private static void updatePOIDistanceAndCompass(@Nullable CommonViewHolder holder, @Nullable POIManager poim, @NonNull POIDataProvider dataProvider) {
		//noinspection ConstantConditions // poi always non-null?
		final POI poi = poim == null ? null : poim.poi;
		if (poi == null || holder == null) {
			MTLog.d(LOG_TAG, "updatePOIDistanceAndCompass() > skip (no poi or view holder)");
			return;
		}
		holder.compassV.setLatLng(poi.getLat(), poi.getLng());
		if (poim.getDistanceString() != null) {
			if (!poim.getDistanceString().equals(holder.distanceTv.getText())) {
				holder.distanceTv.setText(poim.getDistanceString());
			}
			holder.distanceTv.setVisibility(View.VISIBLE);
		} else {
			holder.distanceTv.setVisibility(View.GONE);
			holder.distanceTv.setText(null);
		}
		if (holder.distanceTv.getVisibility() == View.VISIBLE) {
			if (dataProvider.getLocation() != null
					&& dataProvider.getLastCompassInDegree() != null
					&& dataProvider.getLocationDeclination() != null
					&& dataProvider.getLocation().getAccuracy() <= poim.getDistance()) {
				holder.compassV.generateAndSetHeadingN(
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
			MTLog.d(LOG_TAG, "updateCommonView() > SKIP (no poi)");
			return;
		}
		final POI poi = poim.poi;
		holder.uuid = poi.getUUID();
		MTTransitions.setTransitionName(holder.view, "poi_" + poi.getUUID());
		holder.nameTv.setText(poi.getLabel());
		final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
		holder.nameTv.setSingleLine(true); // marquee forever
		holder.nameTv.setSelected(!demoModeManager.getEnabled()); // marquee forever
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

	private static class CommonViewHolder {
		String uuid;
		View view;
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
