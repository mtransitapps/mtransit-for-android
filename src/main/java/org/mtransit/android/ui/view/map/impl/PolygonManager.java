package org.mtransit.android.ui.view.map.impl;

import androidx.collection.ArrayMap;

import org.mtransit.android.ui.view.map.Polygon;
import org.mtransit.android.ui.view.map.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class PolygonManager {

	private final IGoogleMap factory;

	private final ArrayMap<com.google.android.gms.maps.model.Polygon, Polygon> polygons;

	public PolygonManager(IGoogleMap factory) {
		this.factory = factory;
		this.polygons = new ArrayMap<>();
	}

	public Polygon addPolygon(PolygonOptions polygonOptions) {
		Polygon polygon = createPolygon(polygonOptions.real);
		polygon.setData(polygonOptions.getData());
		return polygon;
	}

	private Polygon createPolygon(com.google.android.gms.maps.model.PolygonOptions polygonOptions) {
		com.google.android.gms.maps.model.Polygon real = factory.addPolygon(polygonOptions);
		Polygon polygon = new DelegatingPolygon(real, this);
		polygons.put(real, polygon);
		return polygon;
	}

	public void clear() {
		polygons.clear();
	}

	public List<Polygon> getPolygons() {
		return new ArrayList<>(polygons.values());
	}

	public void onRemove(com.google.android.gms.maps.model.Polygon real) {
		polygons.remove(real);
	}
}
