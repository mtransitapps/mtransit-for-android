package org.mtransit.android.ui.view.map.impl;

import android.graphics.Point;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.VisibleRegion;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class ProjectionWrapper implements IProjection {

	private Projection projection;

	public ProjectionWrapper(Projection projection) {
		this.projection = projection;
	}

	@Override
	public LatLng fromScreenLocation(Point point) {
		return projection.fromScreenLocation(point);
	}

	@Override
	public VisibleRegion getVisibleRegion() {
		return projection.getVisibleRegion();
	}

	@Override
	public Point toScreenLocation(LatLng location) {
		return projection.toScreenLocation(location);
	}

	@Override
	public Projection getProjection() {
		return projection;
	}
}
