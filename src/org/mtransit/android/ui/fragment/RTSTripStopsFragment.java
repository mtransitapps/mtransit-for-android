package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.HashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LoaderUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.RTSTripStopsLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.util.MapUtils;

import android.app.Activity;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class RTSTripStopsFragment extends MTFragmentV4 implements VisibilityAwareFragment, LoaderManager.LoaderCallbacks<ArrayList<POIManager>>,
		MTActivityWithLocation.UserLocationListener, LocationSource, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMyLocationButtonClickListener,
		GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraChangeListener, OnMapReadyCallback {

	private static final String TAG = RTSTripStopsFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String EXTRA_AGENCY_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_ROUTE_ID = "extra_route_id";
	private static final String EXTRA_TRIP_ID = "extra_trip_id";
	private static final String EXTRA_TRIP_STOP_ID = "extra_trip_stop_id";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";
	private static final String EXTRA_SHOWING_LIST_INSTEAD_OF_MAP = "extra_showing_list_instead_of_map";
	private static final String EXTRA_CLOSEST_POI_SHOWN = "extra_closest_poi_shown";

	public static RTSTripStopsFragment newInstance(int fragmentPosition, int lastVisibleFragmentPosition, String authority, long routeId, long tripId,
			Integer optStopId, boolean showingListInsteadOfMap, Route optRoute) {
		RTSTripStopsFragment f = new RTSTripStopsFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AGENCY_AUTHORITY, authority);
		f.authority = authority;
		args.putLong(EXTRA_ROUTE_ID, routeId);
		f.routeId = routeId;
		f.route = optRoute;
		args.putLong(EXTRA_TRIP_ID, tripId);
		f.tripId = tripId;
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
			f.fragmentPosition = fragmentPosition;
		}
		if (lastVisibleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisibleFragmentPosition);
			f.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}
		if (optStopId != null) {
			args.putInt(EXTRA_TRIP_STOP_ID, optStopId);
			f.stopId = optStopId;
		}
		args.putBoolean(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, showingListInsteadOfMap);
		f.showingListInsteadOfMap = showingListInsteadOfMap;
		f.setArguments(args);
		return f;
	}

	private Long routeId;
	private Long tripId;
	private int stopId = -1;
	private boolean closestPOIShow = false;
	private String authority;
	private POIArrayAdapter adapter;
	private Location userLocation;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private String emptyText = null;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_rts_trip_stops, container, false);
		setupView(view);
		if (!this.showingListInsteadOfMap) { // showing map
			initMapView(view, savedInstanceState);
		}
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AGENCY_AUTHORITY, this.authority);
		}
		if (this.routeId != null) {
			outState.putLong(EXTRA_ROUTE_ID, this.routeId);
		}
		if (this.tripId != null) {
			outState.putLong(EXTRA_TRIP_ID, this.tripId);
		}
		outState.putInt(EXTRA_TRIP_STOP_ID, this.stopId);
		outState.putBoolean(EXTRA_CLOSEST_POI_SHOWN, this.closestPOIShow);
		if (this.fragmentPosition >= 0) {
			outState.putInt(EXTRA_FRAGMENT_POSITION, this.fragmentPosition);
		}
		if (this.lastVisibleFragmentPosition >= 0) {
			outState.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, this.lastVisibleFragmentPosition);
		}
		if (this.showingListInsteadOfMap != null) {
			outState.putBoolean(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, this.showingListInsteadOfMap);
		}
		saveMapViewInstance(outState);
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AGENCY_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
			resetRoute();
		}
		Long newTripId = BundleUtils.getLong(EXTRA_TRIP_ID, bundles);
		if (newTripId != null && !newTripId.equals(this.tripId)) {
			this.tripId = newTripId;
		}
		Long newRouteId = BundleUtils.getLong(EXTRA_ROUTE_ID, bundles);
		if (newRouteId != null && !newRouteId.equals(this.routeId)) {
			this.routeId = newRouteId;
			resetRoute();
		}
		Integer newStopId = BundleUtils.getInt(EXTRA_TRIP_STOP_ID, bundles);
		if (newStopId != null && !newStopId.equals(this.stopId)) {
			this.stopId = newStopId;
		}
		Boolean newClosestPOIShown = BundleUtils.getBoolean(EXTRA_CLOSEST_POI_SHOWN, bundles);
		if (newClosestPOIShown != null) {
			this.closestPOIShow = newClosestPOIShown;
		}
		Boolean newShowingListInsteadOfMap = BundleUtils.getBoolean(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, bundles);
		if (newShowingListInsteadOfMap != null) {
			this.showingListInsteadOfMap = newShowingListInsteadOfMap;
		}
		Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, bundles);
		if (fragmentPosition != null) {
			if (fragmentPosition >= 0) {
				this.fragmentPosition = fragmentPosition;
			} else {
				this.fragmentPosition = -1;
			}
		}
		Integer lastVisibleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, bundles);
		if (lastVisibleFragmentPosition != null) {
			if (lastVisibleFragmentPosition >= 0) {
				this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
			} else {
				this.lastVisibleFragmentPosition = -1;
			}
		}
	}


	private Route route;

	private boolean hasRoute() {
		if (this.route == null) {
			initRouteAsync();
			return false;
		}
		return true;
	}

	private Route getRouteOrNull() {
		if (!hasRoute()) {
			return null;
		}
		return this.route;
	}

	private void initRouteAsync() {
		if (this.loadRouteTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (this.routeId == null || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadRouteTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadRouteTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">initRouteAsync";
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initRouteSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewRoute();
			}
		}

	};

	private void applyNewRoute() {
		if (this.route == null) {
			return;
		}
		initMapMarkers(null);
	}

	private boolean initRouteSync() {
		if (this.route != null) {
			return false;
		}
		if (this.routeId != null && !TextUtils.isEmpty(this.authority)) {
			this.route = DataSourceManager.findRTSRoute(getActivity(), this.authority, this.routeId);
		}
		return this.route != null;
	}

	private void resetRoute() {
		this.route = null; // reset
	}

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setShowExtra(false);
	}

	private void linkAdapterWithListView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		View listView = view.findViewById(R.id.list);
		if (listView != null) {
			this.adapter.setListView((AbsListView) listView);
		}
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		if (this.showingListInsteadOfMap) { // showing list
			inflateList(view); // inflate ASAP for view state restore
		}
	}

	@Override
	public void setFragmentPosition(int fragmentPosition) {
		this.fragmentPosition = fragmentPosition;
		setFragmentVisibleAtPosition(this.lastVisibleFragmentPosition); // force reset visibility
	}

	@Override
	public void setFragmentVisibleAtPosition(int visibleFragmentPosition) {
		if (this.lastVisibleFragmentPosition == visibleFragmentPosition //
				&& (//
				(this.fragmentPosition == visibleFragmentPosition && this.fragmentVisible) //
				|| //
				(this.fragmentPosition != visibleFragmentPosition && !this.fragmentVisible) //
				) //
		) {
			return;
		}
		this.lastVisibleFragmentPosition = visibleFragmentPosition;
		if (this.fragmentPosition < 0) {
			return;
		}
		if (this.fragmentPosition == visibleFragmentPosition) {
			onFragmentVisible();
		} else {
			onFragmentInvisible();
		}
	}

	private void onFragmentInvisible() {
		if (!this.fragmentVisible) {
			return; // already invisible
		}
		this.fragmentVisible = false;
		if (this.adapter != null) {
			this.adapter.onPause();
		}
		pauseMapView();
	}

	@Override
	public boolean isFragmentVisible() {
		return this.fragmentVisible;
	}

	private void onFragmentVisible() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		this.fragmentVisible = true;
		switchView(getView());
		if (this.adapter == null || !this.adapter.isInitialized()) {
			LoaderUtils.restartLoader(getLoaderManager(), POIS_LOADER, null, this);
		} else {
			this.adapter.onResume(getActivity());
		}
		resumeMapView();
		Activity activity = getActivity();
		if (activity != null && activity instanceof MTActivityWithLocation) {
			onUserLocationChanged(((MTActivityWithLocation) activity).getUserLocation()); // user location was unknown yet or discarded while not visible
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
	}

	private static final int POIS_LOADER = 0;

	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POIS_LOADER:
			if (this.tripId == null || TextUtils.isEmpty(this.authority)) {
				return null;
			}
			return new RTSTripStopsLoader(getActivity(), this.tripId, this.authority);
		default:
			MTLog.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<ArrayList<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<POIManager>> loader, ArrayList<POIManager> data) {
		Pair<Integer, String> currentSelectedItemIndexUuid = null;
		if (this.stopId > 0 || !this.closestPOIShow) {
			if (this.stopId > 0) {
				currentSelectedItemIndexUuid = findStopIndexUuid(this.stopId, data);
			}
			if (currentSelectedItemIndexUuid == null) {
				if (!this.closestPOIShow) {
					currentSelectedItemIndexUuid = findClosestPOIIndexUuid(data);
				}
			}
			this.stopId = -1; // can only be used once
			this.closestPOIShow = true; // only the 1rst time
		}
		this.mapMarkers = null; // force refresh
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		View view = getView();
		if (this.showingListInsteadOfMap) { // list
			Integer selectedPosition = currentSelectedItemIndexUuid == null ? null : currentSelectedItemIndexUuid.first;
			if (selectedPosition != null && selectedPosition > 0) {
				if (view != null) {
					inflateList(view);
					((AbsListView) view.findViewById(R.id.list)).setSelection(selectedPosition - 1); // show 1 more stop on top of the list
				}
			}
		} else { // map
			String selectedUuid = currentSelectedItemIndexUuid == null ? null : currentSelectedItemIndexUuid.second;
			initMapMarkers(selectedUuid);
		}
		switchView(view);
	}

	private MapView mapView = null;
	private MapView getMapView(View view) {
		if (this.mapView == null) {
			initMapView(view, null);
		}
		return this.mapView;
	}

	private void pauseMapView() {
		if (this.mapView != null) {
			this.mapView.onPause();
		}
	}

	private void resumeMapView() {
		if (this.mapView != null) {
			this.mapView.onResume();
		}
	}

	private void saveMapViewInstance(Bundle outState) {
		if (this.mapView != null) {
			this.mapView.onSaveInstanceState(outState);
		}
	}

	private void destroyMapView() {
		if (this.mapView != null) {
			this.mapView.onDestroy();
			this.mapView = null;
		}
	}

	private void initMapView(View view, Bundle savedInstanceState) {
		if (view != null) {
			this.mapView = (MapView) view.findViewById(R.id.map);
			if (this.mapView != null) {
				this.mapView.onCreate(savedInstanceState);
				if (this.fragmentVisible) {
					this.mapView.onResume();
				}
				initMap(this.mapView);
			}
		}
	}

	private boolean mapLayoutReady = false;

	private ViewTreeObserver.OnGlobalLayoutListener mapViewOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

		@Override
		public void onGlobalLayout() {
			if (RTSTripStopsFragment.this.mapView != null) {
				SupportFactory.get().removeOnGlobalLayoutListener(RTSTripStopsFragment.this.mapView.getViewTreeObserver(), this);
				RTSTripStopsFragment.this.mapLayoutReady = true;
				updateMapPosition(false);
			}
		}
	};

	private void updateMapPosition(boolean anim) {
		if (!this.mapLayoutReady) {
			return;
		}
		if (this.mapMarkersShown) {
			return;
		}
		if (this.mapMarkers == null || this.mapMarkers.size() == 0) {
			return;
		}
		LatLngBounds.Builder llb = LatLngBounds.builder();
		for (Marker mapMarker : this.mapMarkers.values()) {
			llb.include(mapMarker.getPosition());
		}
		MapView mapView = getMapView(getView());
		GoogleMap map = getMap(mapView);
		this.mapMarkersShown = MapUtils.updateMapPosition(getActivity(), map, mapView, anim, llb.build(),
				MapUtils.getMapWithButtonsCameraPaddingInPx(getActivity()));
	}

	private GoogleMap map = null;

	private GoogleMap getMap(MapView mapView) {
		if (this.map == null) {
			initMap(mapView);
		}
		return this.map;
	}

	private void initMap(MapView mapView) {
		if (mapView != null) {
			mapView.getMapAsync(this);
		}
	}

	@Override
	public void onMapReady(GoogleMap map) {
		this.map = map;
		setupMap();
	}

	private void setupMap() {
		if (this.map != null) {
			this.map.setOnMapLoadedCallback(this);
			this.map.setOnMyLocationButtonClickListener(this);
			this.map.setOnInfoWindowClickListener(this);
			this.map.setOnMarkerClickListener(this);
			this.map.setOnCameraChangeListener(this);
			this.map.setLocationSource(this);
			this.map.setMyLocationEnabled(true);
			this.map.getUiSettings().setMyLocationButtonEnabled(true);
			this.map.getUiSettings().setIndoorLevelPickerEnabled(false);
			this.map.setTrafficEnabled(false);
			this.map.setIndoorEnabled(false);
			initMapMarkers(null);
		}
	}

	@Override
	public void onCameraChange(CameraPosition position) {
		this.showingMyLocation = this.showingMyLocation == null;
	}

	private Boolean showingMyLocation = false;

	@Override
	public boolean onMyLocationButtonClick() {
		if (this.showingMyLocation != null && this.showingMyLocation) {
			this.mapMarkersShown = false;
			updateMapPosition(true);
			this.showingMyLocation = false;
			return true; // handled
		}
		if (this.userLocation == null) {
			return false; // not handled
		}
		LatLngBounds.Builder llb = LatLngBounds.builder();
		llb.include(new LatLng(this.userLocation.getLatitude(), this.userLocation.getLongitude()));
		if (this.adapter != null && this.adapter.hasClosestPOI()) {
			POIManager poim = this.adapter.getClosestPOI();
			if (poim != null) {
				LatLng poiLatLng = new LatLng(poim.getLat(), poim.getLng());
				llb.include(poiLatLng);
				Marker poiMarker = this.mapMarkers == null ? null : this.mapMarkers.get(poim.poi.getUUID());
				if (poiMarker != null) {
					poiMarker.showInfoWindow();
				}
			}
		}
		MapView mapView = getMapView(getView());
		GoogleMap map = getMap(mapView);
		boolean success = MapUtils
				.updateMapPosition(getActivity(), map, mapView, true, llb.build(), MapUtils.getMapWithButtonsCameraPaddingInPx(getActivity()));
		this.showingMyLocation = null;
		return success; // handled or not
	}


	@Override
	public void onMapLoaded() {
		updateMapPosition(false);
	}

	private HashMap<String, Marker> mapMarkers = null;
	private HashMap<String, String> mapMarkersIdToUUID = new HashMap<String, String>();

	private boolean mapMarkersShown = false;

	private void initMapMarkers(final String optSelectedItemUuid) {
		if (this.mapMarkers != null) {
			return;
		}
		if (!hasRoute()) {
			return;
		}
		GoogleMap map = getMap(getMapView(getView()));
		if (map == null) {
			return;
		}
		this.mapMarkers = new HashMap<String, Marker>();
		this.mapMarkersShown = false;
		new MTAsyncTask<Void, Void, HashMap<String, MarkerOptions>>() {

			private final String TAG = RTSTripStopsFragment.class.getSimpleName() + ">initMapMarkers";

			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected HashMap<String, MarkerOptions> doInBackgroundMT(Void... params) {
				Route route = getRouteOrNull();
				HashMap<String, MarkerOptions> result = new HashMap<String, MarkerOptions>();
				if (RTSTripStopsFragment.this.adapter != null) {
					for (int i = 0; i < RTSTripStopsFragment.this.adapter.getPoisCount(); i++) {
						POIManager poim = RTSTripStopsFragment.this.adapter.getItem(i);
						if (poim != null) {
							LatLng poiLatLng = new LatLng(poim.poi.getLat(), poim.poi.getLng());
							MarkerOptions poiMarkerOptions = new MarkerOptions() //
									.title(poim.poi.getName()) //
									.position(poiLatLng) //
									.icon(getBitmapDescriptor(route));
							result.put(poim.poi.getUUID(), poiMarkerOptions);
						}
					}
				}
				return result;
			}

			@Override
			protected void onPostExecute(HashMap<String, MarkerOptions> result) {
				super.onPostExecute(result);
				GoogleMap map = getMap(getMapView(getView()));
				if (map != null) {
					if (result != null) {
						for (HashMap.Entry<String, MarkerOptions> uuidMarkerOptions : result.entrySet()) {
							Marker poiMarker = map.addMarker(uuidMarkerOptions.getValue());
							if (optSelectedItemUuid != null && optSelectedItemUuid.equals(uuidMarkerOptions.getKey())) {
								poiMarker.showInfoWindow();
							}
							RTSTripStopsFragment.this.mapMarkers.put(uuidMarkerOptions.getKey(), poiMarker);
							RTSTripStopsFragment.this.mapMarkersIdToUUID.put(poiMarker.getId(), uuidMarkerOptions.getKey());
						}
						updateMapPosition(false);
					}
				}
			}
		}.execute();
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		new MTAsyncTask<Marker, Void, POIManager>() {

			@Override
			public String getLogTag() {
				return RTSTripStopsFragment.class.getSimpleName() + ">onInfoWindowClick";
			}

			@Override
			protected POIManager doInBackgroundMT(Marker... params) {
				Marker marker = params == null || params.length == 0 ? null : params[0];
				String uuid = marker == null ? null : RTSTripStopsFragment.this.mapMarkersIdToUUID.get(marker.getId());
				if (TextUtils.isEmpty(uuid)) {
					return null;
				}
				return RTSTripStopsFragment.this.adapter.getItem(uuid);
			}

			@Override
			protected void onPostExecute(POIManager poim) {
				if (poim != null) {
					poim.onActionItemClick(getActivity(), null);
				}
			}
		}.execute(marker);
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		return false; // not handled
	}

	private BitmapDescriptor mapMarkerBitmap = null;

	private BitmapDescriptor getBitmapDescriptor(Route route) {
		if (this.mapMarkerBitmap == null) {
			try {
				int markerColor = POIManager.getRouteColor(getActivity(), route, this.authority, MapUtils.DEFAULT_MARKET_COLOR);
				if (markerColor == Color.BLACK) {
					markerColor = Color.DKGRAY;
				}
				int bitmapResId = R.drawable.ic_place_white_slim;
				this.mapMarkerBitmap = BitmapDescriptorFactory.fromBitmap(ColorUtils.colorizeBitmapResource(getActivity(), markerColor, bitmapResId));
			} catch (Exception e) {
				MTLog.w(this, e, "Error while setting custom marker color!");
				this.mapMarkerBitmap = null;
			}
			if (this.mapMarkerBitmap == null) {
				return BitmapDescriptorFactory.defaultMarker();
			}
		}
		return this.mapMarkerBitmap;
	}

	private Pair<Integer, String> findStopIndexUuid(int stopId, ArrayList<POIManager> pois) {
		for (int i = 0; i < pois.size(); i++) {
			POIManager poim = pois.get(i);
			if (poim != null && poim.poi instanceof RouteTripStop) {
				RouteTripStop rts = (RouteTripStop) poim.poi;
				if (rts.stop.id == stopId) {
					return new Pair<Integer, String>(i, poim.poi.getUUID());
				}
			}
		}
		return null;
	}

	private Pair<Integer, String> findClosestPOIIndexUuid(ArrayList<POIManager> pois) {
		if (this.userLocation != null) {
			LocationUtils.updateDistance(pois, this.userLocation.getLatitude(), this.userLocation.getLongitude());
			ArrayList<POIManager> sortedPOIs = new ArrayList<POIManager>(pois);
			CollectionUtils.sort(sortedPOIs, LocationUtils.POI_DISTANCE_COMPARATOR);
			String closestPoiUuid = sortedPOIs.get(0).poi.getUUID();
			for (int i = 0; i < pois.size(); i++) {
				POIManager poim = pois.get(i);
				if (poim.poi.getUUID().equals(closestPoiUuid)) {
					return new Pair<Integer, String>(i, poim.poi.getUUID());
				}
			}
		}
		return null;
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				if (this.adapter != null) {
					this.adapter.setLocation(newLocation);
				}
			}
		}
		if (this.locationChandedListener != null) {
			this.locationChandedListener.onLocationChanged(newLocation);
		}
	}

	private LocationSource.OnLocationChangedListener locationChandedListener;

	@Override
	public void activate(LocationSource.OnLocationChangedListener locationChandedListener) {
		this.locationChandedListener = locationChandedListener;
	}

	@Override
	public void deactivate() {
		this.locationChandedListener = null;
	}

	@Override
	public void onPause() {
		super.onPause();
		this.stopId = -1;
		onFragmentInvisible();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.fragmentPosition < 0 || this.fragmentPosition == this.lastVisibleFragmentPosition) {
			onFragmentVisible();
		} // ELSE would be call later
		if (this.adapter != null) {
			this.adapter.setActivity(getActivity());
		}
	}


	@Override
	public void onLowMemory() {
		super.onLowMemory();
		if (!this.showingListInsteadOfMap) { // showing map
			if (getMapView(getView()) != null) {
				getMapView(getView()).onLowMemory();
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		destroyMapView();
		this.map = null;
		if (this.mapMarkers != null) {
			this.mapMarkers.clear();
			this.mapMarkers = null;
		}
		this.mapMarkerBitmap = null;
		this.mapMarkersShown = false;
		this.mapLayoutReady = false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
		}
	}

	private void switchView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading(view);
		} else if (this.adapter.getPoisCount() == 0) {
			showEmpty(view);
		} else {
			showListOrMap(view);
		}
	}

	private void showListOrMap(View view) {
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		if (this.showingListInsteadOfMap) { // list
			view.findViewById(R.id.map).setVisibility(View.GONE); // hide
			inflateList(view);
			view.findViewById(R.id.list).setVisibility(View.VISIBLE); // show
		} else { // map
			if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
				view.findViewById(R.id.list).setVisibility(View.GONE); // hide
			}
			if (this.mapLayoutReady) {
				view.findViewById(R.id.map).setVisibility(View.VISIBLE);
			} else {
				view.findViewById(R.id.map).setVisibility(View.INVISIBLE); // show when ready
				if (view.findViewById(R.id.map).getViewTreeObserver().isAlive()) {
					view.findViewById(R.id.map).getViewTreeObserver().addOnGlobalLayoutListener(this.mapViewOnGlobalLayoutListener);
				}
			}
		}
	}

	private void inflateList(View view) {
		if (view.findViewById(R.id.list) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.list_stub)).inflate(); // inflate
			linkAdapterWithListView(view);
		}
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.map) != null) { // IF inflated/present DO
			view.findViewById(R.id.map).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.loading_stub)).inflate(); // inflate
		}
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.map) != null) { // IF inflated/present DO
			view.findViewById(R.id.map).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.empty_stub)).inflate(); // inflate
		}
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) view.findViewById(R.id.empty_text)).setText(this.emptyText);
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	private Boolean showingListInsteadOfMap = null;

	public void setShowingListInsteadOfMap(boolean newShowingListInsteadOfMap) {
		if (this.showingListInsteadOfMap != null && this.showingListInsteadOfMap == newShowingListInsteadOfMap) {
			return; // nothing changed
		}
		this.showingListInsteadOfMap = newShowingListInsteadOfMap; // switching
		if (this.adapter != null) {
			setupView(getView());
			switchView(getView());
			if (!this.showingListInsteadOfMap) {
				initMapMarkers(null);
			}
		}
	}
}
