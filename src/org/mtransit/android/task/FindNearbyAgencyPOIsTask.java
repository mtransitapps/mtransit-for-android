package org.mtransit.android.task;

import java.util.List;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;

import android.content.Context;
import android.net.Uri;

public class FindNearbyAgencyPOIsTask extends MTCallable<List<POIManager>> {

	private static final String TAG = FindNearbyAgencyPOIsTask.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private Context context;
	private Uri contentUri;
	private double lat;
	private double lng;
	private double aroundDiff;
	private boolean hideDecentOnly;
	private int maxSize;
	private int minCoverage;

	public FindNearbyAgencyPOIsTask(Context context, Uri contentUri, double lat, double lng, double aroundDiff, boolean hideDecentOnly,
			int minCoverage, int maxSize) {
		this.context = context;
		this.contentUri = contentUri;
		this.lat = lat;
		this.lng = lng;
		this.aroundDiff = aroundDiff;
		this.hideDecentOnly = hideDecentOnly;
		this.minCoverage = minCoverage;
		this.maxSize = maxSize;
	}

	@Override
	public List<POIManager> callMT() throws Exception {
		POIFilter poiFilter = new POIFilter(this.lat, this.lng, this.aroundDiff);
		if (this.hideDecentOnly) {
			poiFilter.addExtra("decentOnly", true);
		}
		List<POIManager> pois = DataSourceManager.findPOIs(this.context, this.contentUri, poiFilter);
		LocationUtils.updateDistance(pois, lat, lng);
		float maxDistance = LocationUtils.getAroundCoveredDistance(lat, lng, aroundDiff);
		LocationUtils.removeTooFar(pois, maxDistance);
		CollectionUtils.sort(pois, POIManager.POI_DISTANCE_COMPARATOR);
		LocationUtils.removeTooMuchWhenNotInCoverage(pois, this.minCoverage, this.maxSize);
		return pois;
	}

}
