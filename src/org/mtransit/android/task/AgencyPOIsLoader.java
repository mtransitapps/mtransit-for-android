package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.List;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;

import android.content.Context;
import android.net.Uri;

public class AgencyPOIsLoader extends MTAsyncTaskLoaderV4<List<POIManager>> {

	private static final String TAG = FavoritesLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private AgencyProperties agency;
	private List<POIManager> pois;

	public AgencyPOIsLoader(Context context, AgencyProperties agency) {
		super(context);
		this.agency = agency;
	}

	@Override
	public List<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<POIManager>();
		final Uri contentUri = UriUtils.newContentUri(this.agency.getAuthority());
		this.pois = DataSourceProvider.findAllAgencyPOIs(getContext(), contentUri);
		CollectionUtils.sort(this.pois, POIManager.POI_ALPHA_COMPATOR);
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
