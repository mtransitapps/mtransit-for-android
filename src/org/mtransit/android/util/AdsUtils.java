package org.mtransit.android.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.MTActivityWithLocation;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
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

	private static boolean DEBUG = false;

	private static Boolean showingAds = null;

	private static Boolean generousUser = null;

	public static final boolean AD_ENABLED = true;

	private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(new String[] { "transit", "transport", "bus", "subway", "metro", "taxi",
			"bike", "sharing", "velo", "train" }));

	public static void setupAd(final Activity activity) {
		final Location lastLocation;
		if (activity != null && activity instanceof MTActivityWithLocation) {
			lastLocation = ((MTActivityWithLocation) activity).getLastLocation();
		} else {
			lastLocation = null;
		}
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				return AD_ENABLED && isShowingAds(activity);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (result) {
					final View adLayout = activity.findViewById(R.id.ad_layout);
					if (adLayout != null) {
						final AdView adView = (AdView) adLayout.findViewById(R.id.ad);
						if (adView != null) {
							AdRequest.Builder adRequestBd = new AdRequest.Builder();
							adRequestBd.setLocation(lastLocation);
							for (String keyword : KEYWORDS) {
								adRequestBd.addKeyword(keyword);
							}
							adView.setAdListener(new AdListener() {

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
									hideAds(activity);
								}

								@Override
								public void onAdLoaded() {
									super.onAdLoaded();
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
							});
							if (DEBUG) {
								adRequestBd.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
								adRequestBd.addTestDevice(activity.getString(R.string.google_ads_test_device_id));
							}
							adView.loadAd(adRequestBd.build());
						}
					}
				} else {
					hideAds(activity);
				}
			};
		}.execute();
	}

	public static void showAds(Activity activity) {
		final View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			adLayout.setVisibility(View.VISIBLE);
			final AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.setVisibility(View.VISIBLE);
			}
		}
	}

	public static void hideAds(Activity activity) {
		final View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			adLayout.setVisibility(View.GONE);
			final AdView adView = (AdView) adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.setVisibility(View.GONE);
			}
		}
	}

	public static void pauseAd(Activity activity) {
		if (AD_ENABLED && isShowingAds(activity)) {
			final View adLayout = activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				final AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.pause();
				}
			}
		}
	}

	public static void resumeAd(Activity activity) {
		if (AD_ENABLED && isShowingAds(activity)) {
			final View adLayout = activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				final AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.resume();
				}
			}
		}
	}

	public static void destroyAd(Activity activity) {
		if (AD_ENABLED && isShowingAds(activity)) {
			final View adLayout = activity.findViewById(R.id.ad_layout);
			if (adLayout != null) {
				final AdView adView = (AdView) adLayout.findViewById(R.id.ad);
				if (adView != null) {
					adView.removeAllViews();
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
