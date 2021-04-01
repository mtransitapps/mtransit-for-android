package org.mtransit.android.provider.location;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.FakeLocation;
import org.mtransit.android.provider.permission.LocationPermissionProvider;

import java.util.WeakHashMap;

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
	private final IApplication application;
	@NonNull
	private final LocationPermissionProvider permissionProvider;
	@NonNull
	private final CrashReporter crashReporter;

	public GoogleLocationProvider(@NonNull IApplication application,
								  @NonNull LocationPermissionProvider permissionProvider,
								  @NonNull CrashReporter crashReporter) {
		this.application = application;
		this.permissionProvider = permissionProvider;
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
		return !this.permissionProvider.permissionsGranted(screenWithLocationView);
	}

	@Override
	public void doSetup(@NonNull ScreenWithLocationView screenWithLocationView) {
		if (!this.permissionProvider.permissionsGranted(screenWithLocationView)) {
			if (this.permissionProvider.shouldShowRequestPermissionRationale(screenWithLocationView)) {
				screenWithLocationView.showPermissionsRationale(this);
			} else if (this.permissionProvider.hasRequestedPermissions()) { // user permanently denied permission
				screenWithLocationView.showPermissionsPermanentlyDenied(this);
			} else { // never asked permission
				screenWithLocationView.showPermissionsPreRequest(this);
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
												  final int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
		if (!this.permissionProvider.permissionsGranted(this.application)) {
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
		if (!this.permissionProvider.permissionsGranted(this.application)) {
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
		if (!this.permissionProvider.permissionsGranted(this.application)) {
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
			this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.application.requireApplication());
		}
		return this.fusedLocationProviderClient;
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
