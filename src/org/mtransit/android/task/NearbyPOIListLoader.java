package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIManager;

import android.content.Context;

public class NearbyPOIListLoader extends MTAsyncTaskLoaderV4<ArrayList<POIManager>> {

	private static final String TAG = NearbyPOIListLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ArrayList<POIManager> pois;


	private AgencyProperties[] agencies;

	private double lat;

	private double lng;

	private double aroundDiff;

	private int maxSize;

	private int minCoverage;

	private boolean hideDecentOnly;

	public NearbyPOIListLoader(Context context, double lat, double lng, double aroundDiff, int minCoverage, int maxSize, boolean hideDecentOnly,
			ArrayList<AgencyProperties> agencies) {
		this(context, lat, lng, aroundDiff, minCoverage, maxSize, hideDecentOnly, agencies == null ? null : agencies.toArray(new AgencyProperties[] {}));
	}

	public NearbyPOIListLoader(Context context, double lat, double lng, double aroundDiff, int minCoverage, int maxSize, boolean hideDecentOnly,
			AgencyProperties... agencies) {
		super(context);
		this.agencies = agencies;
		this.lat = lat;
		this.lng = lng;
		this.aroundDiff = aroundDiff;
		this.minCoverage = minCoverage;
		this.maxSize = maxSize;
	}

	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois == null) {
			this.pois = new ArrayList<POIManager>();
		}
		if (this.agencies == null || this.agencies.length == 0) {
			return this.pois;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(this.agencies.length));
		ArrayList<Future<ArrayList<POIManager>>> taskList = new ArrayList<Future<ArrayList<POIManager>>>();
		for (AgencyProperties agency : this.agencies) {
			FindNearbyAgencyPOIsTask task = new FindNearbyAgencyPOIsTask(getContext(), agency.getAuthority(), this.lat, this.lng, this.aroundDiff,
					this.hideDecentOnly, this.minCoverage, this.maxSize);
			taskList.add(executor.submit(task));
		}
		for (Future<ArrayList<POIManager>> future : taskList) {
			try {
				ArrayList<POIManager> agencyNearbyStops = future.get();
				this.pois.addAll(agencyNearbyStops);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while loading in background!");
			}
		}
		executor.shutdown();
		CollectionUtils.sort(this.pois, POIManager.POI_DISTANCE_COMPARATOR);
		LocationUtils.removeTooMuchWhenNotInCoverage(this.pois, this.minCoverage, this.maxSize);
		return this.pois;
	}

	public static void filterAgencies(Collection<AgencyProperties> agencies, double lat, double lng, LocationUtils.AroundDiff ad) {
		if (agencies != null) {
			Iterator<AgencyProperties> it = agencies.iterator();
			while (it.hasNext()) {
				if (!it.next().isInArea(lat, lng, ad.aroundDiff)) {
					it.remove();
				}
			}
		}
	}

	public static ArrayList<AgencyProperties> findTypeAgencies(Context context, DataSourceType type, double lat, double lng, double aroundDiff) {
		ArrayList<AgencyProperties> allTypeAgencies = DataSourceProvider.get(context).getTypeDataSources(type.getId());
		if (allTypeAgencies != null) {
			Iterator<AgencyProperties> it = allTypeAgencies.iterator();
			while (it.hasNext()) {
				if (!it.next().isInArea(lat, lng, aroundDiff)) {
					it.remove();
				}
			}
		}
		return allTypeAgencies;
	}

	@Override
	public void deliverResult(ArrayList<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		} else {
			MTLog.d(this, "deliverResult() > loader NOT started, not delivering result");
		}
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
	public void onCanceled(ArrayList<POIManager> data) {
		super.onCanceled(data);
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
	}

}
