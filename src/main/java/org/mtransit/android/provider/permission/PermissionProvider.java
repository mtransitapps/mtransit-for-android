package org.mtransit.android.provider.permission;

import android.content.Context;

import androidx.annotation.NonNull;

import org.mtransit.android.ui.view.common.IActivity;

public interface PermissionProvider {

	boolean allRequiredPermissionsGranted(@NonNull Context context); // needs to be called w/o activity

	boolean permissionGranted(@NonNull Context context, @NonNull String permission);

	boolean shouldShowRequestPermissionRationale(@NonNull IActivity activity);

	boolean shouldShowRequestPermissionRationale(@NonNull IActivity activity, @NonNull String permission);

	boolean requestedPermissionsDenied();

	boolean hasRequestedPermissions();

	boolean hasRequestedPermission(@NonNull String permission);

	void requestPermissions(@NonNull IActivity activity);

	boolean handleRequestPermissionsResult(int requestCode,
										   @NonNull String[] permissions,
										   @NonNull int[] grantResults,
										   @NonNull OnPermissionGrantedListener onPermissionGrantedListener);

	interface OnPermissionGrantedListener {
		void onPermissionGranted();
	}
}
