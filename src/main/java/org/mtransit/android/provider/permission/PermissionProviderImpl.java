package org.mtransit.android.provider.permission;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.mtransit.android.common.RequestCodes;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.common.IActivity;

public abstract class PermissionProviderImpl implements PermissionProvider, MTLog.Loggable {

	private boolean requestedPermissions = false;

	abstract String getMainPermission();

	abstract String[] getAllPermissions();

	@Override
	public boolean permissionsGranted(@NonNull Context context) {
		return ActivityCompat.checkSelfPermission(context, getMainPermission()) == PackageManager.PERMISSION_GRANTED;
	}

	@Override
	public boolean shouldShowRequestPermissionRationale(@NonNull IActivity activity) {
		return ActivityCompat.shouldShowRequestPermissionRationale(activity.requireActivity(), getMainPermission());
	}

	@Override
	public boolean hasRequestedPermissions() {
		return this.requestedPermissions;
	}

	@Override
	public void requestPermissions(@NonNull IActivity activity) {
		this.requestedPermissions = true;
		ActivityCompat.requestPermissions(activity.requireActivity(), getAllPermissions(), RequestCodes.PERMISSIONS_LOCATION_RC);
	}

	@Override
	public boolean handleRequestPermissionsResult(int requestCode,
												  @NonNull String[] permissions,
												  @NonNull int[] grantResults,
												  @NonNull OnPermissionGrantedListener onPermissionGrantedListener) {
		if (requestCode == RequestCodes.PERMISSIONS_LOCATION_RC) {
			if (grantResults.length > 0
					&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				onPermissionGrantedListener.onPermissionGranted();
			}
			return true; // handled
		}
		return false; // not handled
	}
}
