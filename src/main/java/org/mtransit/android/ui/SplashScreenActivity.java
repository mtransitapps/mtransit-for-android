package org.mtransit.android.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.analytics.AnalyticsUserProperties;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.ui.view.common.IActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashScreenActivity extends MTActivity implements IActivity, IAnalyticsManager.Trackable {

	private static final String LOG_TAG = SplashScreenActivity.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Splash";

	@Inject
	IAnalyticsManager analyticsManager;

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
		analyticsManager.setUserProperty(AnalyticsUserProperties.OPEN_APP_COUNTS, appOpenCounts);
		analyticsManager.trackScreenView(this, this);
		showHomeActivity();
	}

	private void showHomeActivity() {
		// if (true) {
		// // ADD tools:replace="android:supportsRtl" to AndroidManifest.xml <application />
		// // USE real Google Ads app ID & unit IDs in keys.xml
		// com.google.android.ads.mediationtestsuite.MediationTestSuite.launch(this);
		// finish();
		// return;
		// }
		startActivity(MainActivity.newInstance(this));
		finish();
	}
}
