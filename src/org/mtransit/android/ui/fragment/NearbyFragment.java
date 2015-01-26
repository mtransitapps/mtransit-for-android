package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.NavigationDrawerController;
import org.mtransit.android.ui.view.SlidingTabLayout;
import org.mtransit.android.util.MapUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

public class NearbyFragment extends ABFragment implements ViewPager.OnPageChangeListener, MTActivityWithLocation.UserLocationListener,
		SwipeRefreshLayout.OnRefreshListener {

	private static final String TAG = NearbyFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Nearby";

	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}


	private static final String EXTRA_SELECTED_TYPE = "extra_selected_type";

	private static final String EXTRA_FIXED_ON_POI_UUID = "extra_fixed_on_poi_uuid";

	private static final String EXTRA_FIXED_ON_POI_AUTHORITY = "extra_fixed_on_poi_authority";

	public static NearbyFragment newNearbyInstance(Location optNearbyLocation, Integer optTypeId) {
		return newInstance(optNearbyLocation, optTypeId, null, null, null, null);
	}

	public static NearbyFragment newPoiInstance(Integer optTypeId, String fixedOnPoiUuid, String fixedOnPoiAuthority, POIManager optFixedOnPoi,
			AgencyProperties optFixedOnAgency) {
		return newInstance(null, optTypeId, fixedOnPoiUuid, fixedOnPoiAuthority, optFixedOnPoi, optFixedOnAgency);
	}

	private static NearbyFragment newInstance(Location optNearbyLocation, Integer optTypeId, String optFixedOnPoiUuid, String optFixedOnPoiAuthority,
			POIManager optFixedOnPoi, AgencyProperties optFixedOnAgency) {
		NearbyFragment f = new NearbyFragment();
		Bundle args = new Bundle();
		if (optTypeId != null) {
			args.putInt(EXTRA_SELECTED_TYPE, optTypeId);
			f.selectedTypeId = optTypeId;
		}
		if (!TextUtils.isEmpty(optFixedOnPoiUuid) && !TextUtils.isEmpty(optFixedOnPoiAuthority)) {
			args.putString(EXTRA_FIXED_ON_POI_UUID, optFixedOnPoiUuid);
			f.fixedOnPoiUUID = optFixedOnPoiUuid;
			f.fixedOnPoi = optFixedOnPoi;
			args.putString(EXTRA_FIXED_ON_POI_AUTHORITY, optFixedOnPoiAuthority);
			f.fixedOnPoiUAuthority = optFixedOnPoiAuthority;
			f.fixedOnPoiAgency = optFixedOnAgency;
		}
		f.setArguments(args);
		return f;
	}

	private int lastPageSelected = -1;
	private AgencyTypePagerAdapter adapter;
	private Location nearbyLocation;
	protected String nearbyLocationAddress;
	private Location userLocation;
	private MTAsyncTask<Location, Void, String> findNearbyLocationTask;
	private Integer selectedTypeId = null;

	private String fixedOnPoiUAuthority;

	private AgencyProperties fixedOnPoiAgency;

	private boolean hasFixedOnPoiAgency() {
		if (this.fixedOnPoiAgency == null) {
			initFixedOnPoiAgencyAsync();
			return false;
		}
		return true;
	}

	private void resetFixedOnPoiAgency() {
		this.fixedOnPoiAgency = null;
	}

	private AgencyProperties getFixedOnPoiAgencyOrNull() {
		if (!hasFixedOnPoiAgency()) {
			return null;
		}
		return this.fixedOnPoiAgency;
	}

	private void initFixedOnPoiAgencyAsync() {
		if (this.loadFixedOnPoiAgencyTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.fixedOnPoiUAuthority)) {
			return;
		}
		this.loadFixedOnPoiAgencyTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadFixedOnPoiAgencyTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">loadFixedOnPoiAgencyTask";
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initFixedOnPoiAgencySync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewFixedOnPoiAgency();
			}
		}
	};

	private boolean initFixedOnPoiAgencySync() {
		if (this.fixedOnPoiAgency != null) {
			return false;
		}
		if (!TextUtils.isEmpty(this.fixedOnPoiUAuthority)) {
			this.fixedOnPoiAgency = DataSourceProvider.get(getActivity()).getAgency(getActivity(), this.fixedOnPoiUAuthority);
		}
		return this.fixedOnPoiAgency != null;
	}

	private void applyNewFixedOnPoiAgency() {
		if (this.fixedOnPoiAgency == null) {
			return;
		}
		getAbController().setABBgColor(this, getABBgColor(getActivity()), true);
		setupTabTheme(getView());
	}

	private String fixedOnPoiUUID;

	private POIManager fixedOnPoi;

	private void resetFixedOnPoi() {
		this.fixedOnPoi = null;
	}

	private boolean hasFixedOnPoi() {
		if (this.fixedOnPoi == null) {
			initFixedOnPoiAsync();
			return false;
		}
		return true;
	}

	private void initFixedOnPoiAsync() {
		if (this.loadFixedOnPoiTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.fixedOnPoiUUID) || TextUtils.isEmpty(this.fixedOnPoiUAuthority)) {
			return;
		}
		this.loadFixedOnPoiTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadFixedOnPoiTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">loadFixedOnPoiTask";
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initFixedOnPoiSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewFixedOnPoi();
			}
		}
	};

	private POIManager getFixedOnPoiOrNull() {
		if (!hasFixedOnPoi()) {
			return null;
		}
		return fixedOnPoi;
	}

	private boolean initFixedOnPoiSync() {
		if (this.fixedOnPoi != null) {
			return false;
		}
		if (!TextUtils.isEmpty(this.fixedOnPoiUUID) && !TextUtils.isEmpty(this.fixedOnPoiUAuthority)) {
			this.fixedOnPoi = DataSourceManager.findPOI(getActivity(), this.fixedOnPoiUAuthority,
					new POIFilter(Arrays.asList(new String[] { this.fixedOnPoiUUID })));
		}
		return this.fixedOnPoi != null;
	}

	private void applyNewFixedOnPoi() {
		if (this.fixedOnPoi == null) {
			return;
		}
		getAbController().setABTitle(this, getABTitle(getActivity()), false);
		getAbController().setABBgColor(this, getABBgColor(getActivity()), true);
		if (isFixedOnPoi()) {
			setNewNearbyLocation(LocationUtils.getNewLocation(this.fixedOnPoi.poi.getLat(), this.fixedOnPoi.poi.getLng()));
		}
	}

	private Boolean isFixedOnPoi = null;

	private void resetIsFixedOnPoi() {
		this.isFixedOnPoi = null;
	}

	private boolean isFixedOnPoi() {
		if (this.isFixedOnPoi == null) {
			initIsFixedOnPoi();
		}
		return this.isFixedOnPoi;
	}

	private void initIsFixedOnPoi() {
		this.isFixedOnPoi = !TextUtils.isEmpty(this.fixedOnPoiUUID) && !TextUtils.isEmpty(this.fixedOnPoiUAuthority);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		restoreInstanceState(savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_nearby, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onRefresh() {
		initiateRefresh();
	}

	private void initiateRefresh() {
		if (isFixedOnPoi() || LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation, 2)) {
			setSwipeRefreshLayoutRefreshing(false);
			return;
		}
		// broadcast reset nearby location to all fragments
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof NearbyFragment.NearbyLocationListener) {
					((NearbyFragment.NearbyLocationListener) fragment).onNearbyLocationChanged(null);
				}
			}
		}
		// set new nearby location is current user location interesting
		setNewNearbyLocation(this.userLocation);
	}

	@Override
	public void onModulesUpdated() {
		if (this.adapter != null) {
			ArrayList<DataSourceType> newAvailableAgencyTypes = filterAgencyTypes(DataSourceProvider.get(getActivity()).getAvailableAgencyTypes());
			if (CollectionUtils.getSize(newAvailableAgencyTypes) == CollectionUtils.getSize(this.adapter.getAvailableAgencyTypes())) {
				return;
			}
			MainActivity mainActivity = (MainActivity) getActivity();
			if (mainActivity != null) {
				NavigationDrawerController navigationController = mainActivity.getNavigationDrawerController();
				if (navigationController != null) {
					navigationController.forceReset();
				}
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		MTLog.d(this, "onSaveInstanceState() > this.selectedTypeId: %s", this.selectedTypeId);
		if (this.selectedTypeId != null) {
			outState.putInt(EXTRA_SELECTED_TYPE, this.selectedTypeId);
		}
		if (!TextUtils.isEmpty(this.fixedOnPoiUAuthority)) {
			outState.putString(EXTRA_FIXED_ON_POI_AUTHORITY, this.fixedOnPoiUAuthority);
		}
		if (!TextUtils.isEmpty(this.fixedOnPoiUUID)) {
			outState.putString(EXTRA_FIXED_ON_POI_UUID, this.fixedOnPoiUUID);
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		Integer newSelectedId = BundleUtils.getInt(EXTRA_SELECTED_TYPE, bundles);
		if (newSelectedId != null && !newSelectedId.equals(this.selectedTypeId)) {
			this.selectedTypeId = newSelectedId;
		}
		String fixedOnPoiAuthority = BundleUtils.getString(EXTRA_FIXED_ON_POI_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(fixedOnPoiAuthority) && !fixedOnPoiAuthority.equals(this.fixedOnPoiUAuthority)) {
			this.fixedOnPoiUAuthority = fixedOnPoiAuthority;
			resetFixedOnPoiAgency();
			resetIsFixedOnPoi();
		}
		String newFixedOnPoiUUID = BundleUtils.getString(EXTRA_FIXED_ON_POI_UUID, bundles);
		if (!TextUtils.isEmpty(newFixedOnPoiUUID) && !newFixedOnPoiUUID.equals(this.fixedOnPoiUUID)) {
			this.fixedOnPoiUUID = newFixedOnPoiUUID;
			resetFixedOnPoi();
			resetIsFixedOnPoi();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.lastPageSelected >= 0) {
			onPageSelected(this.lastPageSelected); // tell current page it's selected
		}
		switchView(getView());
		if (isFixedOnPoi()) {
			if (hasFixedOnPoi()) {
				setNewNearbyLocation(LocationUtils.getNewLocation(getFixedOnPoiOrNull().poi.getLat(), getFixedOnPoiOrNull().poi.getLng()));
			}
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		this.userLocation = null;
		this.nearbyLocation = null;
		if (this.adapter != null) {
			this.adapter = null;
		}
		if (this.findNearbyLocationTask != null) {
			this.findNearbyLocationTask.cancel(true);
			this.findNearbyLocationTask = null;
		}
	}

	private void findNearbyLocation() {
		if (this.findNearbyLocationTask != null) {
			this.findNearbyLocationTask.cancel(true);
		}
		this.findNearbyLocationTask = new MTAsyncTask<Location, Void, String>() {

			@Override
			public String getLogTag() {
				return TAG + ">findNearbyLocationTask";
			}

			@Override
			protected String doInBackgroundMT(Location... locations) {
				Activity activity = getActivity();
				Location nearbyLocation = locations[0];
				if (activity == null || nearbyLocation == null) {
					return null;
				}
				Address address = LocationUtils.getLocationAddress(activity, nearbyLocation);
				String locationString = LocationUtils.getLocationString(activity, null, address, nearbyLocation.getAccuracy());
				return locationString;
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				boolean refreshRequired = result != null && !result.equals(NearbyFragment.this.nearbyLocationAddress);
				NearbyFragment.this.nearbyLocationAddress = result;
				if (refreshRequired) {
					FragmentActivity activity = NearbyFragment.this.getActivity();
					if (activity != null) {
						getAbController().setABSubtitle(NearbyFragment.this, getABSubtitle(activity), false);
						getAbController().setABReady(NearbyFragment.this, isABReady(), true);
					}
				}
			}

		};
		this.findNearbyLocationTask.execute(this.nearbyLocation);
	}

	private ArrayList<DataSourceType> filterAgencyTypes(ArrayList<DataSourceType> availableAgencyTypes) {
		if (availableAgencyTypes != null) {
			Iterator<DataSourceType> it = availableAgencyTypes.iterator();
			while (it.hasNext()) {
				if (!it.next().isNearbyScreen()) {
					it.remove();
				}
			}
		}
		return availableAgencyTypes;
	}

	private void initTabsAndViewPager(final View view) {
		if (view == null) {
			return;
		}
		final ArrayList<DataSourceType> newAgencyTypes = filterAgencyTypes(DataSourceProvider.get(getActivity()).getAvailableAgencyTypes());
		if (CollectionUtils.getSize(newAgencyTypes) == 0) {
			return;
		}
		if (this.adapter == null) {
			this.adapter = new AgencyTypePagerAdapter(this, newAgencyTypes, !isFixedOnPoi());
		} else if (CollectionUtils.getSize(newAgencyTypes) != CollectionUtils.getSize(this.adapter.getAvailableAgencyTypes())) {
			this.adapter.setAvailableAgencyTypes(newAgencyTypes);
			this.adapter.notifyDataSetChanged();
			this.adapter.setSwipeRefreshLayoutEnabled(!isFixedOnPoi());
		}
		this.adapter.setNearbyLocation(this.nearbyLocation);
		setupAdapter(view);
		if (this.lastPageSelected >= 0) {
			ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
			viewPager.setCurrentItem(NearbyFragment.this.lastPageSelected);
		} else {
			new MTAsyncTask<Void, Void, Integer>() {

				private final String TAG = NearbyFragment.class.getSimpleName() + ">LoadLastPageSelectedFromUserPreferences";

				public String getLogTag() {
					return TAG;
				}

				@Override
				protected Integer doInBackgroundMT(Void... params) {
					try {
						if (NearbyFragment.this.selectedTypeId == null) {
							NearbyFragment.this.selectedTypeId = PreferenceUtils.getPrefLcl(getActivity(), PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE,
									PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE_DEFAULT);
						}
						if (NearbyFragment.this.selectedTypeId >= 0) {
							for (int i = 0; i < newAgencyTypes.size(); i++) {
								if (newAgencyTypes.get(i).getId() == NearbyFragment.this.selectedTypeId) {
									return i;
								}
							}
						}
					} catch (Exception e) {
						MTLog.w(TAG, e, "Error while determining the select nearby tab!");
					}
					return null;
				}

				@Override
				protected void onPostExecute(Integer lastPageSelected) {
					if (NearbyFragment.this.lastPageSelected >= 0) {
						return; // user has manually move to another page before, too late
					}
					if (lastPageSelected == null) {
						NearbyFragment.this.lastPageSelected = 0;
					} else {
						NearbyFragment.this.lastPageSelected = lastPageSelected;
						ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
						viewPager.setCurrentItem(NearbyFragment.this.lastPageSelected);
					}
					switchView(view);
					onPageSelected(NearbyFragment.this.lastPageSelected); // tell current page it's selected
				}
			}.execute();
		}
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setOffscreenPageLimit(3);
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setCustomTabView(R.layout.layout_tab_indicator, R.id.tab_title);
		tabs.setOnPageChangeListener(this);
		tabs.setSelectedIndicatorColors(Color.WHITE);
		setupTabTheme(view);
		setupAdapter(view);
		setSwipeRefreshLayoutEnabled(!isFixedOnPoi());
	}

	private void setupTabTheme(View view) {
		if (view == null) {
			return;
		}
		view.findViewById(R.id.tabs).setBackgroundColor(getABBgColor(getActivity()));
	}

	private void setupAdapter(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setViewPager(viewPager);
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				MTActivityWithLocation.broadcastUserLocationChanged(this, getChildFragmentManager(), newLocation);
			}
			if (!isFixedOnPoi() && this.nearbyLocation == null) {
				setNewNearbyLocation(newLocation);
			}
		}
	}


	private void setNewNearbyLocation(Location newNearbyLocation) {
		if (newNearbyLocation == null) {
			return;
		}
		this.nearbyLocation = newNearbyLocation;
		if (this.adapter == null) {
			initTabsAndViewPager(getView());
		} else {
			this.adapter.setNearbyLocation(this.nearbyLocation);
		}
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof NearbyFragment.NearbyLocationListener) {
					((NearbyFragment.NearbyLocationListener) fragment).onNearbyLocationChanged(this.nearbyLocation);
				}
			}
		}
		setSwipeRefreshLayoutRefreshing(false);
		this.nearbyLocationAddress = null;
		getAbController().setABReady(this, isABReady(), true);
		findNearbyLocation();
	}

	public Location getNearbyLocation() {
		return nearbyLocation;
	}

	public Location getUserLocation() {
		return userLocation;
	}

	private void switchView(View view) {
		if (this.adapter == null) {
			showLoading(view);
		} else if (this.adapter.getCount() > 0) {
			showTabsAndViewPager(view);
		} else {
			showEmpty(view);
		}
	}

	private void showTabsAndViewPager(View view) {
		// loading
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		// empty
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		// tabs
		view.findViewById(R.id.tabs).setVisibility(View.VISIBLE); // show
		// view pager
		view.findViewById(R.id.viewpager).setVisibility(View.VISIBLE); // show
	}

	private void showLoading(View view) {
		// tabs
		if (view.findViewById(R.id.tabs) != null) { // IF inflated/present DO
			view.findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		// view pager
		if (view.findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			view.findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
		}
		// empty
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		// loading
		if (view.findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.loading_stub)).inflate(); // inflate
		}
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty(View view) {
		// tabs
		if (view.findViewById(R.id.tabs) != null) { // IF inflated/present DO
			view.findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		// view pager
		if (view.findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			view.findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
		}
		// loading
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		// empty
		if (view.findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.empty_stub)).inflate(); // inflate
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public void onPageSelected(int position) {
		StatusLoader.get().clearAllTasks();
		ServiceUpdateLoader.get().clearAllTasks();
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					nearbyAgencyTypeFragment.setFragmentVisibleAtPosition(position);
				}
			}
		}
		this.lastPageSelected = position;
		if (this.adapter != null) {
			this.selectedTypeId = this.adapter.getTypeId(position);
			this.adapter.setLastVisibleFragmentPosition(this.lastPageSelected);
		}
		PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE, this.selectedTypeId, false);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		switch (state) {
		case ViewPager.SCROLL_STATE_IDLE:
			resumeAllVisibleAwareChildFragment();
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			pauseAllVisibleAwareChildFragments();
			break;
		}
	}

	private void pauseAllVisibleAwareChildFragments() {
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					nearbyAgencyTypeFragment.setFragmentVisibleAtPosition(-1); // pause
				}
			}
		}
	}

	private void resumeAllVisibleAwareChildFragment() {
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					nearbyAgencyTypeFragment.setFragmentVisibleAtPosition(this.lastPageSelected); // resume
				}
			}
		}
	}


	private void setSwipeRefreshLayoutRefreshing(boolean refreshing) {
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					nearbyAgencyTypeFragment.setSwipeRefreshLayoutRefreshing(refreshing);
				}
			}
		}
	}

	private void setSwipeRefreshLayoutEnabled(boolean enabled) {
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					nearbyAgencyTypeFragment.setSwipeRefreshLayoutEnabled(enabled);
				}
			}
		}
	}

	private MenuItem showDirectionsMenuItem;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_nearby, menu);
		this.showDirectionsMenuItem = menu.findItem(R.id.menu_show_directions);
		updateDirectionsMenuItem();
	}

	private void updateDirectionsMenuItem() {
		if (this.showDirectionsMenuItem == null) {
			return;
		}
		if (isFixedOnPoi()) {
			this.showDirectionsMenuItem.setVisible(true);
		} else {
			this.showDirectionsMenuItem.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_show_directions:
			POIManager fixedOnPoim = getFixedOnPoiOrNull();
			if (fixedOnPoim != null) {
				MapUtils.showDirection(getActivity(), fixedOnPoim.poi.getLat(), fixedOnPoim.poi.getLng(), null, null, fixedOnPoim.poi.getName());
				return true; // handled
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public CharSequence getABTitle(Context context) {
		if (isFixedOnPoi()) {
			POIManager poim = getFixedOnPoiOrNull();
			if (poim != null) {
				return poim.poi.getName();
			}
		}
		return context.getString(R.string.nearby);
	}

	@Override
	public Integer getABBgColor(Context context) {
		if (isFixedOnPoi()) {
			POIManager poim = getFixedOnPoiOrNull();
			if (poim != null && poim.poi instanceof RouteTripStop) {
				return ((RouteTripStop) poim.poi).route.getColorInt();
			} else {
				AgencyProperties agency = getFixedOnPoiAgencyOrNull();
				if (agency != null && agency.hasColor()) {
					return agency.getColorInt();
				}
			}
		}
		return super.getABBgColor(context);
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
		return this.nearbyLocationAddress;
	}


	public static interface NearbyLocationListener extends MTActivityWithLocation.UserLocationListener {
		public void onNearbyLocationChanged(Location location);
	}

	private static class AgencyTypePagerAdapter extends FragmentStatePagerAdapter implements SlidingTabLayout.TabColorizer, MTLog.Loggable {

		private static final String TAG = NearbyFragment.class.getSimpleName() + ">" + AgencyTypePagerAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private ArrayList<DataSourceType> availableAgencyTypes;
		private WeakReference<Context> contextWR;
		private Location nearbyLocation;
		private int lastVisibleFragmentPosition = -1;
		private WeakReference<NearbyFragment> nearbyFragmentWR;
		private int saveStateCount = -1;
		private boolean swipeRefreshLayoutEnabled = true;

		public AgencyTypePagerAdapter(NearbyFragment nearbyFragment, ArrayList<DataSourceType> availableAgencyTypes, boolean swipeRefreshLayoutEnabled) {
			super(nearbyFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(nearbyFragment.getActivity());
			setAvailableAgencyTypes(availableAgencyTypes);
			this.nearbyFragmentWR = new WeakReference<NearbyFragment>(nearbyFragment);
			this.swipeRefreshLayoutEnabled = swipeRefreshLayoutEnabled;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
			if (this.saveStateCount >= 0 && this.saveStateCount != getCount()) {
				return;
			}
			try {
				super.restoreState(state, loader);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while restoring state!");
			}
		}

		@Override
		public Parcelable saveState() {
			try {
				this.saveStateCount = getCount();
				return super.saveState();
			} catch (Exception e) {
				MTLog.w(this, e, "Error while saving fragment state!");
				return null;
			}
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			try {
				super.destroyItem(container, position, object);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while destroying item at position '%s'!", position);
			}
		}

		public ArrayList<DataSourceType> getAvailableAgencyTypes() {
			return availableAgencyTypes;
		}

		private ArrayList<Integer> typeIds = new ArrayList<Integer>();

		public void setAvailableAgencyTypes(ArrayList<DataSourceType> availableAgencyTypes) {
			this.availableAgencyTypes = availableAgencyTypes;
			this.typeIds.clear();
			if (this.availableAgencyTypes != null) {
				for (DataSourceType type : this.availableAgencyTypes) {
					this.typeIds.add(type.getId());
				}
			}
		}

		public void setSwipeRefreshLayoutEnabled(boolean swipeRefreshLayoutEnabled) {
			this.swipeRefreshLayoutEnabled = swipeRefreshLayoutEnabled;
		}

		public Integer getTypeId(int position) {
			return this.typeIds == null ? null : this.typeIds.get(position);
		}


		public void setNearbyLocation(Location nearbyLocation) {
			this.nearbyLocation = nearbyLocation;
		}


		public void setLastVisibleFragmentPosition(int lastVisibleFragmentPosition) {
			this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}

		@Override
		public int getCount() {
			return this.typeIds == null ? 0 : this.typeIds.size();
		}

		@Override
		public int getItemPosition(Object object) {
			if (object != null && object instanceof NearbyAgencyTypeFragment) {
				NearbyAgencyTypeFragment f = (NearbyAgencyTypeFragment) object;
				return getTypePosition(f.getTypeId());
			} else {
				return POSITION_NONE;
			}
		}

		private int getTypePosition(Integer typeId) {
			if (typeId == null) {
				return POSITION_NONE;
			}
			int indexOf = this.typeIds == null ? -1 : this.typeIds.indexOf(typeId);
			if (indexOf < 0) {
				return POSITION_NONE;
			}
			return indexOf;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return StringUtils.EMPTY;
			}
			if (this.availableAgencyTypes == null || position >= this.availableAgencyTypes.size()) {
				return StringUtils.EMPTY;
			}
			return context.getString(this.availableAgencyTypes.get(position).getShortNameResId());
		}

		@Override
		public int getIndicatorColor(int position) {
			return Color.WHITE;
		}

		@Override
		public Fragment getItem(int position) {
			Integer typeId = this.typeIds.get(position);
			NearbyAgencyTypeFragment f = NearbyAgencyTypeFragment.newInstance(position, this.lastVisibleFragmentPosition, typeId, this.nearbyLocation);
			f.setNearbyFragment(this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get());
			f.setSwipeRefreshLayoutEnabled(this.swipeRefreshLayoutEnabled);
			return f;
		}

	}

}
