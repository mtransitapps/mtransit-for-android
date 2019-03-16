package org.mtransit.android.ad;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.mtransit.android.R;
import org.mtransit.android.common.IApplication;
import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.provider.location.MTLocationProvider;
import org.mtransit.android.ui.view.common.IActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import android.content.res.Configuration;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

public class AdManager implements IAdManager, MTLog.Loggable {

	private static final String LOG_TAG = AdManager.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final boolean DEBUG = false;

	private static boolean AD_ENABLED = true;

	private static final int MIN_AGENCIES_FOR_ADS = 2;

	private static final ArrayList<String> KEYWORDS = ArrayUtils.asArrayList(
			"transit", "bus", "subway", "bike", "sharing", "ferries", "boat", "trail", "lrt", "streetcar", "tram", "tramway",
			"transport", "velo", "metro", "taxi", "train", "traversier");

	@Nullable
	private Boolean showingAds = null;
	@Nullable
	private Integer nbAgencies = null;
	@Nullable
	private Boolean adLoaded = null;

	@NonNull
	private final CrashReporter crashReporter;
	@NonNull
	private final MTLocationProvider locationProvider;

	public AdManager(@NonNull CrashReporter crashReporter,
					 @NonNull MTLocationProvider locationProvider) {
		this.crashReporter = crashReporter;
		this.locationProvider = locationProvider;
	}

	@Override
	public void init(@NonNull IApplication application) {
		if (!AD_ENABLED) {
			return;
		}
		try {
			MobileAds.initialize(application.requireContext(), application.requireContext().getString(R.string.google_ads_app_id));
		} catch (Exception e) {
			this.crashReporter.w(LOG_TAG, e, "Error while initializing Ads!");
		}
	}

	@Override
	public void onModulesUpdated(@NonNull IActivity activity) {
		nbAgencies = null; // reset
		refreshAdStatus(activity);
	}

	@Override
	public void setShowingAds(@Nullable Boolean newShowingAds, @NonNull IActivity activity) {
		showingAds = newShowingAds;
		refreshAdStatus(activity);
	}

	@Override
	public int getBannerHeightInPx(@Nullable IContext context) {
		if (adLoaded == null || !adLoaded) {
			return 0; // ad not loaded
		}
		if (!isShowingAds()) {
			return 0; // not showing ads (0 agency installed, paying user...)
		}
		if (context == null) {
			return 0; // can't measure w/o context
		}
		return AdSize.SMART_BANNER.getHeightInPixels(context.requireContext());
	}

	private void refreshAdStatus(@NonNull IActivity activity) {
		if (isShowingAds()) {
			if (adLoaded == null || !adLoaded) {
				setupAd(activity);
			}
		} else {
			if (adLoaded != null && adLoaded) {
				hideAds(activity);
				pauseAd(activity);
			}
		}
	}

	private boolean isShowingAds() {
		if (!AD_ENABLED) {
			return false;
		}
		if (nbAgencies == null) {
			DataSourceProvider dataSourceProvider = DataSourceProvider.get();
			nbAgencies = dataSourceProvider == null ? -1 : dataSourceProvider.getAllAgenciesCount();
		}
		if (nbAgencies <= MIN_AGENCIES_FOR_ADS) {
			return false;
		}
		if (showingAds == null) {
			return false;
		}
		return showingAds;
	}

	private void setupAd(@NonNull IActivity activity) {
		if (!AD_ENABLED) {
			return;
		}
		if (!isShowingAds()) {
			return;
		}
		if (setupAdTask == null) {
			setupAdTask = new SetupAdTask(this, this.crashReporter, activity);
			TaskUtils.execute(setupAdTask);
		}
	}

