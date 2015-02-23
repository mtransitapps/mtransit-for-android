package org.mtransit.android.ui.view.map;

import com.google.android.gms.maps.model.LatLng;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class CircleOptions {

	public final com.google.android.gms.maps.model.CircleOptions real = new com.google.android.gms.maps.model.CircleOptions();
	private Object data;

	public CircleOptions center(LatLng center) {
		real.center(center);
		return this;
	}

	public CircleOptions data(Object data) {
		this.data = data;
		return this;
	}

	public CircleOptions fillColor(int color) {
		real.fillColor(color);
		return this;
	}

	public LatLng getCenter() {
		return real.getCenter();
	}

	public Object getData() {
		return data;
	}

	public int getFillColor() {
		return real.getFillColor();
	}

	public double getRadius() {
		return real.getRadius();
	}

	public int getStrokeColor() {
		return real.getStrokeColor();
	}

	public float getStrokeWidth() {
		return real.getStrokeWidth();
	}

	public float getZIndex() {
		return real.getZIndex();
	}

	public boolean isVisible() {
		return real.isVisible();
	}

	public CircleOptions radius(double radius) {
		real.radius(radius);
		return this;
	}

	public CircleOptions strokeColor(int color) {
		real.strokeColor(color);
		return this;
	}

	public CircleOptions strokeWidth(float width) {
		real.strokeWidth(width);
		return this;
	}

	public CircleOptions visible(boolean visible) {
		real.visible(visible);
		return this;
	}

	public CircleOptions zIndex(float zIndex) {
		real.zIndex(zIndex);
		return this;
	}
}
