package org.mtransit.android.provider.permission;

import org.mtransit.android.common.IContext;
import org.mtransit.android.ui.view.common.IActivity;

import androidx.annotation.NonNull;

public interface PermissionProvider {
	boolean permissionsGranted(@NonNull IContext context);

	boolean shouldShowRequestPermissionRationale(@NonNull IActivity activity);

	boolean hasRequestedPermissions();

	void requestPermissions(@NonNull IActivity activity);

	boolean handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults,
										   @NonNull OnPermissionGrantedListener onPermissionGrantedListener);

	interface OnPermissionGrantedListener {
		void onPermissionGranted();
	}
}
