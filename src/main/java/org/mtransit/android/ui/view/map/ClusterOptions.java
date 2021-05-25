package org.mtransit.android.ui.view.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.BitmapDescriptor;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class ClusterOptions {

	public static final float DEFAULT_ALPHA = 1.0f;
	public static final float DEFAULT_ANCHOR_U = 0.5f;
	public static final float DEFAULT_ANCHOR_V = 1.0f;
	public static final float DEFAULT_INFO_WINDOW_ANCHOR_U = 0.5f;
	public static final float DEFAULT_INFO_WINDOW_ANCHOR_V = 0.0f;

	private float alpha = DEFAULT_ALPHA;
	private float anchorU = DEFAULT_ANCHOR_U;
	private float anchorV = DEFAULT_ANCHOR_V;
	private boolean flat;
	private BitmapDescriptor icon;
	private float infoWindowAnchorU = DEFAULT_INFO_WINDOW_ANCHOR_U;
	private float infoWindowAnchorV = DEFAULT_INFO_WINDOW_ANCHOR_V;
	private float rotation;

	@NonNull
	public ClusterOptions alpha(float alpha) {
		this.alpha = alpha;
		return this;
	}

	@NonNull
	public ClusterOptions anchor(float anchorU, float anchorV) {
		this.anchorU = anchorU;
		this.anchorV = anchorV;
		return this;
	}

	@NonNull
	public ClusterOptions flat(boolean flat) {
		this.flat = flat;
		return this;
	}

	public float getAlpha() {
		return alpha;
	}

	public float getAnchorU() {
		return anchorU;
	}

	public float getAnchorV() {
		return anchorV;
	}

	@Nullable
	public BitmapDescriptor getIcon() {
		return icon;
	}

	public float getInfoWindowAnchorU() {
		return infoWindowAnchorU;
	}

	public float getInfoWindowAnchorV() {
		return infoWindowAnchorV;
	}

	public float getRotation() {
		return rotation;
	}

	@NonNull
	public ClusterOptions icon(@Nullable BitmapDescriptor icon) {
		this.icon = icon;
		return this;
	}

	@NonNull
	public ClusterOptions infoWindowAnchor(float u, float v) {
		this.infoWindowAnchorU = u;
		this.infoWindowAnchorV = v;
		return this;
	}

	public boolean isFlat() {
		return flat;
	}

	@NonNull
	public ClusterOptions rotation(float rotation) {
		this.rotation = rotation;
		return this;
	}
}
