package org.mtransit.android.util;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.ui.MTActivityWithLocation;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public final class AdsUtils implements MTLog.Loggable {

	private static final String TAG = AdsUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static boolean AD_ENABLED = true;

	private static final boolean DEBUG = false;

	private static Boolean showingAds = null;


	private static Boolean adLoaded = null;

	private static final HashSet<String> KEYWORDS = new HashSet<String>(Arrays.asList(new String[] { "transit", "transport", "bus", "subway", "metro", "taxi",
			"bike", "sharing", "velo", "train" }));

	private static void setupAd(Activity activity) {
		if (!AD_ENABLED) {
			return;
		}
		if (!isShowingAds(activity)) {
			return;
		}
		if (setupAdTask == null) {
			setupAdTask = new SetupAdTask(activity);
			setupAdTask.execute();
		}
	}

	private static SetupAdTask setupAdTask;

	private static class SetupAdTask extends MTAsyncTask<Void, Void, Boolean> {

		private static final String TAG = AdsUtils.class.getSimpleName() + ">" + SetupAdTask.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private WeakReference<Activity> activityWR;

		public SetupAdTask(Activity activity) {
			this.activityWR = new WeakReference<Activity>(activity);
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			if (!AD_ENABLED) {
				return false;
			}
			Activity activity = this.activityWR == null ? null : this.activityWR.get();
			return !isCancelled() && isShowingAds(activity);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Activity activity = this.activityWR == null ? null : this.activityWR.get();
			if (activity != null && result && !isCancelled()) { // show ads
				View adLayout = activity.findViewById(R.id.ad_layout);
				if (adLayout != null) {
					AdView adView = (AdView) adLayout.findViewById(R.id.ad);
					if (adView != null) {
						adView.setAdListener(new MTAdListener(activity));
						AdRequest.Builder adRequestBd = new AdRequest.Builder();
						if (activity instanceof MTActivityWithLocation) {
							adRequestBd.setLocation(((MTActivityWithLocation) activity).getLastLocation());
						}
						for (String keyword : KEYWORDS) {
							adRequestBd.addKeyword(keyword);
						}
						if (DEBUG) {
							adRequestBd.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
							for (String deviceId : activity.getResources().getStringArray(R.array.google_ads_test_devices_ids)) {
								adRequestBd.addTestDevice(deviceId);
							}
						}
						adView.loadAd(adRequestBd.build());
					}
				}
			} else { // hide ads
				hideAds(activity);
			}
		}
	}

	private static class MTAdListener extends AdListener {

		private WeakReference<Activity> activityWR;

		public MTAdListener(Activity activity) {
			this.activityWR = new WeakReference<Activity>(activity);
		}

		@Override
		public void onAdFailedToLoad(int errorCode) {
			super.onAdFailedToLoad(errorCode);
			switch (errorCode) {
			case AdRequest.ERROR_CODE_INTERNAL_ERROR:
				MTLog.w(TAG, "Failed to received ad! Internal error code: '%s'.", errorCode);
				break;
			case AdRequest.ERROR_CODE_INVALID_REQUEST:
				MTLog.w(TAG, "Failed to received ad! Invalid request error code: '%s'.", errorCode);
				break;
			case AdRequest.ERROR_CODE_NETWORK_ERROR:
				MTLog.w(TAG, "Failed to received ad! Network error code: '%s'.", errorCode);
				break;
			case AdRequest.ERROR_CODE_NO_FILL:
				MTLog.w(TAG, "Failed to received ad! No fill error code: '%s'.", errorCode);
				break;
			default:
				MTLog.w(TAG, "Failed to received ad! Error code: '%s'.", errorCode);
			}
			adLoaded = null;
			hideAds(this.activityWR == null ? null : this.activityWR.get());
		}

		@Override
		public void onAdLoaded() {
			super.onAdLoaded();
			adLoaded = true;
			showAdsIfEnoughSpace(this.activityWR == null ? null : this.activityWR.get());
		}

		@Override
		public void onAdClosed() {
			super.onAdClosed();
		}

		@Override
		public void onAdLeftApplication() {
			super.onAdLeftApplication();
		}

		@Override
		public void onAdOpened() {
			super.onAdOpened();
		}
	}

	public static void adaptToScreenSize(Activity activity, Configuration configuration) {
		if (!AD_ENABLED) {
			return;
		}
		if (!isShowingAds(activity)) {
			return;
		}
		if (isEnoughSpace(configuration)) {
			if (adLoaded != null && adLoaded) {
				resumeAd(activity);
				showAds(activity);
			}
		} else {
			hideAds(activity);
			pauseAd(activity);
		}
	}

	private static boolean isEnoughSpace(Configuration configuration) {
		if (configuration == null) {
			return false;
		}
		if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
			return true;
		}
		int sizeMask = configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		boolean smallScreen = sizeMask == Configuration.SCREENLAYOUT_SIZE_SMALL || sizeMask == Configuration.SCREENLAYOUT_SIZE_NORMAL;
		return !smallScreen;
	}

	private static void showAdsIfEnoughSpace(Activity activity) {
		MTLog.v(TAG, "showAdsIfEnoughSpace(%s)", activity);
		if (activity != null) {
			adaptToScreenSize(activity, activity.getResources().getConfiguration());
		}
	}

	private static void showAds(Activity activity) {
		View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null && adView.getVisibility() != View.VISIBLE) {
				adView.setVisibility(View.VISIBLE);
			}
			if (adLayout.getVisibility() != View.VISIBLE) {
				adLayout.setVisibility(View.VISIBLE);
			}
		}
	}

	private static void hideAds(Activity activity) {
		View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adLayout.getVisibility() != View.GONE) {
				adLayout.setVisibility(View.GONE);
			}
			if (adView != null && adView.getVisibility() != View.GONE) {
				adView.setVisibility(View.GONE);
			}
		}
	}

	public static void pauseAd(Activity activity) {
		if (!AD_ENABLED) {
			return;
		}
		if (!isShowingAds(activity)) {
			return;
		}
		View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.pause();
			}
		}
	}

	public static void resumeAd(Activity activity) {
		if (!AD_ENABLED) {
			return;
		}
		if (!isShowingAds(activity)) {
			return;
		}
		View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.resume();
			}
		}
	}

	public static void destroyAd(Activity activity) {
		if (!AD_ENABLED) {
			return;
		}
		if (!isShowingAds(activity)) {
			return;
		}
		View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.destroy();
				adLoaded = null;
				if (setupAdTask != null) {
					setupAdTask.cancel(true);
				}
				setupAdTask = null;
			}
		}
	}

	private static final int MIN_AGENCIES_FOR_ADS = 2;

	private static Integer nbAgencies = null;

	public static void onModulesUpdated(Activity activity) {
		nbAgencies = null; // reset
		refreshAdStatus(activity);
	}

	private static boolean isShowingAds(Context context) {
		if (!AD_ENABLED) {
			return false;
		}
		if (nbAgencies == null) {
			nbAgencies = DataSourceProvider.get(context).getAllAgenciesCount();
		}
		if (nbAgencies <= MIN_AGENCIES_FOR_ADS) {
			return false;
		}
		if (showingAds == null) {
			return false;
		}
		return showingAds;
	}

	public static void setShowingAds(Boolean newShowingAds, Activity activity) {
		showingAds = newShowingAds;
		refreshAdStatus(activity);
	}

	private static void refreshAdStatus(Activity activity) {
		if (isShowingAds(activity)) {
			if (adLoaded == null || !adLoaded) {
				setupAd(activity);
			}
		} else {
			if (adLoaded != null && adLoaded) {
				hideAds(activity);
				pauseAd(activity);
			}
			destroyAd(activity);
		}
	}

}
