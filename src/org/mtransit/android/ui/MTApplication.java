package org.mtransit.android.ui;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.ad.AdManager;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.analytics.AnalyticsProperties;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.common.IApplication;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.IStrictMode;
import org.mtransit.android.dev.LeakDetector;
import org.mtransit.android.di.Injection;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MTApplication extends Application implements IApplication, MTLog.Loggable {

	private static final String LOG_TAG = MTApplication.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@SuppressWarnings("NullableProblems") // late-init
	@NonNull
	private static IApplication application;

	@Nullable
	private LeakDetector leakDetector = null;
	@Nullable
	private CrashReporter crashReporter = null;
	@Nullable
	private IStrictMode strictMode = null;
	@Nullable
	private IAdManager adManager = null;
	@Nullable
	private IAnalyticsManager analyticsManager = null;

	@Override
	public void onCreate() {
		super.onCreate();
		if (BuildConfig.DEBUG) {
			if (getLeakDetector().isInAnalyzerProcess(this)) {
				return; // this process is dedicated to Leak detector, skip app init
			}
			getLeakDetector().setup(this);
		}
		if (AdManager.isInAdsProcess(this)) {
			return;
		}
		application = this;
		getStrictMode().setup(BuildConfig.DEBUG);
		getCrashReporter().setup(this, !BuildConfig.DEBUG);
		getAdManager().init(this);
		getAnalyticsManager().trackUserProperty(AnalyticsProperties.DEVICE_MANUFACTURER, Build.MANUFACTURER);
	}

	@NonNull
	public static IApplication getIApplication() {
		return application;
	}

	@NonNull
	public LeakDetector getLeakDetector() {
		if (this.leakDetector == null) {
			this.leakDetector = Injection.providesLeakDetector();
		}
		return this.leakDetector;
	}

	@NonNull
	public IStrictMode getStrictMode() {
		if (this.strictMode == null) {
			this.strictMode = Injection.providesStrictMode();
		}
		return this.strictMode;
	}

	@NonNull
	private CrashReporter getCrashReporter() {
		if (this.crashReporter == null) {
			this.crashReporter = Injection.providesCrashReporter();
		}
		return this.crashReporter;
	}

	@NonNull
	private IAdManager getAdManager() {
		if (this.adManager == null) {
			this.adManager = Injection.providesAdManager();
		}
		return this.adManager;
	}

	@NonNull
	private IAnalyticsManager getAnalyticsManager() {
		if (this.analyticsManager == null) {
			this.analyticsManager = Injection.providesAnalyticsManager();
		}
		return this.analyticsManager;
	}

	@NonNull
	@Override
	public Application getApplication() {
		return this;
	}

	@NonNull
	@Override
	public Application requireApplication() throws IllegalStateException {
		return this;
	}

	@NonNull
	@Override
	public Context getContext() {
		return getApplicationContext();
	}

	@NonNull
	@Override
	public Context requireContext() throws IllegalStateException {
		return getApplicationContext();
	}
}
