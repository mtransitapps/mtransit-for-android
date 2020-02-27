package org.mtransit.android.dev;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.MTLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class CrashlyticsCrashReporter implements CrashReporter, MTLog.Loggable {

	private static final String LOG_TAG = CrashlyticsCrashReporter.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Override
	public void setup(@NonNull IContext context, boolean enabled) {
		Fabric.with(context.requireContext(), new Crashlytics.Builder()
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
	public void reportNonFatal(@Nullable String msg, @NonNull Object... args) {
		reportNonFatal(null, msg, args);
	}

	@Override
	public void reportNonFatal(@Nullable Throwable throwable, @Nullable String message, @NonNull Object... args) {
		try {
			String fMessage = message == null ? null : String.format(message, args);
			Crashlytics.log(fMessage);
			if (throwable == null) {
				throwable = new NoException(fMessage);
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
	public void shouldNotHappen(@Nullable String msg, @NonNull Object... args) throws RuntimeException {
		shouldNotHappen(null, msg, args);
	}

	@Override
	public void shouldNotHappen(@Nullable Throwable throwable, @Nullable String msg, @NonNull Object... args) throws RuntimeException {
		if (BuildConfig.DEBUG) {
			if (msg == null) {
				msg = "No error message";
			} else {
				msg = String.format(msg, args);
			}
			if (throwable == null) {
				throwable = new Exception(msg);
			}
			throw new RuntimeException(msg, throwable);
		}
		reportNonFatal(throwable, msg);
	}

	@Override
	public void w(@NonNull MTLog.Loggable loggable, @Nullable String msg, @NonNull Object... args) {
		w(loggable.getLogTag(), msg, args);
	}

	@Override
	public void w(@NonNull String tag, @Nullable String msg, @NonNull Object... args) {
		MTLog.w(tag, msg, args);
		reportNonFatal(msg, args);
	}

	@Override
	public void w(@NonNull MTLog.Loggable loggable, @Nullable Throwable t, @Nullable String msg, @NonNull Object... args) {
		w(loggable.getLogTag(), t, msg, args);
	}

	@Override
	public void w(@NonNull String tag, @Nullable Throwable t, @Nullable String msg, @NonNull Object... args) {
		MTLog.w(tag, t, msg, args);
		reportNonFatal(t, msg, args);
	}

	public static class NoException extends Exception implements MTLog.Loggable {

		private static final String LOG_TAG = NoException.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final List<String> USELESS_CLASSES = Arrays.asList(
				"org.mtransit.android.dev.CrashlyticsCrashReporter",
				"org.mtransit.android.util.CrashUtils"
		);

		@Nullable
		private StackTraceElement[] customStackTrace = null;

		public NoException(@Nullable String message) {
			super(message);
		}

		@Nullable
		@Override
		public synchronized Throwable getCause() {
			try {
				Throwable cause = super.getCause();
				if (cause != null) {
					cause.setStackTrace(getStackTrace());
				}
				return cause;
			} catch (Exception e) {
				android.util.Log.w(LOG_TAG, "Error while setting cause stacktrace!", e);
				return super.getCause();
			}
		}

		@NonNull
		@Override
		public StackTraceElement[] getStackTrace() {
			if (customStackTrace == null) {
				initCustomStackTrace();
			}
			return customStackTrace;
		}

		private void initCustomStackTrace() {
			StackTraceElement[] stackTrace = super.getStackTrace();
			List<StackTraceElement> customStackTraceList = new ArrayList<>(Arrays.asList(stackTrace));
			try {
				Iterator<StackTraceElement> it = customStackTraceList.iterator();
				while (it.hasNext()) {
					StackTraceElement stackTraceElement = it.next();
					if (USELESS_CLASSES.contains(stackTraceElement.getClassName())) {
						it.remove();
					} else {
						break;
					}
				}
			} catch (Exception e) {
				android.util.Log.w(LOG_TAG, "Error while initializing stacktrace!", e);
			}
			customStackTrace = customStackTraceList.toArray(new StackTraceElement[0]);
		}
	}
}
