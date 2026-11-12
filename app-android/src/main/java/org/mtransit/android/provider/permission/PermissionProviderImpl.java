package org.mtransit.android.provider.permission;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.common.IActivity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class PermissionProviderImpl implements PermissionProvider, MTLog.Loggable {

	@NonNull
	private final Map<String, Integer> requestedPermissions = new HashMap<>();

	private boolean requestedPermissionsDenied = false;

	@NonNull
	private final Context appContext;

	public PermissionProviderImpl(@NonNull Context appContext) {
		this.appContext = appContext.getApplicationContext();
	}

	abstract Collection<String> getRequiredPermissions();

	abstract String[] getAllPermissions();

	abstract int getRequestCode();

	@Override
	public boolean allRequiredPermissionsGranted() {
		for (String requiredPermission : getRequiredPermissions()) {
			if (!permissionGranted(requiredPermission)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean permissionGranted(@NonNull String permission) {
		return ActivityCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED;
	}

	@Override
	public boolean shouldShowRequestRequiredPermissionsRationale(@NonNull IActivity activity) {
		for (String requiredPermission : getRequiredPermissions()) {
			if (shouldShowRequestPermissionRationale(activity, requiredPermission)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean shouldShowRequestPermissionRationale(@NonNull IActivity activity, @NonNull String permission) {
		return ActivityCompat.shouldShowRequestPermissionRationale(activity.requireActivity(), permission);
	}

	@Override
	public boolean requestedPermissionsDenied() {
		return this.requestedPermissionsDenied;
	}

	@Override
	public boolean hasRequestedPermissions() {
		return !this.requestedPermissions.isEmpty();
	}

	@Override
	public boolean hasRequestedPermission(@NonNull String permission) {
		return this.requestedPermissions.get(permission) != null;
	}

	@Override
	public void requestPermissions(@NonNull IActivity activity) {
		ActivityCompat.requestPermissions(activity.requireActivity(), getAllPermissions(), getRequestCode());
	}

	@Override
	public boolean handleRequestPermissionsResult(
			int requestCode,
			@NonNull String[] permissions,
			@NonNull int[] grantResults,
			@NonNull OnPermissionGrantedListener onPermissionGrantedListener
	) {
		if (requestCode != getRequestCode()) return false; // not handled
		int requiredPermissionGranted = 0;
		boolean changed = false;
		for (int i = 0; i < grantResults.length; i++) {
			final int grantResult = grantResults[i];
			final String permission = permissions.length > i ? permissions[i] : null;
			if (getRequiredPermissions().contains(permission)) {
				final Integer previous = this.requestedPermissions.get(permission);
				if (previous == null || previous != grantResult) {
					changed = true;
				}
				this.requestedPermissions.put(permission, grantResult);
				if (grantResult == PackageManager.PERMISSION_GRANTED) {
					requiredPermissionGranted++;
				}
			}
		}
		if (!changed) {
			this.requestedPermissionsDenied = true;
		}
		if (getRequiredPermissions().size() == requiredPermissionGranted) {
			onPermissionGrantedListener.onPermissionGranted();
		}
		return true; // handled
	}
}
