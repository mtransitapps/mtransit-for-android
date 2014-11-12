package org.mtransit.android.ui.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.ui.fragment.MTFragmentV4;
import org.mtransit.android.commons.ui.widget.MTBaseAdapter;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.task.ScheduleTimestampsLoader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.graphics.Typeface;
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
		LoaderManager.LoaderCallbacks<List<Schedule.Timestamp>> {

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

	public static ScheduleDayFragment newInstance(RouteTripStop rts, long dayStartsAtInMs, int fragmentPosition, int lastVisibleFragmentPosition) {
		ScheduleDayFragment f = new ScheduleDayFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, rts.getAuthority());
		args.putString(EXTRA_POI_UUID, rts.getUUID());
		args.putLong(EXTRA_DAY_START_AT_IN_MS, dayStartsAtInMs);
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
		}
		if (lastVisibleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisibleFragmentPosition);
		}
		f.setArguments(args);
		return f;
	}

	private TimeAdapter adapter;
	private CharSequence emptyText;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private long dayStartsAtInMs = -1l;
	private Calendar dayStartsAtCal = null;
	private Date dayStartsAtDate;
	private RouteTripStop rts;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.fragment_schedule_day, container, false);
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
		if (this.rts != null && this.rts != null) {
			outState.putString(EXTRA_POI_UUID, this.rts.getUUID());
			outState.putString(EXTRA_AUTHORITY, this.rts.getAuthority());
		}
		if (this.dayStartsAtInMs >= 0l) {
			outState.putLong(EXTRA_DAY_START_AT_IN_MS, this.dayStartsAtInMs);
		}
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		final String authority = BundleUtils.getString(EXTRA_AUTHORITY, savedInstanceState, getArguments());
		final String uuid = BundleUtils.getString(EXTRA_POI_UUID, savedInstanceState, getArguments());
		if (!TextUtils.isEmpty(authority) && !TextUtils.isEmpty(uuid)) {
			final POIFilter poiFilter = new POIFilter(Arrays.asList(new String[] { uuid }));
			final POIManager poim = DataSourceManager.findPOI(getActivity(), UriUtils.newContentUri(authority), poiFilter);
			if (poim != null && poim.poi instanceof RouteTripStop) {
				this.rts = (RouteTripStop) poim.poi;
			}
		}
		this.dayStartsAtInMs = BundleUtils.getLong(EXTRA_DAY_START_AT_IN_MS, savedInstanceState, getArguments());
		this.dayStartsAtCal = TimeUtils.getNewCalendar(this.dayStartsAtInMs);
		this.dayStartsAtDate = this.dayStartsAtCal.getTime();
		final Integer fragmentPosition = BundleUtils.getInt(EXTRA_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (fragmentPosition != null) {
			if (fragmentPosition.intValue() >= 0) {
				this.fragmentPosition = fragmentPosition.intValue();
			} else {
				this.fragmentPosition = -1;
			}
		}
		final Integer lastVisibleFragmentPosition = BundleUtils.getInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, savedInstanceState, getArguments());
		if (lastVisibleFragmentPosition != null) {
			if (lastVisibleFragmentPosition.intValue() >= 0) {
				this.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
			} else {
				this.lastVisibleFragmentPosition = -1;
			}
		}
	}

	public int getFragmentPosition() {
		return fragmentPosition;
	}

	private static final SimpleDateFormat DAY_DATE_FORMAT = new SimpleDateFormat("EEEE, MMM d, yyyy");

	private void setupView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		inflateList(view);
		((AbsListView) view.findViewById(R.id.list)).setAdapter(this.adapter);
		String dateS = DAY_DATE_FORMAT.format(this.dayStartsAtDate);
		if (TimeUtils.isYesterday(this.dayStartsAtInMs)) {
			dateS = getString(R.string.yesterday) + " " + dateS;
		} else if (TimeUtils.isToday(this.dayStartsAtInMs)) {
			dateS = getString(R.string.today) + " " + dateS;
		} else if (TimeUtils.isTomorrow(this.dayStartsAtInMs)) {
			dateS = getString(R.string.tomorrow) + " " + dateS;
		}
		((TextView) view.findViewById(R.id.dayDate)).setText(dateS);
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
	}

	private void onFragmentVisible() {
		if (this.fragmentVisible) {
			return; // already visible
		}
		this.fragmentVisible = true;
		switchView(getView());
		if (this.adapter == null) {
			getLoaderManager().restartLoader(SCHEDULE_LOADER, null, this);
		} else {
			if (this.adapter.getCount() > 0) {
				this.adapter.onResume(getActivity());
			}
		}
	}

	private static final int SCHEDULE_LOADER = 0;

	@Override
	public Loader<List<Schedule.Timestamp>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case SCHEDULE_LOADER:
			final ScheduleTimestampsLoader scheduleLoader = new ScheduleTimestampsLoader(getActivity(), this.rts, this.dayStartsAtInMs);
			return scheduleLoader;
		default:
			MTLog.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<List<Schedule.Timestamp>> loader) {
		if (this.adapter != null) {
			this.adapter.clearTimes();
			this.adapter.onPause();
		}
	}

	@Override
	public void onLoadFinished(Loader<List<Schedule.Timestamp>> loader, List<Schedule.Timestamp> data) {
		if (this.adapter == null) {
			initAdapter();
		}
		this.adapter.setTimes(data);
		final View view = getView();
		int compareToToday = this.adapter.compareToToday();
		final int selectPosition;
		if (compareToToday < 0) { // past
			selectPosition = this.adapter.getCount(); // scroll down
		} else if (compareToToday > 0) { // future
			selectPosition = 0; // scroll up
		} else { // today
			selectPosition = getTodaySelectPosition();
		}
		((AbsListView) view.findViewById(R.id.list)).setSelection(selectPosition);
		switchView(view);
	}

	private int getTodaySelectPosition() {
		final Schedule.Timestamp nextTime = this.adapter.getNextTimeInMs();
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
		this.adapter = new TimeAdapter(getActivity(), this.dayStartsAtCal, this.rts);
		final View view = getView();
		setupView(view);
		switchView(view);
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
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) view.findViewById(R.id.empty_text)).setText(this.emptyText);
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

		private SparseArray<List<Schedule.Timestamp>> hourToTimes = new SparseArray<List<Schedule.Timestamp>>();

		private List<Date> hours = new ArrayList<Date>();

		private LayoutInflater layoutInflater;

		private Calendar dayStartsAt;

		private Context context;

		private Schedule.Timestamp nextTimeInMs = null;

		private RouteTripStop rts;

		public TimeAdapter(Context context, Calendar dayStartsAt, RouteTripStop rts) {
			super();
			this.context = context;
			this.layoutInflater = LayoutInflater.from(context);
			this.dayStartsAt = dayStartsAt;
			this.rts = rts;
			initHours();
		}

		public void onResume(Activity activity) {
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

		public void setTimes(List<Schedule.Timestamp> times) {
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
				this.context.registerReceiver(this.timeChangedReceiver, TimeUtils.TIME_CHANGED_INTENT_FILTER);
				this.timeChangedReceiverEnabled = true;
			}
		}

		private void disableTimeChangeddReceiver() {
			if (this.timeChangedReceiverEnabled) {
				this.context.unregisterReceiver(this.timeChangedReceiver);
				this.timeChangedReceiverEnabled = false;
				this.nowToTheMinute = -1l;
			}
		}

		private final BroadcastReceiver timeChangedReceiver = new TimeUtils.TimeChangedReceiver(this);

		private void addTime(Schedule.Timestamp time) {
			final int hourOfTheDay = TimeUtils.getHourOfTheDay(time.t);
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
			final Calendar todayStartsAt = TimeUtils.getBeginningOfTodayCal();
			if (this.dayStartsAt.before(todayStartsAt)) {
				return -1; // past
			}
			final Calendar todayEndsAt = TimeUtils.getBeginningOfTomorrowCal();
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
					final Date thatDate = this.hours.get(hourOfTheDay);
					if (date.after(thatDate)) {
						final int nextHourOfTheDay = hourOfTheDay + 1;
						final Date nextDate = nextHourOfTheDay < this.hours.size() ? this.hours.get(nextHourOfTheDay) : null;
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
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_status_detail_schedule_time, parent, false);
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
			if (timestamp != null) {
				String timeS = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(new Date(timestamp.t));
				if (timestamp.hasHeadsign()) {
					final String timestampHeading = timestamp.getHeading(this.context);
					if (!StringUtils.equals(timestampHeading, this.rts.trip.getHeading(context))) {
						timeS += " (" + timestampHeading + ")";
					}
				}
				holder.timeTv.setText(timeS);
				if (this.nextTimeInMs != null && TimeUtils.isSameDay(getNowToTheMinute(), this.nextTimeInMs.t) && this.nextTimeInMs.t == timestamp.t) {
					holder.timeTv.setTextColor(ColorUtils.getTextColorPrimary(context)); // now
					holder.timeTv.setTypeface(Typeface.DEFAULT_BOLD);
				} else if (timestamp.t < getNowToTheMinute()) { // past
					holder.timeTv.setTextColor(ColorUtils.getTextColorTertiary(context));
					holder.timeTv.setTypeface(Typeface.DEFAULT);
				} else { // future
					holder.timeTv.setTextColor(ColorUtils.getTextColorSecondary(context));
					holder.timeTv.setTypeface(Typeface.DEFAULT);
				}
			} else {
				holder.timeTv.setText(null);
			}
		}

		private View getHourSeparatorView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_status_detail_schedule_hour_separator, parent, false);
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
			if (hourOfTheDay != null) {
				holder.hourTv.setText(TimeUtils.getNewHourFormat(this.context).format(this.hours.get(hourOfTheDay.intValue())));
			} else {
				holder.hourTv.setText(null);
			}
		}

		public static class TimeViewHolder {
			TextView timeTv;
		}

		public static class HourSperatorViewHolder {
			TextView hourTv;
		}

	}
}
