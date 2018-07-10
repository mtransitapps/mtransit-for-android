package org.mtransit.android.dev;

import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.MTLog;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface CrashReporter {

	void setup(IContext context, boolean enabled);

	void reportNonFatal(@Nullable Throwable throwable);

	void reportNonFatal(@Nullable String msg, Object... args);

	void reportNonFatal(@Nullable Throwable throwable, @Nullable String msg, Object... args);

	void shouldNotHappen(@Nullable Throwable throwable) throws RuntimeException;

	void shouldNotHappen(@Nullable String msg, Object... args) throws RuntimeException;

	void shouldNotHappen(@Nullable Throwable throwable, @Nullable String msg, Object... args) throws RuntimeException;

	void w(@NonNull MTLog.Loggable loggable, String msg, Object... args);

	void w(String tag, String msg, Object... args);

	void w(@NonNull MTLog.Loggable loggable, Throwable t, String msg, Object... args);

	void w(String tag, Throwable t, String msg, Object... args);
}
