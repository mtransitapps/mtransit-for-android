package org.mtransit.android.ui;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.analytics.AnalyticsUserProperties;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.IStrictMode;
import org.mtransit.android.dev.LeakDetector;
import org.mtransit.android.util.NightModeUtils;
import org.mtransit.commons.CommonsApp;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MTApplication extends Application implements MTLog.Loggable {

	private static final String LOG_TAG = MTApplication.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Inject
	LeakDetector leakDetector;
	@Inject
	CrashReporter crashReporter;
	@Inject
	IStrictMode strictMode;
	@Inject
	IAnalyticsManager analyticsManager;

	@Override
	protected void attachBaseContext(@NonNull Context newBase) {
		// Demo mode: cannot access manager instance before application context is attached => set in activities
		newBase = LocaleUtils.fixDefaultLocale(newBase);
		super.attachBaseContext(newBase);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		CommonsApp.setup(true);
		if (BuildConfig.DEBUG) {
			this.leakDetector.setup(this);
		}
		NightModeUtils.setDefaultNightMode(this, null);
		this.strictMode.setup(BuildConfig.DEBUG);
		this.crashReporter.setup(!BuildConfig.DEBUG);
		this.analyticsManager.setUserProperty(AnalyticsUserProperties.DEVICE_MANUFACTURER, Build.MANUFACTURER);
	}
}