	private void hideAds(@NonNull IActivity activity) {
		View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = adLayout.findViewById(R.id.ad);
			if (adLayout.getVisibility() != View.GONE) {
				adLayout.setVisibility(View.GONE);
			}
			if (adView != null && adView.getVisibility() != View.GONE) {
				adView.setVisibility(View.GONE);
			}
		}
	}

	private void showAdsIfEnoughSpace(@NonNull IActivity activity) {
		adaptToScreenSize(activity, activity.requireContext().getResources().getConfiguration());
	}

	@Override
	public void adaptToScreenSize(@NonNull IActivity activity, @Nullable Configuration configuration) {
		if (!AD_ENABLED) {
			return;
		}
		if (!isShowingAds()) {
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

	private void showAds(@NonNull IActivity activity) {
		View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = adLayout.findViewById(R.id.ad);
			if (adView != null && adView.getVisibility() != View.VISIBLE) {
				adView.setVisibility(View.VISIBLE);
			}
			if (adLayout.getVisibility() != View.VISIBLE) {
				adLayout.setVisibility(View.VISIBLE);
			}
		}
	}

	private boolean isEnoughSpace(@Nullable Configuration configuration) {
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

	@Nullable
	private Location getLastLocation() {
		this.locationProvider.readLastLocation();
		return this.locationProvider.getLastLocationOrNull();
	}

	@Override
	public void pauseAd(@NonNull IActivity activity) {
		if (!AD_ENABLED) {
			return;
		}
		View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.pause();
			}
		}
	}

	@Override
	public void resumeAd(@NonNull IActivity activity) {
		if (!AD_ENABLED) {
			return;
		}
		View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = adLayout.findViewById(R.id.ad);
			if (adView != null) {
				adView.resume();
			}
		}
	}

	@Override
	public void destroyAd(@NonNull IActivity activity) {
		if (!AD_ENABLED) {
			return;
		}
		View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			AdView adView = adLayout.findViewById(R.id.ad);
			if (adView != null) {
				try {
					adView.removeAllViews();
					adView.destroy();
				} catch (Throwable t) {
					this.crashReporter.w(LOG_TAG, t, "Error while destroying ad view!");
				}
			}
		}
		adLoaded = null;
		TaskUtils.cancelQuietly(setupAdTask, true);
		setupAdTask = null;
	}

	private static SetupAdTask setupAdTask;

	private static class SetupAdTask extends MTAsyncTask<Void, Void, Boolean> {

		private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + SetupAdTask.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final AdManager adManager;
		@NonNull
		private final CrashReporter crashReporter;
		@NonNull
		private final WeakReference<IActivity> activityWR;

		SetupAdTask(@NonNull AdManager adManager, @NonNull CrashReporter crashReporter, @NonNull IActivity activity) {
			this.adManager = adManager;
			this.crashReporter = crashReporter;
			this.activityWR = new WeakReference<>(activity);
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			if (!AD_ENABLED) {
				return false;
			}
			return !isCancelled() && this.adManager.isShowingAds();
		}

		@Override
		protected void onPostExecute(@NonNull Boolean result) {
			IActivity activity = this.activityWR.get();
			if (activity == null) {
				return;
			}
			if (Boolean.TRUE.equals(result) && !isCancelled()) { // show ads
				View adLayout = activity.findViewById(R.id.ad_layout);
				if (adLayout != null) {
					AdView adView = adLayout.findViewById(R.id.ad);
					if (adView != null) {
						adView.setAdListener(new MTAdListener(this.adManager, this.crashReporter, activity));
						AdRequest.Builder adRequestBd = new AdRequest.Builder();
						adRequestBd.setLocation(this.adManager.getLastLocation());
						for (String keyword : KEYWORDS) {
							adRequestBd.addKeyword(keyword);
						}
						if (DEBUG) {
							adRequestBd.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
							for (String deviceId : activity.requireContext().getResources().getStringArray(R.array.google_ads_test_devices_ids)) {
								adRequestBd.addTestDevice(deviceId);
							}
						}
						adView.loadAd(adRequestBd.build());
					}
				}
			} else { // hide ads
				this.adManager.hideAds(activity);
			}
		}
	}

	private static class MTAdListener extends AdListener {

		@NonNull
		private final AdManager adManager;
		@NonNull
		private final CrashReporter crashReporter;
		@NonNull
		private final WeakReference<IActivity> activityWR;

		MTAdListener(@NonNull AdManager adManager, @NonNull CrashReporter crashReporter, @NonNull IActivity activity) {
			this.adManager = adManager;
			this.crashReporter = crashReporter;
			this.activityWR = new WeakReference<>(activity);
		}

		@Override
		public void onAdFailedToLoad(int errorCode) {
			super.onAdFailedToLoad(errorCode);
			switch (errorCode) {
			case AdRequest.ERROR_CODE_INTERNAL_ERROR:
				this.crashReporter.w(LOG_TAG, "Failed to received ad! Internal error code: '%s'.", errorCode);
				break;
			case AdRequest.ERROR_CODE_INVALID_REQUEST:
				this.crashReporter.w(LOG_TAG, "Failed to received ad! Invalid request error code: '%s'.", errorCode);
				break;
			case AdRequest.ERROR_CODE_NETWORK_ERROR:
				this.crashReporter.w(LOG_TAG, "Failed to received ad! Network error code: '%s'.", errorCode);
				break;
			case AdRequest.ERROR_CODE_NO_FILL:
				this.crashReporter.w(LOG_TAG, "Failed to received ad! No fill error code: '%s'.", errorCode);
				break;
			default:
				this.crashReporter.w(LOG_TAG, "Failed to received ad! Error code: '%s'.", errorCode);
			}
			this.adManager.adLoaded = null;
			IActivity activity = this.activityWR.get();
			if (activity == null) {
				return;
			}
			this.adManager.hideAds(activity);
		}

		@Override
		public void onAdLoaded() {
			super.onAdLoaded();
			this.adManager.adLoaded = true;
			IActivity activity = this.activityWR.get();
			if (activity == null) {
				return;
			}
			this.adManager.showAdsIfEnoughSpace(activity);
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
}
