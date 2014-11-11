package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIManager;

import android.content.Context;

public class NearbyPOIListLoader extends MTAsyncTaskLoaderV4<List<POIManager>> {

	private static final String TAG = NearbyPOIListLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private List<POIManager> pois;

	private List<AgencyProperties> typeAgencies;


	private double lat;

	private double lng;

	private double aroundDiff;

	private int maxSize;

	private int minCoverage;

	public NearbyPOIListLoader(Context context, List<AgencyProperties> typeAgencies, double lat, double lng, double aroundDiff, int minCoverage, int maxSize) {
		super(context);
		this.typeAgencies = typeAgencies;
		this.lat = lat;
		this.lng = lng;
		this.aroundDiff = aroundDiff;
		this.minCoverage = minCoverage;
		this.maxSize = maxSize;
	}

	@Override
	public List<POIManager> loadInBackgroundMT() {
		if (pois == null) {
			pois = new ArrayList<POIManager>();
		}
		if (typeAgencies.size() == 0) {
			return this.pois;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(typeAgencies.size()));
		List<Future<List<POIManager>>> taskList = new ArrayList<Future<List<POIManager>>>();
		for (AgencyProperties agency : typeAgencies) {
			final FindNearbyAgencyPOIsTask task = new FindNearbyAgencyPOIsTask(getContext(), DataSourceProvider.get().getUri(agency.getAuthority()), this.lat,
					this.lng, this.aroundDiff, true, this.minCoverage, this.maxSize);
			taskList.add(executor.submit(task));
		}
		for (Future<List<POIManager>> future : taskList) {
			try {
				List<POIManager> agencyNearbyStops = future.get();
				this.pois.addAll(agencyNearbyStops);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while loading in background!");
			}
		}
		executor.shutdown();
		CollectionUtils.sort(this.pois, POIManager.POI_DISTANCE_COMPARATOR);
		LocationUtils.removeTooMuchWhenNotInCoverage(this.pois, this.minCoverage, this.maxSize);
		return this.pois;
	}

	public static List<AgencyProperties> findTypeAgencies(Context context, DataSourceType type, double lat, double lng, double aroundDiff) {
		List<AgencyProperties> allTypeAgencies = DataSourceProvider.get().getTypeDataSources(context, type.getId());
		List<AgencyProperties> nearbyTypeAgenciesAuthorities = new ArrayList<AgencyProperties>();
		if (allTypeAgencies != null) {
			for (AgencyProperties agency : allTypeAgencies) {
				if (agency.isInArea(lat, lng, aroundDiff)) {
					nearbyTypeAgenciesAuthorities.add(agency);
				}
			}
		}
		return nearbyTypeAgenciesAuthorities;
	}

	@Override
	public void deliverResult(List<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		} else {
			MTLog.d(this, "deliverResult() > loader NOT started, not delivering result");
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
	public void onCanceled(List<POIManager> data) {
		super.onCanceled(data);
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
	}

}
