package org.mtransit.android.util;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.ui.MTActivityWithLocation;

import android.app.Activity;
import android.content.Context;
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

	public static boolean AD_ENABLED = true;

	private static boolean DEBUG = false;

	private static Boolean showingAds = null;

	private static Boolean generousUser = null;

	private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(new String[] { "transit", "transport", "bus", "subway", "metro", "taxi",
			"bike", "sharing", "velo", "train" }));

	public static void setupAd(Activity activity) {
		new SetupAdTask(activity).execute();
	}

	private static class SetupAdTask extends MTAsyncTask<Void, Void, Boolean> {

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
			final Activity activity = this.activityWR == null ? null : this.activityWR.get();
			return AD_ENABLED && isShowingAds(activity);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			final Activity activity = this.activityWR == null ? null : this.activityWR.get();
			if (result) {
				View adLayout = activity.findViewById(R.id.ad_layout);
				if (adLayout != null) {
					AdView adView = (AdView) adLayout.findViewById(R.id.ad);
					if (adView != null) {
						adView.setAdListener(new MTAdListener(activity));
						AdRequest.Builder adRequestBd = new AdRequest.Builder();
						if (activity != null && activity instanceof MTActivityWithLocation) {
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
			} else {
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
			final Activity activity = this.activityWR == null ? null : this.activityWR.get();
			hideAds(activity);
		}

		@Override
		public void onAdLoaded() {
			super.onAdLoaded();
			final Activity activity = this.activityWR == null ? null : this.activityWR.get();
			showAds(activity);
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

	public static void showAds(Activity activity) {
		View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			adLayout.setVisibility(View.VISIBLE);
			AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.setVisibility(View.VISIBLE);
			}
		}
	}

	public static void hideAds(Activity activity) {
		View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			adLayout.setVisibility(View.GONE);
			AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.setVisibility(View.GONE);
			}
		}
	}

	public static void pauseAd(Activity activity) {
		if (AD_ENABLED && isShowingAds(activity)) {
			View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.pause();
				}
			}
		}
	}

	public static void resumeAd(Activity activity) {
		if (AD_ENABLED && isShowingAds(activity)) {
			View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.resume();
				}
			}
		}
	}

	public static void destroyAd(Activity activity) {
		if (AD_ENABLED && isShowingAds(activity)) {
			View adLayout = activity == null ? null : activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.destroy();
				}
			}
		}
	}

	public static boolean isShowingAds(Context context) {
		if (!AD_ENABLED) {
			return false;
		}
		if (AdsUtils.showingAds == null) {
			AdsUtils.showingAds = true;
		}
		return AdsUtils.showingAds;
	}

	public static void setShowingAds(Boolean showingAds) {
		AdsUtils.showingAds = showingAds;
	}

	public static boolean isGenerousUser(Context context) {
		if (AdsUtils.generousUser == null) {
			AdsUtils.generousUser = false;
		}
		return AdsUtils.generousUser;
	}

	public static void setGenerousUser(Boolean generousUser) {
		AdsUtils.generousUser = generousUser;
	}
}
