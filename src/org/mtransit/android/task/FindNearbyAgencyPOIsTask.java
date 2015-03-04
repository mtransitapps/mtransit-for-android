package org.mtransit.android.task;

import java.util.ArrayList;

import org.mtransit.android.commons.LocationUtils;
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
	private boolean hideDecentOnly;
	private int maxSize;
	private float minCoverageInMeters;

	public FindNearbyAgencyPOIsTask(Context context, String authority, double lat, double lng, double aroundDiff, boolean hideDecentOnly,
			float minCoverageInMeters, int maxSize) {
		this.context = context;
		this.authority = authority;
		this.lat = lat;
		this.lng = lng;
		this.aroundDiff = aroundDiff;
		this.hideDecentOnly = hideDecentOnly;
		this.minCoverageInMeters = minCoverageInMeters;
		this.maxSize = maxSize;
	}

	@Override
	public ArrayList<POIManager> callMT() throws Exception {
		POIProviderContract.Filter poiFilter = POIProviderContract.Filter.getNewAroundFilter(this.lat, this.lng, this.aroundDiff);
		if (this.hideDecentOnly) {
			poiFilter.addExtra("decentOnly", true);
		}
		ArrayList<POIManager> pois = DataSourceManager.findPOIs(this.context, this.authority, poiFilter);
		LocationUtils.updateDistance(pois, this.lat, this.lng);
		float maxDistance = LocationUtils.getAroundCoveredDistanceInMeters(this.lat, this.lng, this.aroundDiff);
		LocationUtils.removeTooFar(pois, maxDistance);
		LocationUtils.removeTooMuchWhenNotInCoverage(pois, this.minCoverageInMeters, this.maxSize);
		return pois;
	}
}
