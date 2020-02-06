package org.mtransit.android.ui.fragment;

import java.util.Calendar;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.FragmentAsyncTaskV4;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MainActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
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

	@NonNull
	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	private static final String EXTRA_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_POI_UUID = "extra_poi_uuid";
	private static final String EXTRA_COLOR_INT = "extra_color_int";

	public static ScheduleFragment newInstance(String uuid, String authority, RouteTripStop optRts, Integer optColorInt) {
		ScheduleFragment f = new ScheduleFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		f.authority = authority;
		args.putString(EXTRA_POI_UUID, uuid);
		f.uuid = uuid;
		f.rts = optRts;
		if (optColorInt != null) {
			args.putInt(EXTRA_COLOR_INT, optColorInt);
			f.colorInt = optColorInt;
		}
		f.setArguments(args);
		return f;
	}

	private DayPagerAdapter adapter;
	private int lastPageSelected = -1;
	private String uuid;
	private RouteTripStop rts;
	@ColorInt
	private Integer colorInt;

	private boolean hasRts() {
		if (this.rts == null) {
			initRtsAsync();
			return false;
		}
		return true;
	}

	private void initRtsAsync() {
		if (this.loadRtsTask != null && this.loadRtsTask.getStatus() == LoadRtsTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.uuid) || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadRtsTask = new LoadRtsTask(this);
		TaskUtils.execute(this.loadRtsTask);
	}

	private LoadRtsTask loadRtsTask = null;

	private static class LoadRtsTask extends FragmentAsyncTaskV4<Void, Void, Boolean, ScheduleFragment> {

		@Override
		public String getLogTag() {
			return ScheduleFragment.class.getSimpleName() + ">" + LoadRtsTask.class.getSimpleName();
		}

		public LoadRtsTask(ScheduleFragment scheduleFragment) {
			super(scheduleFragment);
		}

		@Override
		protected Boolean doInBackgroundWithFragment(@NonNull ScheduleFragment scheduleFragment, Void... params) {
			return scheduleFragment.initRtsSync();
		}

		@Override
		protected void onPostExecuteFragmentReady(@NonNull ScheduleFragment scheduleFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				scheduleFragment.applyNewRts();
			}
		}
	}

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
			POIManager poim = DataSourceManager.findPOI(getContext(), this.authority, POIProviderContract.Filter.getNewUUIDFilter(this.uuid));
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
		getAbController().setABBgColor(this, getABBgColor(getContext()), false);
		getAbController().setABSubtitle(this, getABSubtitle(getContext()), false);
		getAbController().setABReady(this, isABReady(), true);
		if (this.adapter != null) {
			this.adapter.setOptRts(this.rts);
		}
	}

	private String authority;


	@Override
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_schedule, container, false);
		setupView(view);
		switchView(view);
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AUTHORITY, this.authority);
		}
		if (!TextUtils.isEmpty(this.uuid)) {
			outState.putString(EXTRA_POI_UUID, this.uuid);
		}
		if (this.colorInt != null) {
			outState.putInt(EXTRA_COLOR_INT, this.colorInt);
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AUTHORITY, bundles);
		if (!TextUtils.isEmpty(newAuthority) && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
			resetRts();
		}
		String newUuid = BundleUtils.getString(EXTRA_POI_UUID, bundles);
		if (!TextUtils.isEmpty(newUuid) && !newUuid.equals(this.uuid)) {
			this.uuid = newUuid;
			resetRts();
		}
		Integer newColorInt = BundleUtils.getInt(EXTRA_COLOR_INT, bundles);
		if (newColorInt != null) {
			this.colorInt = newColorInt;
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

	@SuppressWarnings("deprecation")
	private void setupView(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = view.findViewById(R.id.viewpager);
		viewPager.setOffscreenPageLimit(2);
		viewPager.setOnPageChangeListener(this); // TODO upgrade to #ViewPager#addOnPageChangeListener()
		setupAdapter(view);
	}

	private void setupAdapter(View view) {
		if (view == null) {
			return;
		}
		ViewPager viewPager = view.findViewById(R.id.viewpager);
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
			setFragmentVisibleAtPosition(this.lastPageSelected); // resume
			break;
		case ViewPager.SCROLL_STATE_DRAGGING:
			setFragmentVisibleAtPosition(-1); // pause
			break;
		}
	}

	@Override
	public void onPageSelected(int position) {
		StatusLoader.get().clearAllTasks();
		ServiceUpdateLoader.get().clearAllTasks();
		setFragmentVisibleAtPosition(position);
		this.lastPageSelected = position;
		if (this.adapter != null) {
			this.adapter.setLastVisibleFragmentPosition(this.lastPageSelected);
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
	public void onResume() {
		super.onResume();
		View view = getView();
		if (this.modulesUpdated) {
			if (view != null) {
				view.post(() -> {
					if (ScheduleFragment.this.modulesUpdated) {
						onModulesUpdated();
					}
				});
			}
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
			POIProviderContract.Filter poiFilter = POIProviderContract.Filter.getNewUUIDFilter(this.uuid);
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
		return hasRts();
	}

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.full_schedule);
	}

	@Override
	public CharSequence getABSubtitle(Context context) {
		RouteTripStop rts = getRtsOrNull();
		return POIManager.getOneLineDescription(getContext(), rts);
	}

	@ColorInt
	@Override
	public Integer getABBgColor(Context context) {
		if (this.colorInt != null) {
			return this.colorInt;
		}
		return super.getABBgColor(context);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		TaskUtils.cancelQuietly(this.loadRtsTask, true);
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
		@SuppressWarnings("FieldCanBeLocal")
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
			return ScheduleDayFragment.newInstance( //
					this.uuid, this.authority, getPageDayCal(position).getTimeInMillis(), position, this.lastVisibleFragmentPosition, this.optRts);
		}

		@Override
		public int getItemPosition(@NonNull Object object) {
			if (object instanceof ScheduleDayFragment) {
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
