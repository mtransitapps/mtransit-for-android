package org.mtransit.android.ui.view.map;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class GroundOverlayOptions {

	public final com.google.android.gms.maps.model.GroundOverlayOptions real = new com.google.android.gms.maps.model.GroundOverlayOptions();
	private Object data;

	public GroundOverlayOptions anchor(float u, float v) {
		real.anchor(u, v);
		return this;
	}

	public GroundOverlayOptions bearing(float bearing) {
		real.bearing(bearing);
		return this;
	}

	public GroundOverlayOptions data(Object data) {
		this.data = data;
		return this;
	}

	public float getAnchorU() {
		return real.getAnchorU();
	}

	public float getAnchorV() {
		return real.getAnchorV();
	}

	public float getBearing() {
		return real.getBearing();
	}

	public LatLngBounds getBounds() {
		return real.getBounds();
	}

	public Object getData() {
		return data;
	}

	public float getHeight() {
		return real.getHeight();
	}

	public BitmapDescriptor getImage() {
		return real.getImage();
	}

	public LatLng getLocation() {
		return real.getLocation();
	}

	public float getTransparency() {
		return real.getTransparency();
	}

	public float getWidth() {
		return real.getWidth();
	}

	public float getZIndex() {
		return real.getZIndex();
	}

	public GroundOverlayOptions image(BitmapDescriptor image) {
		real.image(image);
		return this;
	}

	public boolean isVisible() {
		return real.isVisible();
	}

	public GroundOverlayOptions position(LatLng location, float width) {
		real.position(location, width);
		return this;
	}

	public GroundOverlayOptions position(LatLng location, float width, float height) {
		real.position(location, width, height);
		return this;
	}

	public GroundOverlayOptions positionFromBounds(LatLngBounds bounds) {
		real.positionFromBounds(bounds);
		return this;
	}

	public GroundOverlayOptions transparency(float transparency) {
		real.transparency(transparency);
		return this;
	}

	public GroundOverlayOptions visible(boolean visible) {
		real.visible(visible);
		return this;
	}

	public GroundOverlayOptions zIndex(float zIndex) {
		real.zIndex(zIndex);
		return this;
	}
}
