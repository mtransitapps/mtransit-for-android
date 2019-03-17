package org.mtransit.android.dev;

import com.squareup.leakcanary.LeakCanary;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.commons.MTLog;

import android.support.annotation.NonNull;

public class LeakCanaryDetector implements LeakDetector, MTLog.Loggable {

	private static final String LOG_TAG = LeakCanaryDetector.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Override
	public boolean isInAnalyzerProcess(@NonNull IApplication application) {
		try {
			return LeakCanary.isInAnalyzerProcess(application.requireContext());
		} catch (Exception e) {
			MTLog.w(this, "Error while initializing LeakCanary!", e);
			return false;
		}
	}

	@Override
	public void setup(@NonNull IApplication application) {
		try {
			LeakCanary.install(application.requireApplication());
		} catch (Exception e) {
			MTLog.w(this, "Error while initializing LeakCanary!", e);
		}
	}
}
