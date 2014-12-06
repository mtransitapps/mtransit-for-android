package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
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

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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


	public static RTSRouteFragment newInstance(String authority, int routeId, Integer optTripId, Integer optStopId, Route optRoute) {
		RTSRouteFragment f = new RTSRouteFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		f.authority = authority;
		args.putInt(EXTRA_ROUTE_ID, routeId);
		f.routeId = routeId;
		f.route = optRoute;
		if (optTripId != null) {
			args.putInt(EXTRA_TRIP_ID, optTripId.intValue());
			f.tripId = optTripId;
		}
		if (optStopId != null) {
			args.putInt(EXTRA_STOP_ID, optStopId.intValue());
			f.stopId = optStopId;
		}
		f.setArguments(args);
		return f;
	}

	private int lastPageSelected = -1;
	private RouteTripPagerAdapter adapter;
	private String authority;
	private Integer routeId;
	private Integer tripId;
	private Integer stopId;

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
		restoreInstanceState(savedInstanceState);
		setupView(view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		switchView(getView());
		if (this.adapter == null) {
			initTabsAndViewPager(getView());
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
		}
		Integer newRouteId = BundleUtils.getInt(EXTRA_ROUTE_ID, bundles);
		if (newRouteId != null && !newRouteId.equals(this.routeId)) {
			this.routeId = newRouteId;
			resetRoute();
		}
		Integer newTripId = BundleUtils.getInt(EXTRA_TRIP_ID, bundles);
		if (newTripId != null && !newTripId.equals(this.tripId)) {
			this.tripId = newTripId;
		}
		Integer newStopId = BundleUtils.getInt(EXTRA_STOP_ID, bundles);
		if (newStopId != null && !newStopId.equals(this.stopId)) {
			this.stopId = newStopId;
		}
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
			this.route = DataSourceManager.findRTSRoute(getActivity(), this.authority, this.routeId.intValue());
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
			outState.putInt(EXTRA_ROUTE_ID, this.routeId.intValue());
		}
		if (this.tripId != null) {
			outState.putInt(EXTRA_TRIP_ID, this.tripId.intValue());
		}
		if (this.stopId != null) {
			outState.putInt(EXTRA_STOP_ID, this.stopId.intValue());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
		super.onPause();
		this.stopId = null; // set only once
	}

	@Override
	public void onModulesUpdated() {
		if (this.routeId != null && !TextUtils.isEmpty(this.authority)) {
			Route newRoute = DataSourceManager.findRTSRoute(getActivity(), this.authority, this.routeId);
			if (newRoute == null) {
				((MainActivity) getActivity()).popFragmentFromStack(this); // close this fragment
				return;
			}
			if (isRouteEqual(newRoute)) {
				return;
			}
			resetRoute();
			this.adapter = null; // reset
			this.lastPageSelected = -1; // reset tab position
			initTabsAndViewPager(getView());
		}

	}

	private void initTabsAndViewPager(final View view) {
		if (TextUtils.isEmpty(this.authority) || this.routeId == null || view == null) {
			return;
		}
		getAbController().setABBgColor(this, getABBgColor(getActivity()), false);
		getAbController().setABCustomView(this, getABCustomView(), false);
		getAbController().setABReady(this, isABReady(), true);
		final ArrayList<Trip> routeTrips = DataSourceManager.findRTSRouteTrips(getActivity(), this.authority, this.routeId);
		if (routeTrips == null) {
			return;
		}
		if (this.adapter == null) {
			this.adapter = new RouteTripPagerAdapter(this, routeTrips, this.authority, this.routeId, getRouteOrNull(), this.stopId, isShowingListInsteadOfMap());
		} else {
			this.adapter.setRouteTrips(routeTrips);
			this.adapter.notifyDataSetChanged();
		}
		setupView(view);
		this.lastPageSelected = -1;
		new MTAsyncTask<Void, Void, Integer>() {

			private final String TAG = AgencyTypeFragment.class.getSimpleName() + ">LoadLastPageSelectedFromUserPreferences";

			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Integer doInBackgroundMT(Void... params) {
				try {
					if (RTSRouteFragment.this.tripId == null) {
						RTSRouteFragment.this.tripId = PreferenceUtils.getPrefLcl(getActivity(),
								PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(RTSRouteFragment.this.authority, RTSRouteFragment.this.routeId),
								PreferenceUtils.PREFS_LCL_RTS_ROUTE_TRIP_ID_TAB_DEFAULT);
					}
					for (int i = 0; i < routeTrips.size(); i++) {
						if (routeTrips.get(i).id == RTSRouteFragment.this.tripId) {
							return i;
						}
					}
				} catch (Exception e) {
					MTLog.w(TAG, e, "Error while determining the select agency tab!");
				}
				return null;
			}

			@Override
			protected void onPostExecute(Integer lastPageSelected) {
				if (RTSRouteFragment.this.lastPageSelected >= 0) {
					return; // user has manually move to another page before, too late
				}
				if (lastPageSelected == null) {
					RTSRouteFragment.this.lastPageSelected = 0;
				} else {
					RTSRouteFragment.this.lastPageSelected = lastPageSelected.intValue();
					ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
					viewPager.setCurrentItem(RTSRouteFragment.this.lastPageSelected);
				}
				switchView(view);
				onPageSelected(RTSRouteFragment.this.lastPageSelected); // tell current page it's selected
			}
		}.execute();
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
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
			if (this.adapter != null) {
				this.adapter.setUserLocation(newLocation);
			}
		}
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
			this.tripId = this.adapter.getTrip(position).getId();
			PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(this.authority, this.routeId), this.tripId, false);
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
			ssb.append(route.longName);
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
		return this.showingListInsteadOfMap.booleanValue();
	}
	public void setShowingListInsteadOfMap(boolean newShowingListInsteadOfMap) {
		if (this.showingListInsteadOfMap != null && this.showingListInsteadOfMap.booleanValue() == newShowingListInsteadOfMap) {
			return; // nothing changed
		}
		this.showingListInsteadOfMap = newShowingListInsteadOfMap; // switching
		getActivity().invalidateOptionsMenu(); // change action bar list/map switch icon
		updateListMapToggleMenuItem();
		if (this.adapter != null) {
			this.adapter.setShowingListInsteadOfMap(this.showingListInsteadOfMap.booleanValue());
		}
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment != null && fragment instanceof RTSTripStopsFragment) {
					((RTSTripStopsFragment) fragment).setShowingListInsteadOfMap(this.showingListInsteadOfMap.booleanValue());
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
		private Location userLocation;
		private int lastVisibleFragmentPosition = -1;
		private String authority;
		private Integer optStopId = null;
		private boolean showingListInsteadOfMap;
		private Integer routeId;
		private Route optRoute;

		public RouteTripPagerAdapter(RTSRouteFragment fragment, ArrayList<Trip> routeTrips, String authority, Integer routeId, Route optRoute,
				Integer optStopId, boolean showingListInsteadOfMap) {
			super(fragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(fragment.getActivity());
			this.routeTrips = routeTrips;
			this.authority = authority;
			this.optStopId = optStopId;
			this.showingListInsteadOfMap = showingListInsteadOfMap;
			this.routeId = routeId;
			this.optRoute = optRoute;
		}

		public void setOptRoute(Route optRoute) {
			this.optRoute = optRoute;
		}

		public void setShowingListInsteadOfMap(boolean showingListInsteadOfMap) {
			this.showingListInsteadOfMap = showingListInsteadOfMap;
		}

		public void setRouteTrips(ArrayList<Trip> routeTrips) {
			this.routeTrips = routeTrips;
		}

		public Trip getTrip(int position) {
			return this.routeTrips.size() == 0 ? null : this.routeTrips.get(position);
		}

		public void setUserLocation(Location userLocation) {
			this.userLocation = userLocation;
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
			return RTSTripStopsFragment.newInstance(position, this.lastVisibleFragmentPosition, this.authority, this.routeId, getTrip(position).id,
					this.optStopId, this.userLocation, this.showingListInsteadOfMap, this.optRoute);
		}

	}
}
