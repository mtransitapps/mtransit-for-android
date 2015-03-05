package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.provider.GTFSRouteTripStopProviderContract;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.ui.fragment.SearchFragment.TypeFilter;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;

public class POISearchLoader extends MTAsyncTaskLoaderV4<ArrayList<POIManager>> {

	private static final String TAG = POISearchLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ArrayList<POIManager> pois;

	private String query;

	private TypeFilter typeFilter;

	private Location userLocation;

	public POISearchLoader(Context context, String query, TypeFilter typeFilter, Location userLocation) {
		super(context);
		this.query = query;
		this.typeFilter = typeFilter;
		this.userLocation = userLocation;
	}

	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		clearAllSearchTypesTasks();
		this.pois = new ArrayList<POIManager>();
		if (TextUtils.isEmpty(this.query)) {
			return this.pois;
		}
		HashSet<String> favoriteUUIDs = FavoriteManager.findFavoriteUUIDs(getContext());
		POISearchComparator poiSearchComparator = new POISearchComparator(favoriteUUIDs);
		boolean keepAll;
		ArrayList<DataSourceType> agencyTypes;
		if (this.typeFilter == null || this.typeFilter.getDataSourceTypeId() == TypeFilter.ALL.getDataSourceTypeId()) {
			agencyTypes = DataSourceProvider.get(getContext()).getAvailableAgencyTypes();
			keepAll = false;
		} else {
			agencyTypes = new ArrayList<DataSourceType>();
			agencyTypes.add(DataSourceType.parseId(this.typeFilter.getDataSourceTypeId()));
			keepAll = true;
		}
		ArrayList<Future<ArrayList<POIManager>>> taskList = new ArrayList<Future<ArrayList<POIManager>>>();
		if (agencyTypes != null) {
			for (DataSourceType agencyType : agencyTypes) {
				if (!agencyType.isSearchable()) {
					continue;
				}
				FindSearchTypeTask task = new FindSearchTypeTask(getContext(), agencyType, this.query, keepAll, this.userLocation, poiSearchComparator);
				taskList.add(getFetchSearchTypeExecutor().submit(task));
			}
		}
		for (Future<ArrayList<POIManager>> future : taskList) {
			try {
				ArrayList<POIManager> typePOIs = future.get();
				if (typePOIs != null) {
					this.pois.addAll(typePOIs);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while loading in background!");
			}
		}
		clearAllSearchTypesTasks();
		return this.pois;
	}

	private static final int CORE_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

	private static final int MAX_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

	private ThreadPoolExecutor fetchSearchTypeExecutor;

