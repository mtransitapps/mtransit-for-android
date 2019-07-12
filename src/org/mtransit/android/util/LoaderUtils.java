package org.mtransit.android.util;

import org.mtransit.android.commons.MTLog;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;

public final class LoaderUtils implements MTLog.Loggable {

	private static final String TAG = LoaderUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void restartLoader(Fragment fragment, int loaderId, Bundle args, LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
		try {
			if (fragment == null) {
				return;
			}
			if (fragment.getActivity() == null) {
				return;
			}
			restartLoader(fragment.getLoaderManager(), loaderId, args, loaderCallbacks);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while restarting loader ID '%s' for '%s'", loaderId, loaderCallbacks);
		}
	}

	private static void restartLoader(LoaderManager loaderManager, int loaderId, Bundle args, LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
		try {
			if (loaderManager == null) {
				return;
			}
			if (loaderCallbacks == null) {
				return;
			}
			loaderManager.restartLoader(loaderId, args, loaderCallbacks);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while restarting loader ID '%s' for '%s'", loaderId, loaderCallbacks);
		}
	}
}
