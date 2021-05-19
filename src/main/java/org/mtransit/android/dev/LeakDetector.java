package org.mtransit.android.dev;

import android.content.Context;

import androidx.annotation.NonNull;

public interface LeakDetector {

	void setup(@NonNull Context appContext);
}
