package org.mtransit.android.ui.view.map.utils;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public final class LatLngUtils {

	private LatLngUtils() {
	}

	public static float distanceBetween(LatLng first, LatLng second) {
		float[] distance = new float[1];
		Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, distance);
		return distance[0];
	}

	public static LatLng fromLocation(Location location) {
		return new LatLng(location.getLatitude(), location.getLongitude());
	}
}
