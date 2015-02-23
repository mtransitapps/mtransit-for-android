package org.mtransit.android.ui.view.map;

import java.lang.ref.WeakReference;

import android.content.Context;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class ExtendedMarkerOptions {

	private final com.google.android.gms.maps.model.MarkerOptions real = new com.google.android.gms.maps.model.MarkerOptions();
	private WeakReference<Context> realContextWR;
	private Integer realIconResId;
	private Integer realColor;
	private Integer realSecondaryColor;
	private Integer realDefaultColor;
	private Object data;
	private int clusterGroup;

	public com.google.android.gms.maps.model.MarkerOptions getReal() {
		return real;
	}

	public Context getContext() {
		return this.realContextWR == null ? null : this.realContextWR.get();
	}

	public Integer getIconResId() {
		return this.realIconResId;
	}

	public Integer getColor() {
		return this.realColor;
	}

	public Integer getSecondaryColor() {
		return this.realSecondaryColor;
	}

	public Integer getDefaultColor() {
		return this.realDefaultColor;
	}

	public ExtendedMarkerOptions alpha(float alpha) {
		real.alpha(alpha);
		return this;
	}

	public ExtendedMarkerOptions anchor(float u, float v) {
		real.anchor(u, v);
		return this;
	}

	public ExtendedMarkerOptions clusterGroup(int clusterGroup) {
		this.clusterGroup = clusterGroup;
		return this;
	}

	public ExtendedMarkerOptions data(Object data) {
		this.data = data;
		return this;
	}

	public ExtendedMarkerOptions draggable(boolean draggable) {
		real.draggable(draggable);
		return this;
	}

	public ExtendedMarkerOptions flat(boolean flat) {
		real.flat(flat);
		return this;
	}

	public float getAlpha() {
		return real.getAlpha();
	}

	public float getAnchorU() {
		return real.getAnchorU();
	}

	public float getAnchorV() {
		return real.getAnchorV();
	}

	public int getClusterGroup() {
		return clusterGroup;
	}

	public Object getData() {
		return data;
	}

	public BitmapDescriptor getIcon() {
		return real.getIcon();
	}

	public float getInfoWindowAnchorU() {
		return real.getInfoWindowAnchorU();
	}

	public float getInfoWindowAnchorV() {
		return real.getInfoWindowAnchorV();
	}

	public LatLng getPosition() {
		return real.getPosition();
	}

	public float getRotation() {
		return real.getRotation();
	}

	public String getSnippet() {
		return real.getSnippet();
	}

	public String getTitle() {
		return real.getTitle();
	}

	public ExtendedMarkerOptions icon(BitmapDescriptor icon) {
		real.icon(icon);
		return this;
	}

	public ExtendedMarkerOptions icon(Context context, int iconResId, Integer color, Integer secondaryColor, int defaultColor) {
		real.icon(null);
		realContextWR = new WeakReference<Context>(context);
		realIconResId = iconResId;
		realColor = color;
		realSecondaryColor = secondaryColor;
		realDefaultColor = defaultColor;
		return this;
	}

	public ExtendedMarkerOptions infoWindowAnchor(float u, float v) {
		real.infoWindowAnchor(u, v);
		return this;
	}

	public boolean isDraggable() {
		return real.isDraggable();
	}

	public boolean isFlat() {
		return real.isFlat();
	}

	public boolean isVisible() {
		return real.isVisible();
	}

	public ExtendedMarkerOptions position(LatLng position) {
		real.position(position);
		return this;
	}

	public ExtendedMarkerOptions rotation(float rotation) {
		real.rotation(rotation);
		return this;
	}

	public ExtendedMarkerOptions snippet(String snippet) {
		real.snippet(snippet);
		return this;
	}

	public ExtendedMarkerOptions title(String title) {
		real.title(title);
		return this;
	}

	public ExtendedMarkerOptions visible(boolean visible) {
		real.visible(visible);
		return this;
	}
}
