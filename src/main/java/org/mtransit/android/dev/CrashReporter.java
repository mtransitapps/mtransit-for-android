package org.mtransit.android.dev;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.MTLog;

@SuppressWarnings("unused")
public interface CrashReporter {

	boolean CRASHLYTICS_ENABLED = true;
	// boolean CRASHLYTICS_ENABLED = false; // DEBUG

	void setup(@NonNull IContext context, boolean enabled);

	void reportNonFatal(@Nullable Throwable throwable);

	void reportNonFatal(@Nullable String msg, @NonNull Object... args);

	void reportNonFatal(@Nullable Throwable throwable, @Nullable String msg, @NonNull Object... args);

	void shouldNotHappen(@Nullable Throwable throwable) throws RuntimeException;

	void shouldNotHappen(@Nullable String msg, @NonNull Object... args) throws RuntimeException;

	void shouldNotHappen(@Nullable Throwable throwable, @Nullable String msg, @NonNull Object... args) throws RuntimeException;

	void w(@NonNull MTLog.Loggable loggable, @Nullable String msg, @NonNull Object... args);

	void w(@NonNull String tag, @Nullable String msg, @NonNull Object... args);

	void w(@NonNull MTLog.Loggable loggable, @Nullable Throwable t, @Nullable String msg, @NonNull Object... args);

	void w(@NonNull String tag, @Nullable Throwable t, @Nullable String msg, @NonNull Object... args);
}
