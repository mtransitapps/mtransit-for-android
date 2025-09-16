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
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.IAgencyUIProperties;
import org.mtransit.android.data.JPaths;
import org.mtransit.android.data.Module;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.POIManagerExtKt;
import org.mtransit.android.data.Place;
import org.mtransit.android.databinding.LayoutPoiBasicBinding;
import org.mtransit.android.databinding.LayoutPoiBasicWithAvailabilityPercentBinding;
import org.mtransit.android.databinding.LayoutPoiModuleBinding;
import org.mtransit.android.databinding.LayoutPoiModuleWithAppStatusBinding;
import org.mtransit.android.databinding.LayoutPoiPlaceBinding;
import org.mtransit.android.databinding.LayoutPoiRdsBinding;
import org.mtransit.android.databinding.LayoutPoiRdsWithScheduleBinding;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.rds.route.RDSRouteFragment;
import org.mtransit.android.ui.view.common.MTTransitions;
import org.mtransit.android.ui.view.common.NavControllerExtKt;
import org.mtransit.android.ui.view.poi.serviceupdate.POIServiceUpdateViewHolder;
import org.mtransit.android.ui.view.poi.status.POICommonStatusViewHolder;
import org.mtransit.android.util.UIDirectionUtils;
import org.mtransit.android.util.UIRouteUtils;
import org.mtransit.commons.FeatureFlags;

import java.util.List;

public class POIViewController implements MTLog.Loggable {

