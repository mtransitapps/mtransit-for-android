package org.mtransit.android.ui.view.map.impl;

import androidx.annotation.Nullable;

import org.mtransit.android.ui.view.map.IMarker;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
interface ClusteringStrategy {

	void cleanup();

	void onCameraMoveStarted(int reason);

	void onCameraMove();

	void onCameraIdle();

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
