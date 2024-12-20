package org.mtransit.android.analytics;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.common.IActivity;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

// ENABLE:
// - DEBUG logging:
// adb shell setprop log.tag.FA VERBOSE
// adb shell setprop log.tag.FA-SVC VERBOSE
// - FASTER EVENTS
// adb shell setprop debug.firebase.analytics.app org.mtransit.android
//
// LOGCAT:
// adb logcat -v time -s FA FA-SVC
//
// DISABLE:
// - DEBUG logging:
// adb shell setprop log.tag.FA \"\"
// adb shell setprop log.tag.FA-SVC \"\"
// - FASTER EVENTS
// adb shell setprop debug.firebase.analytics.app .none.
//
// https://firebase.google.com/docs/analytics/debugview
public class AnalyticsManager implements IAnalyticsManager, MTLog.Loggable {

	private static final String LOG_TAG = AnalyticsManager.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final boolean ANALYTICS_ENABLED = true;
	// private static final boolean ANALYTICS_ENABLED = false; // DEBUG

	private static final boolean DEBUG = false;
	// private static final boolean DEBUG = true; // DEBUG

	@NonNull
	private final FirebaseAnalytics firebaseAnalytics;

	@Inject
	public AnalyticsManager(@NonNull @ApplicationContext Context appContext) {
		if (!ANALYTICS_ENABLED) {
			//noinspection ConstantConditions
			firebaseAnalytics = null;
			return;
		}
		firebaseAnalytics = FirebaseAnalytics.getInstance(appContext);
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
	public void logEvent(@NonNull @Size(min = 1L, max = 40L) String name) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		logEvent(name, null);
	}

	@Override
	public void logEvent(@NonNull @Size(min = 1L, max = 40L) String name,
						 @Nullable AnalyticsEventsParamsProvider params) {
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
	public void trackScreenView(@NonNull Trackable page) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		try {
			Bundle bundle = new Bundle();
			bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, page.getScreenName());
			bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, page.getClass().getSimpleName());
			firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while tracing screen view! (%s)", page);
		}
	}
}
