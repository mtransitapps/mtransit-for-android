package org.mtransit.android.ui;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;

import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public abstract class MTActivityWithLocation extends MTActivityWithGoogleAPIClient implements com.google.android.gms.location.LocationListener {

	private boolean useLocation = false;
	private LocationRequest locationRequest;

	public MTActivityWithLocation(boolean useLocation) {
		super(useLocation); // location requires Google Play Services
		this.useLocation = useLocation;
	}

	@Override
	protected void addGoogleAPIs(Builder googleApiClientBuilder) {
		if (useLocation) {
			googleApiClientBuilder.addApi(LocationServices.API);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (useLocation) {
			this.locationRequest = LocationRequest.create();
			this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // foreground app == high accuracy
			this.locationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MS);
			this.locationRequest.setFastestInterval(LocationUtils.FASTEST_INTERVAL_IN_MS);
		}
	}

	@Override
	public void onClientConnected(GoogleApiClient googleApiClient) {
		if (useLocation) {
			LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, this.locationRequest, this);
			final Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
			onLocationChanged(lastLocation);
		}

	}

	@Override
	public void onBeforeClientDisconnected(GoogleApiClient googleApiClient) {
		if (useLocation) {
			if (googleApiClient.isConnected()) {
				LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if (Constants.LOG_LOCATION) {
			MTLog.v(this, "onLocationChanged(%s)", location);
		}
	}

	public Location getLastLocation() {
		if (!useLocation) {
			return null;
		}
		final Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
		return lastLocation;
	}

	public static interface UserLocationListener {
		public void onUserLocationChanged(Location location);
	}
}
