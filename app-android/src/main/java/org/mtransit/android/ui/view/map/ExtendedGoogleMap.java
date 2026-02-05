package org.mtransit.android.ui.view.map;

import android.graphics.Bitmap;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
@SuppressWarnings("unused")
public interface ExtendedGoogleMap {

	int MAP_TYPE_HYBRID = GoogleMap.MAP_TYPE_HYBRID;
	int MAP_TYPE_NONE = GoogleMap.MAP_TYPE_NONE;
	int MAP_TYPE_NORMAL = GoogleMap.MAP_TYPE_NORMAL;
	int MAP_TYPE_SATELLITE = GoogleMap.MAP_TYPE_SATELLITE;
	int MAP_TYPE_TERRAIN = GoogleMap.MAP_TYPE_TERRAIN;

	@Deprecated
	Circle addCircle(CircleOptions circleOptions);

	GroundOverlay addGroundOverlay(GroundOverlayOptions groundOverlayOptions);

	@NonNull
	IMarker addMarker(@NonNull ExtendedMarkerOptions markerOptions);

	Polygon addPolygon(PolygonOptions polygonOptions);

	Polyline addPolyline(PolylineOptions polylineOptions);

	TileOverlay addTileOverlay(TileOverlayOptions tileOverlayOptions);

	void animateCamera(CameraUpdate cameraUpdate, CancelableCallback cancelableCallback);

	void animateCamera(CameraUpdate cameraUpdate, int time, CancelableCallback cancelableCallback);

	void animateCamera(@NonNull CameraUpdate cameraUpdate);

	void clear();

	CameraPosition getCameraPosition();

	List<IMarker> getDisplayedMarkers();

	int getMapType();

	List<Circle> getCircles();

	List<GroundOverlay> getGroundOverlays();

	List<IMarker> getMarkers();

	IMarker getMarkerShowingInfoWindow();

	List<Polygon> getPolygons();

	List<Polyline> getPolylines();

	List<TileOverlay> getTileOverlays();

	float getMaxZoomLevel();

	float getMinZoomLevel();

	float getMinZoomLevelNotClustered(IMarker marker);

	Projection getProjection();

	UiSettings getUiSettings();

	boolean isBuildingsEnabled();

	boolean isIndoorEnabled();

	boolean isMyLocationEnabled();

	boolean isTrafficEnabled();

	void moveCamera(@NonNull CameraUpdate cameraUpdate);

	void setBuildingsEnabled(boolean buildingsEnabled);

	void setClustering(ClusteringSettings clusteringSettings);

	boolean setIndoorEnabled(boolean indoorEnabled);

	void setInfoWindowAdapter(InfoWindowAdapter infoWindowAdapter);

	void setLocationSource(LocationSource locationSource);

	void setMapType(int mapType);

	void setMapStyle(@Nullable MapStyleOptions mapStyleOptions);

	@RequiresPermission(
			anyOf = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}
	)
	void setMyLocationEnabled(boolean myLocationEnabled);

	void setOnCameraMoveStartedListener(@Nullable GoogleMap.OnCameraMoveStartedListener listener);

	void setOnCameraMoveListener(@Nullable GoogleMap.OnCameraMoveListener listener);

	void setOnCameraIdleListener(@Nullable GoogleMap.OnCameraIdleListener listener);

	void setOnInfoWindowClickListener(OnInfoWindowClickListener onInfoWindowClickListener);

	void setOnInfoWindowCloseListener(OnInfoWindowCloseListener onInfoWindowCloseListener);

	void setOnMapClickListener(OnMapClickListener onMapClickListener);

	void setOnMapLoadedCallback(OnMapLoadedCallback onMapLoadedCallback);

	void setOnMapLongClickListener(OnMapLongClickListener onMapLongClickListener);

	void setOnMarkerClickListener(OnMarkerClickListener onMarkerClickListener);

	void setOnMarkerDragListener(OnMarkerDragListener onMarkerDragListener);

	void setOnMyLocationButtonClickListener(OnMyLocationButtonClickListener listener);

	void setPadding(int left, int top, int right, int bottom);

	void setTrafficEnabled(boolean trafficEnabled);

	void snapshot(SnapshotReadyCallback callback);

	void snapshot(SnapshotReadyCallback callback, Bitmap bitmap);

	void stopAnimation();

	interface CancelableCallback extends GoogleMap.CancelableCallback {

		@Override
		void onCancel();

		@Override
		void onFinish();
	}

	interface InfoWindowAdapter {

		View getInfoContents(IMarker marker);

		View getInfoWindow(IMarker marker);
	}

	interface OnCameraMoveStartedListener extends GoogleMap.OnCameraMoveStartedListener {
		@Override
		void onCameraMoveStarted(int reason);
	}

	interface OnCameraMoveListener extends GoogleMap.OnCameraMoveListener {
		@Override
		void onCameraMove();
	}

	interface OnCameraIdleListener extends GoogleMap.OnCameraIdleListener {

		@Override
		void onCameraIdle();
	}

	interface OnInfoWindowClickListener {
		void onInfoWindowClick(@Nullable IMarker marker);
	}

	interface OnInfoWindowCloseListener {
		void onInfoWindowClose(@Nullable IMarker marker);
	}

	interface OnMapClickListener extends GoogleMap.OnMapClickListener {

		@Override
		void onMapClick(@NonNull LatLng position);
	}

	interface OnMapLoadedCallback extends GoogleMap.OnMapLoadedCallback {

		@Override
		void onMapLoaded();
	}

	interface OnMapLongClickListener extends GoogleMap.OnMapLongClickListener {

		@Override
		void onMapLongClick(@NonNull LatLng position);
	}

	interface OnMarkerClickListener {

		boolean onMarkerClick(IMarker marker);
	}

	interface OnMarkerDragListener {

		void onMarkerDragStart(IMarker marker);

		void onMarkerDrag(IMarker marker);

		void onMarkerDragEnd(IMarker marker);
	}

	interface OnMyLocationButtonClickListener extends GoogleMap.OnMyLocationButtonClickListener {

		@Override
		boolean onMyLocationButtonClick();
	}

	interface SnapshotReadyCallback extends GoogleMap.SnapshotReadyCallback {

		@Override
		void onSnapshotReady(@Nullable Bitmap snapshot);
	}
}