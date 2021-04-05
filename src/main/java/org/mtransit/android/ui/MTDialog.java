package org.mtransit.android.ui;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;

public final class MTDialog {

	private MTDialog() {
	}

	public static class Builder extends MaterialAlertDialogBuilder {

		@NonNull
		private final WeakReference<Activity> activityWR;

		public Builder(@NonNull Activity activity) {
			super(activity);
			activityWR = new WeakReference<>(activity);
		}

		/**
		 * @deprecated use {@link #Builder(Activity)}
		 */
		@Deprecated
		public Builder(@NonNull Context context) {
			super(context);
			activityWR = new WeakReference<>(null);
		}

		@Nullable
		@Override
		public AlertDialog show() {
			Activity activity = this.activityWR.get();
			if (activity != null) {
				if (activity.isFinishing()) {
					return null;
				}
			}
			return super.show();
		}
	}
}
