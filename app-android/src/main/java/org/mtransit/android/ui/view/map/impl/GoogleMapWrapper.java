package org.mtransit.android.ui.view.map.impl;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import org.mtransit.android.commons.MTLog;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class GoogleMapWrapper implements IGoogleMap, MTLog.Loggable {

	private static final String TAG = GoogleMapWrapper.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@NonNull
	private final GoogleMap map;

	GoogleMapWrapper(@NonNull GoogleMap map) {
		this.map = map;
	}

	@Override
	public final Circle addCircle(CircleOptions options) {
		return map.addCircle(options);
	}

	@Override
	public final GroundOverlay addGroundOverlay(GroundOverlayOptions options) {
		return map.addGroundOverlay(options);
	}

	@Override
	public final Marker addMarker(MarkerOptions options) {
		return map.addMarker(options);
	}

	@Override
	public final Polygon addPolygon(PolygonOptions options) {
		return map.addPolygon(options);
	}

	@Override
	public final Polyline addPolyline(PolylineOptions options) {
		return map.addPolyline(options);
	}

	@Override
	public final TileOverlay addTileOverlay(TileOverlayOptions options) {
		return map.addTileOverlay(options);
	}

	@Override
	public final void animateCamera(CameraUpdate update, GoogleMap.CancelableCallback callback) {
		map.animateCamera(update, callback);
	}

	@Override
	public final void animateCamera(CameraUpdate update, int durationMs, GoogleMap.CancelableCallback callback) {
		map.animateCamera(update, durationMs, callback);
	}

	@Override
	public final void animateCamera(@NonNull CameraUpdate update) {
		map.animateCamera(update);
	}

	@Override
	public final void clear() {
		map.clear();
	}

	@Override
	public final CameraPosition getCameraPosition() {
		return map.getCameraPosition();
	}

	@Override
	public final int getMapType() {
		return map.getMapType();
	}

	@Override
	public final float getMaxZoomLevel() {
		return map.getMaxZoomLevel();
	}

	@Override
	public final float getMinZoomLevel() {
		return map.getMinZoomLevel();
	}

	@Override
	public final ProjectionWrapper getProjection() {
		return new ProjectionWrapper(map.getProjection());
	}

	@Override
	public final UiSettings getUiSettings() {
		return map.getUiSettings();
	}

	@Override
	public final boolean isBuildingsEnabled() {
		return map.isBuildingsEnabled();
	}

	@Override
	public final boolean isIndoorEnabled() {
		return map.isIndoorEnabled();
	}

	@Override
	public final boolean isMyLocationEnabled() {
		return map.isMyLocationEnabled();
	}

	@Override
	public final boolean isTrafficEnabled() {
		return map.isTrafficEnabled();
	}

	@Override
	public final void moveCamera(@NonNull CameraUpdate update) {
		map.moveCamera(update);
	}

	@Override
	public final void setBuildingsEnabled(boolean enabled) {
		map.setBuildingsEnabled(enabled);
	}

	@Override
	public final boolean setIndoorEnabled(boolean enabled) {
		return map.setIndoorEnabled(enabled);
	}

	@Override
	public final void setInfoWindowAdapter(GoogleMap.InfoWindowAdapter adapter) {
		map.setInfoWindowAdapter(adapter);
	}

	@Override
	public final void setLocationSource(LocationSource source) {
		map.setLocationSource(source);
	}

	@Override
	public final void setMapType(int type) {
		map.setMapType(type);
	}

	@Override
	public final void setMapStyle(@Nullable MapStyleOptions mapStyle) {
		map.setMapStyle(mapStyle);
	}

	@SuppressLint("MissingPermission")
	@RequiresPermission(
			anyOf = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}
	)
	@Override
	public final void setMyLocationEnabled(boolean enabled) {
		map.setMyLocationEnabled(enabled);
	}

	@Override
	public final void setOnCameraMoveStartedListener(@Nullable GoogleMap.OnCameraMoveStartedListener listener) {
		map.setOnCameraMoveStartedListener(listener);
	}

	@Override
	public final void setOnCameraMoveListener(@Nullable GoogleMap.OnCameraMoveListener listener) {
		map.setOnCameraMoveListener(listener);
	}

	@Override
	public final void setOnCameraIdleListener(@Nullable GoogleMap.OnCameraIdleListener listener) {
		map.setOnCameraIdleListener(listener);
	}

	@Override
	public final void setOnInfoWindowClickListener(GoogleMap.OnInfoWindowClickListener listener) {
		map.setOnInfoWindowClickListener(listener);
	}

	@Override
	public final void setOnInfoWindowCloseListener(GoogleMap.OnInfoWindowCloseListener listener) {
		map.setOnInfoWindowCloseListener(listener);
	}

	@Override
	public final void setOnMapClickListener(GoogleMap.OnMapClickListener listener) {
		map.setOnMapClickListener(listener);
	}

	@Override
	public void setOnMapLoadedCallback(GoogleMap.OnMapLoadedCallback callback) {
		map.setOnMapLoadedCallback(callback);
	}

	@Override
	public final void setOnMapLongClickListener(GoogleMap.OnMapLongClickListener listener) {
		map.setOnMapLongClickListener(listener);
	}

	@Override
	public final void setOnMarkerClickListener(GoogleMap.OnMarkerClickListener listener) {
		map.setOnMarkerClickListener(listener);
	}

	@Override
	public final void setOnMarkerDragListener(GoogleMap.OnMarkerDragListener listener) {
		map.setOnMarkerDragListener(listener);
	}

	@Override
	public void setOnMyLocationButtonClickListener(GoogleMap.OnMyLocationButtonClickListener listener) {
		map.setOnMyLocationButtonClickListener(listener);
	}

	@Override
	public void setPadding(int left, int top, int right, int bottom) {
		map.setPadding(left, top, right, bottom);
	}

	@Override
	public final void setTrafficEnabled(boolean enabled) {
		map.setTrafficEnabled(enabled);
	}

	@Override
	public void snapshot(GoogleMap.SnapshotReadyCallback callback) {
		map.snapshot(callback);
	}

	@Override
	public void snapshot(GoogleMap.SnapshotReadyCallback callback, Bitmap bitmap) {
		map.snapshot(callback, bitmap);
	}

	@Override
	public final void stopAnimation() {
		map.stopAnimation();
	}

	@NonNull
	@Override
	public GoogleMap getMap() {
		return map;
	}
}
