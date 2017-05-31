package org.mtransit.android.ui;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.fragment.VisibilityAwareFragment;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.google.android.gms.common.api.GoogleApiClient;
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
	protected void addGoogleAPIs(GoogleApiClient.Builder googleApiClientBuilder) {
		if (this.useLocation) {
			googleApiClientBuilder.addApi(LocationServices.API);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (this.useLocation) {
			this.locationRequest = LocationRequest.create();
			this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // foreground app == high accuracy
			this.locationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MS);
			this.locationRequest.setFastestInterval(LocationUtils.FASTEST_INTERVAL_IN_MS);
		}
	}

	@Override
	public void onClientConnected() {
		if (this.useLocation) {
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
			GoogleApiClient googleApiClient = getGoogleApiClientOrNull();
			if (googleApiClient != null && googleApiClient.isConnected()) {
				try {
					LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, this.locationRequest, this);
					this.locationUpdatesEnabled = true;
				} catch (IllegalStateException ise) { // wrong thread?
					MTLog.w(this, ise, "Error while enabling location updates!");
					this.locationUpdatesEnabled = false;
				}
				Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
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
			GoogleApiClient googleApiClient = getGoogleApiClientOrNull();
			if (googleApiClient != null && googleApiClient.isConnected()) {
				LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
				this.locationUpdatesEnabled = false;
			}
		}
	}

	@Override
	public void onBeforeClientDisconnected() {
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
		return this.userLocation;
	}

	public Location getLastLocation() {
		if (!this.locationUpdatesEnabled) {
			return null;
		}
		GoogleApiClient googleApiClient = getGoogleApiClientOrNull();
		if (googleApiClient == null) {
			return null;
		}
		Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
		onLocationChanged(lastLocation);
		return this.userLocation;
	}

	public static void broadcastUserLocationChanged(MTLog.Loggable loggable, java.util.Set<Fragment> fragments, Location newLocation) {
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment == null) {
					continue;
				}
				if (fragment instanceof MTActivityWithLocation.UserLocationListener) {
					if (!fragment.isResumed() && !fragment.isVisible()) {
						continue;
					}
					if (fragment instanceof VisibilityAwareFragment) {
						VisibilityAwareFragment visibilityAwareFragment = (VisibilityAwareFragment) fragment;
						if (!visibilityAwareFragment.isFragmentVisible()) {
							continue;
						}
					}
					((MTActivityWithLocation.UserLocationListener) fragment).onUserLocationChanged(newLocation);
				}
			}
		}
	}

	public interface UserLocationListener {
		void onUserLocationChanged(Location location);
	}
}
