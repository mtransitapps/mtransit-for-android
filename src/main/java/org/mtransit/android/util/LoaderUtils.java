package org.mtransit.android.util;

import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;

import org.mtransit.android.commons.MTLog;

public final class LoaderUtils implements MTLog.Loggable {

	private static final String LOG_TAG = LoaderUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@MainThread
	public static void restartLoader(@NonNull Fragment fragment, int loaderId, @Nullable Bundle args, @NonNull LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
		try {
			if (fragment.getActivity() == null) {
				return;
			}
			restartLoader(LoaderManager.getInstance(fragment), loaderId, args, loaderCallbacks);
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Error while restarting loader ID '%d' for '%s'!", loaderId, loaderCallbacks);
		}
	}

	@MainThread
	private static void restartLoader(@NonNull LoaderManager loaderManager, int loaderId, @Nullable Bundle args, @NonNull LoaderManager.LoaderCallbacks<?> loaderCallbacks) {
		try {
			//noinspection ConstantConditions
			if (loaderCallbacks == null) {
				return;
			}
			loaderManager.restartLoader(loaderId, args, loaderCallbacks);
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Error while restarting loader ID '%d' for '%s'!", loaderId, loaderCallbacks);
		}
	}

	@MainThread
	public static void destroyLoader(@NonNull Fragment fragment, int loaderId) {
		try {
			if (fragment.getActivity() == null) {
				return;
			}
			destroyLoader(LoaderManager.getInstance(fragment), loaderId);
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Error while destroying loader ID '%d'!", loaderId);
		}
	}

	@MainThread
	public static void destroyLoader(@NonNull LoaderManager loaderManager, int loaderId) {
		try {
			loaderManager.destroyLoader(loaderId);
		} catch (Exception e) {
			CrashUtils.w(LOG_TAG, e, "Error while destroying loader ID '%d'!", loaderId);
		}
	}
}
