package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.JPaths;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.view.MTJPathsView;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import com.viewpagerindicator.TitlePageIndicator;

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_rts_route, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
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
		final List<Trip> routeTrips = DataSourceManager.findRTSRouteTrips(getActivity(), authorityUri, this.routeId);
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
		final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		TitlePageIndicator tabs = (TitlePageIndicator) view.findViewById(R.id.tabs);
		tabs.setViewPager(viewPager);
		tabs.setOnPageChangeListener(this);
		setupTabTheme(view);
	}

	private void setupTabTheme(View view) {
		if (view == null || this.route == null) {
			return;
		}
		TitlePageIndicator tabs = (TitlePageIndicator) view.findViewById(R.id.tabs);
		final int bgColor = ColorUtils.parseColor(this.route.textColor);
		final int textColor = ColorUtils.parseColor(this.route.color);
		tabs.setBackgroundColor(bgColor);
		final int notSelectedTextColor;
		if (bgColor == Color.BLACK) {
			notSelectedTextColor = Color.WHITE;
		} else if (bgColor == Color.WHITE) {
			notSelectedTextColor = Color.BLACK;
		} else {
			notSelectedTextColor = textColor;
		}
		tabs.setTextColor(notSelectedTextColor);
		tabs.setFooterColor(textColor);
		tabs.setSelectedColor(textColor);
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			this.userLocation = newLocation;
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
		if (this.adapter != null) {
			PreferenceUtils.savePrefLcl(getActivity(), PreferenceUtils.getPREFS_LCL_RTS_ROUTE_TRIP_ID_TAB(this.authority, this.routeId), this.adapter
					.getRouteTrip(position).getId(), false);
		}
		final List<Fragment> fragments = getChildFragmentManager().getFragments();
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
			List<Fragment> fragments = getChildFragmentManager().getFragments();
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
			List<Fragment> fragments2 = getChildFragmentManager().getFragments();
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
	public boolean isABThemeDarkInsteadOfThemeLight() {
		if (this.route == null) {
			return super.isABThemeDarkInsteadOfThemeLight();
		}
		return Color.WHITE == ColorUtils.parseColor(this.route.textColor);
	}

	@Override
	public boolean isABDisplayHomeAsUpEnabled() {
		return false; // included in the custom view
	}

	private View customView;

	@Override
	public View getABCustomView() {
		if (getActivity() == null) {
			return null;
		}
		return getRouteView(getActivity(), this.customView, null);
	}

	public static class RouteViewHolder {
		ImageView upImg;
		TextView routeShortNameTv;
		View routeFL;
		MTJPathsView routeTypeImg;
		TextView routeLongNameTv;
	}

	private View getRouteView(Context context, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.layout_rts_route_ab_view, parent, false);
			RouteViewHolder holder = new RouteViewHolder();
			holder.routeFL = convertView.findViewById(R.id.route);
			holder.upImg = (ImageView) convertView.findViewById(R.id.up);
			holder.routeShortNameTv = (TextView) convertView.findViewById(R.id.route_short_name);
			holder.routeTypeImg = (MTJPathsView) convertView.findViewById(R.id.route_type_img);
			holder.routeLongNameTv = (TextView) convertView.findViewById(R.id.route_long_name);
			convertView.setTag(holder);
		}
		updateRouteView(context, convertView);
		return convertView;
	}

	private View updateRouteView(Context context, View convertView) {
		if (convertView == null) {
			return convertView;
		}
		RouteViewHolder holder = (RouteViewHolder) convertView.getTag();
		if (route == null) {
			holder.routeFL.setVisibility(View.GONE);
		} else {
			final int routeTextColor = ColorUtils.parseColor(route.textColor);
			if (Color.WHITE == routeTextColor) {
				holder.upImg.setImageResource(R.drawable.platform_ic_ab_back_holo_dark_am);
			} else if (Color.BLACK == routeTextColor) {
				holder.upImg.setImageResource(R.drawable.platform_ic_ab_back_holo_light_am);
			} else {
				holder.upImg.setImageResource(android.R.attr.homeAsUpIndicator);
			}
			if (TextUtils.isEmpty(route.shortName)) {
				holder.routeShortNameTv.setVisibility(View.GONE);
				final JPaths rtsRouteLogo = DataSourceProvider.get(context).getRTSRouteLogo(this.authority);
				if (rtsRouteLogo != null) {
					holder.routeTypeImg.setJSON(rtsRouteLogo);
					holder.routeTypeImg.setColor(routeTextColor);
					holder.routeTypeImg.setVisibility(View.VISIBLE);
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
				}
			} else {
				holder.routeTypeImg.setVisibility(View.GONE);
				holder.routeShortNameTv.setText(route.shortName);
				holder.routeShortNameTv.setTextColor(routeTextColor);
				holder.routeShortNameTv.setVisibility(View.VISIBLE);
			}
			if (holder.routeLongNameTv != null) {
				holder.routeLongNameTv.setTextColor(routeTextColor);
				if (TextUtils.isEmpty(route.longName)) {
					holder.routeLongNameTv.setVisibility(View.GONE);
				} else {
					holder.routeLongNameTv.setText(route.longName);
					holder.routeLongNameTv.setVisibility(View.VISIBLE);
				}
			}
			holder.routeFL.setVisibility(View.VISIBLE);
		}
		return convertView;
	}


	@Override
	public Integer getABBgColor() {
		if (this.route == null) {
			return super.getABBgColor();
		}
		return ColorUtils.parseColor(this.route.color);
	}

	private static class RouteTripPagerAdapter extends FragmentStatePagerAdapter {

		private List<Trip> routeTrips;
		private WeakReference<Context> contextWR;
		private Location userLocation;
		private int lastVisibleFragmentPosition = -1;
		private String authority;
		private Integer optStopId = null;

		public RouteTripPagerAdapter(RTSRouteFragment fragment, List<Trip> routeTrips, String authority, Integer optStopId) {
			super(fragment.getChildFragmentManager());
			this.contextWR = new WeakReference<Context>(fragment.getActivity());
			this.routeTrips = routeTrips;
			this.authority = authority;
			this.optStopId = optStopId;
		}

		public void setRouteTrips(List<Trip> routeTrips) {
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
