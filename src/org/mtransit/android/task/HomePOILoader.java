package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager;

import android.content.Context;

public class HomePOILoader extends MTAsyncTaskLoaderV4<ArrayList<POIManager>> {

	private static final String TAG = HomePOILoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final int NB_MAX_BY_TYPE = 2;

	private double lat;
	private double lng;

	private ArrayList<POIManager> pois;

	public HomePOILoader(Context context, double lat, double lng) {
		super(context);
		this.lat = lat;
		this.lng = lng;
	}

	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<POIManager>();
		HashSet<String> favoriteUUIDs = FavoriteManager.findFavoriteUUIDs(getContext());
		ArrayList<DataSourceType> availableAgencyTypes = DataSourceProvider.get(getContext()).getAvailableAgencyTypes();
		if (availableAgencyTypes != null) {
			for (DataSourceType type : availableAgencyTypes) {
				ArrayList<POIManager> typePOIs = findNearby(getContext(), type, this.lat, this.lng);
				filterTypePOIs(favoriteUUIDs, typePOIs);
				CollectionUtils.sort(typePOIs, POIManager.POI_ALPHA_COMPARATOR);
				this.pois.addAll(typePOIs);
			}
		}
		return this.pois;
	}

	private void filterTypePOIs(HashSet<String> favoriteUUIDs, ArrayList<POIManager> typePOIs) {
		Iterator<POIManager> it = typePOIs.iterator();
		int nbKept = 0;
		float lastKeptDistance = -1;
		HashSet<String> routeTripKept = new HashSet<String>();
		while (it.hasNext()) {
			POIManager poim = it.next();
			if (!favoriteUUIDs.contains(poim.poi.getUUID())) {
				if (poim.poi instanceof RouteTripStop) {
					RouteTripStop rts = (RouteTripStop) poim.poi;
					String routeTripId = rts.route.id + "-" + rts.trip.id;
					if (routeTripKept.contains(routeTripId) && lastKeptDistance != poim.getDistance()) {
						it.remove();
						continue;
					}
				} else if (nbKept >= NB_MAX_BY_TYPE && lastKeptDistance != poim.getDistance()) {
					it.remove();
					continue;
				}
			}
			if (nbKept >= NB_MAX_BY_TYPE && lastKeptDistance != poim.getDistance() && poim.getDistance() > LocationUtils.MIN_NEARBY_LIST_COVERAGE) {
				it.remove();
				continue;
			}
			if (poim.poi instanceof RouteTripStop) {
				RouteTripStop rts = (RouteTripStop) poim.poi;
				String routeTripId = rts.route.id + "-" + rts.trip.id;
				routeTripKept.add(routeTripId);
			}
			lastKeptDistance = poim.getDistance();
			nbKept++;
		}
	}

	private ArrayList<POIManager> findNearby(Context context, DataSourceType type, double typeLat, double typeLng) {
		ArrayList<POIManager> typePOIs = new ArrayList<POIManager>();
		LocationUtils.AroundDiff typeAd = LocationUtils.getNewDefaultAroundDiff();
		int typeMaxSize = LocationUtils.MIN_NEARBY_LIST_COVERAGE;
		int typeMinCoverage = LocationUtils.MAX_NEARBY_LIST;
		while (CollectionUtils.getSize(typePOIs) < NB_MAX_BY_TYPE && typeAd.aroundDiff < LocationUtils.MAX_AROUND_DIFF) {
			Collection<AgencyProperties> typeAgencies = DataSourceProvider.get(context).getTypeDataSources(getContext(), type.getId());
			typePOIs = findNearby(context, typeLat, typeLng, typeAd, typeMaxSize, typeMinCoverage, typeAgencies);
			LocationUtils.incAroundDiff(typeAd);
		}
		return typePOIs;
	}

	private ArrayList<POIManager> findNearby(Context context, double typeLat, double typeLng, LocationUtils.AroundDiff typeAd, int typeMaxSize,
			int typeMinCoverage, Collection<AgencyProperties> typeAgencies) {
		ArrayList<POIManager> typePOIs = new ArrayList<POIManager>();
		NearbyPOIListLoader.filterAgencies(typeAgencies, typeLat, typeLng, typeAd);
		if (CollectionUtils.getSize(typeAgencies) == 0) {
			return typePOIs;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(typeAgencies.size()));
		ArrayList<Future<ArrayList<POIManager>>> taskList = new ArrayList<Future<ArrayList<POIManager>>>();
		for (AgencyProperties agency : typeAgencies) {
			FindNearbyAgencyPOIsTask task = new FindNearbyAgencyPOIsTask(context, agency.getAuthority(), typeLat, typeLng, typeAd.aroundDiff, true,
					typeMinCoverage, typeMaxSize);
			taskList.add(executor.submit(task));
		}
		for (Future<ArrayList<POIManager>> future : taskList) {
			try {
				ArrayList<POIManager> agencyNearbyStops = future.get();
				typePOIs.addAll(agencyNearbyStops);
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while loading in background!");
			}
		}
		executor.shutdown();
		CollectionUtils.sort(typePOIs, POIManager.POI_DISTANCE_COMPARATOR);
		LocationUtils.removeTooMuchWhenNotInCoverage(typePOIs, typeMinCoverage, typeMaxSize);
		return typePOIs;
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

}
