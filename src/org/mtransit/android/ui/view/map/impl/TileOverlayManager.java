package org.mtransit.android.ui.view.map.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mtransit.android.ui.view.map.TileOverlay;
import org.mtransit.android.ui.view.map.TileOverlayOptions;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class TileOverlayManager {

	private final IGoogleMap factory;

	private final Map<com.google.android.gms.maps.model.TileOverlay, TileOverlay> tileOverlays;

	public TileOverlayManager(IGoogleMap factory) {
		this.factory = factory;
		this.tileOverlays = new HashMap<com.google.android.gms.maps.model.TileOverlay, TileOverlay>();
	}

	public TileOverlay addTileOverlay(TileOverlayOptions tileOverlayOptions) {
		TileOverlay tileOverlay = createTileOverlay(tileOverlayOptions.real);
		tileOverlay.setData(tileOverlayOptions.getData());
		return tileOverlay;
	}

	private TileOverlay createTileOverlay(com.google.android.gms.maps.model.TileOverlayOptions tileOverlayOptions) {
		com.google.android.gms.maps.model.TileOverlay real = factory.addTileOverlay(tileOverlayOptions);
		TileOverlay tileOverlay = new DelegatingTileOverlay(real, this);
		tileOverlays.put(real, tileOverlay);
		return tileOverlay;
	}

	public void clear() {
		tileOverlays.clear();
	}

	public List<TileOverlay> getTileOverlays() {
		return new ArrayList<TileOverlay>(tileOverlays.values());
	}

	public void onRemove(com.google.android.gms.maps.model.TileOverlay real) {
		tileOverlays.remove(real);
	}
}
