package org.mtransit.android.ui.view.common;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.provider.location.MTLocationProvider;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

public final class ScreenWithLocationCommon implements MTLog.Loggable {

	private static final String LOG_TAG = ScreenWithLocationCommon.class.getSimpleName();

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
		new AlertDialog.Builder(screenWithLocationView.requireActivity())
				.setTitle(R.string.location_permission_rationale_title)
				.setMessage(R.string.location_permission_rationale_message)
				.setPositiveButton(R.string.location_permission_rationale_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						listener.onPermissionsRationalePositiveBtnClick(screenWithLocationView);
					}
				})
				.setNegativeButton(R.string.location_permission_rationale_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						listener.onPermissionsRationaleNegativeBtnClick(screenWithLocationView);
					}
				})
				.create()
				.show();
	}

	public static void showPermissionsPermanentlyDenied(@NonNull final MTLocationProvider.ScreenWithLocationView screenWithLocationView,
			@NonNull final MTLocationProvider.OnPermissionsPermanentlyDenied listener) {
		new AlertDialog.Builder(screenWithLocationView.requireActivity())
				.setTitle(R.string.location_permission_rationale_title)
				.setMessage(R.string.location_permission_rationale_message)
				.setPositiveButton(R.string.location_permission_rationale_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						listener.onPermissionsPermanentlyDeniedPositiveBtnClick(screenWithLocationView);
					}
				})
				.setNegativeButton(R.string.location_permission_rationale_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						listener.onPermissionsPermanentlyDeniedNegativeBtnClick(screenWithLocationView);
					}
				})
				.create()
				.show();
	}

	public static void showApplicationDetailsSettingsScreen(@NonNull MTLocationProvider.ScreenWithLocationView screenWithLocationView) {
		PackageManagerUtils.showAppDetailsSettings(screenWithLocationView.requireActivity(), screenWithLocationView.requireActivity().getPackageName());
	}
}
