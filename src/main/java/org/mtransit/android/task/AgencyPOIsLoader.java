package org.mtransit.android.task;

import java.util.ArrayList;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;

import android.content.Context;

public class AgencyPOIsLoader extends MTAsyncTaskLoaderV4<ArrayList<POIManager>> {

	private static final String TAG = AgencyPOIsLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private String authority;
	private ArrayList<POIManager> pois;

	public AgencyPOIsLoader(Context context, String authority) {
		super(context);
		this.authority = authority;
	}

	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<>();
		this.pois = DataSourceManager.findPOIs(getContext(), this.authority, POIProviderContract.Filter.getNewEmptyFilter());
		CollectionUtils.sort(this.pois, POIManager.POI_ALPHA_COMPARATOR);
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
