package org.mtransit.android.analytics;

import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.common.IActivity;

import java.util.Map;

// - To enable debug logging run:
// adb shell setprop log.tag.FA VERBOSE
// adb shell setprop log.tag.FA-SVC VERBOSE
// - To enable faster debug mode event logging run:
// adb shell setprop debug.firebase.analytics.app org.mtransit.android
// - To view the logs:
// adb logcat -v time -s FA FA-SVC
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
		if (!ANALYTICS_ENABLED) {
			//noinspection ConstantConditions
			firebaseAnalytics = null;
			return;
		}
		firebaseAnalytics = FirebaseAnalytics.getInstance(application.requireContext());
		if (DEBUG) {
			// DEBUG adb shell setprop debug.firebase.analytics.app org.mtransit.android
			// DEBUG adb shell setprop log.tag.FA VERBOSE
			// DEBUG adb shell setprop log.tag.FA-SVC VERBOSE
			// DEBUG adb logcat -v time -s FA FA-SVC
		}
	}

	@Override
	public void setUserProperty(@NonNull @Size(min = 1L, max = 24L) String name,
								int value) {
		setUserProperty(name, String.valueOf(value));
	}

	@Override
	public void setUserProperty(@NonNull @Size(min = 1L, max = 24L) String name,
								@NonNull String value) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		try {
			firebaseAnalytics.setUserProperty(name, value);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while tracing user property (%s:%s)", name, value);
		}
	}

	@Override
	public void logEvent(@NonNull String name) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		logEvent(name, null);
	}

	@Override
	public void logEvent(@NonNull String name, @Nullable AnalyticsEventsParamsProvider params) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		Bundle bundle = null;
		if (params != null) {
			Map<String, Object> paramMap = params.to();
			bundle = new Bundle();
			for (Map.Entry<String, Object> param : paramMap.entrySet()) {
				// Firebase: "String, long and double param types are supported."
				if (param.getValue() instanceof String) {
					bundle.putString(param.getKey(), (String) param.getValue());
				} else if (param.getValue() instanceof Long) {
					bundle.putLong(param.getKey(), (Long) param.getValue());
				} else if (param.getValue() instanceof Double) {
					bundle.putDouble(param.getKey(), (Double) param.getValue());
				} else {
					MTLog.w(this, "Unexpected event parameter type for '" + param.getKey() + "'>'" + param.getValue() + "'!");
				}
			}
		}
		this.firebaseAnalytics.logEvent(name, bundle);
	}

	@MainThread
	@Override
	public void trackScreenView(@NonNull IActivity activity, @NonNull Trackable page) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		try {
			firebaseAnalytics.setCurrentScreen(activity.requireActivity(), page.getScreenName(), null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while tracing screen view! (%s)", page);
		}
	}
}
