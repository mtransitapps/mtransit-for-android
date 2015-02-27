package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.NavigationDrawerController;
import org.mtransit.android.ui.view.SlidingTabLayout;
import org.mtransit.android.util.MapUtils;

import android.app.Activity;
import android.content.Context;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.PopupWindow;

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

	private static final String EXTRA_FIXED_ON_LAT = "extra_fixed_on_lat";
	private static final String EXTRA_FIXED_ON_LNG = "extra_fixed_on_lng";
	private static final String EXTRA_FIXED_ON_NAME = "extra_fixed_on_name";
	private static final String EXTRA_FIXED_ON_COLOR = "extra_fixed_on_color";

	public static NearbyFragment newNearbyInstance(Location optNearbyLocation, Integer optTypeId) {
		return newInstance(optNearbyLocation, optTypeId, null, null, null, null);
	}

	public static NearbyFragment newFixedOnInstance(Integer optTypeId, double fixedOnLat, double fixedOnLng, String fixedOnName, Integer optFixedOnColor) {
		return newInstance(null, optTypeId, fixedOnLat, fixedOnLng, fixedOnName, optFixedOnColor);
	}

	private static NearbyFragment newInstance(Location optNearbyLocation, Integer optTypeId, Double optFixedOnLat, Double optFixedOnLng, String optFixedOnName,
			Integer optFixedOnColor) {
		NearbyFragment f = new NearbyFragment();
		Bundle args = new Bundle();
		if (optTypeId != null) {
			args.putInt(EXTRA_SELECTED_TYPE, optTypeId);
			f.selectedTypeId = optTypeId;
		}
		if (optFixedOnLat != null && optFixedOnLng != null && !TextUtils.isEmpty(optFixedOnName)) {
			args.putDouble(EXTRA_FIXED_ON_LAT, optFixedOnLat);
			f.fixedOnLat = optFixedOnLat;
			args.putDouble(EXTRA_FIXED_ON_LNG, optFixedOnLng);
			f.fixedOnLng = optFixedOnLng;
			args.putString(EXTRA_FIXED_ON_NAME, optFixedOnName);
			f.fixedOnName = optFixedOnName;
			if (optFixedOnColor != null) {
				args.putInt(EXTRA_FIXED_ON_COLOR, optFixedOnColor);
				f.fixedOnColor = optFixedOnColor;
			}
		}
		f.setArguments(args);
		return f;
	}

	private int lastPageSelected = -1;
	private AgencyTypePagerAdapter adapter;
	private Location nearbyLocation;
	private String nearbyLocationAddress;
	private Location userLocation;
	private MTAsyncTask<Location, Void, String> findNearbyLocationTask;
	private Integer selectedTypeId = null;
	private Double fixedOnLat = null;
	private Double fixedOnLng = null;
	private String fixedOnName = null;
	private Integer fixedOnColor = null;
	private Boolean isFixedOn = null;

	private void resetIsFixedOn() {
		this.isFixedOn = null;
	}

	private boolean isFixedOn() {
		if (this.isFixedOn == null) {
			initIsFixedOnSync();
		}
		return this.isFixedOn;
	}

	private void initIsFixedOnSync() {
		this.isFixedOn = this.fixedOnLat != null && this.fixedOnLng != null && !TextUtils.isEmpty(this.fixedOnName);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	private void initAdapters(Activity activity) {
		this.adapter = new AgencyTypePagerAdapter(activity, this, null, false);
		this.adapter.setSwipeRefreshLayoutEnabled(!isFixedOn());
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
		View view = inflater.inflate(R.layout.fragment_nearby, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onRefresh() {
		initiateRefresh();
	}

	private boolean initiateRefresh() {
		if (isFixedOn()) {
			setSwipeRefreshLayoutRefreshing(false);
			return false;
		}
		if (LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation, LocationUtils.LOCATION_CHANGED_ALLOW_REFRESH_IN_METERS)) {
			setSwipeRefreshLayoutRefreshing(false);
			return false;
		}
		broadcastNearbyLocationChanged(null); // force reset
		setNewNearbyLocation(this.userLocation);
		return true;
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		if (this.adapter != null) {
			FragmentActivity activity = getActivity();
			if (activity == null) {
				return;
			}
			ArrayList<DataSourceType> newAvailableAgencyTypes = filterAgencyTypes(DataSourceProvider.get(activity).getAvailableAgencyTypes());
			if (CollectionUtils.getSize(newAvailableAgencyTypes) == CollectionUtils.getSize(this.adapter.getAvailableAgencyTypes())) {
				this.modulesUpdated = false; // nothing to do
				return;
			}
			resetAvailableTypes();
			MainActivity mainActivity = (MainActivity) activity;
			if (mainActivity.isMTResumed()) {
				NavigationDrawerController navigationController = mainActivity.getNavigationDrawerController();
				if (navigationController != null) {
					navigationController.forceReset();
					this.modulesUpdated = false; // processed
				}
			}
		} else {
			this.modulesUpdated = false; // nothing to do
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		MTLog.d(this, "onSaveInstanceState() > this.selectedTypeId: %s", this.selectedTypeId);
		if (this.selectedTypeId != null) {
			outState.putInt(EXTRA_SELECTED_TYPE, this.selectedTypeId);
		}
		if (this.fixedOnLat != null) {
			outState.putDouble(EXTRA_FIXED_ON_LAT, this.fixedOnLat);
		}
		if (this.fixedOnLng != null) {
			outState.putDouble(EXTRA_FIXED_ON_LNG, this.fixedOnLng);
		}
		if (!TextUtils.isEmpty(this.fixedOnName)) {
			outState.putString(EXTRA_FIXED_ON_NAME, this.fixedOnName);
		}
		if (this.fixedOnColor != null) {
			outState.putInt(EXTRA_FIXED_ON_COLOR, this.fixedOnColor);
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		Integer newSelectedId = BundleUtils.getInt(EXTRA_SELECTED_TYPE, bundles);
		if (newSelectedId != null && !newSelectedId.equals(this.selectedTypeId)) {
			this.selectedTypeId = newSelectedId;
		}
		Double newFixedOnLat = BundleUtils.getDouble(EXTRA_FIXED_ON_LAT, bundles);
		if (newFixedOnLat != null) {
			this.fixedOnLat = newFixedOnLat;
			resetIsFixedOn();
		}
		Double newFixedOnLng = BundleUtils.getDouble(EXTRA_FIXED_ON_LNG, bundles);
		if (newFixedOnLng != null) {
			this.fixedOnLng = newFixedOnLng;
			resetIsFixedOn();
		}
		String newFixedOnName = BundleUtils.getString(EXTRA_FIXED_ON_NAME, bundles);
		if (!TextUtils.isEmpty(newFixedOnName)) {
			this.fixedOnName = newFixedOnName;
		}
		Integer newFixedOnColor = BundleUtils.getInt(EXTRA_FIXED_ON_COLOR, bundles);
		if (newFixedOnColor != null) {
			this.fixedOnColor = newFixedOnColor;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		hideLocationToast();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.modulesUpdated) {
			getView().post(new Runnable() {
				@Override
				public void run() {
					if (NearbyFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
		if (this.lastPageSelected >= 0) {
			onPageSelected(this.lastPageSelected); // tell current page it's selected
		}
		switchView(getView());
		if (isFixedOn()) {
			setNewNearbyLocation(LocationUtils.getNewLocation(this.fixedOnLat, this.fixedOnLng));
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.userLocation = null;
		this.nearbyLocation = null;
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
				return LocationUtils.getLocationString(activity, null, address, nearbyLocation.getAccuracy());
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

	private ArrayList<DataSourceType> availableTypes = null;

	private ArrayList<DataSourceType> getAvailableTypesOrNull() {
		if (!hasAvailableTypes()) {
			return null;
		}
		return this.availableTypes;
	}

	private boolean hasAvailableTypes() {
		if (this.availableTypes == null) {
			initAvailableTypesAsync();
			return false;
		}
		return true;
	}

	private void initAvailableTypesAsync() {
		if (this.loadAvailableTypesTask != null && this.loadAvailableTypesTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		this.loadAvailableTypesTask = new LoadAvailableTypesTask();
		this.loadAvailableTypesTask.execute();
	}

	private LoadAvailableTypesTask loadAvailableTypesTask = null;

	private class LoadAvailableTypesTask extends MTAsyncTask<Object, Void, Boolean> {

		@Override
		public String getLogTag() {
			return NearbyFragment.this.getLogTag() + ">" + LoadAvailableTypesTask.class.getSimpleName();
		}

		@Override
		protected Boolean doInBackgroundMT(Object... params) {
			return initAvailableTypesSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewAvailableTypes();
			}
		}
	}

	private boolean initAvailableTypesSync() {
		if (this.availableTypes != null) {
			return false;
		}
		this.availableTypes = filterAgencyTypes(DataSourceProvider.get(getActivity()).getAvailableAgencyTypes());
		return this.availableTypes != null;
	}

	private void applyNewAvailableTypes() {
		if (this.availableTypes == null) {
			return;
		}
		if (this.adapter != null) {
			this.adapter.setAvailableAgencyTypes(this.availableTypes);
			View view = getView();
			notifyTabDataChanged(view);
			showSelectedTab(view);
		}
	}

	private void resetAvailableTypes() {
		this.availableTypes = null; // reset
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

	private static class LoadLastPageSelectedFromUserPreference extends MTAsyncTask<Void, Void, Integer> {

		private final String TAG = NearbyFragment.class.getSimpleName() + ">" + LoadLastPageSelectedFromUserPreference.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<Context> contextWR;
		private WeakReference<NearbyFragment> nearbyFragmentWR;
		private Integer selectedTypeId;
		private ArrayList<DataSourceType> newAgencyTypes;

		public LoadLastPageSelectedFromUserPreference(Context context, NearbyFragment nearbyFragment, Integer selectedTypeId,
				ArrayList<DataSourceType> newAgencyTypes) {
			this.contextWR = new WeakReference<Context>(context);
			this.nearbyFragmentWR = new WeakReference<NearbyFragment>(nearbyFragment);
			this.selectedTypeId = selectedTypeId;
			this.newAgencyTypes = newAgencyTypes;
		}

		@Override
		protected Integer doInBackgroundMT(Void... params) {
			try {
				if (this.selectedTypeId == null) {
					Context context = this.contextWR == null ? null : this.contextWR.get();
					if (context != null) {
						this.selectedTypeId = PreferenceUtils.getPrefLcl(context, PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE,
								PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE_DEFAULT);
					} else {
						this.selectedTypeId = PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE_DEFAULT;
					}
				}
				if (this.selectedTypeId >= 0) {
					if (this.newAgencyTypes != null) {
						for (int i = 0; i < this.newAgencyTypes.size(); i++) {
							if (this.newAgencyTypes.get(i).getId() == this.selectedTypeId) {
								return i;
							}
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
			NearbyFragment nearbyFragment = this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get();
			if (nearbyFragment == null) {
				return; // too late
			}
			if (nearbyFragment.lastPageSelected >= 0) {
				return; // user has manually move to another page before, too late
			}
			if (lastPageSelected == null) {
				nearbyFragment.lastPageSelected = 0;
			} else {
				nearbyFragment.lastPageSelected = lastPageSelected;
			}
			View view = nearbyFragment.getView();
			nearbyFragment.showSelectedTab(view);
			nearbyFragment.onPageSelected(nearbyFragment.lastPageSelected); // tell current page it's selected
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
		setupTabTheme(view);
		setupAdapters(view);
		setSwipeRefreshLayoutEnabled(!isFixedOn());
		switchView(view);
	}

	private void setupTabTheme(View view) {
		if (view == null) {
			return;
		}
		view.findViewById(R.id.tabs).setBackgroundColor(getABBgColor(getActivity()));
	}

	private void setupAdapters(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		notifyTabDataChanged(view);
		showSelectedTab(view);
	}

	private void notifyTabDataChanged(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setViewPager(viewPager); // not linked to adapter changes
	}

	private void showSelectedTab(View view) {
		if (view == null) {
			return;
		}
		if (!hasAvailableTypes()) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			return;
		}
		if (this.lastPageSelected < 0) {
			new LoadLastPageSelectedFromUserPreference(getActivity(), this, this.selectedTypeId, getAvailableTypesOrNull()).execute();
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setCurrentItem(this.lastPageSelected);
		switchView(view);
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
			MTActivityWithLocation.broadcastUserLocationChanged(this, getChildFragmentManager(), newLocation);
		}
		if (!isFixedOn()) {
			if (this.nearbyLocation == null) {
				setNewNearbyLocation(newLocation);
			} else {
				if (this.adapter != null && this.adapter.isInitialized()
						&& !LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation, LocationUtils.LOCATION_CHANGED_NOTIFY_USER_IN_METERS)) {
					showLocationToast();
				} else {
					hideLocationToast();
				}
			}
		}
	}

	private PopupWindow locationToast = null;

	private PopupWindow getLocationToast() {
		if (this.locationToast == null) {
			initLocationPopup();
		}
		return this.locationToast;
	}

	private void initLocationPopup() {
		this.locationToast = ToastUtils.getNewTouchableToast(getActivity(), R.string.new_location_toast);
		if (this.locationToast != null) {
			this.locationToast.setTouchInterceptor(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent me) {
					if (me.getAction() == MotionEvent.ACTION_DOWN) {
						boolean handled = initiateRefresh();
						hideLocationToast();
						return handled;
					}
					return false; // not handled
				}
			});
			this.locationToast.setOnDismissListener(new PopupWindow.OnDismissListener() {
				@Override
				public void onDismiss() {
					NearbyFragment.this.toastShown = false;
				}
			});
		}
	}

	private boolean toastShown = false;

	private void showLocationToast() {
		if (!this.toastShown) {
			PopupWindow locationToast = getLocationToast();
			if (locationToast != null) {
				this.toastShown = ToastUtils.showTouchableToast(getActivity(), locationToast, getView());
			}
		}
	}

	private void hideLocationToast() {
		if (this.locationToast != null) {
			this.locationToast.dismiss();
		}
		this.toastShown = false;
	}

	private void setNewNearbyLocation(Location newNearbyLocation) {
		if (newNearbyLocation == null) {
			return;
		}
		this.nearbyLocation = newNearbyLocation;
		hideLocationToast();
		this.adapter.setNearbyLocation(this.nearbyLocation);
		broadcastNearbyLocationChanged(this.nearbyLocation);
		setSwipeRefreshLayoutRefreshing(false);
		this.nearbyLocationAddress = null;
		getAbController().setABReady(this, isABReady(), true);
		findNearbyLocation();
	}

	private void broadcastNearbyLocationChanged(Location location) {
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof NearbyFragment.NearbyLocationListener) {
					((NearbyFragment.NearbyLocationListener) fragment).onNearbyLocationChanged(location);
				}
			}
		}
	}

	public Location getNearbyLocation() {
		return nearbyLocation;
	}

	private void switchView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading(view);
		} else if (this.adapter.getCount() > 0) {
			showTabsAndViewPager(view);
		} else {
			showEmpty(view);
		}
	}

	private void showTabsAndViewPager(View view) {
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		view.findViewById(R.id.tabs).setVisibility(View.VISIBLE); // show
		view.findViewById(R.id.viewpager).setVisibility(View.VISIBLE); // show
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.tabs) != null) { // IF inflated/present DO
			view.findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			view.findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty(View view) {
		if (view.findViewById(R.id.tabs) != null) { // IF inflated/present DO
			view.findViewById(R.id.tabs).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.viewpager) != null) { // IF inflated/present DO
			view.findViewById(R.id.viewpager).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.empty_stub)).inflate(); // inflate
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public void onPageSelected(int position) {
		StatusLoader.get().clearAllTasks();
		ServiceUpdateLoader.get().clearAllTasks();
		setFragmentVisibleAtPosition(position);
		this.lastPageSelected = position;
		this.selectedTypeId = this.adapter.getTypeId(position);
		this.adapter.setLastVisibleFragmentPosition(this.lastPageSelected);
		PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE, this.selectedTypeId, false);
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		switch (state) {
		case ViewPager.SCROLL_STATE_IDLE:
			setFragmentVisibleAtPosition(this.lastPageSelected); // resume
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			setFragmentVisibleAtPosition(-1); // pause
			break;
		}
	}

	private void setFragmentVisibleAtPosition(int position) {
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
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
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
		if (isFixedOn()) {
			this.showDirectionsMenuItem.setVisible(true);
		} else {
			this.showDirectionsMenuItem.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_show_directions:
			if (this.fixedOnLat != null && this.fixedOnLng != null) {
				MapUtils.showDirection(getActivity(), this.fixedOnLat, this.fixedOnLng, null, null, this.fixedOnName);
				return true; // handled
			}
			break;
		case R.id.menu_show_map:
			Location fixedOnLocation = null;
			if (this.fixedOnLat != null && this.fixedOnLng != null) {
				fixedOnLocation = LocationUtils.getNewLocation(this.fixedOnLat, this.fixedOnLng);
			}
			((MainActivity) getActivity()).addFragmentToStack(MapFragment.newInstance(fixedOnLocation, null, this.selectedTypeId));
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public CharSequence getABTitle(Context context) {
		if (isFixedOn()) {
			return this.fixedOnName;
		}
		return context.getString(R.string.nearby);
	}

	@Override
	public Integer getABBgColor(Context context) {
		if (isFixedOn()) {
			if (this.fixedOnColor != null) {
				return this.fixedOnColor;
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

	private static class AgencyTypePagerAdapter extends FragmentStatePagerAdapter implements MTLog.Loggable {

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

		public AgencyTypePagerAdapter(Context context, NearbyFragment nearbyFragment, ArrayList<DataSourceType> availableAgencyTypes,
				boolean swipeRefreshLayoutEnabled) {
			super(nearbyFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(context);
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
			notifyDataSetChanged();
		}

		public boolean isInitialized() {
			return this.availableAgencyTypes != null && this.availableAgencyTypes.size() > 0;
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
		public Fragment getItem(int position) {
			Integer typeId = this.typeIds.get(position);
			NearbyAgencyTypeFragment f = NearbyAgencyTypeFragment.newInstance(position, this.lastVisibleFragmentPosition, typeId, this.nearbyLocation);
			f.setNearbyFragment(this.nearbyFragmentWR == null ? null : this.nearbyFragmentWR.get());
			f.setSwipeRefreshLayoutEnabled(this.swipeRefreshLayoutEnabled);
			return f;
		}
	}
}
