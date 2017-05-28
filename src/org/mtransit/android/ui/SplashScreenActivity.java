package org.mtransit.android.ui;

import org.mtransit.android.util.AnalyticsUtils;

import android.os.Bundle;

public class SplashScreenActivity extends MTActivity implements AnalyticsUtils.Trackable {

	private static final String TAG = SplashScreenActivity.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Splash";

	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AnalyticsUtils.trackScreenView(this, this);
		showHomeActivity();
	}

	private void showHomeActivity() {
		startActivity(MainActivity.newInstance(this));
		finish();
	}
}
