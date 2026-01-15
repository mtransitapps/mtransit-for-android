package org.mtransit.android.ui.view;

import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initBasicViewHolder;
import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initModuleViewHolder;
import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initPlaceViewHolder;
import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initRDSViewHolder;
import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initTextMessageViewHolder;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;

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
import org.mtransit.android.ui.view.poi.CommonViewHolder;
import org.mtransit.android.ui.view.poi.ModuleViewHolder;
import org.mtransit.android.ui.view.poi.PlaceViewHolder;
import org.mtransit.android.ui.view.poi.RouteDirectionStopViewHolder;
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
		holder.setStatusViewHolder(POICommonStatusViewHolder.init(poi, view));
		holder.setServiceUpdateViewHolder(POIServiceUpdateViewHolder.init(poi, view, view.findViewById(R.id.route_direction_service_update_img)));
		view.setTag(holder);
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
		POICommonStatusViewHolder.fetchAndUpdateView(holder.getStatusViewHolder(), poim, dataProvider);
		POIServiceUpdateViewHolder.fetchAndUpdateView(holder.getServiceUpdateViewHolder(), poim, dataProvider);
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

	private static void updatePlaceExtra(@NonNull POIManager poim,
										 @NonNull PlaceViewHolder holder,
										 @NonNull POIDataProvider dataProvider) {
		if (poim.poi instanceof Place) {
			final Place place = (Place) poim.poi;
			POIViewUtils.setupPOIExtraLayoutBackground(holder.getPlaceIconImg(), poim, dataProvider.providesDataSourcesRepository());
			final RequestManager glideRequestManager;
			if (dataProvider.getActivity() != null && dataProvider.getActivity() instanceof FragmentActivity) {
				glideRequestManager = Glide.with((FragmentActivity) dataProvider.getActivity());
			} else {
				glideRequestManager = Glide.with(holder.getView().getContext());
			}
			glideRequestManager
					.load(place.getIconUrl())
					.into(holder.getPlaceIconImg());
			holder.getPlaceIconImg().setVisibility(View.VISIBLE);
		} else {
			holder.getPlaceIconImg().setVisibility(View.GONE);
		}
	}

	private static void updateModuleExtra(@NonNull POIManager poim,
										  @NonNull ModuleViewHolder holder,
										  @NonNull POIDataProvider dataProvider) {
		if (poim.poi instanceof Module) {
			Module module = (Module) poim.poi;
			POIViewUtils.setupPOIExtraLayoutBackground(holder.getModuleExtraTypeImg(), poim, dataProvider.providesDataSourcesRepository());
			final DataSourceType moduleType = DataSourceType.parseId(module.getTargetTypeId());
			if (moduleType != null) {
				holder.getModuleExtraTypeImg().setImageResource(moduleType.getIconResId());
			} else {
				holder.getModuleExtraTypeImg().setImageResource(0);
			}
			holder.getModuleExtraTypeImg().setVisibility(View.VISIBLE);
		} else {
			holder.getModuleExtraTypeImg().setVisibility(View.GONE);
		}
	}

	private static void updateRDSExtra(@NonNull Context context,
									   @NonNull POIManager poim,
									   @NonNull RouteDirectionStopViewHolder holder,
									   @NonNull final POIDataProvider dataProvider) {
		final POI poi = poim.poi;
		if (!(poi instanceof RouteDirectionStop)) {
			return;
		}
		final RouteDirectionStop rds = (RouteDirectionStop) poi;
		if (!dataProvider.isShowingExtra()) {
			holder.getRdsExtraV().setVisibility(View.GONE);
			holder.getRouteFL().setVisibility(View.GONE);
			holder.getDirectionHeadingBg().setVisibility(View.GONE);

			holder.getNoExtra().setVisibility(View.VISIBLE);
			return;
		}
		final Route route = rds.getRoute();
		if (TextUtils.isEmpty(route.getShortName())) {
			holder.getRouteShortNameTv().setVisibility(View.INVISIBLE);
			if (holder.getRouteTypeImg().hasPaths() && poi.getAuthority().equals(holder.getRouteTypeImg().getTag())) {
				holder.getRouteTypeImg().setVisibility(View.VISIBLE);
			} else {
				final IAgencyUIProperties agency = dataProvider.providesDataSourcesRepository().getAgency(poi.getAuthority());
				JPaths rdsRouteLogo = agency == null ? null : agency.getLogo();
				if (rdsRouteLogo != null) {
					holder.getRouteTypeImg().setJSON(rdsRouteLogo);
					holder.getRouteTypeImg().setTag(poi.getAuthority());
					holder.getRouteTypeImg().setVisibility(View.VISIBLE);
				} else {
					holder.getRouteTypeImg().setVisibility(View.GONE);
				}
			}
		} else {
			holder.getRouteTypeImg().setVisibility(View.GONE);
			holder.getRouteShortNameTv().setText(UIRouteUtils.decorateRouteShortName(context, route.getShortName()));
			holder.getRouteShortNameTv().setVisibility(View.VISIBLE);
		}
		holder.getRouteFL().setVisibility(View.VISIBLE);
		holder.getRdsExtraV().setVisibility(View.VISIBLE);
		holder.getNoExtra().setVisibility(View.GONE);
		//noinspection ConstantConditions // always non-null?
		if (rds.getDirection() == null) {
			holder.getDirectionHeadingBg().setVisibility(View.GONE);
		} else {
			holder.getDirectionHeadingTv().setText(
					UIDirectionUtils.decorateDirection(context, rds.getDirection().getUIHeading(context, true), true)
			);
			final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
			holder.getDirectionHeadingTv().setSingleLine(true); // marquee forever
			holder.getDirectionHeadingTv().setSelected(!demoModeManager.isFullDemo()); // marquee forever
			holder.getDirectionHeadingBg().setVisibility(View.VISIBLE);
		}
		POIViewUtils.setupPOIExtraLayoutBackground(holder.getRdsExtraV(), poim, dataProvider.providesDataSourcesRepository());
		holder.getRdsExtraV().setOnClickListener(view -> {
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

	public static void updatePOIStatus(@Nullable View view, @NonNull POIStatus status, @NonNull POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			MTLog.d(LOG_TAG, "updatePOIStatus() > SKIP (no view or view holder)");
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		POICommonStatusViewHolder.updateView(holder.getStatusViewHolder(), status, dataProvider);
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
		POICommonStatusViewHolder.fetchAndUpdateView(holder.getStatusViewHolder(), poim, dataProvider);
	}

	public static void updateServiceUpdatesView(@Nullable View view,
												@Nullable List<ServiceUpdate> serviceUpdates,
												@NonNull POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof CommonViewHolder)) {
			MTLog.d(LOG_TAG, "updateServiceUpdatesView() > SKIP (no view or view holder)");
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) view.getTag();
		POIServiceUpdateViewHolder.updateView(holder.getServiceUpdateViewHolder(), serviceUpdates, dataProvider);
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
		POIServiceUpdateViewHolder.fetchAndUpdateView(holder.getServiceUpdateViewHolder(), poim, dataProvider);
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
		if (holder.getCompassV() != null) {
			holder.getCompassV().setLatLng(poi.getLat(), poi.getLng());
		}
		if (holder.getDistanceTv() != null) {
			if (poim.getDistanceString() != null) {
				if (!poim.getDistanceString().equals(holder.getDistanceTv().getText())) {
					holder.getDistanceTv().setText(poim.getDistanceString());
				}
				holder.getDistanceTv().setVisibility(View.VISIBLE);
			} else {
				holder.getDistanceTv().setVisibility(View.GONE);
				holder.getDistanceTv().setText(null);
			}
		}
		if (holder.getDistanceTv().getVisibility() == View.VISIBLE) {
			if (dataProvider.getDeviceLocation() != null
					&& dataProvider.getLastCompassInDegree() != null
					&& dataProvider.getLocationDeclination() != null
					&& dataProvider.getDeviceLocation().getAccuracy() <= poim.getDistance()) {
				holder.getCompassV().generateAndSetHeadingN(
						dataProvider.getDeviceLocation(), dataProvider.getLastCompassInDegree(), dataProvider.getLocationDeclination());
			} else {
				holder.getCompassV().resetHeading();
			}
			holder.getCompassV().setVisibility(View.VISIBLE);
		} else {
			holder.getCompassV().resetHeading();
			holder.getCompassV().setVisibility(View.GONE);
		}
	}

	private static void updateCommonView(@NonNull Context context, @NonNull CommonViewHolder holder, @NonNull POIManager poim, @NonNull POIDataProvider dataProvider) {
		//noinspection ConstantConditions // poi always non-null?
		if (poim.poi == null) {
			MTLog.d(LOG_TAG, "updateCommonView() > SKIP (no poi)");
			return;
		}
		final POI poi = poim.poi;
		holder.setUuid(poi.getUUID());
		MTTransitions.setTransitionName(holder.getView(), "poi_" + poi.getUUID());
		holder.getNameTv().setText(POIManagerExtKt.getLabelDecorated(poi, context, dataProvider.isShowingAccessibilityInfo()));
		final DemoModeManager demoModeManager = dataProvider.providesDemoModeManager();
		holder.getNameTv().setSingleLine(true); // marquee forever
		holder.getNameTv().setSelected(!demoModeManager.isFullDemo()); // marquee forever
		updatePOIDistanceAndCompass(holder, poim, dataProvider);
		if (dataProvider.isShowingFavorite() && dataProvider.isFavorite(poi.getUUID())) {
			holder.getFavImg().setVisibility(View.VISIBLE);
		} else {
			holder.getFavImg().setVisibility(View.GONE);
		}
		int index;
		if (dataProvider.isClosestPOI(poi.getUUID())) {
			index = 0;
		} else {
			index = -1;
		}
		if (index == 0) {
			holder.getNameTv().setTypeface(Typeface.DEFAULT_BOLD);
			if (holder.getDistanceTv() != null) {
				holder.getDistanceTv().setTypeface(Typeface.DEFAULT_BOLD);
			}
		} else {
			holder.getNameTv().setTypeface(Typeface.DEFAULT);
			if (holder.getDistanceTv() != null) {
				holder.getDistanceTv().setTypeface(Typeface.DEFAULT);
			}
		}
	}
}
