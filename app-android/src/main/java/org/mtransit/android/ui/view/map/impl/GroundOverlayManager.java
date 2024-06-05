package org.mtransit.android.ui.view.map.impl;

import androidx.collection.ArrayMap;

import org.mtransit.android.ui.view.map.GroundOverlay;
import org.mtransit.android.ui.view.map.GroundOverlayOptions;

import java.util.ArrayList;
import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class GroundOverlayManager {

	private final IGoogleMap factory;

	private final ArrayMap<com.google.android.gms.maps.model.GroundOverlay, GroundOverlay> groundOverlays;

	public GroundOverlayManager(IGoogleMap factory) {
		this.factory = factory;
		this.groundOverlays = new ArrayMap<>();
	}

	public GroundOverlay addGroundOverlay(GroundOverlayOptions groundOverlayOptions) {
		GroundOverlay groundOverlay = createGroundOverlay(groundOverlayOptions.real);
		groundOverlay.setData(groundOverlayOptions.getData());
		return groundOverlay;
	}

	private GroundOverlay createGroundOverlay(com.google.android.gms.maps.model.GroundOverlayOptions groundOverlayOptions) {
		com.google.android.gms.maps.model.GroundOverlay real = factory.addGroundOverlay(groundOverlayOptions);
		GroundOverlay groundOverlay = new DelegatingGroundOverlay(real, this);
		groundOverlays.put(real, groundOverlay);
		return groundOverlay;
	}

	public void clear() {
		groundOverlays.clear();
	}

	public List<GroundOverlay> getGroundOverlays() {
		return new ArrayList<>(groundOverlays.values());
	}

	public void onRemove(com.google.android.gms.maps.model.GroundOverlay real) {
		groundOverlays.remove(real);
	}
}
