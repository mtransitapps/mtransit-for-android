package org.mtransit.android.provider.permission;

import android.content.Context;

import androidx.annotation.NonNull;

import org.mtransit.android.ui.view.common.IActivity;

public interface PermissionProvider {

	boolean permissionsGranted(@NonNull Context context); // needs to be called w/o activity

	boolean shouldShowRequestPermissionRationale(@NonNull IActivity activity);

	boolean hasRequestedPermissions();

	void requestPermissions(@NonNull IActivity activity);

	boolean handleRequestPermissionsResult(int requestCode,
										   @NonNull String[] permissions,
										   @NonNull int[] grantResults,
										   @NonNull OnPermissionGrantedListener onPermissionGrantedListener);

	interface OnPermissionGrantedListener {
		void onPermissionGranted();
	}
}
