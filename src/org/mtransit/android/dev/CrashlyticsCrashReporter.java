package org.mtransit.android.dev;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import io.fabric.sdk.android.Fabric;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.MTLog;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class CrashlyticsCrashReporter implements CrashReporter, MTLog.Loggable {

	private static final String LOG_TAG = CrashlyticsCrashReporter.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Override
	public void setup(IContext context, boolean enabled) {
		Fabric.with(context.getContext(), new Crashlytics.Builder()
				.core(new CrashlyticsCore.Builder()
						.disabled(!enabled)
						.build())
				.build());
	}

	@Override
	public void reportNonFatal(@Nullable Throwable throwable) {
		reportNonFatal(throwable, null);
	}

	@Override
	public void reportNonFatal(@Nullable String msg) {
		reportNonFatal(null, msg);
	}

	@Override
	public void reportNonFatal(@Nullable Throwable throwable, @Nullable String message) {
		try {
			Crashlytics.log(message);
			if (throwable == null) {
				throwable = new Exception(message);
			}
			Crashlytics.logException(throwable);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while reporting message '%s'!", message);
		}
	}

	@Override
	public void shouldNotHappen(@Nullable Throwable throwable) throws RuntimeException {
		shouldNotHappen(throwable, null);
	}

	@Override
	public void shouldNotHappen(@Nullable String msg) throws RuntimeException {
		shouldNotHappen(null, msg);
	}

	@Override
	public void shouldNotHappen(@Nullable Throwable throwable, @Nullable String msg) throws RuntimeException {
		if (BuildConfig.DEBUG) {
			if (msg == null) {
				msg = "No error message";
			}
			if (throwable == null) {
				throwable = new Exception(msg);
			}
			throw new RuntimeException(msg, throwable);
		}
		reportNonFatal(throwable, msg);
	}

	@Override
	public void w(@NonNull MTLog.Loggable loggable, String msg, Object... args) {
		w(loggable.getLogTag(), msg, args);
	}

	@Override
	public void w(String tag, String msg, Object... args) {
		MTLog.w(tag, msg, args);
		reportNonFatal(String.format(msg, args));
	}

	@Override
	public void w(@NonNull MTLog.Loggable loggable, Throwable t, String msg, Object... args) {
		w(loggable.getLogTag(), t, msg, args);
	}

	@Override
	public void w(String tag, Throwable t, String msg, Object... args) {
		MTLog.w(tag, t, msg, args);
		reportNonFatal(t, String.format(msg, args));
	}
}
