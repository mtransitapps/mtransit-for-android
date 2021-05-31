package org.mtransit.android.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.data.POIManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// @Deprecated for nearby + home
// TODO: POI
public class NearbyPOIListLoader extends MTAsyncTaskLoaderX<ArrayList<POIManager>> {

	private static final String TAG = NearbyPOIListLoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@Nullable
	private ArrayList<POIManager> pois;

	@Nullable
	private final String[] agenciesAuthority;

	private final double lat;

	private final double lng;

	private final double aroundDiff;

	private final int maxSize;

	private final float minCoverageInMeters;

	private final boolean hideDescentOnly;

	private final boolean avoidLoading;

	public NearbyPOIListLoader(@NonNull Context context,
							   double lat,
							   double lng,
							   double aroundDiff,
							   float minCoverageInMeters,
							   int maxSize,
							   boolean hideDescentOnly,
							   boolean avoidLoading,
							   @Nullable List<String> agenciesAuthority) {
		this(
				context,
				lat,
				lng,
				aroundDiff,
				minCoverageInMeters,
				maxSize,
				hideDescentOnly,
				avoidLoading,
				agenciesAuthority == null ? null : agenciesAuthority.toArray(new String[0])
		);
	}

	public NearbyPOIListLoader(@NonNull Context context,
							   double lat,
							   double lng,
							   double aroundDiff,
							   float minCoverageInMeters,
							   int maxSize,
							   boolean hideDescentOnly,
							   boolean avoidLoading,
							   @Nullable String... agenciesAuthority) {
		super(context);
		this.agenciesAuthority = agenciesAuthority;
		this.lat = lat;
		this.lng = lng;
		this.aroundDiff = aroundDiff;
		this.minCoverageInMeters = minCoverageInMeters;
		this.maxSize = maxSize;
		this.hideDescentOnly = hideDescentOnly;
		this.avoidLoading = avoidLoading;
	}

	@Nullable
	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois == null) {
			this.pois = new ArrayList<>();
		}
		if (this.agenciesAuthority == null || this.agenciesAuthority.length == 0) {
			return this.pois;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<>(this.agenciesAuthority.length));
		ArrayList<Future<ArrayList<POIManager>>> taskList = new ArrayList<>();
		for (String agencyAuthority : this.agenciesAuthority) {
			final FindNearbyAgencyPOIsTask task = new FindNearbyAgencyPOIsTask(
					getContext(),
					agencyAuthority,
					this.lat,
					this.lng,
					this.aroundDiff,
					this.hideDescentOnly,
					this.avoidLoading,
					this.minCoverageInMeters,
					this.maxSize
			);
			taskList.add(executor.submit(task));
		}
		for (Future<ArrayList<POIManager>> future : taskList) {
			try {
				ArrayList<POIManager> agencyNearbyStops = future.get();
				if (agencyNearbyStops != null) {
					this.pois.addAll(agencyNearbyStops);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while loading in background!");
			}
		}
		executor.shutdown();
		LocationUtils.removeTooMuchWhenNotInCoverage(this.pois, this.minCoverageInMeters, this.maxSize);
		return this.pois;
	}

	@Override
	public void deliverResult(@Nullable ArrayList<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
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
	public void onCanceled(@Nullable ArrayList<POIManager> data) {
		super.onCanceled(data);
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
	}
}
