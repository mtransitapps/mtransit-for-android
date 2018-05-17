package org.mtransit.android.dev;

import com.squareup.leakcanary.LeakCanary;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.commons.MTLog;

public class LeakCanaryDetector implements LeakDetector, MTLog.Loggable {

	private static final String LOG_TAG = LeakCanaryDetector.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Override
	public boolean isInAnalyzerProcess(IApplication application) {
		try {
			return LeakCanary.isInAnalyzerProcess(application.getContext());
		} catch (Exception e) {
			MTLog.w(this, "Error while initializing LeakCanary!", e);
			return false;
		}
	}

	@Override
	public void setup(IApplication application) {
		try {
			LeakCanary.install(application.getApplication());
		} catch (Exception e) {
			MTLog.w(this, "Error while initializing LeakCanary!", e);
		}
	}
}
