package org.mtransit.android.dev;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;

@SuppressWarnings("unused")
public interface CrashReporter {

	boolean CRASHLYTICS_ENABLED = true;
	// boolean CRASHLYTICS_ENABLED = false; // DEBUG

	void setup(boolean enabled);

	void reportNonFatal(@Nullable Throwable throwable);

	void reportNonFatal(@NonNull String msg, @NonNull Object... args);

	void reportNonFatal(@Nullable Throwable throwable, @NonNull String msg, @NonNull Object... args);

	void shouldNotHappen(@Nullable Throwable throwable) throws RuntimeException;

	void shouldNotHappen(@NonNull String msg, @NonNull Object... args) throws RuntimeException;

	void shouldNotHappen(@Nullable Throwable throwable, @NonNull String msg, @NonNull Object... args) throws RuntimeException;

	void w(@NonNull MTLog.Loggable loggable, @NonNull String msg, @NonNull Object... args);

	void w(@NonNull String tag, @NonNull String msg, @NonNull Object... args);

	void w(@NonNull MTLog.Loggable loggable, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args);

	void w(@NonNull String tag, @Nullable Throwable t, @NonNull String msg, @NonNull Object... args);

	void log(@NonNull String message);
}
