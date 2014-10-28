package org.mtransit.android.util;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;

import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger.LogLevel;
import com.google.android.gms.analytics.Tracker;

public final class AnalyticsUtils implements MTLog.Loggable {

	private static final String TAG = AnalyticsUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static boolean DEBUG = false;

	private static boolean LOCAL_ONLY = DEBUG & false;

	private static boolean TRACKING_ENABLED = true;

	private static boolean TRACKING_ADVERTISING_ID_COLLECTION = true;

	private static boolean TRACKING_EXCEPTION_REPORTING = true;

	private static int TRACKING_LOCAL_DISPATCH_PERIOD_IN_SEC = 30 * 60; // 30 minutes

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

	private synchronized static Tracker getTracker(final Context context) {
		if (tracker == null) {
			tracker = GoogleAnalytics.getInstance(context).newTracker(context.getString(R.string.google_analytics_id));
			tracker.enableAdvertisingIdCollection(TRACKING_ADVERTISING_ID_COLLECTION);
			tracker.enableExceptionReporting(TRACKING_EXCEPTION_REPORTING);
			GoogleAnalytics.getInstance(context).setLocalDispatchPeriod(TRACKING_LOCAL_DISPATCH_PERIOD_IN_SEC);
			if (DEBUG) {
				GoogleAnalytics.getInstance(context).getLogger().setLogLevel(LogLevel.VERBOSE);
				if (LOCAL_ONLY) {
					GoogleAnalytics.getInstance(context).setDryRun(true);
				}
			}
		}
		return tracker;
	}

	public static void trackEvent(Context context, final String category, final String action, final String label, final int value) {
		if (TRACKING_ENABLED) {
			new AsyncTask<Context, Void, Void>() {
				@Override
				protected Void doInBackground(Context... params) {
					try {
						final Tracker gaTracker = getTracker(params[0]);
						gaTracker.send(new HitBuilders.EventBuilder().setCategory(category).setAction(action).setLabel(label).setValue(value).build());
					} catch (Throwable t) {
						MTLog.w(TAG, t, "Error while tracing event (%s,%s,%s)!", action, label, value);
					}
					return null;
				}
			}.execute(context);
		}
	}

	public static void trackScreenView(Context context, final Trackable page) {
		if (TRACKING_ENABLED) {
			new AsyncTask<Context, Void, Void>() {
				@Override
				protected Void doInBackground(Context... params) {
					try {
						final Tracker gaTracker = getTracker(params[0]);
						gaTracker.setScreenName(page.getScreenName());
						gaTracker.send(new HitBuilders.AppViewBuilder().build());
					} catch (Throwable t) {
						MTLog.w(TAG, t, "Error while tracing screen view! (%s)", page);
					}
					return null;
				}
			}.execute(context);
		}
	}

	public static void dispatch(Context context) {
		if (TRACKING_ENABLED) {
			try {
				GoogleAnalytics.getInstance(context).dispatchLocalHits();
			} catch (Throwable t) {
				MTLog.w(TAG, t, "Error while dispatching analytics data.");
			}
		}
	}

	public static interface Trackable {
		public String getScreenName();
	}
}
