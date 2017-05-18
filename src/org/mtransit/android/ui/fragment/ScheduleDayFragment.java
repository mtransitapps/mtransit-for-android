package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.ui.widget.MTBaseAdapter;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.ScheduleTimestampsLoader;
import org.mtransit.android.util.LoaderUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.TextView;

public class ScheduleDayFragment extends MTFragmentV4 implements VisibilityAwareFragment, DataSourceProvider.ModulesUpdateListener,
		LoaderManager.LoaderCallbacks<ArrayList<Schedule.Timestamp>> {

	private static final String TAG = ScheduleDayFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG + "-" + dayStartsAtInMs;
	}

	private static final String EXTRA_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_POI_UUID = "extra_poi_uuid";
	private static final String EXTRA_DAY_START_AT_IN_MS = "extra_day_starts_at_ms";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";
	private static final String EXTRA_SCOLLED_TO_NOW = "extra_scolled_to_now";

	public static ScheduleDayFragment newInstance(String uuid, String authority, long dayStartsAtInMs, int fragmentPosition, int lastVisibleFragmentPosition,
			RouteTripStop optRts) {
		ScheduleDayFragment f = new ScheduleDayFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		f.authority = authority;
		args.putString(EXTRA_POI_UUID, uuid);
		f.uuid = uuid;
		f.rts = optRts;
		args.putLong(EXTRA_DAY_START_AT_IN_MS, dayStartsAtInMs);
		f.dayStartsAtInMs = dayStartsAtInMs;
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
			f.fragmentPosition = fragmentPosition;
		}
		if (lastVisibleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisibleFragmentPosition);
			f.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
		}
		f.setArguments(args);
		return f;
	}

	private TimeAdapter adapter;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private boolean scrolledToNow = false;
	private long dayStartsAtInMs = -1L;
	private Calendar dayStartsAtCal = null;
	private Date dayStartsAtDate;

	private void resetDayStarts() {
		this.dayStartsAtCal = null;
		this.dayStartsAtDate = null;
	}

	private Calendar getDayStartsAtCal() {
		if (this.dayStartsAtCal == null) {
			initDayStartsAtCal();
		}
		return this.dayStartsAtCal;
	}

	private void initDayStartsAtCal() {
		if (this.dayStartsAtInMs > 0L) {
			this.dayStartsAtCal = TimeUtils.getNewCalendar(this.dayStartsAtInMs);
		}
	}

	private Date getDayStartsAtDate() {
		if (this.dayStartsAtDate == null) {
			initDayStartsAtDate();
		}
		return this.dayStartsAtDate;
	}

	private void initDayStartsAtDate() {
		if (getDayStartsAtCal() != null) {
			this.dayStartsAtDate = getDayStartsAtCal().getTime();
		}
	}

	private String authority;

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
		if (this.loadRtsTask != null && this.loadRtsTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		if (TextUtils.isEmpty(this.uuid) || TextUtils.isEmpty(this.authority)) {
			return;
		}
		this.loadRtsTask = new LoadRtsTask();
		TaskUtils.execute(this.loadRtsTask);
	}

	private LoadRtsTask loadRtsTask = null;

	private class LoadRtsTask extends MTAsyncTask<Void, Void, Boolean> {

		@Override
		public String getLogTag() {
			return ScheduleDayFragment.this.getLogTag() + ">" + LoadRtsTask.class.getSimpleName();
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
			POIManager poim = DataSourceManager.findPOI(getActivity(), this.authority, POIProviderContract.Filter.getNewUUIDFilter(this.uuid));
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
		if (this.adapter != null) {
			this.adapter.setRts(this.rts);
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			LoaderUtils.restartLoader(this, SCHEDULE_LOADER, null, this);
		}
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
		View view = inflater.inflate(R.layout.fragment_schedule_day, container, false);
		setupView(view);
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
		if (this.dayStartsAtInMs >= 0L) {
			outState.putLong(EXTRA_DAY_START_AT_IN_MS, this.dayStartsAtInMs);
		}
		if (this.fragmentPosition >= 0) {
			outState.putInt(EXTRA_FRAGMENT_POSITION, this.fragmentPosition);
		}
		if (this.lastVisibleFragmentPosition >= 0) {
			outState.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, this.lastVisibleFragmentPosition);
		}
		outState.putBoolean(EXTRA_SCOLLED_TO_NOW, this.scrolledToNow);
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
		Long newDayStartsAtInMs = BundleUtils.getLong(EXTRA_DAY_START_AT_IN_MS, bundles);
		if (newDayStartsAtInMs != null && !newDayStartsAtInMs.equals(this.dayStartsAtInMs)) {
			this.dayStartsAtInMs = newDayStartsAtInMs;
			resetDayStarts();
		}
		Integer newFragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, bundles);
		if (newFragmentPosition != null) {
			if (newFragmentPosition >= 0) {
				this.fragmentPosition = newFragmentPosition;
			} else {
				this.fragmentPosition = -1;
			}
		}
		Integer newLastVisibleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, bundles);
		if (newLastVisibleFragmentPosition != null) {
			if (newLastVisibleFragmentPosition >= 0) {
				this.lastVisibleFragmentPosition = newLastVisibleFragmentPosition;
			} else {
				this.lastVisibleFragmentPosition = -1;
			}
		}
		Boolean newScrolledToNow = BundleUtils.getBoolean(EXTRA_SCOLLED_TO_NOW, bundles);
		if (newScrolledToNow != null) {
			this.scrolledToNow = newScrolledToNow;
		}
		this.adapter.setDayStartsAt(getDayStartsAtCal());
	}

	public int getFragmentPosition() {
		return fragmentPosition;
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		((TextView) view.findViewById(R.id.dayDate)).setText(getDayDateString());
		setupAdapter(view);
	}

	private static final ThreadSafeDateFormatter DAY_DATE_FORMAT = new ThreadSafeDateFormatter("EEEE, MMM d, yyyy");

	private CharSequence getDayDateString() {
		CharSequence dateS;
		if (TimeUtils.isYesterday(this.dayStartsAtInMs)) {
			dateS = getString(R.string.yesterday) + " " + DAY_DATE_FORMAT.formatThreadSafe(getDayStartsAtDate());
		} else if (TimeUtils.isToday(this.dayStartsAtInMs)) {
			dateS = getString(R.string.today) + " " + DAY_DATE_FORMAT.formatThreadSafe(getDayStartsAtDate());
		} else if (TimeUtils.isTomorrow(this.dayStartsAtInMs)) {
			dateS = getString(R.string.tomorrow) + " " + DAY_DATE_FORMAT.formatThreadSafe(getDayStartsAtDate());
		} else {
			dateS = DAY_DATE_FORMAT.formatThreadSafe(getDayStartsAtDate());
		}
		return dateS;
	}

	private void setupAdapter(View view) {
		if (view == null) {
			return;
		}
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
			((AbsListView) listView).setAdapter(this.adapter);
		}
	}

	@Override
	public void onModulesUpdated() {
	}

	@Override
	public void onPause() {
		super.onPause();
		onFragmentInvisible();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.fragmentPosition >= 0 && this.fragmentPosition == this.lastVisibleFragmentPosition) {
			onFragmentVisible();
		} // ELSE would be call later
		if (this.adapter != null) {
			this.adapter.setActivity(getActivity());
		}
	}

	@Override
	public void setFragmentPosition(int fragmentPosition) {
		this.fragmentPosition = fragmentPosition;
		setFragmentVisibleAtPosition(this.lastVisibleFragmentPosition); // force reset visibility
	}

	@Override
	public void setFragmentVisibleAtPosition(int visibleFragmentPosition) {
		if (this.lastVisibleFragmentPosition == visibleFragmentPosition //
				&& ( //
				(this.fragmentPosition == visibleFragmentPosition && this.fragmentVisible) //
						|| (this.fragmentPosition != visibleFragmentPosition && !this.fragmentVisible))) {
			return;
		}
		this.lastVisibleFragmentPosition = visibleFragmentPosition;
		if (this.fragmentPosition < 0) {
			return;
		}
		if (this.fragmentPosition == visibleFragmentPosition) {
			onFragmentVisible();
		} else {
			onFragmentInvisible();
		}
	}

	private void onFragmentInvisible() {
		if (!this.fragmentVisible) {
			return; // already invisible
		}
		this.fragmentVisible = false;
		if (this.adapter != null) {
			this.adapter.onPause();
		}
	}

	@Override
	public boolean isFragmentVisible() {
		return this.fragmentVisible;
	}

	private void onFragmentVisible() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		if (!isResumed()) {
			return;
		}
		this.fragmentVisible = true;
		switchView(getView());
		if (this.adapter == null || !this.adapter.isInitialized()) {
			if (hasRts()) {
				LoaderUtils.restartLoader(this, SCHEDULE_LOADER, null, this);
			}
		} else {
			this.adapter.onResume(getActivity());
		}
	}

	private static final int SCHEDULE_LOADER = 0;

	@Override
	public Loader<ArrayList<Schedule.Timestamp>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case SCHEDULE_LOADER:
			RouteTripStop rts = getRtsOrNull();
			if (this.dayStartsAtInMs <= 0L || rts == null) {
				return null;
			}
			return new ScheduleTimestampsLoader(getActivity(), rts, this.dayStartsAtInMs);
		default:
			MTLog.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<ArrayList<Schedule.Timestamp>> loader) {
		if (this.adapter != null) {
			this.adapter.clearTimes();
			this.adapter.onPause();
		}
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<Schedule.Timestamp>> loader, ArrayList<Schedule.Timestamp> data) {
		View view = getView();
		if (view == null) {
			return; // too late
		}
		switchView(view);
		this.adapter.setTimes(data);
		if (!this.scrolledToNow) {
			int compareToToday = this.adapter.compareToToday();
			int selectPosition;
			if (compareToToday < 0) { // past
				selectPosition = this.adapter.getCount(); // scroll down
			} else if (compareToToday > 0) { // future
				selectPosition = 0; // scroll up
			} else { // today
				selectPosition = getTodaySelectPosition();
			}
			((AbsListView) view.findViewById(R.id.list)).setSelection(selectPosition);
			this.scrolledToNow = true;
		}
		switchView(view);
	}

	private int getTodaySelectPosition() {
		Schedule.Timestamp nextTime = this.adapter.getNextTimeInMs();
		if (nextTime != null) {
			int nextTimePosition = this.adapter.getPosition(nextTime);
			if (nextTimePosition > 0) {
				nextTimePosition--; // show 1 more time on top of the list
				if (nextTimePosition > 0) {
					nextTimePosition--; // show 1 more time on top of the list
				}
			}
			if (nextTimePosition >= 0) {
				return nextTimePosition;
			}
		}
		return 0;
	}

	private void initAdapters(Activity activity) {
		this.adapter = new TimeAdapter(activity, null, null);
	}

	private void switchView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading(view);
		} else if (this.adapter.getCount() == 0) {
			showEmpty(view);
		} else {
			showList(view);
		}
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
		}
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
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
	public void onDestroy() {
		super.onDestroy();
		TaskUtils.cancelQuietly(this.loadRtsTask, true);
	}

	private static class TimeAdapter extends MTBaseAdapter implements TimeUtils.TimeChangedReceiver.TimeChangedListener {

		private static final String TAG = ScheduleDayFragment.class.getSimpleName() + ">" + TimeAdapter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final int ITEM_VIEW_TYPE_HOUR_SERATORS = 0;

		private static final int ITEM_VIEW_TYPE_TIME = 1;

		private static final int HOUR_SEPARATORS_COUNT = 24;

		private int timesCount = 0;

		private SparseArray<ArrayList<Schedule.Timestamp>> hourToTimes = new SparseArray<ArrayList<Schedule.Timestamp>>();

		private boolean initialized = false;

		private ArrayList<Date> hours = new ArrayList<Date>();

		private LayoutInflater layoutInflater;

		private Calendar dayStartsAt;

		private WeakReference<Activity> activityWR;

		private Schedule.Timestamp nextTimeInMs = null;

		private RouteTripStop optRts;

		private TimeZone deviceTimeZone = TimeZone.getDefault();

		public TimeAdapter(Activity activity, Calendar dayStartsAt, RouteTripStop optRts) {
			super();
			setActivity(activity);
			this.layoutInflater = LayoutInflater.from(activity);
			setDayStartsAt(dayStartsAt);
			setRts(optRts);
		}

		public void setDayStartsAt(Calendar dayStartsAt) {
			this.dayStartsAt = dayStartsAt;
			resetHours();
			if (this.dayStartsAt != null) {
				initHours();
			}
		}

		public void setRts(RouteTripStop optRts) {
			this.optRts = optRts;
		}

		public void onResume(Activity activity) {
			setActivity(activity);
		}

		public void setActivity(Activity activity) {
			this.activityWR = new WeakReference<Activity>(activity);
		}

		public void onPause() {
			disableTimeChangeddReceiver();
		}

		private void initHours() {
			resetHours();
			Calendar cal;
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				cal = (Calendar) this.dayStartsAt.clone();
				cal.set(Calendar.HOUR_OF_DAY, hourOfTheDay);
				this.hours.add(cal.getTime());
				this.hourToTimes.put(hourOfTheDay, new ArrayList<Schedule.Timestamp>());
			}
		}

		private void resetHours() {
			this.hours.clear();
			this.hourToTimes.clear();
		}

		public void clearTimes() {
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				this.hourToTimes.get(hourOfTheDay).clear();
			}
			this.timesCount = 0;
			this.nextTimeInMs = null;
		}

		public void setTimes(ArrayList<Schedule.Timestamp> times) {
			clearTimes();
			for (Schedule.Timestamp time : times) {
				addTime(time);
				if (this.nextTimeInMs == null && time.t >= getNowToTheMinute()) {
					this.nextTimeInMs = time;
					MTLog.d(this, "setTimes() > this.nextTimeInMs: %s", this.nextTimeInMs);
				}
			}
			this.initialized = true;
		}

		public boolean isInitialized() {
			return this.initialized;
		}

		@Override
		public void notifyDataSetChanged() {
			findNextTimeInMs();
			super.notifyDataSetChanged();
		}

		private void findNextTimeInMs() {
			this.nextTimeInMs = null;
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				for (Schedule.Timestamp time : this.hourToTimes.get(hourOfTheDay)) {
					if (this.nextTimeInMs == null && time.t >= getNowToTheMinute()) {
						this.nextTimeInMs = time;
						return;
					}
				}
			}
		}

		public Schedule.Timestamp getNextTimeInMs() {
			if (this.nextTimeInMs == null) {
				findNextTimeInMs();
			}
			return nextTimeInMs;
		}

		private long nowToTheMinute = -1L;

		private boolean timeChangedReceiverEnabled = false;

		private long getNowToTheMinute() {
			if (this.nowToTheMinute < 0) {
				resetNowToTheMinute();
				enableTimeChangedReceiver();
			}
			return this.nowToTheMinute;
		}

		@Override
		public void onTimeChanged() {
			resetNowToTheMinute();
		}

		private void resetNowToTheMinute() {
			this.nowToTheMinute = TimeUtils.currentTimeToTheMinuteMillis();
			notifyDataSetChanged();
		}

		private void enableTimeChangedReceiver() {
			if (!this.timeChangedReceiverEnabled) {
				Activity activity = this.activityWR == null ? null : this.activityWR.get();
				if (activity != null) {
					activity.registerReceiver(this.timeChangedReceiver, TimeUtils.TIME_CHANGED_INTENT_FILTER);
				}
				this.timeChangedReceiverEnabled = true;
			}
		}

		private void disableTimeChangeddReceiver() {
			if (this.timeChangedReceiverEnabled) {
				Activity activity = this.activityWR == null ? null : this.activityWR.get();
				if (activity != null) {
					activity.unregisterReceiver(this.timeChangedReceiver);
				}
				this.timeChangedReceiverEnabled = false;
				this.nowToTheMinute = -1L;
			}
		}

		private final BroadcastReceiver timeChangedReceiver = new TimeUtils.TimeChangedReceiver(this);

		private void addTime(Schedule.Timestamp time) {
			int hourOfTheDay = TimeUtils.getHourOfTheDay(time.t);
			this.hourToTimes.get(hourOfTheDay).add(time);
			this.timesCount++;
		}

		@Override
		public int getCountMT() {
			return this.timesCount + HOUR_SEPARATORS_COUNT;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public Object getItem(int position) {
			return getItemMT(position);
		}

		@Override
		public Object getItemMT(int position) {
			int index = 0;
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				index++; // separator
				if (position >= index && position < index + this.hourToTimes.get(hourOfTheDay).size()) {
					return this.hourToTimes.get(hourOfTheDay).get(position - index);
				}
				index += this.hourToTimes.get(hourOfTheDay).size();

			}
			return null;
		}

		private Calendar todayStartsAt = TimeUtils.getBeginningOfTodayCal();
		private Calendar todayEndsAt = null;

		public int compareToToday() {
			if (this.dayStartsAt.before(this.todayStartsAt)) {
				return -1; // past
			}
			if (this.todayEndsAt == null) {
				this.todayEndsAt = TimeUtils.getBeginningOfTomorrowCal();
				this.todayEndsAt.add(Calendar.MILLISECOND, -1);
			}
			if (this.dayStartsAt.after(this.todayEndsAt)) {
				return +1; // future
			}
			return 0; // today
		}

		public int getPosition(Object item) {
			int index = 0;
			if (item == null || !(item instanceof Schedule.Timestamp)) {
				return index;
			}
			Schedule.Timestamp time = (Schedule.Timestamp) item;
			Date date = new Date(time.t);
			Date thatDate;
			Date nextDate;
			int nextHourOfTheDay;
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				index++; // separator
				if (this.hourToTimes.get(hourOfTheDay).size() > 0) {
					thatDate = this.hours.get(hourOfTheDay);
					if (date.after(thatDate)) {
						nextHourOfTheDay = hourOfTheDay + 1;
						nextDate = nextHourOfTheDay < this.hours.size() ? this.hours.get(nextHourOfTheDay) : null;
						if (nextDate == null || date.before(nextDate)) {
							for (Schedule.Timestamp hourTime : this.hourToTimes.get(hourOfTheDay)) {
								if (time.t == hourTime.t) {
									return index;
								}
								index++; // after
							}
						} else {
							index += this.hourToTimes.get(hourOfTheDay).size(); // after
						}
					} else {
						index += this.hourToTimes.get(hourOfTheDay).size(); // after
					}
				}
			}
			return -1;
		}

		public Integer getItemHourSeparator(int position) {
			int index = 0;
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				if (index == position) {
					return hourOfTheDay;
				}
				index++;
				index += this.hourToTimes.get(hourOfTheDay).size();
			}
			return null;
		}

		@Override
		public int getItemViewType(int position) {
			Object item = getItem(position);
			if (item != null && item instanceof Schedule.Timestamp) {
				return ITEM_VIEW_TYPE_TIME;
			}
			return ITEM_VIEW_TYPE_HOUR_SERATORS;
		}

		@Override
		public long getItemIdMT(int position) {
			return position;
		}

		@Override
		public View getViewMT(int position, View convertView, ViewGroup parent) {
			switch (getItemViewType(position)) {
			case ITEM_VIEW_TYPE_HOUR_SERATORS:
				return getHourSeparatorView(position, convertView, parent);
			case ITEM_VIEW_TYPE_TIME:
				return getTimeView(position, convertView, parent);
			default:
				MTLog.w(this, "Unexpected view type at position '%s'!", position);
				return null;
			}
		}

		private View getTimeView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_detail_status_schedule_time, parent, false);
				TimeViewHolder holder = new TimeViewHolder();
				holder.timeTv = (TextView) convertView.findViewById(R.id.time);
				convertView.setTag(holder);
			}
			updateTimeView(position, convertView);
			return convertView;
		}

		private static final String P2 = ")";
		private static final String P1 = " (";

		private void updateTimeView(int position, View convertView) {
			TimeViewHolder holder = (TimeViewHolder) convertView.getTag();
			Schedule.Timestamp timestamp = (Schedule.Timestamp) getItem(position);
			Context context = this.activityWR == null ? null : this.activityWR.get();
			if (timestamp != null && context != null) {
				String userTime = TimeUtils.formatTime(context, timestamp.t);
				SpannableStringBuilder timeSb = new SpannableStringBuilder(userTime);
				TimeZone timestampTZ = TimeZone.getTimeZone(timestamp.getLocalTimeZone());
				if (timestamp.hasLocalTimeZone() && !this.deviceTimeZone.equals(timestampTZ)) {
					String localTime = TimeUtils.formatTime(context, timestamp.t, timestampTZ);
					if (!localTime.equalsIgnoreCase(userTime)) {
						timeSb.append(P1).append(context.getString(R.string.local_time_and_time, localTime)).append(P2);
					}
				}
				String timeOnly = timeSb.toString();
				if (timestamp.hasHeadsign()) {
					String timestampHeading = timestamp.getHeading(context);
					String tripHeading = this.optRts == null ? null : this.optRts.getTrip().getHeading(context);
					if (!Trip.isSameHeadsign(timestampHeading, tripHeading)) {
						timeSb.append(P1).append(timestampHeading).append(P2);
					}
				}
				TimeUtils.cleanTimes(timeOnly, timeSb);
				if (this.nextTimeInMs != null  //
						&& TimeUtils.isSameDay(getNowToTheMinute(), this.nextTimeInMs.t) //
						&& this.nextTimeInMs.t == timestamp.t) { // now
					SpanUtils.setAll(timeSb, getScheduleListTimesNowTextColor(context), SCHEDULE_LIST_TIMES_NOW_STYLE);
				} else if (timestamp.t < getNowToTheMinute()) { // past
					SpanUtils.setAll(timeSb, getScheduleListTimesPastTextColor(context), SCHEDULE_LIST_TIMES_PAST_STYLE);
				} else { // future
					SpanUtils.setAll(timeSb, getScheduleListTimesFutureTextColor(context), SCHEDULE_LIST_TIMES_FUTURE_STYLE);
				}
				holder.timeTv.setText(timeSb);
			} else {
				holder.timeTv.setText(null);
			}
		}

		private static final StyleSpan SCHEDULE_LIST_TIMES_PAST_STYLE = SpanUtils.getNewNormalStyleSpan();

		private static final StyleSpan SCHEDULE_LIST_TIMES_NOW_STYLE = SpanUtils.getNewBoldStyleSpan();

		private static final StyleSpan SCHEDULE_LIST_TIMES_FUTURE_STYLE = SpanUtils.getNewNormalStyleSpan();

		private static ForegroundColorSpan scheduleListTimesPastTextColor = null;

		private static ForegroundColorSpan getScheduleListTimesPastTextColor(Context context) {
			if (scheduleListTimesPastTextColor == null) {
				scheduleListTimesPastTextColor = SpanUtils.getNewTextColor(Schedule.getDefaultPastTextColor(context));
			}
			return scheduleListTimesPastTextColor;
		}

		private static ForegroundColorSpan scheduleListTimesNowTextColor = null;

		private static ForegroundColorSpan getScheduleListTimesNowTextColor(Context context) {
			if (scheduleListTimesNowTextColor == null) {
				scheduleListTimesNowTextColor = SpanUtils.getNewTextColor(Schedule.getDefaultNowTextColor(context));
			}
			return scheduleListTimesNowTextColor;
		}

		private static ForegroundColorSpan scheduleListTimesFutureTextColor = null;

		private static ForegroundColorSpan getScheduleListTimesFutureTextColor(Context context) {
			if (scheduleListTimesFutureTextColor == null) {
				scheduleListTimesFutureTextColor = SpanUtils.getNewTextColor(Schedule.getDefaultFutureTextColor(context));
			}
			return scheduleListTimesFutureTextColor;
		}

		private View getHourSeparatorView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_detail_status_schedule_hour_separator, parent, false);
				HourSperatorViewHolder holder = new HourSperatorViewHolder();
				holder.hourTv = (TextView) convertView.findViewById(R.id.hour);
				convertView.setTag(holder);
			}
			updateHourSeparatorView(position, convertView);
			return convertView;
		}

		private void updateHourSeparatorView(int position, View convertView) {
			HourSperatorViewHolder holder = (HourSperatorViewHolder) convertView.getTag();
			Integer hourOfTheDay = getItemHourSeparator(position);
			Context context = this.activityWR == null ? null : this.activityWR.get();
			if (hourOfTheDay != null && context != null) {
				holder.hourTv.setText(getHourFormatter(context).formatThreadSafe(this.hours.get(hourOfTheDay)));
			} else {
				holder.hourTv.setText(null);
			}
		}

		private static ThreadSafeDateFormatter HOUR_FORMATTER;

		private static ThreadSafeDateFormatter getHourFormatter(Context context) {
			if (HOUR_FORMATTER == null) {
				HOUR_FORMATTER = TimeUtils.getNewHourFormat(context);
			}
			return HOUR_FORMATTER;
		}

		public static class TimeViewHolder {
			TextView timeTv;
		}

		public static class HourSperatorViewHolder {
			TextView hourTv;
		}
	}
}
