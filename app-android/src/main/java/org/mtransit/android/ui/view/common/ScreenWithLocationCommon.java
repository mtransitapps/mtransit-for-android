package org.mtransit.android.ui.view.common;

import androidx.annotation.NonNull;

import org.mtransit.android.R;
import org.mtransit.android.commons.DeviceUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.provider.location.MTLocationProvider;
import org.mtransit.android.ui.MTDialog;

public final class ScreenWithLocationCommon implements MTLog.Loggable {

	private static final String LOG_TAG = ScreenWithLocationCommon.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void showPermissionsPreRequest(@NonNull MTLocationProvider.ScreenWithLocationView screenWithLocationView,
												 @NonNull MTLocationProvider.OnPermissionsPreRequest listener) {
		listener.onPermissionsPreRequestPositiveBtnClick(screenWithLocationView); // no UI for now
	}

	public static void showPermissionsRationale(@NonNull final MTLocationProvider.ScreenWithLocationView screenWithLocationView,
												@NonNull final MTLocationProvider.OnPermissionsRationale listener) {
		new MTDialog.Builder(screenWithLocationView.requireActivity())
				.setTitle(R.string.location_permission_rationale_title)
				.setMessage(R.string.location_permission_rationale_message)
				.setPositiveButton(R.string.location_permission_rationale_ok, (dialog, which) -> {
					dialog.dismiss();
					listener.onPermissionsRationalePositiveBtnClick(screenWithLocationView);
				})
				.setNegativeButton(R.string.location_permission_rationale_cancel, (dialog, which) -> {
					dialog.dismiss();
					listener.onPermissionsRationaleNegativeBtnClick(screenWithLocationView);
				})
				.setCancelable(true) // kinda OK not forcing location request
				.create()
				.show();
	}

	public static void showPermissionsPermanentlyDenied(@NonNull final MTLocationProvider.ScreenWithLocationView screenWithLocationView,
														@NonNull final MTLocationProvider.OnPermissionsPermanentlyDenied listener) {
		new MTDialog.Builder(screenWithLocationView.requireActivity())
				.setTitle(R.string.location_permission_rationale_title)
				.setMessage(R.string.location_permission_rationale_message)
				.setPositiveButton(R.string.location_permission_rationale_ok, (dialog, which) -> {
					dialog.dismiss();
					listener.onPermissionsPermanentlyDeniedPositiveBtnClick(screenWithLocationView);
				})
				.setNegativeButton(R.string.location_permission_rationale_cancel, (dialog, which) -> {
					dialog.dismiss();
					listener.onPermissionsPermanentlyDeniedNegativeBtnClick(screenWithLocationView);
				})
				.setCancelable(true) // kinda OK not forcing location request
				.create()
				.show();
	}

	public static void showApplicationDetailsSettingsScreen(@NonNull MTLocationProvider.ScreenWithLocationView screenWithLocationView) {
		DeviceUtils.showAppDetailsSettings(screenWithLocationView.requireActivity(), screenWithLocationView.requireActivity().getPackageName());
	}
}
