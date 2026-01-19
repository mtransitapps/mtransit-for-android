package org.mtransit.android.ui.view.map.impl;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.map.AnimationSettings;
import org.mtransit.android.ui.view.map.ClusteringSettings;
import org.mtransit.android.ui.view.map.ExtendedMarkerOptions;
import org.mtransit.android.ui.view.map.IMarker;
import org.mtransit.android.ui.view.map.lazy.LazyMarker;

import java.util.ArrayList;
import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
@SuppressWarnings("WeakerAccess")
class MarkerManager implements LazyMarker.OnMarkerCreateListener, MTLog.Loggable {

	private static final String LOG_TAG = MarkerManager.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final IGoogleMap factory;

	private final ArrayMap<LazyMarker, DelegatingMarker> markers;
	private final ArrayMap<Marker, LazyMarker> createdMarkers;

	private IMarker markerShowingInfoWindow;

	private ClusteringSettings clusteringSettings = new ClusteringSettings().enabled(false);
	private ClusteringStrategy clusteringStrategy = new NoClusteringStrategy(new ArrayList<>());

	private final MarkerAnimator markerAnimator = new MarkerAnimator();

	public MarkerManager(@NonNull IGoogleMap factory) {
		this.factory = factory;
		this.markers = new ArrayMap<>();
		this.createdMarkers = new ArrayMap<>();
	}

	@NonNull
	public IMarker addMarker(@NonNull ExtendedMarkerOptions markerOptions) {
		final boolean visible = markerOptions.isVisible();
		markerOptions.visible(false);
		final DelegatingMarker marker = createMarker(markerOptions.getReal());
		setExtendedOptions(marker, markerOptions);
		clusteringStrategy.onAdd(marker);
		marker.setVisible(visible);
		markerOptions.visible(visible);
		return marker;
	}

	private void setExtendedOptions(@NonNull DelegatingMarker marker, @NonNull ExtendedMarkerOptions markerOptions) {
		marker.setClusterGroup(markerOptions.getClusterGroup());
		marker.setData(markerOptions.getData());
		marker.setIcon(
				markerOptions.getContext(),
				markerOptions.getIconResId(),
				markerOptions.getReplaceColor(),
				markerOptions.getColor(),
				markerOptions.getSecondaryColor(),
				markerOptions.getDefaultColor()
		);
	}

	@NonNull
	private DelegatingMarker createMarker(@NonNull com.google.android.gms.maps.model.MarkerOptions markerOptions) {
		LazyMarker realMarker = new LazyMarker(factory.getMap(), markerOptions, this);
		DelegatingMarker marker = new DelegatingMarker(realMarker, this);
		// TODO CRASH SimpleArrayMap ClassCastException: String cannot be cast to Object[]
		markers.put(realMarker, marker);
		return marker;
	}

	public void clear() {
		markers.clear();
		createdMarkers.clear();
		clusteringStrategy.cleanup();
	}

	public List<IMarker> getDisplayedMarkers() {
		List<IMarker> displayedMarkers = clusteringStrategy.getDisplayedMarkers();
		if (displayedMarkers == null) {
			displayedMarkers = getMarkers();
			displayedMarkers.removeIf(m -> !m.isVisible());
		}
		return displayedMarkers;
	}

	public List<IMarker> getMarkers() {
		return new ArrayList<>(markers.values());
	}

	public IMarker getMarkerShowingInfoWindow() {
		if (markerShowingInfoWindow != null && !markerShowingInfoWindow.isInfoWindowShown()) {
			markerShowingInfoWindow = null;
		}
		return markerShowingInfoWindow;
	}

	public float getMinZoomLevelNotClustered(IMarker marker) {
		return clusteringStrategy.getMinZoomLevelNotClustered(marker);
	}

	public void onAnimateMarkerPosition(DelegatingMarker marker, LatLng target, AnimationSettings settings, IMarker.AnimationCallback callback) {
		markerAnimator.cancelAnimation(marker, IMarker.AnimationCallback.CancelReason.ANIMATE_POSITION);
		markerAnimator.animate(marker, marker.getPosition(), target, SystemClock.uptimeMillis(), settings, callback);
	}

	public void onCameraMoveStarted(int reason) {
		clusteringStrategy.onCameraMoveStarted(reason);
	}

	public void onCameraMove() {
		clusteringStrategy.onCameraMove();
	}

	public void onCameraIdle() {
		clusteringStrategy.onCameraIdle();
	}

	public void onClusterGroupChange(DelegatingMarker marker) {
		clusteringStrategy.onClusterGroupChange(marker);
	}

	public void onDragStart(DelegatingMarker marker) {
		markerAnimator.cancelAnimation(marker, IMarker.AnimationCallback.CancelReason.DRAG_START);
	}

	public void onPositionChange(DelegatingMarker marker) {
		clusteringStrategy.onPositionChange(marker);
		markerAnimator.cancelAnimation(marker, IMarker.AnimationCallback.CancelReason.SET_POSITION);
	}

	public void onPositionDuringAnimationChange(DelegatingMarker marker) {
		clusteringStrategy.onPositionChange(marker);
	}

	public void onRemove(DelegatingMarker marker) {
		markers.remove(marker.getReal());
		createdMarkers.remove(marker.getReal().getMarker());
		clusteringStrategy.onRemove(marker);
		markerAnimator.cancelAnimation(marker, IMarker.AnimationCallback.CancelReason.REMOVE);
	}

	public void onShowInfoWindow(DelegatingMarker marker) {
		clusteringStrategy.onShowInfoWindow(marker);
	}

	public void onVisibilityChangeRequest(DelegatingMarker marker, boolean visible) {
		clusteringStrategy.onVisibilityChangeRequest(marker, visible);
	}

	public void setClustering(ClusteringSettings clusteringSettings) {
		if (clusteringSettings == null) {
			clusteringSettings = new ClusteringSettings().enabled(false);
		}
		if (!this.clusteringSettings.equals(clusteringSettings)) {
			this.clusteringSettings = clusteringSettings;
			clusteringStrategy.cleanup();
			ArrayList<DelegatingMarker> list = new ArrayList<>(markers.values());
			if (clusteringSettings.isEnabled()) {
				clusteringStrategy = new GridClusteringStrategy(clusteringSettings, factory, list, new ClusterRefresher());
			} else if (clusteringSettings.isAddMarkersDynamically()) {
				clusteringStrategy = new DynamicNoClusteringStrategy(factory, list);
			} else {
				clusteringStrategy = new NoClusteringStrategy(list);
			}
		}
	}

	public void setMarkerShowingInfoWindow(IMarker marker) {
		this.markerShowingInfoWindow = marker;
	}

	@Override
	public void onMarkerCreate(@NonNull LazyMarker marker) {
		createdMarkers.put(marker.getMarker(), marker);
	}

	@Nullable
	public IMarker map(com.google.android.gms.maps.model.Marker marker) {
		final IMarker cluster = clusteringStrategy.map(marker);
		if (cluster != null) {
			return cluster;
		}
		return mapToDelegatingMarker(marker);
	}

	@Nullable
	public DelegatingMarker mapToDelegatingMarker(com.google.android.gms.maps.model.Marker marker) {
		final LazyMarker lazy = createdMarkers.get(marker);
		return markers.get(lazy);
	}
}
