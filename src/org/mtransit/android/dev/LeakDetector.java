package org.mtransit.android.dev;

import org.mtransit.android.common.IApplication;

public interface LeakDetector {
	boolean isInAnalyzerProcess(IApplication application);

	void setup(IApplication application);
}
