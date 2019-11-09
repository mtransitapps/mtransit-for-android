package org.mtransit.android.dev;

import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.MTLog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface CrashReporter {

	void setup(@NonNull IContext context, boolean enabled);

	void reportNonFatal(@Nullable Throwable throwable);

	void reportNonFatal(@Nullable String msg, @Nullable Object... args);

	void reportNonFatal(@Nullable Throwable throwable, @Nullable String msg, @Nullable Object... args);

	void shouldNotHappen(@Nullable Throwable throwable) throws RuntimeException;

	void shouldNotHappen(@Nullable String msg, @Nullable Object... args) throws RuntimeException;

	void shouldNotHappen(@Nullable Throwable throwable, @Nullable String msg, @Nullable Object... args) throws RuntimeException;

	void w(@NonNull MTLog.Loggable loggable, String msg, @Nullable Object... args);

	void w(String tag, String msg, @Nullable Object... args);

	void w(@NonNull MTLog.Loggable loggable, @Nullable Throwable t, String msg, @Nullable Object... args);

	void w(String tag, @Nullable Throwable t, String msg, @Nullable Object... args);
}
