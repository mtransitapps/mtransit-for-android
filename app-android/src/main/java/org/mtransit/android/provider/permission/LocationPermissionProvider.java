package org.mtransit.android.provider.permission;

import android.Manifest;

import androidx.annotation.NonNull;

import org.mtransit.android.common.RequestCodes;
import org.mtransit.android.commons.MTLog;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

public class LocationPermissionProvider extends PermissionProviderImpl implements MTLog.Loggable {

	private static final String LOG_TAG = LocationPermissionProvider.class.getSimpleName();

	private static final Collection<String> REQUIRED_PERMISSIONS = Arrays.asList(
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION
	);
	private static final String[] ALL_PERMISSIONS = new String[]{
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION
	};

	@Override
	int getRequestCode() {
		return RequestCodes.PERMISSIONS_LOCATION_RC;
	}

	@Inject
	public LocationPermissionProvider() {
		// DO NOTHING
	}

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	@Override
	public Collection<String> getRequiredPermissions() {
		return REQUIRED_PERMISSIONS;
	}

	@NonNull
	@Override
	public String[] getAllPermissions() {
		return ALL_PERMISSIONS;
	}
}
