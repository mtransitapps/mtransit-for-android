package org.mtransit.android.dev;

import org.mtransit.android.common.IApplication;

import androidx.annotation.NonNull;

public interface LeakDetector {

	void setup(@NonNull IApplication application);
}
