package org.mtransit.android.ui;

import android.app.PendingIntent;
import android.location.Location;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.provider.location.MTLocationProvider;
import org.mtransit.android.ui.view.common.ScreenWithLocationCommon;

import java.util.Collection;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public abstract class MTActivityWithLocation extends MTActivity implements
		MTLocationProvider.ScreenWithLocationView,
		MTLocationProvider.OnLocationSettingsChangeListener,
		MTLocationProvider.OnLastLocationChangeListener {

	@Inject
	MTLocationProvider locationProvider;

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (locationProvider.handleRequestPermissionsResult(this, requestCode, permissions, grantResults)) {
			return;
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	protected void onHasAgenciesAddedChanged() {
		this.locationProvider.doSetupIfRequired(this);
	}

	@CallSuper
	@Override
	protected void onResume() {
		super.onResume();
		this.locationProvider.doSetupIfRequired(this);
		this.locationProvider.addOnLastLocationChangeListener(this);
		this.locationProvider.addOnLocationSettingsChangeListener(this);
		this.locationProvider.checkLocationSettings();
	}

	@CallSuper
	@Override
	protected void onPause() {
		super.onPause();
		this.locationProvider.removeOnLocationSettingsChangeListener(this);
		this.locationProvider.removeOnLastLocationChangeListener(this);
	}

	@Nullable
	public Location getDeviceLocation() {
		return this.locationProvider.getLastLocationOrNull();
	}

	@Nullable
	public Location getLastLocation() {
		this.locationProvider.readLastLocation();
		return this.locationProvider.getLastLocationOrNull();
	}

	public void checkLocationSettings() {
		this.locationProvider.checkLocationSettings();
	}

	@Nullable
	public abstract PendingIntent getLastLocationSettingsResolution();

	public static void broadcastLocationSettingsResolutionChanged(@SuppressWarnings("unused") @NonNull MTLog.Loggable loggable,
																  @Nullable Collection<Fragment> fragments,
																  @Nullable PendingIntent resolution) {
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment == null) {
					continue;
				}
				if (fragment instanceof DeviceLocationListener) {
					if (!fragment.isResumed() && !fragment.isVisible()) {
						continue;
					}
					((DeviceLocationListener) fragment).onLocationSettingsResolution(resolution);
				}
			}
		}
	}

	public static void broadcastDeviceLocationChanged(@SuppressWarnings("unused") @NonNull MTLog.Loggable loggable,
													  @Nullable Collection<Fragment> fragments,
													  @Nullable Location newLocation) {
		if (fragments != null) {
			for (Fragment fragment : fragments) {
				if (fragment == null) {
					continue;
				}
				if (fragment instanceof DeviceLocationListener) {
					if (!fragment.isResumed() && !fragment.isVisible()) {
						continue;
					}
					((DeviceLocationListener) fragment).onDeviceLocationChanged(newLocation);
				}
			}
		}
	}

	@Override
	public void showPermissionsPreRequest(@NonNull MTLocationProvider.OnPermissionsPreRequest listener) {
		ScreenWithLocationCommon.showPermissionsPreRequest(this, listener);
	}

	@Override
	public void showPermissionsRationale(@NonNull MTLocationProvider.OnPermissionsRationale listener) {
		ScreenWithLocationCommon.showPermissionsRationale(this, listener);
	}

	@Override
	public void showPermissionsPermanentlyDenied(@NonNull MTLocationProvider.OnPermissionsPermanentlyDenied listener) {
		ScreenWithLocationCommon.showPermissionsPermanentlyDenied(this, listener);
	}

	@Override
	public void showApplicationDetailsSettingsScreen() {
		ScreenWithLocationCommon.showApplicationDetailsSettingsScreen(this);
	}

	public interface DeviceLocationListener {

		void onLocationSettingsResolution(@Nullable PendingIntent resolution);

		void onDeviceLocationChanged(@Nullable Location location);
	}
}
