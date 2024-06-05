package org.mtransit.android.ui.view.map.impl;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.CameraPosition;

import org.mtransit.android.ui.view.map.IMarker;

import java.util.List;

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

	@Nullable
	IMarker map(com.google.android.gms.maps.model.Marker original);

	@Nullable
	List<IMarker> getDisplayedMarkers();

	float getMinZoomLevelNotClustered(IMarker marker);
}
