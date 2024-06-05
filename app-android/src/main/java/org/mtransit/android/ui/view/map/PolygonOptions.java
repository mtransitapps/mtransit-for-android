package org.mtransit.android.ui.view.map;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class PolygonOptions {

	public final com.google.android.gms.maps.model.PolygonOptions real = new com.google.android.gms.maps.model.PolygonOptions();
	private Object data;

	public PolygonOptions add(LatLng point) {
		real.add(point);
		return this;
	}

	public PolygonOptions add(LatLng... points) {
		real.add(points);
		return this;
	}

	public PolygonOptions addAll(Iterable<LatLng> points) {
		real.addAll(points);
		return this;
	}

	public PolygonOptions addHole(Iterable<LatLng> points) {
		real.addHole(points);
		return this;
	}

	public PolygonOptions data(Object data) {
		this.data = data;
		return this;
	}

	public PolygonOptions fillColor(int color) {
		real.fillColor(color);
		return this;
	}

	public PolygonOptions geodesic(boolean geodesic) {
		real.geodesic(geodesic);
		return this;
	}

	public Object getData() {
		return data;
	}

	public int getFillColor() {
		return real.getFillColor();
	}

	public List<List<LatLng>> getHoles() {
		return real.getHoles();
	}

	public List<LatLng> getPoints() {
		return real.getPoints();
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

	public boolean isGeodesic() {
		return real.isGeodesic();
	}

	public boolean isVisible() {
		return real.isVisible();
	}

	public PolygonOptions strokeColor(int color) {
		real.strokeColor(color);
		return this;
	}

	public PolygonOptions strokeWidth(float width) {
		real.strokeWidth(width);
		return this;
	}

	public PolygonOptions visible(boolean visible) {
		real.visible(visible);
		return this;
	}

	public PolygonOptions zIndex(float zIndex) {
		real.zIndex(zIndex);
		return this;
	}
}
