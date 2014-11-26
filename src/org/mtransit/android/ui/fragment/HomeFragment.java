package org.mtransit.android.ui.fragment;

import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.HomePOILoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.widget.ListViewSwipeRefreshLayout;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.TextView;

public class HomeFragment extends ABFragment implements LoaderManager.LoaderCallbacks<ArrayList<POIManager>>, MTActivityWithLocation.UserLocationListener,
		FavoriteManager.FavoriteUpdateListener, SwipeRefreshLayout.OnRefreshListener {

	private static final String TAG = HomeFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Home";

	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_NEARBY_LOCATION = "extra_nearby_location";

	public static HomeFragment newInstance(Location nearbyLocationOpt) {
		HomeFragment f = new HomeFragment();
		Bundle args = new Bundle();
		if (nearbyLocationOpt != null) {
			args.putParcelable(EXTRA_NEARBY_LOCATION, nearbyLocationOpt);
		}
		f.setArguments(args);
		return f;
	}

	private POIArrayAdapter adapter;
	private CharSequence emptyText = null;
	private Location userLocation;
	private Location nearbyLocation;
	private String nearbyLocationAddress;
	private ListViewSwipeRefreshLayout swipeRefreshLayout;
	private boolean userAwayFromNearbyLocation = true;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_home, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onRefresh() {
		initiateRefresh();
	}

	private void initiateRefresh() {
		if (LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation, 2)) {
			setSwipeRefreshLayoutRefreshing(false);
			return;
		}
		useNewNearbyLocation(this.userLocation);
	}

	@Override
	public void onModulesUpdated() {
		this.nearbyLocation = null; // force refresh
		initiateRefresh();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		final Location nearbyLocation = BundleUtils.getParcelable(EXTRA_NEARBY_LOCATION, savedInstanceState, getArguments());
		useNewNearbyLocation(nearbyLocation);
	}

	@Override
	public void onResume() {
		super.onResume();
		switchView(getView());
		if (this.adapter != null) {
			this.adapter.onResume(getActivity());
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
	}

	private MTAsyncTask<Location, Void, String> findNearbyLocationTask;

	private void findNearbyLocation() {
		if (this.findNearbyLocationTask != null) {
			this.findNearbyLocationTask.cancel(true);
		}
		this.findNearbyLocationTask = new MTAsyncTask<Location, Void, String>() {

			@Override
			public String getLogTag() {
				return HomeFragment.this.getLogTag();
			}

			@Override
			protected String doInBackgroundMT(Location... locations) {
				final Activity activity = getActivity();
				final Location nearbyLocation = locations[0];
				if (activity == null || nearbyLocation == null) {
					return null;
				}
				final Address address = LocationUtils.getLocationAddress(activity, nearbyLocation);
				if (address == null) {
					return null;
				}
				return LocationUtils.getLocationString(activity, null, address, nearbyLocation.getAccuracy());
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				boolean refreshRequired = result != null && !result.equals(HomeFragment.this.nearbyLocationAddress);
				HomeFragment.this.nearbyLocationAddress = result;
				if (refreshRequired) {
					final FragmentActivity activity = getActivity();
					if (activity != null) {
						getAbController().setABSubtitle(HomeFragment.this, getABSubtitle(activity), false);
						getAbController().setABReady(HomeFragment.this, isABReady(), true);
					}
				}
			}

		};
		this.findNearbyLocationTask.execute(this.nearbyLocation);
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		this.userLocation = null;
		if (this.adapter != null) {
			this.adapter.onDestroy();
			this.adapter = null;
		}
		if (this.findNearbyLocationTask != null) {
			this.findNearbyLocationTask.cancel(true);
			this.findNearbyLocationTask = null;
		}
	}

	private static final int POIS_LOADER = 0;

	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POIS_LOADER:
			if (this.nearbyLocation == null) {
				return null;
			}
			final HomePOILoader homePoiLoader = new HomePOILoader(getActivity(), this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude());
			return homePoiLoader;
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
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		switchView(getView());
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			boolean locationChanged = false;
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				locationChanged = true;
				if (this.adapter != null) {
					this.adapter.setLocation(this.userLocation);
				}
			}
			if (this.nearbyLocation == null) {
				useNewNearbyLocation(newLocation);
				locationChanged = true;
			}
			if (locationChanged) {
				final boolean requireNotifyAB = setUserAwayFromLocation();
				if (requireNotifyAB) {
					getAbController().setABReady(this, isABReady(), true);
				}
			}
		}
	}

	private boolean setUserAwayFromLocation() {
		boolean requireNotifyAB = false;
		if (LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation)) {
			if (this.userAwayFromNearbyLocation) {
				requireNotifyAB = true;
				this.userAwayFromNearbyLocation = false;
			}
		} else {
			if (!this.userAwayFromNearbyLocation) {
				requireNotifyAB = true;
				this.userAwayFromNearbyLocation = true;
			}
		}
		return requireNotifyAB;
	}

	private void useNewNearbyLocation(Location newNearbyLocation) {
		if (newNearbyLocation == null || LocationUtils.areTheSame(newNearbyLocation, this.nearbyLocation)) {
			return;
		}
		this.nearbyLocation = newNearbyLocation;
		if (this.adapter != null) {
			this.adapter.clear();
		}
		final View view = getView();
		if (view != null) {
			switchView(view);
			if (view.findViewById(R.id.list) != null) {
				((AbsListView) view.findViewById(R.id.list)).scrollTo(0, 0);
			}
		}
		if (this.nearbyLocation != null) {
			getLoaderManager().restartLoader(POIS_LOADER, null, this);
		}
		setSwipeRefreshLayoutRefreshing(false);
		this.nearbyLocationAddress = null;
		getAbController().setABSubtitle(this, getABSubtitle(getActivity()), false);
		getAbController().setABReady(this, isABReady(), true);
		findNearbyLocation();
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
	public void onPause() {
		super.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
		}
	}

	@Override
	public void onFavoriteUpdated() {
		if (this.adapter != null) {
			this.adapter.onFavoriteUpdated();
		}
	}

	private void initAdapter() {
		if (this.adapter != null) {
			return;
		}
		this.adapter = new POIArrayAdapter(getActivity());
		this.adapter.setTag(getLogTag());
		this.adapter.setFavoriteUpdateListener(this);
		this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_ALL_NEARBY);
		final View view = getView();
		setupView(view);
		switchView(view);
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		this.swipeRefreshLayout = (ListViewSwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
		this.swipeRefreshLayout.setColorSchemeColors(ColorUtils.getThemeAttribute(getActivity(), R.attr.colorAccent));
		this.swipeRefreshLayout.setOnRefreshListener(this);
		inflateList(view);
		this.adapter.setListView((AbsListView) view.findViewById(R.id.list));
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
			if (this.swipeRefreshLayout != null) {
				this.swipeRefreshLayout.setListViewWR((AbsListView) view.findViewById(R.id.list));
			}
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
			if (this.swipeRefreshLayout != null) {
				this.swipeRefreshLayout.setLoadingViewWR(view.findViewById(R.id.loading));
			}
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
			if (this.swipeRefreshLayout != null) {
				this.swipeRefreshLayout.setEmptyViewWR(view.findViewById(R.id.empty));
			}
		}
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) view.findViewById(R.id.empty_text)).setText(this.emptyText);
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}


	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.app_name);
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
		return this.nearbyLocationAddress;
	}

}
