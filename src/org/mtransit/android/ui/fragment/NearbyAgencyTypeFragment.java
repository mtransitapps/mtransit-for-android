package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.LocationUtils.AroundDiff;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.NearbyPOIListLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.widget.ListViewSwipeRefreshLayout;

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

public class NearbyAgencyTypeFragment extends MTFragmentV4 implements VisibilityAwareFragment, LoaderManager.LoaderCallbacks<List<POIManager>>,
		NearbyFragment.NearbyLocationListener {

	private static final int NEARBY_POIS_LOADER = 0;

	private static final String TAG = NearbyAgencyTypeFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG + "-" + this.type;
	}

	private static final String EXTRA_TYPE_ID = "extra_type_id";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";
	private static final String EXTRA_NEARBY_LOCATION = "extra_nearby_location";
	private static final String EXTRA_USER_LOCATION = "extra_user_location";

	private DataSourceType type;

	private POIArrayAdapter adapter;

	private AroundDiff ad = LocationUtils.DEFAULT_AROUND_DIFF;

	private String emptyText;

	private Location nearbyLocation;

	private Location userLocation;

	private ListViewSwipeRefreshLayout swipeRefreshLayout;

	private int fragmentPosition = -1;

	private int lastVisisbleFragmentPosition = -1;

	private boolean resumed = false;

	private boolean fragmentVisible = false;

	private WeakReference<NearbyFragment> nearbyFragmentWR;

	public static NearbyAgencyTypeFragment newInstance(int fragmentPosition, int lastVisisbleFragmentPosition, DataSourceType type, Location nearbyLocationOpt,
			Location userLocationOpt) {
		NearbyAgencyTypeFragment f = new NearbyAgencyTypeFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_TYPE_ID, type.getId());
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
		}
		if (lastVisisbleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisisbleFragmentPosition);
		}
		if (nearbyLocationOpt != null) {
			args.putParcelable(EXTRA_NEARBY_LOCATION, nearbyLocationOpt);
		}
		if (userLocationOpt != null) {
			args.putParcelable(EXTRA_USER_LOCATION, userLocationOpt);
		}
		f.setArguments(args);
		return f;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.nearbyLocation != null) {
			outState.putParcelable(EXTRA_NEARBY_LOCATION, this.nearbyLocation);
		}
		if (this.userLocation != null) {
			outState.putParcelable(EXTRA_USER_LOCATION, this.userLocation);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_nearby_agency_type, container, false);
		setupView(view);
		return view;
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		this.swipeRefreshLayout = (ListViewSwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
		this.swipeRefreshLayout.setColorSchemeResources(R.color.mt_blue_malibu, R.color.mt_blue_smalt, R.color.mt_blue_malibu, R.color.mt_blue_smalt);
		if (this.adapter != null) {
			inflateList(view);
			this.adapter.setListView((AbsListView) getView().findViewById(R.id.list));
		}
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		// TYPE ID
		Integer typeId = BundleUtils.getInt(EXTRA_TYPE_ID, savedInstanceState, getArguments());
		if (typeId != null) {
			this.type = DataSourceType.parseId(typeId);
			((MainActivity) getActivity()).notifyABChange();
		}
		// FRAGMENT POSITION
		Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (fragmentPosition != null) {
			if (fragmentPosition.intValue() >= 0) {
				this.fragmentPosition = fragmentPosition.intValue();
			} else {
				this.fragmentPosition = -1;
			}
		}
		// LAST VISIBLE FRAGMENT POSITION
		Integer lastVisisbleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (lastVisisbleFragmentPosition != null) {
			if (lastVisisbleFragmentPosition.intValue() >= 0) {
				this.lastVisisbleFragmentPosition = lastVisisbleFragmentPosition;
			} else {
				this.lastVisisbleFragmentPosition = -1;
			}
		}
		// NEARBY LOCATION
		Location nearbyLocation = BundleUtils.getParcelable(EXTRA_NEARBY_LOCATION, savedInstanceState, getArguments());
		if (nearbyLocation != null) {
			useNewNearbyLocation(nearbyLocation, false);
		}
		// USER LOCATION
		Location userLocation = BundleUtils.getParcelable(EXTRA_USER_LOCATION, savedInstanceState, getArguments());
		if (userLocation != null) {
			onUserLocationChanged(userLocation);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
		initAdapter();
	}

	private void initAdapter() {
		this.adapter = new POIArrayAdapter(getActivity());
		this.adapter.setTag(this.type.toString());
		setupView(getView());
		switchView();
		NearbyFragment nearbyFragment = this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get();
		if (nearbyFragment != null) {
			this.swipeRefreshLayout.setOnRefreshListener(nearbyFragment);
		}
	}

	public void setNearbyFragment(NearbyFragment nearbyFragment) {
		this.nearbyFragmentWR = new WeakReference<NearbyFragment>(nearbyFragment);
	}

	public void setSwipeRefreshLayoutRefreshing(boolean refreshing) {
		if (this.swipeRefreshLayout != null) {
			if (refreshing) {
				if (!this.swipeRefreshLayout.isRefreshing()) {
					this.swipeRefreshLayout.setRefreshing(true);
				}
			} else {
				this.swipeRefreshLayout.setRefreshing(false);
			}
		}

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
		if (!this.resumed) {
			return;
		}
		if (this.fragmentPosition == visisbleFragmentPosition) {
			onFragmentVisisble();
		} else {
			onFragmentInvisible();
		}
	}

	private void onFragmentVisisble() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		this.fragmentVisible = true;
		if (this.adapter == null) {
			initAdapter();
		} else {
			if (this.adapter.getPoisCount() > 0) {
				this.adapter.onResume();
				this.adapter.refreshFavorites();
			}
		}
		NearbyFragment nearbyFragment = this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get();
		if (nearbyFragment != null) {
			useNewNearbyLocation(nearbyFragment.getNearbyLocation(), false); // nearby location was unknown yet or reset while not visible
			onUserLocationChanged(nearbyFragment.getUserLocation()); // user location was unknown yet or discarded while not visible
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

	@Override
	public void onPause() {
		super.onPause();
		this.resumed = false;
		onFragmentInvisible();
	}


	@Override
	public void onResume() {
		super.onResume();
		this.resumed = true;
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

	@Override
	public void onNearbyLocationChanged(Location newLocation) {
		if (newLocation != null) {
			useNewNearbyLocation(newLocation, false); // true);
		} else {
			useNewNearbyLocation(null, true);
		}
	}

	private void useNewNearbyLocation(Location newNearbyLocation, boolean force) {
		if (!force && (newNearbyLocation == null || !this.fragmentVisible || LocationUtils.areTheSame(newNearbyLocation, this.nearbyLocation))) {
			return;
		}
		this.nearbyLocation = newNearbyLocation;
		// reset all the things
		if (this.adapter != null) {
			this.adapter.clear();
		}
		switchView();
		final View view = getView();
		if (view != null) {
			if (view.findViewById(R.id.list) != null) {
				((AbsListView) view.findViewById(R.id.list)).scrollTo(0, 0);
			}
		}
		this.ad = LocationUtils.DEFAULT_AROUND_DIFF;
		if (this.nearbyLocation != null) {
			getActivity().getSupportLoaderManager().restartLoader(NEARBY_POIS_LOADER, null, this);
		}
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			if (this.fragmentVisible) {
				if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
					this.userLocation = newLocation;
					if (this.adapter != null) {
						this.adapter.setLocation(this.userLocation);
					}
				}
			}
		}
	}

	private void switchView() {
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading();
		} else if (this.adapter.getPoisCount() == 0) {
			showEmpty();
		} else {
			showList();
		}
	}

	private void showList() {
		// loading
		if (getView().findViewById(R.id.loading) != null) { // IF inflated/present DO
			getView().findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		// empty
		if (getView().findViewById(R.id.empty) != null) { // IF inflated/present DO
			getView().findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		// list
		inflateList(getView());
		getView().findViewById(R.id.list).setVisibility(View.VISIBLE); // show
	}

	private void inflateList(View view) {
		if (view.findViewById(R.id.list) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.list_stub)).inflate(); // inflate
			this.swipeRefreshLayout.setListViewWR((AbsListView) view.findViewById(R.id.list));
		}
	}

	private void showLoading() {
		// list
		if (getView().findViewById(R.id.list) != null) { // IF inflated/present DO
			getView().findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		// empty
		if (getView().findViewById(R.id.empty) != null) { // IF inflated/present DO
			getView().findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		// loading
		if (getView().findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) getView().findViewById(R.id.loading_stub)).inflate(); // inflate
			this.swipeRefreshLayout.setLoadingViewWR(getView().findViewById(R.id.loading));
		}
		getView().findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty() {
		if (getView().findViewById(R.id.list) != null) { // IF inflated/present DO
			getView().findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		// loading
		if (getView().findViewById(R.id.loading) != null) { // IF inflated/present DO
			getView().findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		// empty
		if (getView().findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) getView().findViewById(R.id.empty_stub)).inflate(); // inflate
			this.swipeRefreshLayout.setEmptyViewWR(getView().findViewById(R.id.empty));
		}
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) getView().findViewById(R.id.empty_text)).setText(this.emptyText);
		}
		getView().findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public Loader<List<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case NEARBY_POIS_LOADER:
			if (this.nearbyLocation == null) {
				return null;
			}
			NearbyPOIListLoader nearbyPOIListLoader = new NearbyPOIListLoader(getActivity(), this.type, this.nearbyLocation.getLatitude(),
					this.nearbyLocation.getLongitude(), this.ad.aroundDiff, LocationUtils.MIN_NEARBY_LIST_COVERAGE, LocationUtils.MAX_NEARBY_LIST);
			return nearbyPOIListLoader;
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
		int dataSize = CollectionUtils.getSize(data);
		if (this.nearbyLocation != null) {
			float distanceInKm = LocationUtils.getAroundCoveredDistance(this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude(), ad.aroundDiff) / 1000;
			this.emptyText = String.format("%s stops found within %s km", dataSize, distanceInKm);
		}
		LocationUtils.removeTooMuchWhenNotInCoverage(data, LocationUtils.MIN_NEARBY_LIST_COVERAGE, LocationUtils.MAX_NEARBY_LIST);
		// IF not enough POIs found AND maximum around location not reached DO
		if (dataSize < LocationUtils.MIN_NEARBY_LIST && ad.aroundDiff < LocationUtils.MAX_AROUND_DIFF) {
			// try with larger around location
			LocationUtils.incAroundDiff(this.ad);
			getActivity().getSupportLoaderManager().restartLoader(NEARBY_POIS_LOADER, null, this);
		} else {
			// show found POIs (or empty list)
			this.adapter.setPois(data);
			this.adapter.updateDistanceNowAsync(this.userLocation);
			this.adapter.refreshFavorites();
			switchView();
		}
	}

}
