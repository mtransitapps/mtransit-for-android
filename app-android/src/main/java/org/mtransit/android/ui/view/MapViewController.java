package org.mtransit.android.ui.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.ResourceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.data.IAgencyUIProperties;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.pick.PickPOIDialogFragment;
import org.mtransit.android.ui.view.map.ClusteringSettings;
import org.mtransit.android.ui.view.map.ExtendedGoogleMap;
import org.mtransit.android.ui.view.map.ExtendedMarkerOptions;
import org.mtransit.android.ui.view.map.IMarker;
import org.mtransit.android.ui.view.map.MTClusterOptionsProvider;
import org.mtransit.android.ui.view.map.impl.ExtendedMapFactory;
import org.mtransit.android.ui.view.map.utils.LatLngUtils;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.FragmentUtils;
import org.mtransit.android.util.MapUtils;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class MapViewController implements ExtendedGoogleMap.OnCameraChangeListener, ExtendedGoogleMap.OnInfoWindowClickListener,
		ExtendedGoogleMap.OnMapLoadedCallback, ExtendedGoogleMap.OnMarkerClickListener, ExtendedGoogleMap.OnMyLocationButtonClickListener,
		ExtendedGoogleMap.OnMapClickListener, LocationSource, OnMapReadyCallback, ViewTreeObserver.OnGlobalLayoutListener, MTLog.Loggable {

	private static final String LOG_TAG = MapViewController.class.getSimpleName();

	@NonNull
	private String logTag = LOG_TAG;

	@NonNull
	@Override
	public String getLogTag() {
		return this.logTag;
	}

	private static final String EXTRA_LAST_CAMERA_POSITION = "extra_last_camera_position";
	private static final String EXTRA_LAST_SELECTED_UUID = "extra_last_selected_uuid";

	@Nullable
	private WeakReference<Activity> activityWR;
	@Nullable
	private MapView mapView;
	@Nullable
	private View loadingMapView;
	@Nullable
	private ImageView typeSwitchView;
	@Nullable
	private ExtendedGoogleMap extendedGoogleMap;
	@Nullable
	private Boolean showingMyLocation = false;
	private boolean mapLayoutReady = false;
	private boolean mapVisible = false;
	@Nullable
	private LocationSource.OnLocationChangedListener locationChangedListener;
	@Nullable
	private Location deviceLocation;
	private boolean waitingForGlobalLayout = false;

	@Override
	public void onGlobalLayout() {
		this.waitingForGlobalLayout = false;
		final MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			this.mapLayoutReady = true;
			showMapInternal(null);
		}
	}

	private boolean initialMapCameraSetup = false;

	private void setupInitialCamera() {
		if (this.initialMapCameraSetup) {
			MTLog.d(this, "setupInitialCamera() > SKIP (already setup)");
			return;
		}
		if (!this.mapLayoutReady || !hasMapView() || !hasGoogleMap()) {
			MTLog.d(this, "setupInitialCamera() > SKIP (map not ready)");
			return;
		}
		if (!this.mapVisible) {
			MTLog.d(this, "setupInitialCamera() > SKIP (map NOT visible)");
			return;
		}
		this.initialMapCameraSetup = showLastCameraPosition();
		if (!this.initialMapCameraSetup) {
			this.initialMapCameraSetup = showMarkers(false, this.followingDevice);
		}
		if (!this.initialMapCameraSetup) {
			this.initialMapCameraSetup = showDeviceLocation(false);
		}
	}

	private WeakReference<MapMarkerProvider> markerProviderWR;
	private WeakReference<MapListener> mapListenerWR;
	private final boolean mapToolbarEnabled;
	private final boolean myLocationEnabled;
	private final boolean myLocationButtonEnabled;
	private final boolean indoorLevelPickerEnabled;
	private final boolean trafficEnabled;
	private final boolean indoorEnabled;
	private final int paddingTopSp;
	private final int paddingBottomSp;
	private final boolean followingDevice;
	private final boolean hasButtons;
	private final boolean clusteringEnabled;
	private final boolean showAllMarkersWhenReady;
	private final boolean markerLabelShowExtra;

	private CameraPosition lastCameraPosition;

	private String lastSelectedUUID;

	private boolean locationPermissionGranted = false;

	@Nullable
	private DataSourcesRepository dataSourcesRepository;

	public MapViewController(@NonNull String logTag,
							 @Nullable MapMarkerProvider markerProvider,
							 @Nullable MapListener mapListener,
							 boolean mapToolbarEnabled,
							 boolean myLocationEnabled,
							 boolean myLocationButtonEnabled,
							 boolean indoorLevelPickerEnabled,
							 boolean trafficEnabled,
							 boolean indoorEnabled,
							 int paddingTopSp,
							 int paddingBottomSp,
							 boolean followingDevice,
							 boolean hasButtons,
							 boolean clusteringEnabled,
							 boolean showAllMarkersWhenReady,
							 boolean markerLabelShowExtra,
							 @Nullable DataSourcesRepository dataSourcesRepository) {
		setLogTag(logTag);
		setMarkerProvider(markerProvider);
		setMapListener(mapListener);
		this.mapToolbarEnabled = mapToolbarEnabled;
		this.myLocationEnabled = myLocationEnabled;
		this.myLocationButtonEnabled = myLocationButtonEnabled;
		this.indoorLevelPickerEnabled = indoorLevelPickerEnabled;
		this.trafficEnabled = trafficEnabled;
		this.indoorEnabled = indoorEnabled;
		this.paddingTopSp = paddingTopSp;
		this.paddingBottomSp = paddingBottomSp;
		this.followingDevice = followingDevice;
		this.hasButtons = hasButtons;
		this.clusteringEnabled = clusteringEnabled;
		this.showAllMarkersWhenReady = showAllMarkersWhenReady;
		this.markerLabelShowExtra = markerLabelShowExtra;
		this.dataSourcesRepository = dataSourcesRepository;
	}

	private void setMarkerProvider(@Nullable MapMarkerProvider markerProvider) {
		this.markerProviderWR = new WeakReference<>(markerProvider);
	}

	private void setMapListener(@Nullable MapListener mapListener) {
		this.mapListenerWR = new WeakReference<>(mapListener);
	}

	public void setLogTag(@NonNull String tag) {
		this.logTag = LOG_TAG + "-" + tag;
	}

	public void onAttach(@NonNull Activity activity) {
		setActivity(activity);
	}

	public void setDataSourcesRepository(@Nullable DataSourcesRepository dataSourcesRepository) {
		this.dataSourcesRepository = dataSourcesRepository;
	}

	public void onCreate(@Nullable Bundle savedInstanceState) {
		restoreInstanceState(savedInstanceState);
		this.lastSavedInstanceState = savedInstanceState;
	}

	private void restoreInstanceState(@Nullable Bundle savedInstanceState) {
		final CameraPosition newLastCameraPosition = BundleUtils.getParcelable(EXTRA_LAST_CAMERA_POSITION, savedInstanceState);
		if (newLastCameraPosition != null) {
			this.lastCameraPosition = newLastCameraPosition;
		}
		final String newLastSelectedUUID = BundleUtils.getString(EXTRA_LAST_SELECTED_UUID, savedInstanceState);
		if (!TextUtils.isEmpty(newLastSelectedUUID)) {
			this.lastSelectedUUID = newLastSelectedUUID; // restore from saved state
		}
	}

	public void onViewCreated(@SuppressWarnings("unused") @NonNull View view,
							  @Nullable Bundle savedInstanceState) {
		this.lastSavedInstanceState = savedInstanceState;
		restoreInstanceState(savedInstanceState);
	}

	@Nullable
	private Bundle lastSavedInstanceState = null;

	private boolean hasMapView() {
		return this.mapView != null;
	}

	@Nullable
	private MapView getMapViewOrNull() {
		return this.mapView;
	}

	@MainThread
	@Nullable
	private MapView getMapViewOrInit(@NonNull View view) {
		if (this.mapView == null) {
			initMapViewAsync(view);
		}
		return this.mapView;
	}

	@MainThread
	private void initMapViewAsync(@NonNull View view) {
		if (initializingMapView.getAndSet(true)) {
			MTLog.d(this, "initMapViewAsync() > SKIP (already running)");
			return;
		}
		try {
			MapsInitializerUtil.initMap(view.getContext().getApplicationContext(), renderer -> {
				applyNewMapView(view);
				initializingMapView.set(false);
			});
		} catch (Exception e) {
			MTLog.w(this, e, "Error while initializing map!");
			initializingMapView.set(false);
		}
	}

	@NonNull
	private final AtomicBoolean initializingMapView = new AtomicBoolean(false);

	@MainThread
	private void applyNewMapView(@NonNull View view) {
		if (this.mapView != null) {
			MTLog.d(this, "applyNewMapView() > SKIP (already set)");
			return;
		}
		this.mapView = view.findViewById(R.id.map);
		this.loadingMapView = view.findViewById(R.id.map_loading);
		this.typeSwitchView = view.findViewById(R.id.map_type_switch);
		if (this.mapView != null) {
			try {
				this.mapView.onCreate(this.lastSavedInstanceState);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while creating map view with '%s' (trying again)", this.lastSavedInstanceState);
				this.mapView.onCreate(null);
			}
			if (this.needToResumeMap) {
				this.mapView.onResume();
				this.needToResumeMap = false;
			}
			this.lastSavedInstanceState = null;
			this.mapView.getMapAsync(this);
		}
		showHideLoading();
		initTypeSwitch();
	}

	private void destroyMapView() {
		if (this.mapView != null) {
			this.mapView.onDestroy();
			this.mapView = null;
		}
	}

	public void onActivityCreated(@SuppressWarnings("unused") @Nullable Bundle savedInstanceState) {
		// DO NOTHING
	}

	private boolean hasGoogleMap() {
		return this.extendedGoogleMap != null;
	}

	private void destroyGoogleMap() {
		this.extendedGoogleMap = null;
	}

	@Nullable
	private ExtendedGoogleMap getGoogleMapOrNull() {
		return this.extendedGoogleMap;
	}

	@Override
	public void onMapReady(@NonNull GoogleMap googleMap) {
		setupGoogleMap(googleMap);
	}

	private boolean clusterManagerItemsLoaded = false;

	private void setupGoogleMap(@NonNull GoogleMap googleMap) {
		this.extendedGoogleMap = ExtendedMapFactory.create(googleMap, getActivityOrNull());
		applyMapStyle();
		applyMapType();
		setupGoogleMapMyLocation();
		this.extendedGoogleMap.setTrafficEnabled(this.trafficEnabled);
		this.extendedGoogleMap.setIndoorEnabled(this.indoorEnabled);
		this.extendedGoogleMap.getUiSettings().setIndoorLevelPickerEnabled(this.indoorLevelPickerEnabled);
		this.extendedGoogleMap.getUiSettings().setMapToolbarEnabled(this.mapToolbarEnabled);
		this.extendedGoogleMap.setOnMapLoadedCallback(this);
		this.extendedGoogleMap.setOnMyLocationButtonClickListener(this);
		this.extendedGoogleMap.setOnInfoWindowClickListener(this);
		this.extendedGoogleMap.setOnMarkerClickListener(this);
		this.extendedGoogleMap.setOnMapClickListener(this);
		this.extendedGoogleMap.setLocationSource(this);
		this.extendedGoogleMap.setOnCameraChangeListener(this);
		ClusteringSettings settings = new ClusteringSettings();
		settings.enabled(this.clusteringEnabled);
		settings.clusterOptionsProvider(new MTClusterOptionsProvider(getActivityOrNull())).addMarkersDynamically(true);
		this.extendedGoogleMap.setClustering(settings);
		clearMarkers();
		int paddingTopPx = 0;
		if (this.paddingTopSp > 0) {
			final Context context = getActivityOrNull();
			paddingTopPx = (int) ResourceUtils.convertSPtoPX(context, this.paddingTopSp); // action bar
		}
		int paddingBottomPx = 0;
		if (this.paddingBottomSp > 0) {
			final Context context = getActivityOrNull();
			paddingBottomPx = (int) ResourceUtils.convertSPtoPX(context, this.paddingBottomSp); // fab
		}
		this.extendedGoogleMap.setPadding(0, paddingTopPx, 0, paddingBottomPx);
		final MapListener mapListener = this.mapListenerWR == null ? null : this.mapListenerWR.get();
		if (mapListener != null) {
			mapListener.onMapReady();
		}
		showMapInternal(null);
	}

	public void setLocationPermissionGranted(boolean locationPermissionGranted) {
		if (this.locationPermissionGranted == locationPermissionGranted) {
			return; // no change
		}
		this.locationPermissionGranted = locationPermissionGranted;
		setupGoogleMapMyLocation();
	}

	@SuppressLint("MissingPermission")
	private void setupGoogleMapMyLocation() {
		if (this.extendedGoogleMap == null) {
			return; // SKIP (map not ready)
		}
		if (this.locationPermissionGranted) {
			this.extendedGoogleMap.setMyLocationEnabled(this.myLocationEnabled);
			this.extendedGoogleMap.getUiSettings().setMyLocationButtonEnabled(this.myLocationButtonEnabled);
		} else {
			this.extendedGoogleMap.setMyLocationEnabled(false);
			this.extendedGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
		}
	}

	private void showHideLoading() {
		if (this.clusterManagerItemsLoaded) {
			hideLoading();
		} else {
			showLoading();
		}
	}

	public void showLoading() {
		if (this.loadingMapView != null && this.loadingMapView.getVisibility() != View.VISIBLE) {
			this.loadingMapView.setVisibility(View.VISIBLE);
		}
	}

	public void hideLoading() {
		if (this.loadingMapView != null && this.loadingMapView.getVisibility() != View.GONE) {
			this.loadingMapView.setVisibility(View.GONE);
		}
	}

	private void hideTypeSwitch() {
		if (this.typeSwitchView != null) {
			this.typeSwitchView.setVisibility(View.GONE);
		}
	}

	private void showTypeSwitch() {
		if (this.typeSwitchView != null) {
			this.typeSwitchView.setVisibility(View.VISIBLE);
		}
	}

	private void initTypeSwitch() {
		if (this.typeSwitchView == null) {
			return;
		}
		setTypeSwitchImg();
		this.typeSwitchView.setOnClickListener(v ->
				switchMapType()
		);
		this.typeSwitchView.setVisibility(View.VISIBLE);
	}

	private int mapType = -1;

	private void resetMapType() {
		this.mapType = -1;
	}

	private int getMapType() {
		if (this.mapType < 0) {
			this.mapType = PreferenceUtils.getPrefLcl(getActivityOrNull(), MapUtils.PREFS_LCL_MAP_TYPE, MapUtils.PREFS_LCL_MAP_TYPE_DEFAULT);
		}
		return this.mapType;
	}

	private void switchMapType() {
		int newMapType = getMapType() == MapUtils.MAP_TYPE_NORMAL ? MapUtils.MAP_TYPE_SATELLITE : MapUtils.MAP_TYPE_NORMAL; // switch
		setMapType(newMapType);
	}

	private void setMapType(int newMapType) {
		this.mapType = newMapType;
		setTypeSwitchImg();
		applyMapType();
		final Context context = getActivityOrNull();
		if (context != null) {
			PreferenceUtils.savePrefLclAsync(context, MapUtils.PREFS_LCL_MAP_TYPE, this.mapType); // asynchronous
		}
	}

	private void applyMapType() {
		final ExtendedGoogleMap map = getGoogleMapOrNull();
		if (map == null) {
			return;
		}
		map.setMapType(getMapType());
	}

	private void applyMapStyle() {
		final ExtendedGoogleMap map = getGoogleMapOrNull();
		if (map == null) {
			return;
		}
		final Context context = getActivityOrNull();
		if (context == null) {
			return;
		}
		// https://mapstyle.withgoogle.com/
		map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_default));
	}

	private void setTypeSwitchImg() {
		if (this.typeSwitchView != null) {
			this.typeSwitchView.setImageResource(getMapType() == MapUtils.MAP_TYPE_NORMAL ? R.drawable.map_satellite : R.drawable.map_normal);
		}
	}

	@MainThread
	public boolean showMap(@Nullable View view) {
		this.mapVisible = true;
		return showMapInternal(view);
	}

	@MainThread
	private boolean showMapInternal(@Nullable View optView) {
		final MapView mapView = optView == null ? getMapViewOrNull() : getMapViewOrInit(optView);
		if (mapView == null) {
			MTLog.d(this, "showMapInternal() > SKIP (no map)");
			return false; // not shown
		}
		if (!this.mapVisible) {
			MTLog.d(this, "showMapInternal() > SKIP (map not visible)");
			return false; // not shown
		}
		showHideLoading();
		showTypeSwitch();
		if (this.mapLayoutReady) {
			if (!this.initialMapCameraSetup) {
				setupInitialCamera();
			}
			if (mapView.getVisibility() != View.VISIBLE) {
				mapView.setVisibility(View.VISIBLE);
			}
			initMapMarkers();
			return true; // shown
		}
		if (mapView.getVisibility() == View.GONE) {
			mapView.setVisibility(View.INVISIBLE);
		}
		if (!this.mapLayoutReady && !this.waitingForGlobalLayout) {
			if (mapView.getViewTreeObserver().isAlive()) {
				mapView.getViewTreeObserver().addOnGlobalLayoutListener(this);
				this.waitingForGlobalLayout = true;
			}
		}
		return false; // not shown
	}

	public void hideMap() {
		this.mapVisible = false;
		final MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			if (mapView.getVisibility() == View.VISIBLE) {
				mapView.setVisibility(View.INVISIBLE); // hide
			}
		}
		hideTypeSwitch();
	}

	@Override
	public void onMapLoaded() {
		// DO NOTHING
	}

	@Override
	public void onMapClick(@NonNull LatLng position) {
		final MapListener mapListener = this.mapListenerWR == null ? null : this.mapListenerWR.get();
		if (mapListener != null) {
			mapListener.onMapClick(position);
		}
		this.lastSelectedUUID = null; // not selected anymore (map click)
	}

	@Override
	public void onInfoWindowClick(@Nullable IMarker marker) {
		if (marker != null && marker.getData() != null && marker.getData() instanceof POIMarkerIds) {
			final POIMarkerIds poiMarkerIds = marker.getData();
			final ArrayMap.Entry<String, String> uuidAndAuthority = poiMarkerIds.entrySet().iterator().next();
			this.lastSelectedUUID = uuidAndAuthority.getKey(); // keep selected if leaving the screen
			if (poiMarkerIds.size() >= 1) {
				if (FeatureFlags.F_NAVIGATION) {
					// TODO navigate to dialog
				} else {
					final Activity activity = getActivityOrNull();
					if (activity instanceof MainActivity) {
						FragmentUtils.replaceDialogFragment(
								(MainActivity) activity,
								FragmentUtils.DIALOG_TAG,
								PickPOIDialogFragment.newInstance(poiMarkerIds.getMap()),
								null
						);
					}
				}
			}
		}
	}

	private static final float MARKER_ZOOM_INC = 2.0f;

	@Override
	public boolean onMarkerClick(@Nullable IMarker marker) {
		String newSelectedUUID = null;
		if (marker != null
				&& !marker.isCluster()
				&& marker.getData() != null
				&& marker.getData() instanceof POIMarkerIds) {
			final POIMarkerIds poiMarkerIds = marker.getData();
			final ArrayMap.Entry<String, String> uuidAndAuthority = poiMarkerIds.entrySet().iterator().next();
			newSelectedUUID = uuidAndAuthority.getKey();
		} else if (marker != null && marker.isCluster()) {
			if (this.extendedGoogleMap != null) {
				final float zoom = this.extendedGoogleMap.getCameraPosition().zoom + MARKER_ZOOM_INC;
				return updateMapCamera(true, CameraUpdateFactory.newLatLngZoom(marker.getPosition(), zoom));
			}
		}
		this.lastSelectedUUID = newSelectedUUID; // marker clicked (OR cluster OR else)
		return false; // not handled
	}

	@Override
	public void onCameraChange(@NonNull CameraPosition cameraPosition) {
		this.showingMyLocation = this.showingMyLocation == null;
		notifyNewCameraPosition();
	}

	private void notifyNewCameraPosition() {
		final MapListener mapListener = this.mapListenerWR == null ? null : this.mapListenerWR.get();
		if (mapListener == null) {
			MTLog.d(this, "notifyNewCameraPosition() > SKIP (no listener)");
			return;
		}
		final ExtendedGoogleMap googleMap = getGoogleMapOrNull();
		if (googleMap == null) {
			MTLog.d(this, "notifyNewCameraPosition() > SKIP (no map)");
			return;
		}
		final VisibleRegion visibleRegion = googleMap.getProjection().getVisibleRegion();
		mapListener.onCameraChange(visibleRegion.latLngBounds);
	}

	public void onConfigurationChanged(@SuppressWarnings("unused") @NonNull Configuration newConfig) {
		notifyNewCameraPosition();
		applyMapStyle();
	}

	@Nullable
	public LatLngBounds getBigCameraPosition(@Nullable Activity activity, float factor) {
		if (activity == null) {
			return null;
		}
		final ExtendedGoogleMap googleMap = getGoogleMapOrNull();
		if (googleMap == null) {
			return null;
		}
		try {
			Point size = new Point();
			activity.getWindowManager().getDefaultDisplay().getSize(size);
			int width = size.x;
			int height = size.y;
			int bottom = (int) (height + factor * height);
			int left = (int) (0 - factor * width);
			int top = (int) (0 - factor * height);
			int right = (int) (width + factor * width);
			LatLng southWest = googleMap.getProjection().fromScreenLocation(new Point(left, bottom));
			LatLng northEast = googleMap.getProjection().fromScreenLocation(new Point(right, top));
			return new LatLngBounds(southWest, northEast);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while finding big camera position");
			return null;
		}
	}

	@Override
	public boolean onMyLocationButtonClick() {
		if (Boolean.TRUE.equals(this.showingMyLocation)) {
			showMarkers(true, false);
			this.showingMyLocation = false;
			return true; // handled
		}
		boolean handled = showClosestPOI();
		if (!handled) {
			handled = showDeviceLocation(true);
		}
		return handled;
	}

	private boolean showClosestPOI() {
		if (this.deviceLocation == null) {
			return false; // not handled
		}
		final MapMarkerProvider markerProvider = this.markerProviderWR == null ? null : this.markerProviderWR.get();
		final POIManager poim = markerProvider == null ? null : markerProvider.getClosestPOI();
		if (poim == null) {
			return false; // not handled
		}
		final LatLngBounds.Builder llb = LatLngBounds.builder();
		includeLocationAccuracyBounds(llb, this.deviceLocation);
		llb.include(POIMarker.getLatLng(poim));
		final Context context = getActivityOrNull();
		final boolean success = updateMapCamera(true,
				CameraUpdateFactory.newLatLngBounds(llb.build(), MapUtils.getMapWithButtonsCameraPaddingInPx(context))
		);
		this.showingMyLocation = null;
		return success; // handled or not
	}

	private boolean showMarkers(boolean anim, boolean includeDeviceLocation) {
		if (!this.mapLayoutReady) {
			return false;
		}
		if (!this.mapVisible) {
			return false;
		}
		final LatLngBounds.Builder llb = LatLngBounds.builder();
		final boolean markersFound = includeMarkersInLatLngBounds(llb);
		if (!markersFound) {
			return false; // not shown
		}
		if (includeDeviceLocation) {
			includeLocationAccuracyBounds(llb, this.deviceLocation);
		}
		final Context context = getActivityOrNull();
		final int paddingInPx;
		if (this.hasButtons) {
			paddingInPx = MapUtils.getMapWithButtonsCameraPaddingInPx(context);
		} else {
			paddingInPx = MapUtils.getMapWithoutButtonsCameraPaddingInPx(context);
		}
		try {
			final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(llb.build(), paddingInPx);
			return updateMapCamera(anim, cameraUpdate);
		} catch (Exception e) {
			//noinspection deprecation // FIXME
			CrashUtils.w(this, "Error while computing camera update to show markers!", e);
			return false;
		}
	}

	private static void includeLocationAccuracyBounds(@NonNull LatLngBounds.Builder llb, @Nullable Location location) {
		if (location == null) {
			return;
		}
		Location northEastBound = LocationUtils.computeOffset(location, location.getAccuracy(), LocationUtils.HEADING_NORTH_EAST);
		llb.include(LatLngUtils.fromLocation(northEastBound));
		Location southEastBound = LocationUtils.computeOffset(location, location.getAccuracy(), LocationUtils.HEADING_SOUTH_EAST);
		llb.include(LatLngUtils.fromLocation(southEastBound));
		Location southWestBound = LocationUtils.computeOffset(location, location.getAccuracy(), LocationUtils.HEADING_SOUTH_WEST);
		llb.include(LatLngUtils.fromLocation(southWestBound));
		Location northWestBound = LocationUtils.computeOffset(location, location.getAccuracy(), LocationUtils.HEADING_NORTH_WEST);
		llb.include(LatLngUtils.fromLocation(northWestBound));
	}

	private boolean includeMarkersInLatLngBounds(@NonNull LatLngBounds.Builder llb) {
		java.util.List<IMarker> markers = this.extendedGoogleMap == null ? null : this.extendedGoogleMap.getMarkers();
		if (markers != null && !markers.isEmpty()) {
			for (IMarker imarker : markers) {
				llb.include(imarker.getPosition());
			}
			return true;
		}
		MapMarkerProvider markerProvider = this.markerProviderWR == null ? null : this.markerProviderWR.get();
		if (markerProvider == null) {
			return false;
		}
		Collection<POIMarker> poiMarkers = markerProvider.getPOMarkers();
		if (poiMarkers != null) {
			for (POIMarker poiMarker : poiMarkers) {
				llb.include(poiMarker.position);
			}
			return true;
		}
		Collection<POIManager> pois = markerProvider.getPOIs();
		if (pois != null) {
			for (POIManager poim : pois) {
				llb.include(POIMarker.getLatLng(poim));
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	public boolean zoomIn() {
		return updateMapCamera(false, CameraUpdateFactory.zoomIn());
	}

	@SuppressWarnings("unused")
	public boolean zoomOut() {
		return updateMapCamera(false, CameraUpdateFactory.zoomOut());
	}

	private boolean showDeviceLocation(boolean anim) {
		if (!this.mapLayoutReady) {
			return false;
		}
		if (this.deviceLocation == null) {
			return false;
		}
		return updateMapCamera(anim,
				CameraUpdateFactory.newLatLngZoom(
						LatLngUtils.fromLocation(this.deviceLocation),
						DEVICE_LOCATION_ZOOM
				)
		);
	}

	private static final float DEVICE_LOCATION_ZOOM = MapUtils.MAP_ZOOM_LEVEL_STREETS_BUSY_BUSY;

	private boolean updateMapCamera(boolean anim, @NonNull CameraUpdate cameraUpdate) {
		final ExtendedGoogleMap googleMap = getGoogleMapOrNull();
		if (googleMap == null) {
			MTLog.d(this, "updateMapCamera() > SKIP (no map)");
			return false;
		}
		if (!this.mapLayoutReady) {
			MTLog.d(this, "updateMapCamera() > SKIP (map layout not ready)");
			return false;
		}
		try {
			if (anim) {
				googleMap.animateCamera(cameraUpdate);
			} else {
				googleMap.moveCamera(cameraUpdate);
			}
			return true;
		} catch (IllegalStateException ise) {
			MTLog.w(this, ise, "Illegal State Error while initializing map camera!");
			return false;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while initializing map camera!");
			return false;
		}
	}

	private void initMapMarkers() {
		if (this.clusterManagerItemsLoaded) {
			MTLog.d(this, "initMapMarkers() > SKIP (already loaded)");
			return;
		}
		if (this.extendedGoogleMap == null) {
			MTLog.d(this, "initMapMarkers() > SKIP (no map)");
			return;
		}
		//noinspection deprecation
		if (this.loadClusterItemsTask != null && this.loadClusterItemsTask.getStatus() == LoadClusterItemsTask.Status.RUNNING) {
			MTLog.d(this, "initMapMarkers() > SKIP (already running)");
			return;
		}
		this.loadClusterItemsTask = new LoadClusterItemsTask(this);
		TaskUtils.execute(this.loadClusterItemsTask);
	}

	private static final MarkerNameComparator MARKER_NAME_COMPARATOR = new MarkerNameComparator();

	private static class MarkerNameComparator implements Comparator<String> {

		private static final Pattern DIGITS = Pattern.compile("\\d+");

		@Override
		public int compare(String lhs, String rhs) {
			if (lhs.equals(rhs)) {
				return ComparatorUtils.SAME;
			}
			if (!TextUtils.isEmpty(lhs) && !TextUtils.isEmpty(rhs)) {
				int rDigits = -1;
				Matcher rMatcher = DIGITS.matcher(rhs);
				if (rMatcher.find()) {
					String rDigitS = rMatcher.group();
					if (!TextUtils.isEmpty(rDigitS)) {
						rDigits = Integer.parseInt(rDigitS);
					}
				}
				int lDigits = -1;
				Matcher lMatcher = DIGITS.matcher(lhs);
				if (lMatcher.find()) {
					String lDigitS = lMatcher.group();
					if (!TextUtils.isEmpty(lDigitS)) {
						lDigits = Integer.parseInt(lDigitS);
					}
				}
				if (rDigits != lDigits) {
					return lDigits - rDigits;
				}
			}
			return lhs.compareTo(rhs);
		}
	}

	public static class POIMarker implements MTLog.Loggable {

		private static final String LOG_TAG = MapViewController.class.getSimpleName() + ">" + POIMarker.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private LatLng position;
		@NonNull
		private final ArrayList<String> names = new ArrayList<>();
		@NonNull
		private final ArrayList<String> agencies = new ArrayList<>();
		@NonNull
		private final ArrayList<String> extras = new ArrayList<>();
		@Nullable
		@ColorInt
		private Integer color;
		@Nullable
		@ColorInt
		private Integer secondaryColor;
		@NonNull
		private final POIMarkerIds uuidsAndAuthority = new POIMarkerIds();

		public POIMarker(@NonNull LatLng position,
						 @Nullable String name,
						 @Nullable String agency,
						 @Nullable String extra,
						 @Nullable @ColorInt Integer color,
						 @Nullable @ColorInt Integer secondaryColor,
						 @NonNull String uuid,
						 @NonNull String authority) {
			this.position = position;
			addName(name);
			addAgency(agency);
			addExtras(extra);
			this.color = color;
			this.secondaryColor = secondaryColor;
			this.uuidsAndAuthority.put(uuid, authority);
		}

		private static final String AROUND_TRUNC = "%.6g";

		private static double truncAround(double loc) {
			return Double.parseDouble(String.format(Locale.US, AROUND_TRUNC, loc));
		}

		@NonNull
		public static LatLng getLatLng(@NonNull POIManager poim) {
			return new LatLng(poim.poi.getLat(), poim.poi.getLng());
		}

		@NonNull
		public static LatLng getLatLngTrunc(@NonNull POIManager poim) {
			return getLatLngTrunc(poim.poi.getLat(), poim.poi.getLng());
		}

		@NonNull
		public static LatLng getLatLngTrunc(double lat, double lng) {
			return new LatLng(POIMarker.truncAround(lat), POIMarker.truncAround(lng));
		}

		private static final String SLASH = " / ";

		@NonNull
		public String getTitle() {
			StringBuilder sb = new StringBuilder();
			CollectionUtils.sort(this.names, MARKER_NAME_COMPARATOR);
			for (String name : this.names) {
				if (sb.length() > 0) {
					sb.append(SLASH);
				}
				if (sb.length() == 0 || !sb.toString().contains(name)) {
					sb.append(name);
				}
			}
			return sb.toString();
		}

		private static final String P1 = "(";
		private static final String P2 = ")";

		String getSnippet() {
			StringBuilder sb = new StringBuilder();
			boolean hasExtras = false;
			CollectionUtils.sort(this.extras, MARKER_NAME_COMPARATOR);
			for (int e = 0; e < this.extras.size(); e++) {
				String extra = this.extras.get(e);
				if (hasExtras) {
					sb.append(SLASH);
				}
				if (!hasExtras || !sb.toString().contains(extra)) {
					sb.append(extra);
					hasExtras = true;
				}
			}
			boolean hasAgencies = false;
			CollectionUtils.sort(this.agencies, MARKER_NAME_COMPARATOR);
			for (int a = 0; a < this.agencies.size(); a++) {
				String agency = this.agencies.get(a);
				// if (sb.length() > 0) {
				if (hasAgencies) {
					sb.append(SLASH);
				} else if (hasExtras) {
					sb.append(StringUtils.SPACE_CAR).append(P1);
				}
				if (!hasAgencies || !sb.toString().contains(agency)) {
					sb.append(agency);
					hasAgencies = true;
				}
			}
			if (hasExtras && hasAgencies) {
				sb.append(P2);
			}
			return sb.toString();
		}

		@NonNull
		POIMarkerIds getUuidsAndAuthority() {
			return uuidsAndAuthority;
		}

		boolean hasUUID(String uuid) {
			return this.uuidsAndAuthority.hasUUID(uuid);
		}

		public void merge(@NonNull POIMarker poiMarker) {
			addPosition(poiMarker.position);
			for (String name : poiMarker.names) {
				addName(name);
			}
			for (String agency : poiMarker.agencies) {
				addAgency(agency);
			}
			for (String extra : poiMarker.extras) {
				addExtras(extra);
			}
			if (this.color != null) {
				if (poiMarker.color == null || !this.color.equals(poiMarker.color)) {
					this.color = null;
				}
			}
			if (this.secondaryColor != null) {
				if (poiMarker.secondaryColor == null || !this.secondaryColor.equals(poiMarker.secondaryColor)) {
					this.secondaryColor = null;
				}
			}
			this.uuidsAndAuthority.merge(poiMarker.uuidsAndAuthority);
		}

		public void merge(@NonNull LatLng position,
						  @Nullable String name,
						  @Nullable String agency,
						  @Nullable String extra,
						  @Nullable @ColorInt Integer color,
						  @Nullable @ColorInt Integer secondaryColor,
						  @NonNull String uuid,
						  @NonNull String authority) {
			addPosition(position);
			addName(name);
			addAgency(agency);
			addExtras(extra);
			if (this.color != null) {
				if (!this.color.equals(color)) {
					this.color = null;
				}
			}
			if (this.secondaryColor != null) {
				if (!this.secondaryColor.equals(secondaryColor)) {
					this.secondaryColor = null;
				}
			}
			this.uuidsAndAuthority.put(uuid, authority);
		}

		private void addPosition(@NonNull LatLng position) {
			this.position = new LatLng(
					(this.position.latitude + position.latitude) / 2d,
					(this.position.longitude + position.longitude) / 2d);
		}

		@NonNull
		public LatLng getPosition() {
			return position;
		}

		private void addExtras(@Nullable String extra) {
			if (!TextUtils.isEmpty(extra)) {
				if (!this.extras.contains(extra)) {
					this.extras.add(extra);
				}
			}
		}

		private void addAgency(@Nullable String agency) {
			if (!TextUtils.isEmpty(agency)) {
				if (!this.agencies.contains(agency)) {
					this.agencies.add(agency);
				}
			}
		}

		private void addName(@Nullable String name) {
			if (!TextUtils.isEmpty(name)) {
				if (!this.names.contains(name)) {
					this.names.add(name);
				}
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			POIMarker poiMarker = (POIMarker) o;

			if (!position.equals(poiMarker.position)) return false;
			if (!names.equals(poiMarker.names)) return false;
			if (!agencies.equals(poiMarker.agencies)) return false;
			if (!extras.equals(poiMarker.extras)) return false;
			if (!Objects.equals(color, poiMarker.color)) return false;
			if (!Objects.equals(secondaryColor, poiMarker.secondaryColor)) return false;
			return uuidsAndAuthority.equals(poiMarker.uuidsAndAuthority);
		}

		@Override
		public int hashCode() {
			int result = 0;
			result = 31 * result + position.hashCode();
			result = 31 * result + names.hashCode();
			result = 31 * result + agencies.hashCode();
			result = 31 * result + extras.hashCode();
			result = 31 * result + (color != null ? color.hashCode() : 0);
			result = 31 * result + (secondaryColor != null ? secondaryColor.hashCode() : 0);
			result = 31 * result + uuidsAndAuthority.hashCode();
			return result;
		}

		@NonNull
		@Override
		public String toString() {
			return POIMarker.class.getSimpleName() + "{" +
					"position=" + position +
					", names=" + names +
					", agencies=" + agencies +
					", extras=" + extras +
					", color=" + color +
					", secondaryColor=" + secondaryColor +
					", uuidsAndAuthority=" + uuidsAndAuthority +
					'}';
		}
	}

	private static class POIMarkerIds implements MTLog.Loggable {

		private static final String TAG = MapViewController.class.getSimpleName() + ">" + POIMarkerIds.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		java.util.Set<ArrayMap.Entry<String, String>> entrySet() {
			return this.uuidsAndAuthority.entrySet();
		}

		@NonNull
		public ArrayMap<String, String> getMap() {
			return this.uuidsAndAuthority;
		}

		boolean hasUUID(String uuid) {
			return this.uuidsAndAuthority.containsKey(uuid);
		}

		@NonNull
		private final ArrayMap<String, String> uuidsAndAuthority = new ArrayMap<>();

		public void put(String uuid, String authority) {
			try {
				this.uuidsAndAuthority.put(uuid, authority);
			} catch (Exception e) {
				//noinspection deprecation // FIXME
				CrashUtils.w(this, e, "Error while adding POI marker ID %s:%s", uuid, authority);
			}
		}

		void putAll(SimpleArrayMap<String, String> newUuidsAndAuthority) {
			if (newUuidsAndAuthority != null) {
				this.uuidsAndAuthority.putAll(newUuidsAndAuthority);
			}
		}

		void merge(POIMarkerIds poiMarkerIds) {
			if (poiMarkerIds != null) {
				putAll(poiMarkerIds.getMap());
			}
		}

		public int size() {
			return this.uuidsAndAuthority.size();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			POIMarkerIds that = (POIMarkerIds) o;

			return uuidsAndAuthority.equals(that.uuidsAndAuthority);
		}

		@Override
		public int hashCode() {
			return uuidsAndAuthority.hashCode();
		}

		@NonNull
		@Override
		public String toString() {
			return POIMarkerIds.class.getSimpleName() + "{" +
					"uuidsAndAuthority=" + uuidsAndAuthority +
					'}';
		}
	}

	@Nullable
	private LoadClusterItemsTask loadClusterItemsTask = null;

	@SuppressWarnings("deprecation")
	private static class LoadClusterItemsTask extends MTCancellableAsyncTask<Void, Void, Collection<POIMarker>> {

		private final String LOG_TAG = MapViewController.class.getSimpleName() + ">" + LoadClusterItemsTask.class.getSimpleName();

		@NonNull
		private final WeakReference<MapViewController> mapViewControllerWR;

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private LoadClusterItemsTask(MapViewController mapViewController) {
			this.mapViewControllerWR = new WeakReference<>(mapViewController);
		}

		@WorkerThread
		@Override
		protected Collection<POIMarker> doInBackgroundNotCancelledMT(Void... params) {
			MapViewController mapViewController = this.mapViewControllerWR.get();
			if (mapViewController == null) {
				return null;
			}
			MapMarkerProvider markerProvider = mapViewController.markerProviderWR == null ? null : mapViewController.markerProviderWR.get();
			if (markerProvider == null) {
				return null;
			}
			Collection<POIMarker> poiMarkers = markerProvider.getPOMarkers();
			if (poiMarkers != null) {
				return poiMarkers;
			}
			Collection<POIManager> pois = markerProvider.getPOIs();
			if (pois == null) {
				return null;
			}
			DataSourcesRepository dataSourcesRepository = mapViewController.dataSourcesRepository;
			if (dataSourcesRepository == null) {
				return null;
			}
			ArrayMap<LatLng, POIMarker> clusterItems = new ArrayMap<>();
			LatLng position;
			LatLng positionTrunc;
			String name;
			String agencyShortName;
			String extra;
			String uuid;
			String authority;
			Integer color;
			Integer secondaryColor;
			IAgencyUIProperties agency;
			for (POIManager poim : pois) {
				position = POIMarker.getLatLng(poim);
				positionTrunc = POIMarker.getLatLngTrunc(poim);
				name = poim.poi.getName();
				extra = null;
				agency = dataSourcesRepository.getAgency(poim.poi.getAuthority());
				if (agency == null) {
					continue;
				}
				if (mapViewController.markerLabelShowExtra && poim.poi instanceof RouteTripStop) {
					extra = ((RouteTripStop) poim.poi).getRoute().getShortestName();
				}
				agencyShortName = mapViewController.markerLabelShowExtra ? agency.getShortName() : null;
				uuid = poim.poi.getUUID();
				authority = poim.poi.getAuthority();
				color = poim.getColor(dataSourcesRepository);
				secondaryColor = agency.getColorInt();
				POIMarker currentItem = clusterItems.get(positionTrunc);
				if (currentItem == null) {
					currentItem = new POIMarker(position, name, agencyShortName, extra, color, secondaryColor, uuid, authority);
				} else {
					currentItem.merge(position, name, agencyShortName, extra, color, secondaryColor, uuid, authority);
				}
				try {
					clusterItems.put(positionTrunc, currentItem);
				} catch (ClassCastException cce) {
					CrashUtils.w(this, "ClassCastException while loading cluster items!", cce);
					return null;
				}
			}
			return clusterItems.values();
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledMT(@Nullable Collection<POIMarker> result) {
			MapViewController mapViewController = this.mapViewControllerWR.get();
			if (mapViewController == null) {
				MTLog.d(this, "clearClusterManagerItems() > SKIP (no controller)");
				return;
			}
			if (result == null) {
				MTLog.d(this, "clearClusterManagerItems() > SKIP (no result)");
				return;
			}
			if (mapViewController.extendedGoogleMap == null) {
				MTLog.d(this, "clearClusterManagerItems() > SKIP (no extended map)");
				return;
			}
			if (mapViewController.mapView == null) {
				MTLog.d(this, "clearClusterManagerItems() > SKIP (no map)");
				return;
			}
			ExtendedMarkerOptions options = new ExtendedMarkerOptions();
			final Context context = mapViewController.mapView.getContext();
			for (POIMarker poiMarker : result) {
				options.position(poiMarker.position);
				options.title(poiMarker.getTitle());
				options.snippet(mapViewController.markerLabelShowExtra ? poiMarker.getSnippet() : null);
				options.icon(context, getPlaceIconRes(), poiMarker.color, poiMarker.secondaryColor, Color.BLACK);
				options.data(poiMarker.getUuidsAndAuthority());
				mapViewController.extendedGoogleMap.addMarker(options);
			}
			mapViewController.clusterManagerItemsLoaded = true;
			mapViewController.hideLoading();
			if (mapViewController.showAllMarkersWhenReady) {
				mapViewController.showMarkers(false, mapViewController.followingDevice);
			}
		}
	}

	@DrawableRes
	private static int getPlaceIconRes() {
		return R.drawable.map_icon_place_white_slim_original;
	}

	public boolean addMarkers(@Nullable Collection<POIMarker> poiMarkers) {
		final Context context = getActivityOrNull();
		if (context == null) {
			MTLog.d(this, "addMarkers() > SKIP (no context)");
			return false;
		}
		return addMarkers(context, poiMarkers);
	}

	public boolean addMarkers(@NonNull Context context, @Nullable Collection<POIMarker> poiMarkers) {
		if (MapViewController.this.extendedGoogleMap == null) {
			MTLog.d(this, "addMarkers() > SKIP (no map)");
			return false;
		}
		final ExtendedMarkerOptions options = new ExtendedMarkerOptions();
		if (poiMarkers != null) {
			for (POIMarker poiMarker : poiMarkers) {
				options.position(poiMarker.position);
				options.title(poiMarker.getTitle());
				options.snippet(this.markerLabelShowExtra ? poiMarker.getSnippet() : null);
				options.icon(context, getPlaceIconRes(), poiMarker.color, poiMarker.secondaryColor, Color.BLACK);
				options.data(poiMarker.getUuidsAndAuthority());
				final IMarker marker = this.extendedGoogleMap.addMarker(options);
				if (poiMarker.hasUUID(this.lastSelectedUUID)) {
					marker.showInfoWindow();
					this.lastSelectedUUID = null; // select once only
				}
			}
		}
		this.clusterManagerItemsLoaded = true;
		hideLoading();
		if (this.showAllMarkersWhenReady) {
			showMarkers(false, this.followingDevice);
		}
		return true;
	}

	@Override
	public void activate(@NonNull OnLocationChangedListener onLocationChangedListener) {
		this.locationChangedListener = onLocationChangedListener;
		if (this.deviceLocation != null) {
			this.locationChangedListener.onLocationChanged(this.deviceLocation);
		}
	}

	@Override
	public void deactivate() {
		this.locationChangedListener = null;
	}

	public void onDeviceLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		final boolean firstLocation = this.deviceLocation == null;
		if (this.deviceLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.deviceLocation, newLocation)) {
			this.deviceLocation = newLocation;
			if (this.locationChangedListener != null) {
				this.locationChangedListener.onLocationChanged(newLocation);
			}
			if (this.followingDevice) {
				showMarkers(true, true);
			} else if (firstLocation) {
				this.initialMapCameraSetup = false;
				setupInitialCamera();
			}
		}
	}

	private boolean needToResumeMap = false;

	public boolean onResume() {
		this.needToResumeMap = true;
		final MapView mapView = getMapViewOrNull();
		if (mapView == null) {
			MTLog.d(this, "onResume() > SKIP (no map)");
			return false;
		}
		mapView.onResume();
		this.needToResumeMap = false;
		showMapInternal(null);
		setMapType(getMapType());
		return true; // resumed
	}

	private boolean showLastCameraPosition() {
		if (this.lastCameraPosition == null) {
			return false; // nothing shown
		}
		final boolean success = updateMapCamera(false, CameraUpdateFactory.newCameraPosition(this.lastCameraPosition));
		if (success) {
			this.lastCameraPosition = null; // clear
		}
		return success;
	}

	private void setActivity(@NonNull Activity activity) {
		this.activityWR = new WeakReference<>(activity);
	}

	@Nullable
	private Activity getActivityOrNull() {
		return this.activityWR == null ? null : this.activityWR.get();
	}

	private void clearActivity() {
		if (this.activityWR != null) {
			this.activityWR.clear();
			this.activityWR = null;
		}
	}

	public void onPause() {
		this.needToResumeMap = false;
		final MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			mapView.onPause();
		}
		final ExtendedGoogleMap googleMap = getGoogleMapOrNull();
		if (googleMap != null) {
			this.lastCameraPosition = googleMap.getCameraPosition();
		}
		resetMapType();
	}

	public void setInitialSelectedUUID(@Nullable String uuid) {
		if (TextUtils.isEmpty(uuid)) {
			return;
		}
		this.lastSelectedUUID = uuid; // initial
	}

	public void setInitialLocation(@Nullable Location initialLocation) {
		if (initialLocation == null) {
			return;
		}
		this.lastCameraPosition = CameraPosition.builder() //
				.target(LatLngUtils.fromLocation(initialLocation)) //
				.zoom(DEVICE_LOCATION_ZOOM) //
				.build();
	}

	public void onSaveInstanceState(@NonNull Bundle outState) {
		final MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			mapView.onSaveInstanceState(outState);
		}
		if (this.lastCameraPosition != null) {
			outState.putParcelable(EXTRA_LAST_CAMERA_POSITION, this.lastCameraPosition);
		}
		if (this.lastSelectedUUID != null) {
			outState.putString(EXTRA_LAST_SELECTED_UUID, this.lastSelectedUUID);
		}
	}

	public void notifyMarkerChanged(@Nullable MapMarkerProvider markerProvider) {
		if (markerProvider == null) {
			return;
		}
		setMarkerProvider(markerProvider);
		clearMarkers();
	}

	public void clearMarkers() {
		clearClusterManagerItems();
	}

	public void onLowMemory() {
		final MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			mapView.onLowMemory();
		}
	}

	public void onDetach() {
		clearActivity();
	}

	public void onDestroyView() {
		destroyMapView();
		this.mapVisible = false;
		this.waitingForGlobalLayout = false;
		this.mapLayoutReady = false;
		destroyGoogleMap();
		this.initialMapCameraSetup = false;
		this.initializingMapView.set(false);
		TaskUtils.cancelQuietly(this.loadClusterItemsTask, true);
		this.lastSavedInstanceState = null;
		clearMarkers();
	}

	private void clearClusterManagerItems() {
		if (this.extendedGoogleMap != null) {
			this.extendedGoogleMap.clear();
		}
		this.clusterManagerItemsLoaded = false;
	}

	public void onDestroy() {
		clearActivity();
		if (this.markerProviderWR != null) {
			this.markerProviderWR.clear();
		}
	}

	public interface MapMarkerProvider {

		@Nullable
		Collection<POIMarker> getPOMarkers();

		@Nullable
		Collection<POIManager> getPOIs();

		@Nullable
		POIManager getClosestPOI();

		@SuppressWarnings("unused")
		@Nullable
		POIManager getPOI(@Nullable String uuid);
	}

	public interface MapListener {

		void onMapClick(@NonNull LatLng position);

		void onCameraChange(@NonNull LatLngBounds latLngBounds);

		void onMapReady();
	}
}
