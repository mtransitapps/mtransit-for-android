package org.mtransit.android.ui.fragment;

import java.util.Calendar;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MainActivity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

public class ScheduleFragment extends ABFragment implements ViewPager.OnPageChangeListener {

	private static final String TAG = ScheduleFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Schedule";

	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_POI_UUID = "extra_poi_uuid";

	public static ScheduleFragment newInstance(RouteTripStop rts) {
		ScheduleFragment f = new ScheduleFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, rts.getAuthority());
		args.putString(EXTRA_POI_UUID, rts.getUUID());
		f.setArguments(args);
		return f;
	}

	private DayPagerAdapter adapter;
	private int lastPageSelected = -1;
	private RouteTripStop rts;
	private AgencyProperties agency;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_schedule, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (this.rts != null && this.rts != null) {
			outState.putString(EXTRA_POI_UUID, this.rts.getUUID());
			outState.putString(EXTRA_AUTHORITY, this.rts.getAuthority());
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		final String authority = BundleUtils.getString(EXTRA_AUTHORITY, savedInstanceState, getArguments());
		final String uuid = BundleUtils.getString(EXTRA_POI_UUID, savedInstanceState, getArguments());
		if (!TextUtils.isEmpty(authority) && !TextUtils.isEmpty(uuid)) {
			final POIManager poim = DataSourceProvider.findPOIWithUUID(getActivity(), UriUtils.newContentUri(authority), uuid);
			if (poim != null && poim.poi instanceof RouteTripStop) {
				this.rts = (RouteTripStop) poim.poi;
			}
			this.agency = DataSourceProvider.get().getAgency(getActivity(), authority);
		}
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

	private void initTabsAndViewPager(final View view) {
		if (view == null) {
			return;
		}
		if (this.adapter == null) {
			this.adapter = new DayPagerAdapter(this, this.rts, TimeUtils.getBeginningOfTodayInMs());
		}
		setupView(view);
		if (this.lastPageSelected < 0) {
			this.lastPageSelected = DayPagerAdapter.STARTING_POSITION;
		}
		final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setCurrentItem(this.lastPageSelected);
		switchView(view);
		onPageSelected(this.lastPageSelected);
	}

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		final ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		viewPager.setOffscreenPageLimit(2);
		viewPager.setOnPageChangeListener(this);
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

	private void resumeAllVisibleAwareChildFragment() {
		List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(this.lastPageSelected); // resume
				}
			}
		}
	}

	private void pauseAllVisibleAwareChildFragments() {
		List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					final VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(-1); // pause
				}
			}
		}
	}

	@Override
	public void onPageSelected(int position) {
		StatusLoader.get().clearAllTasks();
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
	public void onModulesUpdated() {
		if (this.rts != null) {
			final POIManager newPoim = DataSourceProvider.findPOIWithUUID(getActivity(), UriUtils.newContentUri(this.rts.getAuthority()), this.rts.getUUID());
			if (newPoim == null) {
				((MainActivity) getActivity()).popFragmentFromStack(this); // close this fragment
				return;
			}
			if (!this.rts.equals(newPoim.poi)) {
				this.rts = (RouteTripStop) newPoim.poi;
				setupView(getView());
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.adapter != null) {
			this.adapter = null;
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
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.full_schedule);
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
		if (this.rts == null || this.agency == null) {
			return super.getABSubtitle(context);
		}
		return this.agency.getShortName() + " - " + this.rts.route.shortName + " - " + this.rts.getName();
	}

	@Override
	public int getABIconDrawableResId() {
		return R.drawable.ic_action_action_schedule_holo_light;
	}

	private static class DayPagerAdapter extends FragmentStatePagerAdapter implements MTLog.Loggable {

		private static final String TAG = ScheduleFragment.class.getSimpleName() + ">" + DayPagerAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static int BEFORE_TODAY = 100;

		private static int COUNT = BEFORE_TODAY + 365; // should be enough
		private static final int STARTING_POSITION = BEFORE_TODAY > 0 ? BEFORE_TODAY : COUNT / 2;

		private int todayPosition = STARTING_POSITION;
		private long todayStartsAtInMs;
		private Calendar todayStartsAtCal;
		private int lastVisibleFragmentPosition = -1;
		private RouteTripStop rts;

		public DayPagerAdapter(ScheduleFragment scheduleFragment, RouteTripStop rts, long todayStartsAtInMs) {
			super(scheduleFragment.getChildFragmentManager());
			this.rts = scheduleFragment.rts;
			this.todayStartsAtInMs = todayStartsAtInMs;
			this.todayStartsAtCal = TimeUtils.getNewCalendarInstance(this.todayStartsAtInMs);
		}

		public void setLastVisibleFragmentPosition(int lastVisibleFragmentPosition) {
			this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}

		private Calendar getPageDayCal(int position) {
			Calendar pageDay = (Calendar) this.todayStartsAtCal.clone();
			pageDay.add(Calendar.DATE, (position - todayPosition));
			return pageDay;
		}

		@Override
		public Fragment getItem(int position) {
			return ScheduleDayFragment.newInstance(this.rts, getPageDayCal(position).getTimeInMillis(), position, this.lastVisibleFragmentPosition);
		}

		@Override
		public int getItemPosition(Object object) {
			if (object != null && object instanceof ScheduleDayFragment) {
				final ScheduleDayFragment f = (ScheduleDayFragment) object;
				return f.getFragmentPosition();
			}
			return POSITION_NONE;
		}

		@Override
		public int getCount() {
			return COUNT;
		}

	}

}
