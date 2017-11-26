package org.mtransit.android.data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.mtransit.android.commons.TaskUtils;
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
import org.mtransit.android.ui.view.MTOnClickListener;
import org.mtransit.android.ui.view.MTOnItemClickListener;
import org.mtransit.android.ui.view.MTOnItemLongClickListener;
import org.mtransit.android.ui.view.MTOnLongClickListener;
import org.mtransit.android.ui.view.MTPieChartPercentView;
import org.mtransit.android.util.CrashUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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

	private HashMap<String, Integer> favUUIDsFolderIds;

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

	private boolean infiniteLoading = false; // infinite loading

	private InfiniteLoadingListener infiniteLoadingListener;

	private ViewGroup manualLayout;

	private ScrollView manualScrollView;

	private long lastNotifyDataSetChanged = -1L;

	private int scrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

	private long nowToTheMinute = -1L;

	private boolean timeChangedReceiverEnabled = false;

	private boolean compassUpdatesEnabled = false;

	private long lastCompassChanged = -1L;

	private FavoriteManager.FavoriteUpdateListener favoriteUpdateListener = this;

	public POIArrayAdapter(Activity activity) {
		super(activity, -1);
		setActivity(activity);
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

	public void setInfiniteLoading(boolean infiniteLoading) {
		this.infiniteLoading = infiniteLoading;
	}

	public void setInfiniteLoadingListener(InfiniteLoadingListener infiniteLoadingListener) {
		this.infiniteLoadingListener = infiniteLoadingListener;
	}

	public interface InfiniteLoadingListener {

		boolean isLoadingMore();

		boolean showingDone();
	}

	private static final int VIEW_TYPE_COUNT = 11;

	/**
	 * @see #getItemViewType(int)
	 */
	@Override
	public int getViewTypeCount() {
		return VIEW_TYPE_COUNT;
	}

	/**
	 * @see #getViewTypeCount()
	 */
	@Override
	public int getItemViewType(int position) {
		POIManager poim = getItem(position);
		if (poim == null) {
			if (this.showBrowseHeaderSection && position == 0) {
				return 0; // BROWSE SECTION
			}
			if (this.infiniteLoading && position + 1 == getCount()) {
				return 9; // LOADING FOOTER
			}
			if (this.showTypeHeader != TYPE_HEADER_NONE) {
				if (this.poisByType != null) {
					Integer typeId = getItemTypeHeader(position);
					if (typeId != null) {
						if (FavoriteManager.isFavoriteDataSourceId(typeId)) {
							return 10; // TYPE FAVORITE FOLDER
						}
						return 8; // TYPE HEADER
					}
				}
			}
			CrashUtils.w(this, "Cannot find type for at position '%s'!", position);
			return IGNORE_ITEM_VIEW_TYPE;
		}
		int type = poim.poi.getType();
		int statusType = poim.getStatusType();
		switch (type) {
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return 7; // TEXT MESSAGE
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
			switch (statusType) {
			case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
				return 1; // DEFAULT & AVAILABILITY %
			default:
				return 2; // DEFAULT
			}
		default:
			CrashUtils.w(this, "Cannot find POI type for at position '%s'!", position);
			return 2; // DEFAULT
		}
	}

	private int count = -1;

	@Override
	public int getCount() {
		if (this.count < 0) {
			initCount();
		}
		return this.count;
	}

	private void initCount() {
		this.count = 0;
		if (this.showBrowseHeaderSection) {
			this.count++;
		}
		if (this.poisByType != null) {
			for (Integer type : this.poisByType.keySet()) {
				if (this.showTypeHeader != TYPE_HEADER_NONE) {
					this.count++;
				}
				this.count += this.poisByType.get(type).size();
			}
		}
		if (this.infiniteLoading) {
			this.count++;
		}
	}

	@Override
	public int getPosition(POIManager item) {
		int position = 0;
		if (this.showBrowseHeaderSection) {
			position++;
		}
		if (this.poisByType != null) {
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

	@Nullable
	@Override
	public POIManager getItem(int position) {
		int index = 0;
		if (this.showBrowseHeaderSection) {
			index++;
		}
		if (this.poisByType != null) {
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

	@Nullable
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

	@Nullable
	private Integer getItemTypeHeader(int position) {
		int index = 0;
		if (this.showBrowseHeaderSection) {
			index++;
		}
		if (this.showTypeHeader != TYPE_HEADER_NONE && this.poisByType != null) {
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

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @Nullable ViewGroup parent) {
		POIManager poim = getItem(position);
		if (poim == null) {
			if (this.showBrowseHeaderSection && position == 0) {
				return getBrowseHeaderSectionView(convertView, parent);
			}
			if (this.infiniteLoading && position + 1 == getCount()) {
				return getInfiniteLoadingView(convertView, parent);
			}
			if (this.showTypeHeader != TYPE_HEADER_NONE) {
				Integer typeId = getItemTypeHeader(position);
				if (typeId != null) {
					if (FavoriteManager.isFavoriteDataSourceId(typeId)) {
						int favoriteFolderId = FavoriteManager.extractFavoriteFolderId(typeId);
						if (FavoriteManager.get(getContext()).hasFavoriteFolder(favoriteFolderId)) {
							return getFavoriteFolderHeaderView(FavoriteManager.get(getContext()).getFolder(favoriteFolderId), convertView, parent);
						}
					}
					DataSourceType dst = DataSourceType.parseId(typeId);
					if (dst != null) {
						return getTypeHeaderView(dst, convertView, parent);
					}
				}
			}
			CrashUtils.w(this, "getView() > Cannot create view for null poi at position '%s'!", position);
			return getInfiniteLoadingView(convertView, parent);
		}
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return getTextMessageView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP:
			return getRouteTripStopView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOIView(poim, convertView, parent);
		default:
			CrashUtils.w(this, "getView() > Unknown view type at position %s!", position);
			return getBasicPOIView(poim, convertView, parent);
		}
	}

	@NonNull
	private View getInfiniteLoadingView(@Nullable View convertView, @Nullable ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.layout_poi_infinite_loading, parent, false);
			InfiniteLoadingViewHolder holder = new InfiniteLoadingViewHolder();
			holder.progressBar = convertView.findViewById(R.id.progress_bar);
			holder.worldExplored = convertView.findViewById(R.id.worldExploredTv);
			convertView.setTag(holder);
		}
		InfiniteLoadingViewHolder holder = (InfiniteLoadingViewHolder) convertView.getTag();
		if (this.infiniteLoadingListener != null) {
			if (this.infiniteLoadingListener.isLoadingMore()) {
				holder.worldExplored.setVisibility(View.GONE);
				holder.progressBar.setVisibility(View.VISIBLE);
				convertView.setVisibility(View.VISIBLE);
			} else if (this.infiniteLoadingListener.showingDone()) {
				holder.progressBar.setVisibility(View.GONE);
				holder.worldExplored.setVisibility(View.VISIBLE);
				convertView.setVisibility(View.VISIBLE);
			} else {
				convertView.setVisibility(View.GONE);
			}
		} else {
			convertView.setVisibility(View.GONE);
		}
		return convertView;
	}

	private int nbAgencyTypes = -1;

	private View getBrowseHeaderSectionView(@Nullable View convertView, @Nullable ViewGroup parent) {
		Activity activity = this.activityWR == null ? null : this.activityWR.get();
		DataSourceProvider dataSourceProvider = DataSourceProvider.get(activity);
		int agenciesCount = dataSourceProvider == null ? 0 : dataSourceProvider.getAllAgenciesCount();
		if (convertView == null || this.nbAgencyTypes != agenciesCount) {
			if (convertView == null) {
				convertView = this.layoutInflater.inflate(R.layout.layout_poi_list_browse_header, parent, false);
			}
			LinearLayout gridLL = (LinearLayout) convertView.findViewById(R.id.gridLL);
			gridLL.removeAllViews();
			ArrayList<DataSourceType> allAgencyTypes = dataSourceProvider == null ? null : dataSourceProvider.getAvailableAgencyTypes();
			this.nbAgencyTypes = CollectionUtils.getSize(allAgencyTypes);
			if (allAgencyTypes == null) {
				gridLL.setVisibility(View.GONE);
			} else {
				int availableButtons = 0;
				View gridLine = null;
				View btn;
				TextView btnTv;
				for (final DataSourceType dst : allAgencyTypes) {
					if (dst.getId() == DataSourceType.TYPE_MODULE.getId() && availableButtons == 0 && allAgencyTypes.size() > 2) {
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
					btn = gridLine.findViewById(availableButtons == 2 ? R.id.btn1 : R.id.btn2);
					btnTv = (TextView) gridLine.findViewById(availableButtons == 2 ? R.id.btn1Tv : R.id.btn2Tv);
					btnTv.setText(dst.getAllStringResId());
					if (dst.getWhiteIconResId() != -1) {
						btnTv.setCompoundDrawablesWithIntrinsicBounds(dst.getWhiteIconResId(), 0, 0, 0);
					} else {
						btnTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
					}
					btn.setOnClickListener(new MTOnClickListener() {
						@Override
						public void onClickMT(View view) {
							onTypeHeaderButtonClick(TypeHeaderButtonsClickListener.BUTTON_ALL, dst);
						}
					});
					btn.setVisibility(View.VISIBLE);
					availableButtons--;
				}
				if (gridLine != null && availableButtons == 1) {
					gridLine.findViewById(R.id.btn2).setVisibility(View.GONE);
				}
				gridLL.setVisibility(View.VISIBLE);
			}
		}
		return convertView;
	}

	@NonNull
	private View updateCommonViewManual(@NonNull POIManager poim, @NonNull View convertView) {
		if (!(convertView.getTag() instanceof CommonViewHolder)) {
			return convertView;
		}
		CommonViewHolder holder = (CommonViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		updatePOIStatus(holder.statusViewHolder, poim);
		return convertView;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MTOnItemClickListener.onItemClickS(parent, view, position, id, new MTOnItemClickListener() {
			@Override
			public void onItemClickMT(AdapterView<?> parent, View view, int position, long id) {
				showPoiViewerScreen(position);
			}
		});
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		return MTOnItemLongClickListener.onItemLongClickS(parent, view, position, id, new MTOnItemLongClickListener() {
			@Override
			public boolean onItemLongClickMT(AdapterView<?> parent, View view, int position, long id) {
				return showPoiMenu(position);
			}
		});
	}

	public interface OnClickHandledListener {
		void onLeaving();
	}

	private WeakReference<OnClickHandledListener> onClickHandledListenerWR;

	public void setOnClickHandledListener(OnClickHandledListener onClickHandledListener) {
		this.onClickHandledListenerWR = new WeakReference<OnClickHandledListener>(onClickHandledListener);
	}

	public interface OnPOISelectedListener {
		boolean onPOISelected(POIManager poim);

		boolean onPOILongSelected(POIManager poim);
	}

	private WeakReference<OnPOISelectedListener> onPoiSelectedListenerWR;

	public void setOnPoiSelectedListener(OnPOISelectedListener onPoiSelectedListener) {
		this.onPoiSelectedListenerWR = new WeakReference<OnPOISelectedListener>(onPoiSelectedListener);
	}

	public boolean showPoiViewerScreen(int position) {
		boolean handled = false;
		POIManager poim = getItem(position);
		if (poim != null) {
			OnPOISelectedListener listener = this.onPoiSelectedListenerWR == null ? null : this.onPoiSelectedListenerWR.get();
			handled = listener != null && listener.onPOISelected(poim);
			if (!handled) {
				handled = showPoiViewerScreen(poim);
			}
		}
		return handled;
	}

	private boolean showPoiMenu(int position) {
		boolean handled = false;
		POIManager poim = getItem(position);
		if (poim != null) {
			OnPOISelectedListener listener = this.onPoiSelectedListenerWR == null ? null : this.onPoiSelectedListenerWR.get();
			handled = listener != null && listener.onPOILongSelected(poim);
			if (!handled) {
				handled = showPoiMenu(poim);
			}
		}
		return handled;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false; // to hide divider around disabled items (list view background visible behind hidden divider)
		// return true; // to show divider around disabled items
	}

	@Override
	public boolean isEnabled(int position) {
		return getItemTypeHeader(position) == null; // is NOT separator
	}

	public boolean showPoiViewerScreen(POIManager poim) {
		if (poim == null) {
			return false;
		}
		Activity activity = this.activityWR == null ? null : this.activityWR.get();
		if (activity == null) {
			return false;
		}
		OnClickHandledListener listener = this.onClickHandledListenerWR == null ? null : this.onClickHandledListenerWR.get();
		return poim.onActionItemClick(activity, FavoriteManager.get(getContext()).getFavoriteFolders(), this.favoriteUpdateListener, listener);
	}

	private boolean showPoiMenu(POIManager poim) {
		if (poim == null) {
			return false;
		}
		Activity activity = this.activityWR == null ? null : this.activityWR.get();
		if (activity == null) {
			return false;
		}
		OnClickHandledListener listener = this.onClickHandledListenerWR == null ? null : this.onClickHandledListenerWR.get();
		return poim.onActionItemLongClick(activity, FavoriteManager.get(getContext()).getFavoriteFolders(), this.favoriteUpdateListener, listener);
	}

	@Override
	public void onFavoriteUpdated() {
		refreshFavorites();
	}

	public void setPois(ArrayList<POIManager> pois) {
		if (this.poisByType != null) {
			this.poisByType.clear();
		}
		this.poiUUID.clear();
		boolean dataSetChanged = append(pois, true);
		if (dataSetChanged) {
			notifyDataSetChanged();
		}
	}

	private HashSet<String> poiUUID = new HashSet<String>();

	public void appendPois(ArrayList<POIManager> pois) {
		boolean dataSetChanged = append(pois, false);
		if (dataSetChanged) {
			notifyDataSetChanged();
		}
	}

	private boolean append(ArrayList<POIManager> pois, boolean dataSetChanged) {
		if (pois != null) {
			if (this.poisByType == null) {
				this.poisByType = new LinkedHashMap<Integer, ArrayList<POIManager>>();
			}
			for (POIManager poim : pois) {
				if (!this.poisByType.containsKey(poim.poi.getDataSourceTypeId())) {
					this.poisByType.put(poim.poi.getDataSourceTypeId(), new ArrayList<POIManager>());
				}
				if (!this.poiUUID.contains(poim.poi.getUUID())) {
					this.poisByType.get(poim.poi.getDataSourceTypeId()).add(poim);
					this.poiUUID.add(poim.poi.getUUID());
					dataSetChanged = true;
				}
			}
		}
		if (dataSetChanged) {
			this.lastNotifyDataSetChanged = -1; // last notify was with old data
			initCount();
			initPoisCount();
			refreshFavorites();
			updateClosestPoi();
		}
		return dataSetChanged;
	}

	private void resetCounts() {
		this.count = -1;
		this.poisCount = -1;
	}

	public boolean isInitialized() {
		return this.poisByType != null;
	}

	private int poisCount = -1;

	public int getPoisCount() {
		if (this.poisCount < 0) {
			initPoisCount();
		}
		return this.poisCount;
	}

	private void initPoisCount() {
		this.poisCount = 0;
		if (this.poisByType != null) {
			for (Integer type : this.poisByType.keySet()) {
				this.poisCount += this.poisByType.get(type).size();
			}
		}
	}

	public boolean hasPois() {
		return getPoisCount() > 0;
	}

	private void updateClosestPoi() {
		if (getPoisCount() == 0) {
			this.closestPoiUuids = null;
			return;
		}
		this.closestPoiUuids = new HashSet<String>();
		if (this.poisByType != null) {
			for (Integer type : this.poisByType.keySet()) {
				ArrayList<POIManager> orderedPoims = new ArrayList<POIManager>(this.poisByType.get(type));
				if (orderedPoims.size() > 0) {
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

	@Nullable
	private UpdateDistanceWithStringTask updateDistanceWithStringTask;

	private void updateDistances(Location currentLocation) {
		TaskUtils.cancelQuietly(this.updateDistanceWithStringTask, true);
		if (currentLocation != null && getPoisCount() > 0) {
			this.updateDistanceWithStringTask = new UpdateDistanceWithStringTask(this);
			TaskUtils.execute(this.updateDistanceWithStringTask, currentLocation);
		}
	}

	private static class UpdateDistanceWithStringTask extends MTAsyncTask<Location, Void, Void> {

		private final WeakReference<POIArrayAdapter> poiArrayAdapterWR;

		@Override
		public String getLogTag() {
			return POIArrayAdapter.class.getSimpleName() + ">" + UpdateDistanceWithStringTask.class.getSimpleName();
		}

		private UpdateDistanceWithStringTask(POIArrayAdapter poiArrayAdapter) {
			this.poiArrayAdapterWR = new WeakReference<POIArrayAdapter>(poiArrayAdapter);
		}

		@Override
		protected Void doInBackgroundMT(Location... params) {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			POIArrayAdapter poiArrayAdapter = this.poiArrayAdapterWR.get();
			if (poiArrayAdapter == null) {
				return null;
			}
			try {
				if (poiArrayAdapter.poisByType != null) {
					Iterator<ArrayList<POIManager>> it = poiArrayAdapter.poisByType.values().iterator();
					while (it.hasNext()) {
						if (isCancelled()) {
							break;
						}
						LocationUtils.updateDistanceWithString(poiArrayAdapter.getContext(), it.next(), params[0], this);
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while update POIs distance strings!");
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			POIArrayAdapter poiArrayAdapter = this.poiArrayAdapterWR.get();
			if (poiArrayAdapter == null) {
				return;
			}
			if (isCancelled()) {
				return;
			}
			poiArrayAdapter.updateClosestPoi();
			poiArrayAdapter.notifyDataSetChanged(true);
		}
	}

	@Deprecated
	public void updateDistancesNowSync(Location currentLocation) {
		if (currentLocation != null) {
			if (this.poisByType != null) {
				Iterator<ArrayList<POIManager>> it = this.poisByType.values().iterator();
				while (it.hasNext()) {
					ArrayList<POIManager> pois = it.next();
					LocationUtils.updateDistanceWithString(getContext(), pois, currentLocation, null);
				}
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

	private void setScrollState(int scrollState) {
		this.scrollState = scrollState;
	}

	@Override
	public void onStatusLoaded(POIStatus status) {
		if (this.showStatus) {
			CommonStatusViewHolder statusViewHolder = this.poiStatusViewHoldersWR.get(status.getTargetUUID());
			if (statusViewHolder != null && status.getTargetUUID().equals(statusViewHolder.uuid)) {
				updatePOIStatus(statusViewHolder, status);
			} else {
				notifyDataSetChanged(false);
			}
		}
	}

	@Override
	public void onServiceUpdatesLoaded(String targetUUID, ArrayList<ServiceUpdate> serviceUpdates) {
		if (this.showServiceUpdate) {
			CommonStatusViewHolder statusViewHolder = this.poiStatusViewHoldersWR.get(targetUUID);
			if (statusViewHolder != null && targetUUID.equals(statusViewHolder.uuid)) {
				updateServiceUpdate(statusViewHolder, ServiceUpdate.isSeverityWarning(serviceUpdates));
			} else {
				notifyDataSetChanged(false);
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

	public void notifyDataSetChanged(boolean force, long minAdapterThresholdInMs) {
		long now = TimeUtils.currentTimeMillis();
		long adapterThreshold = Math.max(minAdapterThresholdInMs, Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS);
		if (this.scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE && (force || (now - this.lastNotifyDataSetChanged) > adapterThreshold)) {
			notifyDataSetChanged();
			notifyDataSetChangedManual();
			this.lastNotifyDataSetChanged = now;
			this.handler.removeCallbacks(this.notifyDataSetChangedLater);
		} else {
			if (force) {
				this.handler.postDelayed(this.notifyDataSetChangedLater, adapterThreshold);
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
				Object tag = view == null ? null : view.getTag();
				if (tag != null && tag instanceof CommonViewHolder) {
					POIManager poim = getItem(position);
					if (poim != null) {
						updateCommonViewManual(poim, view);
					}
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
			this.manualLayout.removeAllViews(); // clear the previous list
			for (int i = 0; i < getPoisCount(); i++) {
				if (this.manualLayout.getChildCount() > 0) {
					this.manualLayout.addView(this.layoutInflater.inflate(R.layout.list_view_divider, this.manualLayout, false));
				}
				View view = getView(i, null, this.manualLayout);
				FrameLayout frameLayout = new FrameLayout(getContext());
				frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				frameLayout.addView(view);
				View selectorView = new View(getContext());
				SupportFactory.get().setBackground(selectorView, ThemeUtils.obtainStyledDrawable(getContext(), R.attr.selectableItemBackground));
				selectorView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
				frameLayout.addView(selectorView);
				final int position = i;
				frameLayout.setOnClickListener(new MTOnClickListener() {
					@Override
					public void onClickMT(View view) {
						showPoiViewerScreen(position);
					}
				});
				frameLayout.setOnLongClickListener(new MTOnLongClickListener() {
					@Override
					public boolean onLongClickkMT(View view) {
						return showPoiMenu(position);
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
		TaskUtils.cancelQuietly(this.refreshFavoritesTask, true);
		disableTimeChangedReceiver();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(POIArrayAdapter.class.getSimpleName()) //
				.append(getLogTag()) //
				.toString();
	}

	public void onResume(Activity activity, Location userLocation) {
		setActivity(activity);
		this.location = null; // clear current location to force refresh
		setLocation(userLocation);
		refreshFavorites();
	}

	public void setActivity(Activity activity) {
		this.activityWR = new WeakReference<Activity>(activity);
	}

	@Override
	public void clear() {
		if (this.poisByType != null) {
			this.poisByType.clear();
			this.poisByType = null; // not initialized
		}
		resetCounts();
		if (this.poiUUID != null) {
			this.poiUUID.clear();
		}
		if (this.closestPoiUuids != null) {
			this.closestPoiUuids.clear();
			this.closestPoiUuids = null;
		}
		disableTimeChangedReceiver();
		if (this.compassImgsWR != null) {
			this.compassImgsWR.clear();
		}
		this.lastCompassChanged = -1;
		this.lastCompassInDegree = -1;
		this.accelerometerValues = new float[3];
		this.magneticFieldValues = new float[3];
		this.lastNotifyDataSetChanged = -1L;
		this.handler.removeCallbacks(this.notifyDataSetChangedLater);
		this.poiStatusViewHoldersWR.clear();
		TaskUtils.cancelQuietly(this.refreshFavoritesTask, true);
		TaskUtils.cancelQuietly(this.updateDistanceWithStringTask, true);
		this.location = null;
		this.locationDeclination = 0f;
		super.clear();
	}

	public void onDestroy() {
		disableTimeChangedReceiver();
		if (this.poisByType != null) {
			this.poisByType.clear();
			this.poisByType = null;
		}
		resetCounts();
		if (this.poiUUID != null) {
			this.poiUUID.clear();
		}
		if (this.compassImgsWR != null) {
			this.compassImgsWR.clear();
		}
		this.poiStatusViewHoldersWR.clear();
		if (this.onClickHandledListenerWR != null) {
			this.onClickHandledListenerWR.clear();
		}
		if (this.onPoiSelectedListenerWR != null) {
			this.onPoiSelectedListenerWR.clear();
		}
	}

	@Override
	public void updateCompass(float orientation, boolean force) {
		if (getPoisCount() == 0) {
			return;
		}
		long now = TimeUtils.currentTimeMillis();
		int roundedOrientation = SensorUtils.convertToPosivite360Degree((int) orientation);
		SensorUtils.updateCompass(force, this.location, roundedOrientation, now, this.scrollState, this.lastCompassChanged, this.lastCompassInDegree,
				Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS, this);
	}

	@Override
	public void onSensorTaskCompleted(boolean result, int roundedOrientation, long now) {
		if (!result) {
			return;
		}
		this.lastCompassInDegree = roundedOrientation;
		this.lastCompassChanged = now;
		if (!this.compassUpdatesEnabled || this.location == null || this.lastCompassInDegree < 0) {
			return;
		}
		if (this.compassImgsWR == null) {
			return;
		}
		for (WeakHashMap.Entry<MTCompassView, View> compassAndDistance : this.compassImgsWR.entrySet()) {
			MTCompassView compassView = compassAndDistance.getKey();
			if (compassView != null && compassView.isHeadingSet()) {
				compassView.generateAndSetHeading(this.location, this.lastCompassInDegree, this.locationDeclination);
			}
		}
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		SensorUtils.checkForCompass(getContext(), se, this.accelerometerValues, this.magneticFieldValues, this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// DO NOTHING
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
			MTLog.w(this, "Unexpected header type '%s'!", this.showTypeHeader);
			return R.layout.layout_poi_list_header;
		}
	}

	private WeakReference<TypeHeaderButtonsClickListener> typeHeaderButtonsClickListenerWR;

	public void setOnTypeHeaderButtonsClickListener(TypeHeaderButtonsClickListener listener) {
		this.typeHeaderButtonsClickListenerWR = new WeakReference<TypeHeaderButtonsClickListener>(listener);
	}

	private void onTypeHeaderButtonClick(int buttonId, DataSourceType type) {
		TypeHeaderButtonsClickListener listener = this.typeHeaderButtonsClickListenerWR == null ? null : this.typeHeaderButtonsClickListenerWR.get();
		if (listener != null && listener.onTypeHeaderButtonClick(buttonId, type)) {
			return;
		}
		switch (buttonId) {
		case TypeHeaderButtonsClickListener.BUTTON_ALL:
			if (type != null) {
				Activity activity = this.activityWR == null ? null : this.activityWR.get();
				if (activity != null) {
					leaving();
					((MainActivity) activity).addFragmentToStack(AgencyTypeFragment.newInstance(type.getId(), type));
				}
			}
			break;
		case TypeHeaderButtonsClickListener.BUTTON_NEARBY:
			if (type != null) {
				Activity activity = this.activityWR == null ? null : this.activityWR.get();
				if (activity != null) {
					leaving();
					((MainActivity) activity).addFragmentToStack(NearbyFragment.newNearbyInstance(null, type.getId()));
				}
			}
			break;
		case TypeHeaderButtonsClickListener.BUTTON_MORE:
			if (type != null) {
				Activity activity = this.activityWR == null ? null : this.activityWR.get();
				if (activity != null) {
					leaving();
					((MainActivity) activity).addFragmentToStack(AgencyTypeFragment.newInstance(type.getId(), type));
				}
			}
			break;
		default:
			MTLog.w(this, "Unexpected type header button %s'' click", type);
		}
	}

	private void leaving() {
		OnClickHandledListener onClickHandledListener = this.onClickHandledListenerWR == null ? null : this.onClickHandledListenerWR.get();
		if (onClickHandledListener != null) {
			onClickHandledListener.onLeaving();
		}
	}

	@NonNull
	private View getTypeHeaderView(final DataSourceType type, @Nullable View convertView, @Nullable ViewGroup parent) {
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
		if (type.getGrey600IconResId() != -1) {
			holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(type.getGrey600IconResId(), 0, 0, 0);
		}
		if (holder.allBtn != null) {
			holder.allBtn.setOnClickListener(new MTOnClickListener() {
				@Override
				public void onClickMT(View view) {
					onTypeHeaderButtonClick(TypeHeaderButtonsClickListener.BUTTON_ALL, type);
				}
			});
		}
		if (holder.nearbyBtn != null) {
			holder.nearbyBtn.setOnClickListener(new MTOnClickListener() {
				@Override
				public void onClickMT(View view) {
					onTypeHeaderButtonClick(TypeHeaderButtonsClickListener.BUTTON_NEARBY, type);
				}
			});
		}
		if (holder.moreBtn != null) {
			holder.moreBtn.setOnClickListener(new MTOnClickListener() {
				@Override
				public void onClickMT(View view) {
					onTypeHeaderButtonClick(TypeHeaderButtonsClickListener.BUTTON_MORE, type);
				}
			});
		}
		return convertView;
	}

	private View getFavoriteFolderHeaderView(final Favorite.Folder favoriteFolder, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.layout_poi_list_header_with_delete, parent, false);
			FavoriteFolderHeaderViewHolder holder = new FavoriteFolderHeaderViewHolder();
			holder.nameTv = (TextView) convertView.findViewById(R.id.name);
			holder.renameBtn = convertView.findViewById(R.id.renameBtn);
			holder.deleteBtn = convertView.findViewById(R.id.deleteBtn);
			convertView.setTag(holder);
		}
		FavoriteFolderHeaderViewHolder holder = (FavoriteFolderHeaderViewHolder) convertView.getTag();
		holder.nameTv.setText(favoriteFolder == null ? null : favoriteFolder.getName());
		if (holder.renameBtn != null) {
			holder.renameBtn.setOnClickListener(new MTOnClickListener() {
				@Override
				public void onClickMT(View view) {
					FavoriteManager.showUpdateFolderDialog(getContext(), POIArrayAdapter.this.layoutInflater, favoriteFolder,
							POIArrayAdapter.this.favoriteUpdateListener);
				}
			});
		}
		if (holder.deleteBtn != null) {
			holder.deleteBtn.setOnClickListener(new MTOnClickListener() {
				@Override
				public void onClickMT(View view) {
					FavoriteManager.showDeleteFolderDialog(POIArrayAdapter.this.getContext(), favoriteFolder, POIArrayAdapter.this.favoriteUpdateListener);
				}
			});
		}
		return convertView;
	}

	private WeakHashMap<String, CommonStatusViewHolder> poiStatusViewHoldersWR = new WeakHashMap<String, CommonStatusViewHolder>();

	@NonNull
	private View getBasicPOIView(@NonNull POIManager poim, @Nullable View convertView, @Nullable ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getBasicPOILayout(poim.getStatusType()), parent, false);
			BasicPOIViewHolder holder = new BasicPOIViewHolder();
			initCommonViewHolder(holder, convertView, poim.poi.getUUID());
			holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), convertView);
			convertView.setTag(holder);
		}
		updateBasicPOIView(poim, convertView);
		return convertView;
	}

	private CommonStatusViewHolder initPOIStatusViewHolder(int status, View convertView) {
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			return null;
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

	@LayoutRes
	private int getBasicPOILayout(int status) {
		int layoutRes = R.layout.layout_poi_basic;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
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

	@NonNull
	private View updateBasicPOIView(@NonNull POIManager poim, @NonNull View convertView) {
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
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.GONE);
				availabilityPercentStatusViewHolder.textTv.setText(availabilityPercent.getStatusMsg(getContext()));
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			} else if (availabilityPercent.isShowingLowerValue()) {
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.GONE);
				availabilityPercentStatusViewHolder.textTv.setText(availabilityPercent.getLowerValueText(getContext()));
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.VISIBLE);
			} else {
				availabilityPercentStatusViewHolder.textTv.setVisibility(View.GONE);
				availabilityPercentStatusViewHolder.piePercentV.setValueColors( //
						availabilityPercent.getValue1Color(), //
						availabilityPercent.getValue1ColorBg(), //
						availabilityPercent.getValue2Color(), //
						availabilityPercent.getValue2ColorBg() //
				);
				availabilityPercentStatusViewHolder.piePercentV.setValues(availabilityPercent.getValue1(), availabilityPercent.getValue2());
				availabilityPercentStatusViewHolder.piePercentV.setVisibility(View.VISIBLE);
			}
			statusViewHolder.statusV.setVisibility(View.VISIBLE);
		} else {
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
		}
	}

	@LayoutRes
	private int getRTSLayout(int status) {
		int layoutRes = R.layout.layout_poi_rts;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			if (this.showExtra) {
				layoutRes = R.layout.layout_poi_rts_with_schedule;
			} else {
				layoutRes = R.layout.layout_poi_basic_with_schedule;
			}
			break;
		default:
			MTLog.w(this, "Unexpected status '%s' (rts view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	@NonNull
	private View getTextMessageView(@NonNull POIManager poim, @Nullable View convertView, @Nullable ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.layout_poi_basic, parent, false);
			TextViewViewHolder holder = new TextViewViewHolder();
			initCommonViewHolder(holder, convertView, poim.poi.getUUID());
			convertView.setTag(holder);
		}
		updateTextMessageView(poim, convertView);
		return convertView;
	}

	@NonNull
	private View updateTextMessageView(@NonNull POIManager poim, @NonNull View convertView) {
		TextViewViewHolder holder = (TextViewViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		return convertView;
	}

	@NonNull
	private View getModuleView(@NonNull POIManager poim, @Nullable View convertView, @Nullable ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getModuleLayout(poim.getStatusType()), parent, false);
			ModuleViewHolder holder = new ModuleViewHolder();
			initCommonViewHolder(holder, convertView, poim.poi.getUUID());
			initModuleExtra(convertView, holder);
			holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), convertView);
			convertView.setTag(holder);
		}
		updateModuleView(poim, convertView);
		return convertView;
	}

	private void initModuleExtra(View convertView, ModuleViewHolder holder) {
		holder.moduleExtraTypeImg = (ImageView) convertView.findViewById(R.id.extra);
	}

	@NonNull
	private View updateModuleView(@NonNull POIManager poim, @NonNull View convertView) {
		ModuleViewHolder holder = (ModuleViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		updateModuleExtra(poim, holder);
		updatePOIStatus(holder.statusViewHolder, poim);
		return convertView;
	}

	private void updateModuleExtra(POIManager poim, ModuleViewHolder holder) {
		if (this.showExtra && poim.poi != null && poim.poi instanceof Module) {
			Module module = (Module) poim.poi;
			holder.moduleExtraTypeImg.setBackgroundColor(poim.getColor(getContext()));
			DataSourceType moduleType = DataSourceType.parseId(module.getTargetTypeId());
			if (moduleType != null) {
				holder.moduleExtraTypeImg.setImageResource(moduleType.getWhiteIconResId());
			} else {
				holder.moduleExtraTypeImg.setImageResource(0);
			}
			holder.moduleExtraTypeImg.setVisibility(View.VISIBLE);
		} else {
			holder.moduleExtraTypeImg.setVisibility(View.GONE);
		}
	}

	@LayoutRes
	private int getModuleLayout(int status) {
		int layoutRes = R.layout.layout_poi_module;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			layoutRes = R.layout.layout_poi_module_with_app_status;
			break;
		default:
			MTLog.w(this, "Unexpected status '%s' (module view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	@NonNull
	private View getRouteTripStopView(@NonNull POIManager poim, @Nullable View convertView, @Nullable ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getRTSLayout(poim.getStatusType()), parent, false);
			RouteTripStopViewHolder holder = new RouteTripStopViewHolder();
			initCommonViewHolder(holder, convertView, poim.poi.getUUID());
			initRTSExtra(convertView, holder);
			holder.statusViewHolder = initPOIStatusViewHolder(poim.getStatusType(), convertView);
			convertView.setTag(holder);
		}
		updateRouteTripStopView(poim, convertView);
		return convertView;
	}

	private void initRTSExtra(@NonNull View convertView, @NonNull RouteTripStopViewHolder holder) {
		holder.rtsExtraV = convertView.findViewById(R.id.extra);
		holder.routeFL = convertView.findViewById(R.id.route);
		holder.routeShortNameTv = (TextView) convertView.findViewById(R.id.route_short_name);
		holder.routeTypeImg = (MTJPathsView) convertView.findViewById(R.id.route_type_img);
		holder.tripHeadingTv = (TextView) convertView.findViewById(R.id.trip_heading);
		holder.tripHeadingBg = convertView.findViewById(R.id.trip_heading_bg);
	}

	@NonNull
	private View updateRouteTripStopView(@NonNull POIManager poim, @NonNull View convertView) {
		if (!(convertView.getTag() instanceof RouteTripStopViewHolder)) {
			CrashUtils.w(this, "updateRouteTripStopView() > unexpected holder class '%s'! (%s)", convertView.getTag(), getLogTag());
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
			if (!this.showExtra || rts.getRoute() == null) {
				if (holder.rtsExtraV != null) {
					holder.rtsExtraV.setVisibility(View.GONE);
				}
				if (holder.routeFL != null) {
					holder.routeFL.setVisibility(View.GONE);
				}
				if (holder.tripHeadingBg != null) {
					holder.tripHeadingBg.setVisibility(View.GONE);
				}
			} else {
				final String authority = rts.getAuthority();
				final Route route = rts.getRoute();
				if (TextUtils.isEmpty(route.getShortName())) {
					holder.routeShortNameTv.setVisibility(View.INVISIBLE);
					if (holder.routeTypeImg.hasPaths() && poim.poi.getAuthority().equals(holder.routeTypeImg.getTag())) {
						holder.routeTypeImg.setVisibility(View.VISIBLE);
					} else {
						AgencyProperties agency = DataSourceProvider.get(getContext()).getAgency(getContext(), poim.poi.getAuthority());
						JPaths rtsRouteLogo = agency == null ? null : agency.getLogo(getContext());
						if (rtsRouteLogo != null) {
							holder.routeTypeImg.setJSON(rtsRouteLogo);
							holder.routeTypeImg.setTag(poim.poi.getAuthority());
							holder.routeTypeImg.setVisibility(View.VISIBLE);
						} else {
							holder.routeTypeImg.setVisibility(View.GONE);
						}
					}
				} else {
					holder.routeTypeImg.setVisibility(View.GONE);
					holder.routeShortNameTv.setText(Route.setShortNameSize(route.getShortName()));
					holder.routeShortNameTv.setVisibility(View.VISIBLE);
				}
				holder.routeFL.setVisibility(View.VISIBLE);
				holder.rtsExtraV.setVisibility(View.VISIBLE);
				final Long tripId;
				if (rts.getTrip() == null) {
					holder.tripHeadingBg.setVisibility(View.GONE);
					tripId = null;
				} else {
					tripId = rts.getTrip().getId();
					holder.tripHeadingTv.setText(rts.getTrip().getHeading(getContext()).toUpperCase(Locale.getDefault()));
					holder.tripHeadingBg.setVisibility(View.VISIBLE);
				}
				holder.rtsExtraV.setBackgroundColor(poim.getColor(getContext()));
				final Integer stopId = rts.getStop() == null ? null : rts.getStop().getId();
				holder.rtsExtraV.setOnClickListener(new MTOnClickListener() {
					@Override
					public void onClickMT(View view) {
						Activity activity = POIArrayAdapter.this.activityWR == null ? null : POIArrayAdapter.this.activityWR.get();
						if (activity == null || !(activity instanceof MainActivity)) {
							MTLog.w(POIArrayAdapter.this, "No activity available to open RTS fragment!");
							return;
						}
						leaving();
						((MainActivity) activity).addFragmentToStack(RTSRouteFragment.newInstance(authority, route.getId(), tripId, stopId, route));
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
		case POI.ITEM_STATUS_TYPE_NONE:
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			break;
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
		case POI.ITEM_STATUS_TYPE_NONE:
			statusViewHolder.statusV.setVisibility(View.INVISIBLE);
			break;
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

	private void disableTimeChangedReceiver() {
		if (this.timeChangedReceiverEnabled) {
			getContext().unregisterReceiver(this.timeChangedReceiver);
			this.timeChangedReceiverEnabled = false;
			this.nowToTheMinute = -1L;
		}
	}

	private void updateCommonView(CommonViewHolder holder, POIManager poim) {
		if (poim == null || poim.poi == null || holder == null) {
			return;
		}
		final POI poi = poim.poi;
		holder.uuid = poi.getUUID();
		if (holder.statusViewHolder != null) {
			holder.statusViewHolder.uuid = holder.uuid;
		}
		if (holder.uuid != null) {
			this.poiStatusViewHoldersWR.put(holder.uuid, holder.statusViewHolder);
		}
		if (holder.compassV != null) {
			holder.compassV.setLatLng(poim.getLat(), poim.getLng());
			this.compassImgsWR.put(holder.compassV, holder.distanceTv);
		}
		holder.nameTv.setText(poi.getName());
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
		if (holder.compassV != null) {
			if (holder.distanceTv != null && holder.distanceTv.getVisibility() == View.VISIBLE) {
				if (this.location != null && this.lastCompassInDegree >= 0 && this.location.getAccuracy() <= poim.getDistance()) {
					holder.compassV.generateAndSetHeading(this.location, this.lastCompassInDegree, this.locationDeclination);
				} else {
					holder.compassV.resetHeading();
				}
				holder.compassV.setVisibility(View.VISIBLE);
			} else {
				holder.compassV.resetHeading();
				holder.compassV.setVisibility(View.GONE);
			}
		}
		if (holder.locationTv != null) {
			if (TextUtils.isEmpty(poim.getLocation())) {
				holder.locationTv.setVisibility(View.GONE);
				holder.locationTv.setText(null);
			} else {
				holder.locationTv.setText(poim.getLocation());
				holder.locationTv.setVisibility(View.VISIBLE);
			}
		}
		if (this.showFavorite && this.favUUIDs != null && this.favUUIDs.contains(poi.getUUID())) {
			holder.favImg.setVisibility(View.VISIBLE);
		} else {
			holder.favImg.setVisibility(View.GONE);
		}
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

	@Nullable
	private RefreshFavoritesTask refreshFavoritesTask;

	private void refreshFavorites() {
		if (this.refreshFavoritesTask != null && this.refreshFavoritesTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return; // skipped, last refresh still in progress so probably good enough
		}
		this.refreshFavoritesTask = new RefreshFavoritesTask(this);
		TaskUtils.execute(this.refreshFavoritesTask);
	}

	private static class RefreshFavoritesTask extends MTAsyncTask<Integer, Void, ArrayList<Favorite>> {

		private final WeakReference<POIArrayAdapter> poiArrayAdapterWR;

		@Override
		public String getLogTag() {
			return POIArrayAdapter.class.getSimpleName() + ">" + RefreshFavoritesTask.class.getSimpleName();
		}

		private RefreshFavoritesTask(POIArrayAdapter poiArrayAdapter) {
			this.poiArrayAdapterWR = new WeakReference<POIArrayAdapter>(poiArrayAdapter);
		}

		@Override
		protected ArrayList<Favorite> doInBackgroundMT(Integer... params) {
			POIArrayAdapter poiArrayAdapter = this.poiArrayAdapterWR.get();
			if (poiArrayAdapter == null) {
				return null;
			}
			return FavoriteManager.findFavorites(poiArrayAdapter.getContext());
		}

		@Override
		protected void onPostExecute(@Nullable ArrayList<Favorite> result) {
			POIArrayAdapter poiArrayAdapter = this.poiArrayAdapterWR.get();
			if (poiArrayAdapter == null) {
				return;
			}
			poiArrayAdapter.setFavorites(result);
		}
	}

	private void setFavorites(@Nullable ArrayList<Favorite> favorites) {
		boolean newFav; // don't trigger update if favorites are the same
		boolean updatedFav; // don't trigger if favorites are the same OR were not set
		if (this.favUUIDs == null) {
			newFav = true; // favorite never set before
			updatedFav = false; // never set before so not updated
		} else if (CollectionUtils.getSize(favorites) != CollectionUtils.getSize(this.favUUIDs)) {
			newFav = true; // different size => different favorites
			updatedFav = true; // different size => different favorites
		} else {
			newFav = false; // favorite set before to the same size
			updatedFav = false; // already set with the same size
		}
		HashSet<String> newFavUUIDs = new HashSet<String>();
		HashMap<String, Integer> newFavUUIDsFolderIds = new HashMap<String, Integer>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				String uid = favorite.getFkId();
				if (!newFav && ( //
						(this.favUUIDs != null && !this.favUUIDs.contains(uid)) || //
						(this.favUUIDsFolderIds != null && this.favUUIDsFolderIds.containsKey(uid) && this.favUUIDsFolderIds.get(uid) != favorite.getFolderId()) //
						)) {
					newFav = true;
					updatedFav = true;
				}
				newFavUUIDs.add(uid);
				newFavUUIDsFolderIds.put(uid, favorite.getFolderId());
			}
		}
		if (!newFav) {
			if (this.favUUIDsFolderIds == null) {
				newFav = true; // favorite never set before
				updatedFav = false; // never set before so not updated
			} else {
				HashSet<Integer> oldFolderIds = new HashSet<Integer>();
				oldFolderIds.addAll(this.favUUIDsFolderIds.values());
				HashSet<Integer> newFolderIds = new HashSet<Integer>();
				newFolderIds.addAll(newFavUUIDsFolderIds.values());
				if (CollectionUtils.getSize(oldFolderIds) != CollectionUtils.getSize(newFolderIds)) {
					newFav = true; // different size => different favorites
					updatedFav = true; // different size => different favorites
				}
			}
		}
		this.favUUIDs = newFavUUIDs;
		this.favUUIDsFolderIds = newFavUUIDsFolderIds;
		if (newFav) {
			notifyDataSetChanged(true);
		}
		if (updatedFav) {
			if (this.favoriteUpdateListener != null) {
				this.favoriteUpdateListener.onFavoriteUpdated();
			}
		}
	}

	private static class InfiniteLoadingViewHolder {
		View progressBar;
		View worldExplored;
	}

	private static class ModuleViewHolder extends CommonViewHolder {
		ImageView moduleExtraTypeImg;
	}

	private static class RouteTripStopViewHolder extends CommonViewHolder {
		TextView routeShortNameTv;
		View routeFL;
		View rtsExtraV;
		MTJPathsView routeTypeImg;
		TextView tripHeadingTv;
		View tripHeadingBg;
	}

	private static class BasicPOIViewHolder extends CommonViewHolder {
	}

	private static class TextViewViewHolder extends CommonViewHolder {
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

	private static class AppStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv;
	}

	private static class ScheduleStatusViewHolder extends CommonStatusViewHolder {
		TextView dataNextLine1Tv;
		TextView dataNextLine2Tv;
	}

	private static class AvailabilityPercentStatusViewHolder extends CommonStatusViewHolder {
		TextView textTv;
		MTPieChartPercentView piePercentV;
	}

	public static class CommonStatusViewHolder {
		String uuid;
		View statusV;
		ImageView warningImg;
	}

	private static class FavoriteFolderHeaderViewHolder {
		TextView nameTv;
		View deleteBtn;
		View renameBtn;
	}

	private static class TypeHeaderViewHolder {
		TextView nameTv;
		TextView allBtnTv;
		View allBtn;
		View nearbyBtn;
		View moreBtn;
	}

	public interface TypeHeaderButtonsClickListener {

		int BUTTON_MORE = 0;
		int BUTTON_NEARBY = 1;
		int BUTTON_ALL = 2;

		boolean onTypeHeaderButtonClick(int buttonId, DataSourceType type);
	}
}
