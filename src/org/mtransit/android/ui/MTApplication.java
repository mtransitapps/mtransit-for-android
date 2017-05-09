package org.mtransit.android.ui;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.util.AdsUtils;

import android.app.Application;

public class MTApplication extends Application implements MTLog.Loggable {

	private static final String TAG = MTApplication.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		AdsUtils.init(this);
	}
}
