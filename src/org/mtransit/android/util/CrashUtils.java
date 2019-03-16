package org.mtransit.android.util;

import android.support.annotation.NonNull;
import org.mtransit.android.commons.MTLog;

import android.support.annotation.Nullable;

@Deprecated
@SuppressWarnings("WeakerAccess")
public final class CrashUtils implements MTLog.Loggable {

	private static final String TAG = CrashUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void report(String message) {
		report(null, message);
	}

	@Deprecated
	public static void report(@Nullable Throwable throwable, String message) {
		try {
			com.crashlytics.android.Crashlytics.log(message);
			if (throwable == null) {
				throwable = new Exception(message);
			}
			com.crashlytics.android.Crashlytics.logException(throwable);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while reporting message '%s'!", message);
		}
	}

	public static void w(@NonNull MTLog.Loggable loggable, String msg, Object... args) {
		w(loggable.getLogTag(), msg, args);
	}

	public static void w(String tag, String msg, Object... args) {
		MTLog.w(tag, msg, args);
		report(String.format(msg, args));
	}

	public static void w(@NonNull MTLog.Loggable loggable, Throwable t, String msg, Object... args) {
		w(loggable.getLogTag(), t, msg, args);
	}

	public static void w(String tag, Throwable t, String msg, Object... args) {
		MTLog.w(tag, t, msg, args);
		report(t, String.format(msg, args));
	}
}
