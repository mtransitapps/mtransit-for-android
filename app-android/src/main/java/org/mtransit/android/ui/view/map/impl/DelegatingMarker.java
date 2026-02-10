package org.mtransit.android.ui.view.map.impl;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.map.AnimationSettings;
import org.mtransit.android.ui.view.map.IMarker;
import org.mtransit.android.ui.view.map.lazy.LazyMarker;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class DelegatingMarker implements IMarker, MTLog.Loggable {

	private static final String LOG_TAG = DelegatingMarker.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private final LazyMarker real;
	private final MarkerManager manager;

	private int clusterGroup;
	@Nullable
	private Object data;

	private LatLng position;
	private boolean visible;

	DelegatingMarker(LazyMarker real, MarkerManager manager) {
		this.real = real;
		this.manager = manager;

		this.position = real.getPosition();
		this.visible = real.isVisible();
	}

	@Override
	public void animatePosition(@Nullable LatLng target) {
		animatePosition(target, new AnimationSettings(), null);
	}

	@Override
	public void animatePosition(@Nullable LatLng target, @Nullable AnimationSettings settings) {
		animatePosition(target, settings, null);
	}

	@Override
	public void animatePosition(@Nullable LatLng target, @Nullable AnimationCallback callback) {
		animatePosition(target, new AnimationSettings(), callback);
	}

	@Override
	public void animatePosition(@Nullable LatLng target, @Nullable AnimationSettings settings, @Nullable AnimationCallback callback) {
		if (target == null || settings == null) {
			throw new IllegalArgumentException();
		}
		manager.onAnimateMarkerPosition(this, target, settings, callback);
	}

	@Override
	public float getAlpha() {
		return real.getAlpha();
	}

	@Override
	public int getClusterGroup() {
		return clusterGroup;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public Object getData() {
		return data;
	}

	@Deprecated
	@NonNull
	@Override
	public String getId() {
		return real.getId();
	}

	@Nullable
	@Override
	public List<IMarker> getMarkers() {
		return null;
	}

	@NonNull
	@Override
	public LatLng getPosition() {
		if (position == null) {
			position = real.getPosition();
		}
		return position;
	}

	@Override
	public float getRotation() {
		return real.getRotation();
	}

	@Override
	public float getZIndex() {
		return real.getZIndex();
	}

	@Override
	public String getSnippet() {
		return real.getSnippet();
	}

	@Override
	public String getTitle() {
		return real.getTitle();
	}

	@Override
	public void hideInfoWindow() {
		real.hideInfoWindow();
	}

	@Override
	public boolean isCluster() {
		return false;
	}

	@Override
	public boolean isDraggable() {
		return real.isDraggable();
	}

	@Override
	public boolean isFlat() {
		return real.isFlat();
	}

	@Override
	public boolean isInfoWindowShown() {
		return real.isInfoWindowShown();
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public void remove() {
		manager.onRemove(this);
		real.remove();
	}

	@Override
	public void setAlpha(float alpha) {
		real.setAlpha(alpha);
	}

	@Override
	public void setAnchor(float anchorU, float anchorV) {
		real.setAnchor(anchorU, anchorV);
	}

	@Override
	public void setClusterGroup(int clusterGroup) {
		if (this.clusterGroup != clusterGroup) {
			this.clusterGroup = clusterGroup;
			manager.onClusterGroupChange(this);
		}
	}

	@Override
	public void setData(@Nullable Object data) {
		this.data = data;
	}

	@Override
	public void setDraggable(boolean draggable) {
		real.setDraggable(draggable);
	}

	@Override
	public void setFlat(boolean flat) {
		real.setFlat(flat);
	}

	@Deprecated
	@Override
	public void setIcon(@Nullable BitmapDescriptor icon) {
		real.setIcon(icon);
	}

	@Override
	public void setIcon(@Nullable Context context,
						@DrawableRes @Nullable Integer iconResId,
						@Nullable Integer targetSize,
						@Nullable Boolean replaceColor,
						@ColorInt @Nullable Integer color,
						@ColorInt @Nullable Integer secondaryColor,
						@ColorInt @Nullable Integer defaultColor) {
		real.setIcon(context, iconResId, targetSize, replaceColor, color, secondaryColor, defaultColor);
	}

	@Nullable
	@Override
	public Integer getColor() {
		return this.real == null ? null : this.real.getColor();
	}

	@Nullable
	@Override
	public Integer getSecondaryColor() {
		return this.real == null ? null : this.real.getSecondaryColor();
	}

	@Nullable
	@Override
	public Integer getDefaultColor() {
		return this.real == null ? null : this.real.getDefaultColor();
	}

	@Override
	public void setInfoWindowAnchor(float anchorU, float anchorV) {
		real.setInfoWindowAnchor(anchorU, anchorV);
	}

	@Override
	public void setPosition(@NonNull LatLng position) {
		this.position = position;
		real.setPosition(position);
		manager.onPositionChange(this);
	}

	void setPositionDuringAnimation(@NonNull LatLng position) {
		this.position = position;
		real.setPosition(position);
		manager.onPositionDuringAnimationChange(this);
	}

	@Override
	public void setRotation(float rotation) {
		real.setRotation(rotation);
	}

	@Override
	public void setZIndex(float zIndex) {
		real.setZIndex(zIndex);
	}

	@Override
	public void setSnippet(@Nullable String snippet) {
		real.setSnippet(snippet);
	}

	@Override
	public void setTitle(@Nullable String title) {
		real.setTitle(title);
	}

	@Override
	public void setVisible(boolean visible) {
		if (this.visible != visible) {
			this.visible = visible;
			manager.onVisibilityChangeRequest(this, visible);
		}
	}

	@Override
	public void showInfoWindow() {
		manager.onShowInfoWindow(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DelegatingMarker)) {
			return false;
		}
		DelegatingMarker other = (DelegatingMarker) o;
		return real.equals(other.real);
	}

	@Override
	public int hashCode() {
		return real.hashCode();
	}

	@NonNull
	@Override
	public String toString() {
		return real.toString();
	}

	LazyMarker getReal() {
		return real;
	}

	void changeVisible(boolean visible) {
		real.setVisible(this.visible && visible);
	}

	void clearCachedPosition() {
		position = null;
	}

	void forceShowInfoWindow() {
		real.showInfoWindow();
	}

	void setVirtualPosition(@NonNull LatLng position) {
		real.setPosition(position);
	}
}
