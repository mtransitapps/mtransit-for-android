package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LoaderUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.NearbyPOIListLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.widget.ListViewSwipeRefreshLayout;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;

public class NearbyAgencyTypeFragment extends MTFragmentV4 implements VisibilityAwareFragment, LoaderManager.LoaderCallbacks<ArrayList<POIManager>>,
		NearbyFragment.NearbyLocationListener, DataSourceProvider.ModulesUpdateListener {

	private static final String TAG = NearbyAgencyTypeFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG + "-" + this.typeId;
	}

	private static final String EXTRA_TYPE_ID = "extra_type_id";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";

	public static NearbyAgencyTypeFragment newInstance(int fragmentPosition, int lastVisibleFragmentPosition, int typeId, Location optNearbyLocation) {
		NearbyAgencyTypeFragment f = new NearbyAgencyTypeFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_TYPE_ID, typeId);
		f.typeId = typeId;
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
			f.fragmentPosition = fragmentPosition;
		}
		if (lastVisibleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisibleFragmentPosition);
			f.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}
		f.setArguments(args);
		return f;
	}

	private POIArrayAdapter adapter;
	private LocationUtils.AroundDiff ad = LocationUtils.getNewDefaultAroundDiff();
	private Double lastAroundDiff = null;
	private Location nearbyLocation;
	private Location userLocation;
	private ListViewSwipeRefreshLayout swipeRefreshLayout;
	private boolean swipeRefreshLayoutEnabled = true;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private WeakReference<NearbyFragment> nearbyFragmentWR;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
		DataSourceProvider.addModulesUpdateListerner(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_nearby_agency_type, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.typeId != null) {
			outState.putInt(EXTRA_TYPE_ID, this.typeId);
		}
		if (this.fragmentPosition >= 0) {
			outState.putInt(EXTRA_FRAGMENT_POSITION, this.fragmentPosition);
		}
		if (this.lastVisibleFragmentPosition >= 0) {
			outState.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, this.lastVisibleFragmentPosition);
		}
		super.onSaveInstanceState(outState);
	}

	private Integer typeId;

	public Integer getTypeId() {
		return this.typeId;
	}

	private ArrayList<String> typeAgenciesAuthority;

	private void resetTypeAgenciesAuthority() {
		this.typeAgenciesAuthority = null;
	}

	private ArrayList<String> getTypeAgenciesAuthorityOrNull() {
		if (!hasTypeAgenciesAuthority()) {
			return null;
		}
		return this.typeAgenciesAuthority;
	}

	private boolean hasTypeAgenciesAuthority() {
		if (this.typeAgenciesAuthority == null) {
			initTypeAgenciesAuthorityAsync();
			return false;
		}
		return true;
	}

	private void initTypeAgenciesAuthorityAsync() {
		if (this.loadTypeAgenciesAuthorityAsyncTask != null && this.loadTypeAgenciesAuthorityAsyncTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (this.nearbyLocation == null || this.typeId == null) {
			return;
		}
		this.loadTypeAgenciesAuthorityAsyncTask = new LoadTypeAgenciesAuthorityAsyncTask();
		this.loadTypeAgenciesAuthorityAsyncTask.execute();
	}

	private LoadTypeAgenciesAuthorityAsyncTask loadTypeAgenciesAuthorityAsyncTask = null;

	private class LoadTypeAgenciesAuthorityAsyncTask extends MTAsyncTask<Void, Void, Boolean> {

		@Override
		public String getLogTag() {
			return NearbyAgencyTypeFragment.class.getSimpleName() + ">" + LoadTypeAgenciesAuthorityAsyncTask.class.getSimpleName();
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initTypeAgenciesAuthoritySync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewTypeAgenciesAuthority();
			}
		}
	};

	private boolean initTypeAgenciesAuthoritySync() {
		if (this.typeAgenciesAuthority != null) {
			return false;
		}
		if (this.nearbyLocation != null && this.typeId != null) {
			this.typeAgenciesAuthority = NearbyPOIListLoader.findTypeAgenciesAuthority(getActivity(), this.typeId, this.nearbyLocation.getLatitude(),
					this.nearbyLocation.getLongitude(), this.ad.aroundDiff, this.lastAroundDiff);
		}
		return this.typeAgenciesAuthority != null;
	}

	private void applyNewTypeAgenciesAuthority() {
		if (this.typeAgenciesAuthority == null) {
			return;
		}
		if (this.nearbyLocation != null) {
			LoaderUtils.restartLoader(getLoaderManager(), NEARBY_POIS_LOADER, null, this);
		}
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		this.swipeRefreshLayout = (ListViewSwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
		this.swipeRefreshLayout.setColorSchemeColors(ThemeUtils.resolveColorAttribute(getActivity(), R.attr.colorAccent));
		setSwipeRefreshLayoutEnabled(this.swipeRefreshLayoutEnabled);
		inflateList(view);
		this.adapter.setListView((AbsListView) view.findViewById(R.id.list));
		NearbyFragment nearbyFragment = this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get();
		if (nearbyFragment != null) {
			this.swipeRefreshLayout.setOnRefreshListener(nearbyFragment);
		}
	}

	private void restoreInstanceState(Bundle... bundles) {
		Integer newTypeId = BundleUtils.getInt(EXTRA_TYPE_ID, bundles);
		if (newTypeId != null && !newTypeId.equals(this.typeId)) {
			this.typeId = newTypeId;
			resetTypeAgenciesAuthority();
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
		this.adapter.setTag(String.valueOf(this.typeId));
	}

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
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

	public void setSwipeRefreshLayoutEnabled(boolean swipeRefreshLayoutEnabled) {
		this.swipeRefreshLayoutEnabled = swipeRefreshLayoutEnabled;
		if (this.swipeRefreshLayout != null) {
			this.swipeRefreshLayout.setRefreshEnabled(swipeRefreshLayoutEnabled);
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
		if (!isResumed()) {
			return;
		}
		if (this.fragmentPosition == visibleFragmentPosition) {
			onFragmentVisible();
		} else {
			onFragmentInvisible();
		}
	}

	private void onFragmentVisible() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		this.fragmentVisible = true;
		switchView(getView());
		if (this.adapter != null) {
			this.adapter.onResume(getActivity(), this.userLocation);
		}
		NearbyFragment nearbyFragment = this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get();
		if (nearbyFragment != null) {
			useNewNearbyLocation(nearbyFragment.getNearbyLocation(), false); // nearby location was unknown yet or reset while not visible
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
	}

	@Override
	public boolean isFragmentVisible() {
		return this.fragmentVisible;
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
		onFragmentInvisible();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.modulesUpdated) {
			getView().post(new Runnable() {
				@Override
				public void run() {
					if (NearbyAgencyTypeFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
		if (this.fragmentPosition < 0 || this.fragmentPosition == this.lastVisibleFragmentPosition) {
			onFragmentVisible();
		} // ELSE would be call later
		this.adapter.setActivity(getActivity());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		DataSourceProvider.removeModulesUpdateListerner(this);
		if (this.adapter != null) {
			this.adapter.onDestroy();
			this.adapter = null;
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
		if (!force) {
			if (newNearbyLocation == null || !this.fragmentVisible || LocationUtils.areTheSame(newNearbyLocation, this.nearbyLocation)) {
				return;
			}
		}
		this.nearbyLocation = newNearbyLocation;
		if (this.adapter != null) {
			this.adapter.clear();
		}
		View view = getView();
		switchView(view);
		if (view != null) {
			if (view.findViewById(R.id.list) != null) {
				view.findViewById(R.id.list).scrollTo(0, 0);
			}
		}
		this.ad = LocationUtils.getNewDefaultAroundDiff();
		this.lastAroundDiff = null;
		resetTypeAgenciesAuthority();
		if (this.nearbyLocation != null && hasTypeAgenciesAuthority()) {
			LoaderUtils.restartLoader(getLoaderManager(), NEARBY_POIS_LOADER, null, this);
		}
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		if (this.adapter != null && this.nearbyLocation != null && this.typeId != null) {
			FragmentActivity activity = getActivity();
			if (activity == null) {
				return;
			}
			ArrayList<String> newTypeAgenciesAuthority = NearbyPOIListLoader.findTypeAgenciesAuthority(activity, this.typeId,
					this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude(), this.ad.aroundDiff, this.lastAroundDiff);
			if (CollectionUtils.getSize(this.typeAgenciesAuthority) != CollectionUtils.getSize(newTypeAgenciesAuthority)) {
				useNewNearbyLocation(this.nearbyLocation, true); // force
			}
			this.modulesUpdated = false; // processed
		} else {
			this.modulesUpdated = false; // processed
		}
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.fragmentVisible) {
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				if (this.adapter != null) {
					this.adapter.setLocation(newLocation);
				}
			}
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
			this.swipeRefreshLayout.setListViewWR((AbsListView) view.findViewById(R.id.list));
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
			this.swipeRefreshLayout.setLoadingViewWR(view.findViewById(R.id.loading));
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
			this.swipeRefreshLayout.setEmptyViewWR(view.findViewById(R.id.empty));
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	private static final int NEARBY_POIS_LOADER = 0;

	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case NEARBY_POIS_LOADER:
			if (this.nearbyLocation == null || this.typeId == null || getTypeAgenciesAuthorityOrNull() == null) {
				return null;
			}
			float minDistanceInMeters = LocationUtils.getAroundCoveredDistanceInMeters(this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude(),
					LocationUtils.MIN_AROUND_DIFF);
			if (minDistanceInMeters < LocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS) {
				minDistanceInMeters = LocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS;
			}
			if (minDistanceInMeters < this.nearbyLocation.getAccuracy()) {
				minDistanceInMeters = this.nearbyLocation.getAccuracy();
			}
			return new NearbyPOIListLoader(getActivity(), this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude(), this.ad.aroundDiff,
					minDistanceInMeters, LocationUtils.MAX_NEARBY_LIST, false, getTypeAgenciesAuthorityOrNull());
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
		int dataSize = CollectionUtils.getSize(data);
		if (dataSize < LocationUtils.MIN_NEARBY_LIST
				&& !LocationUtils.searchComplete(this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude(), this.ad.aroundDiff)) {
			// try with larger around location
			if (dataSize == 0) {
				this.lastAroundDiff = this.ad.aroundDiff;
			} else {
				this.lastAroundDiff = null;
			}
			LocationUtils.incAroundDiff(this.ad);
			resetTypeAgenciesAuthority();
			initTypeAgenciesAuthoritySync();
			LoaderUtils.restartLoader(getLoaderManager(), NEARBY_POIS_LOADER, null, this);
		} else {
			this.adapter.setPois(data);
			if (this.fragmentVisible) {
				this.adapter.updateDistanceNowAsync(this.userLocation);
			} else {
				this.adapter.onPause();
			}
			switchView(getView());
		}
	}
}