	private static final String LOG_TAG = POIViewController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("unused")
	@NonNull
	public static ViewBinding getLayoutViewBinding(int poiType, int poiStatusType, @NonNull ViewStub viewStub) {
		viewStub.setLayoutResource(getLayoutResId(poiType, poiStatusType));
		switch (poiType) {
		case POI.ITEM_VIEW_TYPE_PLACE:
			return LayoutPoiPlaceBinding.bind(viewStub.inflate());
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return LayoutPoiBasicBinding.bind(viewStub.inflate());
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleLayoutViewBinding(poiStatusType, viewStub);
		case POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP:
			return getRDSLayoutViewBinding(poiStatusType, viewStub);
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

	@SuppressWarnings("WeakerAccess")
	@LayoutRes
	public static int getLayoutResId(int poiType, int poiStatusType) {
		switch (poiType) {
		case POI.ITEM_VIEW_TYPE_PLACE:
			return R.layout.layout_poi_place;
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return R.layout.layout_poi_basic;
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleLayout(poiStatusType);
		case POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP:
			return getRDSLayout(poiStatusType);
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOILayout(poiStatusType);
		default:
			MTLog.w(LOG_TAG, "getLayoutResId() > Unknown view type '%s' for status %s!", poiType, poiStatusType);
			return getBasicPOILayout(poiStatusType);
		}
	}

	private static int getRDSLayout(int status) {
		int layoutRes = R.layout.layout_poi_rds;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			layoutRes = R.layout.layout_poi_rds_with_schedule;
			break;
		default:
			MTLog.w(LOG_TAG, "Unexpected status '%s' (rds view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	@NonNull
	private static ViewBinding getRDSLayoutViewBinding(int status, @NonNull ViewStub viewStub) {
		switch (status) {
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			return LayoutPoiRdsWithScheduleBinding.bind(viewStub.inflate());
		case POI.ITEM_STATUS_TYPE_NONE:
			return LayoutPoiRdsBinding.bind(viewStub.inflate());
		default:
			MTLog.w(LOG_TAG, "Unexpected status '%s' (rds view w/o status)!", status);
			return LayoutPoiRdsBinding.bind(viewStub.inflate());
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
		CommonViewHolder holder;
		switch (poi.getType()) {
		case POI.ITEM_VIEW_TYPE_PLACE:
			holder = initPlaceViewHolder(view);
			break;
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			holder = initTextMessageViewHolder(view);
			break;
		case POI.ITEM_VIEW_TYPE_MODULE:
			holder = initModuleViewHolder(view);
			break;
		case POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP:
			holder = initRDSViewHolder(view);
			break;
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			holder = initBasicViewHolder(view);
			break;
		default:
			MTLog.w(LOG_TAG, "initViewHolder() > Unknown view type for poi type %s!", poi.getType());
			holder = initBasicViewHolder(view);
		}
		initCommonViewHolder(holder, view);
		holder.statusViewHolder = POICommonStatusViewHolder.init(poi, view);
		holder.serviceUpdateViewHolder = POIServiceUpdateViewHolder.init(poi, view);
		view.setTag(holder);
	}

	private static CommonViewHolder initModuleViewHolder(@NonNull View view) {
		ModuleViewHolder holder = new ModuleViewHolder();
		holder.moduleExtraTypeImg = view.findViewById(R.id.extra);
		return holder;
	}

	private static CommonViewHolder initTextMessageViewHolder(@SuppressWarnings("unused") @NonNull View view) {
		return new TextMessageViewHolder();
	}

	private static CommonViewHolder initPlaceViewHolder(@NonNull View view) {
		PlaceViewHolder placeViewHolder = new PlaceViewHolder();
		placeViewHolder.placeIconImg = view.findViewById(R.id.extra);
		return placeViewHolder;
	}

	private static CommonViewHolder initBasicViewHolder(@SuppressWarnings("unused") @NonNull View view) {
		return new BasicPOIViewHolder();
	}

	private static CommonViewHolder initRDSViewHolder(@NonNull View view) {
		RouteDirectionStopViewHolder holder = new RouteDirectionStopViewHolder();
		initRDSExtra(view, holder);
		return holder;
	}

	private static void initRDSExtra(@NonNull View view, @NonNull RouteDirectionStopViewHolder holder) {
		holder.rdsExtraV = view.findViewById(R.id.extra);
		holder.routeFL = view.findViewById(R.id.route);
		holder.routeShortNameTv = view.findViewById(R.id.route_short_name);
		holder.routeTypeImg = view.findViewById(R.id.route_type_img);
		holder.directionHeadingTv = view.findViewById(R.id.direction_heading);
		holder.directionHeadingBg = view.findViewById(R.id.direction_heading_bg);
	}

	private static void initCommonViewHolder(@NonNull CommonViewHolder holder, @NonNull View view) {
		holder.uuid = null;
		holder.view = view;
		holder.nameTv = view.findViewById(R.id.name);
		holder.favImg = view.findViewById(R.id.fav);
		holder.distanceTv = view.findViewById(R.id.distance);
		holder.compassV = view.findViewById(R.id.compass);
	}

	@SuppressWarnings("unused")
	public static void updatePOIView(@Nullable View view, @Nullable POI poi, @NonNull POIDataProvider dataProvider) {
		if (view == null || poi == null) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view or poi)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poi, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updatePOICommonView(view.getContext(), holder, poi, dataProvider);
		updateExtra(view.getContext(), holder, poi, dataProvider);
		if (holder.statusViewHolder != null && !dataProvider.isShowingStatus()) {
			holder.statusViewHolder.hideStatus();
			return;
		}
		if (holder.serviceUpdateViewHolder != null && !dataProvider.isShowingServiceUpdates()) {
			holder.serviceUpdateViewHolder.hideServiceUpdate();
		}
	}

	private static void updatePOICommonView(@NonNull Context context, @NonNull CommonViewHolder holder, @NonNull POI poi, @NonNull POIDataProvider dataProvider) {
		//noinspection ConstantConditions // poi always non-null?
		if (poi == null) {
			MTLog.d(LOG_TAG, "updateCommonView() > SKIP (no poi)");
			return;
		}
		holder.nameTv.setText(POIManagerExtKt.getLabelDecorated(poi, context, dataProvider.isShowingAccessibilityInfo()));
		final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
		holder.nameTv.setSingleLine(true); // marquee forever
		holder.nameTv.setSelected(!demoModeManager.isFullDemo()); // marquee forever
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
		if (index == 0) {
			holder.nameTv.setTypeface(Typeface.DEFAULT_BOLD);
			holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			holder.nameTv.setTypeface(Typeface.DEFAULT);
			holder.distanceTv.setTypeface(Typeface.DEFAULT);
		}
	}

	public static void updateView(@Nullable View view, @Nullable POIManager poim, @NonNull POIDataProvider dataProvider) {
		if (view == null || poim == null) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view or poi)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		updateCommonView(view.getContext(), holder, poim, dataProvider);
		updateExtra(view.getContext(), holder, poim, dataProvider);
		POICommonStatusViewHolder.fetchAndUpdateView(holder.statusViewHolder, poim, dataProvider);
		POIServiceUpdateViewHolder.fetchAndUpdateView(holder.serviceUpdateViewHolder, poim, dataProvider);
	}

	private static void updateExtra(@NonNull Context context,
									@NonNull CommonViewHolder holder,
									@NonNull POIManager poim,
									@NonNull POIDataProvider dataProvider) {
		final int poiType = poim.poi.getType();
		switch (poiType) {
		case POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP:
			updateRDSExtra(context, poim, (RouteDirectionStopViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_MODULE:
			updateModuleExtra(poim, (ModuleViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_PLACE:
			updatePlaceExtra(poim, (PlaceViewHolder) holder, dataProvider);
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			break;
		default:
			MTLog.w(LOG_TAG, "updateView() > Unknown view type for poi type %s!", poiType);
		}
	}

	private static void updatePlaceExtra(@NonNull POI poi,
										 @NonNull PlaceViewHolder holder,
										 @NonNull POIDataProvider dataProvider) {
		if (poi instanceof Place) {
			final Place place = (Place) poi;
			final RequestManager glideRequestManager;
			if (dataProvider.getActivity() != null && dataProvider.getActivity() instanceof FragmentActivity) {
				glideRequestManager = Glide.with((FragmentActivity) dataProvider.getActivity());
			} else {
				glideRequestManager = Glide.with(holder.view.getContext());
			}
			glideRequestManager
					.load(place.getIconUrl())
					.into(holder.placeIconImg);
			holder.placeIconImg.setVisibility(View.VISIBLE);
		} else {
			holder.placeIconImg.setVisibility(View.GONE);
		}
	}

	private static void updatePlaceExtra(@NonNull POIManager poim,
										 @NonNull PlaceViewHolder holder,
										 @NonNull POIDataProvider dataProvider) {
		if (poim.poi instanceof Place) {
			final Place place = (Place) poim.poi;
			POIViewUtils.setupPOIExtraLayoutBackground(holder.placeIconImg, poim, dataProvider.providesDataSourcesRepository());
			final RequestManager glideRequestManager;
			if (dataProvider.getActivity() != null && dataProvider.getActivity() instanceof FragmentActivity) {
				glideRequestManager = Glide.with((FragmentActivity) dataProvider.getActivity());
			} else {
				glideRequestManager = Glide.with(holder.view.getContext());
			}
			glideRequestManager
					.load(place.getIconUrl())
					.into(holder.placeIconImg);
			holder.placeIconImg.setVisibility(View.VISIBLE);
		} else {
			holder.placeIconImg.setVisibility(View.GONE);
		}
	}

	private static void updateModuleExtra(@NonNull POIManager poim,
										  @NonNull ModuleViewHolder holder,
										  @NonNull POIDataProvider dataProvider) {
		if (poim.poi instanceof Module) {
			Module module = (Module) poim.poi;
			POIViewUtils.setupPOIExtraLayoutBackground(holder.moduleExtraTypeImg, poim, dataProvider.providesDataSourcesRepository());
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
		case POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP:
			updateRDSExtra(context, poi, (RouteDirectionStopViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_MODULE:
			updateModuleExtra(poi, (ModuleViewHolder) holder);
			break;
		case POI.ITEM_VIEW_TYPE_PLACE:
			updatePlaceExtra(poi, (PlaceViewHolder) holder, dataProvider);
			break;
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			break;
		default:
			MTLog.w(LOG_TAG, "updateView() > Unknown view type for poi type %s!", poiType);
		}
	}

	private static void updateRDSExtra(@NonNull Context context,
									   @NonNull POI poi,
									   @NonNull RouteDirectionStopViewHolder holder,
									   @NonNull final POIDataProvider dataProvider) {
		if (poi instanceof RouteDirectionStop) {
			RouteDirectionStop rds = (RouteDirectionStop) poi;
			//noinspection ConstantConditions // route is always non-null?
			if (dataProvider.isShowingExtra() && rds.getRoute() == null) {
				if (holder.rdsExtraV != null) {
					holder.rdsExtraV.setVisibility(View.GONE);
				}
				if (holder.routeFL != null) {
					holder.routeFL.setVisibility(View.GONE);
				}
				if (holder.directionHeadingBg != null) {
					holder.directionHeadingBg.setVisibility(View.GONE);
				}
			} else {
				final Route route = rds.getRoute();
				if (TextUtils.isEmpty(route.getShortName())) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
					if (holder.routeTypeImg.hasPaths() && poi.getAuthority().equals(holder.routeTypeImg.getTag())) {
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						final IAgencyUIProperties agency = dataProvider.providesDataSourcesRepository().getAgency(poi.getAuthority());
						JPaths rdsRouteLogo = agency == null ? null : agency.getLogo();
						if (rdsRouteLogo != null) {
							holder.routeTypeImg.setJSON(rdsRouteLogo);
							holder.routeTypeImg.setTag(poi.getAuthority());
							holder.routeTypeImg.setVisibility(View.VISIBLE);
						} else {
							holder.routeTypeImg.setVisibility(View.GONE);
						}
					}
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					holder.routeShortNameTv.setText(UIRouteUtils.decorateRouteShortName(context, route.getShortName()));
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				holder.routeFL.setVisibility(View.VISIBLE);
				holder.rdsExtraV.setVisibility(View.VISIBLE);
				//noinspection ConstantConditions // always non-null?
				if (rds.getDirection() == null) {
					holder.directionHeadingBg.setVisibility(View.GONE);
				} else {
					holder.directionHeadingTv.setText(
							UIDirectionUtils.decorateDirection(context, rds.getDirection().getUIHeading(context, true), true)
					);
					final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
					holder.directionHeadingTv.setSingleLine(true); // marquee forever
					holder.directionHeadingTv.setSelected(!demoModeManager.isFullDemo()); // marquee forever
					holder.directionHeadingBg.setVisibility(View.VISIBLE);
				}
				holder.rdsExtraV.setOnClickListener(view -> {
					MTTransitions.setTransitionName(view, "r_" + rds.getAuthority() + "_" + rds.getRoute().getId());
					if (FeatureFlags.F_NAVIGATION) {
						final NavController navController = Navigation.findNavController(view);
						FragmentNavigator.Extras extras = null;
						if (FeatureFlags.F_TRANSITION) {
							extras = new FragmentNavigator.Extras.Builder()
									.addSharedElement(view, view.getTransitionName())
									.build();
						}
						NavControllerExtKt.navigateF(navController,
								R.id.nav_to_rds_route_screen,
								RDSRouteFragment.newInstanceArgs(rds),
								null,
								extras
						);
					} else {
						final MainActivity mainActivity = (MainActivity) dataProvider.getActivity();
						if (mainActivity == null) {
							return;
						}
						mainActivity.addFragmentToStack(
								RDSRouteFragment.newInstance(rds),
								view
						);
					}
				});
			}
		}
	}

	private static void updateModuleExtra(@NonNull POI poi,
										  @NonNull ModuleViewHolder holder) {
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

	private static void updateRDSExtra(@NonNull Context context,
									   @NonNull POIManager poim,
									   @NonNull RouteDirectionStopViewHolder holder,
									   @NonNull final POIDataProvider dataProvider) {
		final POI poi = poim.poi;
		if (poi instanceof RouteDirectionStop) {
			RouteDirectionStop rds = (RouteDirectionStop) poi;
			//noinspection ConstantConditions // route is always non-null?
			if (dataProvider.isShowingExtra() && rds.getRoute() == null) {
				if (holder.rdsExtraV != null) {
					holder.rdsExtraV.setVisibility(View.GONE);
				}
				if (holder.routeFL != null) {
					holder.routeFL.setVisibility(View.GONE);
				}
				if (holder.directionHeadingBg != null) {
					holder.directionHeadingBg.setVisibility(View.GONE);
				}
			} else {
				final Route route = rds.getRoute();
				if (TextUtils.isEmpty(route.getShortName())) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
					if (holder.routeTypeImg.hasPaths() && poi.getAuthority().equals(holder.routeTypeImg.getTag())) {
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						final IAgencyUIProperties agency = dataProvider.providesDataSourcesRepository().getAgency(poi.getAuthority());
						JPaths rdsRouteLogo = agency == null ? null : agency.getLogo();
						if (rdsRouteLogo != null) {
							holder.routeTypeImg.setJSON(rdsRouteLogo);
							holder.routeTypeImg.setTag(poi.getAuthority());
							holder.routeTypeImg.setVisibility(View.VISIBLE);
						} else {
							holder.routeTypeImg.setVisibility(View.GONE);
						}
					}
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					holder.routeShortNameTv.setText(UIRouteUtils.decorateRouteShortName(context, route.getShortName()));
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				holder.routeFL.setVisibility(View.VISIBLE);
				holder.rdsExtraV.setVisibility(View.VISIBLE);
				//noinspection ConstantConditions // always non-null?
				if (rds.getDirection() == null) {
					holder.directionHeadingBg.setVisibility(View.GONE);
				} else {
					holder.directionHeadingTv.setText(
							UIDirectionUtils.decorateDirection(context, rds.getDirection().getUIHeading(context, true), true)
					);
					final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
					holder.directionHeadingTv.setSingleLine(true); // marquee forever
					holder.directionHeadingTv.setSelected(!demoModeManager.isFullDemo()); // marquee forever
					holder.directionHeadingBg.setVisibility(View.VISIBLE);
				}
				POIViewUtils.setupPOIExtraLayoutBackground(holder.rdsExtraV, poim, dataProvider.providesDataSourcesRepository());
				holder.rdsExtraV.setOnClickListener(view -> {
					MTTransitions.setTransitionName(view, "r_" + rds.getAuthority() + "_" + rds.getRoute().getId());
					if (FeatureFlags.F_NAVIGATION) {
						final NavController navController = Navigation.findNavController(view);
						FragmentNavigator.Extras extras = null;
						if (FeatureFlags.F_TRANSITION) {
							extras = new FragmentNavigator.Extras.Builder()
									.addSharedElement(view, view.getTransitionName())
									.build();
						}
						NavControllerExtKt.navigateF(navController,
								R.id.nav_to_rds_route_screen,
								RDSRouteFragment.newInstanceArgs(rds),
								null,
								extras
						);
					} else {
						final MainActivity mainActivity = (MainActivity) dataProvider.getActivity();
						if (mainActivity == null) {
							return;
						}
						mainActivity.addFragmentToStack(
								RDSRouteFragment.newInstance(rds),
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
		POICommonStatusViewHolder.updateView(holder.statusViewHolder, status, dataProvider);
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
		POICommonStatusViewHolder.fetchAndUpdateView(holder.statusViewHolder, poim, dataProvider);
	}

	public static void updateServiceUpdatesView(@Nullable View view,
												@Nullable List<ServiceUpdate> serviceUpdates,
												@NonNull POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			MTLog.d(LOG_TAG, "updateServiceUpdatesView() > SKIP (no view or view holder)");
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		POIServiceUpdateViewHolder.updateView(holder.serviceUpdateViewHolder, serviceUpdates, dataProvider);
	}

	public static void updatePOIServiceUpdate(@Nullable View view,
											  @NonNull POIManager poim,
											  @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updatePOIServiceUpdate() > SKIP (no view");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			initViewHolder(poim, view);
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		POIServiceUpdateViewHolder.fetchAndUpdateView(holder.serviceUpdateViewHolder, poim, dataProvider);
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

	private static void updateCommonView(@NonNull Context context, @NonNull CommonViewHolder holder, @NonNull POIManager poim, @NonNull POIDataProvider dataProvider) {
		//noinspection ConstantConditions // poi always non-null?
		if (poim.poi == null) {
			MTLog.d(LOG_TAG, "updateCommonView() > SKIP (no poi)");
			return;
		}
		final POI poi = poim.poi;
		holder.uuid = poi.getUUID();
		MTTransitions.setTransitionName(holder.view, "poi_" + poi.getUUID());
		holder.nameTv.setText(POIManagerExtKt.getLabelDecorated(poi, context, dataProvider.isShowingAccessibilityInfo()));
		final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
		holder.nameTv.setSingleLine(true); // marquee forever
		holder.nameTv.setSelected(!demoModeManager.isFullDemo()); // marquee forever
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
		if (index == 0) {
			holder.nameTv.setTypeface(Typeface.DEFAULT_BOLD);
			holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			holder.nameTv.setTypeface(Typeface.DEFAULT);
			holder.distanceTv.setTypeface(Typeface.DEFAULT);
		}
	}

	private static class CommonViewHolder {
		String uuid;
		View view;
		TextView nameTv;
		TextView distanceTv;
		ImageView favImg;
		MTCompassView compassV;
		@Nullable
		POICommonStatusViewHolder<?, ?> statusViewHolder;
		POIServiceUpdateViewHolder serviceUpdateViewHolder;
	}

	private static class TextMessageViewHolder extends CommonViewHolder {
	}

	private static class PlaceViewHolder extends CommonViewHolder {
		ImageView placeIconImg;
	}

	private static class ModuleViewHolder extends CommonViewHolder {
		ImageView moduleExtraTypeImg;
	}

	private static class RouteDirectionStopViewHolder extends CommonViewHolder {
		TextView routeShortNameTv;
		View routeFL;
		View rdsExtraV;
		MTJPathsView routeTypeImg;
		TextView directionHeadingTv;
		View directionHeadingBg;
	}

	private static class BasicPOIViewHolder extends CommonViewHolder {
	}
}