	public ThreadPoolExecutor getFetchSearchTypeExecutor() {
		if (this.fetchSearchTypeExecutor == null) {
			this.fetchSearchTypeExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingDeque<Runnable>());
		}
		return fetchSearchTypeExecutor;
	}

	private void clearAllSearchTypesTasks() {
		if (this.fetchSearchTypeExecutor != null) {
			this.fetchSearchTypeExecutor.shutdown();
			this.fetchSearchTypeExecutor = null;
		}
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		if (this.pois != null) {
			deliverResult(this.pois);
		}
		if (this.pois == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void deliverResult(ArrayList<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

	private static class FindSearchTypeTask extends MTCallable<ArrayList<POIManager>> {

		private static final String TAG = POISearchLoader.class.getSimpleName() + ">" + FindSearchTypeTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private Context context;
		private DataSourceType agencyType;
		private String query;
		private boolean keepAll;
		private Location userLocation;
		private POISearchComparator poiSearchComparator;

		public FindSearchTypeTask(Context context, DataSourceType agencyType, String query, boolean keepAll, Location userLocation,
				POISearchComparator poiSearchComparator) {
			this.context = context;
			this.agencyType = agencyType;
			this.query = query;
			this.keepAll = keepAll;
			this.userLocation = userLocation;
			this.poiSearchComparator = poiSearchComparator;
		}

		@Override
		public ArrayList<POIManager> callMT() throws Exception {
			if (TextUtils.isEmpty(this.query)) {
				return null;
			}
			clearFetchAgencySearchTasks();
			ArrayList<AgencyProperties> agencies = DataSourceProvider.get(this.context).getTypeDataSources(this.context, this.agencyType.getId());
			ArrayList<Future<ArrayList<POIManager>>> taskList = new ArrayList<Future<ArrayList<POIManager>>>();
			if (agencies != null) {
				for (AgencyProperties agency : agencies) {
					FindSearchTask task = new FindSearchTask(this.context, agency, this.query, this.userLocation);
					taskList.add(getFetchAgencySearchExecutor().submit(task));
				}
			}
			ArrayList<POIManager> typePois = new ArrayList<POIManager>();
			for (Future<ArrayList<POIManager>> future : taskList) {
				try {
					ArrayList<POIManager> agencyPOIs = future.get();
					typePois.addAll(agencyPOIs);
				} catch (Exception e) {
					MTLog.w(this, e, "Error while loading in background!");
				}
			}
			clearFetchAgencySearchTasks();
			LocationUtils.updateDistance(typePois, this.userLocation);
			CollectionUtils.sort(typePois, this.poiSearchComparator);
			if (!this.keepAll && typePois.size() > 2) {
				typePois = new ArrayList<POIManager>(typePois.subList(0, 2));
			}
			return typePois;
		}

		private static final int CORE_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

		private static final int MAX_POOL_SIZE = RuntimeUtils.NUMBER_OF_CORES;

		private ThreadPoolExecutor fetchAgencySearchExecutor;

		public ThreadPoolExecutor getFetchAgencySearchExecutor() {
			if (this.fetchAgencySearchExecutor == null) {
				this.fetchAgencySearchExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
						new LinkedBlockingDeque<Runnable>());
			}
			return fetchAgencySearchExecutor;
		}

		public void clearFetchAgencySearchTasks() {
			if (this.fetchAgencySearchExecutor != null) {
				this.fetchAgencySearchExecutor.shutdown();
				this.fetchAgencySearchExecutor = null;
			}
		}
	}

	private static class POISearchComparator implements Comparator<POIManager> {

		private HashSet<String> favoriteUUIDs;

		public POISearchComparator(HashSet<String> favoriteUUIDs) {
			this.favoriteUUIDs = favoriteUUIDs;
		}

		@Override
		public int compare(POIManager lhs, POIManager rhs) {
			if (lhs == null && rhs == null) {
				return ComparatorUtils.SAME;
			} else if (lhs == null) {
				return ComparatorUtils.AFTER;
			} else if (rhs == null) {
				return ComparatorUtils.BEFORE;
			}
			int lScore = lhs.poi.getScore() == null ? 0 : lhs.poi.getScore();
			int rScore = lhs.poi.getScore() == null ? 0 : rhs.poi.getScore();
			if (lScore > rScore) {
				return ComparatorUtils.BEFORE;
			} else if (lScore < rScore) {
				return ComparatorUtils.AFTER;
			}
			boolean lfav = this.favoriteUUIDs.contains(lhs.poi.getUUID());
			boolean rfav = this.favoriteUUIDs.contains(rhs.poi.getUUID());
			if (lfav && !rfav) {
				return ComparatorUtils.BEFORE;
			} else if (!lfav && rfav) {
				return ComparatorUtils.AFTER;
			}
			float ld = lhs.getDistance();
			float rd = rhs.getDistance();
			if (ld > rd) {
				return ComparatorUtils.AFTER;
			} else if (ld < rd) {
				return ComparatorUtils.BEFORE;
			}
			return ComparatorUtils.SAME;
		}

	}

	private static class FindSearchTask extends MTCallable<ArrayList<POIManager>> {

		private static final String TAG = POISearchLoader.class.getSimpleName() + ">" + FindSearchTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private Context context;
		private AgencyProperties agency;
		private String query;
		private Location userLocation;

		public FindSearchTask(Context context, AgencyProperties agency, String query, Location userLocation) {
			this.context = context;
			this.agency = agency;
			this.query = query;
			this.userLocation = userLocation;
		}

		@Override
		public ArrayList<POIManager> callMT() throws Exception {
			if (TextUtils.isEmpty(this.query)) {
				return null;
			}
			POIProviderContract.Filter poiFilter = POIProviderContract.Filter.getNewSearchFilter(this.query);
			poiFilter.addExtra(GTFSRouteTripStopProviderContract.POI_FILTER_EXTRA_DESCENT_ONLY, true);
			if (this.userLocation != null) {
				poiFilter.addExtra("lat", this.userLocation.getLatitude());
				poiFilter.addExtra("lng", this.userLocation.getLongitude());
			}
			return DataSourceManager.findPOIs(this.context, this.agency.getAuthority(), poiFilter);
		}
	}
}
