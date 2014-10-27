package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.SlidingTabLayout;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
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


	private static final String EXTRA_NEARBY_LOCATION = "extra_nearby_location";

	public static NearbyFragment newInstance(Location nearbyLocationOpt) {
		NearbyFragment f = new NearbyFragment();
		Bundle args = new Bundle();
		if (nearbyLocationOpt != null) {
			args.putParcelable(EXTRA_NEARBY_LOCATION, nearbyLocationOpt);
		}
		f.setArguments(args);
		return f;
	}

	private int lastPageSelected = -1;
	private AgencyTypePagerAdapter adapter;
	private Location nearbyLocation;
	protected String nearbyLocationAddress;
	private Location userLocation;
	private boolean userAwayFromNearbyLocation = true;
	private MTAsyncTask<Location, Void, String> findNearbyLocationTask;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_nearby, container, false);
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
		// broadcast reset nearby location to all fragments
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
		switchView(getView());
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		final Location nearbyLocation = BundleUtils.getParcelable(EXTRA_NEARBY_LOCATION, savedInstanceState, getArguments());
		setNewNearbyLocation(nearbyLocation);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.lastPageSelected >= 0) {
			onPageSelected(this.lastPageSelected); // tell current page it's selected
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
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
				boolean refreshRequired = result != null && !result.equals(NearbyFragment.this.nearbyLocationAddress);
				NearbyFragment.this.nearbyLocationAddress = result;
				if (refreshRequired) {
					final FragmentActivity activity = getActivity();
					if (activity != null) {
						((MainActivity) getActivity()).notifyABChange();
					}
				}
			}

		};
		this.findNearbyLocationTask.execute(this.nearbyLocation);
	}

	private void initTabsAndViewPager() {
		final List<DataSourceType> availableAgencyTypes = DataSourceProvider.get().getAvailableAgencyTypes(getActivity());
		this.adapter = new AgencyTypePagerAdapter(this, availableAgencyTypes);
		this.adapter.setNearbyLocation(this.nearbyLocation);
		final View view = getView();
		setupView(view);
		final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		if (this.lastPageSelected >= 0) {
			viewPager.setCurrentItem(NearbyFragment.this.lastPageSelected);
		} else {
			this.lastPageSelected = 0;
			new MTAsyncTask<Void, Void, Integer>() {

				private final String TAG = NearbyFragment.class.getSimpleName() + ">LoadLastPageSelectedFromUserPreferences";

				public String getLogTag() {
					return TAG;
				}

				@Override
				protected Integer doInBackgroundMT(Void... params) {
					try {
						final int typeId = PreferenceUtils.getPrefLcl(getActivity(), PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE,
								PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE_DEFAULT);
						if (typeId >= 0) {
							for (int i = 0; i < availableAgencyTypes.size(); i++) {
								if (availableAgencyTypes.get(i).getId() == typeId) {
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
					if (NearbyFragment.this.lastPageSelected != 0) {
						return; // user has manually move to another page before, too late
					}
					if (lastPageSelected != null) {
						NearbyFragment.this.lastPageSelected = lastPageSelected.intValue();
						viewPager.setCurrentItem(NearbyFragment.this.lastPageSelected);
					}
					switchView(view);
					onPageSelected(NearbyFragment.this.lastPageSelected); // tell current page it's selected
				}
			}.execute();
		}
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		viewPager.setOffscreenPageLimit(3);
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setViewPager(viewPager);
		tabs.setOnPageChangeListener(this);
		tabs.setSelectedIndicatorColors(0xff666666);
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			boolean locationChanged = false;
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				locationChanged = true;
				final List<Fragment> fragments = getChildFragmentManager().getFragments();
				if (fragments != null) {
					for (Fragment fragment : fragments) {
						if (fragment != null && fragment instanceof MTActivityWithLocation.UserLocationListener) {
							((MTActivityWithLocation.UserLocationListener) fragment).onUserLocationChanged(this.userLocation);
						}
					}
				}
				if (this.adapter != null) {
					this.adapter.setUserLocation(newLocation);
				}
			}
			if (this.nearbyLocation == null) {
				setNewNearbyLocation(newLocation);
				locationChanged = true;
			}
			if (locationChanged) {
				final boolean requireNotifyAB = setUserAwayFromLocation();
				if (requireNotifyAB) {
					((MainActivity) getActivity()).notifyABChange();
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

	private void setNewNearbyLocation(Location newNearbyLocation) {
		if (newNearbyLocation == null) {
			return;
		}
		this.nearbyLocation = newNearbyLocation;
		if (this.adapter == null) {
			initTabsAndViewPager();
		}
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof NearbyFragment.NearbyLocationListener) {
					((NearbyFragment.NearbyLocationListener) fragment).onNearbyLocationChanged(this.nearbyLocation);
				}
			}
		}
		setSwipeRefreshLayoutRefreshing(false);
		this.nearbyLocationAddress = null;
		((MainActivity) getActivity()).notifyABChange();
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
		PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE, this.adapter.getTypeId(position), false);
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					final NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					nearbyAgencyTypeFragment.setFragmentVisisbleAtPosition(position);
				}
			}
		}
		this.lastPageSelected = position;
		if (this.adapter != null) {
			this.adapter.setLastVisisbleFragmentPosition(this.lastPageSelected);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		switch (state) {
		case ViewPager.SCROLL_STATE_IDLE:
			List<Fragment> fragments = getChildFragmentManager().getFragments();
			if (fragments != null) {
				for (Fragment fragment : fragments) {
					if (fragment instanceof NearbyAgencyTypeFragment) {
						final NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
						nearbyAgencyTypeFragment.setNearbyFragment(this);
						nearbyAgencyTypeFragment.setFragmentVisisbleAtPosition(this.lastPageSelected); // resume
					}
				}
			}
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			List<Fragment> fragments2 = getChildFragmentManager().getFragments();
			if (fragments2 != null) {
				for (Fragment fragment : fragments2) {
					if (fragment instanceof NearbyAgencyTypeFragment) {
						final NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
						nearbyAgencyTypeFragment.setNearbyFragment(this);
						nearbyAgencyTypeFragment.setFragmentVisisbleAtPosition(-1); // pause
					}
				}
			}
			break;
		}
	}


	private void setSwipeRefreshLayoutRefreshing(boolean refreshing) {
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					final NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					nearbyAgencyTypeFragment.setSwipeRefreshLayoutRefreshing(refreshing);
				}
			}
		}
	}

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.nearby);
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
		return this.nearbyLocationAddress;
	}

	@Override
	public int getABIconDrawableResId() {
		if (!this.userAwayFromNearbyLocation) {
			return R.drawable.ic_menu_place_holo_light_active;
		} else {
			return R.drawable.ic_menu_place_holo_light;
		}
	}


	public static interface NearbyLocationListener extends MTActivityWithLocation.UserLocationListener {
		public void onNearbyLocationChanged(Location location);
	}

	private static class AgencyTypePagerAdapter extends FragmentStatePagerAdapter {

		private List<DataSourceType> availableAgencyTypes;
		private WeakReference<Context> contextWR;
		private Location nearbyLocation;
		private Location userLocation;
		private int lastVisisbleFragmentPosition = -1;
		private WeakReference<NearbyFragment> nearbyFragmentWR;

		public AgencyTypePagerAdapter(NearbyFragment nearbyFragment, List<DataSourceType> availableAgencyTypes) {
			super(nearbyFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(nearbyFragment.getActivity());
			this.availableAgencyTypes = availableAgencyTypes;
			this.nearbyFragmentWR = new WeakReference<NearbyFragment>(nearbyFragment);
		}

		public void onDestroy() {
			this.availableAgencyTypes = null;
			this.contextWR = null;
			this.nearbyFragmentWR = null;
		}

		public int getTypeId(int position) {
			DataSourceType type = getType(position);
			if (type == null) {
				return 0;
			}
			return type.getId();
		}

		public DataSourceType getType(int position) {
			return this.availableAgencyTypes.size() == 0 ? null : this.availableAgencyTypes.get(position);
		}

		public void setNearbyLocation(Location nearbyLocation) {
			this.nearbyLocation = nearbyLocation;
		}

		public void setUserLocation(Location userLocation) {
			this.userLocation = userLocation;
		}

		public void setLastVisisbleFragmentPosition(int lastVisisbleFragmentPosition) {
			this.lastVisisbleFragmentPosition = lastVisisbleFragmentPosition;
		}

		@Override
		public int getCount() {
			return this.availableAgencyTypes == null ? 0 : this.availableAgencyTypes.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			final Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return StringUtils.EMPTY;
			}
			if (this.availableAgencyTypes == null || position >= this.availableAgencyTypes.size()) {
				return StringUtils.EMPTY;
			}
			return context.getString(this.availableAgencyTypes.get(position).getShortNameResId());
		}

		@Override
		public Fragment getItem(int position) {
			final NearbyAgencyTypeFragment f = NearbyAgencyTypeFragment.newInstance(position, this.lastVisisbleFragmentPosition,
					this.availableAgencyTypes.get(position), this.nearbyLocation, this.userLocation);
			f.setNearbyFragment(this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get());
			return f;
		}

	}

}
