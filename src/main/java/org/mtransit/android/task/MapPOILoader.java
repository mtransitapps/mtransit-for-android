package org.mtransit.android.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.RuntimeUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.commons.task.MTCallable;
import org.mtransit.android.data.AgencyProperties;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.ui.view.MapViewController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MapPOILoader extends MTAsyncTaskLoaderV4<Collection<MapViewController.POIMarker>> {

	private static final String LOG_TAG = MapPOILoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	private final Collection<Integer> filterTypeIds;
	@Nullable
	private final LatLngBounds latLngBounds;
	@Nullable
	private final LatLngBounds loadedLatLngBounds;

	private Collection<MapViewController.POIMarker> poiMarkers;

	public MapPOILoader(@NonNull Context context,
						@Nullable Collection<Integer> filterTypeIds,
						@Nullable LatLngBounds latLngBounds,
						@Nullable LatLngBounds loadedLatLngBounds) {
		super(context);
		this.filterTypeIds = filterTypeIds;
		this.latLngBounds = latLngBounds;
		this.loadedLatLngBounds = loadedLatLngBounds;
	}

	@Nullable
	@Override
	public Collection<MapViewController.POIMarker> loadInBackgroundMT() {
		if (this.poiMarkers != null) {
			return this.poiMarkers;
		}
		this.poiMarkers = new HashSet<>();
		ArrayList<AgencyProperties> agencies = DataSourceProvider.get(getContext()).getAllAgencies(getContext());
		if (agencies == null || agencies.isEmpty()) {
			return this.poiMarkers;
		}
		if (this.latLngBounds == null) {
			return this.poiMarkers;
		}
		ThreadPoolExecutor executor = new ThreadPoolExecutor(RuntimeUtils.NUMBER_OF_CORES, RuntimeUtils.NUMBER_OF_CORES, 1, TimeUnit.SECONDS,
				new LinkedBlockingDeque<>(agencies.size()));
		ArrayList<Future<ArrayMap<LatLng, MapViewController.POIMarker>>> taskList = new ArrayList<>();
		for (AgencyProperties agency : agencies) {
			DataSourceType type = agency.getType();
			if (!type.isMapScreen()) {
				continue;
			}
			if (!agency.isInArea(this.latLngBounds)) {
				continue;
			}
			if (agency.isEntirelyInside(this.loadedLatLngBounds)) {
				continue;
			}
			if (this.filterTypeIds != null
					&& !this.filterTypeIds.isEmpty()
					&& !this.filterTypeIds.contains(type.getId())) {
				continue;
			}
			FindAgencyPOIsTask task = new FindAgencyPOIsTask(getContext(), agency, this.latLngBounds, this.loadedLatLngBounds);
			taskList.add(executor.submit(task));
		}
		ArrayMap<LatLng, MapViewController.POIMarker> positionToPoiMarkers = new ArrayMap<>();
		for (Future<ArrayMap<LatLng, MapViewController.POIMarker>> future : taskList) {
			try {
				ArrayMap<LatLng, MapViewController.POIMarker> agencyPOIs = future.get();
				if (agencyPOIs != null) {
					for (ArrayMap.Entry<LatLng, MapViewController.POIMarker> agencyMarker : agencyPOIs.entrySet()) {
						MapViewController.POIMarker poiMarker = positionToPoiMarkers.get(agencyMarker.getKey());
						if (poiMarker == null) {
							poiMarker = agencyMarker.getValue();
						} else {
							poiMarker.merge(agencyMarker.getValue());
						}
						positionToPoiMarkers.put(agencyMarker.getKey(), poiMarker);
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while loading in background!");
			}
		}
		executor.shutdown();
		this.poiMarkers = positionToPoiMarkers.values();
		return this.poiMarkers;
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		if (this.poiMarkers != null) {
			deliverResult(this.poiMarkers);
		}
		if (this.poiMarkers == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void deliverResult(@Nullable Collection<MapViewController.POIMarker> data) {
		this.poiMarkers = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

	private static class FindAgencyPOIsTask extends MTCallable<ArrayMap<LatLng, MapViewController.POIMarker>> {

		private static final String LOG_TAG = MapPOILoader.class.getSimpleName() + ">" + FindAgencyPOIsTask.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private final Context context;
		private final AgencyProperties agency;
		private final LatLngBounds latLngBounds;
		private final LatLngBounds loadedLatLngBounds;

		FindAgencyPOIsTask(Context context, AgencyProperties agency, LatLngBounds latLngBounds, LatLngBounds loadedLatLngBounds) {
			this.context = context;
			this.agency = agency;
			this.latLngBounds = latLngBounds;
			this.loadedLatLngBounds = loadedLatLngBounds;
		}

		@Override
		public ArrayMap<LatLng, MapViewController.POIMarker> callMT() {
			double minLat = Math.min(this.latLngBounds.northeast.latitude, this.latLngBounds.southwest.latitude);
			double maxLat = Math.max(this.latLngBounds.northeast.latitude, this.latLngBounds.southwest.latitude);
			double minLng = Math.min(this.latLngBounds.northeast.longitude, this.latLngBounds.southwest.longitude);
			double maxLng = Math.max(this.latLngBounds.northeast.longitude, this.latLngBounds.southwest.longitude);
			Double optLoadedMinLat = this.loadedLatLngBounds == null ? null : //
					Math.min(this.loadedLatLngBounds.northeast.latitude, this.loadedLatLngBounds.southwest.latitude);
			Double optLoadedMaxLat = this.loadedLatLngBounds == null ? null : //
					Math.max(this.loadedLatLngBounds.northeast.latitude, this.loadedLatLngBounds.southwest.latitude);
			Double optLoadedMinLng = this.loadedLatLngBounds == null ? null : //
					Math.min(this.loadedLatLngBounds.northeast.longitude, this.loadedLatLngBounds.southwest.longitude);
			Double optLoadedMaxLng = this.loadedLatLngBounds == null ? null : //
					Math.max(this.loadedLatLngBounds.northeast.longitude, this.loadedLatLngBounds.southwest.longitude);
			POIProviderContract.Filter poiFilter = POIProviderContract.Filter.getNewAreaFilter( //
					minLat, maxLat, minLng, maxLng, optLoadedMinLat, optLoadedMaxLat, optLoadedMinLng, optLoadedMaxLng);
			ArrayMap<LatLng, MapViewController.POIMarker> clusterItems = new ArrayMap<>();
			ArrayList<POIManager> poims = DataSourceManager.findPOIs(this.context, this.agency.getAuthority(), poiFilter);
			String agencyShortName = this.agency.getShortName();
			if (poims != null) {
				LatLng position;
				LatLng positionTrunc;
				String name;
				String extra;
				String uuid;
				String authority;
				Integer color;
				Integer secondaryColor;
				for (POIManager poim : poims) {
					position = MapViewController.POIMarker.getLatLng(poim);
					positionTrunc = MapViewController.POIMarker.getLatLngTrunc(poim);
					if (!this.latLngBounds.contains(position)) {
						continue;
					}
					if (this.loadedLatLngBounds != null && this.loadedLatLngBounds.contains(position)) {
						continue;
					}
					name = poim.poi.getName();
					extra = null;
					if (poim.poi instanceof RouteTripStop) {
						extra = ((RouteTripStop) poim.poi).getRoute().getShortestName();
					}
					uuid = poim.poi.getUUID();
					authority = poim.poi.getAuthority();
					color = POIManager.getColor(this.context, poim.poi, null);
					secondaryColor = agency.getColorInt();
					MapViewController.POIMarker clusterItem = clusterItems.get(positionTrunc);
					if (clusterItem == null) {
						clusterItem = new MapViewController.POIMarker(position, name, agencyShortName, extra, color, secondaryColor, uuid, authority);
					} else {
						clusterItem.merge(position, name, agencyShortName, extra, color, secondaryColor, uuid, authority);
					}
					clusterItems.put(positionTrunc, clusterItem);
				}
			}
			return clusterItems;
		}
	}
}
