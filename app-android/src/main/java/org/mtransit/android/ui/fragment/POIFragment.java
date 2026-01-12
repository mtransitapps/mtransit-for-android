package org.mtransit.android.ui.fragment;

import static org.mtransit.android.ui.fragment.POIFragmentExtKt.startVehicleLocationCountdownRefresh;
import static org.mtransit.android.ui.fragment.POIFragmentExtKt.stopVehicleLocationCountdownRefresh;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.mtransit.android.R;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.ad.IAdScreenActivity;
import org.mtransit.android.analytics.AnalyticsEvents;
import org.mtransit.android.analytics.AnalyticsEventsParamsProvider;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.common.IContext;
import org.mtransit.android.common.repository.DefaultPreferenceRepository;
import org.mtransit.android.common.repository.LocalPreferenceRepository;
import org.mtransit.android.commons.AppUpdateLauncher;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.Schedule.ScheduleStatusFilter;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.news.NewsProviderContract;
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.IAgencyProperties;
import org.mtransit.android.data.IAgencyUpdatableProperties;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.POIManagerExtKt;
import org.mtransit.android.data.ScheduleProviderProperties;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.datasource.POIRepository;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.provider.permission.LocationPermissionProvider;
import org.mtransit.android.provider.sensor.MTSensorManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.EdgeToEdgeKt;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.main.NextMainViewModel;
import org.mtransit.android.ui.map.MapFragment;
import org.mtransit.android.ui.nearby.NearbyFragment;
import org.mtransit.android.ui.news.NewsListAdapter;
import org.mtransit.android.ui.news.NewsListDetailFragment;
import org.mtransit.android.ui.rds.route.RDSRouteFragment;
import org.mtransit.android.ui.schedule.ScheduleFragment;
import org.mtransit.android.ui.view.MapViewController;
import org.mtransit.android.ui.view.MapViewControllerExtKt;
import org.mtransit.android.ui.view.POIDataProvider;
import org.mtransit.android.ui.view.POIServiceUpdateViewController;
import org.mtransit.android.ui.view.POIStatusDetailViewController;
import org.mtransit.android.ui.view.POIViewController;
import org.mtransit.android.ui.view.common.EventObserver;
import org.mtransit.android.ui.view.common.FragmentKtxKt;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.android.ui.view.common.IFragment;
import org.mtransit.android.ui.view.common.ImageManager;
import org.mtransit.android.ui.view.common.MTTransitions;
import org.mtransit.android.ui.view.common.NavControllerExtKt;
import org.mtransit.android.util.BatteryOptimizationIssueUtils;
import org.mtransit.android.util.DegreeUtils;
import org.mtransit.android.util.FragmentUtils;
import org.mtransit.android.util.LinkUtils;
import org.mtransit.android.util.MapUtils;
import org.mtransit.android.util.UIFeatureFlags;
import org.mtransit.android.util.UITimeUtils;
import org.mtransit.commons.FeatureFlags;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.Job;

