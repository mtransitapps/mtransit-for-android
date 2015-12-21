package org.mtransit.android.util;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TaskUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public final class AnalyticsUtils implements MTLog.Loggable {

	private static final String TAG = AnalyticsUtils.class.getSimpleName();

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

	private synchronized static Tracker getTracker(Context context) {
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

	public static void trackEvent(Context context, final String category, final String action, final String label, final int value) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		new AsyncTask<Context, Void, Void>() {
			@Override
			protected Void doInBackground(Context... params) {
				try {
					Tracker gaTracker = getTracker(params[0]);
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

	public static void trackScreenView(Context context, Trackable page) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		new AsyncTask<Object, Void, Void>() {
			@Override
			protected Void doInBackground(Object... params) {
				try {
					Context context = (Context) params[0];
					String pageScreenName = (String) params[1];
					if (!TextUtils.isEmpty(pageScreenName)) {
						Tracker gaTracker = getTracker(context);
						if (gaTracker != null) {
							gaTracker.setScreenName(pageScreenName);
							gaTracker.send(new HitBuilders.ScreenViewBuilder().build());
						}
					}
				} catch (Exception e) {
					MTLog.w(TAG, e, "Error while tracing screen view! (%s)", params);
				}
				return null;
			}
		}.executeOnExecutor(TaskUtils.THREAD_POOL_EXECUTOR, context, page.getScreenName());
	}

	public static void dispatch(Context context) {
		if (!ANALYTICS_ENABLED) {
			return;
		}
		try {
			GoogleAnalytics.getInstance(context).dispatchLocalHits();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while dispatching analytics data.");
		}
	}

	public interface Trackable {
		public String getScreenName();
	}
}
