package org.mtransit.android.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.analytics.AnalyticsUserProperties;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.di.Injection;
import org.mtransit.android.ui.view.common.IActivity;

public class SplashScreenActivity extends MTActivity implements IActivity, IAnalyticsManager.Trackable {

	private static final String LOG_TAG = SplashScreenActivity.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Splash";

	@NonNull
	private final IAnalyticsManager analyticsManager;

	public SplashScreenActivity() {
		super();
		analyticsManager = Injection.providesAnalyticsManager();
	}

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
		// com.google.android.ads.mediationtestsuite.MediationTestSuite.launch(this);
		// return;
		// }
		startActivity(MainActivity.newInstance(this));
		finish();
	}

	@NonNull
	@Override
	public Context getContext() {
		return this;
	}

	@NonNull
	@Override
	public Context requireContext() throws IllegalStateException {
		return this;
	}

	@NonNull
	@Override
	public Activity getActivity() {
		return this;
	}

	@NonNull
	@Override
	public Activity requireActivity() throws IllegalStateException {
		return this;
	}
}
