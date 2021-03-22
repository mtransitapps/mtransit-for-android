package org.mtransit.android.task;

import java.lang.ref.WeakReference;
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
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.ui.fragment.HomeFragment;

import android.app.Activity;
import android.content.Context;

public class HomePOILoader extends MTAsyncTaskLoaderX<ArrayList<POIManager>> {

	private static final String TAG = HomePOILoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final int NB_MAX_BY_TYPE = 2;
	private static final int NB_MAX_BY_TYPE_ONE_TYPE = 6;
	private static final int NB_MAX_BY_TYPE_TWO_TYPES = 4;
	private int nbMaxByType = NB_MAX_BY_TYPE;

	private final double lat;
	private final double lng;
	private final float accuracyInMeters;

	private ArrayList<POIManager> pois;

	private final WeakReference<HomeFragment> homeFragmentWR;

	public HomePOILoader(HomeFragment homeFragment, Context context, double lat, double lng, float accuracyInMeters) {
		super(context);
		this.lat = lat;
		this.lng = lng;
		this.accuracyInMeters = accuracyInMeters;
		this.homeFragmentWR = new WeakReference<>(homeFragment);
	}

	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<>();
		HashSet<String> favoriteUUIDs = FavoriteManager.findFavoriteUUIDs(getContext());
		ArrayList<DataSourceType> availableAgencyTypes = DataSourceProvider.get(getContext()).getAvailableAgencyTypes();
		if (availableAgencyTypes.size() <= 2) {
			this.nbMaxByType = NB_MAX_BY_TYPE_ONE_TYPE;
		} else if (availableAgencyTypes.size() <= 3) {
			this.nbMaxByType = NB_MAX_BY_TYPE_TWO_TYPES;
		}
		float minDistanceInMeters = LocationUtils.getAroundCoveredDistanceInMeters(this.lat, this.lng, LocationUtils.MIN_AROUND_DIFF);
		if (minDistanceInMeters < LocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS) {
			minDistanceInMeters = LocationUtils.MIN_NEARBY_LIST_COVERAGE_IN_METERS;
		}
		if (minDistanceInMeters < this.accuracyInMeters) {
			minDistanceInMeters = this.accuracyInMeters;
		}
		for (DataSourceType type : availableAgencyTypes) {
			if (!type.isHomeScreen()) {
				continue;
			}
			ArrayList<POIManager> typePOIs = findAllNearby(getContext(), type, this.lat, this.lng, minDistanceInMeters, this.nbMaxByType);
			filterTypePOIs(favoriteUUIDs, typePOIs, minDistanceInMeters);
			CollectionUtils.sort(typePOIs, POIManager.POI_ALPHA_COMPARATOR);
			deliverPartialResult(typePOIs);
			this.pois.addAll(typePOIs);
		}
		return this.pois;
	}

	private void deliverPartialResult(final ArrayList<POIManager> typePOIs) {
		HomeFragment homeFragment = this.homeFragmentWR == null ? null : this.homeFragmentWR.get();
		Activity activity = homeFragment == null ? null : homeFragment.getActivity();
		if (activity == null) {
			return;
		}
		activity.runOnUiThread(() -> {
			HomeFragment homeFragment1 = HomePOILoader.this.homeFragmentWR.get();
			if (homeFragment1 == null) {
				return;
			}
			homeFragment1.onLoadPartial(typePOIs);
		});
	}

	private void filterTypePOIs(HashSet<String> favoriteUUIDs, ArrayList<POIManager> typePOIs, float minDistanceInMeters) {
		Iterator<POIManager> it = typePOIs.iterator();
		int nbKept = 0;
		float lastKeptDistance = -1;
		HashSet<String> routeTripKept = new HashSet<>();
		while (it.hasNext()) {
			POIManager poim = it.next();
			if (!favoriteUUIDs.contains(poim.poi.getUUID())) {
				if (poim.poi instanceof RouteTripStop) {
					RouteTripStop rts = (RouteTripStop) poim.poi;
					String routeTripId = rts.getRoute().getId() + "-" + rts.getTrip().getId();
					if (routeTripKept.contains(routeTripId)) {
						it.remove();
						continue;
					}
				} else if (nbKept >= this.nbMaxByType && lastKeptDistance != poim.getDistance()) {
					it.remove();
					continue;
				}
			}
			if (nbKept >= this.nbMaxByType && lastKeptDistance != poim.getDistance() && poim.getDistance() > minDistanceInMeters) {
				it.remove();
				continue;
			}
			if (poim.poi instanceof RouteTripStop) {
				RouteTripStop rts = (RouteTripStop) poim.poi;
				String routeTripId = rts.getRoute().getId() + "-" + rts.getTrip().getId();
				routeTripKept.add(routeTripId);
			}
			lastKeptDistance = poim.getDistance();
			nbKept++;
		}
	}

	private static ArrayList<POIManager> findAllNearby(Context context, DataSourceType type, double typeLat, double typeLng, float typeMinCoverageInMeters,
			int nbMaxByType) {
		ArrayList<POIManager> typePOIs;
		LocationUtils.AroundDiff typeAd = LocationUtils.getNewDefaultAroundDiff();
		Double lastTypeAroundDiff = null;
		int typeMaxSize = LocationUtils.MAX_NEARBY_LIST;
		while (true) {
			Collection<AgencyProperties> typeAgencies = DataSourceProvider.get(context).getTypeDataSources(context, type.getId());
			typePOIs = findNearby(context, typeLat, typeLng, typeAd, lastTypeAroundDiff, typeMaxSize, typeMinCoverageInMeters, typeAgencies);
			if (LocationUtils.searchComplete(typeLat, typeLng, typeAd.aroundDiff)) {
				break;
			}
			if (CollectionUtils.getSize(typePOIs) > nbMaxByType
					&& LocationUtils.getAroundCoveredDistanceInMeters(typeLat, typeLng, typeAd.aroundDiff) >= typeMinCoverageInMeters) {
				break;
			}
			if (CollectionUtils.getSize(typePOIs) == 0) {
				lastTypeAroundDiff = typeAd.aroundDiff;
			} else {
				lastTypeAroundDiff = null;
			}
			LocationUtils.incAroundDiff(typeAd);
		}
		return typePOIs;
	}

	private static ArrayList<POIManager> findNearby( //
			Context context, double typeLat, double typeLng, LocationUtils.AroundDiff typeAd, Double lastTypeAroundDiff, int typeMaxSize,
			float typeMinCoverageInMeters, Collection<AgencyProperties> typeAgencies) {
		ArrayList<POIManager> typePOIs = new ArrayList<>();
		NearbyPOIListLoader.filterAgencies(typeAgencies, typeLat, typeLng, typeAd, lastTypeAroundDiff);
		if (typeAgencies == null || typeAgencies.size() == 0) {
			return typePOIs;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<>(typeAgencies.size()));
		ArrayList<Future<ArrayList<POIManager>>> taskList = new ArrayList<>();
		for (AgencyProperties agency : typeAgencies) {
			FindNearbyAgencyPOIsTask task = new FindNearbyAgencyPOIsTask( //
					context, agency.getAuthority(), typeLat, typeLng, typeAd.aroundDiff, true, true, typeMinCoverageInMeters, typeMaxSize);
			taskList.add(executor.submit(task));
		}
		for (Future<ArrayList<POIManager>> future : taskList) {
			try {
				ArrayList<POIManager> agencyNearbyStops = future.get();
				if (agencyNearbyStops != null) {
					typePOIs.addAll(agencyNearbyStops);
				}
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while loading in background!");
			}
		}
		executor.shutdown();
		LocationUtils.removeTooMuchWhenNotInCoverage(typePOIs, typeMinCoverageInMeters, typeMaxSize);
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
