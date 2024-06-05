package org.mtransit.android.ui.view.map.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.VisibleRegion;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.map.IMarker;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class DynamicNoClusteringStrategy implements ClusteringStrategy, MTLog.Loggable {

	private static final String TAG = DynamicNoClusteringStrategy.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private final IGoogleMap map;
	private final Set<DelegatingMarker> markers = new HashSet<>();
	private LatLngBounds visibleRegionBounds;

	DynamicNoClusteringStrategy(IGoogleMap map, List<DelegatingMarker> markers) {
		this.map = map;
		for (DelegatingMarker marker : markers) {
			if (marker.isVisible()) {
				this.markers.add(marker);
			}
		}
		showMarkersInVisibleRegion();
	}

	@Override
	public void cleanup() {
		markers.clear();
	}

	@Override
	public void onCameraChange(CameraPosition cameraPosition) {
		showMarkersInVisibleRegion();
	}

	@Override
	public void onClusterGroupChange(DelegatingMarker marker) {
		// DO NOTHING
	}

	@Override
	public void onAdd(DelegatingMarker marker) {
		if (!marker.isVisible()) {
			return;
		}
		addMarker(marker);
	}

	@Override
	public void onRemove(DelegatingMarker marker) {
		if (!marker.isVisible()) {
			return;
		}
		markers.remove(marker);
	}

	@Override
	public void onPositionChange(DelegatingMarker marker) {
		if (!marker.isVisible()) {
			return;
		}
		if (markers.contains(marker)) {
			if (visibleRegionBounds.contains(marker.getPosition())) {
				markers.remove(marker);
				marker.changeVisible(true);
			}
		}
	}

	@Override
	public void onVisibilityChangeRequest(DelegatingMarker marker, boolean visible) {
		if (visible) {
			addMarker(marker);
		} else {
			markers.remove(marker);
			marker.changeVisible(false);
		}
	}

	@Override
	public void onShowInfoWindow(DelegatingMarker marker) {
		if (!marker.isVisible()) {
			MTLog.d(this, "onShowInfoWindow() > SKIP (marker not visible)");
			return;
		}
		if (markers.remove(marker)) {
			marker.changeVisible(true);
		}
		marker.forceShowInfoWindow();
	}

	@Nullable
	@Override
	public IMarker map(com.google.android.gms.maps.model.Marker original) {
		return null;
	}

	@Nullable
	@Override
	public List<IMarker> getDisplayedMarkers() {
		return null;
	}

	@Override
	public float getMinZoomLevelNotClustered(IMarker marker) {
		return 0.0f;
	}

	private void showMarkersInVisibleRegion() {
		IProjection projection = map.getProjection();
		VisibleRegion visibleRegion = projection.getVisibleRegion();
		visibleRegionBounds = visibleRegion.latLngBounds;
		Iterator<DelegatingMarker> iterator = markers.iterator();
		while (iterator.hasNext()) {
			DelegatingMarker marker = iterator.next();
			if (visibleRegionBounds.contains(marker.getPosition())) {
				marker.changeVisible(true);
				iterator.remove();
			}
		}
	}

	private void addMarker(DelegatingMarker marker) {
		if (visibleRegionBounds.contains(marker.getPosition())) {
			marker.changeVisible(true);
		} else {
			markers.add(marker);
		}
	}
}
