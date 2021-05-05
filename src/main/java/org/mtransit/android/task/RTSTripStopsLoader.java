package org.mtransit.android.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.provider.GTFSProviderContract;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;

import java.util.ArrayList;

public class RTSTripStopsLoader extends MTAsyncTaskLoaderX<ArrayList<POIManager>> {

	private static final String LOG_TAG = RTSTripStopsLoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final String authority;
	private final long tripId;

	@Nullable
	private ArrayList<POIManager> pois;

	public RTSTripStopsLoader(@NonNull Context context, @NonNull String authority, long tripId) {
		super(context);
		this.authority = authority;
		this.tripId = tripId;
	}

	@Nullable
	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<>();
		POIProviderContract.Filter poiFilter = POIProviderContract.Filter.getNewSqlSelectionFilter(
				SqlUtils.getWhereEquals(
						GTFSProviderContract.RouteTripStopColumns.T_TRIP_K_ID, this.tripId)
		);
		// TODO CRASH SimpleArrayMap ClassCastException: String cannot be cast to Object[]
		poiFilter.addExtra(
				POIProviderContract.POI_FILTER_EXTRA_SORT_ORDER, //
				SqlUtils.getSortOrderAscending(GTFSProviderContract.RouteTripStopColumns.T_TRIP_STOPS_K_STOP_SEQUENCE)
		);
		this.pois = DataSourceManager.findPOIs(getContext(), this.authority, poiFilter);
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
