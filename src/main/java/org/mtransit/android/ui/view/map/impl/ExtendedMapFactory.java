package org.mtransit.android.ui.view.map.impl;

import org.mtransit.android.ui.view.map.ExtendedGoogleMap;

import android.content.Context;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public final class ExtendedMapFactory {

	private ExtendedMapFactory() {
	}

	public static ExtendedGoogleMap create(com.google.android.gms.maps.GoogleMap real, Context context) {
		return new DelegatingGoogleMap(new GoogleMapWrapper(real), context);
	}
}
