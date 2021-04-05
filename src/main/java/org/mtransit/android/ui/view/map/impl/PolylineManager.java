package org.mtransit.android.ui.view.map.impl;

import androidx.collection.ArrayMap;

import org.mtransit.android.ui.view.map.Polyline;
import org.mtransit.android.ui.view.map.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class PolylineManager {

	private final IGoogleMap factory;

	private final ArrayMap<com.google.android.gms.maps.model.Polyline, Polyline> polylines;

	public PolylineManager(IGoogleMap factory) {
		this.factory = factory;
		this.polylines = new ArrayMap<>();
	}

	public Polyline addPolyline(PolylineOptions polylineOptions) {
		Polyline polyline = createPolyline(polylineOptions.real);
		polyline.setData(polylineOptions.getData());
		return polyline;
	}

	private Polyline createPolyline(com.google.android.gms.maps.model.PolylineOptions polylineOptions) {
		com.google.android.gms.maps.model.Polyline real = factory.addPolyline(polylineOptions);
		Polyline polyline = new DelegatingPolyline(real, this);
		polylines.put(real, polyline);
		return polyline;
	}

	public void clear() {
		polylines.clear();
	}

	public List<Polyline> getPolylines() {
		return new ArrayList<>(polylines.values());
	}

	public void onRemove(com.google.android.gms.maps.model.Polyline real) {
		polylines.remove(real);
	}
}
