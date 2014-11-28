package org.mtransit.android.task;

import java.util.ArrayList;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.provider.POIFilter;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;

import android.content.Context;

public class AgencyPOIsLoader extends MTAsyncTaskLoaderV4<ArrayList<POIManager>> {

	private static final String TAG = FavoritesLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private AgencyProperties agency;
	private ArrayList<POIManager> pois;

	public AgencyPOIsLoader(Context context, AgencyProperties agency) {
		super(context);
		this.agency = agency;
	}

	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<POIManager>();
		this.pois = DataSourceManager.findPOIs(getContext(), this.agency.getAuthority(), new POIFilter(StringUtils.EMPTY));
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
	public void deliverResult(ArrayList<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

}
