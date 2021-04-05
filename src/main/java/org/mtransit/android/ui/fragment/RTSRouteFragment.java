package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CompoundButton;

import androidx.annotation.ColorInt;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;
import org.mtransit.android.task.MTCancellableFragmentAsyncTask;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTActivityWithLocation;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class RTSRouteFragment extends ABFragment implements ViewPager.OnPageChangeListener, MTActivityWithLocation.UserLocationListener,
		CompoundButton.OnCheckedChangeListener {

	private static final String TAG = RTSRouteFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "RTSRoute";

	@NonNull
	@Override
	public String getScreenName() {
		if (this.authority != null && this.routeId != null) {
			return TRACKING_SCREEN_NAME + "/" + this.authority + "/" + this.routeId;
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_AUTHORITY = "extra_authority";

	private static final String EXTRA_ROUTE_ID = "extra_route_id";

	private static final String EXTRA_TRIP_ID = "extra_trip_id";

	private static final String EXTRA_STOP_ID = "extra_stop_id";

	@NonNull
	public static RTSRouteFragment newInstance(@NonNull String authority, long routeId, @Nullable Long optTripId, @Nullable Integer optStopId, @Nullable Route optRoute) {
		RTSRouteFragment f = new RTSRouteFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		f.authority = authority;
		args.putLong(EXTRA_ROUTE_ID, routeId);
		f.routeId = routeId;
		f.route = optRoute;
		if (optTripId != null) {
			args.putLong(EXTRA_TRIP_ID, optTripId);
			f.tripId = optTripId;
		}
		if (optStopId != null) {
			args.putInt(EXTRA_STOP_ID, optStopId);
			f.stopId = optStopId;
		}
		f.setArguments(args);
		return f;
	}

	private int lastPageSelected = -1;
	@Nullable
	private RouteTripPagerAdapter adapter;
	@Nullable
	private String authority;
	@Nullable
	private Long routeId;
	private long tripId = -1L;
	private int stopId = -1;

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public RTSRouteFragment() {
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true); // child fragments options menus don't get updated when coming back from another activity
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_rts_route, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		switchView(view);
		if (getActivity() != null) {
			onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
		}
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AUTHORITY, bundles);
		if (newAuthority != null && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
			resetRouteTrips();
		}
		Long newRouteId = BundleUtils.getLong(EXTRA_ROUTE_ID, bundles);
		if (newRouteId != null && !newRouteId.equals(this.routeId)) {
			this.routeId = newRouteId;
			resetRoute();
			resetRouteTrips();
		}
		Long newTripId = BundleUtils.getLong(EXTRA_TRIP_ID, bundles);
		if (newTripId != null && !newTripId.equals(this.tripId)) {
			this.tripId = newTripId;
		}
		Integer newStopId = BundleUtils.getInt(EXTRA_STOP_ID, bundles);
		if (newStopId != null && !newStopId.equals(this.stopId)) {
			this.stopId = newStopId;
		}
		if (this.adapter != null) {
			this.adapter.setAuthority(this.authority);
			this.adapter.setStopId(this.stopId);
		}
	}

	@Nullable
	private ArrayList<Trip> routeTrips;

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean hasRouteTrips() {
		if (this.routeTrips == null) {
			initRouteTripsAsync();
			return false;
		}
		return true;
	}

	@Nullable
	private ArrayList<Trip> getRouteTripsOrNull() {
		if (!hasRouteTrips()) {
			return null;
		}
		return this.routeTrips;
	}

	private void initRouteTripsAsync() {
		if (this.loadRouteTripsTask != null && this.loadRouteTripsTask.getStatus() == LoadRouteTripsTask.Status.RUNNING) {
			return;
		}
		if (this.routeId == null || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadRouteTripsTask = new LoadRouteTripsTask(this);
		TaskUtils.execute(this.loadRouteTripsTask, this.authority, this.routeId);
	}

	@Nullable
	private LoadRouteTripsTask loadRouteTripsTask = null;

	@SuppressWarnings("deprecation")
	private static class LoadRouteTripsTask extends MTCancellableFragmentAsyncTask<Object, Void, Boolean, RTSRouteFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return RTSRouteFragment.class.getSimpleName() + ">" + LoadRouteTripsTask.class.getSimpleName();
		}

		LoadRouteTripsTask(RTSRouteFragment rtsRouteFragment) {
			super(rtsRouteFragment);
		}

		@WorkerThread
		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull RTSRouteFragment rtsRouteFragment, Object... params) {
			return rtsRouteFragment.initRouteTripsSync();
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull RTSRouteFragment rtsRouteFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				rtsRouteFragment.applyNewRouteTrips();
			}
		}
	}

	@WorkerThread
	private boolean initRouteTripsSync() {
		if (this.routeTrips != null) {
			return false;
		}
		if (this.routeId != null && this.authority != null) {
			this.routeTrips = DataSourceManager.findRTSRouteTrips(requireContext(), this.authority, this.routeId);
		}
		return this.routeTrips != null;
	}

	private void applyNewRouteTrips() {
		if (this.routeTrips == null) {
			return;
		}
		if (this.adapter != null) {
			this.adapter.setRouteTrips(this.routeTrips);
			View view = getView();
			notifyTabDataChanged(view);
			showSelectedTab(view);
		}
	}

	private void resetRouteTrips() {
		this.routeTrips = null; // reset
	}

	@Nullable
	private Route route;

	private boolean hasRoute() {
		if (this.route == null) {
			initRouteAsync();
			return false;
		}
		return true;
	}

	@Nullable
	private Route getRouteOrNull() {
		if (!hasRoute()) {
			return null;
		}
		return this.route;
	}

	private void initRouteAsync() {
		if (this.loadRouteTask != null && this.loadRouteTask.getStatus() == LoadRouteTask.Status.RUNNING) {
			return;
		}
		if (this.routeId == null || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadRouteTask = new LoadRouteTask(this);
		TaskUtils.execute(this.loadRouteTask, this.authority, this.routeId);
	}

	@Nullable
	private LoadRouteTask loadRouteTask = null;

	@SuppressWarnings("deprecation")
	private static class LoadRouteTask extends MTCancellableFragmentAsyncTask<Object, Void, Boolean, RTSRouteFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return RTSRouteFragment.class.getSimpleName() + ">" + LoadRouteTask.class.getSimpleName();
		}

		LoadRouteTask(RTSRouteFragment rtsRouteFragment) {
			super(rtsRouteFragment);
		}

		@WorkerThread
		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull RTSRouteFragment rtsRouteFragment, Object... params) {
			return rtsRouteFragment.initRouteSync();
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull RTSRouteFragment rtsRouteFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				rtsRouteFragment.applyNewRoute();
			}
		}
	}

	@WorkerThread
	private boolean initRouteSync() {
		if (this.route != null) {
			return false;
		}
		if (this.routeId != null && this.authority != null) {
			this.route = DataSourceManager.findRTSRoute(requireContext(), this.authority, this.routeId);
		}
		return this.route != null;
	}

	private void applyNewRoute() {
		if (this.route == null) {
			return;
		}
		if (getAbController() != null) {
			getAbController().setABBgColor(this, getABBgColor(getContext()), false);
			getAbController().setABTitle(this, getABTitle(getContext()), false);
			getAbController().setABReady(this, isABReady(), true);
		}
		setupTabTheme(getView());
		this.listMapToggleSelector = null; // force reset to use route color
		if (getActivity() != null) {
			getActivity().invalidateOptionsMenu(); // initialize action bar list/map switch icon
		}
		updateListMapToggleMenuItem();
	}

	private void resetRoute() {
		this.route = null; // reset
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (this.authority != null) {
			outState.putString(EXTRA_AUTHORITY, this.authority);
		}
		if (this.routeId != null) {
			outState.putLong(EXTRA_ROUTE_ID, this.routeId);
		}
		outState.putLong(EXTRA_TRIP_ID, this.tripId);
		outState.putInt(EXTRA_STOP_ID, this.stopId);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
		super.onPause();
		this.stopId = -1; // set only once
	}

	private void initAdapters(@NonNull Context context) {
		this.adapter = new RouteTripPagerAdapter(context, this, null, this.authority, this.stopId, isShowingListInsteadOfMap());
	}

	@SuppressWarnings("deprecation")
	private static class LoadLastPageSelectedFromUserPreference extends MTCancellableFragmentAsyncTask<Void, Void, Integer, RTSRouteFragment> {

		private final String LOG_TAG = RTSRouteFragment.class.getSimpleName() + ">" + LoadLastPageSelectedFromUserPreference.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@Nullable
		private final String authority;
		@Nullable
		private final Long routeId;
		private long tripId;
		@Nullable
		private final ArrayList<Trip> routeTrips;

		LoadLastPageSelectedFromUserPreference(@Nullable RTSRouteFragment rtsRouteFragment,
											   @Nullable String authority,
											   @Nullable Long routeId,
											   long tripId,
											   @Nullable ArrayList<Trip> routeTrips) {
			super(rtsRouteFragment);
			this.authority = authority;
			this.routeId = routeId;
			this.tripId = tripId;
			this.routeTrips = routeTrips;
		}

		@Override
		protected Integer doInBackgroundNotCancelledWithFragmentMT(@NonNull RTSRouteFragment rtsRouteFragment, Void... params) {
			try {
				Context context = rtsRouteFragment.getContext();
				if (this.tripId < 0L) {
					if (context != null && this.authority != null && this.routeId != null) {
						String routePref = PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(this.authority, this.routeId);
						this.tripId = PreferenceUtils.getPrefLcl(context, routePref, PreferenceUtils.PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT);
					} else {
						this.tripId = PreferenceUtils.PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT;
					}
				}
				if (this.routeTrips != null) {
					for (int i = 0; i < this.routeTrips.size(); i++) {
						if (this.routeTrips.get(i).getId() == this.tripId) {
							return i;
						}
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while determining the select agency tab!");
			}
			return null;
		}

		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull RTSRouteFragment rtsRouteFragment, @Nullable Integer lastPageSelected) {
			if (rtsRouteFragment.lastPageSelected >= 0) {
				return; // user has manually move to another page before, too late
			}
			if (lastPageSelected == null) {
				rtsRouteFragment.lastPageSelected = 0;
			} else {
				rtsRouteFragment.lastPageSelected = lastPageSelected;
			}
			View view = rtsRouteFragment.getView();
			rtsRouteFragment.showSelectedTab(view);
			rtsRouteFragment.onPageSelected(rtsRouteFragment.lastPageSelected); // tell current page it's selected
		}
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = view.findViewById(R.id.viewpager);
		viewPager.addOnPageChangeListener(this);
		TabLayout tabs = view.findViewById(R.id.tabs);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
		tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
		setupTabTheme(view);
		setupAdapters(view);
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
		if (!hasRouteTrips()) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			return;
		}
		if (this.lastPageSelected < 0) {
			TaskUtils.execute( //
					new LoadLastPageSelectedFromUserPreference(this, this.authority, this.routeId, this.tripId, getRouteTripsOrNull()));
			return;
		}
		ViewPager viewPager = view.findViewById(R.id.viewpager);
		viewPager.setCurrentItem(this.lastPageSelected);
		MTLog.d(this, "showSelectedTab() > switchView()");
		switchView(view);
	}

	private void setupTabTheme(View view) {
		if (view == null) {
			return;
		}
		Integer abBgColor = getABBgColor(getContext());
		if (abBgColor != null) {
			TabLayout tabs = view.findViewById(R.id.tabs);
			tabs.setBackgroundColor(abBgColor);
		}
	}

	@Override
	public void onUserLocationChanged(@Nullable Location newLocation) {
		if (newLocation != null) {
			MTActivityWithLocation.broadcastUserLocationChanged(this, getChildFragments(), newLocation);
		}
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
		if (this.adapter != null) {
			Trip trip = this.adapter.getTrip(position);
			if (trip != null) {
				this.tripId = trip.getId();
				if (this.authority != null && this.routeId != null) {
					String routePref = PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(this.authority, this.routeId);
					PreferenceUtils.savePrefLcl(getContext(), routePref, this.tripId, false);
				}
			}
		}
		setFragmentVisibleAtPosition(position);
		this.lastPageSelected = position;
		if (this.adapter != null) {
			this.adapter.setLastVisibleFragmentPosition(this.lastPageSelected);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		// DO NOTHING
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
				if (fragment instanceof VisibilityAwareFragment) {
					VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(position);
				}
			}
		}
	}

	@Override
	public boolean isABReady() {
		return hasRoute();
	}

	private static final TypefaceSpan TITLE_RSN_FONT = SpanUtils.getNewSansSerifCondensedTypefaceSpan();
	private static final StyleSpan TITLE_RSN_STYLE = SpanUtils.getNewBoldStyleSpan();
	private static final TypefaceSpan TITLE_RLN_FONT = SpanUtils.getNewSansSerifLightTypefaceSpan();

	@Nullable
	@Override
	public CharSequence getABTitle(@Nullable Context context) {
		Route route = getRouteOrNull();
		if (route == null) {
			return super.getABTitle(context);
		}
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		int startShortName = 0, endShortName = 0;
		if (!TextUtils.isEmpty(route.getShortName())) {
			startShortName = ssb.length();
			ssb.append(route.getShortName());
			endShortName = ssb.length();
		}
		int startLongName = 0, endLongName = 0;
		if (!TextUtils.isEmpty(route.getLongName())) {
			if (ssb.length() > 0) {
				ssb.append(StringUtils.SPACE_CAR).append(StringUtils.SPACE_CAR);
			}
			startLongName = ssb.length();
			ssb.append(route.getLongName());
			endLongName = ssb.length();
		}
		if (startShortName < endShortName) {
			ssb = SpanUtils.set(ssb, startShortName, endShortName, //
					TITLE_RSN_FONT, TITLE_RSN_STYLE);
		}
		if (startLongName < endLongName) {
			ssb = SpanUtils.set(ssb, startLongName, endLongName, TITLE_RLN_FONT);
		}
		return ssb;
	}

	@Nullable
	@ColorInt
	@Override
	public Integer getABBgColor(@Nullable Context context) {
		if (context == null || this.authority == null) {
			return super.getABBgColor(context);
		}
		return POIManager.getRouteColor(this.dataSourcesRepository, getRouteOrNull(), this.authority, super.getABBgColor(context));
	}

	private MenuItem listMapToggleMenuItem;
	private SwitchCompat listMapSwitchMenuItem;

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_rts_route, menu);
		this.listMapToggleMenuItem = menu.findItem(R.id.menu_toggle_list_map);
		this.listMapSwitchMenuItem = this.listMapToggleMenuItem.getActionView().findViewById(R.id.action_bar_switch_list_map);
		this.listMapSwitchMenuItem.setThumbDrawable(getListMapToggleSelector());
		this.listMapSwitchMenuItem.setOnCheckedChangeListener(this);
		updateListMapToggleMenuItem();
	}

	@Nullable
	private StateListDrawable listMapToggleSelector = null;

	@NonNull
	private StateListDrawable getListMapToggleSelector() {
		if (listMapToggleSelector == null) {
			Integer colorInt = POIManager.getRouteColor(this.dataSourcesRepository, getRouteOrNull(), this.authority, null);
			listMapToggleSelector = new StateListDrawable();
			LayerDrawable listLayerDrawable = (LayerDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.switch_thumb_list, requireContext().getTheme());
			if (listLayerDrawable != null) {
				GradientDrawable listOvalShape = (GradientDrawable) listLayerDrawable.findDrawableByLayerId(R.id.switch_list_oval_shape);
				if (colorInt != null) {
					listOvalShape.setColor(colorInt);
				}
				listMapToggleSelector.addState(new int[]{android.R.attr.state_checked}, listLayerDrawable);
			}
			LayerDrawable mapLayerDrawable = (LayerDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.switch_thumb_map, requireContext().getTheme());
			if (mapLayerDrawable != null) {
				GradientDrawable mapOvalShape = (GradientDrawable) mapLayerDrawable.findDrawableByLayerId(R.id.switch_map_oval_shape);
				if (colorInt != null) {
					mapOvalShape.setColor(colorInt);
				}
				listMapToggleSelector.addState(StateSet.WILD_CARD, mapLayerDrawable);
			}
		}
		return this.listMapToggleSelector;
	}

	private void updateListMapToggleMenuItem() {
		if (this.listMapToggleMenuItem == null) {
			return;
		}
		if (this.listMapSwitchMenuItem == null) {
			return;
		}
		this.listMapSwitchMenuItem.setChecked(isShowingListInsteadOfMap());
		this.listMapSwitchMenuItem.setVisibility(View.VISIBLE);
		this.listMapToggleMenuItem.setVisible(true);
	}

	private Boolean showingListInsteadOfMap = null;

	private boolean isShowingListInsteadOfMap() {
		if (this.showingListInsteadOfMap == null) {
			this.showingListInsteadOfMap = PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT;
		}
		return this.showingListInsteadOfMap;
	}

	private void setShowingListInsteadOfMap(boolean newShowingListInsteadOfMap) {
		if (this.showingListInsteadOfMap != null && this.showingListInsteadOfMap == newShowingListInsteadOfMap) {
			return; // nothing changed
		}
		this.showingListInsteadOfMap = newShowingListInsteadOfMap; // switching
		if (getActivity() != null) {
			getActivity().invalidateOptionsMenu(); // change action bar list/map switch icon
		}
		updateListMapToggleMenuItem();
		if (this.adapter != null) {
			this.adapter.setShowingListInsteadOfMap(this.showingListInsteadOfMap);
		}
		java.util.Set<Fragment> fragments = getChildFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof RTSTripStopsFragment) {
					((RTSTripStopsFragment) fragment).setShowingListInsteadOfMap(this.showingListInsteadOfMap);
				}
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.menu_toggle_list_map) {
			setShowingListInsteadOfMap(!isShowingListInsteadOfMap()); // switching
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
		if (buttonView.getId() == R.id.action_bar_switch_list_map) {
			setShowingListInsteadOfMap(isChecked);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		TaskUtils.cancelQuietly(this.loadRouteTask, true);
		TaskUtils.cancelQuietly(this.loadRouteTripsTask, true);
	}

	@SuppressWarnings("deprecation")
	private static class RouteTripPagerAdapter extends FragmentStatePagerAdapter implements MTLog.Loggable {

		private static final String TAG = RTSRouteFragment.class.getSimpleName() + ">" + RouteTripPagerAdapter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		@Nullable
		private ArrayList<Trip> routeTrips;
		@NonNull
		private final WeakReference<Context> contextWR;
		private int lastVisibleFragmentPosition = -1;
		@Nullable
		private String authority;
		private int stopId;
		private boolean showingListInsteadOfMap;

		RouteTripPagerAdapter(@NonNull Context context,
							  @NonNull RTSRouteFragment fragment,
							  @Nullable ArrayList<Trip> routeTrips,
							  @Nullable String authority,
							  int stopId,
							  boolean showingListInsteadOfMap) {
			super(fragment.getChildFragmentManager());
			this.contextWR = new WeakReference<>(context);
			this.routeTrips = routeTrips;
			this.authority = authority;
			this.stopId = stopId;
			this.showingListInsteadOfMap = showingListInsteadOfMap;
		}

		public boolean isInitialized() {
			return CollectionUtils.getSize(this.routeTrips) > 0;
		}

		public void setAuthority(@Nullable String authority) {
			this.authority = authority;
		}

		void setStopId(int stopId) {
			this.stopId = stopId;
		}

		void setShowingListInsteadOfMap(boolean showingListInsteadOfMap) {
			this.showingListInsteadOfMap = showingListInsteadOfMap;
		}

		void setRouteTrips(@Nullable ArrayList<Trip> routeTrips) {
			this.routeTrips = routeTrips;
			notifyDataSetChanged();
		}

		@Nullable
		Trip getTrip(int position) {
			return this.routeTrips == null || this.routeTrips.size() == 0 ? null : this.routeTrips.get(position);
		}

		void setLastVisibleFragmentPosition(int lastVisibleFragmentPosition) {
			this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}

		@Override
		public int getCount() {
			return this.routeTrips == null ? 0 : this.routeTrips.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Context context = this.contextWR.get();
			if (context == null) {
				return StringUtils.EMPTY;
			}
			if (this.routeTrips == null || position >= this.routeTrips.size()) {
				return StringUtils.EMPTY;
			}
			return this.routeTrips.get(position).getHeading(context).toUpperCase(Locale.ENGLISH);
		}

		@NonNull
		@Override
		public Fragment getItem(int position) {
			Trip trip = getTrip(position);
			if (trip == null || this.authority == null) {
				throw new RuntimeException("No item at position " + position + "!");
			}
			return RTSTripStopsFragment.newInstance(
					position,
					this.lastVisibleFragmentPosition,
					this.authority,
					trip.getId(),
					this.stopId,
					this.showingListInsteadOfMap
			);
		}
	}
}
