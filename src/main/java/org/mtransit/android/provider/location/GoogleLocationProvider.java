package org.mtransit.android.provider.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.dev.FakeLocation;
import org.mtransit.android.provider.permission.LocationPermissionProvider;

import java.util.Arrays;
import java.util.WeakHashMap;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class GoogleLocationProvider
		implements MTLocationProvider,
		MTLocationProvider.OnPermissionsPreRequest,
		MTLocationProvider.OnPermissionsRationale,
		MTLocationProvider.OnPermissionsPermanentlyDenied,
		MTLog.Loggable {

	private static final String LOG_TAG = GoogleLocationProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final LocationRequest FOREGROUND_LOCATION_REQUEST = LocationRequest.create() //
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // foreground app == high accuracy
			.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MS) //
			.setFastestInterval(LocationUtils.FASTEST_INTERVAL_IN_MS);

	@Nullable
	private FusedLocationProviderClient fusedLocationProviderClient = null;
	@Nullable
	private Location lastLocation = null;

	@Nullable
	private LocationCallback foregroundLocationCallback = null;
	private boolean foregroundLocationUpdatesEnabled = false;

	@NonNull
	private final WeakHashMap<OnLastLocationChangeListener, Object> onLastLocationChangeListeners = new WeakHashMap<>();

	@NonNull
	private final Context appContext;
	@NonNull
	private final LocationPermissionProvider permissionProvider;
	@NonNull
	private final DemoModeManager demoModeManager;
	@NonNull
	private final CrashReporter crashReporter;

	@Inject
	public GoogleLocationProvider(@NonNull @ApplicationContext Context appContext,
								  @NonNull LocationPermissionProvider permissionProvider,
								  @NonNull DemoModeManager demoModeManager,
								  @NonNull CrashReporter crashReporter) {
		this.appContext = appContext;
		this.permissionProvider = permissionProvider;
		this.demoModeManager = demoModeManager;
		this.crashReporter = crashReporter;
	}

	@Override
	public void doSetupIfRequired(@NonNull ScreenWithLocationView screenWithLocationView) {
		if (needsSetup(screenWithLocationView)) {
			doSetup(screenWithLocationView);
		} else {
			onLocationProviderReady();
		}
	}

	@Override
	public boolean needsSetup(@NonNull ScreenWithLocationView screenWithLocationView) {
		return !this.permissionProvider.allRequiredPermissionsGranted(screenWithLocationView.requireContext());
	}

	@Override
	public void doSetup(@NonNull ScreenWithLocationView screenWithLocationView) {
		for (String requiredPermission : this.permissionProvider.getRequiredPermissions()) {
			if (!this.permissionProvider.permissionGranted(appContext, requiredPermission)) {
				if (this.permissionProvider.shouldShowRequestPermissionRationale(screenWithLocationView, requiredPermission)) {
					screenWithLocationView.showPermissionsRationale(this);
					return;
				} else if (this.permissionProvider.requestedPermissionsDenied()) { // user permanently denied permission
					screenWithLocationView.showPermissionsPermanentlyDenied(this);
					return;
				} else { // never asked permission
					screenWithLocationView.showPermissionsPreRequest(this);
					return;
				}
			}
		}
	}

	@Override
	public void onPermissionsPreRequestNegativeBtnClick(@NonNull ScreenWithLocationView screenWithLocationView) {
		crashReporter.shouldNotHappen("Permission pre-request negative click not handled");
	}

	@Override
	public void onPermissionsPreRequestPositiveBtnClick(@NonNull ScreenWithLocationView screenWithLocationView) {
		this.permissionProvider.requestPermissions(screenWithLocationView);
	}

	@Override
	public void onPermissionsRationaleNegativeBtnClick(@NonNull ScreenWithLocationView screenWithLocationView) {
		screenWithLocationView.finish();
	}

	@Override
	public void onPermissionsRationalePositiveBtnClick(@NonNull ScreenWithLocationView screenWithLocationView) {
		this.permissionProvider.requestPermissions(screenWithLocationView);
	}

	@Override
	public void onPermissionsPermanentlyDeniedNegativeBtnClick(@NonNull ScreenWithLocationView screenWithLocationView) {
		screenWithLocationView.finish();
	}

	@Override
	public void onPermissionsPermanentlyDeniedPositiveBtnClick(@NonNull ScreenWithLocationView screenWithLocationView) {
		screenWithLocationView.showApplicationDetailsSettingsScreen();
	}

	@Override
	public boolean handleRequestPermissionsResult(@NonNull final ScreenWithLocationView screenWithLocationView,
												  final int requestCode,
												  @NonNull String[] permissions,
												  @NonNull int[] grantResults) {
		return this.permissionProvider.handleRequestPermissionsResult(requestCode, permissions, grantResults,
				this::onLocationProviderReady);
	}

	private void onLocationProviderReady() {
		if (this.foregroundLocationUpdatesEnabled) {
			crashReporter.shouldNotHappen("Foreground location updates enabled before provider ready!");
			return;
		}
		enableDisableForegroundLocationUpdates();
	}

	private void enableDisableForegroundLocationUpdates() {
		if (!this.permissionProvider.allRequiredPermissionsGranted(this.appContext)) {
			MTLog.d(this, "enableDisableForegroundLocationUpdates() > SKIP (permission NOT granted yet)");
			return;
		}
		if (this.foregroundLocationUpdatesEnabled) {
			if (this.onLastLocationChangeListeners.isEmpty()) {
				disableForegroundLocationUpdates();
				this.foregroundLocationUpdatesEnabled = false;
			}
		} else { // IF foreground location update NOT enabled
			if (!this.onLastLocationChangeListeners.isEmpty()) {
				enableForegroundLocationUpdates();
				this.foregroundLocationUpdatesEnabled = true;
			}
		}
	}

	@SuppressLint("MissingPermission")
	private void enableForegroundLocationUpdates() {
		if (!this.permissionProvider.allRequiredPermissionsGranted(this.appContext)) {
			crashReporter.shouldNotHappen("Last location requested w/o permission!");
			return;
		}
		if (this.foregroundLocationCallback != null) {
			crashReporter.shouldNotHappen("Foreground location callback already set!");
			this.foregroundLocationCallback = null;
		}
		this.foregroundLocationCallback = new MTLocationCallback(this);
		getFusedLocationProviderClient().requestLocationUpdates(FOREGROUND_LOCATION_REQUEST, this.foregroundLocationCallback, Looper.getMainLooper());
	}

	private void disableForegroundLocationUpdates() {
		if (this.foregroundLocationCallback == null) {
			crashReporter.shouldNotHappen("Foreground location callback null (cannot be disabled)!");
			return;
		}
		getFusedLocationProviderClient().removeLocationUpdates(this.foregroundLocationCallback);
		this.foregroundLocationCallback = null; // required!
	}

	@Nullable
	@Override
	public Location getLastLocationOrNull() {
		return this.lastLocation;
	}

	@SuppressLint("MissingPermission")
	@Override
	public void readLastLocation() {
		if (!this.permissionProvider.allRequiredPermissionsGranted(this.appContext)) {
			MTLog.d(this, "readLastLocation() > SKIP (no permission)");
			return;
		}
		getFusedLocationProviderClient().getLastLocation()
				.addOnSuccessListener(this::onNewLastLocation);
	}

	private void onNewLastLocation(@SuppressWarnings("ParameterCanBeLocal") @Nullable Location lastLocation) {
		if (FakeLocation.ENABLED) {
			lastLocation = FakeLocation.getLocation();
		}
		if (demoModeManager.getEnabled()) {
			final Location filterLocation = demoModeManager.getFilterLocation();
			if (filterLocation != null) {
				lastLocation = filterLocation;
			}
		}
		if (Constants.LOG_LOCATION) {
			MTLog.d(this, "onNewLastLocation(%s)", lastLocation);
		}
		this.lastLocation = lastLocation;
		broadcastUserLocationChanged(this.lastLocation);
	}

	private void broadcastUserLocationChanged(@Nullable Location lastLocation) {
		for (OnLastLocationChangeListener lastLocationChangeListener : this.onLastLocationChangeListeners.keySet()) {
			if (lastLocationChangeListener == null) {
				continue;
			}
			lastLocationChangeListener.onLastLocationChanged(lastLocation);
		}
	}

	@Override
	public void addOnLastLocationChangeListener(@NonNull OnLastLocationChangeListener onLastLocationChangeListener) {
		this.onLastLocationChangeListeners.put(onLastLocationChangeListener, null);
		enableDisableForegroundLocationUpdates();
	}

	@Override
	public void removeOnLastLocationChangeListener(@NonNull OnLastLocationChangeListener onLastLocationChangeListener) {
		this.onLastLocationChangeListeners.remove(onLastLocationChangeListener);
		enableDisableForegroundLocationUpdates();
	}

	@NonNull
	private FusedLocationProviderClient getFusedLocationProviderClient() {
		if (this.fusedLocationProviderClient == null) {
			this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.appContext);
		}
		return this.fusedLocationProviderClient;
	}

	@WorkerThread
	@NonNull
	@Override
	public String getLocationAddressString(@NonNull Location location) {
		return LocationUtils.getLocationString(
				this.appContext,
				null,
				LocationUtils.getLocationAddress(this.appContext, location),
				location.getAccuracy()
		);
	}

	@Override
	public void updateDistanceWithString(@Nullable LocationUtils.LocationPOI poi, @Nullable Location currentLocation) {
		LocationUtils.updateDistanceWithString(this.appContext, poi, currentLocation);
	}

	private static class MTLocationCallback extends LocationCallback implements MTLog.Loggable {

		private static final String LOG_TAG = MTLocationCallback.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final GoogleLocationProvider googleLocationProvider;

		MTLocationCallback(@NonNull GoogleLocationProvider googleLocationProvider) {
			super();
			this.googleLocationProvider = googleLocationProvider;
		}

		@Override
		public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
			super.onLocationAvailability(locationAvailability);
		}

		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			this.googleLocationProvider.onNewLastLocation(locationResult.getLastLocation());
		}
	}
}
