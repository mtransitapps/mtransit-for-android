package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.HashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LoaderUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.Module;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.AgencyPOIsLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.util.MapUtils;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
public class AgencyPOIsFragment extends MTFragmentV4 implements AgencyTypeFragment.AgencyFragment, LoaderManager.LoaderCallbacks<ArrayList<POIManager>>,
		MTActivityWithLocation.UserLocationListener, LocationSource, GoogleMap.OnMapLoadedCallback, GoogleMap.OnMyLocationButtonClickListener,
		GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraChangeListener {

	private static final String TAG = AgencyPOIsFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG + "-" + this.authority;
	}

	private static final String EXTRA_AGENCY_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";
	private static final String EXTRA_SHOWING_LIST_INSTEAD_OF_MAP = "extra_showing_list_instead_of_map";

	public static AgencyPOIsFragment newInstance(int fragmentPosition, int lastVisibleFragmentPosition, String agencyAuthority, Location optUserLocation,
			Boolean optShowingListInsteadOfMap, AgencyProperties optAgency) {
		AgencyPOIsFragment f = new AgencyPOIsFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AGENCY_AUTHORITY, agencyAuthority);
		f.authority = agencyAuthority;
		f.agency = optAgency;
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
			f.fragmentPosition = fragmentPosition;
		}
		if (lastVisibleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisibleFragmentPosition);
			f.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}
		if (optShowingListInsteadOfMap != null) {
			args.putBoolean(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, optShowingListInsteadOfMap.booleanValue());
			f.showingListInsteadOfMap = optShowingListInsteadOfMap;
		}
		f.setArguments(args);
		return f;
	}

	private Location userLocation;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private POIArrayAdapter adapter;
	private String emptyText = null;

	@Override
	public String getAgencyAuthority() {
		return this.authority;
	}

	private String authority;

	private AgencyProperties agency;

	private boolean hasAgency() {
		if (this.agency == null) {
			initAgencyAsync();
			return false;
		}
		return true;
	}

	private void initAgencyAsync() {
		if (this.loadAgencyTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadAgencyTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadAgencyTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">loadAgencyTask";
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initAgencySync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewAgency();
			}
		}
	};

	private AgencyProperties getAgencyOrNull() {
		if (!hasAgency()) {
			return null;
		}
		return this.agency;
	}

	private boolean initAgencySync() {
		if (this.agency != null) {
			return false;
		}
		if (!TextUtils.isEmpty(this.authority)) {
			this.agency = DataSourceProvider.get(getActivity()).getAgency(getActivity(), this.authority);
		}
		return this.agency != null;
	}

	private void applyNewAgency() {
		if (this.agency == null) {
			return;
		}
		initMapMarkers(null);
	}

	private void resetAgency() {
		this.agency = null;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		restoreInstanceState(savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_agency_pois, container, false);
		setupView(view);
		if (!isShowingListInsteadOfMap()) { // showing map
			initMapView(view, savedInstanceState);
		}
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AGENCY_AUTHORITY, this.authority);
		}
		if (this.fragmentPosition >= 0) {
			outState.putInt(EXTRA_FRAGMENT_POSITION, this.fragmentPosition);
		}
		if (this.lastVisibleFragmentPosition >= 0) {
			outState.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, this.lastVisibleFragmentPosition);
		}
		if (this.showingListInsteadOfMap != null) {
			outState.putBoolean(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, this.showingListInsteadOfMap.booleanValue());
		}
		saveMapViewInstance(outState);
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AGENCY_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
			resetAgency();
		}
		Boolean newShowingListInsteadOfMap = BundleUtils.getBoolean(EXTRA_SHOWING_LIST_INSTEAD_OF_MAP, bundles);
		if (newShowingListInsteadOfMap != null) {
			this.showingListInsteadOfMap = newShowingListInsteadOfMap;
		}
		Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, bundles);
		if (fragmentPosition != null) {
			if (fragmentPosition.intValue() >= 0) {
				this.fragmentPosition = fragmentPosition.intValue();
			} else {
				this.fragmentPosition = -1;
			}
		}
		Integer lastVisibleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, bundles);
		if (lastVisibleFragmentPosition != null) {
			if (lastVisibleFragmentPosition.intValue() >= 0) {
				this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
			} else {
				this.lastVisibleFragmentPosition = -1;
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	private void initAdapter() {
		this.adapter = new POIArrayAdapter(getActivity());
		this.adapter.setTag(this.authority);
		View view = getView();
		setupView(view);
		linkAdapterWithListView(view);
		switchView(view);
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
		if (view == null || this.adapter == null) {
			return;
		}
		if (isShowingListInsteadOfMap()) { // showing list
			inflateList(view);
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
		if (this.adapter == null) {
			LoaderUtils.restartLoader(getLoaderManager(), POIS_LOADER, null, this);
		} else {
			this.adapter.onResume(getActivity());
		}
		resumeMapView();
		checkIfShowingListInsteadOfMapChanged();
		getActivity().supportInvalidateOptionsMenu(); // initialize action bar list/map switch icon
		updateListMapToggleMenuItem();
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
	}

	private static final int POIS_LOADER = 0;

	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POIS_LOADER:
			if (TextUtils.isEmpty(this.authority)) {
				return null;
			}
			AgencyPOIsLoader agencyPOIsLoader = new AgencyPOIsLoader(getActivity(), this.authority);
			return agencyPOIsLoader;
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
		if (this.adapter == null) {
			initAdapter();
		}
		this.mapMarkers = null; // force refresh
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		if (!isShowingListInsteadOfMap()) { // showing map
			initMapMarkers(null);
		}
		switchView(getView());
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
			if (this.locationChandedListener != null) {
				this.locationChandedListener.onLocationChanged(newLocation);
			}
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
	public void onDestroyView() {
		super.onDestroyView();
		destroyMapView();
		if (this.mapMarkers != null) {
			this.mapMarkers.clear();
			this.mapMarkers = null;
		}
		this.map = null;
		this.mapLoaded = false;
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

	private GoogleMap map = null;

	private GoogleMap getMap(MapView mapView) {
		if (this.map == null) {
			initMap(mapView);
		}
		return this.map;
	}

	private void initMap(MapView mapView) {
		this.map = mapView == null ? null : mapView.getMap();
		if (this.map != null) {
			this.map.setOnMapLoadedCallback(this);
			this.map.setOnMyLocationButtonClickListener(this);
			this.map.setOnInfoWindowClickListener(this);
			this.map.setOnMarkerClickListener(this);
			this.map.setMyLocationEnabled(true);
			this.map.setLocationSource(this);
			this.map.setOnCameraChangeListener(this);
			this.map.getUiSettings().setMyLocationButtonEnabled(true);
			this.map.getUiSettings().setIndoorLevelPickerEnabled(false);
			this.map.setTrafficEnabled(false);
			this.map.setIndoorEnabled(false);
		}
	}

	@Override
	public void onCameraChange(CameraPosition position) {
		this.showingMyLocation = this.showingMyLocation == null ? true : false;
	}

	private Boolean showingMyLocation = false;

	@Override
	public boolean onMyLocationButtonClick() {
		if (this.showingMyLocation != null && this.showingMyLocation.booleanValue()) {
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
		MTLog.d(this, "onMyLocationButtonClick() > map: %s", map);
		boolean success = MapUtils
				.updateMapPosition(getActivity(), map, mapView, true, llb.build(), MapUtils.getMapWithButtonsCameraPaddingInPx(getActivity()));
		this.showingMyLocation = null;
		return success; // handled or not
	}

	private HashMap<String, Marker> mapMarkers = null;

	private HashMap<String, String> mapMarkersIdToUUID = new HashMap<String, String>();

	private boolean mapMarkersShown = false;

	private void initMapMarkers(final String optSelectedItemUuid) {
		if (this.mapMarkers != null) {
			return;
		}
		if (!hasAgency()) {
			return;
		}
		if (this.adapter == null) {
			return;
		}
		GoogleMap map = getMap(getMapView(getView()));
		if (map == null) {
			return;
		}
		this.mapMarkers = new HashMap<String, Marker>();
		this.mapMarkersShown = false;
		new MTAsyncTask<Void, Void, HashMap<String, MarkerOptions>>() {

			private final String TAG = AgencyPOIsFragment.class.getSimpleName() + ">initMapMarkers";

			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected HashMap<String, MarkerOptions> doInBackgroundMT(Void... params) {
				HashMap<String, MarkerOptions> result = new HashMap<String, MarkerOptions>();
				if (AgencyPOIsFragment.this.adapter != null) {
					for (int i = 0; i < AgencyPOIsFragment.this.adapter.getPoisCount(); i++) {
						POIManager poim = AgencyPOIsFragment.this.adapter.getItem(i);
						if (poim != null) {
							LatLng poiLatLng = new LatLng(poim.poi.getLat(), poim.poi.getLng());
							MarkerOptions poiMarkerOptions = new MarkerOptions() //
									.title(poim.poi.getName()) //
									.position(poiLatLng) //
									.icon(getBitmapDescriptor(poim));
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
							AgencyPOIsFragment.this.mapMarkers.put(uuidMarkerOptions.getKey(), poiMarker);
							AgencyPOIsFragment.this.mapMarkersIdToUUID.put(poiMarker.getId(), uuidMarkerOptions.getKey());
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
				return AgencyPOIsFragment.class.getSimpleName() + ">onInfoWindowClick";
			}

			@Override
			protected POIManager doInBackgroundMT(Marker... params) {
				Marker marker = params == null || params.length == 0 ? null : params[0];
				String uuid = marker == null ? null : AgencyPOIsFragment.this.mapMarkersIdToUUID.get(marker.getId());
				if (TextUtils.isEmpty(uuid)) {
					return null;
				}
				return AgencyPOIsFragment.this.adapter.getItem(uuid);
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
	private boolean mapLayoutReady = false;

	private ViewTreeObserver.OnGlobalLayoutListener mapViewOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

		@Override
		public void onGlobalLayout() {
			if (AgencyPOIsFragment.this.mapView != null) {
				SupportFactory.get().removeOnGlobalLayoutListener(AgencyPOIsFragment.this.mapView.getViewTreeObserver(), this);
				AgencyPOIsFragment.this.mapLayoutReady = true;
				updateMapPosition(false);
			}
		}
	};

	private boolean mapLoaded = false;

	@Override
	public void onMapLoaded() {
		this.mapLoaded = true;
		updateMapPosition(false);
	}

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

	private BitmapDescriptor getBitmapDescriptor(POIManager optPoim) {
		try {
			int markerColor = Color.BLACK;
			if (optPoim != null && optPoim.poi instanceof RouteTripStop) {
				markerColor = ((RouteTripStop) optPoim.poi).route.getColorInt();
			} else if (optPoim != null && optPoim.poi instanceof Module) {
				markerColor = ((Module) optPoim.poi).getColorInt();
			} else {
				AgencyProperties agency = getAgencyOrNull();
				if (agency != null) {
					markerColor = agency.getColorInt();
				}
			}
			if (markerColor == Color.BLACK) {
				markerColor = Color.DKGRAY;
			}
			int bitmapResId = R.drawable.ic_place_white_slim;
			return BitmapDescriptorFactory.fromBitmap(ColorUtils.colorizeBitmapResource(getActivity(), markerColor, bitmapResId));
		} catch (Exception e) {
			MTLog.w(this, e, "Error while setting custom marker color!");
			return BitmapDescriptorFactory.defaultMarker();
		}
	}

	private MenuItem listMapToggleMenuItem;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (menu.findItem(R.id.menu_toggle_list_map) == null) {
			inflater.inflate(R.menu.menu_agency_pois, menu);
			this.listMapToggleMenuItem = menu.findItem(R.id.menu_toggle_list_map);
			if (!this.fragmentVisible) {
				this.listMapToggleMenuItem.setVisible(false);
			}
		} else {
			this.listMapToggleMenuItem = menu.findItem(R.id.menu_toggle_list_map);
		}
		updateListMapToggleMenuItem();
	}

	private void updateListMapToggleMenuItem() {
		if (!this.fragmentVisible) {
			return;
		}
		if (this.listMapToggleMenuItem == null) {
			return;
		}
		this.listMapToggleMenuItem
				.setIcon(isShowingListInsteadOfMap() ? R.drawable.ic_action_action_map_holo_dark : R.drawable.ic_action_action_list_holo_dark);
		this.listMapToggleMenuItem.setTitle(isShowingListInsteadOfMap() ? R.string.menu_action_map : R.string.menu_action_list);
		this.listMapToggleMenuItem.setVisible(true);
	}

	private Boolean showingListInsteadOfMap = null;

	private boolean isShowingListInsteadOfMap() {
		if (this.showingListInsteadOfMap == null) {
			boolean showingListInsteadOfMapLastSet = PreferenceUtils.getPrefDefault(getActivity(),
					PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET,
					PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT);
			this.showingListInsteadOfMap = TextUtils.isEmpty(this.authority) ? showingListInsteadOfMapLastSet : PreferenceUtils.getPrefDefault(getActivity(),
					PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(this.authority), showingListInsteadOfMapLastSet);
		}
		return this.showingListInsteadOfMap.booleanValue();
	}

	private void checkIfShowingListInsteadOfMapChanged() {
		if (this.showingListInsteadOfMap == null) {
			return;
		}
		boolean showingListInsteadOfMapLastSet = PreferenceUtils.getPrefDefault(getActivity(),
				PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET, PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT);
		boolean newShowingListInsteadOfMap = TextUtils.isEmpty(this.authority) ? showingListInsteadOfMapLastSet : PreferenceUtils.getPrefDefault(getActivity(),
				PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(this.authority), showingListInsteadOfMapLastSet);
		if (newShowingListInsteadOfMap != this.showingListInsteadOfMap.booleanValue()) {
			setShowingListInsteadOfMap(newShowingListInsteadOfMap);
		}
	}

	private void setShowingListInsteadOfMap(boolean newShowingListInsteadOfMap) {
		if (this.showingListInsteadOfMap != null && this.showingListInsteadOfMap.booleanValue() == newShowingListInsteadOfMap) {
			return; // nothing changed
		}
		this.showingListInsteadOfMap = newShowingListInsteadOfMap; // switching
		PreferenceUtils.savePrefDefault(getActivity(), PreferenceUtils.PREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP_LAST_SET,
				this.showingListInsteadOfMap.booleanValue(), false);
		if (!TextUtils.isEmpty(this.authority)) {
			PreferenceUtils.savePrefDefault(getActivity(), PreferenceUtils.getPREFS_AGENCY_POIS_SHOWING_LIST_INSTEAD_OF_MAP(this.authority),
					this.showingListInsteadOfMap.booleanValue(), false);
		}
		if (this.adapter != null) {
			setupView(getView());
			switchView(getView());
			if (!this.showingListInsteadOfMap) {
				initMapMarkers(null);
			}
		}
		updateListMapToggleMenuItem();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!this.fragmentVisible) {
			return false; // not handled
		}
		switch (item.getItemId()) {
		case R.id.menu_toggle_list_map:
			setShowingListInsteadOfMap(!isShowingListInsteadOfMap()); // switching
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}

	private void switchView(View view) {
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
		if (isShowingListInsteadOfMap()) { // list
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
			((AbsListView) view.findViewById(R.id.list)).setFastScrollEnabled(true); // long list
			linkAdapterWithListView(view);
		}
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
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

}
