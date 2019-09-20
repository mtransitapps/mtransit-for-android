package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.mtransit.android.R;
import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.di.Injection;
import org.mtransit.android.provider.permission.LocationPermissionProvider;
import org.mtransit.android.task.RTSTripStopsLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.view.MapViewController;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.LoaderUtils;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import android.app.Activity;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.core.util.Pair;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;

public class RTSTripStopsFragment extends MTFragmentV4 implements
		VisibilityAwareFragment,
		LoaderManager.LoaderCallbacks<ArrayList<POIManager>>,
		MTActivityWithLocation.UserLocationListener,
		MapViewController.MapMarkerProvider,
		IContext,
		MapViewController.MapListener {

	private static final String TAG = RTSTripStopsFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG + "-" + this.tripId;
	}

	private static final String EXTRA_AGENCY_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_TRIP_ID = "extra_trip_id";
	private static final String EXTRA_TRIP_STOP_ID = "extra_trip_stop_id";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";
	private static final String EXTRA_SHOWING_LIST_INSTEAD_OF_MAP = "extra_showing_list_instead_of_map";
	private static final String EXTRA_CLOSEST_POI_SHOWN = "extra_closest_poi_shown";

	public static RTSTripStopsFragment newInstance(int fragmentPosition, int lastVisibleFragmentPosition, String authority, long tripId, Integer optStopId,
			boolean showingListInsteadOfMap) {
		RTSTripStopsFragment f = new RTSTripStopsFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AGENCY_AUTHORITY, authority);
		f.authority = authority;
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

	private Long tripId;
	private int stopId = -1;
	private boolean closestPOIShow = false;
	private String authority;
	private POIArrayAdapter adapter;
	private Location userLocation;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private MapViewController mapViewController =
			new MapViewController(getLogTag(), this, this, true, true, true, false, false, false, 0, false, true, false, true, false);

	@NonNull
	private final LocationPermissionProvider locationPermissionProvider;

	public RTSTripStopsFragment() {
		super();
		this.locationPermissionProvider = Injection.providesLocationPermissionProvider();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
		this.mapViewController.setLocationPermissionGranted(this.locationPermissionProvider.permissionsGranted(this));
		this.mapViewController.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		this.mapViewController.onDetach();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
		this.mapViewController.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_rts_trip_stops, container, false);
		this.mapViewController.onCreateView(view, savedInstanceState);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setupView(view);
		this.mapViewController.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AGENCY_AUTHORITY, this.authority);
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
		this.mapViewController.onSaveInstanceState(outState);
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AGENCY_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
		}
		Long newTripId = BundleUtils.getLong(EXTRA_TRIP_ID, bundles);
		if (newTripId != null && !newTripId.equals(this.tripId)) {
			this.tripId = newTripId;
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
		this.adapter.setTag(this.authority + "-" + this.tripId);
		this.mapViewController.setTag(getLogTag());
	}

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setShowExtra(false);
	}

	@Override
	public POIManager getClosestPOI() {
		return this.adapter == null ? null : this.adapter.getClosestPOI();
	}

	@Override
	public POIManager getPOI(String uuid) {
		return this.adapter == null ? null : this.adapter.getItem(uuid);
	}

	@Override
	public Collection<POIManager> getPOIs() {
		if (this.adapter == null || !this.adapter.isInitialized()) {
			return null;
		}
		HashSet<POIManager> pois = new HashSet<POIManager>();
		if (this.adapter != null && this.adapter.hasPois()) {
			for (int i = 0; i < this.adapter.getPoisCount(); i++) {
				pois.add(this.adapter.getItem(i));
			}
		}
		return pois;
	}

	@Override
	public Collection<MapViewController.POIMarker> getPOMarkers() {
		return null;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		this.mapViewController.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCameraChange(LatLngBounds latLngBounds) {
		// DO NOTHING
	}

	@Override
	public void onMapClick(LatLng position) {
		// DO NOTHING
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
		if (this.showingListInsteadOfMap) { // list
			inflateList(view); // inflate ASAP for view state restore
			switchView(view);
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
				&& ((this.fragmentPosition == visibleFragmentPosition && this.fragmentVisible) //
				|| //
				(this.fragmentPosition != visibleFragmentPosition && !this.fragmentVisible)) //
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
		if (!this.showingListInsteadOfMap) { // map
			this.mapViewController.onPause();
		}
	}

	@Override
	public boolean isFragmentVisible() {
		return this.fragmentVisible;
	}

	private void onFragmentVisible() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		if (!isResumed()) {
			return;
		}
		this.fragmentVisible = true;
		if (!this.showingListInsteadOfMap) { // map
			this.mapViewController.onResume();
		}
		View view = getView();
		switchView(view);
		if (this.adapter == null || !this.adapter.isInitialized()) {
			LoaderUtils.restartLoader(this, POIS_LOADER, null, this);
		} else {
			this.adapter.onResume(getActivity(), this.userLocation);
		}
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
		}
	}

	private static final int POIS_LOADER = 0;

	@NonNull
	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, @Nullable Bundle args) {
		switch (id) {
		case POIS_LOADER:
			if (this.tripId == null || TextUtils.isEmpty(this.authority)) {
				CrashUtils.w(this, "onCreateLoader() > no trip '%s' or authority '%s' !", this.tripId, this.authority);
				return null;
			}
			return new RTSTripStopsLoader(requireContext(), this.authority, this.tripId);
		default:
			CrashUtils.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<ArrayList<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
		this.mapViewController.notifyMarkerChanged(this);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<ArrayList<POIManager>> loader, ArrayList<POIManager> data) {
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
		this.adapter.setPois(data);
		this.mapViewController.notifyMarkerChanged(this);
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
		}
		switchView(view);
	}

	private Pair<Integer, String> findStopIndexUuid(int stopId, ArrayList<POIManager> pois) {
		for (int i = 0; i < pois.size(); i++) {
			POIManager poim = pois.get(i);
			if (poim != null && poim.poi instanceof RouteTripStop) {
				RouteTripStop rts = (RouteTripStop) poim.poi;
				if (rts.getStop().getId() == stopId) {
					return new Pair<Integer, String>(i, poim.poi.getUUID());
				}
			}
		}
		return null;
	}

	private Pair<Integer, String> findClosestPOIIndexUuid(ArrayList<POIManager> pois) {
		if (this.userLocation != null && CollectionUtils.getSize(pois) > 0) {
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
	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null) {
			this.mapViewController.setLocationPermissionGranted(this.locationPermissionProvider.permissionsGranted(this));
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
			if (this.adapter != null) {
				this.adapter.setLocation(newLocation);
			}
		}
		this.mapViewController.onUserLocationChanged(newLocation);
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
		if (this.fragmentPosition >= 0 && this.fragmentPosition == this.lastVisibleFragmentPosition) {
			onFragmentVisible();
		} // ELSE would be call later
		if (this.adapter != null) {
			this.adapter.setActivity(getActivity());
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		this.mapViewController.onLowMemory();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		this.mapViewController.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
		}
		this.mapViewController.onDestroy();
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
		if (this.showingListInsteadOfMap) { // list
			this.mapViewController.hideMap();
			inflateList(view);
			view.findViewById(R.id.list).setVisibility(View.VISIBLE); // show
		} else { // map
			this.mapViewController.showMap(view);
			if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
				view.findViewById(R.id.list).setVisibility(View.GONE); // hide
			}
		}
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
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
		this.mapViewController.hideMap();
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		this.mapViewController.hideMap();
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.empty_stub)).inflate(); // inflate
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
			View view = getView();
			setupView(view);
			if (!this.showingListInsteadOfMap) { // map
				this.mapViewController.onResume();
			} else { // list
				this.mapViewController.onPause();
			}
			switchView(view);
		}
	}
}
