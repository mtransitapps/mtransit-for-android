package org.mtransit.android.ui.view.map;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class PolylineOptions {

	public final com.google.android.gms.maps.model.PolylineOptions real = new com.google.android.gms.maps.model.PolylineOptions();
	private Object data;

	public PolylineOptions add(LatLng point) {
		real.add(point);
		return this;
	}

	public PolylineOptions add(LatLng... points) {
		real.add(points);
		return this;
	}

	public PolylineOptions addAll(Iterable<LatLng> points) {
		real.addAll(points);
		return this;
	}

	public PolylineOptions data(Object data) {
		this.data = data;
		return this;
	}

	public PolylineOptions color(int color) {
		real.color(color);
		return this;
	}

	public PolylineOptions geodesic(boolean geodesic) {
		real.geodesic(geodesic);
		return this;
	}

	public int getColor() {
		return real.getColor();
	}

	public Object getData() {
		return data;
	}

	public List<LatLng> getPoints() {
		return real.getPoints();
	}

	public float getWidth() {
		return real.getWidth();
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

	public PolylineOptions visible(boolean visible) {
		real.visible(visible);
		return this;
	}

	public PolylineOptions width(float width) {
		real.width(width);
		return this;
	}

	public PolylineOptions zIndex(float zIndex) {
		real.zIndex(zIndex);
		return this;
	}
}
