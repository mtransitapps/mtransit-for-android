package org.mtransit.android.task;

import android.app.Activity;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.ui.fragment.HomeFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HomePOILoader extends MTAsyncTaskLoaderX<ArrayList<POIManager>> {

	private static final String TAG = HomePOILoader.class.getSimpleName();

	@NonNull
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
	@NonNull
	private final List<AgencyProperties> allAgencyProperties;

	@Nullable
	private ArrayList<POIManager> pois;

	@NonNull
	private final WeakReference<HomeFragment> homeFragmentWR;

	public HomePOILoader(@NonNull HomeFragment homeFragment,
						 @NonNull List<AgencyProperties> allAgencyProperties,
						 @NonNull Location nearbyLocation) {
		super(homeFragment.requireContext());
		this.lat = nearbyLocation.getLatitude();
		this.lng = nearbyLocation.getLongitude();
		this.accuracyInMeters = nearbyLocation.getAccuracy();
		this.allAgencyProperties = allAgencyProperties;
		this.homeFragmentWR = new WeakReference<>(homeFragment);
	}

	@Nullable
	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			MTLog.d(this, "loadInBackgroundMT() > SKIP (POIs already loaded)");
			return this.pois;
		}
		this.pois = new ArrayList<>();
		HashSet<String> favoriteUUIDs = FavoriteManager.findFavoriteUUIDs(getContext());
		List<DataSourceType> availableAgencyTypes = getAllDataSourceTypes();
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
			if (isAbandoned()) {
				MTLog.d(this, "loadInBackgroundMT() > SKIP (load abandoned)");
				break;
			}
			if (isLoadInBackgroundCanceled()) {
				MTLog.d(this, "loadInBackgroundMT() > SKIP (load cancelled)");
				break;
			}
			ArrayList<POIManager> typePOIs = findAllNearby(type, this.lat, this.lng, minDistanceInMeters, this.nbMaxByType);
			filterTypePOIs(favoriteUUIDs, typePOIs, minDistanceInMeters);
			CollectionUtils.sort(typePOIs, POIManager.POI_ALPHA_COMPARATOR);
			deliverPartialResult(typePOIs);
			this.pois.addAll(typePOIs);
		}
		return this.pois;
	}

	private void deliverPartialResult(final ArrayList<POIManager> typePOIs) {
		final HomeFragment homeFragment = this.homeFragmentWR.get();
		final Activity activity = homeFragment == null ? null : homeFragment.getActivity();
		if (activity == null) {
			MTLog.d(this, "deliverPartialResult() > SKIP (no activity)");
			return;
		}
		activity.runOnUiThread(() -> {
			final HomeFragment homeFragment1 = HomePOILoader.this.homeFragmentWR.get();
			if (homeFragment1 == null) {
				MTLog.d(this, "deliverPartialResult() > SKIP (no fragment)");
				return;
			}
			if (isAbandoned()) {
				MTLog.d(this, "deliverPartialResult() > SKIP (load abandoned)");
				return;
			}
			if (isLoadInBackgroundCanceled()) {
				MTLog.d(this, "deliverPartialResult() > SKIP (load cancelled)");
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

	@NonNull
	private ArrayList<POIManager> findAllNearby(@NonNull DataSourceType type,
												double typeLat,
												double typeLng,
												float typeMinCoverageInMeters,
												int nbMaxByType) {
		ArrayList<POIManager> typePOIs;
		LocationUtils.AroundDiff typeAd = LocationUtils.getNewDefaultAroundDiff();
		Double lastTypeAroundDiff = null;
		int typeMaxSize = LocationUtils.MAX_NEARBY_LIST;
		while (true) {
			Collection<AgencyProperties> typeAgencies = getTypeDataSources(type);
			typePOIs = findNearby(getContext(), typeLat, typeLng, typeAd, lastTypeAroundDiff, typeMaxSize, typeMinCoverageInMeters, typeAgencies);
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

	@NonNull
	private List<DataSourceType> getAllDataSourceTypes() {
		List<DataSourceType> availableAgencyTypes = new ArrayList<>();
		for (AgencyProperties agencyProperties : this.allAgencyProperties) {
			final DataSourceType dst = agencyProperties.getType();
			if (availableAgencyTypes.contains(dst)) {
				continue;
			}
			availableAgencyTypes.add(dst);
		}
		CollectionUtils.sort(availableAgencyTypes, new DataSourceType.DataSourceTypeShortNameComparator(getContext())); // sort
		return availableAgencyTypes;
	}

	@NonNull
	private Collection<AgencyProperties> getTypeDataSources(DataSourceType type) {
		List<AgencyProperties> typeDataSources = new ArrayList<>();
		for (AgencyProperties agencyProperties : this.allAgencyProperties) {
			if (agencyProperties.getType() != type) {
				continue;
			}
			typeDataSources.add(agencyProperties);
		}
		return typeDataSources;
	}

	@NonNull
	private static ArrayList<POIManager> findNearby(@NonNull Context context,
													double typeLat,
													double typeLng,
													@NonNull LocationUtils.AroundDiff typeAd,
													@Nullable Double lastTypeAroundDiff,
													int typeMaxSize,
													float typeMinCoverageInMeters,
													@Nullable Collection<AgencyProperties> typeAgencies) {
		ArrayList<POIManager> typePOIs = new ArrayList<>();
		typeAgencies = NearbyPOIListLoader.filterAgenciesInArea(typeAgencies, typeLat, typeLng, typeAd, lastTypeAroundDiff);
		if (typeAgencies.size() == 0) {
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
	public void deliverResult(@Nullable ArrayList<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}
}
