package org.mtransit.android.ui.fragment;

import static org.mtransit.android.ui.fragment.POIFragmentExtKt.makePoiListFooterManager;
import static org.mtransit.android.ui.fragment.POIFragmentExtKt.onResumeKt;
import static org.mtransit.android.ui.fragment.POIFragmentExtKt.setupViewKt;
import static org.mtransit.android.ui.fragment.POIFragmentExtKt.startVehicleLocationCountdownRefresh;
import static org.mtransit.android.ui.fragment.POIFragmentExtKt.stopVehicleLocationCountdownRefresh;
import static org.mtransit.android.ui.fragment.POIFragmentExtKt.updateFooter;

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
import android.widget.AbsListView;
import android.widget.TextView;

import androidx.annotation.AnyThread;
import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.mtransit.android.R;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.ad.IAdScreenActivity;
import org.mtransit.android.analytics.AnalyticsEvents;
import org.mtransit.android.analytics.AnalyticsEventsParamsProvider;
import org.mtransit.android.analytics.AnalyticsScreen;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.billing.IBillingManager;
import org.mtransit.android.common.IContext;
import org.mtransit.android.common.repository.DefaultPreferenceRepository;
import org.mtransit.android.common.repository.LocalPreferenceRepository;
import org.mtransit.android.commons.AppUpdateLauncher;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.data.Area;
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
import org.mtransit.android.data.POIListFooterManager;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.POIManagerExtKt;
import org.mtransit.android.databinding.FragmentPoiBinding;
import org.mtransit.android.databinding.LayoutPoiAppUpdateBinding;
import org.mtransit.android.databinding.LayoutPoiAppWasDisabledBinding;
import org.mtransit.android.databinding.LayoutPoiDetailStatusScheduleBinding;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.datasource.POIRepository;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.provider.FavoriteRepository;
import org.mtransit.android.provider.permission.LocationPermissionProvider;
import org.mtransit.android.provider.sensor.MTSensorManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.EdgeToEdgeKt;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.location.UILocationUtils;
import org.mtransit.android.ui.main.NextMainViewModel;
import org.mtransit.android.ui.nearby.NearbyFragment;
import org.mtransit.android.ui.news.NewsListAdapter;
import org.mtransit.android.ui.news.NewsListDetailFragment;
import org.mtransit.android.ui.schedule.ScheduleFragment;
import org.mtransit.android.ui.serviceupdates.ServiceUpdatesDialog;
import org.mtransit.android.ui.view.listfooter.DefaultPOIListFooterManager;
import org.mtransit.android.ui.view.MapViewController;
import org.mtransit.android.ui.view.MapViewControllerExtKt;
import org.mtransit.android.ui.view.POIDataProvider;
import org.mtransit.android.ui.view.POIServiceUpdateViewController;
import org.mtransit.android.ui.view.POIStatusDetailViewController;
import org.mtransit.android.ui.view.POIViewController;
import org.mtransit.android.ui.view.common.EventObserver;
import org.mtransit.android.ui.view.common.FragmentKtxKt;
import org.mtransit.android.ui.view.common.IFragment;
import org.mtransit.android.ui.view.common.ImageManager;
import org.mtransit.android.ui.view.common.MTTransitions;
import org.mtransit.android.ui.view.common.NavControllerExtKt;
import org.mtransit.android.ui.view.map.IMarker;
import org.mtransit.android.ui.view.map.MTPOIMarker;
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
		UITimeUtils.TimeChangedReceiver.TimeChangedListener,
		IContext,
		IAdManager.RewardedAdListener,
		MenuProvider,
		MapViewController.MapMarkerProvider,
		MapViewController.MapListener {

	private static final String LOG_TAG = POIFragment.class.getSimpleName();

	private static final int SERVICE_UPDATE_MAX_LINES = 5;

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
	public static POIFragment newInstance(@NonNull String authority, @NonNull String uuid) {
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
	protected POIViewModel getAttachedViewModel() {
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
	FavoriteRepository favoriteRepository;
	@Inject
	IBillingManager billingManager;
	@Inject
	DemoModeManager demoModeManager;
	@Inject
	DefaultPreferenceRepository defaultPrefRepository;
	@Inject
	LocalPreferenceRepository lclPrefRepository;
	@Inject
	ImageManager imageManager;

	private static final int TOP_PADDING_DP = 64 - 32;
	private static final int BOTTOM_PADDING_DP = 0;

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
					TOP_PADDING_DP,
					BOTTOM_PADDING_DP,
					false, // manually set
					false,
					false,
					true,
					false
			);

	private void setupMapViewController() {
		this.mapViewController.setHideMapMarkerSnippet(true);
	}

	private void onAgencyLoaded(@Nullable AgencyProperties agency) {
		applyNewAgency();
	}

	private void applyNewAgency() {
		if (getAgencyOrNull() == null) return;
		final Context context = getContext();
		if (context == null) return;
		final POIManager poim = getPoimOrNull();
		if (poim != null) {
			POIStatusDetailViewController.updateView(getPOIStatusView(), poim, this);
		}
		final ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setABTitle(this, getABTitle(context), false);
			abController.setABReady(this, isABReady(), true);
		}
		final Toolbar screenToolbar = binding == null ? null : binding.screenToolbarLayout.screenToolbar;
		if (screenToolbar != null) {
			updateScreenToolbarTitle(screenToolbar);
		}
		if (FeatureFlags.F_NAVIGATION) {
			if (nextMainViewModel != null) {
				nextMainViewModel.setABTitle(getABTitle(context));
			}
		}
		refreshAppUpdateLayout();
		if (viewModel != null) {
			viewModel.refreshAppUpdateAvailable();
		}
		if (getActivity() != null) {
			getActivity().invalidateOptionsMenu(); // update menu_show_fares menu item
		}
	}

	@Nullable
	protected AgencyProperties getAgencyOrNull() {
		return getAttachedViewModel() == null ? null : getAttachedViewModel().getAgency().getValue();
	}

	@Nullable
	protected POIManager poim; // kept for now because it can be updated anywhere #GodObject

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean hasPoim() {
		return this.poim != null;
	}

	@Nullable
	protected POIManager getPoimOrNull() {
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
		if (this.poim != null && this.poim.poi.equals(newPOIM.poi)) {
			MTLog.d(this, "onPOIMLoaded() > SKIP (same POI)");
			MTTransitions.startPostponedEnterTransitionOnPreDraw(this.binding, this);
			return; // SKIP
		}
		this.poim = newPOIM;
		applyNewPoim();
		MTTransitions.startPostponedEnterTransitionOnPreDraw(this.binding, this);
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
		updateDistanceString();
		if (this.nearbyListAdapter != null) {
			this.nearbyListAdapter.clear();
		}
		this.mapViewController.setFocusedOnUUID(this.poim.getPOI().getUUID());
		POIViewController.updateView(getPOIView(), this.poim, this);
		POIStatusDetailViewController.updateView(getPOIStatusView(), this.poim, this);
		POIServiceUpdateViewController.updateView(getPOIServiceUpdateView(), this.poim, this);
		setupRDSFullScheduleBtn();
		setupMoreNewsButton();
		setupAppUpdateButton();
		setupAppWasDisabledButton();
		setupMoreNearbyButton();
		setupNearbyList();
	}

	@Nullable
	private String distanceUnitPref = null;

	private void onDistanceUnitPrefLoaded(@Nullable String distanceUnitPref) {
		this.distanceUnitPref = distanceUnitPref;
		updateDistanceString();
	}

	private void updateDistanceString() {
		if (this.distanceUnitPref == null) return;
		if (this.deviceLocation == null) return;
		if (this.poim == null) return;
		UILocationUtils.updateDistanceWithStringNN(this.distanceUnitPref, this.poim, this.deviceLocation);
	}

	private void onNewsLoaded(@Nullable List<News> news) {
		if (news == null) {
			MTLog.d(this, "onNewsLoaded() > SKIP (no news)");
			return;
		}
		final View poiNewsView = this.binding == null ? null : this.binding.poiNews.getRoot();
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
		return viewModel == null ? null : viewModel.getPoiList().getValue();
	}

	@Nullable
	@Override
	public POIManager getPOI(int position) {
		return POIFragmentExtKt.getPOI(this, position);
	}

	@Nullable
	@Override
	public Collection<MTPOIMarker> getPOMarkers() {
		return null;
	}

	@Nullable
	@Override
	public Collection<VehicleLocation> getVehicleLocations() {
		if (UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) {
			return viewModel == null ? null : viewModel.getVehicleLocations().getValue();
		}
		return null;
	}

	@Nullable
	@Override
	public Integer getVehicleColorInt() {
		final POIManager poim = getPoimOrNull();
		if (poim == null) return null;
		return poim.getColor(dataSourcesRepository);
	}

	@Nullable
	@Override
	public DataSourceType getVehicleType() {
		final IAgencyProperties agency = getAgencyOrNull();
		if (agency == null) return null;
		return agency.getType();
	}

	@Nullable
	@Override
	public Collection<LatLng> getVisibleMarkersLocations() {
		return POIFragmentExtKt.getVisibleMarkersLocationList(this);
	}

	@Nullable
	@Override
	public Float getMapMarkerAlpha(int position, @NonNull Area visibleArea) {
		return POIFragmentExtKt.getMapMarkerAlpha(this, position, visibleArea);
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.mapViewController.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCameraChanged(@NonNull LatLngBounds latLngBounds, float zoom) {
		// DO NOTHING
	}

	@Override
	public void onMapReady() {
		// DO NOTHING
	}

	@Override
	public boolean onMarkerClick(@Nullable IMarker marker) {
		return POIFragmentExtKt.onMapClick(this);
	}

	@Override
	public void onMapClick(@NonNull LatLng position) {
		POIFragmentExtKt.onMapClick(this);
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		initAdapters(this);
		setupMapViewController();
		this.mapViewController.setDI(this.dataSourcesRepository, this.lclPrefRepository);
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

	protected @Nullable FragmentPoiBinding binding = null;

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		this.binding = FragmentPoiBinding.bind(view);
		setupView();
		MTTransitions.postponeEnterTransition(this);
		if (FeatureFlags.F_NAVIGATION) {
			nextMainViewModel = new ViewModelProvider(requireActivity()).get(NextMainViewModel.class);
		}
		viewModel = new ViewModelProvider(this).get(POIViewModel.class);
		if (this.nearbyListAdapter != null) {
			this.nearbyListAdapter.onCreateView(getViewLifecycleOwner());
		}
		viewModel.getDataSourceRemovedEvent().observe(getViewLifecycleOwner(), new EventObserver<>(removed -> {
			if (removed) {
				onDataSourceRemoved();
			}
			return null;
		}));
		viewModel.getAgency().observe(getViewLifecycleOwner(), this::onAgencyLoaded);
		viewModel.getPoim().observe(getViewLifecycleOwner(), this::onPOIMLoaded);
		viewModel.getDistanceUnitsPref().observe(getViewLifecycleOwner(), this::onDistanceUnitPrefLoaded);
		viewModel.getUseInternalWebBrowserPref().observe(getViewLifecycleOwner(), this::onUseInternalWebBrowserPrefLoaded);
		viewModel.getHasScheduleProviders().observe(getViewLifecycleOwner(), hasScheduleProviders -> setupRDSFullScheduleBtn());
		viewModel.getNearbyPOIs().observe(getViewLifecycleOwner(), this::onNearbyPOIsLoaded);
		viewModel.getLatestNewsArticleList().observe(getViewLifecycleOwner(), this::onNewsLoaded);
		viewModel.getPoiList().observe(getViewLifecycleOwner(), this::onPOIsLoaded);
		viewModel.isFavorite().observe(getViewLifecycleOwner(), this::onFavoriteLoaded);
		viewModel.getUsingFavoriteFolders().observe(getViewLifecycleOwner(), this::onUsingFavoriteFoldersLoaded);
		viewModel.getHasSeenDisabledModule().observe(getViewLifecycleOwner(), this::onHasSeenDisabledModuleLoaded);
		if (UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) {
			viewModel.getVehicleLocations().observe(getViewLifecycleOwner(), this::onVehicleLocationsLoaded);
		}
		this.adManager.getRewardedUntilLive().observe(getViewLifecycleOwner(), rewardedUntil -> refreshRewardedLayout());
		this.adManager.getRewardedNowLive().observe(getViewLifecycleOwner(), rewardedNow -> refreshRewardedLayout());
		DefaultPOIListFooterManager.observe(getViewLifecycleOwner(), viewModel.getNearbyPOIs(), this.billingManager, this.dataSourcesRepository, () -> {
			updateFooter(this);
			return kotlin.Unit.INSTANCE;
		});
		this.mapViewController.onViewCreated(view, savedInstanceState);
	}

	@Nullable
	protected Job _vehicleLocationCountdownRefreshJob = null;

	private void onVehicleLocationsLoaded(@Nullable Collection<VehicleLocation> vehicleLocations) {
		if (!UIFeatureFlags.F_CONSUME_VEHICLE_LOCATION) return;
		final Context context = getContext();
		if (context != null) {
			final String closestVehicleLocationUuid = POIFragmentExtKt.getClosestVehicleLocationUuid(this, vehicleLocations);
			MapViewControllerExtKt.updateVehicleLocationMarkers(this.mapViewController, context, closestVehicleLocationUuid, this, vehicleLocations);
		}
		this.mapViewController.showMarkers(true, false);
		if (vehicleLocations == null || vehicleLocations.isEmpty()) {
			stopVehicleLocationCountdownRefresh(this);
		} else {
			startVehicleLocationCountdownRefresh(this, vehicleLocations);
		}
	}

	private void onPOIsLoaded(@Nullable List<POIManager> poiList) {
		this.mapViewController.notifyMarkerChanged(this);
		this.mapViewController.showMap(this.binding == null ? null : this.binding.getRoot());
	}

	private void setupNewsLayout() {
		if (this.binding == null) return;
		final RecyclerView newsList = this.binding.poiNews.newsList;
		final PagerSnapHelper snapHelper = new PagerSnapHelper();
		snapHelper.attachToRecyclerView(newsList);
		newsList.setAdapter(getNewsListAdapter());
		setupMoreNewsButton();
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
											NewsProviderContract.Filter.makeTargets(poim.poi),
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
												NewsProviderContract.Filter.makeTargets(poim.poi),
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
	private POIArrayAdapter nearbyListAdapter;

	private void setupNearbyList() {
		if (this.nearbyListAdapter != null && this.nearbyListAdapter.isInitialized()) {
			showNearbyList();
			return;
		}
		hideNearbyList(true);
	}

	private void onNearbyPOIsLoaded(@Nullable List<POIManager> nearbyPOIs) {
		if (this.nearbyListAdapter == null) return;
		this.nearbyListAdapter.setPois(nearbyPOIs);
		this.nearbyListAdapter.updateDistanceNowAsync(this.deviceLocation);
		this.nearbyListAdapter.initManual();
		if (this.nearbyListAdapter.getPoisCount() > 0) {
			showNearbyList();
		} else {
			hideNearbyList(false);
		}
	}

	private void hideNearbyList(boolean invisibleInsteadOfGone) {
		if (this.binding == null) return;
		this.binding.poiNearbyPoisTitle.getRoot().setVisibility(invisibleInsteadOfGone ? View.INVISIBLE : View.GONE);
		this.binding.poiNearbyPoisList.setVisibility(invisibleInsteadOfGone ? View.INVISIBLE : View.GONE);
	}

	private void showNearbyList() {
		if (this.binding == null) return;
		this.binding.poiNearbyPoisTitle.getRoot().setVisibility(View.VISIBLE);
		this.binding.poiNearbyPoisList.setVisibility(View.VISIBLE);
	}

	private void initAdapters(IFragment activity) {
		this.nearbyListAdapter = new POIArrayAdapter(
				activity,
				this.sensorManager,
				this.dataSourcesRepository,
				this.defaultPrefRepository,
				this.lclPrefRepository,
				this.poiRepository,
				this.favoriteRepository,
				this.sharedStatusLoader,
				this.sharedServiceUpdateLoader,
				this.analyticsManager
		);
		this.nearbyListAdapter.setLogTag(getLogTag());
	}

	@Nullable
	private POIListFooterManager _footerManager = null;

	@NonNull
	protected POIListFooterManager getFooterManager() {
		if (_footerManager == null) {
			_footerManager = makePoiListFooterManager(this);
		}
		return _footerManager;
	}

	@MainThread
	private void setupView() {
		if (this.binding == null) return;
		setupScreenToolbar(this.binding.screenToolbarLayout);
		final Resources resources = this.binding.getRoot().getContext().getResources();
		final MapView map = this.binding.map;
		EdgeToEdgeKt.setUpMapEdgeToEdge(map, this.mapViewController, TOP_PADDING_DP, BOTTOM_PADDING_DP,
				resources.getDimensionPixelSize(R.dimen.large_header_height)
		);
		if (this.nearbyListAdapter != null) {
			this.nearbyListAdapter.setManualScrollView(this.binding.scrollView);
			this.nearbyListAdapter.setManualLayout(this.binding.poiNearbyPoisList);
		}
		setupViewKt(this);
		setupNewsLayout();
		setupServiceUpdateLayout();
	}

	private void setupServiceUpdateLayout() {
		if (!UIFeatureFlags.F_SERVICE_UPDATE_ELLIPSIZE_IN_POI) return;
		if (this.binding == null) return;
		final TextView serviceUpdateText = this.binding.poiServiceUpdate.serviceUpdateText;
		serviceUpdateText.setMaxLines(SERVICE_UPDATE_MAX_LINES);
		serviceUpdateText.setOnClickListener(v -> {
			analyticsManager.trackButtonClick("service_update_more", this);
			final POIManager poim = getPoimOrNull();
			if (poim == null) return;
			if (FeatureFlags.F_NAVIGATION) {
				// TODO navigate to dialog
			} else {
				FragmentUtils.replaceDialogFragment(
						getActivity(),
						FragmentUtils.DIALOG_TAG,
						ServiceUpdatesDialog.newInstanceForStop(poim.poi.getAuthority(), poim.poi.getUUID()),
						null
				);
			}
		});
	}

	private void setupRDSFullScheduleBtn() {
		if (this.binding == null) return;
		final ViewBinding poiStatusBinding = getPOIStatusBinding();
		final LayoutPoiDetailStatusScheduleBinding scheduleStatusBinding =
				(poiStatusBinding instanceof LayoutPoiDetailStatusScheduleBinding) ? (LayoutPoiDetailStatusScheduleBinding) poiStatusBinding : null;
		if (scheduleStatusBinding == null) return;
		final View rdsScheduleBtn = scheduleStatusBinding.poiDetailStatusScheduleTitle.fullScheduleBtn;
		final Boolean hasScheduleProviders = this.viewModel == null ? null : this.viewModel.getHasScheduleProviders().getValue();
		if (hasScheduleProviders == null || !hasScheduleProviders) {
			rdsScheduleBtn.setVisibility(View.GONE);
			return;
		}
		final POIManager poimForColor = getPoimOrNull();
		if (poimForColor != null) {
			final int poiColor = poimForColor.getColor(dataSourcesRepository);
			rdsScheduleBtn.setBackgroundColor(poiColor);
		}
		rdsScheduleBtn.setOnClickListener(v -> {
			analyticsManager.trackButtonClick("full_schedule", this);
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

	private void setupMoreNewsButton() {
		if (this.binding == null) return;
		final View moreBtn = binding.poiNews.newsTitleLayout.moreBtn;
		moreBtn.setOnClickListener(v -> {
			analyticsManager.trackButtonClick("news_more", this);
			final POIManager poim = getPoimOrNull();
			if (poim == null) return;
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
								NewsProviderContract.Filter.makeTargets(poim.poi)
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
								NewsProviderContract.Filter.makeTargets(poim.poi)
						),
						POIFragment.this
				);
			}
		});
		binding.poiNews.newsTitleLayout.moreSectionTitle.setOnClickListener(v -> moreBtn.performClick());
		moreBtn.setVisibility(View.VISIBLE);
	}

	private void setupMoreNearbyButton() {
		if (this.binding == null) return;
		final View moreBtn = binding.poiNearbyPoisTitle.moreBtn;
		moreBtn.setOnClickListener(v -> {
			analyticsManager.trackButtonClick("nearby_more", this);
			final POIManager poim = getPoimOrNull();
			if (poim == null) return;
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
				if (!(activity instanceof MainActivity)) return;
				((MainActivity) activity).addFragmentToStack(
						NearbyFragment.newFixedOnPOIInstance(poim, dataSourcesRepository),
						POIFragment.this
				);
			}
		});
		binding.poiNearbyPoisTitle.getRoot().setOnClickListener(v -> moreBtn.performClick());
		moreBtn.setVisibility(View.VISIBLE);
	}

	private @Nullable ViewBinding poiStatusBinding = null;

	// TODO @Deprecated
	@Nullable
	private View getPOIStatusView() {
		final ViewBinding poiStatusBinding = getPOIStatusBinding();
		return poiStatusBinding == null ? null : poiStatusBinding.getRoot();
	}

	@Nullable
	private ViewBinding getPOIStatusBinding() {
		final POIManager poim = getPoimOrNull();
		if (this.binding == null || poim == null) return null;
		if (poiStatusBinding == null) {
			poiStatusBinding = POIStatusDetailViewController.getLayoutViewBinding(poim, this.binding.poiStatusDetailStub);
		}
		return poiStatusBinding;
	}

	// TODO @Deprecated
	@Nullable
	private View getPOIServiceUpdateView() {
		if (this.binding == null) return null;
		return this.binding.poiServiceUpdate.getRoot();
	}

	private void setupAppUpdateButton() {
		final TextView appUpdateText = this.binding == null ? null : this.binding.poiAppUpdate.appUpdateText;
		if (appUpdateText == null) return;
		appUpdateText.setText(appUpdateText.getContext().getText(R.string.app_update_btn_text_short_formatted));
		appUpdateText.setOnClickListener(v -> {
			analyticsManager.trackButtonClick("app_update", this);
			final Activity activity = getActivity();
			if (activity == null) return;
			final IAgencyUpdatableProperties agency = getAgencyOrNull();
			if (agency == null) return;
			final String pkg = agency.getPkg();
			analyticsManager.logEvent(AnalyticsEvents.CLICK_APP_UPDATE_POI, new AnalyticsEventsParamsProvider()
					.put(AnalyticsEvents.Params.PKG, pkg)
			);
			if (agency.getUpdateAvailable()) {
				AppUpdateLauncher.launchAppUpdate(activity, pkg);
				return; // handled
			}
			StoreUtils.viewAppPage(activity, pkg, activity.getString(org.mtransit.android.commons.R.string.google_play));
		});
	}

	private void setupAppWasDisabledButton() {
		if (this.binding == null) return;
		this.binding.poiModuleWasDisabled.appWasDisabledBtn.setOnClickListener(v -> {
			analyticsManager.trackButtonClick("module_was_disabled", this);
			final Activity activity = getActivity();
			if (activity == null) return;
			if (viewModel != null) {
				viewModel.onBatteryOptimizationSettingsOpened();
			}
			analyticsManager.logEvent(AnalyticsEvents.CLICK_APP_WAS_DISABLED_POI, null);
			BatteryOptimizationIssueUtils.openDeviceBatteryOptimizationSettings(activity);
		});
	}

	private @Nullable ViewBinding poiViewBinding = null;

	// TODO @Deprecated
	@Nullable
	private View getPOIView() {
		final ViewBinding poiViewBinding = getPOIViewBinding();
		return poiViewBinding == null ? null : poiViewBinding.getRoot();
	}

	@Nullable
	private ViewBinding getPOIViewBinding() {
		final POIManager poim = getPoimOrNull();
		if (this.binding == null || poim == null) return null;
		if (poiViewBinding == null) { // IF NOT present/inflated DO
			poiViewBinding = POIViewController.getLayoutViewBinding(poim, this.binding.thisPoiStub);
		}
		return poiViewBinding;
	}

	@Override
	public void onStatusLoaded(@NonNull POIStatus status) {
		if (this.binding == null) return;
		POIViewController.updatePOIStatus(getPOIView(), status, this, getPoimOrNull());
		POIStatusDetailViewController.updatePOIStatus(getPOIStatusView(), status, this, getPoimOrNull());
	}

	@Override
	public void onServiceUpdatesLoaded(@NonNull String targetUUID, @NonNull List<ServiceUpdate> serviceUpdates) {
		if (this.binding == null) return;
		POIViewController.updateServiceUpdatesView(getPOIView(), serviceUpdates, this);
		POIServiceUpdateViewController.updateServiceUpdate(getPOIServiceUpdateView(), serviceUpdates, this);
	}

	@Nullable
	private Boolean useInternalWebBrowserPref = null;

	private void onUseInternalWebBrowserPrefLoaded(@Nullable Boolean useInternalWebBrowserPref) {
		this.useInternalWebBrowserPref = useInternalWebBrowserPref;
	}

	@Override
	public boolean onURLClick(@NonNull View view, @NonNull String url) {
		return LinkUtils.open(view, requireActivity(), url, getString(org.mtransit.android.commons.R.string.web_browser), true, this.useInternalWebBrowserPref);
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
		// DO NOTHING
	}

	@MainThread
	@Override
	public void onDeviceLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) return;
		final Context context = getContext();
		if (this.deviceLocation == null && context != null) {
			this.mapViewController.setLocationPermissionGranted(this.locationPermissionProvider.allRequiredPermissionsGranted(context));
		}
		if (this.deviceLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.deviceLocation, newLocation)) {
			this.deviceLocation = newLocation;
			this.locationDeclination = sensorManager.getLocationDeclination(newLocation);
			if (!this.compassUpdatesEnabled) {
				sensorManager.registerCompassListener(this, this);
				this.compassUpdatesEnabled = true;
			}
			updateDistanceString();
			final POIManager poim = getPoimOrNull();
			if (poim != null) {
				POIViewController.updatePOIDistanceAndCompass(getPOIView(), poim, this);
			}
			this.mapViewController.onDeviceLocationChanged(this.deviceLocation);
			if (this.nearbyListAdapter != null) {
				this.nearbyListAdapter.setLocation(newLocation);
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
			POIViewController.updatePOIDistanceAndCompass(getPOIView(), poim, this);
		}
	}

	@MainThread
	@Override
	public void onResume() {
		super.onResume();
		enableTimeChangedReceiver();
		this.showingAccessibilityInfo = null; // force user preference check
		this.mapViewController.onResume();
		if (this.nearbyListAdapter != null) {
			this.nearbyListAdapter.onResume(this, this.deviceLocation);
		}
		if (this.newsListAdapter != null) {
			this.newsListAdapter.onResume(this);
		}
		final POIManager poim = getPoimOrNull();
		if (poim != null) {
			POIViewController.updateView(getPOIView(), poim, this);
			POIStatusDetailViewController.updateView(getPOIStatusView(), poim, this);
			POIServiceUpdateViewController.updateView(getPOIServiceUpdateView(), poim, this);
			setupRDSFullScheduleBtn();
			setupMoreNewsButton();
			setupAppUpdateButton();
			setupAppWasDisabledButton();
			setupMoreNearbyButton();
			setupNearbyList();
		}
		onDeviceLocationChanged(((MTActivityWithLocation) requireActivity()).getDeviceLocation());
		this.adManager.setRewardedAdListener(this);
		onResumeKt(this);
		refreshRewardedLayout();
		refreshAppUpdateLayout();
		if (this.viewModel != null) {
			this.viewModel.refreshAppUpdateAvailable();
			// this.viewModel.onResumeScreen(this);
			this.viewModel.startVehicleLocationRefresh();
			startVehicleLocationCountdownRefresh(this);
		}
		if (FeatureFlags.F_NAVIGATION) {
			if (nextMainViewModel != null) {
				nextMainViewModel.setABBgColor(getABBgColor(getContext()));
			}
		}
	}

	@WorkerThread
	@Override
	public boolean skipLoadingRewardedAd() {
		return this.adManager.shouldSkipLoadingRewardedAd();
	}

	@AnyThread
	@Override
	public void onRewardedAdStatusChanged() {
		final View view = getView();
		if (view == null) return;
		view.post(this::refreshRewardedLayout);
	}

	@MainThread
	private void refreshRewardedLayout() {
		updateFooter(this);
	}

	private void refreshAppUpdateLayout() {
		final LayoutPoiAppUpdateBinding appUpdateLayout = this.binding == null ? null : this.binding.poiAppUpdate;
		if (appUpdateLayout == null) {
			MTLog.d(this, "refreshAppUpdateLayout() > SKIP (no layout)");
			return;
		}
		final IAgencyUpdatableProperties agency = getAgencyOrNull();
		boolean appUpdateAvailable = agency != null && agency.getUpdateAvailable()
				&& agency.getShouldShowUpdateLayout();
		if (demoModeManager.isFullDemo()) {
			appUpdateAvailable = false; // always false (demo mode ON)
		}
		final String pkg = agency == null ? "" : agency.getPkg();
		if (appUpdateAvailable) {
			if (appUpdateLayout.getRoot().getVisibility() != View.VISIBLE) {
				appUpdateLayout.getRoot().setVisibility(View.VISIBLE);
				this.analyticsManager.logEvent(AnalyticsEvents.SHOWED_APP_UPDATE_POI, new AnalyticsEventsParamsProvider()
						.put(AnalyticsEvents.Params.PKG, pkg)
				);
			}
		} else {
			if (appUpdateLayout.getRoot().getVisibility() != View.GONE) {
				appUpdateLayout.getRoot().setVisibility(View.GONE);
				this.analyticsManager.logEvent(AnalyticsEvents.HIDDEN_APP_UPDATE_POI, new AnalyticsEventsParamsProvider()
						.put(AnalyticsEvents.Params.PKG, pkg)
				);
			}
		}
	}

	private void onHasSeenDisabledModuleLoaded(@Nullable Boolean hasSeenDisabledModule) {
		if (hasSeenDisabledModule == null) return;
		refreshAppWasDisabledLayout(hasSeenDisabledModule);
	}

	@MainThread
	private void refreshAppWasDisabledLayout(boolean appWasDisabled) {
		final LayoutPoiAppWasDisabledBinding appWasDisabledLayout = this.binding == null ? null : this.binding.poiModuleWasDisabled;
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
		if (appUpdateAvailable) {
			appWasDisabled = false; // avoid too many messages
		} else if (demoModeManager.isFullDemo()) {
			appWasDisabled = false; // always false (demo mode ON)
		}
		if (appWasDisabled) {
			final Resources resources = appWasDisabledLayout.getRoot().getContext().getResources();
			final TextView appWasDisabledTitleTv = appWasDisabledLayout.appWasDisabledTitleLayout.appWasDisabledTitle;
			final TextView appWasDisabledBtn = appWasDisabledLayout.appWasDisabledBtn;
			final int agencyCount = this.dataSourcesRepository.getAllAgenciesCount();
			appWasDisabledTitleTv.setText(resources.getQuantityString(R.plurals.app_disabled_issue_title, agencyCount, agencyCount));
			appWasDisabledBtn.setText(R.string.battery_optimization_samsung_use_device_care);
			if (appWasDisabledLayout.getRoot().getVisibility() != View.VISIBLE) {
				appWasDisabledLayout.getRoot().setVisibility(View.VISIBLE);
				this.analyticsManager.logEvent(AnalyticsEvents.SHOWED_APP_WAS_DISABLED_POI, null);
			}
		} else {
			if (appWasDisabledLayout.getRoot().getVisibility() != View.GONE) {
				appWasDisabledLayout.getRoot().setVisibility(View.GONE);
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
			stopVehicleLocationCountdownRefresh(this);
		}
		if (this.nearbyListAdapter != null) {
			this.nearbyListAdapter.onPause();
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
		final Activity activity = getActivity();
		if (activity instanceof IAdScreenActivity) {
			this.adManager.onTimeChanged((IAdScreenActivity) activity);
		}
	}

	private void resetNowToTheMinute() {
		MTLog.i(this, "Refreshing UI data...");
		this.nowToTheMinute = UITimeUtils.currentTimeToTheMinuteMillis();
		final POIManager poim = getPoimOrNull();
		if (poim != null) {
			final View poiView = getPOIView();
			POIViewController.updatePOIStatus(poiView, poim, this);
			POIViewController.updatePOIServiceUpdate(poiView, poim, this);
			POIStatusDetailViewController.updateView(getPOIStatusView(), poim, this);
			POIServiceUpdateViewController.updateView(getPOIServiceUpdateView(), poim, this);
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

	@Override
	public boolean isFavorite() {
		final Boolean isFavorite = viewModel == null ? null : viewModel.isFavorite().getValue();
		return Boolean.TRUE.equals(isFavorite);
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
			this.showingAccessibilityInfo = this.defaultPrefRepository.getPref().getBoolean(
					DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY,
					DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY_DEFAULT
			);
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

	private void onUsingFavoriteFoldersLoaded(@Nullable Boolean usingFavoriteFolders) {
		final Boolean isFavorite = viewModel == null ? null : viewModel.isFavorite().getValue();
		updateFabFavorite(isFavorite, usingFavoriteFolders);
	}

	private void onFavoriteLoaded(@Nullable Boolean isFavorite) {
		final Boolean usingFavoriteFolders = viewModel == null ? null : viewModel.getUsingFavoriteFolders().getValue();
		updateFabFavorite(isFavorite, usingFavoriteFolders);
	}

	private void updateFabFavorite(@Nullable Boolean isFavorite, @Nullable Boolean usingFavoriteFolders) {
		if (this.binding == null) return;
		final FloatingActionButton fabFavorite = this.binding.fabFavorite;
		final POIManager poim = getPoimOrNull();
		if (isFavorite == null || poim == null || !poim.isFavoritable()) {
			fabFavorite.hide();
			return;
		}
		@DrawableRes int iconResId;
		@StringRes int contentDescriptionResId;
		if (isFavorite) {
			iconResId = R.drawable.ic_star_black_24dp;
			if (Boolean.TRUE.equals(usingFavoriteFolders)) {
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
				LinkUtils.open(this.binding == null ? null : this.binding.getRoot(), requireActivity(), faresWebUrl, getString(R.string.fares), null, true, this.useInternalWebBrowserPref);
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
				MapUtils.showDirection(this.binding, requireActivity(), poim2.poi.getLat(), poim2.poi.getLng(), optSrcLat, optSrcLng, poim2.poi.getName(), this.useInternalWebBrowserPref);
				return true; // handled
			}
		}
		return false; // not handled
	}

	@Override
	public @NonNull AnalyticsScreen getAnalyticsScreen() {
		return this;
	}

	@NonNull
	@Override
	public IAnalyticsManager getAnalyticsManager() {
		return this.analyticsManager;
	}

	@Override
	public boolean hasToolbar() {
		return true;
	}

	@Override
	public boolean isABReady() {
		return getAgencyOrNull() != null;
	}

	@Nullable
	@Override
	public CharSequence getABTitle(@Nullable Context context) {
		final AgencyProperties agency = getAgencyOrNull();
		if (agency != null) return agency.getShortName();
		if (context == null) return super.getABTitle(null);
		return context.getString(org.mtransit.android.commons.R.string.ellipsis);
	}

	@Override
	public boolean isABStatusBarTransparent() {
		return true;
	}

	@Override
	public boolean isABOverrideGradient() {
		return hasToolbar() || UIFeatureFlags.F_EDGE_TO_EDGE;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		this.mapViewController.onLowMemory();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		this.binding = null;
		this.poiViewBinding = null;
		this.poiStatusBinding = null;
		if (this.nearbyListAdapter != null) {
			this.nearbyListAdapter.onDestroyView();
		}
		this.mapViewController.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.mapViewController.onDestroy();
		if (this.nearbyListAdapter != null) {
			this.nearbyListAdapter.onDestroy();
			this.nearbyListAdapter = null;
		}
		if (this.newsListAdapter != null) {
			this.newsListAdapter.onDestroy(this);
			this.newsListAdapter = null;
		}
	}
}
