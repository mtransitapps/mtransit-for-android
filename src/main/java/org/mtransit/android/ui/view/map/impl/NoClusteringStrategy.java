package org.mtransit.android.ui.view.map.impl;

import com.google.android.gms.maps.model.CameraPosition;

import java.util.List;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.map.IMarker;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class NoClusteringStrategy implements ClusteringStrategy, MTLog.Loggable {

	private static final String TAG = NoClusteringStrategy.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public NoClusteringStrategy(List<DelegatingMarker> markers) {
		for (DelegatingMarker marker : markers) {
			if (marker.isVisible()) {
				marker.changeVisible(true);
			}
		}
	}

	@Override
	public void cleanup() {

	}

	@Override
	public void onCameraChange(CameraPosition cameraPosition) {

	}

	@Override
	public void onClusterGroupChange(DelegatingMarker marker) {

	}

	@Override
	public void onAdd(DelegatingMarker marker) {

	}

	@Override
	public void onRemove(DelegatingMarker marker) {

	}

	@Override
	public void onPositionChange(DelegatingMarker marker) {

	}

	@Override
	public void onVisibilityChangeRequest(DelegatingMarker marker, boolean visible) {
		marker.changeVisible(visible);
	}

	@Override
	public void onShowInfoWindow(DelegatingMarker marker) {
		marker.forceShowInfoWindow();
	}

	@Override
	public IMarker map(com.google.android.gms.maps.model.Marker original) {
		return null;
	}

	@Override
	public List<IMarker> getDisplayedMarkers() {
		return null;
	}

	@Override
	public float getMinZoomLevelNotClustered(IMarker marker) {
		return 0.0f;
	}
}
