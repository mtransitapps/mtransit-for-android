package org.mtransit.android.ui.view.map.impl;

import android.graphics.Point;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.VisibleRegion;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
interface IProjection {

	LatLng fromScreenLocation(Point point);

	VisibleRegion getVisibleRegion();

	Point toScreenLocation(LatLng location);

	Projection getProjection();
}