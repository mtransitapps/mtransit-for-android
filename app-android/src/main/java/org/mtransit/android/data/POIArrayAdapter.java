package org.mtransit.android.data;

import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initBasicViewHolder;
import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initModuleViewHolder;
import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initPlaceViewHolder;
import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initRDSViewHolder;
import static org.mtransit.android.ui.view.poi.POIViewHolderBindingUtilsKt.initTextMessageViewHolder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.android.material.button.MaterialButton;
import com.google.common.primitives.Ints;

import org.mtransit.android.R;
import org.mtransit.android.common.repository.DefaultPreferenceRepository;
import org.mtransit.android.common.repository.LocalPreferenceRepository;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.LocationUtilsExtKt;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ResourceUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.commons.ui.widget.MTArrayAdapter;
import org.mtransit.android.databinding.LayoutPoiListBrowseHeaderBinding;
import org.mtransit.android.databinding.LayoutPoiListBrowseHeaderButtonBinding;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.datasource.POIRepository;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.provider.sensor.MTSensorManager;
import org.mtransit.android.task.ServiceUpdateLoader;
import org.mtransit.android.task.StatusLoader;
import org.mtransit.android.task.serviceupdate.ServiceUpdateLoaderProvider;
import org.mtransit.android.ui.MainActivity;
import org.mtransit.android.ui.common.UIColorUtils;
import org.mtransit.android.ui.favorites.FavoritesFragment;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.nearby.NearbyFragment;
import org.mtransit.android.ui.news.NewsListDetailFragment;
import org.mtransit.android.ui.rds.route.RDSRouteFragment;
import org.mtransit.android.ui.type.AgencyTypeFragment;
import org.mtransit.android.ui.view.MTCompassView;
import org.mtransit.android.ui.view.POIViewUtils;
import org.mtransit.android.ui.view.common.IFragment;
import org.mtransit.android.ui.view.common.MTTransitions;
import org.mtransit.android.ui.view.common.NavControllerExtKt;
import org.mtransit.android.ui.view.poi.BasicPOIViewHolder;
import org.mtransit.android.ui.view.poi.CommonViewHolder;
import org.mtransit.android.ui.view.poi.ModuleViewHolder;
import org.mtransit.android.ui.view.poi.PlaceViewHolder;
import org.mtransit.android.ui.view.poi.RouteDirectionStopViewHolder;
import org.mtransit.android.ui.view.poi.TextMessageViewHolder;
import org.mtransit.android.ui.view.poi.serviceupdate.POIServiceUpdateViewHolder;
import org.mtransit.android.ui.view.poi.status.POICommonStatusViewHolder;
import org.mtransit.android.ui.view.poi.status.POIStatusDataProvider;
import org.mtransit.android.util.CrashUtils;
import org.mtransit.android.util.DegreeUtils;
import org.mtransit.android.util.UIDirectionUtils;
import org.mtransit.android.util.UIFeatureFlags;
import org.mtransit.android.util.UIRouteUtils;
import org.mtransit.android.util.UITimeUtils;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@SuppressWarnings("WeakerAccess")
public class POIArrayAdapter extends MTArrayAdapter<POIManager> implements MTSensorManager.CompassListener, AdapterView.OnItemClickListener,
		AdapterView.OnItemLongClickListener, SensorEventListener, AbsListView.OnScrollListener, StatusLoader.StatusLoaderListener,
		ServiceUpdateLoader.ServiceUpdateLoaderListener, FavoriteManager.FavoriteUpdateListener, MTSensorManager.SensorTaskCompleted,
		UITimeUtils.TimeChangedReceiver.TimeChangedListener,
		POIStatusDataProvider, ServiceUpdateLoaderProvider {

	private static final String LOG_TAG = POIArrayAdapter.class.getSimpleName();

	@NonNull
	private String logTag = LOG_TAG;

	@NonNull
	@Override
	public String getLogTag() {
		return logTag;
	}

	public void setLogTag(@NonNull String tag) {
		this.logTag = LOG_TAG + "-" + tag;
	}

	public static final int TYPE_HEADER_NONE = 0;
	public static final int TYPE_HEADER_BASIC = 1;
	public static final int TYPE_HEADER_ALL_NEARBY = 2;
	public static final int TYPE_HEADER_MORE = 3;

	private final LayoutInflater layoutInflater;

	private LinkedHashMap<Integer, List<POIManager>> poisByType;

	@Nullable
	private HashSet<String> favUUIDs;

	@Nullable
	private HashMap<String, Integer> favUUIDsFolderIds;

	@Nullable
	private WeakReference<IFragment> activityWR;

	@Nullable
	private Location location;

	@Nullable
	private Integer lastCompassInDegree = null;

	@Nullable
	private Float locationDeclination = null;

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

	@Nullable
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

	@NonNull
	private final MTSensorManager sensorManager;
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;
	@NonNull
	private final DefaultPreferenceRepository defaultPrefRepository;
	@NonNull
	private final LocalPreferenceRepository localPreferenceRepository;
	@NonNull
	private final POIRepository poiRepository;
	@NonNull
	private final FavoriteManager favoriteManager;
	@NonNull
	private final StatusLoader statusLoader;
	@NonNull
	private final ServiceUpdateLoader serviceUpdateLoader;

	public POIArrayAdapter(@NonNull IFragment activity,
						   @NonNull MTSensorManager sensorManager,
						   @NonNull DataSourcesRepository dataSourcesRepository,
						   @NonNull DefaultPreferenceRepository defaultPrefRepository,
						   @NonNull LocalPreferenceRepository localPreferenceRepository,
						   @NonNull POIRepository poiRepository,
						   @NonNull FavoriteManager favoriteManager,
						   @NonNull StatusLoader statusLoader,
						   @NonNull ServiceUpdateLoader serviceUpdateLoader) {
		super(activity.requireContext(), -1);
		setActivity(activity);
		this.layoutInflater = LayoutInflater.from(getContext());
		this.sensorManager = sensorManager;
		this.dataSourcesRepository = dataSourcesRepository;
		this.defaultPrefRepository = defaultPrefRepository;
		this.localPreferenceRepository = localPreferenceRepository;
		this.poiRepository = poiRepository;
		this.favoriteManager = favoriteManager;
		this.statusLoader = statusLoader;
		this.serviceUpdateLoader = serviceUpdateLoader;
		this.dataSourcesRepository.readingAllAgencies().observe(activity, agencyProperties ->
				resetModulesStatus()
		);
	}

	public void setManualLayout(@Nullable ViewGroup manualLayout) {
		this.manualLayout = manualLayout;
	}

	public void setFavoriteUpdateListener(@NonNull FavoriteManager.FavoriteUpdateListener favoriteUpdateListener) {
		this.favoriteUpdateListener = favoriteUpdateListener;
	}

	@SuppressWarnings("unused")
	public void setShowStatus(boolean showData) {
		this.showStatus = showData;
	}

	@SuppressWarnings("unused")
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

	public void setInfiniteLoadingListener(@Nullable InfiniteLoadingListener infiniteLoadingListener) {
		this.infiniteLoadingListener = infiniteLoadingListener;
	}

	public interface InfiniteLoadingListener {

		boolean isLoadingMore();

		boolean showingDone();
	}

	private static final int VIEW_TYPE_COUNT = 12;

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
	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	@Override
	public int getItemViewType(int position) {
		final POIManager poim = getItem(position);
		if (poim == null) {
			if (this.showBrowseHeaderSection && position == 0) {
				return 0; // BROWSE SECTION
			}
			if (this.infiniteLoading && position + 1 == getCount()) {
				return 9; // LOADING FOOTER
			}
			if (this.showTypeHeader != TYPE_HEADER_NONE) {
				if (this.poisByType != null) {
					final Integer typeId = getItemTypeHeader(position);
					if (typeId != null) {
						if (this.favoriteManager.isFavoriteDataSourceId(typeId)) {
							return 10; // TYPE FAVORITE FOLDER
						}
						return 8; // TYPE HEADER
					}
				}
			}
			CrashUtils.w(this, "Cannot find type for at position '%s'!", position);
			return IGNORE_ITEM_VIEW_TYPE;
		}
		final int type = poim.poi.getType();
		final int statusType = poim.getStatusType();
		switch (type) {
		case POI.ITEM_VIEW_TYPE_PLACE:
			return 11; // PLACE
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return 7; // TEXT MESSAGE
		case POI.ITEM_VIEW_TYPE_MODULE:
			switch (statusType) {
			case POI.ITEM_STATUS_TYPE_APP:
				return 5; // MODULE & APP STATUS
			default:
				return 6; // MODULE
			}
		case POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP:
			switch (statusType) {
			case POI.ITEM_STATUS_TYPE_SCHEDULE:
				return 3; // RDS & SCHEDULE
			default:
				return 4; // RDS
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
				final List<POIManager> typePOIMs = this.poisByType.get(type);
				this.count += typePOIMs == null ? 0 : typePOIMs.size();
			}
		}
		if (this.infiniteLoading) {
			this.count++;
		}
	}

	@Override
	public int getPosition(@Nullable POIManager item) {
		int position = 0;
		if (this.showBrowseHeaderSection) {
			position++;
		}
		if (this.poisByType != null) {
			for (Integer type : this.poisByType.keySet()) {
				if (this.showTypeHeader != TYPE_HEADER_NONE) {
					position++;
				}
				final List<POIManager> typePOIMs = this.poisByType.get(type);
				int indexOf = typePOIMs == null ? -1 : typePOIMs.indexOf(item);
				if (indexOf >= 0) {
					return position + indexOf;
				}
				position += typePOIMs == null ? 0 : typePOIMs.size();
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
				final List<POIManager> typePOIMs = this.poisByType.get(type);
				final int typePOIMCount = typePOIMs == null ? 0 : typePOIMs.size();
				if (position >= index && position < index + typePOIMCount) {
					return typePOIMs.get(position - index);
				}
				index += typePOIMCount;
			}
		}
		return null;
	}

	@Nullable
	public POIManager getItem(@Nullable String uuid) {
		if (this.poisByType != null
				&& uuid != null && !uuid.isEmpty()) {
			for (Integer type : this.poisByType.keySet()) {
				final List<POIManager> typePOIMs = this.poisByType.get(type);
				if (typePOIMs != null) {
					for (POIManager poim : typePOIMs) {
						if (poim.poi.getUUID().equals(uuid)) {
							return poim;
						}
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
				final List<POIManager> typePOIMs = this.poisByType.get(type);
				index += typePOIMs == null ? 0 : typePOIMs.size();
			}
		}
		return null;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		final POIManager poim = getItem(position);
		if (poim == null) {
			if (this.showBrowseHeaderSection && position == 0) {
				return getBrowseHeaderSectionView(convertView, parent);
			}
			if (this.infiniteLoading && position + 1 == getCount()) {
				return getInfiniteLoadingView(convertView, parent);
			}
			if (this.showTypeHeader != TYPE_HEADER_NONE) {
				final Integer typeId = getItemTypeHeader(position);
				if (typeId != null) {
					if (this.favoriteManager.isFavoriteDataSourceId(typeId)) {
						final int favoriteFolderId = this.favoriteManager.extractFavoriteFolderId(typeId);
						final Favorite.Folder favoriteFolder = this.favoriteManager.getFolder(favoriteFolderId);
						if (favoriteFolder != null) {
							return getFavoriteFolderHeaderView(favoriteFolder, convertView, parent);
						}
					}
					final DataSourceType dst = DataSourceType.parseId(typeId);
					if (dst != null) {
						return getTypeHeaderView(dst, convertView, parent);
					}
				}
			}
			CrashUtils.w(this, "getView() > Cannot create view for null poi at position '%s'!", position);
			return getInfiniteLoadingView(null, parent); // ignore convert view since we don't know what it was
		}
		switch (poim.poi.getType()) {
		case POI.ITEM_VIEW_TYPE_PLACE:
			return getPlaceView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_TEXT_MESSAGE:
			return getTextMessageView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_MODULE:
			return getModuleView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_ROUTE_DIRECTION_STOP:
			return getRouteDirectionStopView(poim, convertView, parent);
		case POI.ITEM_VIEW_TYPE_BASIC_POI:
			return getBasicPOIView(poim, convertView, parent);
		default:
			CrashUtils.w(this, "getView() > Unknown view type at position %s!", position);
			return getBasicPOIView(poim, convertView, parent);
		}
	}

	@NonNull
	private View getInfiniteLoadingView(@Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.layout_poi_infinite_loading, parent, false);
			final InfiniteLoadingViewHolder holder = new InfiniteLoadingViewHolder();
			holder.progressBar = convertView.findViewById(R.id.progress_bar);
			holder.worldExplored = convertView.findViewById(R.id.worldExploredTv);
			convertView.setTag(holder);
		}
		final InfiniteLoadingViewHolder holder = (InfiniteLoadingViewHolder) convertView.getTag();
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

	private int nbDisplayedAgencyTypes = -1;

	private View getBrowseHeaderSectionView(@Nullable View convertView, @NonNull ViewGroup parent) {
		final Map<DataSourceType, List<AgencyProperties>> dstToAgencies = this.dataSourcesRepository.getAllTypeToAgencies();
		// noinspection deprecation // FIXME use live data
		final ArrayList<DataSourceType> allAgencyTypes = new ArrayList<>(
				this.dataSourcesRepository.filterDataSourceTypes(
						this.dataSourcesRepository.getAllSupportedDataSourceTypes()
				)
		);
		CollectionUtils.removeIfNN(allAgencyTypes, dst -> !dst.isHomeScreen());
		final boolean hasFavorites = (this.favUUIDs != null && !this.favUUIDs.isEmpty())
				|| (this.favUUIDsFolderIds != null && !this.favUUIDsFolderIds.isEmpty());
		if (hasFavorites && this.dataSourcesRepository.hasAgenciesEnabled()) {
			allAgencyTypes.add(0, DataSourceType.TYPE_FAVORITE); // 1st
		}
		if (!allAgencyTypes.isEmpty()) {
			if (!this.dataSourcesRepository.getAllNewsProvidersEnabled().isEmpty()) {
				allAgencyTypes.add(allAgencyTypes.size() - 1, DataSourceType.TYPE_NEWS); // LAST before MODULE
			}
		}
		final int nbDisplayedAgencyTypeCount = allAgencyTypes.size();
		if (convertView != null && this.nbDisplayedAgencyTypes == nbDisplayedAgencyTypeCount) {
			return convertView;
		}
		LayoutPoiListBrowseHeaderBinding convertViewBinding;
		if (convertView == null) {
			convertViewBinding = LayoutPoiListBrowseHeaderBinding.inflate(this.layoutInflater, parent, false);
		} else {
			convertViewBinding = LayoutPoiListBrowseHeaderBinding.bind(convertView);
		}
		final LinearLayout gridLL = convertViewBinding.gridLL;
		gridLL.removeAllViews();
		this.nbDisplayedAgencyTypes = nbDisplayedAgencyTypeCount;
		if (allAgencyTypes.isEmpty()) { // if no module installed > only show agencies list
			gridLL.setVisibility(View.GONE); // TODO? agency browse button could be useful to access list of available agencies w/o device location
			return convertViewBinding.getRoot();
		}
		int availableButtons = 0;
		final int maxButtonsPerLines = optimizeMaxButtonPerLines(
				nbDisplayedAgencyTypeCount,
				getContext().getResources().getBoolean(R.bool.two_pane) ? 6 : 3
		);
		ViewGroup gridLine = null;
		MaterialButton btn;
		for (int i = 0; i < nbDisplayedAgencyTypeCount; i++) {
			final DataSourceType dst = allAgencyTypes.get(i);
			if (dst.getId() == DataSourceType.TYPE_MODULE.getId()
					&& availableButtons == 0
					&& allAgencyTypes.size() > maxButtonsPerLines) {
				MTLog.d(this, "getBrowseHeaderSectionView() > SKIP modules (no room)");
				continue;
			}
			if (dst.getId() == DataSourceType.TYPE_PLACE.getId()) {
				MTLog.d(this, "getBrowseHeaderSectionView() > SKIP place");
				continue;
			}
			if (availableButtons == 0) {
				gridLine = makeHeaderBrowseLine();
				gridLL.addView(gridLine);
				availableButtons = maxButtonsPerLines;
			}
			btn = makeHeaderBrowseButton(gridLine);
			gridLine.addView(btn);
			btn.setText(dst.getShortNamesResId());
			if (UIFeatureFlags.F_HIDE_ONE_AGENCY_TYPE_TABS) {
				final List<AgencyProperties> dstAgencies = dstToAgencies.get(dst);
				final AgencyProperties oneAgency = dstAgencies == null || dstAgencies.size() != 1
						|| DataSourceType.TYPE_MODULE.equals(dstAgencies.get(0).getType()) ? null
						: dstAgencies.get(0);
				final String oneAgencyShortName = oneAgency == null ? null : oneAgency.getShortName();
				if (oneAgencyShortName != null && !oneAgencyShortName.isEmpty()) {
					final int shortNameCount = IAgencyProperties.countAgenciesListShortName(dstToAgencies.values(), oneAgencyShortName);
					if (shortNameCount == 1) {
						btn.setText(oneAgencyShortName);
					}
				}
			}
			if (dst.getIconResId() != -1) {
				btn.setIconResource(dst.getIconResId());
			} else {
				btn.setIcon(null);
			}
			btn.setOnClickListener(view ->
					onTypeHeaderButtonClick(view, TypeHeaderButtonsClickListener.BUTTON_ALL, dst)
			);
			if (UIFeatureFlags.F_HOME_SCREEN_BROWSE_COLORS_COUNT > 0) {
				final List<AgencyProperties> dstAgencies = dstToAgencies.get(dst);
				if (dstAgencies != null && !dstAgencies.isEmpty()) {
					String selectedAgencyAuthority = null;
					if (UIFeatureFlags.F_HOME_SCREEN_BROWSE_COLORS_COUNT == 1) {
						selectedAgencyAuthority = this.localPreferenceRepository.getValue( // TODO async?
								LocalPreferenceRepository.getPREFS_LCL_AGENCY_TYPE_TAB_AGENCY(dst.getId()),
								LocalPreferenceRepository.PREFS_LCL_AGENCY_TYPE_TAB_AGENCY_DEFAULT
						);
						if (selectedAgencyAuthority == null || selectedAgencyAuthority.isEmpty()) {
							selectedAgencyAuthority = dstAgencies.get(0).getAuthority();
						}
					}
					final ArrayList<Integer> colors = new ArrayList<>();
					for (AgencyProperties agency : dstAgencies) {
						if (UIFeatureFlags.F_HOME_SCREEN_BROWSE_COLORS_COUNT == 1
								&& !agency.getAuthority().equals(selectedAgencyAuthority)) {
							continue;
						}
						final Integer color = agency.getColorInt();
						if (color != null) {
							colors.add(UIColorUtils.adaptBackgroundColorToLightText(getContext(), color));
						}
					}
					if (colors.size() == 1) {
						btn.setBackgroundColor(colors.get(0));
					} else if (colors.size() >= 2) {
						final int max = Math.max(2, UIFeatureFlags.F_HOME_SCREEN_BROWSE_COLORS_COUNT); // gradient needs 2+
						final int[] colorArray = Ints.toArray(ColorUtils.filterColors(colors, max));
						final GradientDrawable gradient = new GradientDrawable(
								GradientDrawable.Orientation.LEFT_RIGHT,
								colorArray
						);
						gradient.setShape(GradientDrawable.RECTANGLE);
						gradient.setCornerRadius(ResourceUtils.convertDPtoPX(getContext(), 4));
						btn.setBackgroundTintList(null);
						btn.setBackgroundDrawable(gradient);
					}
				}
			}
			btn.setVisibility(View.VISIBLE);
			availableButtons--;
		}
		if (gridLine != null) {
			while (availableButtons > 0) {
				btn = makeHeaderBrowseButton(gridLine);
				gridLine.addView(btn);
				btn.setVisibility(View.INVISIBLE);
				availableButtons--;
			}
		}
		gridLL.setVisibility(View.VISIBLE);
		return convertViewBinding.getRoot();
	}

	protected static int optimizeMaxButtonPerLines(int nbDisplayedAgencyTypeCount, int maxButtonsPerLines) {
		int minNbDisplayedAgencyTypeCount = nbDisplayedAgencyTypeCount - 1;
		double nbLineRequired = (double) minNbDisplayedAgencyTypeCount / (double) maxButtonsPerLines;
		double nbLineForAll = (double) nbDisplayedAgencyTypeCount / (double) maxButtonsPerLines;
		double minNbLineRequired = Math.ceil(nbLineRequired);
		double minNbLineForAll = Math.ceil(nbLineForAll);
		int optimizedMaxButtonsPerLines;
		if (minNbLineRequired == 0.0d || minNbLineForAll - minNbLineRequired < 1.0f) {
			optimizedMaxButtonsPerLines = (int) Math.ceil(nbDisplayedAgencyTypeCount / minNbLineForAll);
		} else {
			optimizedMaxButtonsPerLines = (int) Math.ceil(minNbDisplayedAgencyTypeCount / nbLineRequired);
		}
		return optimizedMaxButtonsPerLines;
	}

	@NonNull
	private LinearLayout makeHeaderBrowseLine() {
		final LinearLayout linearLayout = new LinearLayout(getContext());
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		linearLayout.setBaselineAligned(false);
		return linearLayout;
	}

	@NonNull
	private MaterialButton makeHeaderBrowseButton(@Nullable ViewGroup root) {
		return (MaterialButton) LayoutPoiListBrowseHeaderButtonBinding.inflate(this.layoutInflater, root, false).getRoot();
	}

	@SuppressWarnings("UnusedReturnValue")
	@NonNull
	private View updateCommonViewManual(@NonNull POIManager poim, @NonNull View convertView) {
		if (!(convertView.getTag() instanceof CommonViewHolder)) {
			return convertView;
		}
		CommonViewHolder holder = (CommonViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		POICommonStatusViewHolder.fetchAndUpdateView(holder.getStatusViewHolder(), poim, this);
		POIServiceUpdateViewHolder.fetchAndUpdateView(holder.getServiceUpdateViewHolder(), poim, this);
		return convertView;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		showPoiViewerScreen(view, position);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		return showPoiMenu(view, position);
	}

	public interface OnClickHandledListener {
		void onLeaving();
	}

	@Nullable
	private WeakReference<OnClickHandledListener> onClickHandledListenerWR;

	public void setOnClickHandledListener(@Nullable OnClickHandledListener onClickHandledListener) {
		this.onClickHandledListenerWR = new WeakReference<>(onClickHandledListener);
	}

	public interface OnPOISelectedListener {
		boolean onPOISelected(@NonNull POIManager poim);

		boolean onPOILongSelected(@NonNull POIManager poim);
	}

	private WeakReference<OnPOISelectedListener> onPoiSelectedListenerWR;

	@SuppressWarnings("unused")
	public void setOnPoiSelectedListener(@Nullable OnPOISelectedListener onPoiSelectedListener) {
		this.onPoiSelectedListenerWR = new WeakReference<>(onPoiSelectedListener);
	}

	@SuppressWarnings("UnusedReturnValue")
	public boolean showPoiViewerScreen(@NonNull View view, int position) {
		boolean handled = false;
		final POIManager poim = getItem(position);
		if (poim != null) {
			OnPOISelectedListener listener = this.onPoiSelectedListenerWR == null ? null : this.onPoiSelectedListenerWR.get();
			handled = listener != null && listener.onPOISelected(poim);
			if (!handled) {
				handled = showPoiViewerScreen(view, poim);
			}
		}
		return handled;
	}

	private boolean showPoiMenu(View view, int position) {
		boolean handled = false;
		final POIManager poim = getItem(position);
		if (poim != null) {
			OnPOISelectedListener listener = this.onPoiSelectedListenerWR == null ? null : this.onPoiSelectedListenerWR.get();
			handled = listener != null && listener.onPOILongSelected(poim);
			if (!handled) {
				handled = showPoiMenu(view, poim);
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

	public boolean showPoiViewerScreen(@NonNull View view, @Nullable POIManager poim) {
		if (poim == null) {
			return false;
		}
		final Activity activity = getActivity();
		if (activity == null) {
			return false;
		}
		return poim.onActionItemClick(
				activity,
				view,
				this.favoriteManager,
				this.dataSourcesRepository,
				this.poiRepository,
				this.favoriteManager.getFavoriteFolders(),
				this.favoriteUpdateListener,
				this.onClickHandledListenerWR == null ? null : this.onClickHandledListenerWR.get()
		);
	}

	private boolean showPoiMenu(View view, POIManager poim) {
		if (poim == null) {
			return false;
		}
		Activity activity = getActivity();
		if (activity == null) {
			return false;
		}
		OnClickHandledListener listener = this.onClickHandledListenerWR == null ? null : this.onClickHandledListenerWR.get();
		return poim.onActionItemLongClick(
				activity,
				view,
				this.favoriteManager,
				this.dataSourcesRepository,
				this.poiRepository,
				this.favoriteManager.getFavoriteFolders(),
				this.favoriteUpdateListener,
				listener
		);
	}

	@Override
	public void onFavoriteUpdated() {
		refreshFavorites();
	}

	public void initPOITypes(@NonNull List<DataSourceType> poiTypes) {
		if (this.poisByType == null) {
			this.poisByType = new LinkedHashMap<>();
		}
		for (DataSourceType poiType : poiTypes) {
			if (!this.poisByType.containsKey(poiType.getId())) {
				this.poisByType.put(poiType.getId(), new ArrayList<>());
			}
		}
	}

	public void setPois(@Nullable List<POIManager> pois) {
		final boolean dataSetChanged = clearPois();
		appendPois(pois, dataSetChanged);
	}

	private boolean clearPois() {
		boolean dataSetChanged = false;
		if (this.poisByType != null && !this.poisByType.isEmpty()) {
			this.poisByType.clear();
			dataSetChanged = true;
		}
		if (!this.poiUUID.isEmpty()) {
			this.poiUUID.clear();
			dataSetChanged = true;
		}
		return dataSetChanged;
	}

	@NonNull
	private final HashSet<String> poiUUID = new HashSet<>();

	public void appendPois(@Nullable List<POIManager> pois) {
		appendPois(pois, false);
	}

	public void appendPois(@Nullable List<POIManager> pois, boolean dataSetChanged) {
		dataSetChanged = append(pois, dataSetChanged);
		if (!dataSetChanged) {
			MTLog.d(this, "appendPois() > SKIP (data not changed)");
			return;
		}
		MTLog.d(this, "appendPois() > data changed");
		notifyDataSetChanged();
	}

	private boolean append(@Nullable List<POIManager> pois, boolean dataSetChanged) {
		if (pois != null) {
			if (this.poisByType == null) {
				this.poisByType = new LinkedHashMap<>();
			}
			for (POIManager poim : pois) {
				List<POIManager> typePOIMs = CollectionUtils.getOrDefault(this.poisByType, poim.poi.getDataSourceTypeId(), new ArrayList<>());
				if (!this.poiUUID.contains(poim.poi.getUUID())) {
					typePOIMs.add(poim);
					this.poiUUID.add(poim.poi.getUUID());
					dataSetChanged = true;
				}
				this.poisByType.put(poim.poi.getDataSourceTypeId(), typePOIMs);
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
				final List<POIManager> typePOIMs = this.poisByType.get(type);
				this.poisCount += typePOIMs == null ? 0 : typePOIMs.size();
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
		this.closestPoiUuids = new HashSet<>();
		if (this.poisByType != null) {
			for (Integer type : this.poisByType.keySet()) {
				List<POIManager> poiManagers = this.poisByType.get(type);
				if (poiManagers == null || poiManagers.isEmpty()) {
					continue;
				}
				this.closestPoiUuids.addAll(
						LocationUtilsExtKt.findClosestPOISUuid(
								poiManagers
						)
				);
			}
		}
	}

	@SuppressWarnings("unused")
	public boolean hasClosestPOI() {
		return this.closestPoiUuids != null && !this.closestPoiUuids.isEmpty();
	}

	@SuppressWarnings("unused")
	public boolean isClosestPOI(int position) {
		if (this.closestPoiUuids == null) {
			return false;
		}
		POIManager poim = getItem(position);
		return poim != null && this.closestPoiUuids.contains(poim.poi.getUUID());
	}

	@Nullable
	public POIManager getClosestPOI() {
		if (this.closestPoiUuids == null || this.closestPoiUuids.isEmpty()) {
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

	@SuppressWarnings("deprecation")
	private static class UpdateDistanceWithStringTask extends MTCancellableAsyncTask<Location, Void, Void> {

		@NonNull
		private final WeakReference<POIArrayAdapter> poiArrayAdapterWR;

		@NonNull
		@Override
		public String getLogTag() {
			return POIArrayAdapter.class.getSimpleName() + ">" + UpdateDistanceWithStringTask.class.getSimpleName();
		}

		private UpdateDistanceWithStringTask(POIArrayAdapter poiArrayAdapter) {
			this.poiArrayAdapterWR = new WeakReference<>(poiArrayAdapter);
		}

		@Override
		protected Void doInBackgroundNotCancelledMT(Location... params) {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
			POIArrayAdapter poiArrayAdapter = this.poiArrayAdapterWR.get();
			if (poiArrayAdapter == null) {
				return null;
			}
			try {
				if (poiArrayAdapter.poisByType != null) {
					for (List<POIManager> poiManagers : poiArrayAdapter.poisByType.values()) {
						if (isCancelled()) {
							break;
						}
						LocationUtils.updateDistanceWithString(poiArrayAdapter.getContext(), poiManagers, params[0], this);
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while update POIs distance strings!");
			}
			return null;
		}

		@Override
		protected void onPostExecuteNotCancelledMT(Void result) {
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
	public void updateDistancesNowSync(@Nullable Location currentLocation) {
		if (currentLocation != null) {
			if (this.poisByType != null) {
				for (List<POIManager> pois : this.poisByType.values()) {
					LocationUtils.updateDistanceWithString(getContext(), pois, currentLocation, null);
				}
			}
			updateClosestPoi();
		}
		setLocation(currentLocation);
	}

	public void updateDistanceNowAsync(@Nullable Location currentLocation) {
		this.location = null; // clear current location to force refresh
		setLocation(currentLocation);
	}

	@Override
	public void onScroll(@NonNull AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// DO NOTHING
	}

	@Override
	public void onScrollStateChanged(@NonNull AbsListView view, int scrollState) {
		setScrollState(scrollState);
	}

	private void setScrollState(int scrollState) {
		this.scrollState = scrollState;
	}

	@Override
	public void onStatusLoaded(@NonNull POIStatus status) {
		if (this.showStatus) {
			final POICommonStatusViewHolder<?, ?> statusViewHolder = this.poiStatusViewHoldersWR.get(status.getTargetUUID());
			if (statusViewHolder != null && status.getTargetUUID().equals(statusViewHolder.getUuid())) {
				POICommonStatusViewHolder.updateView(statusViewHolder, status, this);
			} else {
				notifyDataSetChanged(false);
			}
		}
	}

	@Override
	public void onServiceUpdatesLoaded(@NonNull String targetUUID, @Nullable List<ServiceUpdate> serviceUpdates) {
		if (this.showServiceUpdate) {
			final POIServiceUpdateViewHolder serviceUpdateViewHolder = this.poiServiceUpdateViewHoldersWR.get(targetUUID);
			if (serviceUpdateViewHolder != null && targetUUID.equals(serviceUpdateViewHolder.getUuid())) {
				POIServiceUpdateViewHolder.updateView(serviceUpdateViewHolder, serviceUpdates, this);
			} else {
				notifyDataSetChanged(false);
			}
		}
	}

	@SuppressWarnings("UnusedReturnValue")
	private boolean resetModulesStatus() {
		boolean didReset = false;
		if (this.poisByType != null) {
			for (List<POIManager> poimList : this.poisByType.values()) {
				for (POIManager poim : poimList) {
					if (poim.poi.getType() == POI.ITEM_VIEW_TYPE_MODULE) {
						poim.resetLastFindTimestamps(); // force get status from provider
						didReset = true;
					}
				}
			}
		}
		return didReset;
	}

	public void notifyDataSetChanged(boolean force) {
		notifyDataSetChanged(force, Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS);
	}

	@NonNull
	private final Handler handler = new Handler(Looper.getMainLooper());

	@NonNull
	private final Runnable notifyDataSetChangedLater = () -> {
		notifyDataSetChanged(true); // it still really needs to show new data
	};

	public void notifyDataSetChanged(boolean force, long minAdapterThresholdInMs) {
		long now = UITimeUtils.currentTimeMillis();
		long adapterThreshold = Math.max(minAdapterThresholdInMs, Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS);
		if (this.scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE
				&& (force || (now - this.lastNotifyDataSetChanged) > adapterThreshold)) {
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
				if (tag instanceof CommonViewHolder) {
					POIManager poim = getItem(position);
					if (poim != null) {
						updateCommonViewManual(poim, view);
					}
					position++;
				}
			}
		}
	}

	public void setListView(@NonNull AbsListView listView) {
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
					this.manualLayout.addView(this.layoutInflater.inflate(R.layout.list_view_divider_rounded, this.manualLayout, false));
				}
				View itemView = getView(i, null, this.manualLayout);
				FrameLayout frameLayout = new FrameLayout(getContext());
				frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				frameLayout.addView(itemView);
				View selectorView = new View(getContext());
				SupportFactory.get().setBackground(selectorView, ThemeUtils.obtainStyledDrawable(getContext(), android.R.attr.selectableItemBackground));
				selectorView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
				frameLayout.addView(selectorView);
				final int position = i;
				frameLayout.setOnClickListener(view ->
						showPoiViewerScreen(view, position)
				);
				frameLayout.setOnLongClickListener(view ->
						showPoiMenu(view, position)
				);
				this.manualLayout.addView(frameLayout);
			}
		}
	}

	@SuppressWarnings("unused")
	public void scrollManualScrollViewTo(int x, int y) {
		if (this.manualScrollView != null) {
			this.manualScrollView.scrollTo(x, y);
		}
	}

	@SuppressLint("ClickableViewAccessibility") // TODO Accessibility
	public void setManualScrollView(@Nullable ScrollView scrollView) {
		this.manualScrollView = scrollView;
		if (scrollView == null) {
			return;
		}
		scrollView.setOnTouchListener((v, event) -> {
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
		});
	}

	public void setLocation(@Nullable Location newLocation) {
		if (newLocation == null) {
			return;
		}
		if (this.location == null || LocationUtils.isMoreRelevant(getLogTag(), this.location, newLocation)) {
			this.location = newLocation;
			this.locationDeclination = this.sensorManager.getLocationDeclination(this.location);
			if (!this.compassUpdatesEnabled) {
				this.sensorManager.registerCompassListener(this);
				this.compassUpdatesEnabled = true;
			}
			updateDistances(this.location);
		}
	}

	public void onPause() {
		if (this.activityWR != null) {
			this.activityWR.clear();
			this.activityWR = null;
		}
		if (this.compassUpdatesEnabled) {
			this.sensorManager.unregisterSensorListener(this);
			this.compassUpdatesEnabled = false;
		}
		this.handler.removeCallbacks(this.notifyDataSetChangedLater);
		TaskUtils.cancelQuietly(this.refreshFavoritesTask, true);
		disableTimeChangedReceiver();
	}

	@NonNull
	@Override
	public String toString() {
		return POIArrayAdapter.class.getSimpleName() + getLogTag();
	}

	public void onResume(@NonNull IFragment activity, @Nullable Location deviceLocation) {
		setActivity(activity);
		this.showingAccessibilityInfo = null; // force user preference check
		this.location = null; // clear current location to force refresh
		setLocation(deviceLocation);
		refreshFavorites();
		enableTimeChangedReceiver(); // need to be enabled even if no schedule status displayed to keep others statuses up-to-date
	}

	public void setActivity(@NonNull IFragment activity) {
		this.activityWR = new WeakReference<>(activity);
	}

	@Nullable
	private Activity getActivity() {
		IFragment activity = this.activityWR == null ? null : this.activityWR.get();
		return activity == null ? null : activity.getActivity();
	}

	@Override
	public void clear() {
		if (this.poisByType != null) {
			this.poisByType.clear();
			this.poisByType = null; // not initialized
		}
		resetCounts();
		this.poiUUID.clear();
		if (this.closestPoiUuids != null) {
			this.closestPoiUuids.clear();
			this.closestPoiUuids = null;
		}
		disableTimeChangedReceiver();
		this.compassImgsWR.clear();
		this.lastCompassChanged = -1;
		this.lastCompassInDegree = null;
		this.accelerometerValues = new float[3];
		this.magneticFieldValues = new float[3];
		this.lastNotifyDataSetChanged = -1L;
		this.handler.removeCallbacks(this.notifyDataSetChangedLater);
		this.poiStatusViewHoldersWR.clear();
		this.poiServiceUpdateViewHoldersWR.clear();
		TaskUtils.cancelQuietly(this.refreshFavoritesTask, true);
		TaskUtils.cancelQuietly(this.updateDistanceWithStringTask, true);
		this.location = null;
		this.locationDeclination = null;
		super.clear();
	}

	public void onDestroyView() {
		this.compassImgsWR.clear();
		this.poiStatusViewHoldersWR.clear();
		this.poiServiceUpdateViewHoldersWR.clear();
		if (this.onClickHandledListenerWR != null) {
			this.onClickHandledListenerWR.clear();
		}
		if (this.onPoiSelectedListenerWR != null) {
			this.onPoiSelectedListenerWR.clear();
		}
	}

	public void onDestroy() {
		disableTimeChangedReceiver();
		if (this.poisByType != null) {
			this.poisByType.clear();
			this.poisByType = null;
		}
		resetCounts();
		this.poiUUID.clear();
		onDestroyView();
		this.infiniteLoadingListener = null;
	}

	@Override
	public void updateCompass(float orientation, boolean force) {
		if (getPoisCount() == 0) {
			return;
		}
		long now = UITimeUtils.currentTimeMillis();
		int roundedOrientation = DegreeUtils.convertToPositive360Degree((int) orientation);
		this.sensorManager.updateCompass(
				force,
				this.location,
				roundedOrientation,
				now,
				this.scrollState,
				this.lastCompassChanged,
				this.lastCompassInDegree,
				Constants.ADAPTER_NOTIFY_THRESHOLD_IN_MS,
				this
		);
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
		for (WeakHashMap.Entry<MTCompassView, View> compassAndDistance : this.compassImgsWR.entrySet()) {
			MTCompassView compassView = compassAndDistance.getKey();
			if (compassView != null && compassView.isHeadingSet()) {
				compassView.generateAndSetHeadingN(this.location, this.lastCompassInDegree, this.locationDeclination);
			}
		}
	}

	@Override
	public void onSensorChanged(SensorEvent se) {
		IFragment activity = this.activityWR == null ? null : this.activityWR.get();
		if (activity == null) {
			return;
		}
		this.sensorManager.checkForCompass(activity, se, this.accelerometerValues, this.magneticFieldValues, this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// DO NOTHING
	}

	@LayoutRes
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

	@Nullable
	private WeakReference<TypeHeaderButtonsClickListener> typeHeaderButtonsClickListenerWR;

	public void setOnTypeHeaderButtonsClickListener(@Nullable TypeHeaderButtonsClickListener listener) {
		this.typeHeaderButtonsClickListenerWR = new WeakReference<>(listener);
	}

	private void onTypeHeaderButtonClick(@NonNull View view, int buttonId, @NonNull DataSourceType type) {
		final TypeHeaderButtonsClickListener listener = this.typeHeaderButtonsClickListenerWR == null ? null : this.typeHeaderButtonsClickListenerWR.get();
		if (listener != null && listener.onTypeHeaderButtonClick(buttonId, type)) {
			MTLog.d(this, "onTypeHeaderButtonClick() > SKIP (listener handled)");
			return;
		}
		switch (buttonId) {
		case TypeHeaderButtonsClickListener.BUTTON_ALL:
			leaving();
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = Navigation.findNavController(view);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							.addSharedElement(view, view.getTransitionName())
							.build();
				}
				if (type == DataSourceType.TYPE_FAVORITE) {
					NavControllerExtKt.navigateF(navController,
							R.id.nav_to_favorite_screen,
							FavoritesFragment.newInstanceArgs(),
							null,
							extras
					);
				} else if (type == DataSourceType.TYPE_NEWS) {
					NavControllerExtKt.navigateF(navController,
							R.id.nav_to_news_screen,
							NewsListDetailFragment.newInstanceArgs(),
							null,
							extras
					);
				} else {
					NavControllerExtKt.navigateF(navController,
							R.id.nav_to_type_screen,
							AgencyTypeFragment.newInstanceArgs(type),
							null,
							extras
					);
				}
			} else {
				final Activity activity = getActivity();
				if (activity != null) {
					final ABFragment fragment;
					if (type == DataSourceType.TYPE_FAVORITE) {
						fragment = FavoritesFragment.newInstance();
					} else if (type == DataSourceType.TYPE_NEWS) {
						fragment = NewsListDetailFragment.newInstance();
					} else {
						fragment = AgencyTypeFragment.newInstance(type);
					}
					((MainActivity) activity).addFragmentToStack(
							fragment
					);
				}
			}
			break;
		case TypeHeaderButtonsClickListener.BUTTON_NEARBY:
			leaving();
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = Navigation.findNavController(view);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							.addSharedElement(view, view.getTransitionName())
							.build();
				}
				NavControllerExtKt.navigateF(navController,
						R.id.nav_to_nearby_screen,
						NearbyFragment.newNearbyInstanceArgs(type),
						null,
						extras
				);
			} else {
				final Activity activity = getActivity();
				if (activity != null) {
					((MainActivity) activity).addFragmentToStack(
							NearbyFragment.newNearbyInstance(type)
					);
				}
			}
			break;
		case TypeHeaderButtonsClickListener.BUTTON_MORE:
			leaving();
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = Navigation.findNavController(view);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							.addSharedElement(view, view.getTransitionName())
							.build();
				}
				NavControllerExtKt.navigateF(navController,
						R.id.nav_to_type_screen,
						AgencyTypeFragment.newInstanceArgs(type),
						null,
						extras
				);
			} else {
				final Activity activity = getActivity();
				if (activity != null) {
					((MainActivity) activity).addFragmentToStack(
							AgencyTypeFragment.newInstance(type)
					);
				}
			}
			break;
		default:
			MTLog.w(this, "Unexpected type header button %s'' click", type);
		}
	}

	private void leaving() {
		final OnClickHandledListener onClickHandledListener = this.onClickHandledListenerWR == null ? null : this.onClickHandledListenerWR.get();
		if (onClickHandledListener != null) {
			onClickHandledListener.onLeaving();
		}
	}

	@NonNull
	private View getTypeHeaderView(@NonNull final DataSourceType type, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			final int layoutRes = getTypeHeaderLayoutResId();
			convertView = this.layoutInflater.inflate(layoutRes, parent, false);
			TypeHeaderViewHolder holder = new TypeHeaderViewHolder();
			holder.nameTv = convertView.findViewById(R.id.name);
			holder.nearbyBtn = convertView.findViewById(R.id.nearbyBtn);
			holder.allBtn = convertView.findViewById(R.id.allBtn);
			holder.moreBtn = convertView.findViewById(R.id.moreBtn);
			holder.layout = convertView;
			convertView.setTag(holder);
		}
		TypeHeaderViewHolder holder = (TypeHeaderViewHolder) convertView.getTag();
		holder.nameTv.setText(this.showTypeHeaderNearby ? type.getNearbyName(holder.nameTv.getContext()) : type.getPoiShortName(holder.nameTv.getContext()));
		if (type.getIconResId() != -1) {
			holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(type.getIconResId(), 0, 0, 0);
		}
		if (holder.allBtn != null) {
			holder.allBtn.setOnClickListener(view ->
					onTypeHeaderButtonClick(view, TypeHeaderButtonsClickListener.BUTTON_ALL, type)
			);
		}
		if (holder.nearbyBtn != null) {
			holder.nearbyBtn.setOnClickListener(view ->
					onTypeHeaderButtonClick(view, TypeHeaderButtonsClickListener.BUTTON_NEARBY, type)
			);
		}
		if (holder.moreBtn != null) {
			holder.moreBtn.setOnClickListener(view ->
					onTypeHeaderButtonClick(view, TypeHeaderButtonsClickListener.BUTTON_MORE, type)
			);
			holder.layout.setOnClickListener(view ->
					holder.moreBtn.performClick()
			);
		} else {
			holder.layout.setClickable(false);
		}
		return convertView;
	}

	private View getFavoriteFolderHeaderView(final @NonNull Favorite.Folder favoriteFolder,
											 @Nullable View convertView,
											 @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.layout_poi_list_header_with_delete, parent, false);
			FavoriteFolderHeaderViewHolder holder = new FavoriteFolderHeaderViewHolder();
			holder.nameTv = convertView.findViewById(R.id.name);
			holder.renameBtn = convertView.findViewById(R.id.renameBtn);
			holder.deleteBtn = convertView.findViewById(R.id.deleteBtn);
			convertView.setTag(holder);
		}
		FavoriteFolderHeaderViewHolder holder = (FavoriteFolderHeaderViewHolder) convertView.getTag();
		holder.nameTv.setText(favoriteFolder.getName());
		if (holder.renameBtn != null) {
			holder.renameBtn.setOnClickListener(view -> {
				final Activity activity = POIArrayAdapter.this.getActivity();
				favoriteManager.showUpdateFolderDialog(activity, POIArrayAdapter.this.layoutInflater, favoriteFolder,
						POIArrayAdapter.this.favoriteUpdateListener);
			});
		}
		if (holder.deleteBtn != null) {
			holder.deleteBtn.setOnClickListener(view -> {
				final Activity activity = POIArrayAdapter.this.getActivity();
				favoriteManager.showDeleteFolderDialog(activity, favoriteFolder, POIArrayAdapter.this.favoriteUpdateListener);
			});
		}
		return convertView;
	}

	@NonNull
	private final WeakHashMap<String, POICommonStatusViewHolder<?, ?>> poiStatusViewHoldersWR = new WeakHashMap<>();

	@NonNull
	private final WeakHashMap<String, POIServiceUpdateViewHolder> poiServiceUpdateViewHoldersWR = new WeakHashMap<>();

	@NonNull
	private View getBasicPOIView(@NonNull POIManager poim, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getBasicPOILayout(poim.getStatusType()), parent, false);
			final BasicPOIViewHolder holder = initBasicViewHolder(convertView, poim.poi.getUUID());
			holder.setStatusViewHolder(POICommonStatusViewHolder.init(poim.poi, convertView));
			holder.setServiceUpdateViewHolder(POIServiceUpdateViewHolder.init(poim.poi, convertView));
			convertView.setTag(holder);
		}
		updateBasicPOIView(poim, convertView);
		return convertView;
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

	@NonNull
	private final WeakHashMap<MTCompassView, View> compassImgsWR = new WeakHashMap<>();

	@SuppressWarnings("UnusedReturnValue")
	@NonNull
	private View updateBasicPOIView(@NonNull POIManager poim, @NonNull View convertView) {
		BasicPOIViewHolder holder = (BasicPOIViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		POICommonStatusViewHolder.fetchAndUpdateView(holder.getStatusViewHolder(), poim, this);
		POIServiceUpdateViewHolder.fetchAndUpdateView(holder.getServiceUpdateViewHolder(), poim, this);
		return convertView;
	}

	@LayoutRes
	private int getRDSLayout(int status) {
		int layoutRes = R.layout.layout_poi_rds;
		switch (status) {
		case POI.ITEM_STATUS_TYPE_NONE:
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			layoutRes = R.layout.layout_poi_rds_with_schedule;
			break;
		default:
			MTLog.w(this, "Unexpected status '%s' (rds view w/o status)!", status);
			break;
		}
		return layoutRes;
	}

	@NonNull
	private View getPlaceView(@NonNull POIManager poim, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.layout_poi_place, parent, false);
			final PlaceViewHolder holder = initPlaceViewHolder(convertView, poim.poi.getUUID());
			convertView.setTag(holder);
		}
		updatePlaceView(poim, convertView);
		return convertView;
	}

	@SuppressWarnings("UnusedReturnValue")
	@NonNull
	private View updatePlaceView(@NonNull POIManager poim, @NonNull View convertView) {
		final PlaceViewHolder holder = (PlaceViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		initPlaceExtra(poim, holder);
		return convertView;
	}

	private void initPlaceExtra(@NonNull POIManager poim, @NonNull PlaceViewHolder holder) {
		if (this.showExtra && poim.poi instanceof Place) {
			final Place place = (Place) poim.poi;
			POIViewUtils.setupPOIExtraLayoutBackground(holder.getPlaceIconImg(), poim, dataSourcesRepository);
			final RequestManager glideRequestManager;
			if (getActivity() != null && getActivity() instanceof FragmentActivity) {
				glideRequestManager = Glide.with((FragmentActivity) getActivity());
			} else {
				glideRequestManager = Glide.with(holder.getView().getContext());
			}
			glideRequestManager
					.load(place.getIconUrl())
					.into(holder.getPlaceIconImg());
			holder.getPlaceIconImg().setVisibility(View.VISIBLE);
		} else {
			holder.getPlaceIconImg().setVisibility(View.GONE);
		}
	}

	@NonNull
	private View getTextMessageView(@NonNull POIManager poim, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.layout_poi_basic, parent, false);
			final TextMessageViewHolder holder = initTextMessageViewHolder(convertView, poim.poi.getUUID());
			convertView.setTag(holder);
		}
		updateTextMessageView(poim, convertView);
		return convertView;
	}

	@SuppressWarnings("UnusedReturnValue")
	@NonNull
	private View updateTextMessageView(@NonNull POIManager poim, @NonNull View convertView) {
		TextMessageViewHolder holder = (TextMessageViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		return convertView;
	}

	@NonNull
	private View getModuleView(@NonNull POIManager poim, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getModuleLayout(poim.getStatusType()), parent, false);
			final ModuleViewHolder holder = initModuleViewHolder(convertView, poim.poi.getUUID());
			holder.setStatusViewHolder(POICommonStatusViewHolder.init(poim.poi, convertView));
			holder.setServiceUpdateViewHolder(POIServiceUpdateViewHolder.init(poim.poi, convertView));
			convertView.setTag(holder);
		}
		updateModuleView(poim, convertView);
		return convertView;
	}

	@SuppressWarnings("UnusedReturnValue")
	@NonNull
	private View updateModuleView(@NonNull POIManager poim, @NonNull View convertView) {
		ModuleViewHolder holder = (ModuleViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		updateModuleExtra(poim, holder);
		POICommonStatusViewHolder.fetchAndUpdateView(holder.getStatusViewHolder(), poim, this);
		POIServiceUpdateViewHolder.fetchAndUpdateView(holder.getServiceUpdateViewHolder(), poim, this);
		return convertView;
	}

	private void updateModuleExtra(@NonNull POIManager poim, @NonNull ModuleViewHolder holder) {
		if (this.showExtra && poim.poi instanceof Module) {
			Module module = (Module) poim.poi;
			POIViewUtils.setupPOIExtraLayoutBackground(holder.getModuleExtraTypeImg(), poim, dataSourcesRepository);
			DataSourceType moduleType = DataSourceType.parseId(module.getTargetTypeId());
			if (moduleType != null) {
				holder.getModuleExtraTypeImg().setImageResource(moduleType.getIconResId());
			} else {
				holder.getModuleExtraTypeImg().setImageResource(0);
			}
			holder.getModuleExtraTypeImg().setVisibility(View.VISIBLE);
		} else {
			holder.getModuleExtraTypeImg().setVisibility(View.GONE);
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

	@UiThread
	@NonNull
	private View getRouteDirectionStopView(@NonNull POIManager poim, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(getRDSLayout(poim.getStatusType()), parent, false);
			final RouteDirectionStopViewHolder holder = initRDSViewHolder(convertView, poim.poi.getUUID());
			holder.setStatusViewHolder(POICommonStatusViewHolder.init(poim.poi, convertView));
			holder.setServiceUpdateViewHolder(POIServiceUpdateViewHolder.init(poim.poi, convertView, convertView.findViewById(R.id.route_direction_service_update_img)));
			convertView.setTag(holder);
		}
		updateRouteDirectionStopView(poim, convertView);
		return convertView;
	}

	@SuppressWarnings("UnusedReturnValue")
	@UiThread
	@NonNull
	private View updateRouteDirectionStopView(@NonNull POIManager poim, @NonNull View convertView) {
		if (!(convertView.getTag() instanceof RouteDirectionStopViewHolder)) {
			CrashUtils.w(this, "updateRouteDirectionStopView() > unexpected holder class '%s'! (%s)", convertView.getTag(), getLogTag());
			return convertView;
		}
		RouteDirectionStopViewHolder holder = (RouteDirectionStopViewHolder) convertView.getTag();
		updateCommonView(holder, poim);
		updateRDSExtra(poim, holder);
		POICommonStatusViewHolder.fetchAndUpdateView(holder.getStatusViewHolder(), poim, this);
		POIServiceUpdateViewHolder.fetchAndUpdateView(holder.getServiceUpdateViewHolder(), poim, this);
		return convertView;
	}

	private boolean showExtra = true;

	public void setShowExtra(boolean showExtra) {
		this.showExtra = showExtra;
	}

	@UiThread
	private void updateRDSExtra(POIManager poim, RouteDirectionStopViewHolder holder) {
		if (!(poim.poi instanceof RouteDirectionStop)) {
			return;
		}
		final RouteDirectionStop rds = (RouteDirectionStop) poim.poi;
		if (!this.showExtra) {
			holder.getRdsExtraV().setVisibility(View.GONE);
			holder.getRouteFL().setVisibility(View.GONE);
			holder.getDirectionHeadingBg().setVisibility(View.GONE);
			holder.getNoExtra().setVisibility(View.VISIBLE);
			return;
		}
		final Route route = rds.getRoute();
		if (TextUtils.isEmpty(route.getShortName())) {
			holder.getRouteShortNameTv().setVisibility(View.INVISIBLE);
			if (holder.getRouteTypeImg().hasPaths() && poim.poi.getAuthority().equals(holder.getRouteTypeImg().getTag())) {
				holder.getRouteTypeImg().setVisibility(View.VISIBLE);
			} else {
				final AgencyProperties agency = this.dataSourcesRepository.getAgency(poim.poi.getAuthority());
				final JPaths rdsRouteLogo = agency == null ? null : agency.getLogo();
				if (rdsRouteLogo != null) {
					holder.getRouteTypeImg().setJSON(rdsRouteLogo);
					holder.getRouteTypeImg().setTag(poim.poi.getAuthority());
					holder.getRouteTypeImg().setVisibility(View.VISIBLE);
				} else {
					holder.getRouteTypeImg().setVisibility(View.GONE);
				}
			}
		} else {
			holder.getRouteTypeImg().setVisibility(View.GONE);
			holder.getRouteShortNameTv().setText(UIRouteUtils.decorateRouteShortName(getContext(), route.getShortName()));
			holder.getRouteShortNameTv().setVisibility(View.VISIBLE);
		}
		holder.getRouteFL().setVisibility(View.VISIBLE);
		holder.getRdsExtraV().setVisibility(View.VISIBLE);
		holder.getNoExtra().setVisibility(View.GONE);
		holder.getDirectionHeadingTv().setText(
				UIDirectionUtils.decorateDirection(getContext(), rds.getDirection().getUIHeading(getContext(), true), true)
		);
		holder.getDirectionHeadingBg().setVisibility(View.VISIBLE);
		POIViewUtils.setupPOIExtraLayoutBackground(holder.getRdsExtraV(), poim, dataSourcesRepository);
		holder.getRdsExtraV().setOnClickListener(view -> {
			leaving();
			MTTransitions.setTransitionName(view, "r_" + rds.getAuthority() + "_" + rds.getRoute().getId());
			if (FeatureFlags.F_NAVIGATION) {
				final NavController navController = Navigation.findNavController(view);
				FragmentNavigator.Extras extras = null;
				if (FeatureFlags.F_TRANSITION) {
					extras = new FragmentNavigator.Extras.Builder()
							.addSharedElement(view, view.getTransitionName())
							.build();
				}
				NavControllerExtKt.navigateF(navController,
						R.id.nav_to_rds_route_screen,
						RDSRouteFragment.newInstanceArgs(rds),
						null,
						extras
				);
			} else {
				final Activity activity = POIArrayAdapter.this.getActivity();
				if (!(activity instanceof MainActivity)) {
					MTLog.w(POIArrayAdapter.this, "No activity available to open RDS fragment!");
					return;
				}
				((MainActivity) activity).addFragmentToStack(
						RDSRouteFragment.newInstance(rds),
						view
				);
			}
		});
	}

	@Override
	public boolean isShowingStatus() {
		return this.showStatus;
	}

	@NonNull
	@Override
	public StatusLoader providesStatusLoader() {
		return this.statusLoader;
	}

	@Override
	public boolean isShowingServiceUpdates() {
		return this.showServiceUpdate;
	}

	@NonNull
	@Override
	public ServiceUpdateLoader providesServiceUpdateLoader() {
		return this.serviceUpdateLoader;
	}

	@Nullable
	private Set<String> ignoredTargetUUIDsOrUnknown = new HashSet<>(); // null == unknown

	public boolean setIgnoredTargetUUIDs(@Nullable Collection<String> ignoredTargetUUIDs) {
		boolean changed = false;
		if (ignoredTargetUUIDs == null) {
			if (this.ignoredTargetUUIDsOrUnknown != null) {
				this.ignoredTargetUUIDsOrUnknown = null;
				changed = true;
			}
		} else if (this.ignoredTargetUUIDsOrUnknown == null) {
			this.ignoredTargetUUIDsOrUnknown = new HashSet<>(ignoredTargetUUIDs);
			changed = true;
		} else if (!CollectionUtils.equalsCollectionContent(ignoredTargetUUIDs, this.ignoredTargetUUIDsOrUnknown)) {
			this.ignoredTargetUUIDsOrUnknown.clear();
			this.ignoredTargetUUIDsOrUnknown.addAll(ignoredTargetUUIDs);
			changed = true;
		}
		return changed;
	}

	@Nullable
	@Override
	public Collection<String> getIgnoredTargetUUIDsOrUnknown() {
		return this.ignoredTargetUUIDsOrUnknown;
	}

	@Override
	public long getNowToTheMinute() {
		if (this.nowToTheMinute < 0) {
			resetNowToTheMinute();
			enableTimeChangedReceiver();
		}
		return this.nowToTheMinute;
	}

	private void resetNowToTheMinute() {
		this.nowToTheMinute = UITimeUtils.currentTimeToTheMinuteMillis();
		notifyDataSetChanged(false);
	}

	@Override
	public void onTimeChanged() {
		resetNowToTheMinute();
	}

	private final UITimeUtils.TimeChangedReceiver timeChangedReceiver = new UITimeUtils.TimeChangedReceiver(this);

	private void enableTimeChangedReceiver() {
		if (!this.timeChangedReceiverEnabled) {
			ContextCompat.registerReceiver(getContext(), timeChangedReceiver, UITimeUtils.TIME_CHANGED_INTENT_FILTER, ContextCompat.RECEIVER_NOT_EXPORTED);
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

	@Nullable
	private Boolean showingAccessibilityInfo = null;

	public boolean isShowingAccessibilityInfo() {
		if (this.showingAccessibilityInfo == null) {
			this.showingAccessibilityInfo = this.defaultPrefRepository.getValue(DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY, DefaultPreferenceRepository.PREFS_SHOW_ACCESSIBILITY_DEFAULT);
		}
		return this.showingAccessibilityInfo;
	}

	private void updateCommonView(CommonViewHolder holder, POIManager poim) {
		if (poim == null || holder == null) {
			return;
		}
		final POI poi = poim.poi;
		holder.setUuid(poi.getUUID());
		MTTransitions.setTransitionName(holder.getView(), "poi_" + poi.getUUID());
		if (holder.getStatusViewHolder() != null) {
			holder.getStatusViewHolder().setUuid(poi.getUUID());
		}
		this.poiStatusViewHoldersWR.put(holder.getUuid(), holder.getStatusViewHolder());
		if (holder.getServiceUpdateViewHolder() != null) {
			holder.getServiceUpdateViewHolder().setTarget(poi);
		}
		this.poiServiceUpdateViewHoldersWR.put(holder.getUuid(), holder.getServiceUpdateViewHolder());
		holder.getNameTv().setText(POIManagerExtKt.getLabelDecorated(poi, getContext(), isShowingAccessibilityInfo()));
		if (holder.getDistanceTv() != null) {
			if (poim.getDistanceString() != null) {
				if (!poim.getDistanceString().equals(holder.getDistanceTv().getText())) {
					holder.getDistanceTv().setText(poim.getDistanceString());
				}
				holder.getDistanceTv().setVisibility(View.VISIBLE);
			} else {
				holder.getDistanceTv().setVisibility(View.GONE);
				holder.getDistanceTv().setText(null);
			}
		}
		if (holder.getCompassV() != null) {
			holder.getCompassV().setLatLng(poim.getLat(), poim.getLng());
			this.compassImgsWR.put(holder.getCompassV(), holder.getDistanceTv());
		}
		if (holder.getCompassV() != null) {
			if (holder.getDistanceTv() != null && holder.getDistanceTv().getVisibility() == View.VISIBLE) {
				if (this.location != null
						&& this.lastCompassInDegree != null
						&& this.location.getAccuracy() <= poim.getDistance()) {
					holder.getCompassV().generateAndSetHeadingN(this.location, this.lastCompassInDegree, this.locationDeclination);
				} else {
					holder.getCompassV().resetHeading();
				}
				holder.getCompassV().setVisibility(View.VISIBLE);
			} else {
				holder.getCompassV().resetHeading();
				holder.getCompassV().setVisibility(View.GONE);
			}
		}
		if (holder.getLocationTv() != null) {
			if (TextUtils.isEmpty(poim.getLocation())) {
				holder.getLocationTv().setVisibility(View.GONE);
				holder.getLocationTv().setText(null);
			} else {
				holder.getLocationTv().setText(poim.getLocation());
				holder.getLocationTv().setVisibility(View.VISIBLE);
			}
		}
		if (this.showFavorite && this.favUUIDs != null && this.favUUIDs.contains(poi.getUUID())) {
			holder.getFavImg().setVisibility(View.VISIBLE);
		} else {
			holder.getFavImg().setVisibility(View.GONE);
		}
		int index;
		if (this.closestPoiUuids != null && this.closestPoiUuids.contains(poi.getUUID())) {
			index = 0;
		} else {
			index = -1;
		}
		//noinspection SwitchStatementWithTooFewBranches
		switch (index) {
		case 0:
			holder.getNameTv().setTypeface(Typeface.DEFAULT_BOLD);
			if (holder.getDistanceTv() != null) {
				holder.getDistanceTv().setTypeface(Typeface.DEFAULT_BOLD);
			}
			break;
		default:
			holder.getNameTv().setTypeface(Typeface.DEFAULT);
			if (holder.getDistanceTv() != null) {
				holder.getDistanceTv().setTypeface(Typeface.DEFAULT);
			}
			break;
		}
	}

	@Nullable
	private RefreshFavoritesTask refreshFavoritesTask;

	@SuppressWarnings("deprecation")
	private void refreshFavorites() {
		if (this.refreshFavoritesTask != null && this.refreshFavoritesTask.getStatus() == MTCancellableAsyncTask.Status.RUNNING) {
			return; // skipped, last refresh still in progress so probably good enough
		}
		this.refreshFavoritesTask = new RefreshFavoritesTask(this);
		TaskUtils.execute(this.refreshFavoritesTask);
	}

	@SuppressWarnings("deprecation")
	private static class RefreshFavoritesTask extends MTCancellableAsyncTask<Integer, Void, List<Favorite>> {

		@NonNull
		private final WeakReference<POIArrayAdapter> poiArrayAdapterWR;

		@NonNull
		@Override
		public String getLogTag() {
			return POIArrayAdapter.class.getSimpleName() + ">" + RefreshFavoritesTask.class.getSimpleName();
		}

		private RefreshFavoritesTask(@Nullable POIArrayAdapter poiArrayAdapter) {
			this.poiArrayAdapterWR = new WeakReference<>(poiArrayAdapter);
		}

		@Override
		protected List<Favorite> doInBackgroundNotCancelledMT(Integer... params) {
			final POIArrayAdapter poiArrayAdapter = this.poiArrayAdapterWR.get();
			if (poiArrayAdapter == null) {
				return null;
			}
			return poiArrayAdapter.favoriteManager.findFavorites(poiArrayAdapter.getContext());
		}

		@Override
		protected void onPostExecuteNotCancelledMT(@Nullable List<Favorite> result) {
			final POIArrayAdapter poiArrayAdapter = this.poiArrayAdapterWR.get();
			if (poiArrayAdapter == null) {
				return;
			}
			poiArrayAdapter.setFavorites(result);
		}
	}

	private void setFavorites(@Nullable List<Favorite> favorites) {
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
		HashSet<String> newFavUUIDs = new HashSet<>();
		HashMap<String, Integer> newFavUUIDsFolderIds = new HashMap<>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				final String uid = favorite.getFkId();
				if (!newFav) {
					if ((this.favUUIDs != null && !this.favUUIDs.contains(uid)) //
							|| //
							(this.favUUIDsFolderIds != null //
									&& this.favUUIDsFolderIds.containsKey(uid) //
									&& !SupportFactory.get().equals(this.favUUIDsFolderIds.get(uid), favorite.getFolderId())) //
					) {
						newFav = true;
						updatedFav = true;
					}
				}
				newFavUUIDs.add(uid);
				newFavUUIDsFolderIds.put(uid, favorite.getFolderId());
			}
		}
		if (!newFav) {
			if (this.favUUIDsFolderIds == null) {
				newFav = true; // favorite never set before
				// noinspection ConstantConditions
				updatedFav = false; // never set before so not updated
			} else {
				HashSet<Integer> oldFolderIds = new HashSet<>(this.favUUIDsFolderIds.values());
				HashSet<Integer> newFolderIds = new HashSet<>(newFavUUIDsFolderIds.values());
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

	private static class FavoriteFolderHeaderViewHolder {
		TextView nameTv;
		@Nullable
		View deleteBtn;
		@Nullable
		View renameBtn;
	}

	private static class TypeHeaderViewHolder {
		TextView nameTv;
		@Nullable
		View allBtn;
		@Nullable
		View nearbyBtn;
		@Nullable
		View moreBtn;
		View layout;
	}

	public interface TypeHeaderButtonsClickListener {

		int BUTTON_MORE = 0;
		int BUTTON_NEARBY = 1;
		int BUTTON_ALL = 2;

		boolean onTypeHeaderButtonClick(int buttonId, @NonNull DataSourceType type);
	}
}
