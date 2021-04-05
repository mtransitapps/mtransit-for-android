package org.mtransit.android.ui.view.map;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
@SuppressWarnings("unused")
public class ExtendedMarkerOptions {

	@NonNull
	private final com.google.android.gms.maps.model.MarkerOptions real = new com.google.android.gms.maps.model.MarkerOptions();
	@Nullable
	private WeakReference<Context> realContextWR;
	@DrawableRes
	private Integer realIconResId;
	private Integer realColor;
	private Integer realSecondaryColor;
	private Integer realDefaultColor;
	private Object data;
	private int clusterGroup;

	@NonNull
	public com.google.android.gms.maps.model.MarkerOptions getReal() {
		return real;
	}

	@Nullable
	public Context getContext() {
		return this.realContextWR == null ? null : this.realContextWR.get();
	}

	@DrawableRes
	public Integer getIconResId() {
		return this.realIconResId;
	}

	@ColorInt
	public Integer getColor() {
		return this.realColor;
	}

	@ColorInt
	public Integer getSecondaryColor() {
		return this.realSecondaryColor;
	}

	@ColorInt
	public Integer getDefaultColor() {
		return this.realDefaultColor;
	}

	@NonNull
	public ExtendedMarkerOptions alpha(float alpha) {
		real.alpha(alpha);
		return this;
	}

	@NonNull
	public ExtendedMarkerOptions anchor(float u, float v) {
		real.anchor(u, v);
		return this;
	}

	@NonNull
	public ExtendedMarkerOptions clusterGroup(int clusterGroup) {
		this.clusterGroup = clusterGroup;
		return this;
	}

	@NonNull
	public ExtendedMarkerOptions data(Object data) {
		this.data = data;
		return this;
	}

	@NonNull
	public ExtendedMarkerOptions draggable(boolean draggable) {
		real.draggable(draggable);
		return this;
	}

	@NonNull
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

	@NonNull
	public ExtendedMarkerOptions icon(BitmapDescriptor icon) {
		real.icon(icon);
		return this;
	}

	@NonNull
	public ExtendedMarkerOptions icon(Context context, @DrawableRes int iconResId, @ColorInt Integer color, @ColorInt Integer secondaryColor, @ColorInt int defaultColor) {
		real.icon(null);
		realContextWR = new WeakReference<>(context);
		realIconResId = iconResId;
		realColor = color;
		realSecondaryColor = secondaryColor;
		realDefaultColor = defaultColor;
		return this;
	}

	@NonNull
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

	@NonNull
	public ExtendedMarkerOptions position(LatLng position) {
		real.position(position);
		return this;
	}

	@NonNull
	public ExtendedMarkerOptions rotation(float rotation) {
		real.rotation(rotation);
		return this;
	}

	@NonNull
	public ExtendedMarkerOptions snippet(String snippet) {
		real.snippet(snippet);
		return this;
	}

	@NonNull
	public ExtendedMarkerOptions title(String title) {
		real.title(title);
		return this;
	}

	@NonNull
	public ExtendedMarkerOptions visible(boolean visible) {
		real.visible(visible);
		return this;
	}
}
