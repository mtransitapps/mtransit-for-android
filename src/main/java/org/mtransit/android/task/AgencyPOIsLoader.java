package org.mtransit.android.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;

import java.util.ArrayList;

public class AgencyPOIsLoader extends MTAsyncTaskLoaderX<ArrayList<POIManager>> {

	private static final String TAG = AgencyPOIsLoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@NonNull
	private final String authority;

	@Nullable
	private ArrayList<POIManager> pois;

	public AgencyPOIsLoader(@NonNull Context context, @NonNull String authority) {
		super(context);
		this.authority = authority;
	}

	@Nullable
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
	public void deliverResult(@Nullable ArrayList<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}
}
