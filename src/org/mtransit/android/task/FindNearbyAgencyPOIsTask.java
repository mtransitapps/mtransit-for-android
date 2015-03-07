package org.mtransit.android.task;

import java.util.ArrayList;

import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;

import android.content.Context;

public class FindNearbyAgencyPOIsTask extends MTCallable<ArrayList<POIManager>> {

	private static final String TAG = FindNearbyAgencyPOIsTask.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private Context context;
	private String authority;
	private double lat;
	private double lng;
	private double aroundDiff;
	private boolean hideDescentOnly;
	private boolean avoidLoading;
	private int maxSize;
	private float minCoverageInMeters;

	public FindNearbyAgencyPOIsTask(Context context, String authority, double lat, double lng, double aroundDiff, boolean hideDescentOnly,
			boolean avoidLoading, float minCoverageInMeters, int maxSize) {
		this.context = context;
		this.authority = authority;
		this.lat = lat;
		this.lng = lng;
		this.aroundDiff = aroundDiff;
		this.hideDescentOnly = hideDescentOnly;
		this.avoidLoading = avoidLoading;
		this.minCoverageInMeters = minCoverageInMeters;
		this.maxSize = maxSize;
	}

	@Override
	public ArrayList<POIManager> callMT() throws Exception {
		POIProviderContract.Filter poiFilter = POIProviderContract.Filter.getNewAroundFilter(this.lat, this.lng, this.aroundDiff);
		if (this.hideDescentOnly) {
			poiFilter.addExtra(GTFSProviderContract.POI_FILTER_EXTRA_DESCENT_ONLY, true);
		}
		if (this.avoidLoading) {
			poiFilter.addExtra(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, true);
		}
		ArrayList<POIManager> pois = DataSourceManager.findPOIs(this.context, this.authority, poiFilter);
		LocationUtils.updateDistance(pois, this.lat, this.lng);
		float maxDistance = LocationUtils.getAroundCoveredDistanceInMeters(this.lat, this.lng, this.aroundDiff);
		LocationUtils.removeTooFar(pois, maxDistance);
		LocationUtils.removeTooMuchWhenNotInCoverage(pois, this.minCoverageInMeters, this.maxSize);
		return pois;
	}
}
