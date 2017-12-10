package org.mtransit.android.ui;

import java.lang.ref.WeakReference;

import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.fragment.VisibilityAwareFragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public abstract class MTActivityWithLocation extends MTActivity implements OnCompleteListener<Location>, OnSuccessListener<Location> {

	private static final String LOCATION_UPDATES_ENABLED = "location_updates_enabled";

	private boolean locationUpdatesEnabled = false;
	private FusedLocationProviderClient fusedLocationClient;
	private LocationRequest locationRequest;
	@Nullable
	private Location userLocation;

	public MTActivityWithLocation() {
		super();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
		restoreState(savedInstanceState);
		this.locationRequest = LocationRequest.create();
		this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // foreground app == high accuracy
		this.locationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MS);
		this.locationRequest.setFastestInterval(LocationUtils.FASTEST_INTERVAL_IN_MS);
	}

	@Override
	protected void onResume() {
		super.onResume();
		enableLocationUpdates();
	}

	public void enableLocationUpdates() {
		if (!this.locationUpdatesEnabled) {
			try {
				this.fusedLocationClient.requestLocationUpdates(this.locationRequest, getLocationCallback(), null);
				this.locationUpdatesEnabled = true;
			} catch (IllegalStateException ise) { // wrong thread?
				MTLog.w(this, ise, "Error while enabling location updates!");
				this.locationUpdatesEnabled = false;
			}
			this.fusedLocationClient.getLastLocation().addOnCompleteListener(this, this);
		}
	}

	@Nullable
	private LocationCallback locationCallback = null;

	@NonNull
	private LocationCallback getLocationCallback() {
		if (locationCallback == null) {
			locationCallback = new LocationCallback(this);
		}
		return locationCallback;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(LOCATION_UPDATES_ENABLED, this.locationUpdatesEnabled);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		restoreState(savedInstanceState);
	}

	private void restoreState(@Nullable Bundle savedInstanceState) {
		Boolean newLocationUpdatesEnabled = BundleUtils.getBoolean(LOCATION_UPDATES_ENABLED, savedInstanceState);
		if (newLocationUpdatesEnabled != null) {
			this.locationUpdatesEnabled = newLocationUpdatesEnabled;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		disableLocationUpdates();
	}

	public void disableLocationUpdates() {
		if (this.locationUpdatesEnabled) {
			this.fusedLocationClient.removeLocationUpdates(getLocationCallback());
			this.locationUpdatesEnabled = false;
		}
	}

	@Override
	public void onSuccess(@Nullable Location location) {
		onLocationChanged(location);
	}

	@Override
	public void onComplete(@NonNull Task<Location> task) {
		if (task.isSuccessful()) {
			onLocationChanged(task.getResult());
		} else {
			MTLog.w(this, "Error while getting location!", task.getException());
		}
	}

	public void onLocationChanged(@Nullable Location location) {
		if (Constants.LOG_LOCATION) {
			MTLog.v(this, "onLocationChanged(%s)", location);
		}
		this.userLocation = location;
		onUserLocationChanged(this.userLocation);
	}

	public abstract void onUserLocationChanged(@Nullable Location newLocation);

	@Nullable
	public Location getUserLocation() {
		return this.userLocation;
	}

	@Nullable
	public Location getLastLocation() {
		if (!this.locationUpdatesEnabled) {
			return null;
		}
		this.fusedLocationClient.getLastLocation().addOnCompleteListener(this, this);
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
		void onUserLocationChanged(@Nullable Location location);
	}

	private static class LocationCallback extends com.google.android.gms.location.LocationCallback implements MTLog.Loggable {

		private static final String TAG = MTActivityWithLocation.class.getSimpleName() + ">" + LocationCallback.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private final WeakReference<MTActivityWithLocation> activityWithLocationWR;

		LocationCallback(MTActivityWithLocation activityWithLocation) {
			this.activityWithLocationWR = new WeakReference<MTActivityWithLocation>(activityWithLocation);
		}

		@Override
		public void onLocationAvailability(LocationAvailability locationAvailability) {
		}

		@Override
		public void onLocationResult(LocationResult locationResult) {
			MTActivityWithLocation activityWithLocation = this.activityWithLocationWR.get();
			if (activityWithLocation == null) {
				return;
			}
			activityWithLocation.onLocationChanged(locationResult.getLastLocation());
		}
	}
}
