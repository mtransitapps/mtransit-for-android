package org.mtransit.android.dev;

import android.content.Context;

import androidx.annotation.NonNull;

import org.mtransit.android.commons.MTLog;

import javax.inject.Inject;

public class LeakCanaryDetector implements LeakDetector, MTLog.Loggable {

	private static final String LOG_TAG = LeakCanaryDetector.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Inject
	public LeakCanaryDetector() {
		// DO NOTHING
	}

	@Override
	public void setup(@NonNull Context appContext) {
		// DO NOTHING https://square.github.io/leakcanary
	}
}
