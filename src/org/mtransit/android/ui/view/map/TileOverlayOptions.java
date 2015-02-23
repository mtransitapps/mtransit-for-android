package org.mtransit.android.ui.view.map;

import com.google.android.gms.maps.model.TileProvider;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class TileOverlayOptions {

	public final com.google.android.gms.maps.model.TileOverlayOptions real = new com.google.android.gms.maps.model.TileOverlayOptions();
	private Object data;

	public TileOverlayOptions data(Object data) {
		this.data = data;
		return this;
	}

	public TileOverlayOptions fadeIn(boolean fadeIn) {
		real.fadeIn(fadeIn);
		return this;
	}

	public Object getData() {
		return data;
	}

	public boolean getFadeIn() {
		return real.getFadeIn();
	}

	public TileProvider getTileProvider() {
		return real.getTileProvider();
	}

	public float getZIndex() {
		return real.getZIndex();
	}

	public boolean isVisible() {
		return real.isVisible();
	}

	public TileOverlayOptions tileProvider(TileProvider tileProvider) {
		real.tileProvider(tileProvider);
		return this;
	}

	public TileOverlayOptions visible(boolean visible) {
		real.visible(visible);
		return this;
	}

	public TileOverlayOptions zIndex(float zIndex) {
		real.zIndex(zIndex);
		return this;
	}
}
