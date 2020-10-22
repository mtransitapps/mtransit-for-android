package org.mtransit.android.ui.view.map.impl;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.List;

import org.mtransit.android.ui.view.map.AnimationSettings;
import org.mtransit.android.ui.view.map.IMarker;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import android.content.Context;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class ClusterMarker implements IMarker {

	private int lastCount = -1;

	private GridClusteringStrategy strategy;

	private com.google.android.gms.maps.model.Marker virtual;

	private List<DelegatingMarker> markers = new ArrayList<>();

	public ClusterMarker(GridClusteringStrategy strategy) {
		this.strategy = strategy;
	}

	com.google.android.gms.maps.model.Marker getVirtual() {
		return virtual;
	}

	void add(DelegatingMarker marker) {
		markers.add(marker);
	}

	void remove(DelegatingMarker marker) {
		markers.remove(marker);
	}

	void refresh() {
		int count = markers.size();
		if (count == 0) {
			removeVirtual();
		} else if (count == 1) {
			removeVirtual();
			markers.get(0).changeVisible(true);
		} else {
			LatLngBounds.Builder builder = LatLngBounds.builder();
			for (DelegatingMarker m : markers) {
				builder.include(m.getPosition());
				m.changeVisible(false);
			}
			LatLng position = builder.build().getCenter();
			if (virtual == null || lastCount != count) {
				removeVirtual();
				lastCount = count;
				virtual = strategy.createMarker(new ArrayList<>(markers), position);
			} else {
				virtual.setPosition(position);
			}
		}
	}

	IMarker getDisplayedMarker() {
		int count = markers.size();
		if (count == 0) {
			return null;
		} else if (count == 1) {
			return markers.get(0);
		} else {
			return this;
		}
	}

	void removeVirtual() {
		if (virtual != null) {
			virtual.remove();
			virtual = null;
		}
	}

	void cleanup() {
		if (virtual != null) {
			virtual.remove();
		}
	}

	List<DelegatingMarker> getMarkersInternal() {
		return new ArrayList<>(markers);
	}

	@Override
	public void animatePosition(LatLng target) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void animatePosition(LatLng target, AnimationSettings settings) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void animatePosition(LatLng target, AnimationCallback callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void animatePosition(LatLng target, AnimationSettings settings, AnimationCallback callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public float getAlpha() {
		if (virtual != null) {
			return virtual.getAlpha();
		}
		return 1.0f;
	}

	@Override
	public int getClusterGroup() {
		if (markers.size() > 0) {
			return markers.get(0).getClusterGroup();
		}
		throw new IllegalStateException();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getData() {
		return null;
	}

	@Deprecated
	@Override
	public String getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<IMarker> getMarkers() {
		return new ArrayList<>(markers);
	}

	@Override
	public LatLng getPosition() {
		if (virtual != null) {
			return virtual.getPosition();
		}
		LatLngBounds.Builder builder = LatLngBounds.builder();
		for (DelegatingMarker m : markers) {
			builder.include(m.getPosition());
		}
		return builder.build().getCenter();
	}

	@Override
	public float getRotation() {
		if (virtual != null) {
			return virtual.getRotation();
		}
		return 0.0f;
	}

	@Override
	public String getSnippet() {
		return null;
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public void hideInfoWindow() {
		if (virtual != null) {
			virtual.hideInfoWindow();
		}
	}

	@Override
	public boolean isCluster() {
		return true;
	}

	@Override
	public boolean isDraggable() {
		return false;
	}

	@Override
	public boolean isFlat() {
		if (virtual != null) {
			return virtual.isFlat();
		}
		return false;
	}

	@Override
	public boolean isInfoWindowShown() {
		if (virtual != null) {
			return virtual.isInfoWindowShown();
		}
		return false;
	}

	@Override
	public boolean isVisible() {
		if (virtual != null) {
			return virtual.isVisible();
		}
		return false;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAlpha(float alpha) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAnchor(float anchorU, float anchorV) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setClusterGroup(int clusterGroup) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setData(Object data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDraggable(boolean draggable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setFlat(boolean flat) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public void setIcon(BitmapDescriptor icon) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setIcon(Context context, @DrawableRes Integer iconResId, @ColorInt Integer color, @ColorInt Integer secondaryColor, @ColorInt Integer defaultColor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getSecondaryColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getDefaultColor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setInfoWindowAnchor(float anchorU, float anchorV) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPosition(LatLng position) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRotation(float rotation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSnippet(String snippet) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setVisible(boolean visible) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void showInfoWindow() {
		if (virtual == null && markers.size() > 1) {
			refresh();
		}
		if (virtual != null) {
			virtual.showInfoWindow();
		}
	}

	void setVirtualPosition(LatLng position) {
		int count = markers.size();
		if (count == 0) {
			// no op
		} else if (count == 1) {
			markers.get(0).setVirtualPosition(position);
		} else {
			virtual.setPosition(position);
		}
	}
}
