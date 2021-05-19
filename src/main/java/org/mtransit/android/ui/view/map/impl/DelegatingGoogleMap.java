package org.mtransit.android.ui.view.map.impl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MapStyleOptions;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.map.Circle;
import org.mtransit.android.ui.view.map.CircleOptions;
import org.mtransit.android.ui.view.map.ClusteringSettings;
import org.mtransit.android.ui.view.map.ExtendedGoogleMap;
import org.mtransit.android.ui.view.map.ExtendedMarkerOptions;
import org.mtransit.android.ui.view.map.GroundOverlay;
import org.mtransit.android.ui.view.map.GroundOverlayOptions;
import org.mtransit.android.ui.view.map.IMarker;
import org.mtransit.android.ui.view.map.MTClusterOptionsProvider;
import org.mtransit.android.ui.view.map.Polygon;
import org.mtransit.android.ui.view.map.PolygonOptions;
import org.mtransit.android.ui.view.map.Polyline;
import org.mtransit.android.ui.view.map.PolylineOptions;
import org.mtransit.android.ui.view.map.TileOverlay;
import org.mtransit.android.ui.view.map.TileOverlayOptions;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class DelegatingGoogleMap implements ExtendedGoogleMap, MTLog.Loggable {

	private static final String TAG = DelegatingGoogleMap.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private final IGoogleMap real;
	private final Context context;

	private InfoWindowAdapter infoWindowAdapter;
	private OnCameraChangeListener onCameraChangeListener;
	private OnMarkerDragListener onMarkerDragListener;

	private MarkerManager markerManager;
	private PolylineManager polylineManager;
	private PolygonManager polygonManager;
	private CircleManager circleManager;
	private GroundOverlayManager groundOverlayManager;
	private TileOverlayManager tileOverlayManager;

	DelegatingGoogleMap(IGoogleMap real, Context context) {
		this.real = real;
		this.context = context;
		createManagers();
		assignMapListeners();
	}

	@Deprecated
	@Override
	public Circle addCircle(CircleOptions circleOptions) {
		return circleManager.addCircle(circleOptions);
	}

	@Override
	public GroundOverlay addGroundOverlay(GroundOverlayOptions groundOverlayOptions) {
		return groundOverlayManager.addGroundOverlay(groundOverlayOptions);
	}

	@Override
	public IMarker addMarker(ExtendedMarkerOptions markerOptions) {
		return markerManager.addMarker(markerOptions);
	}

	@Override
	public Polygon addPolygon(PolygonOptions polygonOptions) {
		return polygonManager.addPolygon(polygonOptions);
	}

	@Override
	public Polyline addPolyline(PolylineOptions polylineOptions) {
		return polylineManager.addPolyline(polylineOptions);
	}

	@Override
	public TileOverlay addTileOverlay(TileOverlayOptions tileOverlayOptions) {
		return tileOverlayManager.addTileOverlay(tileOverlayOptions);
	}

	@Override
	public void animateCamera(CameraUpdate cameraUpdate, CancelableCallback cancelableCallback) {
		real.animateCamera(cameraUpdate, cancelableCallback);
	}

	@Override
	public void animateCamera(CameraUpdate cameraUpdate, int time, CancelableCallback cancelableCallback) {
		real.animateCamera(cameraUpdate, time, cancelableCallback);
	}

	@Override
	public void animateCamera(CameraUpdate cameraUpdate) {
		real.animateCamera(cameraUpdate);
	}

	@Override
	public void clear() {
		real.clear();
		clearManagers();
	}

	@Override
	public CameraPosition getCameraPosition() {
		return real.getCameraPosition();
	}

	@Override
	public List<IMarker> getDisplayedMarkers() {
		return markerManager.getDisplayedMarkers();
	}

	@Override
	public int getMapType() {
		return real.getMapType();
	}

	@Override
	public List<Circle> getCircles() {
		return circleManager.getCircles();
	}

	@Override
	public List<GroundOverlay> getGroundOverlays() {
		return groundOverlayManager.getGroundOverlays();
	}

	@Override
	public List<IMarker> getMarkers() {
		return markerManager.getMarkers();
	}

	@Override
	public IMarker getMarkerShowingInfoWindow() {
		return markerManager.getMarkerShowingInfoWindow();
	}

	@Override
	public List<Polygon> getPolygons() {
		return polygonManager.getPolygons();
	}

	@Override
	public List<Polyline> getPolylines() {
		return polylineManager.getPolylines();
	}

	@Override
	public List<TileOverlay> getTileOverlays() {
		return tileOverlayManager.getTileOverlays();
	}

	@Override
	public float getMaxZoomLevel() {
		return real.getMaxZoomLevel();
	}

	@Override
	public float getMinZoomLevel() {
		return real.getMinZoomLevel();
	}

	@Override
	public float getMinZoomLevelNotClustered(IMarker marker) {
		return markerManager.getMinZoomLevelNotClustered(marker);
	}

	@Override
	public Projection getProjection() {
		return real.getProjection().getProjection();
	}

	@Override
	public UiSettings getUiSettings() {
		return real.getUiSettings();
	}

	@Override
	public boolean isBuildingsEnabled() {
		return real.isBuildingsEnabled();
	}

	@Override
	public boolean isIndoorEnabled() {
		return real.isIndoorEnabled();
	}

	@Override
	public boolean isMyLocationEnabled() {
		return real.isMyLocationEnabled();
	}

	@Override
	public boolean isTrafficEnabled() {
		return real.isTrafficEnabled();
	}

	@Override
	public void moveCamera(CameraUpdate cameraUpdate) {
		real.moveCamera(cameraUpdate);
	}

	@Override
	public void setBuildingsEnabled(boolean buildingsEnabled) {
		real.setBuildingsEnabled(buildingsEnabled);
	}

	@Override
	public void setClustering(ClusteringSettings clusteringSettings) {
		if (clusteringSettings != null && clusteringSettings.isEnabled() && clusteringSettings.getClusterOptionsProvider() == null) {
			clusteringSettings.clusterOptionsProvider(new MTClusterOptionsProvider(context));
		}
		markerManager.setClustering(clusteringSettings);
	}

	@Override
	public boolean setIndoorEnabled(boolean indoorEnabled) {
		return real.setIndoorEnabled(indoorEnabled);
	}

	@Override
	public void setInfoWindowAdapter(final InfoWindowAdapter infoWindowAdapter) {
		this.infoWindowAdapter = infoWindowAdapter;
	}

	@Override
	public void setLocationSource(LocationSource locationSource) {
		real.setLocationSource(locationSource);
	}

	@Override
	public void setMapType(int mapType) {
		real.setMapType(mapType);
	}

	@Override
	public void setMapStyle(@Nullable MapStyleOptions mapStyleOptions) {
		real.setMapStyle(mapStyleOptions);
	}

	@SuppressLint("MissingPermission")
	@RequiresPermission(
			anyOf = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}
	)
	@Override
	public void setMyLocationEnabled(boolean myLocationEnabled) {
		real.setMyLocationEnabled(myLocationEnabled);
	}

	@Override
	public void setOnCameraChangeListener(OnCameraChangeListener onCameraChangeListener) {
		this.onCameraChangeListener = onCameraChangeListener;
	}

	@Override
	public void setOnInfoWindowClickListener(OnInfoWindowClickListener onInfoWindowClickListener) {
		com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener realOnInfoWindowClickListener = null;
		if (onInfoWindowClickListener != null) {
			realOnInfoWindowClickListener = new DelegatingOnInfoWindowClickListener(onInfoWindowClickListener);
		}
		real.setOnInfoWindowClickListener(realOnInfoWindowClickListener);
	}

	@Override
	public void setOnMapClickListener(OnMapClickListener onMapClickListener) {
		real.setOnMapClickListener(onMapClickListener);
	}

	@Override
	public void setOnMapLoadedCallback(OnMapLoadedCallback onMapLoadedCallback) {
		real.setOnMapLoadedCallback(onMapLoadedCallback);
	}

	@Override
	public void setOnMapLongClickListener(OnMapLongClickListener onMapLongClickListener) {
		real.setOnMapLongClickListener(onMapLongClickListener);
	}

	@Override
	public void setOnMarkerClickListener(OnMarkerClickListener onMarkerClickListener) {
		com.google.android.gms.maps.GoogleMap.OnMarkerClickListener realOnMarkerClickListener = null;
		if (onMarkerClickListener != null) {
			realOnMarkerClickListener = new DelegatingOnMarkerClickListener(onMarkerClickListener);
		}
		real.setOnMarkerClickListener(realOnMarkerClickListener);
	}

	@Override
	public void setOnMarkerDragListener(OnMarkerDragListener onMarkerDragListener) {
		this.onMarkerDragListener = onMarkerDragListener;
	}

	@Override
	public void setOnMyLocationButtonClickListener(OnMyLocationButtonClickListener listener) {
		real.setOnMyLocationButtonClickListener(listener);
	}

	@Override
	public void setPadding(int left, int top, int right, int bottom) {
		real.setPadding(left, top, right, bottom);
	}

	@Override
	public void setTrafficEnabled(boolean trafficEnabled) {
		real.setTrafficEnabled(trafficEnabled);
	}

	@Override
	public void snapshot(SnapshotReadyCallback callback) {
		real.snapshot(callback);
	}

	@Override
	public void snapshot(SnapshotReadyCallback callback, Bitmap bitmap) {
		real.snapshot(callback, bitmap);
	}

	@Override
	public void stopAnimation() {
		real.stopAnimation();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DelegatingGoogleMap)) {
			return false;
		}
		DelegatingGoogleMap other = (DelegatingGoogleMap) o;
		return real.equals(other.real);
	}

	@Override
	public int hashCode() {
		return real.hashCode();
	}

	@NonNull
	@Override
	public String toString() {
		return real.toString();
	}

	private void createManagers() {
		markerManager = new MarkerManager(this.real);
		polylineManager = new PolylineManager(this.real);
		polygonManager = new PolygonManager(this.real);
		circleManager = new CircleManager(this.real);
		groundOverlayManager = new GroundOverlayManager(this.real);
		tileOverlayManager = new TileOverlayManager(this.real);
	}

	private void clearManagers() {
		markerManager.clear();
		polylineManager.clear();
		polygonManager.clear();
		circleManager.clear();
		groundOverlayManager.clear();
		tileOverlayManager.clear();
	}

	private void assignMapListeners() {
		real.setInfoWindowAdapter(new DelegatingInfoWindowAdapter());
		//noinspection deprecation // FIXME
		real.setOnCameraChangeListener(new DelegatingOnCameraChangeListener());
		real.setOnMarkerDragListener(new DelegatingOnMarkerDragListener());
	}

	@SuppressWarnings("deprecation") // FIXME
	private class DelegatingOnCameraChangeListener implements com.google.android.gms.maps.GoogleMap.OnCameraChangeListener {

		@Override
		public void onCameraChange(@NonNull CameraPosition cameraPosition) {
			markerManager.onCameraChange(cameraPosition);
			if (onCameraChangeListener != null) {
				onCameraChangeListener.onCameraChange(cameraPosition);
			}
		}
	}

	private class DelegatingInfoWindowAdapter implements com.google.android.gms.maps.GoogleMap.InfoWindowAdapter, MTLog.Loggable {

		private final String TAG = DelegatingGoogleMap.this.getLogTag() + ">" + DelegatingInfoWindowAdapter.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		@Override
		public View getInfoWindow(@NonNull com.google.android.gms.maps.model.Marker marker) {
			IMarker mapped = markerManager.map(marker);
			markerManager.setMarkerShowingInfoWindow(mapped);
			if (infoWindowAdapter != null) {
				return infoWindowAdapter.getInfoWindow(mapped);
			}
			return null;
		}

		@Override
		public View getInfoContents(@NonNull com.google.android.gms.maps.model.Marker marker) {
			if (infoWindowAdapter != null) {
				return infoWindowAdapter.getInfoContents(markerManager.map(marker));
			}
			return null;
		}
	}

	private class DelegatingOnInfoWindowClickListener implements com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener, MTLog.Loggable {

		private final String TAG = DelegatingGoogleMap.this.getLogTag() + ">" + DelegatingOnInfoWindowClickListener.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		private final OnInfoWindowClickListener onInfoWindowClickListener;

		DelegatingOnInfoWindowClickListener(OnInfoWindowClickListener onInfoWindowClickListener) {
			this.onInfoWindowClickListener = onInfoWindowClickListener;
		}

		@Override
		public void onInfoWindowClick(@NonNull com.google.android.gms.maps.model.Marker marker) {
			IMarker imarker = markerManager.map(marker);
			onInfoWindowClickListener.onInfoWindowClick(imarker);
		}
	}

	private class DelegatingOnMarkerClickListener implements com.google.android.gms.maps.GoogleMap.OnMarkerClickListener, MTLog.Loggable {

		private final String TAG = DelegatingGoogleMap.this.getLogTag() + ">" + DelegatingOnMarkerClickListener.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		private final OnMarkerClickListener onMarkerClickListener;

		DelegatingOnMarkerClickListener(OnMarkerClickListener onMarkerClickListener) {
			this.onMarkerClickListener = onMarkerClickListener;
		}

		@Override
		public boolean onMarkerClick(@NonNull com.google.android.gms.maps.model.Marker marker) {
			return onMarkerClickListener.onMarkerClick(markerManager.map(marker));
		}
	}

	private class DelegatingOnMarkerDragListener implements com.google.android.gms.maps.GoogleMap.OnMarkerDragListener {

		@Override
		public void onMarkerDragStart(@NonNull com.google.android.gms.maps.model.Marker marker) {
			DelegatingMarker delegating = markerManager.mapToDelegatingMarker(marker);
			delegating.clearCachedPosition();
			markerManager.onDragStart(delegating);
			if (onMarkerDragListener != null) {
				onMarkerDragListener.onMarkerDragStart(delegating);
			}
		}

		@Override
		public void onMarkerDrag(@NonNull com.google.android.gms.maps.model.Marker marker) {
			DelegatingMarker delegating = markerManager.mapToDelegatingMarker(marker);
			delegating.clearCachedPosition();
			if (onMarkerDragListener != null) {
				onMarkerDragListener.onMarkerDrag(delegating);
			}
		}

		@Override
		public void onMarkerDragEnd(@NonNull com.google.android.gms.maps.model.Marker marker) {
			DelegatingMarker delegating = markerManager.mapToDelegatingMarker(marker);
			delegating.clearCachedPosition();
			markerManager.onPositionChange(delegating);
			if (onMarkerDragListener != null) {
				onMarkerDragListener.onMarkerDragEnd(delegating);
			}
		}
	}
}
