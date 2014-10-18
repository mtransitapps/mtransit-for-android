package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.List;

import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;

import android.content.Context;
import android.net.Uri;

public class RTSTripStopsLoader extends MTAsyncTaskLoaderV4<List<POIManager>> {

	private static final String TAG = RTSAgencyRoutesLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private Trip trip;
	private List<POIManager> pois;
	private String authority;

	public RTSTripStopsLoader(Context context, Trip trip, String authority) {
		super(context);
		this.trip = trip;
		this.authority = authority;
	}

	@Override
	public List<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<POIManager>();
		final Uri contentUri = UriUtils.newContentUri(this.authority);
		this.pois = DataSourceProvider.findRTSTripPOIs(getContext(), contentUri, this.trip.id);
		return pois;
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

}
