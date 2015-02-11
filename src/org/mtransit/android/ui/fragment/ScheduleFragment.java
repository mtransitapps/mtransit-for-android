package org.mtransit.android.ui.fragment;

import java.util.Arrays;
import java.util.Calendar;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MainActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

	public static ScheduleFragment newInstance(String uuid, String authority, AgencyProperties optAgency, RouteTripStop optRts) {
		ScheduleFragment f = new ScheduleFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		f.authority = authority;
		f.agency = optAgency;
		args.putString(EXTRA_POI_UUID, uuid);
		f.uuid = uuid;
		f.rts = optRts;
		f.setArguments(args);
		return f;
	}

	private DayPagerAdapter adapter;
	private int lastPageSelected = -1;
	private String uuid;
	private RouteTripStop rts;

	private boolean hasRts() {
		if (this.rts == null) {
			initRtsAsync();
			return false;
		}
		return true;
	}

	private void initRtsAsync() {
		if (this.loadRtsTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.uuid) || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadRtsTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadRtsTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">loadRtsTask";
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initRtsSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewRts();
			}
		}
	};

	private void resetRts() {
		this.rts = null;
	}

	private RouteTripStop getRtsOrNull() {
		if (!hasRts()) {
			return null;
		}
		return this.rts;
	}

	private boolean initRtsSync() {
		if (this.rts != null) {
			return false;
		}
		if (!TextUtils.isEmpty(this.uuid) && !TextUtils.isEmpty(this.authority)) {
			POIManager poim = DataSourceManager.findPOI(getActivity(), this.authority, new POIFilter(Arrays.asList(new String[] { this.uuid })));
			if (poim != null && poim.poi instanceof RouteTripStop) {
				this.rts = (RouteTripStop) poim.poi;
			}
		}
		return this.rts != null;
	}

	private void applyNewRts() {
		if (this.rts == null) {
			return;
		}
		getAbController().setABBgColor(this, getABBgColor(getActivity()), false);
		getAbController().setABSubtitle(this, getABSubtitle(getActivity()), false);
		getAbController().setABReady(this, isABReady(), true);
		if (this.adapter != null) {
			this.adapter.setOptRts(this.rts);
		}
	}

	private String authority;

	private AgencyProperties agency;

	private boolean hasAgency() {
		if (this.agency == null) {
			initAgencyAsync();
			return false;
		}
		return true;
	}

	private void initAgencyAsync() {
		if (this.loadAgencyTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadAgencyTask.execute();
	}

	private MTAsyncTask<Void, Void, Boolean> loadAgencyTask = new MTAsyncTask<Void, Void, Boolean>() {
		@Override
		public String getLogTag() {
			return TAG + ">loadAgencyTask";
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initAgencySync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewAgency();
			}
		}
	};

	private void applyNewAgency() {
		if (this.agency == null) {
			return;
		}
		getAbController().setABSubtitle(this, getABSubtitle(getActivity()), false);
		getAbController().setABReady(this, isABReady(), true);
	}

	private AgencyProperties getAgencyOrNull() {
		if (!hasAgency()) {
			return null;
		}
		return this.agency;
	}

	private boolean initAgencySync() {
		if (this.agency != null) {
			return false;
		}
		if (!TextUtils.isEmpty(this.authority)) {
			this.agency = DataSourceProvider.get(getActivity()).getAgency(getActivity(), this.authority);
		}
		return this.agency != null;
	}

	private void resetAgency() {
		this.agency = null;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_schedule, container, false);
		setupView(view);
		switchView(view);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AUTHORITY, this.authority);
		}
		if (!TextUtils.isEmpty(this.uuid)) {
			outState.putString(EXTRA_POI_UUID, this.uuid);
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
			resetRts();
			resetAgency();
		}
		String newUuid = BundleUtils.getString(EXTRA_POI_UUID, bundles);
		if (!TextUtils.isEmpty(newUuid) && !newUuid.equals(this.uuid)) {
			this.uuid = newUuid;
			resetRts();
		}
		this.adapter.setUuid(this.uuid);
		this.adapter.setAuthority(this.authority);
	}

	private void initAdapters(Activity activity) {
		if (activity == null) {
			return;
		}
		this.adapter = new DayPagerAdapter(this, TimeUtils.getBeginningOfTodayInMs(), null, null, null);
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setOffscreenPageLimit(2);
		viewPager.setOnPageChangeListener(this);
		setupAdapter(view);
	}

	private void setupAdapter(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		viewPager.setAdapter(this.adapter);
		if (this.lastPageSelected < 0) {
			this.lastPageSelected = DayPagerAdapter.STARTING_POSITION;
		}
		viewPager.setCurrentItem(this.lastPageSelected);
		switchView(view);
		onPageSelected(this.lastPageSelected); // tell current page it's selected
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
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(this.lastPageSelected); // resume
				}
			}
		}
	}

	private void pauseAllVisibleAwareChildFragments() {
		java.util.List<Fragment> fragments = getChildFragmentManager().getFragments();
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment instanceof VisibilityAwareFragment) {
					VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
					visibilityAwareFragment.setFragmentVisibleAtPosition(-1); // pause
				}
			}
		}
	}

	@Override
	public void onPageSelected(int position) {
		StatusLoader.get().clearAllTasks();
		ServiceUpdateLoader.get().clearAllTasks();
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
	public void onResume() {
		super.onResume();
		if (this.modulesUpdated) {
			getView().post(new Runnable() {
				@Override
				public void run() {
					if (ScheduleFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		if (!TextUtils.isEmpty(this.uuid) && !TextUtils.isEmpty(this.authority)) {
			FragmentActivity activity = getActivity();
			if (activity == null) {
				return;
			}
			POIFilter poiFilter = new POIFilter(Arrays.asList(new String[] { this.uuid }));
			POIManager newPoim = DataSourceManager.findPOI(activity, this.authority, poiFilter);
			if (newPoim == null || !(newPoim.poi instanceof RouteTripStop)) {
				((MainActivity) activity).popFragmentFromStack(this); // close this fragment
				this.modulesUpdated = false; // processed
				return;
			}
			resetRts();
			setupView(getView());
			this.modulesUpdated = false; // processed
		} else {
			this.modulesUpdated = false; // processed
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
	public boolean isABReady() {
		boolean ready = hasAgency() && hasRts();
		return ready;
	}

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.full_schedule);
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
		RouteTripStop rts = getRtsOrNull();
		AgencyProperties agency = getAgencyOrNull();
		if (agency == null || rts == null) {
			return super.getABSubtitle(context);
		}
		StringBuilder sb = new StringBuilder(agency.getShortName());
		sb.append(" -");
		if (!TextUtils.isEmpty(rts.getRoute().getShortName())) {
			sb.append(" ").append(rts.getRoute().getShortName());
		}
		if (!TextUtils.isEmpty(rts.getRoute().getLongName())) {
			sb.append(" ").append(rts.getRoute().getLongName());
		}
		return sb.toString();
	}

	@Override
	public Integer getABBgColor(Context context) {
		RouteTripStop rts = getRtsOrNull();
		return POIManager.getRouteColor(context, rts == null ? null : rts.getRoute(), this.authority, super.getABBgColor(context));
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
		private String uuid;
		private String authority;
		private RouteTripStop optRts;

		public DayPagerAdapter(ScheduleFragment scheduleFragment, long todayStartsAtInMs, String uuid, String authority, RouteTripStop optRts) {
			super(scheduleFragment.getChildFragmentManager());
			this.uuid = uuid;
			this.authority = authority;
			this.optRts = optRts;
			this.todayStartsAtInMs = todayStartsAtInMs;
			this.todayStartsAtCal = TimeUtils.getNewCalendarInstance(this.todayStartsAtInMs);
		}

		public boolean isInitialized() {
			return !TextUtils.isEmpty(this.uuid) && !TextUtils.isEmpty(this.authority);
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public void setAuthority(String authority) {
			this.authority = authority;
		}

		public void setOptRts(RouteTripStop optRts) {
			this.optRts = optRts;
		}

		public void setLastVisibleFragmentPosition(int lastVisibleFragmentPosition) {
			this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}

		private Calendar getPageDayCal(int position) {
			Calendar pageDay = (Calendar) this.todayStartsAtCal.clone();
			pageDay.add(Calendar.DATE, (position - this.todayPosition));
			return pageDay;
		}

		@Override
		public Fragment getItem(int position) {
			return ScheduleDayFragment.newInstance(this.uuid, this.authority, getPageDayCal(position).getTimeInMillis(), position,
					this.lastVisibleFragmentPosition, this.optRts);
		}

		@Override
		public int getItemPosition(Object object) {
			if (object != null && object instanceof ScheduleDayFragment) {
				ScheduleDayFragment f = (ScheduleDayFragment) object;
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
