package org.mtransit.android.dev;

import org.mtransit.android.commons.Constants;

import android.os.StrictMode;

public class StrictModeImpl implements IStrictMode {

	@Override
	public void setup(boolean enabled) {
		if (Constants.FORCE_STRICT_MODE_OFF) {
			return;
		}
		if (enabled) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectAll()
					.penaltyLog()
					.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectAll()
					.penaltyLog()
					.build());
		}
	}
}
