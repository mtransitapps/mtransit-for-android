package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.SlidingTabLayout;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;


public class RTSRouteFragment extends ABFragment implements ViewPager.OnPageChangeListener, MTActivityWithLocation.UserLocationListener {

	private static final String TAG = RTSRouteFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "RTSRoute";

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


	public static RTSRouteFragment newInstance(String authority, long routeId, Long optTripId, Integer optStopId, Route optRoute) {
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
	private RouteTripPagerAdapter adapter;
	private String authority;
	private Long routeId;
	private long tripId = -1l;
	private int stopId = -1;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true); // child fragments options menus don't get updated when coming back from another activity
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_rts_route, container, false);
		setupView(view);
		return view;
	}


	@Override
	public void onResume() {
		super.onResume();
		if (this.modulesUpdated) {
			getView().post(new Runnable() {
				@Override
				public void run() {
					if (RTSRouteFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
		switchView(getView());
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
		}
		Long newRouteId = BundleUtils.getLong(EXTRA_ROUTE_ID, bundles);
		if (newRouteId != null && !newRouteId.equals(this.routeId)) {
			this.routeId = newRouteId;
			resetRoute();
		}
		Long newTripId = BundleUtils.getLong(EXTRA_TRIP_ID, bundles);
		if (newTripId != null && !newTripId.equals(this.tripId)) {
			this.tripId = newTripId;
		}
		Integer newStopId = BundleUtils.getInt(EXTRA_STOP_ID, bundles);
		if (newStopId != null && !newStopId.equals(this.stopId)) {
			this.stopId = newStopId;
		}
		this.adapter.setAuthority(this.authority);
		this.adapter.setRouteId(this.routeId);
		this.adapter.setOptStopId(this.stopId);
		ArrayList<Trip> routeTrips = DataSourceManager.findRTSRouteTrips(getActivity(), this.authority, this.routeId);
		if (CollectionUtils.getSize(routeTrips) == 0) {
			return;
		}
		this.adapter.setRouteTrips(routeTrips);
	}

	private Route route;

	private boolean hasRoute() {
		if (this.route == null) {
			initRouteAsync();
			return false;
		}
		return true;
	}

	private Route getRouteOrNull() {
		if (!hasRoute()) {
			return null;
		}
		return this.route;
	}

	private void initRouteAsync() {
		if (this.loadRouteTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (this.routeId == null || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadRouteTask.execute(this.authority, this.routeId);
	}

	private MTAsyncTask<Object, Void, Boolean> loadRouteTask = new MTAsyncTask<Object, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">initRouteAsync";
		}

		@Override
		protected Boolean doInBackgroundMT(Object... params) {
			return initRouteSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewRoute();
			}
		}
	};

	private boolean initRouteSync() {
		if (this.route != null) {
			return false;
		}
		if (this.routeId != null && !TextUtils.isEmpty(this.authority)) {
			this.route = DataSourceManager.findRTSRoute(getActivity(), this.authority, this.routeId);
		}
		return this.route != null;
	}

	private void applyNewRoute() {
		if (this.route == null) {
			return;
		}
		getAbController().setABBgColor(this, getABBgColor(getActivity()), false);
		getAbController().setABTitle(this, getABTitle(getActivity()), false);
		getAbController().setABReady(this, isABReady(), true);
		setupTabTheme(getView());
		if (this.adapter != null) {
			this.adapter.setOptRoute(this.route);
		}
	}

	private void resetRoute() {
		this.route = null; // reset
	}

	private boolean isRouteEqual(Route otherRoute) {
		if (this.route == null) {
			return otherRoute == null;
		}
		return this.route.equals(otherRoute);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
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

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		if (this.routeId != null && !TextUtils.isEmpty(this.authority)) {
			FragmentActivity activity = getActivity();
			if (activity == null) {
				return;
			}
			Route newRoute = DataSourceManager.findRTSRoute(activity, this.authority, this.routeId);
			if (newRoute == null) {
				((MainActivity) activity).popFragmentFromStack(this); // close this fragment
				this.modulesUpdated = false; // processed
				return;
			}
			if (isRouteEqual(newRoute)) {
				this.modulesUpdated = false; // nothing to do
				return;
			}
			resetRoute();
			initAdapters(activity);
			setupView(getView());
			this.modulesUpdated = false; // processed
		}
	}

	private void initAdapters(Activity activity) {
		this.adapter = new RouteTripPagerAdapter(activity, this, null, this.authority, this.routeId, getRouteOrNull(), this.stopId, isShowingListInsteadOfMap());
	}

	private static class LoadLastPageSelectedFromUserPreference extends MTAsyncTask<Void, Void, Integer> {

		private final String TAG = RTSRouteFragment.class.getSimpleName() + ">" + LoadLastPageSelectedFromUserPreference.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<Context> contextWR;
		private WeakReference<RTSRouteFragment> rtsRouteFragmentWR;
		private String authority;
		private Long routeId;
		private Long tripId;
		private ArrayList<Trip> routeTrips;

		public LoadLastPageSelectedFromUserPreference(Context context, RTSRouteFragment rtsRouteFragment, String authority, Long routeId, Long tripId,
				ArrayList<Trip> routeTrips) {
			this.contextWR = new WeakReference<Context>(context);
			this.rtsRouteFragmentWR = new WeakReference<RTSRouteFragment>(rtsRouteFragment);
			this.authority = authority;
			this.routeId = routeId;
			this.tripId = tripId;
			this.routeTrips = routeTrips;
		}

		@Override
		protected Integer doInBackgroundMT(Void... params) {
			try {
				Context context = this.contextWR == null ? null : this.contextWR.get();
				if (this.tripId == null) {
					if (context != null) {
						this.tripId = PreferenceUtils.getPrefLcl(context, PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(this.authority, this.routeId),
								PreferenceUtils.PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT);
					} else {
						this.tripId = PreferenceUtils.PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT;
					}
				}
				if (this.routeTrips != null) {
					for (int i = 0; i < this.routeTrips.size(); i++) {
						if (this.routeTrips.get(i).id == this.tripId) {
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
		protected void onPostExecute(Integer lastPageSelected) {
			RTSRouteFragment rtsRouteFragment = this.rtsRouteFragmentWR == null ? null : this.rtsRouteFragmentWR.get();
			if (rtsRouteFragment == null) {
				return; // too late
			}
			if (rtsRouteFragment.lastPageSelected >= 0) {
				return; // user has manually move to another page before, too late
			}
			if (lastPageSelected == null) {
				rtsRouteFragment.lastPageSelected = 0;
			} else {
				rtsRouteFragment.lastPageSelected = lastPageSelected;
				View view = rtsRouteFragment.getView();
				if (view != null) {
					ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
					viewPager.setCurrentItem(rtsRouteFragment.lastPageSelected);
				}
			}
			rtsRouteFragment.onPageSelected(rtsRouteFragment.lastPageSelected); // tell current page it's selected
		}
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setCustomTabView(R.layout.layout_tab_indicator, R.id.tab_title);
		tabs.setSelectedIndicatorColors(Color.WHITE);
		tabs.setOnPageChangeListener(this);
		setupTabTheme(view);
		viewPager.setAdapter(this.adapter);
		tabs.setViewPager(viewPager);
		if (this.lastPageSelected >= 0) {
			viewPager.setCurrentItem(this.lastPageSelected);
		} else {
			new LoadLastPageSelectedFromUserPreference(getActivity(), this, this.authority, this.routeId, this.tripId, this.adapter.getRouteTrips()).execute();
		}
	}

	private void setupTabTheme(View view) {
		if (view == null) {
			return;
		}
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		Integer abBgColor = getABBgColor(getActivity());
		if (abBgColor != null) {
			tabs.setBackgroundColor(abBgColor);
		}
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			MTActivityWithLocation.broadcastUserLocationChanged(this, getChildFragmentManager(), newLocation);
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
		if (view.findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.loading_stub)).inflate(); // inflate
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
				String routePref = PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(this.authority, this.routeId);
				PreferenceUtils.savePrefLcl(getActivity(), routePref, this.tripId, false);
			}
		}
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(position);
				}
			}
		}
		this.lastPageSelected = position;
		if (this.adapter != null) {
			this.adapter.setLastVisibleFragmentPosition(this.lastPageSelected);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		switch (state) {
		case ViewPager.SCROLL_STATE_IDLE:
			java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
			if (fragments != null) {
				for (Fragment fragment : fragments) {
					if (fragment instanceof VisibilityAwareFragment) {
						VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
						visibilityAwareFragment.setFragmentVisibleAtPosition(this.lastPageSelected); // resume
					}
				}
			}
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			java.util.List<Fragment> fragments2 = getChildFragmentManager().getFragments();
			if (fragments2 != null) {
				for (Fragment fragment : fragments2) {
					if (fragment instanceof VisibilityAwareFragment) {
						VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
						visibilityAwareFragment.setFragmentVisibleAtPosition(-1); // pause
					}
				}
			}
			break;
		}
	}

	@Override
	public boolean isABReady() {
		return hasRoute();
	}



	@Override
	public CharSequence getABTitle(Context context) {
		Route route = getRouteOrNull();
		if (route == null) {
			return super.getABTitle(context);
		}
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		int startShortName = 0, endShortName = 0;
		if (!TextUtils.isEmpty(route.shortName)) {
			startShortName = ssb.length();
			ssb.append(route.shortName);
			endShortName = ssb.length();
		}
		int startLongName = 0, endLongName = 0;
		if (!TextUtils.isEmpty(route.longName)) {
			if (ssb.length() > 0) {
				ssb.append(StringUtils.SPACE_CAR).append(StringUtils.SPACE_CAR);
			}
			startLongName = ssb.length();
			ssb.append(route.longName);
			endLongName = ssb.length();
		}
		if (startShortName < endShortName) {
			SpanUtils.set(ssb, SpanUtils.SANS_SERIF_CONDENSED_TYPEFACE_SPAN, startShortName, endShortName);
			SpanUtils.set(ssb, SpanUtils.BOLD_STYLE_SPAN, startShortName, endShortName);
		}
		if (startLongName < endLongName) {
			SpanUtils.set(ssb, SpanUtils.SANS_SERIF_LIGHT_TYPEFACE_SPAN, startLongName, endLongName);
		}
		return ssb;
	}


	@Override
	public Integer getABBgColor(Context context) {
		Route route = getRouteOrNull();
		if (route == null) {
			return null; // not ready
		}
		return route.getColorInt();
	}

	private MenuItem listMapToggleMenuItem;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_rts_route, menu);
		this.listMapToggleMenuItem = menu.findItem(R.id.menu_toggle_list_map);
		updateListMapToggleMenuItem();
	}

	private void updateListMapToggleMenuItem() {
		if (this.listMapToggleMenuItem == null) {
			return;
		}
		this.listMapToggleMenuItem
				.setIcon(isShowingListInsteadOfMap() ? R.drawable.ic_action_action_map_holo_dark : R.drawable.ic_action_action_list_holo_dark);
		this.listMapToggleMenuItem.setTitle(isShowingListInsteadOfMap() ? R.string.menu_action_map : R.string.menu_action_list);
		this.listMapToggleMenuItem.setVisible(true);
	}

	private Boolean showingListInsteadOfMap = null;

	private boolean isShowingListInsteadOfMap() {
		if (this.showingListInsteadOfMap == null) {
			this.showingListInsteadOfMap = PreferenceUtils.PREFS_RTS_ROUTES_SHOWING_LIST_INSTEAD_OF_MAP_DEFAULT;
		}
		return this.showingListInsteadOfMap;
	}

	public void setShowingListInsteadOfMap(boolean newShowingListInsteadOfMap) {
		if (this.showingListInsteadOfMap != null && this.showingListInsteadOfMap == newShowingListInsteadOfMap) {
			return; // nothing changed
		}
		this.showingListInsteadOfMap = newShowingListInsteadOfMap; // switching
		getActivity().invalidateOptionsMenu(); // change action bar list/map switch icon
		updateListMapToggleMenuItem();
		if (this.adapter != null) {
			this.adapter.setShowingListInsteadOfMap(this.showingListInsteadOfMap);
		}
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof RTSTripStopsFragment) {
					((RTSTripStopsFragment) fragment).setShowingListInsteadOfMap(this.showingListInsteadOfMap);
				}
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_toggle_list_map:
			setShowingListInsteadOfMap(!isShowingListInsteadOfMap()); // switching
			return true; // handled
		}
		return super.onOptionsItemSelected(item);
	}

	private static class RouteTripPagerAdapter extends FragmentStatePagerAdapter {

		private ArrayList<Trip> routeTrips;
		private WeakReference<Context> contextWR;
		private int lastVisibleFragmentPosition = -1;
		private String authority;
		private Integer optStopId = null;
		private boolean showingListInsteadOfMap;
		private Long routeId;
		private Route optRoute;

		public RouteTripPagerAdapter(Context context, RTSRouteFragment fragment, ArrayList<Trip> routeTrips, String authority, Long routeId, Route optRoute,
				Integer optStopId, boolean showingListInsteadOfMap) {
			super(fragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(context);
			this.routeTrips = routeTrips;
			this.authority = authority;
			this.optStopId = optStopId;
			this.showingListInsteadOfMap = showingListInsteadOfMap;
			this.routeId = routeId;
			this.optRoute = optRoute;
		}

		public boolean isInitialized() {
			return CollectionUtils.getSize(this.routeTrips) > 0;
		}

		public void setOptRoute(Route optRoute) {
			this.optRoute = optRoute;
			if (this.optRoute != null) {
				setRouteId(this.optRoute.id);
			}
		}

		public void setAuthority(String authority) {
			this.authority = authority;
		}

		public void setRouteId(Long routeId) {
			this.routeId = routeId;
			if (this.routeId != null && this.optRoute != null && this.optRoute.id != routeId) {
				this.optRoute = null;
			}
		}

		public void setOptStopId(Integer optStopId) {
			this.optStopId = optStopId;
		}

		public void setShowingListInsteadOfMap(boolean showingListInsteadOfMap) {
			this.showingListInsteadOfMap = showingListInsteadOfMap;
		}

		public void setRouteTrips(ArrayList<Trip> routeTrips) {
			this.routeTrips = routeTrips;
		}

		public ArrayList<Trip> getRouteTrips() {
			return routeTrips;
		}

		public Trip getTrip(int position) {
			return this.routeTrips == null || this.routeTrips.size() == 0 ? null : this.routeTrips.get(position);
		}


		public void setLastVisibleFragmentPosition(int lastVisibleFragmentPosition) {
			this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}

		@Override
		public int getCount() {
			return this.routeTrips == null ? 0 : this.routeTrips.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Context context = this.contextWR == null ? null : this.contextWR.get();
			if (context == null) {
				return StringUtils.EMPTY;
			}
			if (this.routeTrips == null || position >= this.routeTrips.size()) {
				return StringUtils.EMPTY;
			}
			return this.routeTrips.get(position).getHeading(context).toUpperCase(Locale.ENGLISH);
		}

		@Override
		public Fragment getItem(int position) {
			Trip trip = getTrip(position);
			if (trip == null) {
				return null;
			}
			return RTSTripStopsFragment.newInstance(position, this.lastVisibleFragmentPosition, this.authority, this.routeId, trip.id, this.optStopId,
					this.showingListInsteadOfMap, this.optRoute);
		}

	}
}
