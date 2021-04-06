package org.mtransit.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule.Timestamp;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.ui.widget.MTBaseAdapter;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.UISchedule;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;
import org.mtransit.android.task.MTCancellableFragmentAsyncTask;
import org.mtransit.android.task.ScheduleTimestampsLoader;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.LoaderUtils;
import org.mtransit.android.util.UITimeUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduleDayFragment extends MTFragmentX implements
		VisibilityAwareFragment,
		LoaderManager.LoaderCallbacks<ArrayList<Timestamp>> {

	private static final String LOG_TAG = ScheduleDayFragment.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG + "-" + dayStartsAtInMs;
	}

	private static final String EXTRA_AUTHORITY = "extra_agency_authority";
	private static final String EXTRA_POI_UUID = "extra_poi_uuid";
	private static final String EXTRA_DAY_START_AT_IN_MS = "extra_day_starts_at_ms";
	private static final String EXTRA_FRAGMENT_POSITION = "extra_fragment_position";
	private static final String EXTRA_LAST_VISIBLE_FRAGMENT_POSITION = "extra_last_visible_fragment_position";
	private static final String EXTRA_SCROLLED_TO_NOW = "extra_scrolled_to_now";

	@NonNull
	public static ScheduleDayFragment newInstance(@NonNull String uuid,
												  @NonNull String authority,
												  long dayStartsAtInMs, int fragmentPosition, int lastVisibleFragmentPosition,
												  @Nullable RouteTripStop optRts) {
		ScheduleDayFragment f = new ScheduleDayFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_AUTHORITY, authority);
		if (!Constants.FORCE_FRAGMENT_USE_ARGS) {
			f.authority = authority;
		}
		args.putString(EXTRA_POI_UUID, uuid);
		if (!Constants.FORCE_FRAGMENT_USE_ARGS) {
			f.uuid = uuid;
			f.rts = optRts;
		}
		args.putLong(EXTRA_DAY_START_AT_IN_MS, dayStartsAtInMs);
		if (!Constants.FORCE_FRAGMENT_USE_ARGS) {
			f.dayStartsAtInMs = dayStartsAtInMs;
		}
		if (fragmentPosition >= 0) {
			args.putInt(EXTRA_FRAGMENT_POSITION, fragmentPosition);
			if (!Constants.FORCE_FRAGMENT_USE_ARGS) {
				f.fragmentPosition = fragmentPosition;
			}
		}
		if (lastVisibleFragmentPosition >= 0) {
			args.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, lastVisibleFragmentPosition);
			if (!Constants.FORCE_FRAGMENT_USE_ARGS) {
				f.lastVisibleFragmentPosition = lastVisibleFragmentPosition;
			}
		}
		f.setArguments(args);
		return f;
	}

	@Nullable
	private TimeAdapter adapter;
	private int fragmentPosition = -1;
	private int lastVisibleFragmentPosition = -1;
	private boolean fragmentVisible = false;
	private boolean scrolledToNow = false;
	private long dayStartsAtInMs = -1L;
	@Nullable
	private Calendar dayStartsAtCal = null;
	@Nullable
	private Date dayStartsAtDate;

	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public ScheduleDayFragment() {
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

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
			this.dayStartsAtCal = UITimeUtils.getNewCalendar(this.dayStartsAtInMs);
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

	@Nullable
	private String authority;

	@Nullable
	private String uuid;

	@Nullable
	private RouteTripStop rts;

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

	@Nullable
	private LoadRtsTask loadRtsTask = null;

	@SuppressWarnings("deprecation")
	private static class LoadRtsTask extends MTCancellableFragmentAsyncTask<Void, Void, Boolean, ScheduleDayFragment> {

		@NonNull
		@Override
		public String getLogTag() {
			return ScheduleDayFragment.class.getSimpleName() + ">" + LoadRtsTask.class.getSimpleName();
		}

		LoadRtsTask(ScheduleDayFragment scheduleDayFragment) {
			super(scheduleDayFragment);
		}

		@Override
		protected Boolean doInBackgroundNotCancelledWithFragmentMT(@NonNull ScheduleDayFragment scheduleDayFragment, Void... params) {
			return scheduleDayFragment.initRtsSync();
		}

		@Override
		protected void onPostExecuteNotCancelledFragmentReadyMT(@NonNull ScheduleDayFragment scheduleDayFragment, @Nullable Boolean result) {
			if (Boolean.TRUE.equals(result)) {
				scheduleDayFragment.applyNewRts();
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
		if (this.uuid != null && this.authority != null) {
			POIManager poim = DataSourceManager.findPOI(requireContext(), this.authority, POIProviderContract.Filter.getNewUUIDFilter(this.uuid));
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
	public void onAttach(@NonNull Activity activity) {
		super.onAttach(activity);
		initAdapters(getActivity());
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState, getArguments());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_schedule_day, container, false);
		setupView(view);
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
		if (this.dayStartsAtInMs >= 0L) {
			outState.putLong(EXTRA_DAY_START_AT_IN_MS, this.dayStartsAtInMs);
		}
		if (this.fragmentPosition >= 0) {
			outState.putInt(EXTRA_FRAGMENT_POSITION, this.fragmentPosition);
		}
		if (this.lastVisibleFragmentPosition >= 0) {
			outState.putInt(EXTRA_LAST_VISIBLE_FRAGMENT_POSITION, this.lastVisibleFragmentPosition);
		}
		outState.putBoolean(EXTRA_SCROLLED_TO_NOW, this.scrolledToNow);
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle... bundles) {
		String newAuthority = BundleUtils.getString(EXTRA_AUTHORITY, bundles);
		if (newAuthority != null && !newAuthority.equals(this.authority)) {
			this.authority = newAuthority;
			resetRts();
		}
		String newUuid = BundleUtils.getString(EXTRA_POI_UUID, bundles);
		if (newUuid != null && !newUuid.equals(this.uuid)) {
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
		Boolean newScrolledToNow = BundleUtils.getBoolean(EXTRA_SCROLLED_TO_NOW, bundles);
		if (newScrolledToNow != null) {
			this.scrolledToNow = newScrolledToNow;
		}
		if (this.adapter != null) {
			this.adapter.setDayStartsAt(getDayStartsAtCal());
		}
	}

	int getFragmentPosition() {
		return fragmentPosition;
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		((TextView) view.findViewById(R.id.dayDate)).setText(getDayDateString(), TextView.BufferType.SPANNABLE);
		setupAdapter(view);
	}

	@NonNull
	private final ThreadSafeDateFormatter dayDateFormat = new ThreadSafeDateFormatter("EEEE, MMM d, yyyy", Locale.getDefault());

	private CharSequence getDayDateString() {
		CharSequence dateS;
		if (UITimeUtils.isYesterday(this.dayStartsAtInMs)) {
			dateS = getString(R.string.yesterday) + " " + dayDateFormat.formatThreadSafe(getDayStartsAtDate());
		} else if (UITimeUtils.isToday(this.dayStartsAtInMs)) {
			dateS = getString(R.string.today) + " " + dayDateFormat.formatThreadSafe(getDayStartsAtDate());
		} else if (UITimeUtils.isTomorrow(this.dayStartsAtInMs)) {
			dateS = getString(R.string.tomorrow) + " " + dayDateFormat.formatThreadSafe(getDayStartsAtDate());
		} else {
			dateS = dayDateFormat.formatThreadSafe(getDayStartsAtDate());
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

	@NonNull
	@Override
	public Loader<ArrayList<Timestamp>> onCreateLoader(int id, @Nullable Bundle args) {
		switch (id) {
		case SCHEDULE_LOADER:
			RouteTripStop rts = getRtsOrNull();
			if (this.dayStartsAtInMs <= 0L || rts == null) {
				//noinspection deprecation FIXME
				CrashUtils.w(this, "onCreateLoader() > skip (start time or RTL not set)");
				//noinspection ConstantConditions // FIXME
				return null;
			}
			return new ScheduleTimestampsLoader(requireContext(), this.dataSourcesRepository, rts, this.dayStartsAtInMs);
		default:
			//noinspection deprecation FIXME
			CrashUtils.w(this, "Loader id '%s' unknown!", id);
			//noinspection ConstantConditions // FIXME
			return null;
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<ArrayList<Timestamp>> loader) {
		if (this.adapter != null) {
			this.adapter.clearTimes();
			this.adapter.onPause();
		}
	}

	@Override
	public void onLoadFinished(@NonNull Loader<ArrayList<Timestamp>> loader, @Nullable ArrayList<Timestamp> data) {
		View view = getView();
		if (view == null) {
			return; // too late
		}
		switchView(view);
		if (this.adapter != null) {
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
		}
		switchView(view);
	}

	private int getTodaySelectPosition() {
		if (this.adapter != null) {
			Timestamp nextTime = this.adapter.getNextTimeInMs();
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
		}
		return 0;
	}

	private void initAdapters(FragmentActivity activity) {
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

	public static void resetColorCache() {
		TimeAdapter.resetColorCache();
	}

	private static class TimeAdapter extends MTBaseAdapter implements UITimeUtils.TimeChangedReceiver.TimeChangedListener {

		private static final String LOG_TAG = ScheduleDayFragment.class.getSimpleName() + ">" + TimeAdapter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final int ITEM_VIEW_TYPE_HOUR_SEPARATORS = 0;

		private static final int ITEM_VIEW_TYPE_TIME = 1;

		private static final int HOUR_SEPARATORS_COUNT = 24;

		private int timesCount = 0;

		@NonNull
		private final SparseArray<ArrayList<Timestamp>> hourToTimes = new SparseArray<>();

		private boolean initialized = false;

		@NonNull
		private final ArrayList<Date> hours = new ArrayList<>();

		private final LayoutInflater layoutInflater;

		private Calendar dayStartsAt;

		@NonNull
		private WeakReference<FragmentActivity> activityWR = new WeakReference<>(null);

		@Nullable
		private Timestamp nextTimeInMs = null;

		@Nullable
		private RouteTripStop optRts;

		@NonNull
		private final TimeZone deviceTimeZone = TimeZone.getDefault();

		TimeAdapter(FragmentActivity activity, Calendar dayStartsAt, RouteTripStop optRts) {
			super();
			setActivity(activity);
			this.layoutInflater = LayoutInflater.from(activity);
			setDayStartsAt(dayStartsAt);
			setRts(optRts);
		}

		void setDayStartsAt(Calendar dayStartsAt) {
			this.dayStartsAt = dayStartsAt;
			resetHours();
			if (this.dayStartsAt != null) {
				initHours();
			}
		}

		void setRts(@Nullable RouteTripStop optRts) {
			this.optRts = optRts;
		}

		public void onResume(FragmentActivity activity) {
			setActivity(activity);
		}

		public void setActivity(FragmentActivity activity) {
			this.activityWR = new WeakReference<>(activity);
		}

		public void onPause() {
			disableTimeChangedReceiver();
		}

		private void initHours() {
			resetHours();
			Calendar cal;
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				cal = (Calendar) this.dayStartsAt.clone();
				cal.set(Calendar.HOUR_OF_DAY, hourOfTheDay);
				this.hours.add(cal.getTime());
				this.hourToTimes.put(hourOfTheDay, new ArrayList<>());
			}
		}

		private void resetHours() {
			this.hours.clear();
			this.hourToTimes.clear();
		}

		void clearTimes() {
			for (int hourOfTheDay = 0; hourOfTheDay < HOUR_SEPARATORS_COUNT; hourOfTheDay++) {
				this.hourToTimes.get(hourOfTheDay).clear();
			}
			this.timesCount = 0;
			this.nextTimeInMs = null;
		}

		void setTimes(@Nullable ArrayList<Timestamp> times) {
			clearTimes();
			if (times != null) {
				for (Timestamp time : times) {
					addTime(time);
					if (this.nextTimeInMs == null && time.t >= getNowToTheMinute()) {
						this.nextTimeInMs = time;
						MTLog.d(this, "setTimes() > this.nextTimeInMs: %s", this.nextTimeInMs);
					}
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
				for (Timestamp time : this.hourToTimes.get(hourOfTheDay)) {
					if (this.nextTimeInMs == null && time.t >= getNowToTheMinute()) {
						this.nextTimeInMs = time;
						return;
					}
				}
			}
		}

		Timestamp getNextTimeInMs() {
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
			this.nowToTheMinute = UITimeUtils.currentTimeToTheMinuteMillis();
			notifyDataSetChanged();
		}

		private void enableTimeChangedReceiver() {
			if (!this.timeChangedReceiverEnabled) {
				FragmentActivity activity = this.activityWR.get();
				if (activity != null) {
					activity.registerReceiver(this.timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER);
				}
				this.timeChangedReceiverEnabled = true;
			}
		}

		private void disableTimeChangedReceiver() {
			if (this.timeChangedReceiverEnabled) {
				FragmentActivity activity = this.activityWR.get();
				if (activity != null) {
					activity.unregisterReceiver(this.timeChangedReceiver);
				}
				this.timeChangedReceiverEnabled = false;
				this.nowToTheMinute = -1L;
			}
		}

		@NonNull
		private final UITimeUtils.TimeChangedReceiver timeChangedReceiver = new UITimeUtils.TimeChangedReceiver(this);

		private void addTime(@NonNull Timestamp time) {
			int hourOfTheDay = UITimeUtils.getHourOfTheDay(time.t);
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

		@NonNull
		private final Calendar todayStartsAt = UITimeUtils.getBeginningOfTodayCal();
		@Nullable
		private Calendar todayEndsAt = null;

		int compareToToday() {
			if (this.dayStartsAt.before(this.todayStartsAt)) {
				return -1; // past
			}
			if (this.todayEndsAt == null) {
				this.todayEndsAt = UITimeUtils.getBeginningOfTomorrowCal();
				this.todayEndsAt.add(Calendar.MILLISECOND, -1);
			}
			if (this.dayStartsAt.after(this.todayEndsAt)) {
				return +1; // future
			}
			return 0; // today
		}

		public int getPosition(Object item) {
			int index = 0;
			if (!(item instanceof Timestamp)) {
				return index;
			}
			Timestamp time = (Timestamp) item;
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
							for (Timestamp hourTime : this.hourToTimes.get(hourOfTheDay)) {
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

		@Nullable
		Integer getItemHourSeparator(int position) {
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
			if (item instanceof Timestamp) {
				return ITEM_VIEW_TYPE_TIME;
			}
			return ITEM_VIEW_TYPE_HOUR_SEPARATORS;
		}

		@Override
		public long getItemIdMT(int position) {
			return position;
		}

		@Override
		public View getViewMT(int position, View convertView, ViewGroup parent) {
			switch (getItemViewType(position)) {
			case ITEM_VIEW_TYPE_HOUR_SEPARATORS:
				return getHourSeparatorView(position, convertView, parent);
			case ITEM_VIEW_TYPE_TIME:
				return getTimeView(position, convertView, parent);
			default:
				MTLog.w(this, "Unexpected view type at position '%s'!", position);
				return null;
			}
		}

		@NonNull
		private View getTimeView(int position, @Nullable View convertView, @Nullable ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_detail_status_schedule_time, parent, false);
				TimeViewHolder holder = new TimeViewHolder();
				holder.timeTv = convertView.findViewById(R.id.time);
				convertView.setTag(holder);
			}
			updateTimeView(position, convertView);
			return convertView;
		}

		private static final String P2 = ")";
		private static final String P1 = " (";

		private void updateTimeView(int position, @NonNull View convertView) {
			TimeViewHolder holder = (TimeViewHolder) convertView.getTag();
			Timestamp timestamp = (Timestamp) getItem(position);
			Context context = this.activityWR.get();
			if (timestamp != null && context != null) {
				String userTime = UITimeUtils.formatTime(context, timestamp);
				SpannableStringBuilder timeSb = new SpannableStringBuilder(userTime);
				TimeZone timestampTZ = TimeZone.getTimeZone(timestamp.getLocalTimeZone());
				if (timestamp.hasLocalTimeZone() && !this.deviceTimeZone.equals(timestampTZ)) {
					String localTime = UITimeUtils.formatTime(context, timestamp, timestampTZ);
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
				UITimeUtils.cleanTimes(timeOnly, timeSb);
				timeSb = UISchedule.decorateRealTime(context, timestamp, userTime, timeSb);
				timeSb = UISchedule.decorateOldSchedule(timestamp, timeSb);
				long nextTimeInMsT = this.nextTimeInMs == null ? -1L : this.nextTimeInMs.t;
				if (nextTimeInMsT >= 0L //
						&& UITimeUtils.isSameDay(getNowToTheMinute(), nextTimeInMsT) //
						&& nextTimeInMsT == timestamp.t) { // now
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

		@NonNull
		private static final StyleSpan SCHEDULE_LIST_TIMES_PAST_STYLE = SpanUtils.getNewNormalStyleSpan();

		@NonNull
		private static final StyleSpan SCHEDULE_LIST_TIMES_NOW_STYLE = SpanUtils.getNewBoldStyleSpan();

		@NonNull
		private static final StyleSpan SCHEDULE_LIST_TIMES_FUTURE_STYLE = SpanUtils.getNewNormalStyleSpan();

		@Nullable
		private static ForegroundColorSpan scheduleListTimesPastTextColor = null;

		@NonNull
		private static ForegroundColorSpan getScheduleListTimesPastTextColor(Context context) {
			if (scheduleListTimesPastTextColor == null) {
				scheduleListTimesPastTextColor = SpanUtils.getNewTextColor(UISchedule.getDefaultPastTextColor(context));
			}
			return scheduleListTimesPastTextColor;
		}

		@Nullable
		private static ForegroundColorSpan scheduleListTimesNowTextColor = null;

		@NonNull
		private static ForegroundColorSpan getScheduleListTimesNowTextColor(Context context) {
			if (scheduleListTimesNowTextColor == null) {
				scheduleListTimesNowTextColor = SpanUtils.getNewTextColor(UISchedule.getDefaultNowTextColor(context));
			}
			return scheduleListTimesNowTextColor;
		}

		@Nullable
		private static ForegroundColorSpan scheduleListTimesFutureTextColor = null;

		@NonNull
		private static ForegroundColorSpan getScheduleListTimesFutureTextColor(Context context) {
			if (scheduleListTimesFutureTextColor == null) {
				scheduleListTimesFutureTextColor = SpanUtils.getNewTextColor(UISchedule.getDefaultFutureTextColor(context));
			}
			return scheduleListTimesFutureTextColor;
		}

		static void resetColorCache() {
			scheduleListTimesPastTextColor = null;
			scheduleListTimesNowTextColor = null;
			scheduleListTimesFutureTextColor = null;
		}

		@NonNull
		private View getHourSeparatorView(int position, @Nullable View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_detail_status_schedule_hour_separator, parent, false);
				HourSeparatorViewHolder holder = new HourSeparatorViewHolder();
				holder.hourTv = convertView.findViewById(R.id.hour);
				convertView.setTag(holder);
			}
			updateHourSeparatorView(position, convertView);
			return convertView;
		}

		private void updateHourSeparatorView(int position, @NonNull View convertView) {
			HourSeparatorViewHolder holder = (HourSeparatorViewHolder) convertView.getTag();
			Integer hourOfTheDay = getItemHourSeparator(position);
			Context context = this.activityWR.get();
			if (hourOfTheDay != null && context != null) {
				holder.hourTv.setText(
						UITimeUtils.cleanNoRealTime(false,
								getHourFormatter(context).formatThreadSafe(
										this.hours.get(hourOfTheDay)
								)
						)
				);
			} else {
				holder.hourTv.setText(null);
			}
		}

		@Nullable
		private static ThreadSafeDateFormatter HOUR_FORMATTER;

		@NonNull
		private static ThreadSafeDateFormatter getHourFormatter(@NonNull Context context) {
			if (HOUR_FORMATTER == null) {
				HOUR_FORMATTER = UITimeUtils.getNewHourFormat(context);
			}
			return HOUR_FORMATTER;
		}

		static class TimeViewHolder {
			TextView timeTv;
		}

		static class HourSeparatorViewHolder {
			TextView hourTv;
		}
	}
}
