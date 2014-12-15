package org.mtransit.android.ui.fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.LoaderUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.commons.ui.widget.MTBaseAdapter;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.ScheduleTimestampsLoader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
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
	private long dayStartsAtInMs = -1l;
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
		if (this.dayStartsAtInMs > 0l) {
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
		if (this.adapter == null) {
			LoaderUtils.restartLoader(getLoaderManager(), SCHEDULE_LOADER, null, this);
		} else {
			this.adapter.setRts(this.rts);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		restoreInstanceState(savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_schedule_day, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (!TextUtils.isEmpty(this.authority)) {
			outState.putString(EXTRA_AUTHORITY, this.authority);
		}
		if (!TextUtils.isEmpty(this.uuid)) {
			outState.putString(EXTRA_POI_UUID, this.uuid);
		}
		if (this.dayStartsAtInMs >= 0l) {
			outState.putLong(EXTRA_DAY_START_AT_IN_MS, this.dayStartsAtInMs);
		}
		if (this.fragmentPosition >= 0) {
			outState.putInt(EXTRA_FRAGMENT_POSITION, this.fragmentPosition);
		}
		if (this.lastVisibleFragmentPosition >= 0) {
			outState.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, this.lastVisibleFragmentPosition);
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
		Long newDayStartsAtInMs = BundleUtils.getLong(EXTRA_DAY_START_AT_IN_MS, bundles);
		if (newDayStartsAtInMs != null && !newDayStartsAtInMs.equals(this.dayStartsAtInMs)) {
			this.dayStartsAtInMs = newDayStartsAtInMs;
			resetDayStarts();
		}
		Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, bundles);
		if (fragmentPosition != null) {
			if (fragmentPosition >= 0) {
				this.fragmentPosition = fragmentPosition;
			} else {
				this.fragmentPosition = -1;
			}
		}
		Integer lastVisibleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, bundles);
		if (lastVisibleFragmentPosition != null) {
			if (lastVisibleFragmentPosition >= 0) {
				this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
			} else {
				this.lastVisibleFragmentPosition = -1;
			}
		}
	}

	public int getFragmentPosition() {
		return fragmentPosition;
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		((TextView) view.findViewById(R.id.dayDate)).setText(getDayDateString());
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
		if (view == null || this.adapter == null) {
			return;
		}
		inflateList(view);
		((AbsListView) view.findViewById(R.id.list)).setAdapter(this.adapter);
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
		if (this.fragmentPosition < 0 || this.fragmentPosition == this.lastVisibleFragmentPosition) {
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
				&& (//
				(this.fragmentPosition == visibleFragmentPosition && this.fragmentVisible) //
				|| //
				(this.fragmentPosition != visibleFragmentPosition && !this.fragmentVisible) //
				) //
		) {
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
		this.fragmentVisible = true;
		switchView(getView());
		if (this.adapter == null) {
			if (hasRts()) {
				LoaderUtils.restartLoader(getLoaderManager(), SCHEDULE_LOADER, null, this);
			}
		} else {
			if (this.adapter.getCount() > 0) {
				this.adapter.onResume(getActivity());
			}
		}
	}

	private static final int SCHEDULE_LOADER = 0;

	@Override
	public Loader<ArrayList<Schedule.Timestamp>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case SCHEDULE_LOADER:
			RouteTripStop rts = getRtsOrNull();
			if (this.dayStartsAtInMs <= 0l || rts == null) {
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
		switchView(getView());
		if (this.adapter == null) {
			initAdapter();
		}
		this.adapter.setTimes(data);
		View view = getView();
		if (view != null) {
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

	private void initAdapter() {
		this.adapter = new TimeAdapter(getActivity(), getDayStartsAtCal(), getRtsOrNull());
		View view = getView();
		setupAdapter(view);
	}

	private void switchView(View view) {
		if (this.adapter == null) {
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
		if (view.findViewById(R.id.loading) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.loading_stub)).inflate(); // inflate
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
			this.dayStartsAt = dayStartsAt;
			this.optRts = optRts;
			initHours();
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
			this.hours.clear();
			this.hourToTimes.clear();
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				Calendar cal = (Calendar) dayStartsAt.clone();
				cal.set(Calendar.HOUR_OF_DAY, hourOfTheDay);
				this.hours.add(cal.getTime());
				this.hourToTimes.put(hourOfTheDay, new ArrayList<Schedule.Timestamp>());
			}
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

		private long nowToTheMinute = -1l;

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
				this.nowToTheMinute = -1l;
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

		public int compareToToday() {
			Calendar todayStartsAt = TimeUtils.getBeginningOfTodayCal();
			if (this.dayStartsAt.before(todayStartsAt)) {
				return -1; // past
			}
			Calendar todayEndsAt = TimeUtils.getBeginningOfTomorrowCal();
			todayEndsAt.add(Calendar.MILLISECOND, -1);
			if (this.dayStartsAt.after(todayEndsAt)) {
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
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				index++; // separator
				if (this.hourToTimes.get(hourOfTheDay).size() > 0) {
					Date thatDate = this.hours.get(hourOfTheDay);
					if (date.after(thatDate)) {
						int nextHourOfTheDay = hourOfTheDay + 1;
						Date nextDate = nextHourOfTheDay < this.hours.size() ? this.hours.get(nextHourOfTheDay) : null;
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


		private void updateTimeView(int position, View convertView) {
			TimeViewHolder holder = (TimeViewHolder) convertView.getTag();
			Schedule.Timestamp timestamp = (Schedule.Timestamp) getItem(position);
			Context context = this.activityWR == null ? null : this.activityWR.get();
			if (timestamp != null && context != null) {
				StringBuilder timeSb = new StringBuilder(TimeUtils.formatTime(context, timestamp.t));
				if (timestamp.hasLocalTimeZone() && !this.deviceTimeZone.equals(TimeZone.getTimeZone(timestamp.getLocalTimeZone()))) {
					String localTime = TimeUtils.formatTime(context, timestamp.t, timestamp.getLocalTimeZone());
					timeSb.append(" (").append(context.getString(R.string.local_time_and_time, localTime)).append(")");
				}
				if (timestamp.hasHeadsign()) {
					String timestampHeading = timestamp.getHeading(context);
					if (this.optRts != null && !StringUtils.equals(timestampHeading, this.optRts.trip.getHeading(context))) {
						timeSb.append(" (").append(timestampHeading).append(")");
					}
				}
				holder.timeTv.setText(timeSb);
				if (this.nextTimeInMs != null && TimeUtils.isSameDay(getNowToTheMinute(), this.nextTimeInMs.t) && this.nextTimeInMs.t == timestamp.t) { // now
					holder.timeTv.setTextColor(Schedule.getDefaultNowTextColor(context));
					holder.timeTv.setTypeface(Schedule.getDefaultNowTypeface());
				} else if (timestamp.t < getNowToTheMinute()) { // past
					holder.timeTv.setTextColor(Schedule.getDefaultPastTextColor(context));
					holder.timeTv.setTypeface(Schedule.getDefaultPastTypeface());
				} else { // future
					holder.timeTv.setTextColor(Schedule.getDefaultFutureTextColor(context));
					holder.timeTv.setTypeface(Schedule.getDefaultFutureTypeface());
				}
			} else {
				holder.timeTv.setText(null);
			}
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
				holder.hourTv.setText(getHourFormatter(context).formatThreadSafe(this.hours.get(hourOfTheDay.intValue())));
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
