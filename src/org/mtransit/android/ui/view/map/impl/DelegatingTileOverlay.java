package org.mtransit.android.ui.view.map.impl;

import org.mtransit.android.ui.view.map.TileOverlay;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class DelegatingTileOverlay implements TileOverlay {

	private com.google.android.gms.maps.model.TileOverlay real;
	private TileOverlayManager manager;

	private Object data;

	DelegatingTileOverlay(com.google.android.gms.maps.model.TileOverlay real, TileOverlayManager manager) {
		this.real = real;
		this.manager = manager;
	}

	@Override
	public void clearTileCache() {
		real.clearTileCache();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getData() {
		return data;
	}

	@Override
	public boolean getFadeIn() {
		return real.getFadeIn();
	}

	@Deprecated
	@Override
	public String getId() {
		return real.getId();
	}

	@Override
	public float getZIndex() {
		return real.getZIndex();
	}

	@Override
	public boolean isVisible() {
		return real.isVisible();
	}

	@Override
	public void remove() {
		manager.onRemove(real);
		real.remove();
	}

	@Override
	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public void setFadeIn(boolean fadeIn) {
		real.setFadeIn(fadeIn);
	}

	@Override
	public void setVisible(boolean visible) {
		real.setVisible(visible);
	}

	@Override
	public void setZIndex(float zIndex) {
		real.setZIndex(zIndex);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DelegatingTileOverlay)) {
			return false;
		}
		DelegatingTileOverlay other = (DelegatingTileOverlay) o;
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
}
