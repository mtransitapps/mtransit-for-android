package org.mtransit.android.ui.view.map.impl;

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

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
interface IGoogleMap {

	Circle addCircle(CircleOptions options);

	GroundOverlay addGroundOverlay(GroundOverlayOptions options);

	Marker addMarker(MarkerOptions options);

	Polygon addPolygon(PolygonOptions options);

	Polyline addPolyline(PolylineOptions options);

	TileOverlay addTileOverlay(TileOverlayOptions options);

	void animateCamera(CameraUpdate update, GoogleMap.CancelableCallback callback);

	void animateCamera(CameraUpdate update, int durationMs, GoogleMap.CancelableCallback callback);

	void animateCamera(@NonNull CameraUpdate update);

	void clear();

	@NonNull
	CameraPosition getCameraPosition();

	int getMapType();

	float getMaxZoomLevel();

	float getMinZoomLevel();

	IProjection getProjection();

	UiSettings getUiSettings();

	boolean isBuildingsEnabled();

	boolean isIndoorEnabled();

	boolean isMyLocationEnabled();

	boolean isTrafficEnabled();

	void moveCamera(@NonNull CameraUpdate update);

	void setBuildingsEnabled(boolean enabled);

	boolean setIndoorEnabled(boolean enabled);

	void setInfoWindowAdapter(GoogleMap.InfoWindowAdapter adapter);

	void setLocationSource(LocationSource source);

	void setMapType(int type);

	void setMapStyle(@Nullable MapStyleOptions mapStyle);

	@RequiresPermission(
			anyOf = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}
	)
	void setMyLocationEnabled(boolean enabled);

	void setOnCameraMoveStartedListener(@Nullable GoogleMap.OnCameraMoveStartedListener listener);

	void setOnCameraMoveListener(@Nullable GoogleMap.OnCameraMoveListener listener);

	void setOnCameraIdleListener(@Nullable GoogleMap.OnCameraIdleListener listener);

	void setOnInfoWindowClickListener(GoogleMap.OnInfoWindowClickListener listener);

	void setOnInfoWindowCloseListener(GoogleMap.OnInfoWindowCloseListener listener);

	void setOnMapClickListener(GoogleMap.OnMapClickListener listener);

	void setOnMapLoadedCallback(GoogleMap.OnMapLoadedCallback callback);

	void setOnMapLongClickListener(GoogleMap.OnMapLongClickListener listener);

	void setOnMarkerClickListener(GoogleMap.OnMarkerClickListener listener);

	void setOnMarkerDragListener(GoogleMap.OnMarkerDragListener listener);

	void setOnMyLocationButtonClickListener(GoogleMap.OnMyLocationButtonClickListener listener);

	void setPadding(int left, int top, int right, int bottom);

	void setTrafficEnabled(boolean enabled);

	void snapshot(GoogleMap.SnapshotReadyCallback callback);

	void snapshot(GoogleMap.SnapshotReadyCallback callback, Bitmap bitmap);

	void stopAnimation();

	GoogleMap getMap();
}