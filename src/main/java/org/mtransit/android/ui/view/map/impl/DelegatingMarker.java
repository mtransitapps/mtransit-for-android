package org.mtransit.android.ui.view.map.impl;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

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
	public void animatePosition(LatLng target) {
		animatePosition(target, new AnimationSettings(), null);
	}

	@Override
	public void animatePosition(LatLng target, AnimationSettings settings) {
		animatePosition(target, settings, null);
	}

	@Override
	public void animatePosition(LatLng target, AnimationCallback callback) {
		animatePosition(target, new AnimationSettings(), callback);
	}

	@Override
	public void animatePosition(LatLng target, AnimationSettings settings, AnimationCallback callback) {
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
	@Override
	public Object getData() {
		return data;
	}

	@Deprecated
	@Override
	public String getId() {
		return real.getId();
	}

	@Override
	public List<IMarker> getMarkers() {
		return null;
	}

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
	public void setData(Object data) {
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
	public void setIcon(BitmapDescriptor icon) {
		real.setIcon(icon);
	}

	@Override
	public void setIcon(Context context, @DrawableRes Integer iconResId, @ColorInt Integer color, @ColorInt Integer secondaryColor, @ColorInt Integer defaultColor) {
		real.setIcon(context, iconResId, color, secondaryColor, defaultColor);
	}

	@Override
	public Integer getColor() {
		return this.real == null ? null : this.real.getColor();
	}

	@Override
	public Integer getSecondaryColor() {
		return this.real == null ? null : this.real.getSecondaryColor();
	}

	@Override
	public Integer getDefaultColor() {
		return this.real == null ? null : this.real.getDefaultColor();
	}

	@Override
	public void setInfoWindowAnchor(float anchorU, float anchorV) {
		real.setInfoWindowAnchor(anchorU, anchorV);
	}

	@Override
	public void setPosition(LatLng position) {
		this.position = position;
		real.setPosition(position);
		manager.onPositionChange(this);
	}

	void setPositionDuringAnimation(LatLng position) {
		this.position = position;
		real.setPosition(position);
		manager.onPositionDuringAnimationChange(this);
	}

	@Override
	public void setRotation(float rotation) {
		real.setRotation(rotation);
	}

	@Override
	public void setSnippet(String snippet) {
		real.setSnippet(snippet);
	}

	@Override
	public void setTitle(String title) {
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

	void setVirtualPosition(LatLng position) {
		real.setPosition(position);
	}
}
