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
	private boolean locationUpdatesEnabled = false;
	private LocationRequest locationRequest;
	private Location userLocation;

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
			enableLocationUpdates();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		enableLocationUpdates();
	}

	public void enableLocationUpdates() {
		if (this.useLocation && !this.locationUpdatesEnabled) {
			if (this.googleApiClient != null && this.googleApiClient.isConnected()) {
				LocationServices.FusedLocationApi.requestLocationUpdates(this.googleApiClient, this.locationRequest, this);
				this.locationUpdatesEnabled = true;
				final Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
				onLocationChanged(lastLocation);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		disableLocationUpdates();
	}

	public void disableLocationUpdates() {
		if (this.locationUpdatesEnabled) {
			if (this.googleApiClient != null && this.googleApiClient.isConnected()) {
				LocationServices.FusedLocationApi.removeLocationUpdates(this.googleApiClient, this);
				this.locationUpdatesEnabled = false;
			}
		}
	}

	@Override
	public void onBeforeClientDisconnected(GoogleApiClient googleApiClient) {
		disableLocationUpdates();
	}

	@Override
	public void onLocationChanged(Location location) {
		if (Constants.LOG_LOCATION) {
			MTLog.v(this, "onLocationChanged(%s)", location);
		}
		this.userLocation = location;
		onUserLocationChanged(this.userLocation);
	}

	public abstract void onUserLocationChanged(Location newLocation);

	public Location getUserLocation() {
		return userLocation;
	}

	public Location getLastLocation() {
		if (!locationUpdatesEnabled) {
			return null;
		}
		final Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
		onLocationChanged(lastLocation);
		return this.userLocation;
	}

	public static interface UserLocationListener {
		public void onUserLocationChanged(Location location);
	}
}
