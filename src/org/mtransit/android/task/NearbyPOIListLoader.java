package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Collection;
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

	private DataSourceType type;

	private double lat;

	private double lng;

	private double aroundDiff;

	private int maxSize;

	private int minCoverage;

	public NearbyPOIListLoader(Context context, DataSourceType type, double lat, double lng, double aroundDiff, int minCoverage, int maxSize) {
		super(context);
		this.type = type;
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
		Collection<AgencyProperties> typeAgencies = DataSourceProvider.get().getTypeDataSources(getContext(), this.type);
		List<String> typeAgenciesAuthorities = new ArrayList<String>();
		if (typeAgencies != null) {
			for (AgencyProperties agency : typeAgencies) {
				if (agency.isInArea(this.lat, this.lng, this.aroundDiff)) {
					typeAgenciesAuthorities.add(agency.getAuthority());
				}
			}
		}
		if (typeAgenciesAuthorities.size() == 0) {
			return this.pois;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(typeAgenciesAuthorities.size()));
		List<Future<List<POIManager>>> taskList = new ArrayList<Future<List<POIManager>>>();
		for (String agencyAuthority : typeAgenciesAuthorities) {
			final FindNearbyAgencyPOIsTask task = new FindNearbyAgencyPOIsTask(getContext(), DataSourceProvider.get().getUri(agencyAuthority), this.lat,
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

	@Override
	public void deliverResult(List<POIManager> data) {
		if (isReset()) {
			onReleaseResources(data);
		}
		List<POIManager> oldPOIs = this.pois;
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		} else {
			MTLog.d(this, "deliverResult() > loader NOT started, not delivering result");
		}
		onReleaseResources(oldPOIs);
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
		onReleaseResources(data);
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		onReleaseResources(this.pois);
	}

	private void onReleaseResources(List<?> data) {
		if (data != null) {
			// DO NOT CLEAR LIST, ONLY REMOVE REFERENCE
			data = null;
		}
	}

}
