package org.mtransit.android.ui.view.map.impl;

import android.graphics.Bitmap;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
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

	void animateCamera(CameraUpdate update, CancelableCallback callback);

	void animateCamera(CameraUpdate update, int durationMs, CancelableCallback callback);

	void animateCamera(CameraUpdate update);

	void clear();

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

	void moveCamera(CameraUpdate update);

	void setBuildingsEnabled(boolean enabled);

	boolean setIndoorEnabled(boolean enabled);

	void setInfoWindowAdapter(InfoWindowAdapter adapter);

	void setLocationSource(LocationSource source);

	void setMapType(int type);

	@RequiresPermission(
			anyOf = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}
	)
	void setMyLocationEnabled(boolean enabled);

	@Deprecated
	void setOnCameraChangeListener(OnCameraChangeListener listener);

	void setOnInfoWindowClickListener(OnInfoWindowClickListener listener);

	void setOnMapClickListener(OnMapClickListener listener);

	void setOnMapLoadedCallback(GoogleMap.OnMapLoadedCallback callback);

	void setOnMapLongClickListener(OnMapLongClickListener listener);

	void setOnMarkerClickListener(OnMarkerClickListener listener);

	void setOnMarkerDragListener(OnMarkerDragListener listener);

	void setOnMyLocationButtonClickListener(OnMyLocationButtonClickListener listener);

	void setPadding(int left, int top, int right, int bottom);

	void setTrafficEnabled(boolean enabled);

	void snapshot(GoogleMap.SnapshotReadyCallback callback);

	void snapshot(GoogleMap.SnapshotReadyCallback callback, Bitmap bitmap);

	void stopAnimation();

	GoogleMap getMap();
}