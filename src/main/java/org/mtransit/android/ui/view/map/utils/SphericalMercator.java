package org.mtransit.android.ui.view.map.utils;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public final class SphericalMercator {

	private static final double MIN_LATITUDE = -85.0511287798;
	private static final double MAX_LATITUDE = 85.0511287798;

	private SphericalMercator() {
	}

	public static double fromLatitude(double latitude) {
		double radians = Math.toRadians(latitude + 90) / 2;
		return Math.toDegrees(Math.log(Math.tan(radians)));
	}

	public static double toLatitude(double mercator) {
		double radians = Math.atan(Math.exp(Math.toRadians(mercator)));
		return Math.toDegrees(2 * radians) - 90;
	}

	public static double scaleLatitude(double latitude) {
		if (latitude < MIN_LATITUDE) {
			latitude = MIN_LATITUDE;
		} else if (latitude > MAX_LATITUDE) {
			latitude = MAX_LATITUDE;
		}
		return fromLatitude(latitude) + 180.0;
	}

	public static double scaleLongitude(double longitude) {
		return longitude + 180.0;
	}
}
