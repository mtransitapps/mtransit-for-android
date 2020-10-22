package org.mtransit.android.dev;

import androidx.annotation.NonNull;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.commons.MTLog;

public class LeakCanaryDetector implements LeakDetector, MTLog.Loggable {

	private static final String LOG_TAG = LeakCanaryDetector.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Override
	public void setup(@NonNull IApplication application) {
		// DO NOTHING https://square.github.io/leakcanary
	}
}
