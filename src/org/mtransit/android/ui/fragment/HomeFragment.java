package org.mtransit.android.ui.fragment;

import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.di.Injection;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.FragmentAsyncTaskV4;
import org.mtransit.android.task.HomePOILoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.widget.ListViewSwipeRefreshLayout;
import org.mtransit.android.util.LoaderUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.PopupWindow;

public class HomeFragment extends ABFragment implements LoaderManager.LoaderCallbacks<ArrayList<POIManager>>, MTActivityWithLocation.UserLocationListener,
		FavoriteManager.FavoriteUpdateListener, SwipeRefreshLayout.OnRefreshListener, POIArrayAdapter.TypeHeaderButtonsClickListener,
		POIArrayAdapter.InfiniteLoadingListener {

	private static final String TAG = HomeFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Home";

	@NonNull
	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_NEARBY_LOCATION = "extra_nearby_location";

	public static HomeFragment newInstance(Location optNearbyLocation) {
		HomeFragment f = new HomeFragment();
		Bundle args = new Bundle();
		if (optNearbyLocation != null) {
			args.putParcelable(EXTRA_NEARBY_LOCATION, optNearbyLocation);
		}
		f.setArguments(args);
		return f;
	}

	private POIArrayAdapter adapter;
	private Location userLocation;
	private Location nearbyLocation;
	private String nearbyLocationAddress;
	private ListViewSwipeRefreshLayout swipeRefreshLayout;

	@NonNull
	private final CrashReporter crashReporter;
	@NonNull
	private final IAdManager adManager;

	public HomeFragment() {
		super();
		this.crashReporter = Injection.providesCrashReporter();
		this.adManager = Injection.providesAdManager();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
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
		View view = inflater.inflate(R.layout.fragment_home, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onRefresh() {
		initiateRefresh();
	}

	private boolean initiateRefresh() {
		if (LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation, LocationUtils.LOCATION_CHANGED_ALLOW_REFRESH_IN_METERS)) {
			setSwipeRefreshLayoutRefreshing(false);
			return false;
		}
		useNewNearbyLocation(this.userLocation);
		return true;
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		this.nearbyLocation = null; // force refresh
		initiateRefresh();
		this.modulesUpdated = false; // processed
	}

	private void restoreInstanceState(Bundle... bundles) {
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.nearbyLocation != null) {
			outState.putParcelable(EXTRA_NEARBY_LOCATION, this.nearbyLocation);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		if (this.modulesUpdated) {
			if (view != null) {
				view.post(new Runnable() {
					@Override
					public void run() {
						if (HomeFragment.this.modulesUpdated) {
							onModulesUpdated();
						}
					}
				});
			}
		}
		switchView(view);
		if (this.adapter != null) {
			this.adapter.onResume(getActivity(), this.userLocation);
		}
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getLastLocation());
		}
	}

	private FindNearbyLocationTask findNearbyLocationTask;

	private static class FindNearbyLocationTask extends FragmentAsyncTaskV4<Location, Void, String, HomeFragment> {

		@Override
		public String getLogTag() {
			return HomeFragment.class.getSimpleName() + ">" + FindNearbyLocationTask.class.getSimpleName();
		}

		public FindNearbyLocationTask(HomeFragment homeFragment) {
			super(homeFragment);
		}

		@Override
		protected String doInBackgroundWithFragment(@NonNull HomeFragment homeFragment, Location... locations) {
			Context context = homeFragment.getContext();
			Location nearbyLocation = locations[0];
			if (context == null || nearbyLocation == null) {
				return null;
			}
			Address address = LocationUtils.getLocationAddress(context, nearbyLocation);
			return LocationUtils.getLocationString(context, null, address, nearbyLocation.getAccuracy());
		}

		@Override
		protected void onPostExecuteFragmentReady(@NonNull HomeFragment homeFragment, @Nullable String newLocationAddress) {
			boolean refreshRequired = newLocationAddress != null && !newLocationAddress.equals(homeFragment.nearbyLocationAddress);
			homeFragment.nearbyLocationAddress = newLocationAddress;
			if (refreshRequired) {
				Context context = homeFragment.getContext();
				if (context != null) {
					homeFragment.getAbController().setABSubtitle(homeFragment, homeFragment.getABSubtitle(context), false);
					homeFragment.getAbController().setABReady(homeFragment, homeFragment.isABReady(), true);
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

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (this.swipeRefreshLayout != null) {
			this.swipeRefreshLayout.setOnRefreshListener(null);
			this.swipeRefreshLayout = null;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.userLocation = null;
		if (this.adapter != null) {
			this.adapter.onDestroy();
			this.adapter = null;
		}
		this.loadFinished = false;
		TaskUtils.cancelQuietly(this.findNearbyLocationTask, true);
	}

	private static final int POIS_LOADER = 0;

	@NonNull
	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case POIS_LOADER:
			if (this.nearbyLocation == null) {
				this.crashReporter.w(this, "onCreateLoader() > skip (no nearby location)");
				return null;
			}
			this.loadFinished = false;
			return new HomePOILoader(this, getContext(), this.nearbyLocation.getLatitude(), this.nearbyLocation.getLongitude(),
					this.nearbyLocation.getAccuracy());
		default:
			this.crashReporter.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<ArrayList<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
		this.loadFinished = false;
	}

	public void onLoadPartial(ArrayList<POIManager> data) {
		addPOIs(data);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<ArrayList<POIManager>> loader, ArrayList<POIManager> data) {
		this.loadFinished = true;
		addPOIs(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		findNearbyLocation();
	}

	private void addPOIs(ArrayList<POIManager> data) {
		if (this.adapter == null) {
			return; // too late
		}
		boolean scrollToTop = this.adapter.getPoisCount() == 0;
		this.adapter.appendPois(data);
		View view = getView();
		if (scrollToTop && view != null && view.findViewById(R.id.list) != null) {
			((AbsListView) view.findViewById(R.id.list)).setSelection(0);
		}
	}

	@Override
	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
			this.userLocation = newLocation;
			if (this.adapter != null) {
				this.adapter.setLocation(newLocation);
			}
		}
		if (this.nearbyLocation == null) {
			useNewNearbyLocation(newLocation);
		} else {
			if (this.adapter != null && this.adapter.isInitialized() //
					&& !LocationUtils.areAlmostTheSame(this.nearbyLocation, this.userLocation, LocationUtils.LOCATION_CHANGED_NOTIFY_USER_IN_METERS)) {
				showLocationToast();
			} else {
				hideLocationToast();
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
		this.locationToast = ToastUtils.getNewTouchableToast(getContext(), R.drawable.toast_frame_old, R.string.new_location_toast);
		if (this.locationToast != null) {
			this.locationToast.setTouchInterceptor(new View.OnTouchListener() {
				@SuppressLint("ClickableViewAccessibility")
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
					HomeFragment.this.toastShown = false;
				}
			});
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

	private void useNewNearbyLocation(Location newNearbyLocation) {
		if (newNearbyLocation == null) {
			return;
		}
		if (LocationUtils.areTheSame(newNearbyLocation, this.nearbyLocation)) {
			return;
		}
		this.nearbyLocation = newNearbyLocation;
		if (this.adapter != null) {
			this.adapter.clear();
		}
		this.loadFinished = false;
		switchView(getView());
		if (this.nearbyLocation != null) {
			LoaderUtils.restartLoader(this, POIS_LOADER, null, this);
		}
		hideLocationToast();
		setSwipeRefreshLayoutRefreshing(false);
		this.nearbyLocationAddress = null;
		getAbController().setABSubtitle(this, getABSubtitle(getContext()), false);
		getAbController().setABReady(this, isABReady(), true);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_home, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_show_map:
			if (getActivity() != null) {
				((MainActivity) getActivity()).addFragmentToStack(MapFragment.newInstance(null, null, null), this);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
		}
		hideLocationToast();
	}

	@Override
	public void onFavoriteUpdated() {
		if (this.adapter != null) {
			this.adapter.onFavoriteUpdated();
		}
	}

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setTag(getLogTag());
		this.adapter.setFavoriteUpdateListener(this);
		this.adapter.setShowBrowseHeaderSection(true);
		this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_MORE);
		this.adapter.setShowTypeHeaderNearby(true);
		this.adapter.setInfiniteLoading(true);
		this.adapter.setInfiniteLoadingListener(this);
		this.adapter.setOnTypeHeaderButtonsClickListener(this);
	}

	private boolean loadFinished = false;

	@Override
	public boolean isLoadingMore() {
		return this.adapter == null || this.adapter.getPoisCount() == 0 || !this.loadFinished;
	}

	@Override
	public boolean showingDone() {
		return false;
	}

	@Override
	public boolean onTypeHeaderButtonClick(int buttonId, DataSourceType type) {
		if (buttonId == POIArrayAdapter.TypeHeaderButtonsClickListener.BUTTON_MORE) {
			if (getActivity() != null) {
				((MainActivity) getActivity()).addFragmentToStack(NearbyFragment.newNearbyInstance(null, type.getId()), this);
			}
			return true; // handled
		}
		return false; // not handled
	}

	private void setupView(@NonNull View view) {
		this.swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
		this.swipeRefreshLayout.setColorSchemeColors(ThemeUtils.resolveColorAttribute(view.getContext(), R.attr.colorAccent));
		this.swipeRefreshLayout.setOnRefreshListener(this);
		inflateList(view);
		switchView(view);
		linkAdapterWithListView(view);
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

	private void switchView(View view) {
		if (view == null) {
			return;
		}
		showList(view);
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

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.app_name);
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
		return this.nearbyLocationAddress;
	}
}
