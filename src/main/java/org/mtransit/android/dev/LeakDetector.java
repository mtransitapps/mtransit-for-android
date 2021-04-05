package org.mtransit.android.dev;

import androidx.annotation.NonNull;

import org.mtransit.android.common.IApplication;

public interface LeakDetector {

	void setup(@NonNull IApplication application);
}
