package org.mtransit.android.ui.view;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ResourceUtils;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.util.MapUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapViewController implements GoogleMap.OnCameraChangeListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLoadedCallback,
		GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationButtonClickListener, LocationSource, OnMapReadyCallback,
		ViewTreeObserver.OnGlobalLayoutListener, MTLog.Loggable {

	private static final String TAG = MapViewController.class.getSimpleName();

	private String tag = null;

	@Override
	public String getLogTag() {
		return TAG + "-" + this.tag;
	}

	private static final String EXTRA_LAST_CAMERA_POSITION = "extra_last_camera_position";
	private static final String EXTRA_LAST_SELECTED_UUID = "extra_last_selected_uuid";

	private WeakReference<Activity> activityWR;
	private MapView mapView;
	private GoogleMap googleMap;
	private Boolean showingMyLocation = false;
	private HashMap<String, Marker> markers = new HashMap<String, Marker>();
	private HashMap<String, String> markersIdToUUID = new HashMap<String, String>();
	private boolean mapLayoutReady = false;
	private boolean mapVisible = false;
	private LocationSource.OnLocationChangedListener locationChandedListener;
	private Location userLocation;

	private boolean waitingForGlobalLayout = false;

	@Override
	public void onGlobalLayout() {
		this.waitingForGlobalLayout = false;
		MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			SupportFactory.get().removeOnGlobalLayoutListener(mapView.getViewTreeObserver(), this);
			this.mapLayoutReady = true;
			setupInitialCamera();
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
		selectLastSelectedMarker();
		this.initialMapCameraSetup = showLastCameraPosition();
		if (!this.initialMapCameraSetup) {
			this.initialMapCameraSetup = showMarkers(false, this.followingUser);
		}
	}

	private WeakReference<MapMarkerProvider> markerProviderWR;
	private boolean myLocationEnabled;
	private boolean myLocationButtonEnabled;
	private boolean indoorLevelPickerEnabled;
	private boolean trafficEnabled;
	private boolean indoorEnabled;
	private int paddingTopSp = -1;
	private boolean followingUser = false;
	private boolean hasButtons;

	private CameraPosition lastCameraPosition;

	private String lastSelectedUUID;

	public MapViewController(String tag, MapMarkerProvider markerProvider, boolean myLocationEnabled, boolean myLocationButtonEnabled,
			boolean indoorLevelPickerEnabled, boolean trafficEnabled, boolean indoorEnabled, int paddingTopSp, boolean followingUser, boolean hasButtons) {
		this.tag = tag;
		setMarkerProvider(markerProvider);
		this.myLocationEnabled = myLocationEnabled;
		this.myLocationButtonEnabled = myLocationButtonEnabled;
		this.indoorLevelPickerEnabled = indoorLevelPickerEnabled;
		this.trafficEnabled = trafficEnabled;
		this.indoorEnabled = indoorEnabled;
		this.paddingTopSp = paddingTopSp;
		this.followingUser = followingUser;
		this.hasButtons = hasButtons;
	}

	private void setMarkerProvider(MapMarkerProvider markerProvider) {
		this.markerProviderWR = new WeakReference<MapViewController.MapMarkerProvider>(markerProvider);
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
			initMapViewSync(view);
		}
		return this.mapView;
	}

	private void initMapViewSync(View view) {
		if (view == null) {
			return;
		}
		if (this.mapView != null) {
			return;
		}
		MapsInitializer.initialize(view.getContext());
		this.mapView = (MapView) view.findViewById(R.id.map);
		if (this.mapView != null) {
			this.mapView.onCreate(this.lastSavedInstanceState);
			if (this.needToResumeMap) {
				this.mapView.onResume();
				this.needToResumeMap = false;
			}
			this.lastSavedInstanceState = null;
			this.mapView.getMapAsync(this);
		}
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
		return this.googleMap != null;
	}

	private void destroyGoogleMap() {
		this.googleMap = null;
	}

	private GoogleMap getGoogleMapOrNull() {
		return this.googleMap;
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		setupGoogleMap(googleMap);
	}

	private void setupGoogleMap(GoogleMap googleMap) {
		if (googleMap == null) {
			return;
		}
		this.googleMap = googleMap;
		this.googleMap.setMyLocationEnabled(this.myLocationEnabled);
		this.googleMap.setTrafficEnabled(this.trafficEnabled);
		this.googleMap.setIndoorEnabled(this.indoorEnabled);
		this.googleMap.getUiSettings().setMyLocationButtonEnabled(this.myLocationButtonEnabled);
		this.googleMap.getUiSettings().setIndoorLevelPickerEnabled(this.indoorLevelPickerEnabled);
		this.googleMap.setOnMapLoadedCallback(this);
		this.googleMap.setOnMyLocationButtonClickListener(this);
		this.googleMap.setOnInfoWindowClickListener(this);
		this.googleMap.setOnMarkerClickListener(this);
		this.googleMap.setLocationSource(this);
		this.googleMap.setOnCameraChangeListener(this);
		if (this.paddingTopSp > 0) {
			Context context = getActivityOrNull();
			int paddingTop = (int) ResourceUtils.convertSPtoPX(context, this.paddingTopSp); // action bar
			this.googleMap.setPadding(0, paddingTop, 0, 0);
		}
		initMapMarkers();
	}

	public boolean showMap(View view) {
		this.mapVisible = true;
		return showMapInternal(view);
	}

	private boolean showMapInternal(View optView) {
		MapView mapView = getMapViewOrNull(optView);
		if (mapView == null) {
			return false; // not shown
		}
		if (!this.mapVisible) {
			return false; // not shown
		}
		if (this.mapLayoutReady) {
			if (mapView.getVisibility() != View.VISIBLE) {
				mapView.setVisibility(View.VISIBLE);
			}
			return true; // shown
		}
		if (mapView.getVisibility() != View.VISIBLE) {
			mapView.setVisibility(View.VISIBLE);
		}
		if (!this.waitingForGlobalLayout) {
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
			if (mapView.getVisibility() != View.GONE) {
				mapView.setVisibility(View.GONE); // hide
			}
		}
	}

	@Override
	public void onMapLoaded() {
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		new MTAsyncTask<Marker, Void, POIManager>() {

			@Override
			public String getLogTag() {
				return MapViewController.this.getLogTag() + ">onInfoWindowClick";
			}

			@Override
			protected POIManager doInBackgroundMT(Marker... params) {
				Marker marker = params == null || params.length == 0 ? null : params[0];
				String uuid = marker == null ? null : MapViewController.this.markersIdToUUID.get(marker.getId());
				MapViewController.this.lastSelectedUUID = uuid;
				if (TextUtils.isEmpty(uuid)) {
					return null;
				}
				MapMarkerProvider markerProvider = MapViewController.this.markerProviderWR == null ? null : MapViewController.this.markerProviderWR.get();
				if (markerProvider == null) {
					return null;
				}
				return markerProvider.getPOI(uuid);
			}

			@Override
			protected void onPostExecute(POIManager poim) {
				if (poim == null) {
					return;
				}
				Activity activity = getActivityOrNull();
				if (activity == null) {
					return;
				}
				poim.onActionItemClick(activity, null);
			}
		}.execute(marker);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		String uuid = marker == null ? null : MapViewController.this.markersIdToUUID.get(marker.getId());
		if (!TextUtils.isEmpty(uuid)) {
			this.lastSelectedUUID = uuid;
		}
		return false; // not handled
	}

	@Override
	public void onCameraChange(CameraPosition cameraPosition) {
		this.showingMyLocation = this.showingMyLocation == null;
	}

	@Override
	public boolean onMyLocationButtonClick() {
		if (this.showingMyLocation != null && this.showingMyLocation) {
			showMarkers(true, false);
			this.showingMyLocation = false;
			return true; // handled
		}
		return showClosestPOI();
	}

	private boolean showClosestPOI() {
		if (this.userLocation == null) {
			return false; // not handled
		}
		LatLngBounds.Builder llb = LatLngBounds.builder();
		llb.include(new LatLng(this.userLocation.getLatitude(), this.userLocation.getLongitude()));
		MapMarkerProvider markerProvider = this.markerProviderWR == null ? null : this.markerProviderWR.get();
		POIManager poim = markerProvider == null ? null : markerProvider.getClosestPOI();
		if (poim != null) {
			LatLng poiLatLng = new LatLng(poim.getLat(), poim.getLng());
			llb.include(poiLatLng);
			selectMarker(poim.poi.getUUID());
		}
		Context context = getActivityOrNull();
		boolean success = updateMapCamera(true, CameraUpdateFactory.newLatLngBounds(llb.build(), MapUtils.getMapWithButtonsCameraPaddingInPx(context)));
		this.showingMyLocation = null;
		return success; // handled or not
	}

	private boolean showMarkers(boolean anim, boolean includeUserLocation) {
		if (CollectionUtils.getSize(this.markers) == 0) {
			return false; // not shown
		}
		LatLngBounds.Builder llb = LatLngBounds.builder();
		for (Marker mapMarker : this.markers.values()) {
			llb.include(mapMarker.getPosition());
		}
		if (includeUserLocation) {
			if (this.userLocation != null) {
				llb.include(new LatLng(this.userLocation.getLatitude(), this.userLocation.getLongitude()));
			}
		}
		Context context = getActivityOrNull();
		int paddingInPx;
		if (this.hasButtons) {
			paddingInPx = MapUtils.getMapWithButtonsCameraPaddingInPx(context);
		} else {
			paddingInPx = MapUtils.getMapWithoutButtonsCameraPaddingInPx(context);
		}
		return updateMapCamera(anim, CameraUpdateFactory.newLatLngBounds(llb.build(), paddingInPx));
	}

	private boolean updateMapCamera(boolean anim, CameraUpdate cameraUpdate) {
		GoogleMap googleMap = getGoogleMapOrNull();
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

	public void initMapMarkers() {
		if (CollectionUtils.getSize(this.markers) > 0) {
			return;
		}
		if (!hasGoogleMap()) {
			return;
		}
		MapMarkerProvider markerProvider = this.markerProviderWR == null ? null : this.markerProviderWR.get();
		if (markerProvider == null) {
			return;
		}
		new MTAsyncTask<Void, Void, HashMap<String, MarkerOptions>>() {

			private final String TAG = MapViewController.this.getLogTag() + ">initMapMarkers";

			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected HashMap<String, MarkerOptions> doInBackgroundMT(Void... params) {
				HashMap<String, MarkerOptions> uuidMarkerOptions = new HashMap<String, MarkerOptions>();
				MapMarkerProvider markerProvider = MapViewController.this.markerProviderWR == null ? null : MapViewController.this.markerProviderWR.get();
				if (markerProvider != null) {
					for (POIManager poim : markerProvider.getPOIs()) {
						MarkerOptions poiMarkerOptions = new MarkerOptions() //
								.title(poim.poi.getName()) //
								.position(new LatLng(poim.poi.getLat(), poim.poi.getLng())) //
								.icon(getBitmapDescriptor(poim));
						uuidMarkerOptions.put(poim.poi.getUUID(), poiMarkerOptions);
					}
				}
				return uuidMarkerOptions;
			}

			@Override
			protected void onPostExecute(HashMap<String, MarkerOptions> uuidMarkerOptions) {
				super.onPostExecute(uuidMarkerOptions);
				GoogleMap googleMap = getGoogleMapOrNull();
				if (googleMap == null) {
					return;
				}
				if (uuidMarkerOptions != null) {
					for (HashMap.Entry<String, MarkerOptions> uuidMarkerOption : uuidMarkerOptions.entrySet()) {
						Marker poiMarker = googleMap.addMarker(uuidMarkerOption.getValue());
						MapViewController.this.markers.put(uuidMarkerOption.getKey(), poiMarker);
						MapViewController.this.markersIdToUUID.put(poiMarker.getId(), uuidMarkerOption.getKey());
					}
				}
				selectLastSelectedMarker();
				setupInitialCamera();
				showMapInternal(null);
			}
		}.execute();
	}

	public boolean selectMarker(String uuid) {
		if (TextUtils.isEmpty(uuid)) {
			return false;
		}
		this.lastSelectedUUID = uuid;
		Marker marker = this.markers.get(uuid);
		if (marker == null) {
			return false; // not selected
		}
		marker.showInfoWindow();
		this.lastSelectedUUID = null; // already shown
		return true; // selected
	}

	private BitmapDescriptor getBitmapDescriptor(POIManager optPoim) {
		try {
			Context context = getActivityOrNull();
			if (context == null) {
				return BitmapDescriptorFactory.defaultMarker();
			}
			int markerColor = MapUtils.DEFAULT_MARKET_COLOR;
			if (optPoim != null) {
				markerColor = optPoim.getColor(context);
			}
			if (markerColor == Color.BLACK) {
				markerColor = Color.DKGRAY;
			}
			int bitmapResId = R.drawable.ic_place_white_slim;
			return BitmapDescriptorFactory.fromBitmap(ColorUtils.colorizeBitmapResource(context, markerColor, bitmapResId));
		} catch (Exception e) {
			MTLog.w(this, e, "Error while setting custom marker color!");
			return BitmapDescriptorFactory.defaultMarker();
		}
	}

	@Override
	public void activate(OnLocationChangedListener onLocationChangedListener) {
		this.locationChandedListener = onLocationChangedListener;
		if (this.userLocation != null) {
			this.locationChandedListener.onLocationChanged(this.userLocation);
		}
	}

	@Override
	public void deactivate() {
		this.locationChandedListener = null;
	}

	public void onUserLocationChanged(Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
			if (this.locationChandedListener != null) {
				this.locationChandedListener.onLocationChanged(newLocation);
			}
			if (this.followingUser) {
				showMarkers(true, true);
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
		setupInitialCamera();
		showMapInternal(null);
		return true; // resumed
	}

	private void selectLastSelectedMarker() {
		if (TextUtils.isEmpty(this.lastSelectedUUID)) {
			return;
		}
		boolean success = selectMarker(this.lastSelectedUUID);
		if (success) {
			this.lastSelectedUUID = null;
		}
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

	public void onDetach() {
		clearActivity();
	}

	private void setActivity(Activity activity) {
		this.activityWR = new WeakReference<Activity>(activity);
	}

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
		GoogleMap googleMap = getGoogleMapOrNull();
		if (googleMap != null) {
			this.lastCameraPosition = googleMap.getCameraPosition();
		}
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
		initMapMarkers();
	}

	public void clearMarkers() {
		if (this.markers != null) {
			this.markers.clear();
		}
	}

	public void onLowMemory() {
		MapView mapView = getMapViewOrNull();
		if (mapView != null) {
			mapView.onLowMemory();
		}
	}

	public void onDestroyView() {
		destroyMapView();
		this.mapVisible = false;
		destroyGoogleMap();
		this.mapLayoutReady = false;
		this.waitingForGlobalLayout = false;
		this.initialMapCameraSetup = false;
		if (this.markerProviderWR != null) {
			this.markerProviderWR.clear();
		}
		clearMarkers();
	}

	public void onDestroy() {
	}

	public static interface MapMarkerProvider {
		public Collection<POIManager> getPOIs();
		public POIManager getClosestPOI();
		public POIManager getPOI(String uuid);
	}
}
