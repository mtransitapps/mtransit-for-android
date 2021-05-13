package org.mtransit.android.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;

import java.util.ArrayList;

// @Deprecated for nearby
// TODO: home
public class FindNearbyAgencyPOIsTask extends MTCallable<ArrayList<POIManager>> {

	private static final String TAG = FindNearbyAgencyPOIsTask.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@NonNull
	private final Context context;
	@NonNull
	private final String authority;
	private final double lat;
	private final double lng;
	private final double aroundDiff;
	private final boolean hideDescentOnly;
	private final boolean avoidLoading;
	private final int maxSize;
	private final float minCoverageInMeters;

	FindNearbyAgencyPOIsTask(@NonNull Context context,
							 @NonNull String authority,
							 double lat,
							 double lng,
							 double aroundDiff,
							 boolean hideDescentOnly,
							 boolean avoidLoading,
							 float minCoverageInMeters,
							 int maxSize) {
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

	@Nullable
	@Override
	public ArrayList<POIManager> callMT() {
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
