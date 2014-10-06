package org.mtransit.android.data;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SensorUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.ui.widget.MTArrayAdapter;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.provider.FavoriteManager.FavoriteUpdateListener;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.view.MTCompassView;
import org.mtransit.android.ui.view.MTPieChartPercentView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

public class POIArrayAdapter extends MTArrayAdapter<POIManager> implements SensorUtils.CompassListener, AdapterView.OnItemClickListener,
		AdapterView.OnItemLongClickListener, SensorEventListener, AbsListView.OnScrollListener, StatusLoader.StatusLoaderListener, MTLog.Loggable,
		FavoriteUpdateListener, SensorUtils.SensorTaskCompleted {

	private static final String TAG = POIArrayAdapter.class.getSimpleName();

	private String tag = TAG;

	@Override
	public String getLogTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	private static IntentFilter s_intentFilter;

	static {
		s_intentFilter = new IntentFilter();
		s_intentFilter.addAction(Intent.ACTION_TIME_TICK);
		s_intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		s_intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
	}

	private LayoutInflater layoutInflater;

	private List<POIManager> pois;

	private Set<String> favUUIDs;

	private Activity activity;

	private Location location;

	private int lastCompassInDegree = -1;

	private float locationDeclination;

	@Deprecated
	private Pair<Integer, String> closestPOI;


	private float[] accelerometerValues = new float[3];

	private float[] magneticFieldValues = new float[3];


	private boolean showStatus = true; // show times / availability

	private ViewGroup manualLayout;

	private ScrollView manualScrollView;

	private long lastNotifyDataSetChanged = -1l;

	private int scrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

	private long nowToTheMinute = -1l;

	private boolean timeChangedReceiverEnabled = false;

	private boolean compassUpdatesEnabled = false;

	private long lastCompassChanged = -1l;

	public POIArrayAdapter(Activity activity) {
		super(activity, R.layout.layout_loading_small);
		this.activity = activity;
		this.layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void setManualLayout(ViewGroup manualLayout) {
		this.manualLayout = manualLayout;
	}

	public void setShowStatus(boolean showData) {
		this.showStatus = showData;
	}

	private static final int VIEW_TYPE_COUNT = 4;

	@Override
	public int getViewTypeCount() {
		// RETURN MUST MATCH getItemViewType(position) !
		return VIEW_TYPE_COUNT; // see getItemViewType()
	}

	@Override
	public int getItemViewType(int position) {
		// RETURN MUST MATCH getViewTypeCount() !
		POIManager poim = getItem(position);
		if (poim == null) {
			MTLog.d(this, "Cannot find type for object null!");
			return Adapter.IGNORE_ITEM_VIEW_TYPE;
		}
		int type = poim.poi.getType();
		int statusType = poim.getStatusType();
		switch (type) {
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			switch (statusType) {
			case POI.ITEM_STATUS_TYPE_SCHEDULE:
				return 3; // RTS & SCHEDULE
			default:
				return 4; // RTS
			}
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
		default:
			switch (statusType) {
			case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
				return 0; // DEFAULT & AVAILABILITY %
			default:
				return 1; // DEFAULT
			}
		}
	}

	@Override
	public int getCount() {
		final int count = pois == null ? 0 : pois.size();
		return count;
	}

	@Override
	public int getPosition(POIManager item) {
		final int position = pois == null ? 0 : pois.indexOf(item);
		return position;
	}

	@Override
	public POIManager getItem(int position) {
		final POIManager item = this.pois == null ? null : this.pois.get(position);
		return item;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		POIManager poim = getItem(position);
		if (poim == null) {
			MTLog.w(this, "getView() > Cannot create view for null poi at position '%s'!", position);
			return null; // CRASH!!!
		}
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return getRouteTripStopView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOIView(poim, convertView, parent);
		default:
			MTLog.w(this, "getView() > Unknow view type at position %s!", position);
			return null; // CRASH!!!
		}
	}

	private void updateCommonViewManual(int position, View convertView) {
		if (convertView == null || convertView.getTag() == null || !(convertView.getTag() instanceof CommonViewHolder)) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) convertView.getTag();
		POIManager poim = getItem(position);
		updateCommonView(holder, poim);
		return;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		showPoiViewerActivity(position);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		return showPoiMenu(position);
	}

	public boolean showClosestPOI() {
		if (!hasClosestPOI()) {
			return false;
		}
		return false;
	}

	public boolean showPoiViewerActivity(int position) {
		if (this.pois != null && position < this.pois.size() && this.pois.get(position) != null) {
			return showPoiViewerActivity(this.pois.get(position));
		}
		return false;
	}

	public boolean showPoiMenu(int position) {
		if (this.pois != null && position < this.pois.size() && this.pois.get(position) != null) {
			return showPoiMenu(this.pois.get(position));
		}
		return false;
	}

	public boolean showPoiViewerActivity(final POIManager poim) {
		if (poim == null) {
			return false;
		}
		if (poim.onActionItemClick(this.activity)) {
			return true;
		}
		MTLog.w(this, "Unknow view type for poi %s!", poim);
		return false;
	}

	public boolean showPoiMenu(final POIManager poim) {
		if (poim == null) {
			return false;
		}
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			StringBuilder title = new StringBuilder(poim.poi.getName());
			final Favorite findFavorite = FavoriteManager.findFavorite(getContext(), poim.poi.getUUID());
			final boolean isFavorite = findFavorite != null;
			new AlertDialog.Builder(getContext()).setTitle(title)
					.setItems(poim.getActionsItems(getContext(), getContext().getString(R.string.view_details), isFavorite),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int item) {
									MTLog.v(POIArrayAdapter.this, "onClick(%s)", item);
									if (poim.onActionsItemClick(POIArrayAdapter.this.activity, item, isFavorite, POIArrayAdapter.this)) {
										return;
									}
									switch (item) {
									case 0:
										showPoiViewerActivity(poim);
										break;
									default:
										break;
									}
								}
							}).create().show();
			return true;
		default:
			MTLog.w(this, "Unknow view type for poi %s!", poim);
			return false;
		}
	}

	@Override
	public void onFavoriteUpdated() {
		refreshFavorites();
	}

	public void setPois(List<POIManager> pois) {
		this.pois = pois;
	}

	public List<POIManager> getPois() {
		return pois;
	}

	public int getPoisCount() {
		if (pois == null) {
			return 0;
		}
		return pois.size();
	}

	public boolean hasPois() {
		return getPoisCount() > 0;
	}

	@Deprecated
	private void updateClosestPoi() {
	}

	@Deprecated
	public boolean hasClosestPOI() {
		return false;
	}

	@Deprecated
	public boolean isClosestPOI(int position) {
		return false;
	}

	public POI findPoi(String uuid) {
		if (this.pois != null) {
			for (final POIManager poim : this.pois) {
				if (poim.poi.getUUID().equals(uuid)) {
					return poim.poi;
				}
			}
		}
		return null;
	}

	public int indexOfPoi(String uuid) {
		if (this.pois != null) {
			for (int i = 0; i < pois.size(); i++) {
				if (pois.get(i).poi.getUUID().equals(uuid)) {
					return i;
				}
			}
		}
		return -1;
	}

	@Deprecated
	public void prefetchClosests() {
	}

	@Deprecated
	public void prefetchFavorites() {
	}


	private MTAsyncTask<Location, Void, Void> updateDistanceWithStringTask;

	private void updateDistances(Location currentLocation) {
		if (this.updateDistanceWithStringTask != null) {
			this.updateDistanceWithStringTask.cancel(true);
		}
		if (this.pois != null && currentLocation != null) {
			this.updateDistanceWithStringTask = new MTAsyncTask<Location, Void, Void>() {

				@Override
				public String getLogTag() {
					return POIArrayAdapter.this.getLogTag();
				}

				@Override
				protected Void doInBackgroundMT(Location... params) {
					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
					LocationUtils.updateDistanceWithString(POIArrayAdapter.this.getContext(), POIArrayAdapter.this.pois, params[0], this);
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					if (isCancelled()) {
						return;
					}
					final Pair<Integer, String> previousClosest = POIArrayAdapter.this.closestPOI;
					updateClosestPoi();
					boolean newClosest = POIArrayAdapter.this.closestPOI == null ? false : POIArrayAdapter.this.closestPOI.equals(previousClosest);
					notifyDataSetChanged(newClosest);
					prefetchClosests();
				}
			};
			this.updateDistanceWithStringTask.execute(currentLocation);
		}
	}

	public void updateDistancesNowSync(Location currentLocation) {
		if (this.pois != null && currentLocation != null) {
			LocationUtils.updateDistanceWithString(getContext(), this.pois, currentLocation, null);
			updateClosestPoi();
		}
		setLocation(currentLocation);
	}

	public void updateDistanceNowAsync(Location currentLocation) {
		this.location = null; // clear current location to force refresh
		setLocation(currentLocation);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		setScrollState(scrollState);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	public void setScrollState(int scrollState) {
		this.scrollState = scrollState;
	}

	@Override
	public void onStatusLoaded(POIStatus status) {
		if (this.showStatus) {
			// needs to force data set changed or schedule might never be shown
			if (this.poiStatusViewHoldersWR != null && this.poiStatusViewHoldersWR.containsKey(status.getTargetUUID())) {
				updatePOIStatus(this.poiStatusViewHoldersWR.get(status.getTargetUUID()), status);
			}
		}
	}

	public void notifyDataSetChanged(boolean force) {
		notifyDataSetChanged(force, Constants.ADAPTER_NOTIFY_THRESOLD_IN_MS);
	}

	private Handler handler = new Handler();

	private Runnable notifyDataSetChangedLater = new Runnable() {

		@Override
		public void run() {
			notifyDataSetChanged(true); // still really need to show new data
		}
	};

	public void notifyDataSetChanged(boolean force, int minAdapterThresoldInMs) {
		long now = System.currentTimeMillis();
		final long adapterThreasold = Math.max(minAdapterThresoldInMs, Constants.ADAPTER_NOTIFY_THRESOLD_IN_MS);
		if (this.scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && (force || (now - this.lastNotifyDataSetChanged) > adapterThreasold)) {
			notifyDataSetChanged();
			notifyDataSetChangedManual();
			this.lastNotifyDataSetChanged = now;
			this.handler.removeCallbacks(this.notifyDataSetChangedLater);
			tryLoadingScheduleIfNotBusy();
		} else {
			// IF we really needed to show new data AND list wasn't not idle DO try again later
			if (force && this.scrollState != AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
				this.handler.postDelayed(this.notifyDataSetChangedLater, adapterThreasold);
			}
		}
	}

	private void notifyDataSetChangedManual() {
		if (this.manualLayout != null && hasPois()) {
			int position = 0;
			for (int i = 0; i < this.manualLayout.getChildCount(); i++) {
				View view = this.manualLayout.getChildAt(i);
				Object tag = view.getTag();
				if (tag != null && tag instanceof CommonViewHolder) {
					updateCommonViewManual(position, view);
					position++;
				}
			}
		}
	}

	public void setListView(AbsListView listView) {
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		listView.setOnScrollListener(this);
		listView.setAdapter(this);
	}

	public void initManual() {
		if (this.manualLayout != null && hasPois()) {
			this.manualLayout.removeAllViews();
			for (int i = 0; i < getPoisCount(); i++) {
				if (this.manualLayout.getChildCount() > 0) {
					this.manualLayout.addView(this.layoutInflater.inflate(R.layout.list_view_divider, this.manualLayout, false));
				}
				View view = getView(i, null, this.manualLayout);
				final int position = i;
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showPoiViewerActivity(position);
					}
				});
				view.setOnLongClickListener(new View.OnLongClickListener() {

					@Override
					public boolean onLongClick(View v) {
						return showPoiMenu(position);
					}
				});
				this.manualLayout.addView(view);
			}
		}
	}

	public void scrollManualScrollViewTo(int x, int y) {
		if (this.manualScrollView != null) {
			this.manualScrollView.scrollTo(x, y);
		}
	}

	public void setManualScrollView(ScrollView scrollView) {
		this.manualScrollView = scrollView;
		if (scrollView == null) {
			return;
		}
		scrollView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_SCROLL:
				case MotionEvent.ACTION_MOVE:
					setScrollState(AbsListView.OnScrollListener.SCROLL_STATE_FLING);
					break;
				case MotionEvent.ACTION_DOWN:
					setScrollState(AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
					break;
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					// scroll view can still by flying
					setScrollState(AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
					break;
				default:
					MTLog.v(POIArrayAdapter.this, "Unexpected event %s", event);
				}
				return false;
			}
		});
	}

	public void setLocation(Location newLocation) {
		if (newLocation != null) {
			if (this.location == null || LocationUtils.isMoreRelevant(getLogTag(), this.location, newLocation)) {
				this.location = newLocation;
				this.locationDeclination = SensorUtils.getLocationDeclination(this.location);
				if (!this.compassUpdatesEnabled) {
					SensorUtils.registerCompassListener(getContext(), this);
					this.compassUpdatesEnabled = true;
				}
				updateDistances(this.location);
			}
		}
	}

	public void onPause() {
		if (this.compassUpdatesEnabled) {
			SensorUtils.unregisterSensorListener(getContext(), this);
			this.compassUpdatesEnabled = false;
		}
		this.handler.removeCallbacks(this.notifyDataSetChangedLater);
		if (this.refreshFavoritesTask != null) {
			this.refreshFavoritesTask.cancel(true);
			this.refreshFavoritesTask = null;
		}
		disableTimeChangeddReceiver();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(POIArrayAdapter.class.getSimpleName()) //
				.append(getLogTag()) //
				.toString();
	}

	public void onResume() {
		if (!this.compassUpdatesEnabled) {
			SensorUtils.registerCompassListener(getContext(), this);
			this.compassUpdatesEnabled = true;
		}
	}

	public void clearAll() {
		if (this.pois != null) {
			this.pois.clear();
			this.pois = null;
		}
		disableTimeChangeddReceiver();
		//
		this.compassImgsWR.clear();
		this.lastCompassChanged = -1;
		this.lastCompassInDegree = -1;
		this.accelerometerValues = new float[3];
		this.magneticFieldValues = new float[3];
		//
		this.lastNotifyDataSetChanged = -1l;
		this.handler.removeCallbacks(this.notifyDataSetChangedLater);
		//
		this.poiStatusViewHoldersWR.clear();
		//
		if (this.refreshFavoritesTask != null) {
			this.refreshFavoritesTask.cancel(true);
			this.refreshFavoritesTask = null;
		}
		//
		if (this.updateDistanceWithStringTask != null) {
			this.updateDistanceWithStringTask.cancel(true);
			this.updateDistanceWithStringTask = null;
		}
		this.location = null;
		this.locationDeclination = 0f;
	}

	public void onDestroy() {
		disableTimeChangeddReceiver();
		if (this.pois != null) {
			this.pois.clear();
		}
		this.compassImgsWR.clear();
		this.poiStatusViewHoldersWR.clear();
	}

	@Override
	public void updateCompass(final float orientation, boolean force) {
		if (this.pois == null) {
			return;
		}
		long now = System.currentTimeMillis();
		int roundedOrientation = SensorUtils.convertToPosivite360Degree((int) orientation);
		SensorUtils.updateCompass(force, this.location, roundedOrientation, now, this.scrollState, this.lastCompassChanged, this.lastCompassInDegree,
				Constants.ADAPTER_NOTIFY_THRESOLD_IN_MS, this);
	}

	@Override
	public void onSensorTaskCompleted(boolean result, int roundedOrientation, long now) {
		if (result) {
			this.lastCompassInDegree = roundedOrientation;
			this.lastCompassChanged = now;
			if (this.compassUpdatesEnabled && this.location != null && this.lastCompassInDegree >= 0) {
				if (this.compassImgsWR != null) {
					for (MTCompassView compassView : this.compassImgsWR.keySet()) {
						if (compassView != null && compassView.getVisibility() == View.VISIBLE) {
							compassView.generateAndSetHeading(this.location, this.lastCompassInDegree, this.locationDeclination);
						}
					}
				}
			}
		}
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		SensorUtils.checkForCompass(getContext(), se, this.accelerometerValues, this.magneticFieldValues, this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private WeakHashMap<String, CommonStatusViewHolder> poiStatusViewHoldersWR = new WeakHashMap<String, CommonStatusViewHolder>();

	private View getBasicPOIView(POIManager poim, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getBasicPOILayout(poim.getStatusType()), parent, false);
			BasicPOIViewHolder holder = new BasicPOIViewHolder();
			initCommonViewHolder(holder, convertView, poim.poi.getUUID());
			holder.statusViewHolder = getPOIStatusViewHolder(poim.getStatusType(), convertView);
			this.poiStatusViewHoldersWR.put(holder.uuid, holder.statusViewHolder);
			convertView.setTag(holder);
		}
		updateBasicPOIView(poim, convertView);
		return convertView;
	}

	private CommonStatusViewHolder getPOIStatusViewHolder(int status, View convertView) {
		switch (status) {
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = new AvailabilityPercentStatusViewHolder();
			initCommonStatusViewHolderHolder(availabilityPercentStatusViewHolder, convertView);
			availabilityPercentStatusViewHolder.textTv = (TextView) convertView.findViewById(R.id.textTv);
			availabilityPercentStatusViewHolder.piePercentV = (MTPieChartPercentView) convertView.findViewById(R.id.pie);
			return availabilityPercentStatusViewHolder;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			ScheduleStatusViewHolder scheduleStatusViewHolder = new ScheduleStatusViewHolder();
			initCommonStatusViewHolderHolder(scheduleStatusViewHolder, convertView);
			scheduleStatusViewHolder.dataNextLine1Tv = (TextView) convertView.findViewById(R.id.data_next_line_1);
			scheduleStatusViewHolder.dataNextLine2Tv = (TextView) convertView.findViewById(R.id.data_next_line_2);
			return scheduleStatusViewHolder;
		default:
			MTLog.w(this, "Unexpected status '%s' (no view holder)!", status);
			return null;
		}
	}

	private int getBasicPOILayout(int status) {
		int layoutRes = R.layout.layout_poi_basic;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			layoutRes = R.layout.layout_poi_basic_availability_percent;
			break;
		default:
			MTLog.w(this, "Unexpected status '%s' (basic view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	private WeakHashMap<MTCompassView, Object> compassImgsWR = new WeakHashMap<MTCompassView, Object>();

	private void initCommonViewHolder(CommonViewHolder holder, View convertView, String poiUUID) {
		holder.uuid = poiUUID;
		holder.view = convertView;
		holder.nameTv = (TextView) convertView.findViewById(R.id.name);
		holder.favImg = (ImageView) convertView.findViewById(R.id.fav);
		holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
		holder.compassV = (MTCompassView) convertView.findViewById(R.id.compass);
		this.compassImgsWR.put(holder.compassV, null);
	}

	private static void initCommonStatusViewHolderHolder(CommonStatusViewHolder holder, View convertView) {
		holder.statusV = convertView.findViewById(R.id.status);
	}

	private View updateBasicPOIView(POIManager poim, View convertView) {
		if (convertView == null || poim == null) {
			return convertView;
		}
		BasicPOIViewHolder holder = (BasicPOIViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		updatePOIStatus(holder.statusViewHolder, poim);
		return convertView;
	}

	private void updateAvailabilityPercent(CommonStatusViewHolder statusViewHolder, POIManager poim) {
		if (this.showStatus && poim != null && statusViewHolder instanceof AvailabilityPercentStatusViewHolder) {
			poim.setStatusLoaderListener(this);
			updateAvailabilityPercent(statusViewHolder, poim.getStatus(getContext()));
		} else {
			statusViewHolder.statusV.setVisibility(View.GONE);
		}
	}

	private void updateAvailabilityPercent(CommonStatusViewHolder statusViewHolder, POIStatus status) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = (AvailabilityPercentStatusViewHolder) statusViewHolder;
		if (status != null && status instanceof AvailabilityPercent) {
			AvailabilityPercent availabilityPercent = (AvailabilityPercent) status;
			if (!availabilityPercent.isStatusOK()) {
				availabilityPercentStatusViewHolder.textTv.setText(availabilityPercent.getStatusMsg(getContext()));
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.VISIBLE);
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.GONE);
			} else if (availabilityPercent.isShowingLowerValue()) {
				availabilityPercentStatusViewHolder.textTv.setText(availabilityPercent.getLowerValueText(getContext()));
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.VISIBLE);
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.GONE);
			} else {
				availabilityPercentStatusViewHolder.piePercentV.setValueColors( //
						availabilityPercent.getValue1Color(), //
						availabilityPercent.getValue1ColorBg(), //
						availabilityPercent.getValue2Color(), //
						availabilityPercent.getValue2ColorBg() //
						);
				availabilityPercentStatusViewHolder.piePercentV.setValues(availabilityPercent.getValue1(), availabilityPercent.getValue2());
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.VISIBLE);
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.GONE);
			}
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.GONE);
		}
	}



	private int getRTSLayout(int status) {
		int layoutRes = R.layout.layout_poi_rts;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			layoutRes = R.layout.layout_poi_rts_schedule;
			break;
		default:
			MTLog.w(this, "Unexpected status '%s' (rts view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	private View getRouteTripStopView(POIManager poim, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getRTSLayout(poim.getStatusType()), parent, false);
			RouteTripStopViewHolder holder = new RouteTripStopViewHolder();
			initCommonViewHolder(holder, convertView, poim.poi.getUUID());
			initRTSExtra(convertView, holder);
			// schedule status
			holder.statusViewHolder = getPOIStatusViewHolder(poim.getStatusType(), convertView);
			this.poiStatusViewHoldersWR.put(holder.uuid, holder.statusViewHolder);
			convertView.setTag(holder);
		}
		updateRouteTripStopView(poim, convertView);
		return convertView;
	}

	private void initRTSExtra(View convertView, RouteTripStopViewHolder holder) {
		holder.rtsExtraV = convertView.findViewById(R.id.rts_extra);
		holder.routeFL = convertView.findViewById(R.id.route);
		holder.routeShortNameTv = (TextView) convertView.findViewById(R.id.route_short_name);
		holder.tripHeadingTv = (TextView) convertView.findViewById(R.id.trip_heading);
	}

	private View updateRouteTripStopView(POIManager poim, View convertView) {
		if (convertView == null || poim == null) {
			return convertView;
		}
		RouteTripStopViewHolder holder = (RouteTripStopViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		updateRTSExtra(poim, holder);
		updatePOIStatus(holder.statusViewHolder, poim);
		return convertView;
	}

	private void updateRTSExtra(POIManager poim, RouteTripStopViewHolder holder) {
		if (poim.poi instanceof RouteTripStop) {
			final RouteTripStop rts = (RouteTripStop) poim.poi;
			if (rts.route == null) {
				holder.rtsExtraV.setVisibility(View.GONE);
				holder.routeFL.setVisibility(View.GONE);
				holder.tripHeadingTv.setVisibility(View.GONE);
			} else {
				int routeTextColor = ColorUtils.parseColor(rts.route.textColor);
				int routeColor = ColorUtils.parseColor(rts.route.color);
				if (TextUtils.isEmpty(rts.route.shortName)) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
				} else {
					holder.routeShortNameTv.setText(rts.route.shortName);
					holder.routeShortNameTv.setTextColor(routeTextColor);
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				holder.routeFL.setBackgroundColor(routeColor);
				holder.routeFL.setVisibility(View.VISIBLE);
				holder.rtsExtraV.setVisibility(View.VISIBLE);
				if (rts.trip == null) {
					holder.tripHeadingTv.setVisibility(View.GONE);
				} else {
					holder.tripHeadingTv.setText(rts.trip.getHeading(getContext()).toUpperCase(Locale.getDefault()));
					holder.tripHeadingTv.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private void updatePOIStatus(CommonStatusViewHolder statusViewHolder, POIStatus status) {
		if (!this.showStatus || status == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				statusViewHolder.statusV.setVisibility(View.GONE);
			}
			return;
		}
		switch (status.getType()) {
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			updateAvailabilityPercent(statusViewHolder, status);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			updateRTSSchedule(statusViewHolder, status);
			break;
		default:
			MTLog.w(this, "Unexpected status type '%s'!", status.getType());
			statusViewHolder.statusV.setVisibility(View.GONE);
			return;
		}
	}

	private void updatePOIStatus(CommonStatusViewHolder statusViewHolder, POIManager poim) {
		if (!this.showStatus || poim == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				statusViewHolder.statusV.setVisibility(View.GONE);
			}
			return;
		}
		switch (poim.getStatusType()) {
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			updateAvailabilityPercent(statusViewHolder, poim);
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			updateRTSSchedule(statusViewHolder, poim);
			break;
		default:
			MTLog.w(this, "Unexpected status type '%s'!", poim.getStatusType());
			statusViewHolder.statusV.setVisibility(View.GONE);
		}
	}

	private void updateRTSSchedule(CommonStatusViewHolder statusViewHolder, POIManager poim) {
		if (this.showStatus && poim != null && statusViewHolder instanceof ScheduleStatusViewHolder) {
			poim.setStatusLoaderListener(this);
			updateRTSSchedule(statusViewHolder, poim.getStatus(getContext()));
		} else {
			statusViewHolder.statusV.setVisibility(View.GONE);
		}
	}

	private void updateRTSSchedule(CommonStatusViewHolder statusViewHolder, POIStatus status) {
		CharSequence line1CS = null;
		CharSequence line2CS = null;
		if (status != null && status instanceof Schedule) {
			Schedule schedule = (Schedule) status;
			final int count = 20; // needs enough to check if service is frequent (every 5 minutes or less for at least 30 minutes)
			List<Pair<CharSequence, CharSequence>> lines = schedule.getNextTimesStrings(getContext(), getNowToTheMinute(), count); // , schedule.decentOnly);
			if (lines != null && lines.size() >= 1) {
				line1CS = lines.get(0).first;
				line2CS = lines.get(0).second;
			}
		}
		ScheduleStatusViewHolder scheduleStatusViewHolder = (ScheduleStatusViewHolder) statusViewHolder;
		scheduleStatusViewHolder.dataNextLine1Tv.setText(line1CS);
		scheduleStatusViewHolder.dataNextLine2Tv.setText(line2CS);
		scheduleStatusViewHolder.dataNextLine2Tv.setVisibility(line2CS != null && line2CS.length() > 0 ? View.VISIBLE : View.GONE);
		statusViewHolder.statusV.setVisibility(line1CS != null && line1CS.length() > 0 ? View.VISIBLE : View.GONE);
	}

	private long getNowToTheMinute() {
		if (this.nowToTheMinute < 0) {
			resetNowToTheMinute();
			enableTimeChangedReceiver();
		}
		return this.nowToTheMinute;
	}

	private void resetNowToTheMinute() {
		this.nowToTheMinute = TimeUtils.currentTimeToTheMinuteMillis();
		notifyDataSetChanged(false);
	}

	private final BroadcastReceiver timeChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_TIME_TICK.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
				resetNowToTheMinute();
				tryLoadingScheduleIfNotBusy();
			}
		}
	};

	private void tryLoadingScheduleIfNotBusy() {
	}

	private void enableTimeChangedReceiver() {
		if (!this.timeChangedReceiverEnabled) {
			getContext().registerReceiver(timeChangedReceiver, s_intentFilter);
			this.timeChangedReceiverEnabled = true;
		}
	}

	private void disableTimeChangeddReceiver() {
		if (this.timeChangedReceiverEnabled) {
			getContext().unregisterReceiver(this.timeChangedReceiver);
			this.timeChangedReceiverEnabled = false;
			this.nowToTheMinute = -1l;
		}
	}

	private void updateCommonView(CommonViewHolder holder, POIManager poim) {
		if (poim == null || holder == null) {
			return;
		}
		if (holder.uuid != null) {
			this.poiStatusViewHoldersWR.remove(holder.uuid);
		}
		holder.uuid = poim.poi.getUUID();
		if (holder.uuid != null) {
			this.poiStatusViewHoldersWR.put(holder.uuid, holder.statusViewHolder);
		}
		holder.compassV.setLatLng(poim.getLat(), poim.getLng());
		// name
		holder.nameTv.setText(poim.poi.getName());
		// distance
		if (!TextUtils.isEmpty(poim.getDistanceString())) {
			if (!poim.getDistanceString().equals(holder.distanceTv.getText())) {
				holder.distanceTv.setText(poim.getDistanceString());
			}
			holder.distanceTv.setVisibility(View.VISIBLE);
		} else {
			holder.distanceTv.setVisibility(View.GONE);
			holder.distanceTv.setText(null);
		}
		// compass (if distance available)
		if (holder.distanceTv.getVisibility() == View.VISIBLE && this.location != null && this.lastCompassInDegree >= 0
				&& this.location.getAccuracy() <= poim.getDistance()) {
			float compassRotation = SensorUtils.getCompassRotationInDegree(this.location.getLatitude(), this.location.getLongitude(), poim.getLat(),
					poim.getLng(), this.lastCompassInDegree, this.locationDeclination);
			holder.compassV.setHeadingInDegree((int) compassRotation);
			holder.compassV.setVisibility(View.VISIBLE);
		} else {
			holder.compassV.setVisibility(View.GONE);
		}
		// favorite
		if (this.favUUIDs != null && this.favUUIDs.contains(poim.poi.getUUID())) {
			holder.favImg.setVisibility(View.VISIBLE);
		} else {
			holder.favImg.setVisibility(View.GONE);
		}
	}

	private MTAsyncTask<Integer, Void, List<Favorite>> refreshFavoritesTask;

	public void refreshFavorites(Integer... typesFilter) {
		if (this.refreshFavoritesTask != null && this.refreshFavoritesTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return; // skipped, last refresh still in progress so probably good enough
		}
		this.refreshFavoritesTask = new MTAsyncTask<Integer, Void, List<Favorite>>() {
			@Override
			public String getLogTag() {
				return POIArrayAdapter.this.getLogTag();
			}

			@Override
			protected List<Favorite> doInBackgroundMT(Integer... params) {
				return FavoriteManager.findFavorites(POIArrayAdapter.this.getContext(), params);
			}

			@Override
			protected void onPostExecute(List<Favorite> result) {
				setFavorites(result);
			};
		};
		this.refreshFavoritesTask.execute(typesFilter);

	}

	public void setFavorites(List<Favorite> favorites) {
		boolean newFav = false; // don't trigger update if favorites are the same
		if (CollectionUtils.getSize(favorites) != CollectionUtils.getSize(this.favUUIDs)) {
			newFav = true; // different size => different favorites
		}
		Set<String> newFavUUIDs = new HashSet<String>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				final String uid = favorite.getFkId();
				if (!newFav && this.favUUIDs != null && !this.favUUIDs.contains(uid)) {
					newFav = true;
				}
				newFavUUIDs.add(uid);
				break;
			}
		}
		this.favUUIDs = newFavUUIDs;
		if (newFav) {
			notifyDataSetChanged(true);
			prefetchFavorites();
		}
	}

	public static class RouteTripStopViewHolder extends CommonViewHolder {
		TextView routeShortNameTv;
		View routeFL;
		View rtsExtraV;
		TextView tripHeadingTv;
	}

	public static class BasicPOIViewHolder extends CommonViewHolder {
	}

	public static class CommonViewHolder {
		String uuid;
		View view;
		TextView nameTv;
		TextView distanceTv;
		ImageView favImg;
		MTCompassView compassV;
		CommonStatusViewHolder statusViewHolder;
	}

	public static class ScheduleStatusViewHolder extends CommonStatusViewHolder {
		TextView dataNextLine1Tv;
		TextView dataNextLine2Tv;
	}

	public static class AvailabilityPercentStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv;
		MTPieChartPercentView piePercentV;
	}

	public static class CommonStatusViewHolder {
		View statusV;
	}

}
