package org.mtransit.android.util;

import org.mtransit.android.commons.MTLog;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;

public final class LoaderUtils implements MTLog.Loggable {

	private static final String LOG_TAG = LoaderUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void restartLoader(@NonNull Fragment fragment, int loaderId, @Nullable Bundle args, @NonNull LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
		try {
			if (fragment.getActivity() == null) {
				return;
			}
			restartLoader(LoaderManager.getInstance(fragment), loaderId, args, loaderCallbacks);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while restarting loader ID '%s' for '%s'", loaderId, loaderCallbacks);
		}
	}

	private static void restartLoader(@NonNull LoaderManager loaderManager, int loaderId, @Nullable Bundle args, @NonNull LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
		try {
			//noinspection ConstantConditions
			if (loaderCallbacks == null) {
				return;
			}
			loaderManager.restartLoader(loaderId, args, loaderCallbacks);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while restarting loader ID '%s' for '%s'", loaderId, loaderCallbacks);
		}
	}
}
