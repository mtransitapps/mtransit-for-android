package org.mtransit.android.provider.permission;

import android.Manifest;

import org.mtransit.android.commons.MTLog;

public class LocationPermissionProvider extends PermissionProviderImpl implements MTLog.Loggable {

	private static final String LOG_TAG = LocationPermissionProvider.class.getSimpleName();

	private static final String MAIN_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
	private static final String[] ALL_PERMISSIONS = new String[] {
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION
	};

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Override
	public String getMainPermission() {
		return MAIN_PERMISSION;
	}

	@Override
	public String[] getAllPermissions() {
		return ALL_PERMISSIONS;
	}
}
