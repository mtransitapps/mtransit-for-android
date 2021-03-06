package org.mtransit.android.ui.view.map.impl;

import androidx.collection.ArrayMap;

import org.mtransit.android.ui.view.map.TileOverlay;
import org.mtransit.android.ui.view.map.TileOverlayOptions;

import java.util.ArrayList;
import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class TileOverlayManager {

	private final IGoogleMap factory;

	private final ArrayMap<com.google.android.gms.maps.model.TileOverlay, TileOverlay> tileOverlays;

	public TileOverlayManager(IGoogleMap factory) {
		this.factory = factory;
		this.tileOverlays = new ArrayMap<>();
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
		return new ArrayList<>(tileOverlays.values());
	}

	public void onRemove(com.google.android.gms.maps.model.TileOverlay real) {
		tileOverlays.remove(real);
	}
}
