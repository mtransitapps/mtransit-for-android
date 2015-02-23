package org.mtransit.android.ui.view.map.impl;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import org.mtransit.android.ui.view.map.utils.SphericalMercator;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class DebugHelper {

	private List<Polyline> gridLines = new ArrayList<Polyline>();

	void drawDebugGrid(IGoogleMap map, double clusterSize) {
		cleanup();
		IProjection projection = map.getProjection();
		LatLngBounds bounds = projection.getVisibleRegion().latLngBounds;
		double minY = -180 + clusterSize * (int) (SphericalMercator.scaleLatitude(bounds.southwest.latitude) / clusterSize);
		double minX = -180 + clusterSize * (int) (SphericalMercator.scaleLongitude(bounds.southwest.longitude) / clusterSize);
		double maxY = -180 + clusterSize * (int) (SphericalMercator.scaleLatitude(bounds.northeast.latitude) / clusterSize);
		double maxX = -180 + clusterSize * (int) (SphericalMercator.scaleLongitude(bounds.northeast.longitude) / clusterSize);
		for (double y = minY; y <= maxY; y += clusterSize) {
			gridLines.add(map.addPolyline(new PolylineOptions().width(1.0f).add(new LatLng(SphericalMercator.toLatitude(y), bounds.southwest.longitude),
					new LatLng(SphericalMercator.toLatitude(y), bounds.northeast.longitude))));
		}
		if (minX <= maxX) {
			for (double x = minX; x <= maxX; x += clusterSize) {
				gridLines.add(map.addPolyline(new PolylineOptions().width(1.0f).add(new LatLng(bounds.southwest.latitude, x),
						new LatLng(bounds.northeast.latitude, x))));
			}
		} else {
			for (double x = -180; x <= minX; x += clusterSize) {
				gridLines.add(map.addPolyline(new PolylineOptions().width(1.0f).add(new LatLng(bounds.southwest.latitude, x),
						new LatLng(bounds.northeast.latitude, x))));
			}
			for (double x = maxX; x < 180; x += clusterSize) {
				gridLines.add(map.addPolyline(new PolylineOptions().width(1.0f).add(new LatLng(bounds.southwest.latitude, x),
						new LatLng(bounds.northeast.latitude, x))));
			}
		}
	}

	void cleanup() {
		for (Polyline polyline : gridLines) {
			polyline.remove();
		}
		gridLines.clear();
	}
}
