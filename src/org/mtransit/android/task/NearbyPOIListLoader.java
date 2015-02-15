package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.POIManager;

import android.content.Context;

public class NearbyPOIListLoader extends MTAsyncTaskLoaderV4<ArrayList<POIManager>> {

	private static final String TAG = NearbyPOIListLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ArrayList<POIManager> pois;

	private String[] agenciesAuthority;

	private double lat;

	private double lng;

	private double aroundDiff;

	private int maxSize;

	private float minCoverageInMeters;

	private boolean hideDecentOnly;

	public NearbyPOIListLoader(Context context, double lat, double lng, double aroundDiff, float minCoverageInMeters, int maxSize, boolean hideDecentOnly,
			ArrayList<String> agenciesAuthority) {
		this(context, lat, lng, aroundDiff, minCoverageInMeters, maxSize, hideDecentOnly, agenciesAuthority == null ? null : agenciesAuthority
				.toArray(new String[agenciesAuthority.size()]));
	}

	public NearbyPOIListLoader(Context context, double lat, double lng, double aroundDiff, float minCoverageInMeters, int maxSize, boolean hideDecentOnly,
			String... agenciesAuthority) {
		super(context);
		this.agenciesAuthority = agenciesAuthority;
		this.lat = lat;
		this.lng = lng;
		this.aroundDiff = aroundDiff;
		this.minCoverageInMeters = minCoverageInMeters;
		this.maxSize = maxSize;
		this.hideDecentOnly = hideDecentOnly;
	}

	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois == null) {
			this.pois = new ArrayList<POIManager>();
		}
		if (this.agenciesAuthority == null || this.agenciesAuthority.length == 0) {
			return this.pois;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<Runnable>(this.agenciesAuthority.length));
		ArrayList<Future<ArrayList<POIManager>>> taskList = new ArrayList<Future<ArrayList<POIManager>>>();
		for (String agencyAuthority : this.agenciesAuthority) {
			FindNearbyAgencyPOIsTask task = new FindNearbyAgencyPOIsTask(getContext(), agencyAuthority, this.lat, this.lng, this.aroundDiff,
					this.hideDecentOnly, this.minCoverageInMeters, this.maxSize);
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
		LocationUtils.removeTooMuchWhenNotInCoverage(this.pois, this.minCoverageInMeters, this.maxSize);
		return this.pois;
	}

	public static void filterAgencies(Collection<AgencyProperties> agencies, double lat, double lng, LocationUtils.AroundDiff ad, Double optLastAroundDiff) {
		if (agencies != null) {
			LocationUtils.Area area = LocationUtils.getArea(lat, lng, ad.aroundDiff);
			LocationUtils.Area optLastArea = optLastAroundDiff == null ? null : LocationUtils.getArea(lat, lng, optLastAroundDiff);
			Iterator<AgencyProperties> it = agencies.iterator();
			while (it.hasNext()) {
				AgencyProperties agency = it.next();
				if (!agency.isInArea(area)) {
					it.remove();
				} else if (optLastArea != null && agency.isEntirelyInside(optLastArea)) {
					it.remove();
				}
			}
		}
	}

	public static ArrayList<AgencyProperties> findTypeAgencies(Context context, int typeId, double lat, double lng, double aroundDiff, Double optLastAroundDiff) {
		ArrayList<AgencyProperties> allTypeAgencies = DataSourceProvider.get(context).getTypeDataSources(context, typeId);
		if (allTypeAgencies != null) {
			LocationUtils.Area area = LocationUtils.getArea(lat, lng, aroundDiff);
			LocationUtils.Area optLastArea = optLastAroundDiff == null ? null : LocationUtils.getArea(lat, lng, optLastAroundDiff);
			Iterator<AgencyProperties> it = allTypeAgencies.iterator();
			while (it.hasNext()) {
				AgencyProperties agency = it.next();
				if (!agency.isInArea(area)) {
					it.remove();
				} else if (optLastArea != null && agency.isEntirelyInside(optLastArea)) {
					it.remove();
				}
			}
		}
		return allTypeAgencies;
	}

	public static ArrayList<String> findTypeAgenciesAuthority(Context context, int typeId, double lat, double lng, double aroundDiff, Double optLastAroundDiff) {
		ArrayList<String> authorities = new ArrayList<String>();
		ArrayList<AgencyProperties> agencies = findTypeAgencies(context, typeId, lat, lng, aroundDiff, optLastAroundDiff);
		if (agencies != null) {
			for (AgencyProperties agency : agencies) {
				authorities.add(agency.getAuthority());
			}
		}
		return authorities;
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
