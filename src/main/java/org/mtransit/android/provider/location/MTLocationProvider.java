package org.mtransit.android.provider.location;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.ui.view.common.IActivity;

public interface MTLocationProvider {

	void doSetupIfRequired(@NonNull ScreenWithLocationView screenWithLocationView);

	boolean needsSetup(@NonNull ScreenWithLocationView screenWithLocationView);

	void doSetup(@NonNull ScreenWithLocationView screenWithLocationView);

	boolean handleRequestPermissionsResult(@NonNull ScreenWithLocationView screenWithLocationView,
										   int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);

	@Nullable
	Location getLastLocationOrNull();

	void readLastLocation();

	void addOnLastLocationChangeListener(@NonNull OnLastLocationChangeListener onLastLocationChangeListener);

	void removeOnLastLocationChangeListener(@NonNull OnLastLocationChangeListener onLastLocationChangeListener);

	@WorkerThread
	@NonNull
	String getLocationAddressString(@NonNull Location location);

	void updateDistanceWithString(@Nullable LocationUtils.LocationPOI poi, @Nullable Location currentLocation);

	interface OnPermissionsPreRequest {
		void onPermissionsPreRequestPositiveBtnClick(@NonNull ScreenWithLocationView screenWithLocationView);

		@SuppressWarnings("unused")
		void onPermissionsPreRequestNegativeBtnClick(@NonNull ScreenWithLocationView screenWithLocationView);
	}

	interface OnPermissionsRationale {
		void onPermissionsRationalePositiveBtnClick(@NonNull ScreenWithLocationView screenWithLocationView);

		void onPermissionsRationaleNegativeBtnClick(@NonNull ScreenWithLocationView screenWithLocationView);
	}

	interface OnPermissionsPermanentlyDenied {
		void onPermissionsPermanentlyDeniedPositiveBtnClick(@NonNull ScreenWithLocationView screenWithLocationView);

		void onPermissionsPermanentlyDeniedNegativeBtnClick(@NonNull ScreenWithLocationView screenWithLocationView);
	}

	interface ScreenWithLocationView extends IActivity {
		void showPermissionsPreRequest(@NonNull MTLocationProvider.OnPermissionsPreRequest listener);

		void showPermissionsRationale(@NonNull MTLocationProvider.OnPermissionsRationale listener);

		void showPermissionsPermanentlyDenied(@NonNull MTLocationProvider.OnPermissionsPermanentlyDenied listener);

		void showApplicationDetailsSettingsScreen();
	}

	interface OnLastLocationChangeListener {
		void onLastLocationChanged(@Nullable Location lastLocation);
	}
}
