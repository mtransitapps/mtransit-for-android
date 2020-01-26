package org.mtransit.android.ui.view;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.ResourceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.PickPOIDialogFragment;
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

public class MapViewController implements ExtendedGoogleMap.OnCameraChangeListener, ExtendedGoogleMap.OnInfoWindowClickListener,
		ExtendedGoogleMap.OnMapLoadedCallback, ExtendedGoogleMap.OnMarkerClickListener, ExtendedGoogleMap.OnMyLocationButtonClickListener,
		ExtendedGoogleMap.OnMapClickListener, LocationSource, OnMapReadyCallback, ViewTreeObserver.OnGlobalLayoutListener, MTLog.Loggable {

	private static final String LOG_TAG = MapViewController.class.getSimpleName();

	@Nullable
	private String tag;

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG + "-" + this.tag;
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
	private Location userLocation;
	private boolean waitingForGlobalLayout = false;

	@Override
	public void onGlobalLayout() {
		this.waitingForGlobalLayout = false;
		MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			SupportFactory.get().removeOnGlobalLayoutListener(mapView.getViewTreeObserver(), this);
			this.mapLayoutReady = true;
			showMapInternal(null);
		}
	}

	private boolean initialMapCameraSetup = false;

	private void setupInitialCamera() {
		if (this.initialMapCameraSetup) {
			return;
		}
		if (!this.mapLayoutReady || !hasMapView() || !hasGoogleMap()) {
			return;
		}
		if (!this.mapVisible) {
			return;
		}
		this.initialMapCameraSetup = showLastCameraPosition();
		if (!this.initialMapCameraSetup) {
			this.initialMapCameraSetup = showMarkers(false, this.followingUser);
		}
		if (!this.initialMapCameraSetup) {
			this.initialMapCameraSetup = showUserLocation(false);
		}
	}

	private WeakReference<MapMarkerProvider> markerProviderWR;
	private WeakReference<MapListener> mapListenerWR;
	private boolean mapToolbarEnabled;
	private boolean myLocationEnabled;
	private boolean myLocationButtonEnabled;
	private boolean indoorLevelPickerEnabled;
	private boolean trafficEnabled;
	private boolean indoorEnabled;
	private int paddingTopSp;
	private boolean followingUser;
	private boolean hasButtons;
	private boolean clusteringEnabled;
	private boolean showAllMarkersWhenReady;
	private boolean markerLabelShowExtra;

	private CameraPosition lastCameraPosition;

	private String lastSelectedUUID;

	private boolean locationPermissionGranted = false;

	public MapViewController(String tag, MapMarkerProvider markerProvider, MapListener mapListener, boolean mapToolbarEnabled, boolean myLocationEnabled,
							 boolean myLocationButtonEnabled, boolean indoorLevelPickerEnabled, boolean trafficEnabled, boolean indoorEnabled, int paddingTopSp,
							 boolean followingUser, boolean hasButtons, boolean clusteringEnabled, boolean showAllMarkersWhenReady, boolean markerLabelShowExtra) {
		this.tag = tag;
		setMarkerProvider(markerProvider);
		setMapListener(mapListener);
		this.mapToolbarEnabled = mapToolbarEnabled;
		this.myLocationEnabled = myLocationEnabled;
		this.myLocationButtonEnabled = myLocationButtonEnabled;
		this.indoorLevelPickerEnabled = indoorLevelPickerEnabled;
		this.trafficEnabled = trafficEnabled;
		this.indoorEnabled = indoorEnabled;
		this.paddingTopSp = paddingTopSp;
		this.followingUser = followingUser;
		this.hasButtons = hasButtons;
		this.clusteringEnabled = clusteringEnabled;
		this.showAllMarkersWhenReady = showAllMarkersWhenReady;
		this.markerLabelShowExtra = markerLabelShowExtra;
	}

	private void setMarkerProvider(MapMarkerProvider markerProvider) {
		this.markerProviderWR = new WeakReference<>(markerProvider);
	}

	private void setMapListener(MapListener mapListener) {
		this.mapListenerWR = new WeakReference<>(mapListener);
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void onAttach(Activity activity) {
		setActivity(activity);
	}

	public void onCreate(Bundle savedInstanceState) {
		restoreInstanceState(savedInstanceState);
		this.lastSavedInstanceState = savedInstanceState;
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		CameraPosition newLastCameraPosition = BundleUtils.getParcelable(EXTRA_LAST_CAMERA_POSITION, savedInstanceState);
		if (newLastCameraPosition != null) {
			this.lastCameraPosition = newLastCameraPosition;
		}
		String newLastSelectedUUID = BundleUtils.getString(EXTRA_LAST_SELECTED_UUID, savedInstanceState);
		if (!TextUtils.isEmpty(newLastSelectedUUID)) {
			this.lastSelectedUUID = newLastSelectedUUID;
		}
	}

	public void onCreateView(View view, Bundle savedInstanceState) {
		this.lastSavedInstanceState = savedInstanceState;
	}

	public void onViewCreated(View view, Bundle savedInstanceState) {
		this.lastSavedInstanceState = savedInstanceState;
	}

	private Bundle lastSavedInstanceState = null;

	private boolean hasMapView() {
		return this.mapView != null;
	}

	private MapView getMapViewOrNull() {
		return this.mapView;
	}

	private MapView getMapViewOrNull(View view) {
		if (this.mapView == null) {
			initMapViewAsync(view);
		}
		return this.mapView;
	}

	private void initMapViewAsync(View view) {
		if (this.initMapViewTask != null && this.initMapViewTask.getStatus() == InitMapViewTask.Status.RUNNING) {
			return;
		}
		if (view == null) {
			return;
		}
		this.initMapViewTask = new InitMapViewTask(this, view);
		TaskUtils.execute(this.initMapViewTask);
	}

	@Nullable
	private InitMapViewTask initMapViewTask = null;

	private static class InitMapViewTask extends MTAsyncTask<Object, Void, Boolean> {

		@Override
		public String getLogTag() {
			return MapViewController.class.getSimpleName() + ">" + InitMapViewTask.class.getSimpleName();
		}

		@NonNull
		private final WeakReference<MapViewController> mapViewControllerWR;

		@NonNull
		private final WeakReference<View> viewWR;

		InitMapViewTask(MapViewController mapViewController, View view) {
			this.mapViewControllerWR = new WeakReference<>(mapViewController);
			this.viewWR = new WeakReference<>(view);
		}

		@Override
		protected Boolean doInBackgroundMT(Object... params) {
			View view = this.viewWR.get();
			if (view == null) {
				return false;
			}
			try {
				MapsInitializer.initialize(view.getContext());
				return true;
			} catch (Exception e) {
				MTLog.w(this, e, "Error while initializing map!");
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			MapViewController mapViewController = this.mapViewControllerWR.get();
			if (mapViewController == null) {
				return;
			}
			if (result) {
				View view = this.viewWR.get();
				mapViewController.applyNewMapView(view);
			}
		}
	}

	private void applyNewMapView(@Nullable View view) {
		if (this.mapView != null) {
			return;
		}
		if (view == null) {
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
		hideShowLoading();
		initTypeSwitch();
	}

	private void destroyMapView() {
		if (this.mapView != null) {
			this.mapView.onDestroy();
			this.mapView = null;
		}
	}

	public void onActivityCreated(Bundle savedInstanceState) {
	}

	private boolean hasGoogleMap() {
		return this.extendedGoogleMap != null;
	}

	private void destroyGoogleMap() {
		this.extendedGoogleMap = null;
	}

	private ExtendedGoogleMap getGoogleMapOrNull() {
		return this.extendedGoogleMap;
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		setupGoogleMap(googleMap);
	}

	private boolean clusterManagerItemsLoaded = false;

	private void setupGoogleMap(GoogleMap googleMap) {
		if (googleMap == null) {
			return;
		}
		this.extendedGoogleMap = ExtendedMapFactory.create(googleMap, getActivityOrNull());
		applyMapType();
		applyMapStyle();
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
		if (this.paddingTopSp > 0) {
			Context context = getActivityOrNull();
			int paddingTop = (int) ResourceUtils.convertSPtoPX(context, this.paddingTopSp); // action bar
			this.extendedGoogleMap.setPadding(0, paddingTop, 0, 0);
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

	private void hideShowLoading() {
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
		this.typeSwitchView.setOnClickListener(new MTOnClickListener() {
			@Override
			public void onClickMT(View view) {
				switchMapType();
			}
		});
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
		PreferenceUtils.savePrefLcl(getActivityOrNull(), MapUtils.PREFS_LCL_MAP_TYPE, this.mapType, false); // asynchronous
	}

	private void applyMapType() {
		ExtendedGoogleMap map = getGoogleMapOrNull();
		if (map != null) {
			map.setMapType(getMapType());
		}
	}

	private void applyMapStyle() {
		ExtendedGoogleMap map = getGoogleMapOrNull();
		if (map == null) {
			return;
		}
		Context context = getActivityOrNull();
		if (context == null) {
			return;
		}
		map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle));
	}

	private void setTypeSwitchImg() {
		if (this.typeSwitchView != null) {
			this.typeSwitchView.setImageResource(getMapType() == MapUtils.MAP_TYPE_NORMAL ? R.drawable.ic_map_satellite : R.drawable.ic_map_normal);
		}
	}

	public boolean showMap(View view) {
		this.mapVisible = true;
		return showMapInternal(view);
	}

	private boolean showMapInternal(@Nullable View optView) {
		MapView mapView = getMapViewOrNull(optView);
		if (mapView == null) {
			return false; // not shown
		}
		if (!this.mapVisible) {
			return false; // not shown
		}
		hideShowLoading();
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
		MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			if (mapView.getVisibility() == View.VISIBLE) {
				mapView.setVisibility(View.INVISIBLE); // hide
			}
		}
		hideTypeSwitch();
	}

	@Override
	public void onMapLoaded() {
	}

	@Override
	public void onMapClick(LatLng position) {
		MapListener mapListener = this.mapListenerWR == null ? null : this.mapListenerWR.get();
		if (mapListener != null) {
			mapListener.onMapClick(position);
		}
	}

	@Override
	public void onInfoWindowClick(IMarker imarker) {
		if (imarker != null && imarker.getData() != null && imarker.getData() instanceof POIMarkerIds) {
			POIMarkerIds poiMarkerIds = imarker.getData();
			if (poiMarkerIds.size() >= 1) {
				Activity activity = getActivityOrNull();
				if (activity instanceof MainActivity) {
					FragmentUtils.replaceDialogFragment((MainActivity) activity, FragmentUtils.DIALOG_TAG, //
							PickPOIDialogFragment.newInstance(poiMarkerIds.getMap()), //
							null);
				}
			}
		}
	}

	private static final float MARKER_ZOOM_INC = 2.0f;

	@Override
	public boolean onMarkerClick(IMarker imarker) {
		if (imarker != null && !imarker.isCluster() && imarker.getData() != null && imarker.getData() instanceof POIMarkerIds) {
			POIMarkerIds poiMarkerIds = imarker.getData();
			ArrayMap.Entry<String, String> uuidAndAuthority = poiMarkerIds.entrySet().iterator().next();
			String uuid = uuidAndAuthority.getKey();
			if (!TextUtils.isEmpty(uuid)) {
				this.lastSelectedUUID = uuid;
			}
		} else if (imarker != null && imarker.isCluster()) {
			if (this.extendedGoogleMap != null) {
				float zoom = this.extendedGoogleMap.getCameraPosition().zoom + MARKER_ZOOM_INC;
				return updateMapCamera(true, CameraUpdateFactory.newLatLngZoom(imarker.getPosition(), zoom));
			}
		}
		return false; // not handled
	}

	@Override
	public void onCameraChange(CameraPosition cameraPosition) {
		this.showingMyLocation = this.showingMyLocation == null;
		notifyNewCameraPosition();
	}

	private void notifyNewCameraPosition() {
		MapListener mapListener = this.mapListenerWR == null ? null : this.mapListenerWR.get();
		if (mapListener == null) {
			return;
		}
		ExtendedGoogleMap googleMap = getGoogleMapOrNull();
		if (googleMap == null) {
			return;
		}
		VisibleRegion visibleRegion = googleMap.getProjection().getVisibleRegion();
		mapListener.onCameraChange(visibleRegion.latLngBounds);
	}

	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		notifyNewCameraPosition();
		applyMapStyle();
	}

	public LatLngBounds getBigCameraPosition(Activity activity, float factor) {
		ExtendedGoogleMap googleMap = getGoogleMapOrNull();
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
			handled = showUserLocation(true);
		}
		return handled;
	}

	private boolean showClosestPOI() {
		if (this.userLocation == null) {
			return false; // not handled
		}
		MapMarkerProvider markerProvider = this.markerProviderWR == null ? null : this.markerProviderWR.get();
		POIManager poim = markerProvider == null ? null : markerProvider.getClosestPOI();
		if (poim == null) {
			return false; // not handled
		}
		LatLngBounds.Builder llb = LatLngBounds.builder();
		includeLocationAccuracyBounds(llb, this.userLocation);
		llb.include(POIMarker.getLatLng(poim));
		Context context = getActivityOrNull();
		boolean success = updateMapCamera(true, CameraUpdateFactory.newLatLngBounds(llb.build(), MapUtils.getMapWithButtonsCameraPaddingInPx(context)));
		this.showingMyLocation = null;
		return success; // handled or not
	}

	private boolean showMarkers(boolean anim, boolean includeUserLocation) {
		if (!this.mapLayoutReady) {
			return false;
		}
		if (!this.mapVisible) {
			return false;
		}
		LatLngBounds.Builder llb = LatLngBounds.builder();
		boolean markersFound = includeMarkersInLatLngBounds(llb);
		if (!markersFound) {
			return false; // not shown
		}
		if (includeUserLocation) {
			includeLocationAccuracyBounds(llb, this.userLocation);
		}
		Context context = getActivityOrNull();
		int paddingInPx;
		if (this.hasButtons) {
			paddingInPx = MapUtils.getMapWithButtonsCameraPaddingInPx(context);
		} else {
			paddingInPx = MapUtils.getMapWithoutButtonsCameraPaddingInPx(context);
		}
		try {
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(llb.build(), paddingInPx);
			return updateMapCamera(anim, cameraUpdate);
		} catch (Exception e) {
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
		if (markers != null && markers.size() > 0) {
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

	private boolean showUserLocation(boolean anim) {
		if (!this.mapLayoutReady) {
			return false;
		}
		if (this.userLocation == null) {
			return false;
		}
		return updateMapCamera(anim, CameraUpdateFactory.newLatLngZoom( //
				LatLngUtils.fromLocation(this.userLocation), //
				USER_LOCATION_ZOOM) //
		);
	}

	private static final float USER_LOCATION_ZOOM = 17f;

	private boolean updateMapCamera(boolean anim, CameraUpdate cameraUpdate) {
		ExtendedGoogleMap googleMap = getGoogleMapOrNull();
		if (googleMap == null) {
			return false;
		}
		if (!this.mapLayoutReady) {
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
			return;
		}
		if (this.extendedGoogleMap == null) {
			return;
		}
		if (this.loadClusterItemsTask != null && this.loadClusterItemsTask.getStatus() == LoadClusterItemsTask.Status.RUNNING) {
			return;
		}
		this.loadClusterItemsTask = new LoadClusterItemsTask(this);
		TaskUtils.execute(this.loadClusterItemsTask);
	}

	private static final MarkerNameComparator MARKER_NAME_COMPARATOR = new MarkerNameComparator();

	private static class MarkerNameComparator implements Comparator<String> {

		private static final Pattern DIGITS = Pattern.compile("[\\d]+");

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

		private static final String TAG = MapViewController.class.getSimpleName() + ">" + POIMarker.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private LatLng position;
		private ArrayList<String> names = new ArrayList<>();
		private ArrayList<String> agencies = new ArrayList<>();
		private ArrayList<String> extras = new ArrayList<>();
		private Integer color;
		private Integer secondaryColor;
		private POIMarkerIds uuidsAndAuthority = new POIMarkerIds();

		public POIMarker(LatLng position, String name, String agency, String extra, Integer color, Integer secondaryColor, String uuid, String authority) {
			addPosition(position);
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

		public static LatLng getLatLng(POIManager poim) {
			return new LatLng(poim.poi.getLat(), poim.poi.getLng());
		}

		public static LatLng getLatLngTrunc(POIManager poim) {
			return new LatLng(POIMarker.truncAround(poim.poi.getLat()), POIMarker.truncAround(poim.poi.getLng()));
		}

		private static final String SLASH = " / ";

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

		public String getSnippet() {
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

		public POIMarkerIds getUuidsAndAuthority() {
			return uuidsAndAuthority;
		}

		public boolean hasUUID(String uuid) {
			return this.uuidsAndAuthority.hasUUID(uuid);
		}

		public void merge(POIMarker poiMarker) {
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

		public void merge(LatLng position, String name, String agency, String extra, Integer color, Integer secondaryColor, String uuid, String authority) {
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

		private void addPosition(LatLng position) {
			if (this.position == null) {
				this.position = position;
			} else {
				this.position = new LatLng( //
						(this.position.latitude + position.latitude) / 2d, //
						(this.position.longitude + position.longitude) / 2d);
			}
		}

		private void addExtras(String extra) {
			if (!TextUtils.isEmpty(extra)) {
				if (!this.extras.contains(extra)) {
					this.extras.add(extra);
				}
			}
		}

		private void addAgency(String agency) {
			if (!TextUtils.isEmpty(agency)) {
				if (!this.agencies.contains(agency)) {
					this.agencies.add(agency);
				}
			}
		}

		private void addName(String name) {
			if (!TextUtils.isEmpty(name)) {
				if (!this.names.contains(name)) {
					this.names.add(name);
				}
			}
		}
	}

	private static class POIMarkerIds implements MTLog.Loggable {

		private static final String TAG = MapViewController.class.getSimpleName() + ">" + POIMarkerIds.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		public java.util.Set<ArrayMap.Entry<String, String>> entrySet() {
			return this.uuidsAndAuthority.entrySet();
		}

		public ArrayMap<String, String> getMap() {
			return this.uuidsAndAuthority;
		}

		public boolean hasUUID(String uuid) {
			return this.uuidsAndAuthority.containsKey(uuid);
		}

		private ArrayMap<String, String> uuidsAndAuthority = new ArrayMap<>();

		public void put(String uuid, String authority) {
			try {
				this.uuidsAndAuthority.put(uuid, authority);
			} catch (Exception e) {
				CrashUtils.w(this, e, "Error while adding POI marker ID %s:%s", uuid, authority);
			}
		}

		public void putAll(SimpleArrayMap<String, String> newUuidsAndAuthority) {
			if (newUuidsAndAuthority != null) {
				this.uuidsAndAuthority.putAll(newUuidsAndAuthority);
			}
		}

		public void merge(POIMarkerIds poiMarkerIds) {
			if (poiMarkerIds != null) {
				putAll(poiMarkerIds.getMap());
			}
		}

		public int size() {
			return this.uuidsAndAuthority.size();
		}
	}

	@Nullable
	private LoadClusterItemsTask loadClusterItemsTask = null;

	private static class LoadClusterItemsTask extends MTAsyncTask<Void, Void, Collection<POIMarker>> {

		private final String LOG_TAG = MapViewController.class.getSimpleName() + ">" + LoadClusterItemsTask.class.getSimpleName();

		@NonNull
		private final WeakReference<MapViewController> mapViewControllerWR;

		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private LoadClusterItemsTask(MapViewController mapViewController) {
			this.mapViewControllerWR = new WeakReference<>(mapViewController);
		}

		@Override
		protected Collection<POIMarker> doInBackgroundMT(Void... params) {
			MapViewController mapViewController = this.mapViewControllerWR.get();
			if (mapViewController == null) {
				return null;
			}
			Activity activity = mapViewController.getActivityOrNull();
			if (activity == null) {
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
			AgencyProperties agency;
			for (POIManager poim : pois) {
				position = POIMarker.getLatLng(poim);
				positionTrunc = POIMarker.getLatLngTrunc(poim);
				name = poim.poi.getName();
				extra = null;
				agency = DataSourceProvider.get(activity).getAgency(activity, poim.poi.getAuthority());
				if (agency == null) {
					continue;
				}
				if (mapViewController.markerLabelShowExtra && poim.poi instanceof RouteTripStop) {
					extra = ((RouteTripStop) poim.poi).getRoute().getShortestName();
				}
				agencyShortName = mapViewController.markerLabelShowExtra ? agency.getShortName() : null;
				uuid = poim.poi.getUUID();
				authority = poim.poi.getAuthority();
				color = POIManager.getColor(activity, poim.poi, null);
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

		@Override
		protected void onPostExecute(@Nullable Collection<POIMarker> result) {
			super.onPostExecute(result);
			MapViewController mapViewController = this.mapViewControllerWR.get();
			if (mapViewController == null) {
				return;
			}
			if (result == null) {
				return;
			}
			if (mapViewController.extendedGoogleMap == null) {
				return;
			}
			ExtendedMarkerOptions options = new ExtendedMarkerOptions();
			for (POIMarker poiMarker : result) {
				options.position(poiMarker.position);
				options.title(poiMarker.getTitle());
				if (mapViewController.markerLabelShowExtra) {
					options.snippet(poiMarker.getSnippet());
				}
				options.icon(mapViewController.getActivityOrNull(), R.drawable.ic_place_white_slim, poiMarker.color, poiMarker.secondaryColor, Color.BLACK);
				options.data(poiMarker.getUuidsAndAuthority());
				mapViewController.extendedGoogleMap.addMarker(options);
			}
			mapViewController.clusterManagerItemsLoaded = true;
			mapViewController.hideLoading();
			if (mapViewController.showAllMarkersWhenReady) {
				mapViewController.showMarkers(false, mapViewController.followingUser);
			}
		}
	}

	public void addMarkers(@NonNull Collection<POIMarker> result) {
		if (MapViewController.this.extendedGoogleMap == null) {
			return;
		}
		ExtendedMarkerOptions options = new ExtendedMarkerOptions();
		for (POIMarker poiMarker : result) {
			options.position(poiMarker.position);
			options.title(poiMarker.getTitle());
			if (this.markerLabelShowExtra) {
				options.snippet(poiMarker.getSnippet());
			}
			options.icon(getActivityOrNull(), R.drawable.ic_place_white_slim, poiMarker.color, poiMarker.secondaryColor, Color.BLACK);
			options.data(poiMarker.getUuidsAndAuthority());
			IMarker marker = this.extendedGoogleMap.addMarker(options);
			if (poiMarker.hasUUID(this.lastSelectedUUID)) {
				marker.showInfoWindow();
			}
		}
		this.clusterManagerItemsLoaded = true;
		hideLoading();
		if (this.showAllMarkersWhenReady) {
			showMarkers(false, this.followingUser);
		}
	}

	@Override
	public void activate(OnLocationChangedListener onLocationChangedListener) {
		this.locationChangedListener = onLocationChangedListener;
		if (this.userLocation != null) {
			this.locationChangedListener.onLocationChanged(this.userLocation);
		}
	}

	@Override
	public void deactivate() {
		this.locationChangedListener = null;
	}

	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		boolean firstLocation = this.userLocation == null;
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
			if (this.locationChangedListener != null) {
				this.locationChangedListener.onLocationChanged(newLocation);
			}
			if (this.followingUser) {
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
		MapView mapView = getMapViewOrNull();
		if (mapView == null) {
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
		boolean success = updateMapCamera(false, CameraUpdateFactory.newCameraPosition(this.lastCameraPosition));
		if (success) {
			this.lastCameraPosition = null; // clear
		}
		return success;
	}

	private void setActivity(Activity activity) {
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
		MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			mapView.onPause();
		}
		ExtendedGoogleMap googleMap = getGoogleMapOrNull();
		if (googleMap != null) {
			this.lastCameraPosition = googleMap.getCameraPosition();
		}
		resetMapType();
	}

	public void setInitialSelectedUUID(String uuid) {
		if (TextUtils.isEmpty(uuid)) {
			return;
		}
		this.lastSelectedUUID = uuid;
	}

	public void setInitialLocation(Location initialLocation) {
		if (initialLocation == null) {
			return;
		}
		this.lastCameraPosition = CameraPosition.builder() //
				.target(LatLngUtils.fromLocation(initialLocation)) //
				.zoom(USER_LOCATION_ZOOM) //
				.build();
	}

	public void onSaveInstanceState(Bundle outState) {
		MapView mapView = getMapViewOrNull();
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

	public void notifyMarkerChanged(MapMarkerProvider markerProvider) {
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
		MapView mapView = getMapViewOrNull();
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
		TaskUtils.cancelQuietly(this.initMapViewTask, true);
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

		Collection<POIMarker> getPOMarkers();

		Collection<POIManager> getPOIs();

		POIManager getClosestPOI();

		POIManager getPOI(String uuid);
	}

	public interface MapListener {

		void onMapClick(LatLng position);

		void onCameraChange(LatLngBounds latLngBounds);
	}
}