@AndroidEntryPoint
public class POIFragment extends ABFragment implements
		POIDataProvider,
		MTActivityWithLocation.DeviceLocationListener,
		SensorEventListener,
		MTSensorManager.CompassListener,
		MTSensorManager.SensorTaskCompleted,
		FavoriteManager.FavoriteUpdateListener,
		UITimeUtils.TimeChangedReceiver.TimeChangedListener,
		IContext,
		IAdManager.RewardedAdListener,
		MenuProvider,
		MapViewController.MapMarkerProvider,
		MapViewController.MapListener {

	private static final String LOG_TAG = POIFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		final String uuid = viewModel == null ? null : viewModel.getUuid().getValue();
		if (uuid != null && !uuid.isEmpty()) {
			final int index = uuid.indexOf(IAgencyProperties.PKG_COMMON);
			return LOG_TAG + "-" + (index == -1 ? uuid : uuid.substring(index + IAgencyProperties.PKG_COMMON.length()));
		}
		return LOG_TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "POI";

	@NonNull
	@Override
	public String getScreenName() {
		final String uuid = viewModel == null ? null : viewModel.getUuid().getValue();
		if (!TextUtils.isEmpty(uuid)) {
			return TRACKING_SCREEN_NAME + "/" + uuid;
		}
		return TRACKING_SCREEN_NAME;
	}

	@NonNull
	public static POIFragment newInstance(@NonNull POIManager poim) {
		return newInstance(poim.poi.getAuthority(), poim.poi.getUUID());
	}

	@NonNull
	public static Bundle newInstanceArgs(@NonNull POIManager poim) {
		return newInstanceArgs(poim.poi);
	}

	@NonNull
	public static Bundle newInstanceArgs(@NonNull POI poi) {
		return newInstanceArgs(poi.getAuthority(), poi.getUUID());
	}

	@NonNull
	public static POIFragment newInstance(@NonNull String authority,
										  @NonNull String uuid) {
		POIFragment f = new POIFragment();
		f.setArguments(newInstanceArgs(authority, uuid));
		return f;
	}

	@NonNull
	public static Bundle newInstanceArgs(@NonNull String authority, @NonNull String uuid) {
		Bundle args = new Bundle();
		args.putString(POIViewModel.EXTRA_AUTHORITY, authority);
		args.putString(POIViewModel.EXTRA_POI_UUID, uuid);
		return args;
	}

	@Nullable
	protected POIViewModel viewModel;
	@Nullable
	private NextMainViewModel nextMainViewModel;

	@Nullable
	private POIViewModel getAttachedViewModel() {
		return FragmentKtxKt.isAttached(this) ? this.viewModel : null;
	}

	@Inject
	LocationPermissionProvider locationPermissionProvider;
	@Inject
	MTSensorManager sensorManager;
	@Inject
	IAdManager adManager;
	@Inject
	DataSourcesRepository dataSourcesRepository;
	@Inject
	POIRepository poiRepository;
	@Inject
	IAnalyticsManager analyticsManager;
	@Inject
	FavoriteManager favoriteManager;
	@Inject
	DemoModeManager demoModeManager;
	@Inject
	DefaultPreferenceRepository defaultPrefRepository;
	@Inject
	LocalPreferenceRepository localPreferenceRepository;
	@Inject
	ImageManager imageManager;

	private static final int TOP_PADDING_SP = 64 - 32;
	private static final int BOTTOM_PADDING_SP = 0;

	@NonNull
	protected final MapViewController mapViewController =
			new MapViewController(
					LOG_TAG,
					this,
					this,
					false,
					true,
					false,
					false,
					false,
					false,
					TOP_PADDING_SP,
					BOTTOM_PADDING_SP,
					true,
					false,
					false,
					true,
					false,
					null // this.dataSourcesRepository
			);

	private void onAgencyLoaded(@Nullable AgencyProperties agency) {
		applyNewAgency();
	}

	private void applyNewAgency() {
		if (getAgencyOrNull() == null) {
			return;
		}
		final Context context = getContext();
		if (context == null) {
			return;
		}
		final POIManager poim = getPoimOrNull();
		if (poim != null) {
			POIStatusDetailViewController.updateView(getPOIStatusView(getView()), poim, this);
		}
		if (getAbController() != null) {
			getAbController().setABTitle(this, getABTitle(context), false);
			getAbController().setABReady(this, isABReady(), true);
		}
		if (FeatureFlags.F_NAVIGATION) {
			if (nextMainViewModel != null) {
				nextMainViewModel.setABTitle(getABTitle(context));
			}
		}
		refreshAppUpdateLayout(getView());
		refreshAppWasDisabledLayout(getView());
		if (viewModel != null) {
			viewModel.refreshAppUpdateAvailable();
		}
		if (getActivity() != null) {
			getActivity().invalidateOptionsMenu(); // update menu_show_fares menu item
		}
	}

	@Nullable
	private AgencyProperties getAgencyOrNull() {
		return getAttachedViewModel() == null ? null : getAttachedViewModel().getAgency().getValue();
	}

	@Nullable
	protected POIManager poim; // kept for now because it can be updated anywhere #GodObject

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean hasPoim() {
		return this.poim != null;
	}

	@Nullable
	private POIManager getPoimOrNull() {
		if (!hasPoim()) {
			return null;
		}
		return this.poim;
	}

	private void setPOIProperties() {
		if (this.poim != null) {
			this.poim.setInFocus(true);
			this.poim.setScheduleMaxDataRequests(ScheduleStatusFilter.DATA_REQUEST_MONTHS);
			this.poim.resetLastFindTimestamps();
		}
	}

	private void onPOIMLoaded(@Nullable POIManager newPOIM) {
		if (newPOIM == null) {
			MTLog.d(this, "onPOIMLoaded() > SKIP (no POI)");
			return; // SKIP
		}
		if (this.poim != null
				&& this.poim.poi.equals(newPOIM.poi)) {
			MTLog.d(this, "onPOIMLoaded() > SKIP (same POI)");
			MTTransitions.startPostponedEnterTransitionOnPreDraw(getView(), this);
			return; // SKIP
		}
		this.poim = newPOIM;
		applyNewPoim();
		MTTransitions.startPostponedEnterTransitionOnPreDraw(getView(), this);
	}

	private void applyNewPoim() {
		if (this.poim == null) {
			MTLog.d(this, "applyNewPoim() > SKIP (no poi)");
			return;
		}
		final Context context = getContext();
		if (context == null) {
			MTLog.d(this, "applyNewPoim() > SKIP (no context)");
			return;
		}
		setPOIProperties();
		if (this.deviceLocation != null) {
			LocationUtils.updateDistanceWithString(context, this.poim, this.deviceLocation);
		}
		if (this.adapter != null) {
			this.adapter.clear();
		}
		resetFavorite();
		final View view = getView();
		if (!FeatureFlags.F_EXPORT_TRIP_ID) {
			this.mapViewController.notifyMarkerChanged(this);
			this.mapViewController.showMap(view);
		}
		this.mapViewController.setFocusedOnUUID(this.poim.getPOI().getUUID());
		POIViewController.updateView(getPOIView(view), this.poim, this);
		POIStatusDetailViewController.updateView(getPOIStatusView(view), this.poim, this);
		POIServiceUpdateViewController.updateView(getPOIServiceUpdateView(view), this.poim, this);
		setupRDSFullScheduleBtn(view);
		setupMoreNewsButton(view);
		setupAppUpdateButton(view);
		setupAppWasDisabledButton(view);
		setupRewardedAdButton(view);
		setupMoreNearbyButton(view);
		updateFabFavorite();
		setupNearbyList();
	}

	private void onNewsLoaded(@Nullable List<News> news) {
		if (news == null) {
			MTLog.d(this, "onNewsLoaded() > SKIP (no news)");
			return;
		}
		final View poiNewsView = getPOINewsView(getView());
		if (poiNewsView != null) {
			poiNewsView.setVisibility(news.isEmpty() ? View.GONE : View.VISIBLE);
		}
		getNewsListAdapter().submitList(news);
	}

	@Nullable
	@Override
	public POIManager getClosestPOI() {
		return this.poim;
	}

	@Nullable
	@Override
	public POIManager getPOI(@Nullable String uuid) {
		return this.poim != null && this.poim.poi.getUUID().equals(uuid) ? this.poim : null;
	}

	@Nullable
	@Override
	public Collection<POIManager> getPOIs() {
		if (FeatureFlags.F_EXPORT_TRIP_ID) {
			final List<POIManager> poiList = viewModel == null ? null : viewModel.getPoiList().getValue();
			if (poiList != null) {
				return poiList;
			}
		}
		if (this.poim == null) {
			return null;
		}
		return Collections.singletonList(this.poim);
	}

	@Nullable
	@Override
	public POIManager getPOI(int position) {
		if (FeatureFlags.F_EXPORT_TRIP_ID) {
			final List<POIManager> poiList = viewModel == null ? null : viewModel.getPoiList().getValue();
			if (poiList == null || position < 0 || position >= poiList.size()) {
				return null;
			}
			return poiList.get(position);
		}
		return position == 0 ? this.poim : null;
	}

	@Nullable
	@Override
	public Collection<MapViewController.POIMarker> getPOMarkers() {
		return null;
	}

	@Nullable
	@Override
	public Collection<VehicleLocation> getVehicleLocations() {
		if (FeatureFlags.F_EXPORT_TRIP_ID) {
			return viewModel == null ? null : viewModel.getVehicleLocations().getValue();
		}
		return null;
	}

	@Nullable
	@Override
	public Integer getVehicleColorInt() {
		final POIManager poim = getPoimOrNull();
		if (poim == null) {
			return null;
		}
		return poim.getColor(dataSourcesRepository);
	}

	@Nullable
	@Override
	public DataSourceType getVehicleType() {
		final IAgencyProperties agency = getAgencyOrNull();
		if (agency == null) {
			return null;
		}
		return agency.getType();
	}

	@Nullable
	@Override
	public Collection<LatLng> getVisibleMarkersLocations() {
		return POIFragmentExtKt.getVisibleMarkersLocationList(this);
	}

	@Nullable
	@Override
	public Float getMapMarkerAlpha(int position) {
		return POIFragmentExtKt.getMapMarkerAlpha(this, position);
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.mapViewController.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCameraChange(@NonNull LatLngBounds latLngBounds, float zoom) {
		// DO NOTHING
	}

	@Override
	public void onMapReady() {
		// DO NOTHING
	}

	@Override
	public void onMapClick(@NonNull LatLng position) {
		if (!FragmentUtils.isFragmentReady(this)) {
			return;
		}
		final POIManager poim = getPoimOrNull();
		if (poim == null) {
			return;
		}
		if (FeatureFlags.F_NAVIGATION) {
			final NavController navController = NavHostFragment.findNavController(this);
			FragmentNavigator.Extras extras = null;
			if (FeatureFlags.F_TRANSITION) {
				extras = new FragmentNavigator.Extras.Builder()
						// TODO marker? .addSharedElement(view, view.getTransitionName())
						.build();
			}
			if (FeatureFlags.F_EXPORT_TRIP_ID) {
				if (poim.poi instanceof RouteDirectionStop) {
					NavControllerExtKt.navigateF(navController,
							R.id.nav_to_rds_route_screen,
							RDSRouteFragment.newInstanceArgs((RouteDirectionStop) poim.poi),
							null,
							extras
					);
				}
			}
			NavControllerExtKt.navigateF(navController,
					R.id.nav_to_map_screen,
					MapFragment.newInstanceArgs(poim),
					null,
					extras
			);
		} else {
			final FragmentActivity activity = getActivity();
			if (activity == null) {
				return;
			}
			if (FeatureFlags.F_EXPORT_TRIP_ID) {
				if (poim.poi instanceof RouteDirectionStop) {
					((MainActivity) activity).addFragmentToStack(
							RDSRouteFragment.newInstance((RouteDirectionStop) poim.poi),
							this
					);
					return;
				}
			}
			((MainActivity) activity).addFragmentToStack(
					MapFragment.newInstance(poim),
					this);
		}
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		initAdapters(this);
		this.mapViewController.setDataSourcesRepository(this.dataSourcesRepository);
		this.mapViewController.setLocationPermissionGranted(this.locationPermissionProvider.allRequiredPermissionsGranted(context));
		this.mapViewController.onAttach(requireActivity());
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.mapViewController.onDetach();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MTTransitions.setContainerTransformTransition(this);
		this.mapViewController.onCreate(savedInstanceState);
	}

	private void onDataSourceRemoved() {
		final MainActivity activity = (MainActivity) getActivity();
		if (activity == null) {
			return;
		}
		if (activity.isMTResumed()) {
			activity.popFragmentFromStack(this); // close this fragment
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.fragment_poi, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		MTTransitions.postponeEnterTransition(this);
		requireActivity().addMenuProvider(
				this, getViewLifecycleOwner(), Lifecycle.State.RESUMED
		);
		if (FeatureFlags.F_NAVIGATION) {
			nextMainViewModel = new ViewModelProvider(requireActivity()).get(NextMainViewModel.class);
		}
		viewModel = new ViewModelProvider(this).get(POIViewModel.class);
		viewModel.getDataSourceRemovedEvent().observe(getViewLifecycleOwner(), new EventObserver<>(removed -> {
			if (removed) {
				onDataSourceRemoved();
			}
			return null;
		}));
		viewModel.getAgency().observe(getViewLifecycleOwner(), this::onAgencyLoaded);
		viewModel.getPoim().observe(getViewLifecycleOwner(), this::onPOIMLoaded);
		viewModel.getScheduleProviders().observe(getViewLifecycleOwner(), scheduleProviders -> setupRDSFullScheduleBtn(getView()));
		viewModel.getNearbyPOIs().observe(getViewLifecycleOwner(), this::onNearbyPOIsLoaded);
		viewModel.getLatestNewsArticleList().observe(getViewLifecycleOwner(), this::onNewsLoaded);
		if (FeatureFlags.F_EXPORT_TRIP_ID) {
			viewModel.getVehicleLocations().observe(getViewLifecycleOwner(), this::onVehicleLocationsLoaded);
			viewModel.getPoiList().observe(getViewLifecycleOwner(), this::onPOIsLoaded);
		}
		setupView(view);
		this.mapViewController.onViewCreated(view, savedInstanceState);
	}

	@Nullable
	protected Job _vehicleLocationCountdownRefreshJob = null;

	private void onVehicleLocationsLoaded(@Nullable List<VehicleLocation> vehicleLocations) {
		if (!FeatureFlags.F_EXPORT_TRIP_ID) return;
		final Context context = getContext();
		if (context != null) {
			MapViewControllerExtKt.updateVehicleLocationMarkers(this.mapViewController, context);
		}
		if (vehicleLocations == null || vehicleLocations.isEmpty()) {
			stopVehicleLocationCountdownRefresh(this);
		} else {
			startVehicleLocationCountdownRefresh(this);
		}
	}

	private void onPOIsLoaded(@Nullable List<POIManager> poiList) {
		if (!FeatureFlags.F_EXPORT_TRIP_ID) return;
		this.mapViewController.notifyMarkerChanged(this);
		this.mapViewController.showMap(getView());
	}

	private void setupNewsLayout(@NonNull View view) {
		final RecyclerView newsList = view.findViewById(R.id.news_list);
		final PagerSnapHelper snapHelper = new PagerSnapHelper();
		snapHelper.attachToRecyclerView(newsList);
		newsList.setAdapter(getNewsListAdapter());
		setupMoreNewsButton(view);
	}

	@Nullable
	private NewsListAdapter newsListAdapter;

	@NonNull
	private NewsListAdapter getNewsListAdapter() {
		if (this.newsListAdapter == null) {
			this.newsListAdapter = new NewsListAdapter(
					this.imageManager,
					(view, article) -> {
						final POIManager poim = getPoimOrNull();
						if (poim == null) {
							return kotlin.Unit.INSTANCE;
						}
						if (FeatureFlags.F_NAVIGATION) {
							final NavController navController = NavHostFragment.findNavController(this);
							FragmentNavigator.Extras extras = null;
							if (FeatureFlags.F_TRANSITION) {
								extras = new FragmentNavigator.Extras.Builder()
										// TODO button? .addSharedElement(view, view.getTransitionName())
										.build();
							}
							NavControllerExtKt.navigateF(navController,
									R.id.nav_to_news_detail_screen,
									NewsListDetailFragment.newInstanceArgs(
											poim.getColor(dataSourcesRepository),
											POIManagerExtKt.getNewOneLineDescriptionForNews(poim.poi, POIFragment.this.dataSourcesRepository),
											Collections.singletonList(poim.poi.getAuthority()),
											NewsProviderContract.Filter.getNewTargetFilter(poim.poi).getTargets(),
											null,
											article.getAuthority(),
											article.getUUID()
									),
									null,
									extras
							);
						} else {
							final Activity activity = getActivity();
							if (activity != null) {
								((MainActivity) activity).addFragmentToStack(
										NewsListDetailFragment.newInstance(
												poim.getColor(dataSourcesRepository),
												POIManagerExtKt.getNewOneLineDescriptionForNews(poim.poi, POIFragment.this.dataSourcesRepository),
												Collections.singletonList(poim.poi.getAuthority()),
												NewsProviderContract.Filter.getNewTargetFilter(poim.poi).getTargets(),
												null,
												article.getAuthority(),
												article.getUUID()
										),
										POIFragment.this
								);
							}
						}
						return kotlin.Unit.INSTANCE;
					},
					5,
					true
			);
		}
		return this.newsListAdapter;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		this.mapViewController.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	@Nullable
	private POIArrayAdapter adapter;

	private void setupNearbyList() {
		if (this.adapter != null && this.adapter.isInitialized()) {
			showNearbyList();
			return;
		}
		hideNearbyList(true);
	}

	private void onNearbyPOIsLoaded(@Nullable List<POIManager> nearbyPOIs) {
		if (this.adapter != null) {
			this.adapter.setPois(nearbyPOIs);
			this.adapter.updateDistanceNowAsync(this.deviceLocation);
			this.adapter.initManual();
			if (this.adapter.getPoisCount() > 0) {
				showNearbyList();
			} else {
				hideNearbyList(false);
			}
		}
	}

	private void hideNearbyList(boolean invisibleInsteadOfGone) {
		View view = getView();
		if (view == null) {
			return;
		}
		view.findViewById(R.id.poi_nearby_pois_title).setVisibility(invisibleInsteadOfGone ? View.INVISIBLE : View.GONE);
		view.findViewById(R.id.poi_nearby_pois_list).setVisibility(invisibleInsteadOfGone ? View.INVISIBLE : View.GONE);
	}

	private void showNearbyList() {
		View view = getView();
		if (view == null) {
			return;
		}
		view.findViewById(R.id.poi_nearby_pois_title).setVisibility(View.VISIBLE);
		view.findViewById(R.id.poi_nearby_pois_list).setVisibility(View.VISIBLE);
	}

	private void initAdapters(IFragment activity) {
		this.adapter = new POIArrayAdapter(
				activity,
				this.sensorManager,
				this.dataSourcesRepository,
				this.defaultPrefRepository,
				this.localPreferenceRepository,
				this.poiRepository,
				this.favoriteManager,
				this.sharedStatusLoader,
				this.sharedServiceUpdateLoader
		);
		this.adapter.setLogTag(getLogTag());
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		final Resources resources = view.getContext().getResources();
		final View fragmentStatusBarBg = view.findViewById(R.id.fragment_status_bar_bg);
		if (fragmentStatusBarBg != null) {
			EdgeToEdgeKt.applyStatusBarsHeightEdgeToEdge(fragmentStatusBarBg, resources.getDimensionPixelSize(R.dimen.action_bar_size_static));
		}
		final MapView map = view.findViewById(R.id.map);
		if (map != null) {
			EdgeToEdgeKt.setUpMapEdgeToEdge(map, this.mapViewController, TOP_PADDING_SP, BOTTOM_PADDING_SP,
					resources.getDimensionPixelSize(R.dimen.large_header_height)
			);
		}
		if (this.adapter != null) {
			this.adapter.setManualScrollView(view.findViewById(R.id.scroll_view));
			this.adapter.setManualLayout(view.findViewById(R.id.poi_nearby_pois_list));
		}
		final FloatingActionButton fabFavorite = view.findViewById(R.id.fab_favorite);
		if (fabFavorite != null) {
			fabFavorite.setOnClickListener(v -> {
						POIManager poim = getPoimOrNull();
						if (poim != null && poim.isFavoritable()) {
							this.favoriteManager.addRemoveFavorite(requireActivity(), poim.poi.getUUID(), this);
						}
					}
			);
			EdgeToEdgeKt.setUpFabEdgeToEdge(
					fabFavorite,
					R.dimen.fab_auto_margin_end,
					R.dimen.fab_auto_margin_bottom
			);
		}
		updateFabFavorite();
		setupNewsLayout(view);
	}

	private void setupRDSFullScheduleBtn(View view) {
		if (view == null) {
			return;
		}
		final View rdsScheduleBtn = view.findViewById(R.id.fullScheduleBtn);
		if (rdsScheduleBtn != null) {
			final Collection<ScheduleProviderProperties> scheduleProviders = this.viewModel == null ? null : this.viewModel.getScheduleProviders().getValue();
			if (scheduleProviders == null || scheduleProviders.isEmpty()) {
				rdsScheduleBtn.setVisibility(View.GONE);
			} else {
				final POIManager poimForColor = getPoimOrNull();
				if (poimForColor != null) {
					final int poiColor = poimForColor.getColor(dataSourcesRepository);
					rdsScheduleBtn.setBackgroundColor(poiColor);
				}
				rdsScheduleBtn.setOnClickListener(v -> {
					final POIManager poim = getPoimOrNull();
					if (poim == null || !(poim.poi instanceof RouteDirectionStop)) {
						MTLog.w(POIFragment.this, "onClick() > skip (no poi or not RDS)");
						return;
					}
					poiRepository.push(poim);
					if (FeatureFlags.F_NAVIGATION) {
						final NavController navController = NavHostFragment.findNavController(this);
						FragmentNavigator.Extras extras = null;
						if (FeatureFlags.F_TRANSITION) {
							extras = new FragmentNavigator.Extras.Builder()
									// TODO button? .addSharedElement(view, view.getTransitionName())
									.build();
						}
						NavControllerExtKt.navigateF(navController,
								R.id.nav_to_schedule_screen,
								ScheduleFragment.newInstanceArgs(poim, dataSourcesRepository),
								null,
								extras
						);
					} else {
						final FragmentActivity activity = getActivity();
						if (activity == null) {
							MTLog.w(POIFragment.this, "onClick() > skip (no activity)");
							return;
						}
						((MainActivity) activity).addFragmentToStack(
								ScheduleFragment.newInstance(poim, dataSourcesRepository),
								POIFragment.this);
					}
				});
				rdsScheduleBtn.setVisibility(View.VISIBLE);
			}
		}
	}

	private void setupMoreNewsButton(View view) {
		if (view == null) {
			return;
		}
		View newsView = view.findViewById(R.id.poi_news);
		View moreBtn = newsView == null ? null : newsView.findViewById(R.id.moreBtn);
		View titleMoreLayout = newsView == null ? null : newsView.findViewById(R.id.moreSectionTitle);
		if (moreBtn == null) {
			if (titleMoreLayout != null) {
				titleMoreLayout.setClickable(false);
			}
			return;
		}
		moreBtn.setOnClickListener(v -> {
			final POIManager poim = getPoimOrNull();
			if (poim == null) {
				return;
			}
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = NavHostFragment.findNavController(this);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							// TODO button? .addSharedElement(view, view.getTransitionName())
							.build();
				}
				NavControllerExtKt.navigateF(navController,
						R.id.nav_to_news_screen,
						NewsListDetailFragment.newInstanceArgs(
								poim.getColor(dataSourcesRepository),
								POIManagerExtKt.getNewOneLineDescriptionForNews(poim.poi, POIFragment.this.dataSourcesRepository),
								Collections.singletonList(poim.poi.getAuthority()),
								NewsProviderContract.Filter.getNewTargetFilter(poim.poi).getTargets()
						),
						null,
						extras
				);
			} else {
				final FragmentActivity activity = getActivity();
				if (activity == null) {
					MTLog.w(POIFragment.this, "onClick() > skip (no activity)");
					return;
				}
				((MainActivity) activity).addFragmentToStack(
						NewsListDetailFragment.newInstance(
								poim.getColor(dataSourcesRepository),
								POIManagerExtKt.getNewOneLineDescriptionForNews(poim.poi, POIFragment.this.dataSourcesRepository),
								Collections.singletonList(poim.poi.getAuthority()),
								NewsProviderContract.Filter.getNewTargetFilter(poim.poi).getTargets()
						),
						POIFragment.this
				);
			}
		});
		if (titleMoreLayout != null) {
			titleMoreLayout.setOnClickListener(v -> moreBtn.performClick());
		}
		moreBtn.setVisibility(View.VISIBLE);
	}

	private void setupMoreNearbyButton(View view) {
		if (view == null) {
			return;
		}
		View nearbyView = view.findViewById(R.id.poi_nearby_pois_title);
		View moreBtn = nearbyView == null ? null : nearbyView.findViewById(R.id.moreBtn);
		//noinspection UnnecessaryLocalVariable
		View titleMoreLayout = nearbyView;
		if (moreBtn == null) {
			titleMoreLayout.setClickable(false);
			return;
		}
		moreBtn.setOnClickListener(v -> {
			final POIManager poim = getPoimOrNull();
			if (poim == null) {
				return;
			}
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = NavHostFragment.findNavController(this);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							// TODO button? .addSharedElement(view, view.getTransitionName())
							.build();
				}
				NavControllerExtKt.navigateF(navController,
						R.id.nav_to_nearby_screen,
						NearbyFragment.newFixedOnPOIInstanceArgs(poim, dataSourcesRepository),
						null,
						extras
				);
			} else {
				final Activity activity = getActivity();
				if (!(activity instanceof MainActivity)) {
					return;
				}
				((MainActivity) activity).addFragmentToStack(
						NearbyFragment.newFixedOnPOIInstance(poim, dataSourcesRepository),
						POIFragment.this
				);
			}
		});
		titleMoreLayout.setOnClickListener(v -> moreBtn.performClick());
		moreBtn.setVisibility(View.VISIBLE);
	}

	@Nullable
	private View getPOIStatusView(View view) {
		final POIManager poim = getPoimOrNull();
		if (view == null || poim == null) {
			return null;
		}
		if (view.findViewById(R.id.poi_status_detail) == null) { // IF NOT present/inflated DO
			final Integer layoutResId = POIStatusDetailViewController.getLayoutResId(poim);
			if (layoutResId != null) {
				((ViewStub) view.findViewById(R.id.poi_status_detail_stub)).setLayoutResource(layoutResId);
				((ViewStub) view.findViewById(R.id.poi_status_detail_stub)).inflate(); // inflate
				setupRDSFullScheduleBtn(view);
			}
		}
		return view.findViewById(R.id.poi_status_detail);
	}

	@Nullable
	private View getPOIServiceUpdateView(@Nullable View view) {
		if (view == null) {
			return null;
		}
		return view.findViewById(R.id.poi_service_update);
	}

	@Nullable
	private View getPOIAppUpdateView(@Nullable View view) {
		if (view == null) {
			return null;
		}
		return view.findViewById(R.id.poi_app_update);
	}

	private void setupAppUpdateButton(@Nullable View view) {
		if (view == null) {
			return;
		}
		view.findViewById(R.id.appUpdateBtn).setOnClickListener(v -> {
			final Activity activity = getActivity();
			if (activity == null) {
				return;
			}
			final IAgencyUpdatableProperties agency = getAgencyOrNull();
			if (agency == null) {
				return;
			}
			final String pkg = agency.getPkg();
			POIFragment.this.analyticsManager.logEvent(AnalyticsEvents.CLICK_APP_UPDATE_POI, new AnalyticsEventsParamsProvider()
					.put(AnalyticsEvents.Params.PKG, pkg)
			);
			if (agency.getUpdateAvailable()) {
				AppUpdateLauncher.launchAppUpdate(activity, pkg);
				return; // handled
			}
			StoreUtils.viewAppPage(activity, pkg, activity.getString(org.mtransit.android.commons.R.string.google_play));
		});
	}

	@Nullable
	private View getPOIAppWasDisabledView(@Nullable View view) {
		if (view == null) {
			return null;
		}
		return view.findViewById(R.id.poi_module_was_disabled);
	}

	private void setupAppWasDisabledButton(@Nullable View view) {
		if (view == null) {
			return;
		}
		view.findViewById(R.id.appWasDisabledBtn).setOnClickListener(v -> {
			final Activity activity = getActivity();
			if (activity == null) {
				return;
			}
			if (viewModel != null) {
				viewModel.onBatteryOptimizationSettingsOpened();
			}
			this.analyticsManager.logEvent(AnalyticsEvents.CLICK_APP_WAS_DISABLED_POI, null);
			BatteryOptimizationIssueUtils.openDeviceBatteryOptimizationSettings(activity);
		});
	}

	@Nullable
	private View getPOINewsView(@Nullable View view) {
		if (view == null) {
			return null;
		}
		return view.findViewById(R.id.poi_news);
	}

	@Nullable
	private View getPOIRewardedAdView(@Nullable View view) {
		if (view == null) {
			return null;
		}
		return view.findViewById(R.id.poi_rewarded_ad);
	}

	private void setupRewardedAdButton(@Nullable View view) {
		if (view == null) {
			return;
		}
		view.findViewById(R.id.rewardedAdsBtn).setOnClickListener(v ->
				onRewardedAdButtonClick(view.getContext())
		);
	}

	private void onRewardedAdButtonClick(@NonNull Context context) {
		final Activity activity = getActivity();
		if (activity == null) {
			MTLog.w(this, "onRewardedAdButtonClick() > skip (no view or no activity)");
			ToastUtils.makeTextAndShowCentered(context, R.string.support_watch_rewarded_ad_default_failure_message);
			return;
		}
		if (!this.adManager.isRewardedAdAvailableToShow()) {
			MTLog.w(this, "onRewardedAdButtonClick() > skip (no ad available)");
			ToastUtils.makeTextAndShowCentered(context, R.string.support_watch_rewarded_ad_not_ready);
			return;
		}
		final View view = getView();
		if (view != null) {
			view.findViewById(R.id.rewardedAdsBtn).setEnabled(false);
		}
		this.adManager.showRewardedAd((IAdScreenActivity) activity);
	}

	@Nullable
	private View getPOIView(@Nullable View view) {
		final POIManager poim = getPoimOrNull();
		if (view == null || poim == null) {
			return null;
		}
		if (view.findViewById(R.id.this_poi) == null) { // IF NOT present/inflated DO
			final int layoutResId = POIViewController.getLayoutResId(poim);
			((ViewStub) view.findViewById(R.id.this_poi_stub)).setLayoutResource(layoutResId);
			((ViewStub) view.findViewById(R.id.this_poi_stub)).inflate(); // inflate
		}
		return view.findViewById(R.id.this_poi);
	}

	@Override
	public void onStatusLoaded(@NonNull POIStatus status) {
		final View view = getView();
		if (view == null) {
			return;
		}
		POIViewController.updatePOIStatus(getPOIView(view), status, this);
		POIStatusDetailViewController.updatePOIStatus(getPOIStatusView(view), status, this, getPoimOrNull());
	}

	@Override
	public void onServiceUpdatesLoaded(@NonNull String targetUUID, @Nullable List<ServiceUpdate> serviceUpdates) {
		final View view = getView();
		if (view == null) {
			return;
		}
		POIViewController.updateServiceUpdatesView(getPOIView(view), serviceUpdates, this);
		POIServiceUpdateViewController.updateServiceUpdate(getPOIServiceUpdateView(view), serviceUpdates, this);
	}

	@Override
	public boolean onURLClick(@NonNull View view, @NonNull String url) {
		return LinkUtils.open(view, requireActivity(), url, getString(org.mtransit.android.commons.R.string.web_browser), true);
	}

	@Nullable
	protected Location deviceLocation;

	@Nullable
	@Override
	public Location getDeviceLocation() {
		return this.deviceLocation;
	}

	@Override
	public void onLocationSettingsResolution(@Nullable PendingIntent resolution) {
	}

	@Override
	public void onDeviceLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		final Context context = getContext();
		if (this.deviceLocation == null && context != null) {
			this.mapViewController.setLocationPermissionGranted(this.locationPermissionProvider.allRequiredPermissionsGranted(context));
		}
		if (this.deviceLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.deviceLocation, newLocation)) {
			this.deviceLocation = newLocation;
			this.locationDeclination = sensorManager.getLocationDeclination(newLocation);
			if (!this.compassUpdatesEnabled) {
				sensorManager.registerCompassListener(this);
				this.compassUpdatesEnabled = true;
			}
			final POIManager poim = getPoimOrNull();
			if (poim != null) {
				LocationUtils.updateDistanceWithString(requireContext(), poim, newLocation);
				POIViewController.updatePOIDistanceAndCompass(getPOIView(getView()), poim, this);
			}
			this.mapViewController.onDeviceLocationChanged(this.deviceLocation);
			if (this.adapter != null) {
				this.adapter.setLocation(newLocation);
			}
		}
	}

	private boolean compassUpdatesEnabled = false;

	@Override
	public void onAccuracyChanged(@NonNull Sensor sensor, int accuracy) {
		// DO NOTHING
	}

	private final float[] accelerometerValues = new float[3];

	private final float[] magneticFieldValues = new float[3];

	@Override
	public void onSensorChanged(@NonNull SensorEvent se) {
		sensorManager.checkForCompass(this, se, this.accelerometerValues, this.magneticFieldValues, this);
	}

	private long lastCompassChanged = -1L;

	@Override
	public void updateCompass(float orientation, boolean force) {
		final long now = UITimeUtils.currentTimeMillis();
		final int roundedOrientation = DegreeUtils.convertToPositive360Degree((int) orientation);
		sensorManager.updateCompass(
				force,
				this.deviceLocation,
				roundedOrientation,
				now,
				AbsListView.OnScrollListener.SCROLL_STATE_IDLE,
				this.lastCompassChanged,
				this.lastCompassInDegree,
				Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS,
				this
		);
	}

	@Override
	public void onSensorTaskCompleted(boolean result, int roundedOrientation, long now) {
		if (!result) {
			return;
		}
		this.lastCompassInDegree = roundedOrientation;
		this.lastCompassChanged = now;
		if (!this.compassUpdatesEnabled || this.deviceLocation == null || this.lastCompassInDegree < 0) {
			return;
		}
		final POIManager poim = getPoimOrNull();
		if (poim != null) {
			POIViewController.updatePOIDistanceAndCompass(getPOIView(getView()), poim, this);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		resetFavorite(); // force refresh
		getFavoriteFolderId();
		enableTimeChangedReceiver();
		this.showingAccessibilityInfo = null; // force user preference check
		this.mapViewController.onResume();
		if (this.adapter != null) {
			this.adapter.onResume(this, this.deviceLocation);
		}
		if (this.newsListAdapter != null) {
			this.newsListAdapter.onResume(this);
		}
		final POIManager poim = getPoimOrNull();
		if (poim != null) {
			if (!FeatureFlags.F_EXPORT_TRIP_ID) {
				this.mapViewController.notifyMarkerChanged(this);
				this.mapViewController.showMap(view);
			}
			POIViewController.updateView(getPOIView(view), poim, this);
			POIStatusDetailViewController.updateView(getPOIStatusView(view), poim, this);
			POIServiceUpdateViewController.updateView(getPOIServiceUpdateView(view), poim, this);
			setupRDSFullScheduleBtn(view);
			setupMoreNewsButton(view);
			setupAppUpdateButton(view);
			setupAppWasDisabledButton(view);
			setupRewardedAdButton(view);
			setupMoreNearbyButton(view);
			setupNearbyList();
		}
		onDeviceLocationChanged(((MTActivityWithLocation) requireActivity()).getDeviceLocation());
		this.adManager.setRewardedAdListener(this);
		this.adManager.refreshRewardedAdStatus((IActivity) requireActivity());
		refreshRewardedLayout(getView());
		refreshAppUpdateLayout(getView());
		if (this.viewModel != null) {
			this.viewModel.refreshAppUpdateAvailable();
			// this.viewModel.onResumeScreen(this);
			this.viewModel.startVehicleLocationRefresh();
		}
		refreshAppWasDisabledLayout(getView());
		if (FeatureFlags.F_NAVIGATION) {
			if (nextMainViewModel != null) {
				nextMainViewModel.setABBgColor(getABBgColor(getContext()));
			}
		}
	}

	@Override
	public boolean skipRewardedAd() {
		return this.adManager.shouldSkipRewardedAd();
	}

	@Override
	public void onRewardedAdStatusChanged() {
		View view = getView();
		if (view != null) {
			refreshRewardedLayout(view);
		}
	}

	@NonNull
	private final ThreadSafeDateFormatter rewardedAdDateFormatter = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM);

	private void refreshRewardedLayout(@Nullable View view) {
		final View rewardedLayout = getPOIRewardedAdView(view);
		if (rewardedLayout == null) {
			return;
		}
		final TextView rewardedAdTitleTv = rewardedLayout.findViewById(R.id.rewardAdTitle);
		final TextView rewardedAdsBtn = rewardedLayout.findViewById(R.id.rewardedAdsBtn);

		final boolean availableToShow = this.adManager.isRewardedAdAvailableToShow();
		final boolean rewardedNow = this.adManager.isRewardedNow();
		final long rewardedUntilInMs = this.adManager.getRewardedUntilInMs();
		final int rewardedAmount = this.adManager.getRewardedAdAmount();

		rewardedLayout.setVisibility(availableToShow ? View.VISIBLE : View.GONE);

		if (rewardedNow) {
			rewardedAdTitleTv.setText(getString(
					R.string.watch_rewarded_ad_title_text_and_date,
					this.rewardedAdDateFormatter.formatThreadSafe(rewardedUntilInMs)
			));
		} else {
			rewardedAdTitleTv.setText(getString(
					R.string.watch_rewarded_ad_title_text
			));
		}
		rewardedAdTitleTv.setVisibility(View.VISIBLE);

		rewardedAdsBtn.setText(getResources().getQuantityString(
				rewardedNow ?
						R.plurals.watch_rewarded_ad_btn_more_and_days :
						R.plurals.watch_rewarded_ad_btn_and_days,
				rewardedAmount,
				rewardedAmount
		));
		if (availableToShow) { // only if NOT paying user
			rewardedAdsBtn.setEnabled(true);
			rewardedAdsBtn.setVisibility(View.VISIBLE);
		} else {
			rewardedAdsBtn.setEnabled(false); // keep but disable
		}
	}

	private void refreshAppUpdateLayout(@Nullable View view) {
		final View appUpdateLayout = getPOIAppUpdateView(view);
		if (appUpdateLayout == null) {
			MTLog.d(this, "refreshAppUpdateLayout() > SKIP (no layout)");
			return;
		}
		final IAgencyUpdatableProperties agency = getAgencyOrNull();
		boolean appUpdateAvailable = agency != null && agency.getUpdateAvailable()
				&& agency.shouldShowUpdateLayout();
		if (demoModeManager.isFullDemo()) {
			appUpdateAvailable = false; // always false (demo mode ON)
		}
		final String pkg = agency == null ? "" : agency.getPkg();
		if (appUpdateAvailable) {
			if (appUpdateLayout.getVisibility() != View.VISIBLE) {
				appUpdateLayout.setVisibility(View.VISIBLE);
				this.analyticsManager.logEvent(AnalyticsEvents.SHOWED_APP_UPDATE_POI, new AnalyticsEventsParamsProvider()
						.put(AnalyticsEvents.Params.PKG, pkg)
				);
			}
		} else {
			if (appUpdateLayout.getVisibility() != View.GONE) {
				appUpdateLayout.setVisibility(View.GONE);
				this.analyticsManager.logEvent(AnalyticsEvents.HIDDEN_APP_UPDATE_POI, new AnalyticsEventsParamsProvider()
						.put(AnalyticsEvents.Params.PKG, pkg)
				);
			}
		}
	}

	private void refreshAppWasDisabledLayout(@Nullable View view) {
		final View appWasDisabledLayout = getPOIAppWasDisabledView(view);
		if (appWasDisabledLayout == null) {
			MTLog.d(this, "refreshAppWasDisabledLayout() > SKIP (no layout)");
			return;
		}
		if (!BatteryOptimizationIssueUtils.isSamsungDevice()) {
			MTLog.d(this, "refreshAppWasDisabledLayout() > SKIP (not Samsung)");
			return;
		}
		final IAgencyUpdatableProperties agency = getAgencyOrNull();
		boolean appUpdateAvailable = agency != null && agency.getUpdateAvailable();
		boolean appWasDisabled = this.viewModel != null && this.viewModel.hasSeenDisabledModule();
		if (appUpdateAvailable) {
			appWasDisabled = false; // avoid too many messages
		} else if (demoModeManager.isFullDemo()) {
			appWasDisabled = false; // always false (demo mode ON)
		}
		if (appWasDisabled) {
			final Resources resources = appWasDisabledLayout.getContext().getResources();
			final TextView appWasDisabledTitleTv = appWasDisabledLayout.findViewById(R.id.appWasDisabledTitle);
			final TextView appWasDisabledBtn = appWasDisabledLayout.findViewById(R.id.appWasDisabledBtn);
			final int agencyCount = this.dataSourcesRepository.getAllAgenciesCount();
			appWasDisabledTitleTv.setText(resources.getQuantityString(R.plurals.app_disabled_issue_title, agencyCount, agencyCount));
			appWasDisabledBtn.setText(R.string.battery_optimization_samsung_use_device_care);
			if (appWasDisabledLayout.getVisibility() != View.VISIBLE) {
				appWasDisabledLayout.setVisibility(View.VISIBLE);
				this.analyticsManager.logEvent(AnalyticsEvents.SHOWED_APP_WAS_DISABLED_POI, null);
			}
		} else {
			if (appWasDisabledLayout.getVisibility() != View.GONE) {
				appWasDisabledLayout.setVisibility(View.GONE);
				this.analyticsManager.logEvent(AnalyticsEvents.HIDDEN_APP_WAS_DISABLED_POI, null);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.compassUpdatesEnabled) {
			sensorManager.unregisterSensorListener(this);
			this.compassUpdatesEnabled = false;
		}
		disableTimeChangedReceiver();
		this.mapViewController.onPause();
		if (this.viewModel != null) {
			this.viewModel.stopVehicleLocationRefresh();
		}
		if (this.adapter != null) {
			this.adapter.onPause();
		}
		if (this.newsListAdapter != null) {
			this.newsListAdapter.onPause(this);
		}
		this.adManager.setRewardedAdListener(null);
	}

	private long nowToTheMinute = -1L;

	@Override
	public long getNowToTheMinute() {
		if (this.nowToTheMinute < 0L) {
			resetNowToTheMinute();
			enableTimeChangedReceiver();
		}
		return this.nowToTheMinute;
	}

	@Override
	public void onTimeChanged() {
		resetNowToTheMinute();
	}

	private void resetNowToTheMinute() {
		MTLog.i(this, "Refreshing UI data...");
		this.nowToTheMinute = UITimeUtils.currentTimeToTheMinuteMillis();
		final View view = getView();
		final POIManager poim = getPoimOrNull();
		if (poim != null) {
			final View poiView = getPOIView(view);
			POIViewController.updatePOIStatus(poiView, poim, this);
			POIViewController.updatePOIServiceUpdate(poiView, poim, this);
			POIStatusDetailViewController.updateView(getPOIStatusView(view), poim, this);
			POIServiceUpdateViewController.updateView(getPOIServiceUpdateView(view), poim, this);
		}
	}

	private boolean timeChangedReceiverEnabled = false;

	private void enableTimeChangedReceiver() {
		if (!this.timeChangedReceiverEnabled) {
			if (getContext() != null) {
				ContextCompat.registerReceiver(getContext(), timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER, ContextCompat.RECEIVER_NOT_EXPORTED);
				this.timeChangedReceiverEnabled = true;
			}
		}
	}

	private void disableTimeChangedReceiver() {
		if (this.timeChangedReceiverEnabled) {
			if (getContext() != null) {
				getContext().unregisterReceiver(this.timeChangedReceiver);
				this.timeChangedReceiverEnabled = false;
				this.nowToTheMinute = -1L;
			}
		}
	}

	private final UITimeUtils.TimeChangedReceiver timeChangedReceiver = new UITimeUtils.TimeChangedReceiver(this);

	@Override
	public boolean hasLocation() {
		return this.deviceLocation != null;
	}

	@Nullable
	private Integer lastCompassInDegree = null;

	@Nullable
	@Override
	public Integer getLastCompassInDegree() {
		return lastCompassInDegree;
	}

	@Override
	public boolean hasLastCompassInDegree() {
		return lastCompassInDegree != null;
	}

	@Nullable
	private Float locationDeclination = null;

	@Nullable
	@Override
	public Float getLocationDeclination() {
		return this.locationDeclination;
	}

	@Override
	public boolean isClosestPOI(@NonNull String uuid) {
		return false;
	}

	@Nullable
	private Integer favoriteFolderId = null;

	@Nullable
	private Integer getFavoriteFolderId() {
		if (this.favoriteFolderId == null) {
			POIManager poim = getPoimOrNull();
			if (poim != null) {
				this.favoriteFolderId = this.favoriteManager.findFavoriteFolderId(requireContext(), poim.poi.getUUID());
			}
		}
		return this.favoriteFolderId;
	}

	public boolean isFavorite() {
		if (this.favoriteFolderId == null) {
			POIManager poim = getPoimOrNull();
			if (poim != null) {
				this.favoriteFolderId = this.favoriteManager.findFavoriteFolderId(requireContext(), poim.poi.getUUID());
			}
		}
		return getFavoriteFolderId() != null && getFavoriteFolderId() >= 0;
	}

	private void resetFavorite() {
		this.favoriteFolderId = null;
	}

	@Override
	public boolean isFavorite(@NonNull String uuid) {
		POIManager poim = getPoimOrNull();
		if (poim != null && poim.poi.getUUID().equals(uuid)) {
			return isFavorite();
		}
		return this.favoriteManager.isFavorite(requireContext(), uuid);
	}

	@Override
	public boolean isShowingFavorite() {
		return false; // shown in the action bar
	}

	@Override
	public boolean isShowingStatus() {
		return true;
	}

	@Override
	public boolean isShowingExtra() {
		return true;
	}

	@Override
	public boolean isShowingServiceUpdates() {
		return true;
	}

	@NonNull
	@Override
	public DataSourcesRepository providesDataSourcesRepository() {
		return this.dataSourcesRepository;
	}

	@NonNull
	@Override
	public StatusLoader providesStatusLoader() {
		return this.sharedStatusLoader;
	}

	@NonNull
	@Override
	public ServiceUpdateLoader providesServiceUpdateLoader() {
		return this.sharedServiceUpdateLoader;
	}

	@Override
	public @Nullable Collection<String> getIgnoredTargetUUIDsOrUnknown() {
		return Collections.emptyList(); // empty = ready = no filter
	}

	@NonNull
	@Override
	public DemoModeManager providesDemoModeManager() {
		return this.demoModeManager;
	}

	@Nullable
	private Boolean showingAccessibilityInfo = null;

	@Override
	public boolean isShowingAccessibilityInfo() {
		if (this.showingAccessibilityInfo == null) {
			this.showingAccessibilityInfo = this.defaultPrefRepository.getValue(DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY, DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY_DEFAULT);
		}
		return this.showingAccessibilityInfo;
	}

	@Nullable
	private MenuItem showFareMenuItem;

	@Override
	public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
		menuInflater.inflate(R.menu.menu_poi, menu);
		this.showFareMenuItem = menu.findItem(R.id.menu_show_fares);
		updateFaresMenuItem();
	}

	private void updateFabFavorite() {
		final View view = getView();
		final FloatingActionButton fabFavorite = view == null ? null : view.findViewById(R.id.fab_favorite);
		if (fabFavorite == null) {
			return;
		}
		final POIManager poim = getPoimOrNull();
		if (poim != null && poim.isFavoritable()) {
			final boolean isFav = isFavorite();
			@DrawableRes int iconResId;
			@StringRes int contentDescriptionResId;
			if (isFav) {
				iconResId = R.drawable.ic_star_black_24dp;
				if (this.favoriteManager.isUsingFavoriteFolders()) {
					contentDescriptionResId = R.string.menu_action_edit_favorite;
				} else {
					contentDescriptionResId = R.string.menu_action_remove_favorite;
				}
			} else {
				iconResId = R.drawable.ic_star_border_black_24dp;
				contentDescriptionResId = R.string.menu_action_add_favorite;
			}
			fabFavorite.setImageResource(iconResId);
			fabFavorite.setContentDescription(getString(contentDescriptionResId));
			final int poiColor = poim.getColor(dataSourcesRepository);
			fabFavorite.setBackgroundTintList(ColorStateList.valueOf(poiColor));
			fabFavorite.setRippleColor(poiColor);
			fabFavorite.show();
		} else {
			fabFavorite.hide();
		}
	}

	private void updateFaresMenuItem() {
		if (this.showFareMenuItem == null) {
			return;
		}
		final AgencyProperties agency = getAgencyOrNull();
		final String faresWebUrl = agency == null ? null : agency.getFaresWebForLang();
		this.showFareMenuItem.setVisible(!TextUtils.isEmpty(faresWebUrl));
	}

	@Override
	public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
		if (menuItem.getItemId() == R.id.menu_show_fares) {
			final AgencyProperties agency = getAgencyOrNull();
			final String faresWebUrl = agency == null ? null : agency.getFaresWebForLang();
			if (!TextUtils.isEmpty(faresWebUrl)) {
				LinkUtils.open(getView(), requireActivity(), faresWebUrl, getString(R.string.fares), null, true);
				return true; // handled
			}
		} else if (menuItem.getItemId() == R.id.menu_show_directions) {
			POIManager poim2 = getPoimOrNull();
			if (poim2 != null) {
				this.analyticsManager.logEvent(AnalyticsEvents.OPENED_GOOGLE_MAPS_TRIP_PLANNER);
				Double optSrcLat = null;
				Double optSrcLng = null;
				if (this.deviceLocation != null) {
					optSrcLat = this.deviceLocation.getLatitude();
					optSrcLng = this.deviceLocation.getLongitude();
				}
				MapUtils.showDirection(getView(), requireActivity(), poim2.poi.getLat(), poim2.poi.getLng(), optSrcLat, optSrcLng, poim2.poi.getName());
				return true; // handled
			}
		}
		return false; // not handled
	}

	@Override
	public void onFavoriteUpdated() {
		resetFavorite();
		updateFabFavorite();
		final POIManager poim = getPoimOrNull();
		if (poim != null) {
			POIViewController.updateView(getPOIView(getView()), poim, this);
		}
	}

	@Override
	public boolean isABReady() {
		return getAgencyOrNull() != null;
	}

	@Nullable
	@Override
	public CharSequence getABTitle(@Nullable Context context) {
		final AgencyProperties agency = getAgencyOrNull();
		if (agency != null) {
			return agency.getShortName();
		}
		if (context == null) {
			return super.getABTitle(null);
		}
		return context.getString(org.mtransit.android.commons.R.string.ellipsis);
	}

	@Override
	public boolean isABStatusBarTransparent() {
		return true;
	}

	@Override
	public boolean isABOverrideGradient() {
		return UIFeatureFlags.F_EDGE_TO_EDGE;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		this.mapViewController.onLowMemory();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (this.adapter != null) {
			this.adapter.onDestroyView();
		}
		this.mapViewController.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.mapViewController.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
			this.adapter = null;
		}
		if (this.newsListAdapter != null) {
			this.newsListAdapter.onDestroy(this);
			this.newsListAdapter = null;
		}
	}
}
