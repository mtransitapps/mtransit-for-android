package org.mtransit.android.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
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

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.mtransit.android.R;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;
import org.mtransit.android.task.MTCancellableFragmentAsyncTask;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.ActionBarController;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.NavigationDrawerController;
import org.mtransit.android.util.MapUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mtransit.commons.FeatureFlags.F_CACHE_DATA_SOURCES;

public class NearbyFragment extends ABFragment implements ViewPager.OnPageChangeListener,
		MTActivityWithLocation.UserLocationListener,
		SwipeRefreshLayout.OnRefreshListener {

	private static final String TAG = NearbyFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Nearby";

	@NonNull
	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_SELECTED_TYPE = "extra_selected_type";

	private static final String EXTRA_FIXED_ON_LAT = "extra_fixed_on_lat";
	private static final String EXTRA_FIXED_ON_LNG = "extra_fixed_on_lng";
	private static final String EXTRA_FIXED_ON_NAME = "extra_fixed_on_name";
	private static final String EXTRA_FIXED_ON_COLOR = "extra_fixed_on_color";

	@NonNull
	public static NearbyFragment newNearbyInstance(@Nullable Location optNearbyLocation, @Nullable Integer optTypeId) {
		return newInstance(optNearbyLocation, optTypeId, null, null, null, null);
	}

	@NonNull
	public static NearbyFragment newFixedOnInstance(@Nullable Integer optTypeId,
													double fixedOnLat,
													double fixedOnLng,
													@NonNull String fixedOnName,
													@Nullable Integer optFixedOnColor) {
		return newInstance(null, optTypeId, fixedOnLat, fixedOnLng, fixedOnName, optFixedOnColor);
	}

	@NonNull
	private static NearbyFragment newInstance(@SuppressWarnings("unused") @Nullable Location optNearbyLocation,
											  @Nullable Integer optTypeId,
											  @Nullable Double optFixedOnLat,
											  @Nullable Double optFixedOnLng,
											  @Nullable String optFixedOnName,
											  @Nullable Integer optFixedOnColor) {
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
	@Nullable
	private AgencyTypePagerAdapter adapter;
	@Nullable
	private Location nearbyLocation;
	@Nullable
	private String nearbyLocationAddress;
	@Nullable
	private Location userLocation;
	@Nullable
	private Integer selectedTypeId = null;
	@Nullable
	private Double fixedOnLat = null;
	@Nullable
	private Double fixedOnLng = null;
	@Nullable
	private String fixedOnName = null;
	@Nullable
	private Integer fixedOnColor = null;
	@Nullable
	private Boolean isFixedOn = null;

	@NonNull
	private final IAdManager adManager;
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public NearbyFragment() {
		super();
		this.adManager = Injection.providesAdManager();
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

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
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	private void initAdapters(@NonNull Activity activity) {
		this.adapter = new AgencyTypePagerAdapter(activity, this, null, false);
		this.adapter.setSwipeRefreshLayoutEnabled(!isFixedOn());
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		restoreInstanceState(savedInstanceState, getArguments());
		if (F_CACHE_DATA_SOURCES) {
			this.dataSourcesRepository.readingAllDataSourceTypesDistinct().observe(this, newAllDataSourceTypes -> {
				List<DataSourceType> newAvailableAgencyTypes = filterAgencyTypes(newAllDataSourceTypes);
				if (CollectionUtils.getSize(newAvailableAgencyTypes) != CollectionUtils.getSize(this.availableTypes)) {
					this.availableTypes = newAvailableAgencyTypes;
					applyNewAvailableTypes();
				}
			});
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
		if (!F_CACHE_DATA_SOURCES) {
			if (this.adapter != null) {
				FragmentActivity activity = getActivity();
				if (activity == null) {
					return;
				}
				Collection<DataSourceType> availableAgencyTypes;
				if (F_CACHE_DATA_SOURCES) {
					availableAgencyTypes = this.dataSourcesRepository.getAllDataSourceTypes();
				} else {
					availableAgencyTypes = org.mtransit.android.data.DataSourceProvider.get(getContext()).getAvailableAgencyTypes();
				}
				List<DataSourceType> newAvailableAgencyTypes = filterAgencyTypes(availableAgencyTypes);
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
		this.modulesUpdated = false; // processed
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
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
		View view = getView();
		if (this.modulesUpdated) {
			if (view != null) {
				view.post(() -> {
					if (NearbyFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				});
			}
		}
		if (this.lastPageSelected >= 0) {
			onPageSelected(this.lastPageSelected); // tell current page it's selected
		}
		switchView(view);
		if (isFixedOn() && this.fixedOnLat != null && this.fixedOnLng != null) {
			setNewNearbyLocation(LocationUtils.getNewLocation(this.fixedOnLat, this.fixedOnLng));
		}
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.userLocation = null;
		this.nearbyLocation = null;
		TaskUtils.cancelQuietly(this.findNearbyLocationTask, true);
		TaskUtils.cancelQuietly(this.loadAvailableTypesTask, true);
	}

	@Nullable
	private FindNearbyLocationTask findNearbyLocationTask;

	@SuppressWarnings("deprecation")
	private static class FindNearbyLocationTask extends MTCancellableFragmentAsyncTask<Location, Void, String, NearbyFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return NearbyFragment.class.getSimpleName() + ">" + FindNearbyLocationTask.class.getSimpleName();
		}

		FindNearbyLocationTask(NearbyFragment nearbyFragment) {
			super(nearbyFragment);
		}

		@Override
		protected String doInBackgroundNotCancelledWithFragmentMT(@NonNull NearbyFragment nearbyFragment, Location... locations) {
			Context context = nearbyFragment.getActivity();
			Location nearbyLocation = locations == null ? null : locations[0];
			if (context == null || nearbyLocation == null) {
				return null;
			}
			Address address = LocationUtils.getLocationAddress(context, nearbyLocation);
			return LocationUtils.getLocationString(context, null, address, nearbyLocation.getAccuracy());
		}

		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull NearbyFragment nearbyFragment, @Nullable String newLocationAddress) {
			boolean refreshRequired = newLocationAddress != null && !newLocationAddress.equals(nearbyFragment.nearbyLocationAddress);
			nearbyFragment.nearbyLocationAddress = newLocationAddress;
			if (refreshRequired) {
				Context context = nearbyFragment.getContext();
				if (context != null) {
					MTLog.d(this, "onPostExecute() > ab controller refresh");
					final ActionBarController abController = nearbyFragment.getAbController();
					if (abController != null) {
						abController.setABSubtitle(nearbyFragment, nearbyFragment.getABSubtitle(context), false);
						abController.setABReady(nearbyFragment, nearbyFragment.isABReady(), true);
					}
				}
			}
		}
	}

	private void findNearbyLocation() {
		if (!TextUtils.isEmpty(this.nearbyLocationAddress)) {
			return;
		}
		TaskUtils.cancelQuietly(this.findNearbyLocationTask, true);
		this.findNearbyLocationTask = new FindNearbyLocationTask(this);
		TaskUtils.execute(this.findNearbyLocationTask, this.nearbyLocation);
	}

	@Nullable
	private List<DataSourceType> availableTypes = null;

	@Nullable
	private List<DataSourceType> getAvailableTypesOrNull() {
		if (!hasAvailableTypes()) {
			return null;
		}
		return this.availableTypes;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean hasAvailableTypes() {
		if (this.availableTypes == null) {
			initAvailableTypesAsync();
			return false;
		}
		return true;
	}

	private void initAvailableTypesAsync() {
		if (F_CACHE_DATA_SOURCES) {
			return;
		}
		if (this.loadAvailableTypesTask != null && this.loadAvailableTypesTask.getStatus() == LoadAvailableTypesTask.Status.RUNNING) {
			return;
		}
		this.loadAvailableTypesTask = new LoadAvailableTypesTask(this);
		TaskUtils.execute(this.loadAvailableTypesTask);
	}

	@Nullable
	private LoadAvailableTypesTask loadAvailableTypesTask = null;

	@SuppressWarnings("deprecation")
	private static class LoadAvailableTypesTask extends MTCancellableFragmentAsyncTask<Void, Void, Boolean, NearbyFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return NearbyFragment.class.getSimpleName() + ">" + LoadAvailableTypesTask.class.getSimpleName();
		}

		LoadAvailableTypesTask(NearbyFragment nearbyFragment) {
			super(nearbyFragment);
		}

		@WorkerThread
		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull NearbyFragment nearbyFragment, Void... params) {
			return nearbyFragment.initAvailableTypesSync();
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull NearbyFragment nearbyFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				nearbyFragment.applyNewAvailableTypes();
			}
		}
	}

	@WorkerThread
	private boolean initAvailableTypesSync() {
		if (this.availableTypes != null) {
			return false;
		}
		Collection<DataSourceType> availableAgencyTypes;
		if (F_CACHE_DATA_SOURCES) {
			availableAgencyTypes = this.dataSourcesRepository.getAllDataSourceTypes();
		} else {
			availableAgencyTypes = org.mtransit.android.data.DataSourceProvider.get(getContext()).getAvailableAgencyTypes();
		}
		this.availableTypes = filterAgencyTypes(availableAgencyTypes);
		return !this.availableTypes.isEmpty();
	}

	@AnyThread
	private void applyNewAvailableTypes() {
		if (this.availableTypes == null) {
			return;
		}
		if (this.adapter != null) {
			if (this.adapter.setAvailableAgencyTypes(this.availableTypes)) {
				setFragmentPosition();
				View view = getView();
				notifyTabDataChanged(view);
				showSelectedTab(view);
			}
		}
	}

	private void resetAvailableTypes() {
		if (F_CACHE_DATA_SOURCES) {
			return;
		}
		this.availableTypes = null; // reset
	}

	@NonNull
	private List<DataSourceType> filterAgencyTypes(@Nullable Collection<DataSourceType> availableAgencyTypes) {
		List<DataSourceType> filteredAgencyTypes = new ArrayList<>();
		if (availableAgencyTypes != null) {
			for (DataSourceType type : availableAgencyTypes) {
				if (!type.isNearbyScreen()) {
					continue;
				}
				filteredAgencyTypes.add(type);
			}
		}
		return filteredAgencyTypes;
	}

	@SuppressWarnings("deprecation")
	private static class LoadLastPageSelectedFromUserPreference extends MTCancellableFragmentAsyncTask<Void, Void, Integer, NearbyFragment> {

		private static final String LOG_TAG = NearbyFragment.class.getSimpleName() + ">" + LoadLastPageSelectedFromUserPreference.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@Nullable
		private Integer selectedTypeId;
		@Nullable
		private final List<DataSourceType> newAgencyTypes;

		LoadLastPageSelectedFromUserPreference(@NonNull NearbyFragment nearbyFragment, @Nullable Integer selectedTypeId, @Nullable List<DataSourceType> newAgencyTypes) {
			super(nearbyFragment);
			this.selectedTypeId = selectedTypeId;
			this.newAgencyTypes = newAgencyTypes;
		}

		@WorkerThread
		@Override
		protected Integer doInBackgroundNotCancelledWithFragmentMT(@NonNull NearbyFragment nearbyFragment, Void... params) {
			try {
				if (this.selectedTypeId == null) {
					Context context = nearbyFragment.getContext();
					if (context != null) {
						this.selectedTypeId = PreferenceUtils.getPrefLcl(context, PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE, //
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
				MTLog.w(LOG_TAG, e, "Error while determining the select nearby tab!");
			}
			return null;
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull NearbyFragment nearbyFragment, @Nullable Integer lastPageSelected) {
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
		ViewPager viewPager = view.findViewById(R.id.viewpager);
		viewPager.setOffscreenPageLimit(3);
		viewPager.addOnPageChangeListener(this);
		TabLayout tabs = view.findViewById(R.id.tabs);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
		tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
		setupTabTheme(view);
		setupAdapters(view);
		setSwipeRefreshLayoutEnabled(!isFixedOn());
		switchView(view);
	}

	private void setupTabTheme(View view) {
		if (view == null) {
			return;
		}
		final Integer abBgColor = getABBgColor(getContext());
		if (abBgColor != null) {
			view.findViewById(R.id.tabs).setBackgroundColor(abBgColor);
		}
	}

	private void setupAdapters(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		TabLayout tabs = view.findViewById(R.id.tabs);
		tabs.setupWithViewPager(viewPager);
		notifyTabDataChanged(view);
		showSelectedTab(view);
	}

	private void notifyTabDataChanged(@SuppressWarnings("unused") View view) {
		// DO NOTHING
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
			TaskUtils.execute(new LoadLastPageSelectedFromUserPreference(this, this.selectedTypeId, getAvailableTypesOrNull()));
			return;
		}
		ViewPager viewPager = view.findViewById(R.id.viewpager);
		viewPager.setCurrentItem(this.lastPageSelected);
		switchView(view);
	}

	@Override
	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
			MTActivityWithLocation.broadcastUserLocationChanged(this, getChildFragments(), newLocation);
		}
		if (!isFixedOn()) {
			if (this.nearbyLocation == null) {
				setNewNearbyLocation(newLocation);
			} else {
				if (this.adapter != null && this.adapter.isInitialized() //
						&& !LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation, LocationUtils.LOCATION_CHANGED_NOTIFY_USER_IN_METERS)) {
					showLocationToast();
				} else {
					hideLocationToast();
				}
			}
		}
	}

	@Nullable
	private PopupWindow locationToast = null;

	@Nullable
	private PopupWindow getLocationToast() {
		if (this.locationToast == null) {
			initLocationPopup();
		}
		return this.locationToast;
	}

	@SuppressLint("ClickableViewAccessibility")
	private void initLocationPopup() {
		this.locationToast = ToastUtils.getNewTouchableToast(getContext(), R.drawable.toast_frame_old, R.string.new_location_toast);
		if (this.locationToast != null) {
			this.locationToast.setTouchInterceptor((v, me) -> {
				if (me.getAction() == MotionEvent.ACTION_DOWN) {
					boolean handled = initiateRefresh();
					hideLocationToast();
					return handled;
				}
				return false; // not handled
			});
			this.locationToast.setOnDismissListener(() ->
					NearbyFragment.this.toastShown = false
			);
		}
	}

	private boolean toastShown = false;

	private void showLocationToast() {
		if (!this.toastShown) {
			PopupWindow locationToast = getLocationToast();
			if (locationToast != null) {
				int adHeightInPx = this.adManager.getBannerHeightInPx(this);
				this.toastShown = ToastUtils.showTouchableToastPx(getContext(), locationToast, getView(), adHeightInPx);
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
		if (this.adapter != null) {
			this.adapter.setNearbyLocation(this.nearbyLocation);
		}
		broadcastNearbyLocationChanged(this.nearbyLocation);
		setSwipeRefreshLayoutRefreshing(false);
		this.nearbyLocationAddress = null;
		final ActionBarController abController = getAbController();
		if (abController != null) {
			abController.setABSubtitle(this, getABSubtitle(getContext()), false);
			abController.setABReady(this, isABReady(), true);
		}
		View view = getView();
		if (view != null) {
			view.postDelayed(
					this::findNearbyLocation,
					TimeUnit.SECONDS.toMillis(1L));
		}
	}

	private void broadcastNearbyLocationChanged(@Nullable Location location) {
		java.util.Set<Fragment> fragments = getChildFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyLocationListener) {
					((NearbyFragment.NearbyLocationListener) fragment).onNearbyLocationChanged(location);
				}
			}
		}
	}

	@Nullable
	Location getNearbyLocation() {
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
		if (this.adapter != null) {
			this.selectedTypeId = this.adapter.getTypeId(position);
			this.adapter.setLastVisibleFragmentPosition(this.lastPageSelected);
		}
		PreferenceUtils.savePrefLcl(getContext(), PreferenceUtils.PREFS_LCL_NEARBY_TAB_TYPE, this.selectedTypeId, false);
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
		java.util.Set<Fragment> fragments = getChildFragments();
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
		// DO NOTHING
	}

	private void setFragmentPosition() {
		java.util.Set<Fragment> fragments = getChildFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof NearbyAgencyTypeFragment) {
					NearbyAgencyTypeFragment nearbyAgencyTypeFragment = (NearbyAgencyTypeFragment) fragment;
					nearbyAgencyTypeFragment.setNearbyFragment(this);
					int newPosition = PagerAdapter.POSITION_NONE;
					if (this.adapter != null) {
						newPosition = this.adapter.getItemPosition(nearbyAgencyTypeFragment);
					}
					nearbyAgencyTypeFragment.setFragmentPosition(newPosition);
				}
			}
		}
	}

	private void setSwipeRefreshLayoutRefreshing(@SuppressWarnings("SameParameterValue") boolean refreshing) {
		java.util.Set<Fragment> fragments = getChildFragments();
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
		java.util.Set<Fragment> fragments = getChildFragments();
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

	@Nullable
	private MenuItem showDirectionsMenuItem;

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_nearby, menu);
		this.showDirectionsMenuItem = menu.findItem(R.id.menu_show_directions);
		updateDirectionsMenuItem();
	}

	private void updateDirectionsMenuItem() {
		if (this.showDirectionsMenuItem == null) {
			return;
		}
		//noinspection RedundantIfStatement
		if (isFixedOn()) {
			this.showDirectionsMenuItem.setVisible(true);
		} else {
			this.showDirectionsMenuItem.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_show_directions) {
			if (this.fixedOnLat != null && this.fixedOnLng != null) {
				MapUtils.showDirection(requireActivity(), this.fixedOnLat, this.fixedOnLng, null, null, this.fixedOnName);
				return true; // handled
			}
		} else if (itemId == R.id.menu_show_map) {
			Location fixedOnLocation = null;
			if (this.fixedOnLat != null && this.fixedOnLng != null) {
				fixedOnLocation = LocationUtils.getNewLocation(this.fixedOnLat, this.fixedOnLng);
			}
			if (getActivity() != null) {
				((MainActivity) getActivity()).addFragmentToStack(
						MapFragment.newInstance(fixedOnLocation, null, this.selectedTypeId),
						this);
			}
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}

	@Nullable
	@Override
	public CharSequence getABTitle(@Nullable Context context) {
		if (isFixedOn()) {
			return this.fixedOnName;
		}
		if (context == null) {
			return super.getABTitle(null);
		}
		return context.getString(R.string.nearby);
	}

	@Nullable
	@Override
	public Integer getABBgColor(@Nullable Context context) {
		if (isFixedOn()) {
			if (this.fixedOnColor != null) {
				return this.fixedOnColor;
			}
		}
		return super.getABBgColor(context);
	}

	@Nullable
	@Override
	public CharSequence getABSubtitle(@Nullable Context context) {
		return this.nearbyLocationAddress;
	}

	public interface NearbyLocationListener extends MTActivityWithLocation.UserLocationListener {
		void onNearbyLocationChanged(@Nullable Location location);
	}

	@SuppressWarnings("deprecation")
	private static class AgencyTypePagerAdapter extends FragmentStatePagerAdapter implements MTLog.Loggable {

		private static final String TAG = NearbyFragment.class.getSimpleName() + ">" + AgencyTypePagerAdapter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		@Nullable
		private List<DataSourceType> availableAgencyTypes;
		@NonNull
		private final WeakReference<Context> contextWR;
		@Nullable
		private Location nearbyLocation;
		private int lastVisibleFragmentPosition = -1;
		@NonNull
		private final WeakReference<NearbyFragment> nearbyFragmentWR;
		private int saveStateCount = -1;
		private boolean swipeRefreshLayoutEnabled;

		AgencyTypePagerAdapter(@NonNull Context context,
							   @NonNull NearbyFragment nearbyFragment,
							   @Nullable ArrayList<DataSourceType> availableAgencyTypes,
							   boolean swipeRefreshLayoutEnabled) {
			super(nearbyFragment.getChildFragmentManager());
			this.contextWR = new WeakReference<>(context);
			setAvailableAgencyTypes(availableAgencyTypes);
			this.nearbyFragmentWR = new WeakReference<>(nearbyFragment);
			this.swipeRefreshLayoutEnabled = swipeRefreshLayoutEnabled;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
			if (this.saveStateCount >= 0 && this.saveStateCount != getCount()) {
				if (!F_CACHE_DATA_SOURCES) {
					MTLog.d(this, "restoreState() > SKIP");
					return;
				}
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
		public void destroyItem(@NonNull View container, int position, @NonNull Object object) {
			try {
				super.destroyItem(container, position, object);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while destroying item at position '%s'!", position);
			}
		}

		@Override
		public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
			try {
				super.destroyItem(container, position, object);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while destroying item at position '%s'!", position);
			}
		}

		@Nullable
		List<DataSourceType> getAvailableAgencyTypes() {
			return availableAgencyTypes;
		}

		@NonNull
		private final List<Integer> typeIds = new ArrayList<>();

		boolean setAvailableAgencyTypes(@Nullable List<DataSourceType> availableAgencyTypes) {
			if (this.availableAgencyTypes == null && availableAgencyTypes == null) {
				MTLog.d(this, "applyNewAvailableTypes() > SKIP (same)");
				return false; // not changed
			}
			if (this.availableAgencyTypes != null && this.availableAgencyTypes.equals(availableAgencyTypes)) {
				MTLog.d(this, "applyNewAvailableTypes() > SKIP (same)");
				return false; // not changed
			}
			this.availableAgencyTypes = availableAgencyTypes;
			this.typeIds.clear();
			if (this.availableAgencyTypes != null) {
				for (DataSourceType type : this.availableAgencyTypes) {
					this.typeIds.add(type.getId());
				}
			}
			notifyDataSetChanged();
			return true;
		}

		public boolean isInitialized() {
			return this.availableAgencyTypes != null && this.availableAgencyTypes.size() > 0;
		}

		void setSwipeRefreshLayoutEnabled(boolean swipeRefreshLayoutEnabled) {
			this.swipeRefreshLayoutEnabled = swipeRefreshLayoutEnabled;
		}

		Integer getTypeId(int position) {
			return this.typeIds.get(position);
		}

		void setNearbyLocation(@Nullable Location nearbyLocation) {
			this.nearbyLocation = nearbyLocation;
		}

		void setLastVisibleFragmentPosition(int lastVisibleFragmentPosition) {
			this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}

		@Override
		public int getCount() {
			return this.typeIds.size();
		}

		@Override
		public int getItemPosition(@NonNull Object object) {
			if (object instanceof NearbyAgencyTypeFragment) {
				NearbyAgencyTypeFragment f = (NearbyAgencyTypeFragment) object;
				return getTypePosition(f.getTypeId());
			} else {
				return POSITION_NONE;
			}
		}

		private int getTypePosition(@Nullable Integer typeId) {
			if (typeId == null) {
				return POSITION_NONE;
			}
			final int indexOf = this.typeIds.indexOf(typeId);
			if (indexOf < 0) {
				return POSITION_NONE;
			}
			return indexOf;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Context context = this.contextWR.get();
			if (context == null) {
				return StringUtils.EMPTY;
			}
			if (this.availableAgencyTypes == null || position >= this.availableAgencyTypes.size()) {
				return StringUtils.EMPTY;
			}
			return context.getString(this.availableAgencyTypes.get(position).getShortNameResId());
		}

		@NonNull
		@Override
		public Fragment getItem(int position) {
			Integer typeId = this.typeIds.get(position);
			NearbyAgencyTypeFragment f = NearbyAgencyTypeFragment.newInstance(position, this.lastVisibleFragmentPosition, typeId, this.nearbyLocation);
			f.setNearbyFragment(this.nearbyFragmentWR.get());
			f.setSwipeRefreshLayoutEnabled(this.swipeRefreshLayoutEnabled);
			return f;
		}
	}
}
