package org.mtransit.android.ui.view.map.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.map.ClusterOptions;
import org.mtransit.android.ui.view.map.ClusterOptionsProvider;
import org.mtransit.android.ui.view.map.ClusteringSettings;
import org.mtransit.android.ui.view.map.IMarker;
import org.mtransit.android.ui.view.map.utils.SphericalMercator;

import java.util.ArrayList;
import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class GridClusteringStrategy implements ClusteringStrategy, MTLog.Loggable {

	private static final String TAG = GridClusteringStrategy.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final boolean DEBUG_GRID = false;
	private DebugHelper debugHelper;

	private final MarkerOptions markerOptions = new MarkerOptions();

	private final boolean addMarkersDynamically;
	private final double baseClusterSize;
	private final IGoogleMap map;
	private final ArrayMap<DelegatingMarker, ClusterMarker> markers;
	private double clusterSize;
	private int oldZoom, zoom;
	private final int[] visibleClusters = new int[4];

	private ArrayMap<ClusterKey, ClusterMarker> clusters = new ArrayMap<>();

	private final ClusterRefresher refresher;
	private final ClusterOptionsProvider clusterOptionsProvider;

	GridClusteringStrategy(ClusteringSettings settings, IGoogleMap map, List<DelegatingMarker> markers, ClusterRefresher refresher) {
		this.clusterOptionsProvider = settings.getClusterOptionsProvider();
		this.addMarkersDynamically = settings.isAddMarkersDynamically();
		this.baseClusterSize = settings.getClusterSize();
		this.map = map;
		this.markers = new ArrayMap<>();
		this.refresher = refresher;
		this.zoom = Math.round(map.getCameraPosition().zoom);
		this.clusterSize = calculateClusterSize(zoom);
		addVisibleMarkers(markers);
	}

	@Override
	public void cleanup() {
		for (ClusterMarker cluster : clusters.values()) {
			cluster.cleanup();
		}
		clusters.clear();
		markers.clear();
		refresher.cleanup();
		if (DEBUG_GRID) {
			if (debugHelper != null) {
				debugHelper.cleanup();
			}
		}
	}

	@Override
	public void onCameraChange(CameraPosition cameraPosition) {
		oldZoom = zoom;
		zoom = Math.round(cameraPosition.zoom);
		double clusterSize = calculateClusterSize(zoom);
		if (this.clusterSize != clusterSize) {
			this.clusterSize = clusterSize;
			recalculate();
		} else if (addMarkersDynamically) {
			addMarkersInVisibleRegion();
		}
		if (DEBUG_GRID) {
			if (debugHelper == null) {
				debugHelper = new DebugHelper();
			}
			debugHelper.drawDebugGrid(map, clusterSize);
		}
	}

	@Override
	public void onClusterGroupChange(DelegatingMarker marker) {
		if (!marker.isVisible()) {
			return;
		}
		ClusterMarker oldCluster = markers.get(marker);
		if (oldCluster != null) {
			oldCluster.remove(marker);
			refresh(oldCluster);
		}
		addMarker(marker);
	}

	@Override
	public void onAdd(DelegatingMarker marker) {
		if (!marker.isVisible()) {
			return;
		}
		addMarker(marker);
	}

	private void addMarker(DelegatingMarker marker) {
		int clusterGroup = marker.getClusterGroup();
		if (clusterGroup < 0) {
			markers.put(marker, null);
			marker.changeVisible(true);
		} else {
			LatLng position = marker.getPosition();
			ClusterKey key = calculateClusterKey(clusterGroup, position);
			ClusterMarker cluster = findClusterById(key);
			cluster.add(marker);
			markers.put(marker, cluster);
			if (!addMarkersDynamically || isPositionInVisibleClusters(position)) {
				refresh(cluster);
			}
		}
	}

	private boolean isPositionInVisibleClusters(LatLng position) {
		int y = convLat(position.latitude);
		int x = convLng(position.longitude);
		int[] b = visibleClusters;
		return b[0] <= y && y <= b[2] && (b[1] <= x && x <= b[3] || b[1] > b[3] && (b[1] <= x || x <= b[3]));
	}

	@Override
	public void onRemove(DelegatingMarker marker) {
		if (!marker.isVisible()) {
			return;
		}
		removeMarker(marker);
	}

	private void removeMarker(DelegatingMarker marker) {
		ClusterMarker cluster = markers.remove(marker);
		if (cluster != null) {
			cluster.remove(marker);
			refresh(cluster);
		}
	}

	@Override
	public void onPositionChange(DelegatingMarker marker) {
		if (!marker.isVisible()) {
			return;
		}
		ClusterMarker oldCluster = markers.get(marker);
		if (oldCluster != null) {
			oldCluster.remove(marker);
			refresh(oldCluster);
		}
		addMarker(marker);
	}

	@Nullable
	@Override
	public IMarker map(com.google.android.gms.maps.model.Marker original) {
		for (ClusterMarker cluster : clusters.values()) {
			if (original.equals(cluster.getVirtual())) {
				return cluster;
			}
		}
		return null;
	}

	@Override
	public @Nullable
	List<IMarker> getDisplayedMarkers() {
		List<IMarker> displayedMarkers = new ArrayList<>();
		for (ClusterMarker cluster : clusters.values()) {
			IMarker displayedMarker = cluster.getDisplayedMarker();
			if (displayedMarker != null) {
				displayedMarkers.add(displayedMarker);
			}
		}
		for (DelegatingMarker marker : markers.keySet()) {
			if (markers.get(marker) == null) {
				displayedMarkers.add(marker);
			}
		}
		return displayedMarkers;
	}

	@Override
	public float getMinZoomLevelNotClustered(IMarker marker) {
		//noinspection SuspiciousMethodCalls DelegatingMarker implements IMarker
		if (!markers.containsKey(marker)) {
			throw new UnsupportedOperationException("marker is not visible or is a cluster");
		}
		int zoom = 0;
		while (zoom <= 25 && hasCollision(marker, zoom)) {
			zoom++;
		}
		if (zoom > 25) {
			return Float.POSITIVE_INFINITY;
		}
		return zoom;
	}

	private boolean hasCollision(IMarker marker, int zoom) {
		double clusterSize = calculateClusterSize(zoom);
		LatLng position = marker.getPosition();
		int x = (int) (SphericalMercator.scaleLongitude(position.longitude) / clusterSize);
		int y = (int) (SphericalMercator.scaleLatitude(position.latitude) / clusterSize);
		for (DelegatingMarker m : markers.keySet()) {
			if (m.equals(marker)) {
				continue;
			}
			LatLng mPosition = m.getPosition();
			int mX = (int) (SphericalMercator.scaleLongitude(mPosition.longitude) / clusterSize);
			if (x != mX) {
				continue;
			}
			int mY = (int) (SphericalMercator.scaleLatitude(mPosition.latitude) / clusterSize);
			if (y == mY) {
				return true;
			}
		}
		return false;
	}

	private ClusterMarker findClusterById(ClusterKey key) {
		ClusterMarker cluster = clusters.get(key);
		if (cluster == null) {
			cluster = new ClusterMarker(this);
			clusters.put(key, cluster);
		}
		return cluster;
	}

	@Override
	public void onVisibilityChangeRequest(DelegatingMarker marker, boolean visible) {
		if (visible) {
			addMarker(marker);
		} else {
			removeMarker(marker);
			marker.changeVisible(false);
		}
	}

	@Override
	public void onShowInfoWindow(DelegatingMarker marker) {
		if (!marker.isVisible()) {
			MTLog.d(this, "onShowInfoWindow() > SKIP (marker not visible)");
			return;
		}
		final ClusterMarker cluster = markers.get(marker);
		if (cluster == null) {
			marker.forceShowInfoWindow();
		} else if (cluster.getMarkersInternal().size() == 1) {
			cluster.refresh();
			marker.forceShowInfoWindow();
		}
	}

	private void refresh(ClusterMarker cluster) {
		if (cluster != null) {
			refresher.refresh(cluster);
		}
	}

	private void addVisibleMarkers(List<DelegatingMarker> markers) {
		if (addMarkersDynamically) {
			calculateVisibleClusters();
		}
		for (DelegatingMarker marker : markers) {
			if (marker.isVisible()) {
				addMarker(marker);
			}
		}
		refresher.refreshAll();
	}

	private void recalculate() {
		if (addMarkersDynamically) {
			calculateVisibleClusters();
		}
		if (zoomedIn()) {
			splitClusters();
		} else {
			joinClusters();
		}
		refresher.refreshAll();
	}

	private boolean zoomedIn() {
		return zoom > oldZoom;
	}

	private void splitClusters() {
		ArrayMap<ClusterKey, ClusterMarker> newClusters = new ArrayMap<>();
		for (ClusterMarker cluster : clusters.values()) {
			List<DelegatingMarker> ms = cluster.getMarkersInternal();
			if (ms.isEmpty()) {
				cluster.removeVirtual();
				continue;
			}
			final int msSize = ms.size();
			ClusterKey[] clusterIds = new ClusterKey[msSize];
			boolean allSame = true;
			for (int j = 0; j < msSize; j++) {
				clusterIds[j] = calculateClusterKey(ms.get(j).getClusterGroup(), ms.get(j).getPosition());
				if (!clusterIds[j].equals(clusterIds[0])) {
					allSame = false;
				}
			}
			if (allSame) {
				newClusters.put(clusterIds[0], cluster);
				if (addMarkersDynamically && isPositionInVisibleClusters(cluster.getMarkersInternal().get(0).getPosition())) {
					refresh(cluster);
				}
			} else {
				cluster.removeVirtual();
				for (int j = 0; j < msSize; j++) {
					cluster = newClusters.get(clusterIds[j]);
					if (cluster == null) {
						cluster = new ClusterMarker(this);
						newClusters.put(clusterIds[j], cluster);
						if (!addMarkersDynamically || isPositionInVisibleClusters(ms.get(j).getPosition())) {
							refresh(cluster);
						}
					}
					cluster.add(ms.get(j));
					markers.put(ms.get(j), cluster);
				}
			}
		}
		clusters = newClusters;
	}

	private void joinClusters() {
		ArrayMap<ClusterKey, ClusterMarker> newClusters = new ArrayMap<>();
		ArrayMap<ClusterKey, List<ClusterMarker>> oldClusters = new ArrayMap<>();
		for (ClusterMarker cluster : clusters.values()) {
			List<DelegatingMarker> ms = cluster.getMarkersInternal();
			if (ms.isEmpty()) {
				cluster.removeVirtual();
				continue;
			}
			ClusterKey clusterId = calculateClusterKey(ms.get(0).getClusterGroup(), ms.get(0).getPosition());
			List<ClusterMarker> clusterList = oldClusters.get(clusterId);
			if (clusterList == null) {
				clusterList = new ArrayList<>();
				oldClusters.put(clusterId, clusterList);
			}
			clusterList.add(cluster);
		}
		for (ClusterKey key : oldClusters.keySet()) {
			final List<ClusterMarker> clusterList = oldClusters.get(key);
			if (clusterList == null) {
				continue; // show never happen
			}
			if (clusterList.size() == 1) {
				ClusterMarker cluster = clusterList.get(0);
				newClusters.put(key, cluster);
				if (addMarkersDynamically && isPositionInVisibleClusters(cluster.getMarkersInternal().get(0).getPosition())) {
					refresh(cluster);
				}
			} else {
				ClusterMarker cluster = new ClusterMarker(this);
				newClusters.put(key, cluster);
				if (!addMarkersDynamically || isPositionInVisibleClusters(clusterList.get(0).getMarkersInternal().get(0).getPosition())) {
					refresh(cluster);
				}
				for (ClusterMarker old : clusterList) {
					old.removeVirtual();
					List<DelegatingMarker> ms = old.getMarkersInternal();
					for (DelegatingMarker m : ms) {
						cluster.add(m);
						markers.put(m, cluster);
					}
				}
			}
		}
		clusters = newClusters;
	}

	private void addMarkersInVisibleRegion() {
		calculateVisibleClusters();
		for (DelegatingMarker marker : markers.keySet()) {
			LatLng position = marker.getPosition();
			if (isPositionInVisibleClusters(position)) {
				ClusterMarker cluster = markers.get(marker);
				refresh(cluster);
			}
		}
		refresher.refreshAll();
	}

	private void calculateVisibleClusters() {
		IProjection projection = map.getProjection();
		VisibleRegion visibleRegion = projection.getVisibleRegion();
		LatLngBounds bounds = visibleRegion.latLngBounds;
		visibleClusters[0] = convLat(bounds.southwest.latitude);
		visibleClusters[1] = convLng(bounds.southwest.longitude);
		visibleClusters[2] = convLat(bounds.northeast.latitude);
		visibleClusters[3] = convLng(bounds.northeast.longitude);
	}

	private ClusterKey calculateClusterKey(int group, LatLng position) {
		int y = convLat(position.latitude);
		int x = convLng(position.longitude);
		return new ClusterKey(group, y, x);
	}

	private int convLat(double lat) {
		return (int) (SphericalMercator.scaleLatitude(lat) / clusterSize);
	}

	private int convLng(double lng) {
		return (int) (SphericalMercator.scaleLongitude(lng) / clusterSize);
	}

	private double calculateClusterSize(int zoom) {
		return baseClusterSize / (1 << zoom);
	}

	com.google.android.gms.maps.model.Marker createMarker(@NonNull List<IMarker> markers, LatLng position) {
		markerOptions.position(position);
		ClusterOptions opts = clusterOptionsProvider.getClusterOptions(markers, map.getCameraPosition().zoom);
		markerOptions.icon(opts.getIcon());
		markerOptions.alpha(opts.getAlpha());
		markerOptions.anchor(opts.getAnchorU(), opts.getAnchorV());
		markerOptions.flat(opts.isFlat());
		markerOptions.infoWindowAnchor(opts.getInfoWindowAnchorU(), opts.getInfoWindowAnchorV());
		markerOptions.rotation(opts.getRotation());
		return map.addMarker(markerOptions);
	}

	private static class ClusterKey {
		private final int group;
		private final int latitudeId;
		private final int longitudeId;

		ClusterKey(int group, int latitudeId, int longitudeId) {
			this.group = group;
			this.latitudeId = latitudeId;
			this.longitudeId = longitudeId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ClusterKey that = (ClusterKey) o;

			if (group != that.group) {
				return false;
			}
			if (latitudeId != that.latitudeId) {
				return false;
			}
			//noinspection RedundantIfStatement
			if (longitudeId != that.longitudeId) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			int result = group;
			result = 31 * result + latitudeId;
			result = 31 * result + longitudeId;
			return result;
		}
	}
}
