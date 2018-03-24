package org.mtransit.android.ui.view.map;

import java.util.List;

import android.graphics.Bitmap;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public interface ExtendedGoogleMap {

	int MAP_TYPE_HYBRID = com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID;
	int MAP_TYPE_NONE = com.google.android.gms.maps.GoogleMap.MAP_TYPE_NONE;
	int MAP_TYPE_NORMAL = com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;
	int MAP_TYPE_SATELLITE = com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE;
	int MAP_TYPE_TERRAIN = com.google.android.gms.maps.GoogleMap.MAP_TYPE_TERRAIN;

	@Deprecated
	Circle addCircle(CircleOptions circleOptions);

	GroundOverlay addGroundOverlay(GroundOverlayOptions groundOverlayOptions);

	IMarker addMarker(ExtendedMarkerOptions markerOptions);

	Polygon addPolygon(PolygonOptions polygonOptions);

	Polyline addPolyline(PolylineOptions polylineOptions);

	TileOverlay addTileOverlay(TileOverlayOptions tileOverlayOptions);

	void animateCamera(CameraUpdate cameraUpdate, CancelableCallback cancelableCallback);

	void animateCamera(CameraUpdate cameraUpdate, int time, CancelableCallback cancelableCallback);

	void animateCamera(CameraUpdate cameraUpdate);

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

	void moveCamera(CameraUpdate cameraUpdate);

	void setBuildingsEnabled(boolean buildingsEnabled);

	void setClustering(ClusteringSettings clusteringSettings);

	boolean setIndoorEnabled(boolean indoorEnabled);

	void setInfoWindowAdapter(InfoWindowAdapter infoWindowAdapter);

	void setLocationSource(LocationSource locationSource);

	void setMapType(int mapType);

	void setMyLocationEnabled(boolean myLocationEnabled);

	void setOnCameraChangeListener(OnCameraChangeListener onCameraChangeListener);

	void setOnInfoWindowClickListener(OnInfoWindowClickListener onInfoWindowClickListener);

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

	interface CancelableCallback extends com.google.android.gms.maps.GoogleMap.CancelableCallback {

		@Override
		void onCancel();

		@Override
		void onFinish();
	}

	interface InfoWindowAdapter {

		View getInfoContents(IMarker marker);

		View getInfoWindow(IMarker marker);
	}

	interface OnCameraChangeListener extends com.google.android.gms.maps.GoogleMap.OnCameraChangeListener {

		@Override
		void onCameraChange(CameraPosition cameraPosition);
	}

	interface OnInfoWindowClickListener {

		void onInfoWindowClick(IMarker marker);
	}

	interface OnMapClickListener extends com.google.android.gms.maps.GoogleMap.OnMapClickListener {

		@Override
		void onMapClick(LatLng position);
	}

	interface OnMapLoadedCallback extends com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback {

		@Override
		void onMapLoaded();
	}

	interface OnMapLongClickListener extends com.google.android.gms.maps.GoogleMap.OnMapLongClickListener {

		@Override
		void onMapLongClick(LatLng position);
	}

	interface OnMarkerClickListener {

		boolean onMarkerClick(IMarker marker);
	}

	interface OnMarkerDragListener {

		void onMarkerDragStart(IMarker marker);

		void onMarkerDrag(IMarker marker);

		void onMarkerDragEnd(IMarker marker);
	}

	interface OnMyLocationButtonClickListener extends com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener {

		@Override
		boolean onMyLocationButtonClick();
	}

	interface SnapshotReadyCallback extends com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback {

		@Override
		void onSnapshotReady(Bitmap snapshot);
	}
}