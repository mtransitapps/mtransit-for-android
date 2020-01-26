package org.mtransit.android.ui.view.map.impl;

import java.util.List;

import org.mtransit.android.ui.view.map.IMarker;

import com.google.android.gms.maps.model.CameraPosition;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
interface ClusteringStrategy {

	void cleanup();

	void onCameraChange(CameraPosition cameraPosition);

	void onClusterGroupChange(DelegatingMarker marker);

	void onAdd(DelegatingMarker marker);

	void onRemove(DelegatingMarker marker);

	void onPositionChange(DelegatingMarker marker);

	void onVisibilityChangeRequest(DelegatingMarker marker, boolean visible);

	void onShowInfoWindow(DelegatingMarker marker);

	IMarker map(com.google.android.gms.maps.model.Marker original);

	List<IMarker> getDisplayedMarkers();

	float getMinZoomLevelNotClustered(IMarker marker);
}
