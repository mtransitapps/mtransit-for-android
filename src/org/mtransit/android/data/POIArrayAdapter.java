package org.mtransit.android.data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SensorUtils;
import org.mtransit.android.commons.SpanUtils;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.commons.ui.widget.MTArrayAdapter;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.fragment.AgencyTypeFragment;
import org.mtransit.android.ui.fragment.NearbyFragment;
import org.mtransit.android.ui.fragment.RTSRouteFragment;
import org.mtransit.android.ui.view.MTCompassView;
import org.mtransit.android.ui.view.MTJPathsView;
import org.mtransit.android.ui.view.MTPieChartPercentView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class POIArrayAdapter extends MTArrayAdapter<POIManager> implements SensorUtils.CompassListener, AdapterView.OnItemClickListener,
		AdapterView.OnItemLongClickListener, SensorEventListener, AbsListView.OnScrollListener, StatusLoader.StatusLoaderListener,
		ServiceUpdateLoader.ServiceUpdateLoaderListener, FavoriteManager.FavoriteUpdateListener, SensorUtils.SensorTaskCompleted,
		TimeUtils.TimeChangedReceiver.TimeChangedListener {

	private static final String TAG = POIArrayAdapter.class.getSimpleName();

	private String tag = TAG;

	@Override
	public String getLogTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = TAG + "-" + tag;
	}

	public static final int TYPE_HEADER_NONE = 0;
	public static final int TYPE_HEADER_BASIC = 1;
	public static final int TYPE_HEADER_ALL_NEARBY = 2;
	public static final int TYPE_HEADER_MORE = 3;

	private LayoutInflater layoutInflater;

	private LinkedHashMap<Integer, ArrayList<POIManager>> poisByType;

	private HashSet<String> favUUIDs;

	private WeakReference<Activity> activityWR;

	private Location location;

	private int lastCompassInDegree = -1;

	private float locationDeclination;

	private HashSet<String> closestPoiUuids;

	private float[] accelerometerValues = new float[3];

	private float[] magneticFieldValues = new float[3];


	private boolean showStatus = true; // show times / availability

	private boolean showServiceUpdate = true; // show warning icon

	private boolean showFavorite = true; // show favorite star

	private boolean showBrowseHeaderSection = false; // show header with shortcut to agency type screens

	private int showTypeHeader = TYPE_HEADER_NONE;

	private boolean showTypeHeaderNearby = false; // show nearby header instead of default type header


	private ViewGroup manualLayout;

	private ScrollView manualScrollView;

	private long lastNotifyDataSetChanged = -1l;

	private int scrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

	private long nowToTheMinute = -1l;

	private boolean timeChangedReceiverEnabled = false;

	private boolean compassUpdatesEnabled = false;

	private long lastCompassChanged = -1l;

	private FavoriteManager.FavoriteUpdateListener favoriteUpdateListener = this;

	public POIArrayAdapter(Activity activity) {
		super(activity, -1);
		this.activityWR = new WeakReference<Activity>(activity);
		this.layoutInflater = LayoutInflater.from(getContext());
	}

	public void setManualLayout(ViewGroup manualLayout) {
		this.manualLayout = manualLayout;
	}

	public void setFavoriteUpdateListener(FavoriteManager.FavoriteUpdateListener favoriteUpdateListener) {
		this.favoriteUpdateListener = favoriteUpdateListener;
	}

	public void setShowStatus(boolean showData) {
		this.showStatus = showData;
	}

	public void setShowServiceUpdate(boolean showServiceUpdate) {
		this.showServiceUpdate = showServiceUpdate;
	}

	public void setShowFavorite(boolean showFavorite) {
		this.showFavorite = showFavorite;
	}

	public void setShowBrowseHeaderSection(boolean showBrowseHeaderSection) {
		this.showBrowseHeaderSection = showBrowseHeaderSection;
	}

	public void setShowTypeHeader(int showTypeHeader) {
		this.showTypeHeader = showTypeHeader;
	}

	public void setShowTypeHeaderNearby(boolean showTypeHeaderNearby) {
		this.showTypeHeaderNearby = showTypeHeaderNearby;
	}


	private static final int VIEW_TYPE_COUNT = 8;

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
			if (this.showBrowseHeaderSection && position == 0) {
				return 0;
			}
			if (this.showTypeHeader != TYPE_HEADER_NONE) {
				if (this.poisByType != null) {
					Integer type = getItemTypeHeader(position);
					if (type != null) {
						return 7; // TYPE HEADER
					}
				}
			}
			MTLog.d(this, "Cannot find type for at position '%s'!", position);
			return Adapter.IGNORE_ITEM_VIEW_TYPE;
		}
		int type = poim.poi.getType();
		int statusType = poim.getStatusType();
		switch (type) {
		case POI.ITEM_VIEW_TYPE_MODULE:
			switch (statusType) {
			case POI.ITEM_STATUS_TYPE_SCHEDULE:
				return 5; // MODULE & APP STATUS
			default:
				return 6; // MODULE
			}
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
				return 1; // DEFAULT & AVAILABILITY %
			default:
				return 2; // DEFAULT
			}
		}
	}

	@Override
	public int getCount() {
		int count = 0;
		if (this.poisByType != null) {
			if (this.showBrowseHeaderSection) {
				count++;
			}
			for (Integer type : this.poisByType.keySet()) {
				if (this.showTypeHeader != TYPE_HEADER_NONE) {
					count++;
				}
				count += this.poisByType.get(type).size();
			}
		}
		return count;
	}

	@Override
	public int getPosition(POIManager item) {
		int position = 0;
		if (this.poisByType != null) {
			if (this.showBrowseHeaderSection) {
				position++;
			}
			for (Integer type : this.poisByType.keySet()) {
				if (this.showTypeHeader != TYPE_HEADER_NONE) {
					position++;
				}
				int indexOf = this.poisByType.get(type).indexOf(item);
				if (indexOf >= 0) {
					return position + indexOf;
				}
				position += this.poisByType.get(type).size();
			}
		}
		return position;
	}

	@Override
	public POIManager getItem(int position) {
		if (this.poisByType != null) {
			int index = 0;
			if (this.showBrowseHeaderSection) {
				index++;
			}
			for (Integer type : this.poisByType.keySet()) {
				if (this.showTypeHeader != TYPE_HEADER_NONE) {
					index++;
				}
				if (position >= index && position < index + this.poisByType.get(type).size()) {
					return this.poisByType.get(type).get(position - index);
				}
				index += this.poisByType.get(type).size();
			}
		}
		return null;
	}

	public POIManager getItem(String uuid) {
		if (this.poisByType != null) {
			for (Integer type : this.poisByType.keySet()) {
				for (POIManager poim : this.poisByType.get(type)) {
					if (poim.poi.getUUID().equals(uuid)) {
						return poim;
					}
				}
			}
		}
		return null;
	}

	public Integer getItemTypeHeader(int position) {
		if (this.showTypeHeader != TYPE_HEADER_NONE && this.poisByType != null) {
			int index = 0;
			if (this.showBrowseHeaderSection) {
				index++;
			}
			for (Integer type : this.poisByType.keySet()) {
				if (index == position) {
					return type;
				}
				index++;
				index += this.poisByType.get(type).size();
			}
		}
		return null;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		POIManager poim = getItem(position);
		if (poim == null) {
			if (this.showBrowseHeaderSection && position == 0) {
				return getBrowseHeaderSectionView(convertView, parent);
			}
			if (this.showTypeHeader != TYPE_HEADER_NONE) {
				Integer typeId = getItemTypeHeader(position);
				if (typeId != null) {
					DataSourceType dst = DataSourceType.parseId(typeId);
					if (dst != null) {
						return getTypeHeaderView(dst, convertView, parent);
					}
				}
			}
			MTLog.w(this, "getView() > Cannot create view for null poi at position '%s'!", position);
			return null; // CRASH!!!
		}
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return getRouteTripStopView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOIView(poim, convertView, parent);
		default:
			MTLog.w(this, "getView() > Unknow view type at position %s!", position);
			return null; // CRASH!!!
		}
	}

	private ArrayList<DataSourceType> allAgencyTypes = null;

	private View getBrowseHeaderSectionView(View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.layout_poi_list_browse_header, parent, false);
		}
		if (this.allAgencyTypes == null) {
			LinearLayout gridLL = (LinearLayout) convertView.findViewById(R.id.gridLL);
			gridLL.removeAllViews();
			Activity activity = this.activityWR == null ? null : this.activityWR.get();
			this.allAgencyTypes = DataSourceProvider.get(activity).getAvailableAgencyTypes();
			if (this.allAgencyTypes == null || this.allAgencyTypes.size() <= 1) {
				gridLL.setVisibility(View.GONE);
			} else {
				int availableButtons = 0;
				View gridLine = null;
				for (final DataSourceType dst : this.allAgencyTypes) {
					if (availableButtons == 0 && dst.getId() == DataSourceType.TYPE_MODULE.getId()) {
						continue;
					}
					if (dst.getId() == DataSourceType.TYPE_PLACE.getId()) {
						continue;
					}
					if (availableButtons == 0) {
						gridLine = this.layoutInflater.inflate(R.layout.layout_poi_list_browse_header_line, this.manualLayout, false);
						gridLL.addView(gridLine);
						availableButtons = 2;
					}
					View btn = gridLine.findViewById(availableButtons == 2 ? R.id.btn1 : R.id.btn2);
					TextView btnTv = (TextView) gridLine.findViewById(availableButtons == 2 ? R.id.btn1Tv : R.id.btn2Tv);
					btnTv.setText(dst.getAllStringResId());
					if (dst.getAbIconResId() != -1) {
						btnTv.setCompoundDrawablesWithIntrinsicBounds(dst.getAbIconResId(), 0, 0, 0);
					} else {
						btnTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
					}
					btn.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							onTypeHeaderButtonClick(TypeHeaderButtonsClickListener.BUTTON_ALL, dst);
						}
					});
					btn.setVisibility(View.VISIBLE);
					availableButtons--;
				}
				gridLL.setVisibility(View.VISIBLE);
			}
		}
		return convertView;
	}

	private void updateCommonViewManual(int position, View convertView) {
		if (convertView == null || convertView.getTag() == null || !(convertView.getTag() instanceof CommonViewHolder)) {
			return;
		}
		CommonViewHolder holder = (CommonViewHolder) convertView.getTag();
		POIManager poim = getItem(position);
		updateCommonView(holder, poim);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		showPoiViewerScreen(position);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		showPoiMenu(position);
		return true; // handled
	}


	public static interface OnPOISelectedListener {
		public boolean onPOISelected(POIManager poim);
	}

	private WeakReference<OnPOISelectedListener> onPoiSelectedListenerWR;

	public void setOnPoiSelectedListener(OnPOISelectedListener onPoiSelectedListener) {
		this.onPoiSelectedListenerWR = new WeakReference<POIArrayAdapter.OnPOISelectedListener>(onPoiSelectedListener);
	}

	public void showPoiViewerScreen(int position) {
		new MTAsyncTask<Integer, Void, POIManager>() {

			@Override
			public String getLogTag() {
				return POIArrayAdapter.class.getSimpleName() + ">showPoiViewerScreen";
			}

			@Override
			protected POIManager doInBackgroundMT(Integer... params) {
				return getItem(params[0]);
			}

			@Override
			protected void onPostExecute(POIManager poim) {
				if (poim != null) {
					OnPOISelectedListener listerner = POIArrayAdapter.this.onPoiSelectedListenerWR == null ? null
							: POIArrayAdapter.this.onPoiSelectedListenerWR.get();
					if (listerner == null || !listerner.onPOISelected(poim)) {
						showPoiViewerScreen(poim);
					}
				}
			}
		}.execute(position);
	}

	public void showPoiMenu(int position) {
		new MTAsyncTask<Integer, Void, POIManager>() {

			@Override
			public String getLogTag() {
				return POIArrayAdapter.class.getSimpleName() + ">showPoiMenu";
			}

			@Override
			protected POIManager doInBackgroundMT(Integer... params) {
				return getItem(params[0]);
			}

			@Override
			protected void onPostExecute(POIManager poim) {
				if (poim != null) {
					OnPOISelectedListener listerner = POIArrayAdapter.this.onPoiSelectedListenerWR == null ? null
							: POIArrayAdapter.this.onPoiSelectedListenerWR.get();
					if (listerner == null || !listerner.onPOISelected(poim)) {
						showPoiMenu(poim);
					}
				}
			}
		}.execute(position);
	}


	@Override
	public boolean areAllItemsEnabled() {
		return false; // to hide divider around disabled items (list view background visible behind hidden divider)
		// return true; // to show divider around disabled items
	}

	@Override
	public boolean isEnabled(int position) {
		Integer type = getItemTypeHeader(position);
		if (type != null) {
			return false;
		}
		return true;
	}

	public void showPoiViewerScreen(POIManager poim) {
		if (poim == null) {
			return;
		}
		Activity activity = this.activityWR == null ? null : this.activityWR.get();
		if (activity == null) {
			return;
		}
		poim.onActionItemClick(activity, this.favoriteUpdateListener);
	}

	public void showPoiMenu(POIManager poim) {
		if (poim == null) {
			return;
		}
		Activity activity = this.activityWR == null ? null : this.activityWR.get();
		if (activity == null) {
			return;
		}
		poim.onActionItemLongClick(activity, this.favoriteUpdateListener);
	}

	@Override
	public void onFavoriteUpdated() {
		refreshFavorites();
	}

	public void setPois(ArrayList<POIManager> pois) {
		this.lastNotifyDataSetChanged = -1; // last notify was with old data
		this.allAgencyTypes = null;
		this.poisByType = null;
		if (pois != null) {
			this.poisByType = new LinkedHashMap<Integer, ArrayList<POIManager>>();
			for (POIManager poim : pois) {
				AgencyProperties agency = DataSourceProvider.get(getContext()).getAgency(getContext(), poim.poi.getAuthority());
				if (agency != null) {
					Integer typeId = agency.getType().getId();
					if (!this.poisByType.containsKey(typeId)) {
						this.poisByType.put(typeId, new ArrayList<POIManager>());
					}
					this.poisByType.get(typeId).add(poim);
				}
			}
		}
		refreshFavorites();
		updateClosestPoi();
	}

	public boolean isInitialized() {
		return this.poisByType != null;
	}

	public int getPoisCount() {
		int count = 0;
		if (this.poisByType != null) {
			for (Integer type : this.poisByType.keySet()) {
				count += this.poisByType.get(type).size();
			}
		}
		return count;
	}

	public boolean hasPois() {
		return getPoisCount() > 0;
	}

	private void updateClosestPoi() {
		if (getPoisCount() == 0) {
			this.closestPoiUuids = null;
			return;
		}
		if (this.poisByType != null) {
			this.closestPoiUuids = new HashSet<String>();
			for (Integer type : this.poisByType.keySet()) {
				ArrayList<POIManager> orderedPoims = new ArrayList<POIManager>(this.poisByType.get(type));
				CollectionUtils.sort(orderedPoims, LocationUtils.POI_DISTANCE_COMPARATOR);
				POIManager theClosestOne = orderedPoims.get(0);
				float theClosestDistance = theClosestOne.getDistance();
				if (theClosestDistance > 0) {
					for (POIManager poim : orderedPoims) {
						if (poim.getDistance() <= theClosestDistance) {
							this.closestPoiUuids.add(poim.poi.getUUID());
							continue;
						}
						break;
					}
				}
			}
		}
	}

	public boolean hasClosestPOI() {
		return this.closestPoiUuids != null && this.closestPoiUuids.size() > 0;
	}

	public boolean isClosestPOI(int position) {
		if (this.closestPoiUuids == null) {
			return false;
		}
		POIManager poim = getItem(position);
		return poim != null && this.closestPoiUuids.contains(poim.poi.getUUID());
	}

	public POIManager getClosestPOI() {
		if (this.closestPoiUuids == null || this.closestPoiUuids.size() == 0) {
			return null;
		}
		String closestPOIUUID = this.closestPoiUuids.iterator().next();
		return getItem(closestPOIUUID);
	}


	private MTAsyncTask<Location, Void, Void> updateDistanceWithStringTask;

	private void updateDistances(Location currentLocation) {
		if (this.updateDistanceWithStringTask != null) {
			this.updateDistanceWithStringTask.cancel(true);
		}
		if (this.poisByType != null && currentLocation != null) {
			this.updateDistanceWithStringTask = new MTAsyncTask<Location, Void, Void>() {

				@Override
				public String getLogTag() {
					return POIArrayAdapter.this.getLogTag();
				}

				@Override
				protected Void doInBackgroundMT(Location... params) {
					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
					if (POIArrayAdapter.this.poisByType != null) {
						for (ArrayList<POIManager> pois : POIArrayAdapter.this.poisByType.values()) {
							LocationUtils.updateDistanceWithString(POIArrayAdapter.this.getContext(), pois, params[0], this);
						}
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					if (isCancelled()) {
						return;
					}
					updateClosestPoi();
					notifyDataSetChanged(true);
				}
			};
			this.updateDistanceWithStringTask.execute(currentLocation);
		}
	}

	public void updateDistancesNowSync(Location currentLocation) {
		if (this.poisByType != null && currentLocation != null) {
			for (ArrayList<POIManager> pois : this.poisByType.values()) {
				LocationUtils.updateDistanceWithString(getContext(), pois, currentLocation, null);
			}
			updateClosestPoi();
		}
		setLocation(currentLocation);
	}

	public void updateDistanceNowAsync(Location currentLocation) {
		this.location = null; // clear current location to force refresh
		setLocation(currentLocation);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		setScrollState(scrollState);
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

	@Override
	public void onServiceUpdatesLoaded(String targetUUID, ArrayList<ServiceUpdate> serviceUpdates) {
		if (this.showServiceUpdate) {
			if (this.poiStatusViewHoldersWR != null && this.poiStatusViewHoldersWR.containsKey(targetUUID)) {
				updateServiceUpdate(this.poiStatusViewHoldersWR.get(targetUUID), ServiceUpdate.isSeverityWarning(serviceUpdates));
			}
		}
	}

	public void notifyDataSetChanged(boolean force) {
		notifyDataSetChanged(force, Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS);
	}

	private Handler handler = new Handler();

	private Runnable notifyDataSetChangedLater = new Runnable() {

		@Override
		public void run() {
			notifyDataSetChanged(true); // still really need to show new data
		}
	};

	public void notifyDataSetChanged(boolean force, int minAdapterThresoldInMs) {
		long now = TimeUtils.currentTimeMillis();
		long adapterThreasold = Math.max(minAdapterThresoldInMs, Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS);
		if (this.scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && (force || (now - this.lastNotifyDataSetChanged) > adapterThreasold)) {
			notifyDataSetChanged();
			notifyDataSetChangedManual();
			this.lastNotifyDataSetChanged = now;
			this.handler.removeCallbacks(this.notifyDataSetChangedLater);
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
				if (view instanceof FrameLayout) {
					view = ((FrameLayout) view).getChildAt(0);
				}
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
				FrameLayout frameLayout = new FrameLayout(getContext());
				frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				frameLayout.addView(view);
				View selectorView = new View(getContext());
				SupportFactory.get().setBackground(selectorView, ThemeUtils.obtainStyledDrawable(getContext(), R.attr.selectableItemBackground));
				selectorView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
				frameLayout.addView(selectorView);
				final int position = i;
				frameLayout.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showPoiViewerScreen(position);
					}
				});
				frameLayout.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						showPoiMenu(position);
						return true; // handled
					}
				});
				this.manualLayout.addView(frameLayout);
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
		if (this.activityWR != null) {
			this.activityWR.clear();
			this.activityWR = null;
		}
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

	public void onResume(Activity activity) {
		this.activityWR = new WeakReference<Activity>(activity);
		if (!this.compassUpdatesEnabled) {
			SensorUtils.registerCompassListener(getContext(), this);
			this.compassUpdatesEnabled = true;
		}
		refreshFavorites();
	}

	public void setActivity(Activity activity) {
		this.activityWR = new WeakReference<Activity>(activity);
	}

	@Override
	public void clear() {
		if (this.poisByType != null) {
			this.poisByType.clear();
			this.poisByType = null;
		}
		if (this.closestPoiUuids != null) {
			this.closestPoiUuids.clear();
			this.closestPoiUuids = null;
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
		super.clear();
	}

	public void onDestroy() {
		disableTimeChangeddReceiver();
		if (this.poisByType != null) {
			this.poisByType.clear();
			this.poisByType = null;
		}
		this.compassImgsWR.clear();
		this.poiStatusViewHoldersWR.clear();
		this.allAgencyTypes = null;
	}

	@Override
	public void updateCompass(float orientation, boolean force) {
		if (this.poisByType == null) {
			return;
		}
		long now = TimeUtils.currentTimeMillis();
		int roundedOrientation = SensorUtils.convertToPosivite360Degree((int) orientation);
		SensorUtils.updateCompass(force, this.location, roundedOrientation, now, this.scrollState, this.lastCompassChanged, this.lastCompassInDegree,
				Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS, this);
	}

	@Override
	public void onSensorTaskCompleted(boolean result, int roundedOrientation, long now) {
		if (result) {
			this.lastCompassInDegree = roundedOrientation;
			this.lastCompassChanged = now;
			if (this.compassUpdatesEnabled && this.location != null && this.lastCompassInDegree >= 0) {
				if (this.compassImgsWR.size() > 0) {
					for (WeakHashMap.Entry<MTCompassView, View> compassAndDistance : this.compassImgsWR.entrySet()) {
						MTCompassView compassView = compassAndDistance.getKey();
						if (compassView != null) {
							compassView.generateAndSetHeading(this.location, this.lastCompassInDegree, this.locationDeclination);
							if (compassView.getVisibility() != View.VISIBLE) {
								View distanceView = compassAndDistance.getValue();
								if (distanceView != null && distanceView.getVisibility() == View.VISIBLE) {
									compassView.setVisibility(View.VISIBLE);
								}
							}
						}
					}
				} else {
					notifyDataSetChanged(true); // weak reference list not set
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

	private int getTypeHeaderLayoutResId() {
		switch (this.showTypeHeader) {
		case TYPE_HEADER_BASIC:
			return R.layout.layout_poi_list_header;
		case TYPE_HEADER_MORE:
			return R.layout.layout_poi_list_header_with_more;
		case TYPE_HEADER_ALL_NEARBY:
			return R.layout.layout_poi_list_header_with_all_nearby;
		default:
			MTLog.w(this, "Unexected header type '%s'!", this.showTypeHeader);
			return R.layout.layout_poi_list_header;
		}
	}

	private WeakReference<TypeHeaderButtonsClickListener> typeHeaderButtonsClickListenerWR;

	public void setOnTypeHeaderButtonsClickListener(TypeHeaderButtonsClickListener listener) {
		this.typeHeaderButtonsClickListenerWR = new WeakReference<TypeHeaderButtonsClickListener>(listener);
	}

	private void onTypeHeaderButtonClick(int buttonId, DataSourceType type) {
		TypeHeaderButtonsClickListener listener = POIArrayAdapter.this.typeHeaderButtonsClickListenerWR == null ? null
				: POIArrayAdapter.this.typeHeaderButtonsClickListenerWR.get();
		if (listener != null && listener.onTypeHeaderButtonClick(buttonId, type)) {
			return;
		}
		switch (buttonId) {
		case TypeHeaderButtonsClickListener.BUTTON_ALL:
			if (type != null) {
				Activity activity = this.activityWR == null ? null : this.activityWR.get();
				if (activity != null) {
					((MainActivity) activity).addFragmentToStack(AgencyTypeFragment.newInstance(type.getId(), type));
				}
			}
			break;
		case TypeHeaderButtonsClickListener.BUTTON_NEARBY:
			if (type != null) {
				Activity activity = this.activityWR == null ? null : this.activityWR.get();
				if (activity != null) {
					((MainActivity) activity).addFragmentToStack(NearbyFragment.newNearbyInstance(null, type.getId()));
				}
			}
			break;
		case TypeHeaderButtonsClickListener.BUTTON_MORE:
			if (type != null) {
				Activity activity = this.activityWR == null ? null : this.activityWR.get();
				if (activity != null) {
					((MainActivity) activity).addFragmentToStack(AgencyTypeFragment.newInstance(type.getId(), type));
				}
			}
			break;
		default:
			MTLog.w(this, "Unexected type header button %s'' click", type);
		}
	}

	private View getTypeHeaderView(final DataSourceType type, View convertView, ViewGroup parent) {
		if (convertView == null) {
			int layoutRes = getTypeHeaderLayoutResId();
			convertView = this.layoutInflater.inflate(layoutRes, parent, false);
			TypeHeaderViewHolder holder = new TypeHeaderViewHolder();
			holder.nameTv = (TextView) convertView.findViewById(R.id.name);
			holder.nearbyBtn = convertView.findViewById(R.id.nearbyBtn);
			holder.allBtn = convertView.findViewById(R.id.allBtn);
			holder.allBtnTv = (TextView) convertView.findViewById(R.id.allBtnTv);
			holder.moreBtn = convertView.findViewById(R.id.moreBtn);
			convertView.setTag(holder);
		}
		TypeHeaderViewHolder holder = (TypeHeaderViewHolder) convertView.getTag();
		holder.nameTv.setText(this.showTypeHeaderNearby ? type.getNearbyNameResId() : type.getPoiShortNameResId());
		if (type.getMenuResId() != -1) {
			holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(type.getMenuResId(), 0, 0, 0);
		}
		if (holder.allBtn != null) {
			holder.allBtn.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					onTypeHeaderButtonClick(TypeHeaderButtonsClickListener.BUTTON_ALL, type);
				}
			});
		}
		if (holder.nearbyBtn != null) {
			holder.nearbyBtn.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					onTypeHeaderButtonClick(TypeHeaderButtonsClickListener.BUTTON_NEARBY, type);
				}
			});
		}
		if (holder.moreBtn != null) {
			holder.moreBtn.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					onTypeHeaderButtonClick(TypeHeaderButtonsClickListener.BUTTON_MORE, type);
				}
			});
		}
		return convertView;
	}

	private WeakHashMap<String, CommonStatusViewHolder> poiStatusViewHoldersWR = new WeakHashMap<String, CommonStatusViewHolder>();

	private View getBasicPOIView(POIManager poim, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getBasicPOILayout(poim.getStatusType()), parent, false);
			BasicPOIViewHolder holder = new BasicPOIViewHolder();
			initCommonViewHolder(holder, convertView, poim.poi.getUUID());
			holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), convertView);
			this.poiStatusViewHoldersWR.put(holder.uuid, holder.statusViewHolder);
			convertView.setTag(holder);
		}
		updateBasicPOIView(poim, convertView);
		return convertView;
	}

	private CommonStatusViewHolder initPOIStatusViewHolder(int status, View convertView) {
		switch (status) {
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			return initAvailabilityPercentViewHolder(convertView);
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			return initScheduleViewHolder(convertView);
		case POI.ITEM_STATUS_TYPE_APP:
			return initAppStatusViewHolder(convertView);
		default:
			MTLog.w(this, "Unexpected status '%s' (no view holder)!", status);
			return null;
		}
	}

	private CommonStatusViewHolder initScheduleViewHolder(View convertView) {
		ScheduleStatusViewHolder scheduleStatusViewHolder = new ScheduleStatusViewHolder();
		initCommonStatusViewHolderHolder(scheduleStatusViewHolder, convertView);
		scheduleStatusViewHolder.dataNextLine1Tv = (TextView) convertView.findViewById(R.id.data_next_line_1);
		scheduleStatusViewHolder.dataNextLine2Tv = (TextView) convertView.findViewById(R.id.data_next_line_2);
		return scheduleStatusViewHolder;
	}

	private CommonStatusViewHolder initAppStatusViewHolder(View convertView) {
		AppStatusViewHolder appStatusViewHolder = new AppStatusViewHolder();
		initCommonStatusViewHolderHolder(appStatusViewHolder, convertView);
		appStatusViewHolder.textTv = (TextView) convertView.findViewById(R.id.textTv);
		return appStatusViewHolder;
	}

	private CommonStatusViewHolder initAvailabilityPercentViewHolder(View convertView) {
		AvailabilityPercentStatusViewHolder availabilityPercentStatusViewHolder = new AvailabilityPercentStatusViewHolder();
		initCommonStatusViewHolderHolder(availabilityPercentStatusViewHolder, convertView);
		availabilityPercentStatusViewHolder.textTv = (TextView) convertView.findViewById(R.id.textTv);
		availabilityPercentStatusViewHolder.piePercentV = (MTPieChartPercentView) convertView.findViewById(R.id.pie);
		return availabilityPercentStatusViewHolder;
	}

	private int getBasicPOILayout(int status) {
		int layoutRes = R.layout.layout_poi_basic;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			layoutRes = R.layout.layout_poi_basic_with_availability_percent;
			break;
		default:
			MTLog.w(this, "Unexpected status '%s' (basic view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	private WeakHashMap<MTCompassView, View> compassImgsWR = new WeakHashMap<MTCompassView, View>();

	private void initCommonViewHolder(CommonViewHolder holder, View convertView, String poiUUID) {
		holder.uuid = poiUUID;
		holder.view = convertView;
		holder.nameTv = (TextView) convertView.findViewById(R.id.name);
		holder.favImg = (ImageView) convertView.findViewById(R.id.fav);
		holder.locationTv = (TextView) convertView.findViewById(R.id.location);
		holder.distanceTv = (TextView) convertView.findViewById(R.id.distance);
		holder.compassV = (MTCompassView) convertView.findViewById(R.id.compass);
	}

	private static void initCommonStatusViewHolderHolder(CommonStatusViewHolder holder, View convertView) {
		holder.statusV = convertView.findViewById(R.id.status);
		holder.warningImg = (ImageView) convertView.findViewById(R.id.service_update_warning);
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

	private void updateAppStatus(CommonStatusViewHolder statusViewHolder, POIManager poim) {
		if (this.showStatus && poim != null && statusViewHolder instanceof AppStatusViewHolder) {
			poim.setStatusLoaderListener(this);
			updateAppStatus(statusViewHolder, poim.getStatus(getContext()));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
		if (poim != null) {
			poim.setServiceUpdateLoaderListener(this);
			updateServiceUpdate(statusViewHolder, poim.isServiceUpdateWarning(getContext()));
		}
	}

	private void updateAppStatus(CommonStatusViewHolder statusViewHolder, POIStatus status) {
		AppStatusViewHolder appStatusViewHolder = (AppStatusViewHolder) statusViewHolder;
		if (status != null && status instanceof AppStatus) {
			AppStatus appStatus = (AppStatus) status;
			appStatusViewHolder.textTv.setText(appStatus.getStatusMsg(getContext()));
			appStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private void updateServiceUpdate(CommonStatusViewHolder statusViewHolder, Boolean isServiceUpdateWarning) {
		if (statusViewHolder.warningImg == null) {
			return;
		}
		if (this.showServiceUpdate && isServiceUpdateWarning != null) {
			statusViewHolder.warningImg.setVisibility(isServiceUpdateWarning ? View.VISIBLE : View.GONE);
		} else {
			statusViewHolder.warningImg.setVisibility(View.GONE);
		}
	}

	private void updateAvailabilityPercent(CommonStatusViewHolder statusViewHolder, POIManager poim) {
		if (this.showStatus && poim != null && statusViewHolder instanceof AvailabilityPercentStatusViewHolder) {
			poim.setStatusLoaderListener(this);
			updateAvailabilityPercent(statusViewHolder, poim.getStatus(getContext()));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
		if (poim != null) {
			poim.setServiceUpdateLoaderListener(this);
			updateServiceUpdate(statusViewHolder, poim.isServiceUpdateWarning(getContext()));
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
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private int getRTSLayout(int status) {
		int layoutRes = R.layout.layout_poi_rts;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			layoutRes = R.layout.layout_poi_rts_with_schedule;
			break;
		default:
			MTLog.w(this, "Unexpected status '%s' (rts view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	private View getModuleView(POIManager poim, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getModuleLayout(poim.getStatusType()), parent, false);
			ModuleViewHolder holder = new ModuleViewHolder();
			initCommonViewHolder(holder, convertView, poim.poi.getUUID());
			initModuleExtra(convertView, holder);
			holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), convertView);
			this.poiStatusViewHoldersWR.put(holder.uuid, holder.statusViewHolder);
			convertView.setTag(holder);
		}
		updateModuleView(poim, convertView);
		return convertView;
	}

	private void initModuleExtra(View convertView, ModuleViewHolder holder) {
		holder.moduleTypeImg = (ImageView) convertView.findViewById(R.id.module_extra);
	}

	private View updateModuleView(POIManager poim, View convertView) {
		if (convertView == null || poim == null) {
			return convertView;
		}
		ModuleViewHolder holder = (ModuleViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		updateModuleExtra(poim, holder);
		updatePOIStatus(holder.statusViewHolder, poim);
		return convertView;
	}

	private void updateModuleExtra(POIManager poim, ModuleViewHolder holder) {
		if (this.showExtra && poim.poi != null && poim.poi instanceof Module) {
			Module module = (Module) poim.poi;
			holder.moduleTypeImg.setBackgroundColor(poim.getColor(getContext()));
			DataSourceType moduleType = DataSourceType.parseId(module.getTargetTypeId());
			if (moduleType != null) {
				holder.moduleTypeImg.setImageResource(moduleType.getAbIconResId());
			} else {
				holder.moduleTypeImg.setImageResource(0);
			}
			holder.moduleTypeImg.setVisibility(View.VISIBLE);
		} else {
			holder.moduleTypeImg.setVisibility(View.GONE);
		}
	}

	private int getModuleLayout(int status) {
		int layoutRes = R.layout.layout_poi_module;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_APP:
			layoutRes = R.layout.layout_poi_module_with_app_status;
			break;
		default:
			MTLog.w(this, "Unexpected status '%s' (module view w/o status)!", status);
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
			holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), convertView);
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
		holder.routeTypeImg = (MTJPathsView) convertView.findViewById(R.id.route_type_img);
		holder.tripHeadingTv = (TextView) convertView.findViewById(R.id.trip_heading);
		holder.tripHeadingBg = convertView.findViewById(R.id.trip_heading_bg);
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

	private boolean showExtra = true;

	public void setShowExtra(boolean showExtra) {
		this.showExtra = showExtra;
	}

	private void updateRTSExtra(POIManager poim, RouteTripStopViewHolder holder) {
		if (poim.poi instanceof RouteTripStop) {
			RouteTripStop rts = (RouteTripStop) poim.poi;
			if (!this.showExtra || rts.route == null) {
				holder.rtsExtraV.setVisibility(View.GONE);
				holder.routeFL.setVisibility(View.GONE);
				holder.tripHeadingBg.setVisibility(View.GONE);
			} else {
				final String authority = rts.getAuthority();
				final Route route = rts.route;
				int routeTextColor = Color.WHITE;
				if (TextUtils.isEmpty(rts.route.shortName)) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
					JPaths rtsRouteLogo = DataSourceProvider.get(getContext()).getRTSAgencyRouteLogo(getContext(), poim.poi.getAuthority());
					if (rtsRouteLogo != null) {
						holder.routeTypeImg.setJSON(rtsRouteLogo);
						holder.routeTypeImg.setColor(routeTextColor);
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						holder.routeTypeImg.setVisibility(View.GONE);
					}
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					SpannableStringBuilder ssb = new SpannableStringBuilder(rts.route.shortName);
					if (ssb.length() > 3) {
						SpanUtils.set(ssb, SpanUtils.FIFTY_PERCENT_SIZE_SPAN);
						holder.routeShortNameTv.setSingleLine(false);
						holder.routeShortNameTv.setMaxLines(2);
					} else {
						holder.routeShortNameTv.setSingleLine(true);
						holder.routeShortNameTv.setMaxLines(1);
					}
					holder.routeShortNameTv.setText(ssb);
					holder.routeShortNameTv.setTextColor(routeTextColor);
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				holder.routeFL.setVisibility(View.VISIBLE);
				holder.rtsExtraV.setVisibility(View.VISIBLE);
				final Long tripId;
				if (rts.trip == null) {
					holder.tripHeadingBg.setVisibility(View.GONE);
					tripId = null;
				} else {
					tripId = rts.trip.id;
					holder.tripHeadingTv.setText(rts.trip.getHeading(getContext()).toUpperCase(Locale.getDefault()));
					holder.tripHeadingBg.setVisibility(View.VISIBLE);
				}
				holder.rtsExtraV.setBackgroundColor(poim.getColor(getContext()));
				final Integer stopId = rts.stop == null ? null : rts.stop.id;
				holder.rtsExtraV.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						Activity activity = POIArrayAdapter.this.activityWR == null ? null : POIArrayAdapter.this.activityWR.get();
						if (activity != null && activity instanceof MainActivity) {
							((MainActivity) activity).addFragmentToStack(RTSRouteFragment.newInstance(authority, route.id, tripId, stopId, route));
						} else {
							MTLog.w(POIArrayAdapter.this, "No activity available to open RTS fragment!");
						}
					}
				});
			}
		}
	}

	private void updatePOIStatus(CommonStatusViewHolder statusViewHolder, POIStatus status) {
		if (!this.showStatus || status == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				statusViewHolder.statusV.setVisibility(View.INVISIBLE);
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
		case POI.ITEM_STATUS_TYPE_APP:
			updateAppStatus(statusViewHolder, status);
			break;
		default:
			MTLog.w(this, "Unexpected status type '%s'!", status.getType());
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private void updatePOIStatus(CommonStatusViewHolder statusViewHolder, POIManager poim) {
		if (!this.showStatus || poim == null || statusViewHolder == null) {
			if (statusViewHolder != null) {
				statusViewHolder.statusV.setVisibility(View.INVISIBLE);
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
		case POI.ITEM_STATUS_TYPE_APP:
			updateAppStatus(statusViewHolder, poim);
			break;
		default:
			MTLog.w(this, "Unexpected status type '%s'!", poim.getStatusType());
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	private void updateRTSSchedule(CommonStatusViewHolder statusViewHolder, POIManager poim) {
		if (this.showStatus && poim != null && statusViewHolder instanceof ScheduleStatusViewHolder) {
			poim.setStatusLoaderListener(this);
			updateRTSSchedule(statusViewHolder, poim.getStatus(getContext()));
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
		if (poim != null) {
			poim.setServiceUpdateLoaderListener(this);
			updateServiceUpdate(statusViewHolder, poim.isServiceUpdateWarning(getContext()));
		}
	}

	private void updateRTSSchedule(CommonStatusViewHolder statusViewHolder, POIStatus status) {
		CharSequence line1CS = null;
		CharSequence line2CS = null;
		if (status != null && status instanceof Schedule) {
			Schedule schedule = (Schedule) status;
			ArrayList<Pair<CharSequence, CharSequence>> lines = schedule.getStatus(getContext(), getNowToTheMinute(), TimeUnit.MINUTES.toMillis(30), null, 10,
					null);
			if (lines != null && lines.size() >= 1) {
				line1CS = lines.get(0).first;
				line2CS = lines.get(0).second;
			}
		}
		ScheduleStatusViewHolder scheduleStatusViewHolder = (ScheduleStatusViewHolder) statusViewHolder;
		scheduleStatusViewHolder.dataNextLine1Tv.setText(line1CS);
		scheduleStatusViewHolder.dataNextLine2Tv.setText(line2CS);
		scheduleStatusViewHolder.dataNextLine2Tv.setVisibility(line2CS != null && line2CS.length() > 0 ? View.VISIBLE : View.GONE);
		statusViewHolder.statusV.setVisibility(line1CS != null && line1CS.length() > 0 ? View.VISIBLE : View.INVISIBLE);
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

	@Override
	public void onTimeChanged() {
		resetNowToTheMinute();
	}

	private final BroadcastReceiver timeChangedReceiver = new TimeUtils.TimeChangedReceiver(this);

	private void enableTimeChangedReceiver() {
		if (!this.timeChangedReceiverEnabled) {
			getContext().registerReceiver(timeChangedReceiver, TimeUtils.TIME_CHANGED_INTENT_FILTER);
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
		if (poim == null || poim.poi == null || holder == null) {
			return;
		}
		if (holder.uuid != null) {
			this.poiStatusViewHoldersWR.remove(holder.uuid);
		}
		final POI poi = poim.poi;
		holder.uuid = poi.getUUID();
		if (holder.uuid != null) {
			this.poiStatusViewHoldersWR.put(holder.uuid, holder.statusViewHolder);
		}
		if (holder.compassV != null) {
			holder.compassV.setLatLng(poim.getLat(), poim.getLng());
			this.compassImgsWR.put(holder.compassV, holder.distanceTv);
		}
		// name
		holder.nameTv.setText(poi.getName());
		// distance
		if (holder.distanceTv != null) {
			if (!TextUtils.isEmpty(poim.getDistanceString())) {
				if (!poim.getDistanceString().equals(holder.distanceTv.getText())) {
					holder.distanceTv.setText(poim.getDistanceString());
				}
				holder.distanceTv.setVisibility(View.VISIBLE);
			} else {
				holder.distanceTv.setVisibility(View.GONE);
				holder.distanceTv.setText(null);
			}
		}
		// compass (if distance available)
		if (holder.compassV != null) {
			if (holder.distanceTv != null && holder.distanceTv.getVisibility() == View.VISIBLE && this.location != null && this.lastCompassInDegree >= 0
					&& this.location.getAccuracy() <= poim.getDistance()) {
				holder.compassV.generateAndSetHeading(this.location, this.lastCompassInDegree, this.locationDeclination);
				holder.compassV.setVisibility(View.VISIBLE);
			} else {
				holder.compassV.setVisibility(View.GONE);
			}
		}
		// location
		if (holder.locationTv != null) {
			if (TextUtils.isEmpty(poim.getLocation())) {
				holder.locationTv.setVisibility(View.GONE);
				holder.locationTv.setText(null);
			} else {
				holder.locationTv.setText(poim.getLocation());
				holder.locationTv.setVisibility(View.VISIBLE);
			}
		}
		// favorite
		if (this.showFavorite && this.favUUIDs != null && this.favUUIDs.contains(poi.getUUID())) {
			holder.favImg.setVisibility(View.VISIBLE);
		} else {
			holder.favImg.setVisibility(View.GONE);
		}
		// closest POI
		int index;
		if (this.closestPoiUuids != null && this.closestPoiUuids.contains(poi.getUUID())) {
			index = 0;
		} else {
			index = -1;
		}
		switch (index) {
		case 0:
			holder.nameTv.setTypeface(Typeface.DEFAULT_BOLD);
			if (holder.distanceTv != null) {
				holder.distanceTv.setTypeface(Typeface.DEFAULT_BOLD);
			}
			break;
		default:
			holder.nameTv.setTypeface(Typeface.DEFAULT);
			if (holder.distanceTv != null) {
				holder.distanceTv.setTypeface(Typeface.DEFAULT);
			}
			break;
		}
	}

	private MTAsyncTask<Integer, Void, ArrayList<Favorite>> refreshFavoritesTask;

	public void refreshFavorites(Integer... typesFilter) {
		if (this.refreshFavoritesTask != null && this.refreshFavoritesTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return; // skipped, last refresh still in progress so probably good enough
		}
		this.refreshFavoritesTask = new MTAsyncTask<Integer, Void, ArrayList<Favorite>>() {
			@Override
			public String getLogTag() {
				return POIArrayAdapter.this.getLogTag() + ">refreshFavoritesTask";
			}

			@Override
			protected ArrayList<Favorite> doInBackgroundMT(Integer... params) {
				return FavoriteManager.findFavorites(POIArrayAdapter.this.getContext(), params);
			}

			@Override
			protected void onPostExecute(ArrayList<Favorite> result) {
				setFavorites(result);
			}
		};
		this.refreshFavoritesTask.execute(typesFilter);

	}

	private void setFavorites(ArrayList<Favorite> favorites) {
		boolean newFav = false; // don't trigger update if favorites are the same
		boolean updatedFav = false; // don't trigger it favorites are the same OR were not set
		if (this.favUUIDs == null) {
			newFav = true; // favorite never set before
			updatedFav = false; // never set before so not updated
		} else if (CollectionUtils.getSize(favorites) != CollectionUtils.getSize(this.favUUIDs)) {
			newFav = true; // different size => different favorites
			updatedFav = true; // different size => different favorites
		}
		HashSet<String> newFavUUIDs = new HashSet<String>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				String uid = favorite.getFkId();
				if (!newFav && this.favUUIDs != null && !this.favUUIDs.contains(uid)) {
					newFav = true;
					updatedFav = true;
				}
				newFavUUIDs.add(uid);
			}
		}
		this.favUUIDs = newFavUUIDs;
		if (newFav) {
			notifyDataSetChanged(true);
		}
		if (updatedFav) {
			if (this.favoriteUpdateListener != null) {
				this.favoriteUpdateListener.onFavoriteUpdated();
			}
		}
	}

	public static class ModuleViewHolder extends CommonViewHolder {
		ImageView moduleTypeImg;
	}

	public static class RouteTripStopViewHolder extends CommonViewHolder {
		TextView routeShortNameTv;
		View routeFL;
		View rtsExtraV;
		MTJPathsView routeTypeImg;
		TextView tripHeadingTv;
		View tripHeadingBg;
	}

	public static class BasicPOIViewHolder extends CommonViewHolder {
	}

	public static class CommonViewHolder {
		String uuid;
		View view;
		TextView nameTv;
		TextView distanceTv;
		TextView locationTv;
		ImageView favImg;
		MTCompassView compassV;
		CommonStatusViewHolder statusViewHolder;
	}

	public static class AppStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv;
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
		ImageView warningImg;
	}

	public static class TypeHeaderViewHolder {
		TextView nameTv;
		TextView allBtnTv;
		View allBtn;
		View nearbyBtn;
		View moreBtn;
	}

	public static interface TypeHeaderButtonsClickListener {

		public static final int BUTTON_MORE = 0;
		public static final int BUTTON_NEARBY = 1;
		public static final int BUTTON_ALL = 2;

		public boolean onTypeHeaderButtonClick(int buttonId, DataSourceType type);
	}

}
