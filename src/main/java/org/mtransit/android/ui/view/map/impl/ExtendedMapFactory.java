package org.mtransit.android.ui.view.map.impl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.ui.view.map.ExtendedGoogleMap;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public final class ExtendedMapFactory {

	private ExtendedMapFactory() {
	}

	@NonNull
	public static ExtendedGoogleMap create(@NonNull com.google.android.gms.maps.GoogleMap real, @Nullable Context context) {
		return new DelegatingGoogleMap(new GoogleMapWrapper(real), context);
	}
}
