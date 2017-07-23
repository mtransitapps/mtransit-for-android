package org.mtransit.android.util;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TaskUtils;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

public final class AnalyticsUtils implements MTLog.Loggable {

	private static final String TAG = AnalyticsUtils.class.getSimpleName();

	public static final String USER_PROPERTY_OPEN_APP_COUNTS = "mt_app_open_counts";

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static boolean ANALYTICS_ENABLED = true;

	private static final boolean DEBUG = false;

	private static boolean LOCAL_ONLY = false;

	private static final boolean TRACKING_ADVERTISING_ID_COLLECTION = true;

	private static final boolean TRACKING_EXCEPTION_REPORTING = true;

	private static final int TRACKING_LOCAL_DISPATCH_PERIOD_IN_SEC = 30 * 60; // 30 minutes

	private static AnalyticsUtils instance;

	private AnalyticsUtils() {
	}

	public static AnalyticsUtils get() {
		if (instance == null) {
			instance = new AnalyticsUtils();
		}
		return instance;
	}

	private static Tracker tracker;

	private synchronized static Tracker getGoogleAnalyticsTracker(Context context) {
		if (!ANALYTICS_ENABLED) {
			return null;
		}
		if (tracker == null) {
			tracker = GoogleAnalytics.getInstance(context).newTracker(context.getString(R.string.google_analytics_id));
			tracker.enableAdvertisingIdCollection(TRACKING_ADVERTISING_ID_COLLECTION);
			tracker.enableExceptionReporting(TRACKING_EXCEPTION_REPORTING);
			GoogleAnalytics.getInstance(context).setLocalDispatchPeriod(TRACKING_LOCAL_DISPATCH_PERIOD_IN_SEC);
			if (DEBUG) {
				// adb shell setprop log.tag.GAv4 DEBUG
				// adb logcat -v time -s GAv4
				if (LOCAL_ONLY) {
					GoogleAnalytics.getInstance(context).setDryRun(true);
				}
			}
		}
		return tracker;
	}

	private static FirebaseAnalytics mFirebaseAnalytics;

	private synchronized static FirebaseAnalytics getFirebaseAnalytics(Context context) {
		if (!ANALYTICS_ENABLED) {
			return null;
		}
		if (mFirebaseAnalytics == null) {
			mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
			if (DEBUG) {
				// DEBUG adb shell setprop debug.firebase.analytics.app org.mtransit.android
				// DEBUG adb shell setprop log.tag.FA VERBOSE
				// DEBUG adb shell setprop log.tag.FA-SVC VERBOSE
				// DEBUG adb logcat -v time -s FA FA-SVC
			}
		}
		return mFirebaseAnalytics;
	}

	public static void trackEvent(Context context, final String category, final String action, final String label, final int value) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		new AsyncTask<Context, Void, Void>() {
			@Override
			protected Void doInBackground(Context... params) {
				try {
					Tracker gaTracker = getGoogleAnalyticsTracker(params[0]);
					if (gaTracker != null) {
						gaTracker.send(new HitBuilders.EventBuilder().setCategory(category).setAction(action).setLabel(label).setValue(value).build());
					}
				} catch (Exception e) {
					MTLog.w(TAG, e, "Error while tracing event (%s,%s,%s)!", action, label, value);
				}
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR, context);
	}

	public static void trackUserProperty(Activity activity, String name, String value) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		try {
			FirebaseAnalytics firebaseAnalytics = getFirebaseAnalytics(activity);
			if (firebaseAnalytics != null) {
				firebaseAnalytics.setUserProperty(name, value);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while tracing user property (%s:%s)", name, value);
		}
	}

	public static void trackScreenView(Activity activity, Trackable page) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		final String pageScreenName = page.getScreenName();
		new AsyncTask<Context, Void, Void>() {
			@Override
			protected Void doInBackground(Context... params) {
				try {
					Context context = params[0];
					if (!TextUtils.isEmpty(pageScreenName)) {
						Tracker gaTracker = getGoogleAnalyticsTracker(context);
						if (gaTracker != null) {
							gaTracker.setScreenName(pageScreenName);
							gaTracker.send(new HitBuilders.ScreenViewBuilder().build());
						}
					}
				} catch (Exception e) {
					MTLog.w(TAG, e, "Error while tracing screen view! (%s)", pageScreenName);
				}
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR, activity);
		try {
			FirebaseAnalytics firebaseAnalytics = getFirebaseAnalytics(activity);
			if (firebaseAnalytics != null) {
				firebaseAnalytics.setCurrentScreen(activity, pageScreenName, null);
			}
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while tracing screen view! (%s)", pageScreenName);
		}
	}

	public static void dispatch(Context context) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		try {
			GoogleAnalytics.getInstance(context).dispatchLocalHits();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while dispatching Google Analytics data.");
		}
	}

	public interface Trackable {
		String getScreenName();
	}
}
