package org.mtransit.android.analytics;

import androidx.annotation.NonNull;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.common.IActivity;

import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsManager implements IAnalyticsManager, MTLog.Loggable {

	private static final String LOG_TAG = AnalyticsManager.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static boolean ANALYTICS_ENABLED = true;

	private static final boolean DEBUG = false;

	@NonNull
	private final FirebaseAnalytics firebaseAnalytics;

	public AnalyticsManager(@NonNull IApplication application) {
		firebaseAnalytics = FirebaseAnalytics.getInstance(application.requireContext());
		if (DEBUG) {
			// DEBUG adb shell setprop debug.firebase.analytics.app org.mtransit.android
			// DEBUG adb shell setprop log.tag.FA VERBOSE
			// DEBUG adb shell setprop log.tag.FA-SVC VERBOSE
			// DEBUG adb logcat -v time -s FA FA-SVC
		}
	}

	@Override
	public void trackUserProperty(@NonNull String name, @NonNull String value) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		try {
			firebaseAnalytics.setUserProperty(name, value);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while tracing user property (%s:%s)", name, value);
		}
	}

	@Override
	public void trackScreenView(@NonNull IActivity activity, @NonNull Trackable page) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		final String pageScreenName = page.getScreenName();
		try {
			firebaseAnalytics.setCurrentScreen(activity.requireActivity(), pageScreenName, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while tracing screen view! (%s)", pageScreenName);
		}
	}
}
