package org.mtransit.android.ui;

import org.mtransit.android.R;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.util.AnalyticsUtils;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class SplashScreenActivity extends MTActivity implements AnalyticsUtils.Trackable {

	private static final String LOG_TAG = SplashScreenActivity.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Splash";

	@NonNull
	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		setTheme(R.style.MTTheme);
		super.onCreate(savedInstanceState);
		int appOpenCounts = PreferenceUtils.getPrefDefault(this, //
				PreferenceUtils.PREF_USER_APP_OPEN_COUNTS, //
				PreferenceUtils.PREF_USER_APP_OPEN_COUNTS_DEFAULT);
		appOpenCounts++;
		PreferenceUtils.savePrefDefault(this, PreferenceUtils.PREF_USER_APP_OPEN_COUNTS, appOpenCounts, false); // asynchronous
		AnalyticsUtils.trackUserProperty(this, AnalyticsUtils.USER_PROPERTY_OPEN_APP_COUNTS, String.valueOf(appOpenCounts));
		AnalyticsUtils.trackScreenView(this, this);
		showHomeActivity();
	}

	private void showHomeActivity() {
		startActivity(MainActivity.newInstance(this));
		finish();
	}
}
