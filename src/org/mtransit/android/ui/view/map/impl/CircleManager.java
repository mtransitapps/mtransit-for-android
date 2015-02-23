package org.mtransit.android.ui.view.map.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mtransit.android.ui.view.map.Circle;
import org.mtransit.android.ui.view.map.CircleOptions;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class CircleManager {

	private final IGoogleMap factory;

	private final Map<com.google.android.gms.maps.model.Circle, Circle> circles;

	public CircleManager(IGoogleMap factory) {
		this.factory = factory;
		this.circles = new HashMap<com.google.android.gms.maps.model.Circle, Circle>();
	}

	public Circle addCircle(CircleOptions circleOptions) {
		Circle circle = createCircle(circleOptions.real);
		circle.setData(circleOptions.getData());
		return circle;
	}

	private Circle createCircle(com.google.android.gms.maps.model.CircleOptions circleOptions) {
		com.google.android.gms.maps.model.Circle real = factory.addCircle(circleOptions);
		Circle circle = new DelegatingCircle(real, this);
		circles.put(real, circle);
		return circle;
	}

	public void clear() {
		circles.clear();
	}

	public List<Circle> getCircles() {
		return new ArrayList<Circle>(circles.values());
	}

	public void onRemove(com.google.android.gms.maps.model.Circle real) {
		circles.remove(real);
	}
}
