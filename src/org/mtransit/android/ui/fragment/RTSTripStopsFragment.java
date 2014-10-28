package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.RTSTripStopsLoader;
import org.mtransit.android.ui.MTActivityWithLocation;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.TextView;

public class RTSTripStopsFragment extends MTFragmentV4 implements VisibilityAwareFragment, LoaderManager.LoaderCallbacks<List<POIManager>>,
		MTActivityWithLocation.UserLocationListener {

	private static final String TAG = RTSTripStopsFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String EXTRA_AGENCY_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_TRIP_ID = "extra_trip_id";
	private static final String EXTRA_STOP_ID = "extra_stop_id";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";
	private static final String EXTRA_USER_LOCATION = "extra_user_location";
	private static final String EXTRA_SELECTED_ITEM_POSITION = "extra_selected_item_position";

	public static RTSTripStopsFragment newInstance(int fragmentPosition, int lastVisisbleFragmentPosition, String authority, Trip trip, Integer optStopId,
			Location userLocationOpt) {
		RTSTripStopsFragment f = new RTSTripStopsFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AGENCY_AUTHORITY, authority);
		args.putInt(EXTRA_TRIP_ID, trip.id);
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
		}
		if (lastVisisbleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisisbleFragmentPosition);
		}
		if (optStopId != null) {
			args.putInt(EXTRA_STOP_ID, optStopId);
		}
		if (userLocationOpt != null) {
			args.putParcelable(EXTRA_USER_LOCATION, userLocationOpt);
		}
		f.setArguments(args);
		return f;
	}

	private Trip trip;
	private String authority;
	private Integer tripId;
	private Integer stopId;
	private POIArrayAdapter adapter;
	private Location userLocation;
	private int fragmentPosition = -1;
	private int lastVisisbleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private String emptyText = null;
	private Integer currentSelectedItemPosition = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_rts_trip_stops, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.userLocation != null) {
			outState.putParcelable(EXTRA_USER_LOCATION, this.userLocation);
		}
		final View view = getView();
		if (view != null && view.findViewById(R.id.list) != null) {
			outState.putInt(EXTRA_SELECTED_ITEM_POSITION, ((AbsListView) view.findViewById(R.id.list)).getFirstVisiblePosition());
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		this.authority = BundleUtils.getString(EXTRA_AGENCY_AUTHORITY, savedInstanceState, getArguments());
		this.tripId = BundleUtils.getInt(EXTRA_TRIP_ID, savedInstanceState, getArguments());
		if (this.tripId != null) {
			this.trip = DataSourceProvider.findRTSTrip(getActivity(), UriUtils.newContentUri(this.authority), this.tripId);
		}
		final Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (fragmentPosition != null) {
			if (fragmentPosition.intValue() >= 0) {
				this.fragmentPosition = fragmentPosition.intValue();
			} else {
				this.fragmentPosition = -1;
			}
		}
		final Integer lastVisisbleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (lastVisisbleFragmentPosition != null) {
			if (lastVisisbleFragmentPosition.intValue() >= 0) {
				this.lastVisisbleFragmentPosition = lastVisisbleFragmentPosition;
			} else {
				this.lastVisisbleFragmentPosition = -1;
			}
		}
		this.stopId = BundleUtils.getInt(EXTRA_STOP_ID, savedInstanceState, getArguments());
		final Location userLocation = BundleUtils.getParcelable(EXTRA_USER_LOCATION, savedInstanceState, getArguments());
		if (userLocation != null) {
			onUserLocationChanged(userLocation);
		}
		this.currentSelectedItemPosition = BundleUtils.getInt(EXTRA_SELECTED_ITEM_POSITION, savedInstanceState, getArguments());
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	private void initAdapter() {
		this.adapter = new POIArrayAdapter(getActivity());
		this.adapter.setTag(this.authority + this.tripId);
		this.adapter.setShowRTSExtra(false);
		final View view = getView();
		setupView(view);
		switchView(view);
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		inflateList(view);
		this.adapter.setListView((AbsListView) view.findViewById(R.id.list));
	}

	@Override
	public void setFragmentVisisbleAtPosition(int visisbleFragmentPosition) {
		if (this.lastVisisbleFragmentPosition == visisbleFragmentPosition //
				&& (//
				(this.fragmentPosition == visisbleFragmentPosition && this.fragmentVisible) //
				|| //
				(this.fragmentPosition != visisbleFragmentPosition && !this.fragmentVisible) //
				) //
		) {
			return;
		}
		this.lastVisisbleFragmentPosition = visisbleFragmentPosition;
		if (this.fragmentPosition < 0) {
			return;
		}
		if (this.fragmentPosition == visisbleFragmentPosition) {
			onFragmentVisisble();
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
	}

	private void onFragmentVisisble() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		this.fragmentVisible = true;
		if (this.adapter == null) {
			initAdapter();
			getLoaderManager().restartLoader(POIS_LOADER, null, this);
		} else {
			if (this.adapter.getPoisCount() > 0) {
				this.adapter.onResume(getActivity());
			}
		}
		final Activity activity = getActivity();
		if (activity != null && activity instanceof MTActivityWithLocation) {
			onUserLocationChanged(((MTActivityWithLocation) activity).getUserLocation()); // user location was unknown yet or discarded while not visible
		}
	}

	private static final int POIS_LOADER = 0;

	@Override
	public Loader<List<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POIS_LOADER:
			final RTSTripStopsLoader rtsTripStopsLoader = new RTSTripStopsLoader(getActivity(), this.trip, this.authority);
			return rtsTripStopsLoader;
		default:
			MTLog.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<List<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
			this.adapter.onPause();
		}
	}

	@Override
	public void onLoadFinished(Loader<List<POIManager>> loader, List<POIManager> data) {
		if (this.currentSelectedItemPosition == null) {
			if (this.stopId != null) {
				this.currentSelectedItemPosition = findStopIndex(this.stopId.intValue(), data);
			}
			if (this.currentSelectedItemPosition == null) {
				this.currentSelectedItemPosition = findClosestPOIIndex(data);
			}
		}
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		final View view = getView();
		if (this.currentSelectedItemPosition != null && this.currentSelectedItemPosition > 0) {
			((AbsListView) view.findViewById(R.id.list)).setSelection(this.currentSelectedItemPosition - 1); // show 1 more stop on top of the list
		}
		switchView(view);
	}

	private Integer findStopIndex(int stopId, List<POIManager> pois) {
		for (int i = 0; i < pois.size(); i++) {
			final POIManager poim = pois.get(i);
			if (poim != null && poim.poi instanceof RouteTripStop) {
				final RouteTripStop rts = (RouteTripStop) poim.poi;
				if (rts.stop.id == stopId) {
					return i;
				}
			}
		}
		return null;
	}

	private Integer findClosestPOIIndex(List<POIManager> pois) {
		if (this.userLocation != null) {
			LocationUtils.updateDistance(pois, this.userLocation.getLatitude(), this.userLocation.getLongitude());
			ArrayList<POIManager> sortedPOIs = new ArrayList<POIManager>(pois);
			CollectionUtils.sort(sortedPOIs, POIManager.POI_DISTANCE_COMPARATOR);
			final String closestPoiUuid = sortedPOIs.get(0).poi.getUUID();
			for (int i = 0; i < pois.size(); i++) {
				if (pois.get(i).poi.getUUID().equals(closestPoiUuid)) {
					return i;
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
					this.adapter.setLocation(this.userLocation);
				}
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		onFragmentInvisible();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.fragmentPosition < 0 || this.fragmentPosition == this.lastVisisbleFragmentPosition) {
			onFragmentVisisble();
		} // ELSE would be call later
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
		}
	}

	private void switchView(View view) {
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading(view);
		} else if (this.adapter.getPoisCount() == 0) {
			showEmpty(view);
		} else {
			showList(view);
		}
	}

	private void showList(View view) {
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		inflateList(view);
		view.findViewById(R.id.list).setVisibility(View.VISIBLE); // show
	}

	private void inflateList(View view) {
		if (view.findViewById(R.id.list) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.list_stub)).inflate(); // inflate
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
