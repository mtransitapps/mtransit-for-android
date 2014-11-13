package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
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

public class POISearchLoader extends MTAsyncTaskLoaderV4<List<POIManager>> {

	private static final String TAG = FavoritesLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private List<POIManager> pois;

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
	public List<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		clearAllSearchTypesTasks();
		this.pois = new ArrayList<POIManager>();
		if (TextUtils.isEmpty(this.query)) {
			return this.pois;
		}
		Set<String> favoriteUUIDs = FavoriteManager.findFavoriteUUIDs(getContext());
		POISearchComparator poiSearchComparator = new POISearchComparator(favoriteUUIDs);
		final boolean keepAll;
		final List<DataSourceType> agencyTypes;
		if (this.typeFilter == null || this.typeFilter.getDataSourceTypeId() == TypeFilter.ALL.getDataSourceTypeId()) {
			agencyTypes = DataSourceProvider.get(getContext()).getAvailableAgencyTypes();
			keepAll = false;
		} else {
			agencyTypes = new ArrayList<DataSourceType>();
			agencyTypes.add(DataSourceType.parseId(this.typeFilter.getDataSourceTypeId()));
			keepAll = true;
		}
		List<Future<List<POIManager>>> taskList = new ArrayList<Future<List<POIManager>>>();
		if (agencyTypes != null) {
			for (DataSourceType agencyType : agencyTypes) {
				if (!agencyType.isSearchable()) {
					continue;
				}
				final FindSearchTypeTask task = new FindSearchTypeTask(getContext(), agencyType, this.query, keepAll, this.userLocation, poiSearchComparator);
				taskList.add(getFetchSearchTypeExecutor().submit(task));
			}
		}
		for (Future<List<POIManager>> future : taskList) {
			try {
				List<POIManager> typePOIs = future.get();
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
	public void deliverResult(List<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

	private static class FindSearchTypeTask extends MTCallable<List<POIManager>> {

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
		public List<POIManager> callMT() throws Exception {
			if (TextUtils.isEmpty(this.query)) {
				return null;
			}
			clearFetchAgencySearchTasks();
			final List<AgencyProperties> agencies = DataSourceProvider.get(this.context).getTypeDataSources(this.agencyType.getId());
			List<Future<List<POIManager>>> taskList = new ArrayList<Future<List<POIManager>>>();
			if (agencies != null) {
				for (AgencyProperties agency : agencies) {
					final FindSearchTask task = new FindSearchTask(this.context, agency, this.query);
					taskList.add(getFetchAgencySearchExecutor().submit(task));
				}
			}
			List<POIManager> typePois = new ArrayList<POIManager>();
			for (Future<List<POIManager>> future : taskList) {
				try {
					List<POIManager> agencyPOIs = future.get();
					typePois.addAll(agencyPOIs);
				} catch (Exception e) {
					MTLog.w(this, e, "Error while loading in background!");
				}
			}
			clearFetchAgencySearchTasks();
			LocationUtils.updateDistance(typePois, this.userLocation);
			CollectionUtils.sort(typePois, this.poiSearchComparator);
			if (!keepAll && typePois.size() > 2) {
				typePois = typePois.subList(0, 2);
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

		private Set<String> favoriteUUIDs;

		public POISearchComparator(Set<String> favoriteUUIDs) {
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
			final int lScore = lhs.poi.getScore() == null ? 0 : lhs.poi.getScore().intValue();
			final int rScore = lhs.poi.getScore() == null ? 0 : rhs.poi.getScore().intValue();
			if (lScore > rScore) {
				return ComparatorUtils.BEFORE;
			} else if (lScore < rScore) {
				return ComparatorUtils.AFTER;
			}
			final boolean lfav = this.favoriteUUIDs.contains(lhs.poi.getUUID());
			final boolean rfav = this.favoriteUUIDs.contains(rhs.poi.getUUID());
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

	private static class FindSearchTask extends MTCallable<List<POIManager>> {

		private static final String TAG = POISearchLoader.class.getSimpleName() + ">" + FindSearchTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private Context context;
		private AgencyProperties agency;
		private String query;

		public FindSearchTask(Context context, AgencyProperties agency, String query) {
			this.context = context;
			this.agency = agency;
			this.query = query;
		}

		@Override
		public List<POIManager> callMT() throws Exception {
			if (TextUtils.isEmpty(this.query)) {
				return null;
			}
			POIFilter poiFilter = new POIFilter(new String[] { this.query });
			poiFilter.addExtra("decentOnly", true);
			return DataSourceManager.findPOIs(this.context, UriUtils.newContentUri(this.agency.getAuthority()), poiFilter);
		}

	}
}
