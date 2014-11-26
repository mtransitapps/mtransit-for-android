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
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Stop;
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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
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
		if (this.route != null) {
			return TRACKING_SCREEN_NAME + "/" + this.route.id;
		}
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_AUTHORITY = "extra_authority";

	private static final String EXTRA_ROUTE_ID = "extra_route_id";

	private static final String EXTRA_TRIP_ID = "extra_trip_id";

	private static final String EXTRA_STOP_ID = "extra_stop_id";

	public static RTSRouteFragment newInstance(RouteTripStop rts) {
		return newInstance(rts.getAuthority(), rts.route, rts.trip, rts.stop);
	}

	public static RTSRouteFragment newInstance(String authority, Route route, Trip optTrip, Stop optStop) {
		final RTSRouteFragment f = new RTSRouteFragment();
		final Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		args.putInt(EXTRA_ROUTE_ID, route.id);
		if (optTrip != null) {
			args.putInt(EXTRA_TRIP_ID, optTrip.id);
		}
		if (optStop != null) {
			args.putInt(EXTRA_STOP_ID, optStop.id);
		}
		f.setArguments(args);
		return f;
	}

	private int lastPageSelected = -1;
	private Location userLocation;
	private RouteTripPagerAdapter adapter;
	private String authority;
	private Route route;
	private Integer routeId;
	private Integer tripId;
	private Integer stopId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_rts_route, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		switchView(getView());
		if (this.adapter == null) {
			initTabsAndViewPager(getView());
		}
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		this.routeId = BundleUtils.getInt(EXTRA_ROUTE_ID, savedInstanceState, getArguments());
		this.authority = BundleUtils.getString(EXTRA_AUTHORITY, savedInstanceState, getArguments());
		this.tripId = BundleUtils.getInt(EXTRA_TRIP_ID, savedInstanceState, getArguments());
		this.stopId = BundleUtils.getInt(EXTRA_STOP_ID, savedInstanceState, getArguments());
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
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
		if (this.route != null && !TextUtils.isEmpty(this.authority)) {
			final Route newRoute = DataSourceManager.findRTSRoute(getActivity(), UriUtils.newContentUri(this.authority), this.route.id);
			if (newRoute == null) {
				((MainActivity) getActivity()).popFragmentFromStack(this); // close this fragment
				return;
			}
			if (this.route != null && this.route.equals(newRoute)) {
				return;
			}
			this.adapter = null; // reset
			this.lastPageSelected = -1; // reset tab position
			initTabsAndViewPager(getView());
		}

	}

	private void initTabsAndViewPager(final View view) {
		if (TextUtils.isEmpty(this.authority) || this.routeId == null || view == null) {
			return;
		}
		final Uri authorityUri = UriUtils.newContentUri(this.authority);
		this.route = DataSourceManager.findRTSRoute(getActivity(), authorityUri, this.routeId);
		getAbController().setABBgColor(this, getABBgColor(getActivity()), false);
		getAbController().setABCustomView(this, getABCustomView(), false);
		getAbController().setABReady(this, isABReady(), true);
		final ArrayList<Trip> routeTrips = DataSourceManager.findRTSRouteTrips(getActivity(), authorityUri, this.routeId);
		if (routeTrips == null) {
			return;
		}
		if (this.adapter == null) {
			this.adapter = new RouteTripPagerAdapter(this, routeTrips, this.authority, this.stopId);
		} else {
			this.adapter.setRouteTrips(routeTrips);
			this.adapter.notifyDataSetChanged();
		}
		setupView(view);
		this.lastPageSelected = 0;
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
				if (RTSRouteFragment.this.lastPageSelected != 0) {
					return; // user has manually move to another page before, too late
				}
				if (lastPageSelected != null) {
					RTSRouteFragment.this.lastPageSelected = lastPageSelected.intValue();
					final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
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
		if (view == null || this.route == null) {
			return;
		}
		SlidingTabLayout tabs = (SlidingTabLayout) view.findViewById(R.id.tabs);
		tabs.setBackgroundColor(getABBgColor(getActivity()));
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			this.userLocation = newLocation;
			final java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
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
			this.tripId = this.adapter.getRouteTrip(position).getId();
			PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(this.authority, this.routeId), this.tripId, false);
		}
		final java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
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
						final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
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
						final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
						visibilityAwareFragment.setFragmentVisibleAtPosition(-1); // pause
					}
				}
			}
			break;
		}
	}

	@Override
	public boolean isABReady() {
		return this.route != null;
	}



	@Override
	public CharSequence getABTitle(Context context) {
		if (this.route == null) {
			return super.getABTitle(context);
		}
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		int startShortName = 0, endShortName = 0;
		if (!TextUtils.isEmpty(this.route.shortName)) {
			startShortName = ssb.length();
			ssb.append(this.route.shortName);
			endShortName = ssb.length();
		}
		int startLongName = 0, endLongName = 0;
		if (!TextUtils.isEmpty(this.route.longName)) {
			if (ssb.length() > 0) {
				ssb.append(StringUtils.SPACE_CAR).append(StringUtils.SPACE_CAR);
			}
			ssb.append(this.route.longName);
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
		if (this.route == null) {
			return null; // not ready
		}
		return this.route.getColorInt();
	}

	private static class RouteTripPagerAdapter extends FragmentStatePagerAdapter {

		private ArrayList<Trip> routeTrips;
		private WeakReference<Context> contextWR;
		private Location userLocation;
		private int lastVisibleFragmentPosition = -1;
		private String authority;
		private Integer optStopId = null;

		public RouteTripPagerAdapter(RTSRouteFragment fragment, ArrayList<Trip> routeTrips, String authority, Integer optStopId) {
			super(fragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(fragment.getActivity());
			this.routeTrips = routeTrips;
			this.authority = authority;
			this.optStopId = optStopId;
		}

		public void setRouteTrips(ArrayList<Trip> routeTrips) {
			this.routeTrips = routeTrips;
		}

		public Trip getRouteTrip(int position) {
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
			final Context context = this.contextWR == null ? null : this.contextWR.get();
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
			final Trip trip = getRouteTrip(position);
			return RTSTripStopsFragment.newInstance(position, this.lastVisibleFragmentPosition, this.authority, trip, this.optStopId, this.userLocation);
		}

	}
}
