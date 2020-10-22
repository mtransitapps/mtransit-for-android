package org.mtransit.android.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.dev.CrashlyticsCrashReporter;

@Deprecated
@SuppressWarnings("WeakerAccess")
public final class CrashUtils implements MTLog.Loggable {

	private static final String LOG_TAG = CrashUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void report(@NonNull String message) {
		report(null, message);
	}

	@Deprecated
	public static void report(@Nullable Throwable throwable, @NonNull String message) {
		try {
			final com.google.firebase.crashlytics.FirebaseCrashlytics firebaseCrashlytics
					= com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance();
			firebaseCrashlytics.log(message);
			if (throwable == null) {
				throwable = new CrashlyticsCrashReporter.NoException(message);
			}
			firebaseCrashlytics.recordException(throwable);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while reporting message '%s'!", message);
		}
	}

	public static void w(@NonNull MTLog.Loggable loggable, @NonNull String msg, @NonNull Object... args) {
		w(loggable.getLogTag(), msg, args);
	}

	public static void w(@NonNull String tag, @NonNull String msg, @NonNull Object... args) {
		MTLog.w(tag, msg, args);
		report(String.format(msg, args));
	}

	public static void w(@NonNull MTLog.Loggable loggable, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		w(loggable.getLogTag(), t, msg, args);
	}

	public static void w(@NonNull String tag, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args) {
		MTLog.w(tag, t, msg, args);
		report(t, String.format(msg, args));
	}
}
