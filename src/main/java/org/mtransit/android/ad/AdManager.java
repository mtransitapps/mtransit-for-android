package org.mtransit.android.ad;

import android.content.res.Configuration;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

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

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class AdManager implements IAdManager, MTLog.Loggable {

	private static final String LOG_TAG = AdManager.class.getSimpleName();

	public static boolean isInAdsProcess(@NonNull IContext context) {
		return AudienceNetworkAds.isInAdsProcess(context.requireContext());
	}

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final boolean DEBUG = false;
	// private static final boolean DEBUG = true; // DEBUG

	private static boolean AD_ENABLED = true;
	// private static boolean AD_ENABLED = false; // DEBUG

	private static final int MIN_AGENCIES_FOR_ADS = 2;
	// private static final int MIN_AGENCIES_FOR_ADS = 0; // DEBUG

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
			MobileAds.initialize(
					application.requireApplication().getApplicationContext(),
					application.requireContext().getString(R.string.google_ads_app_id)
			);
		} catch (Exception e) {
			this.crashReporter.w(this, e, "Error while initializing Ads!");
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
	public int getBannerHeightInPx(@Nullable IActivity activity) {
		if (adLoaded == null || !adLoaded) {
			return 0; // ad not loaded
		}
		if (!isShowingAds()) {
			return 0; // not showing ads (0 agency installed, paying user...)
		}
		if (activity == null) {
			return 0; // can't measure w/o context
		}
		final AdSize adSize = getAdSize(activity);
		return adSize.getHeightInPixels(activity.requireContext());
	}

	@NonNull
	private static AdSize getAdSize(@NonNull IActivity activity) {
		Display display = activity.requireActivity().getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);

		float density = outMetrics.density;

		View adLayout = activity.findViewById(R.id.ad_layout);
		float adWidthPixels = adLayout == null ? 0 : adLayout.getWidth();

		float widthPixels = outMetrics.widthPixels;

		if (adWidthPixels == 0) {
			adWidthPixels = widthPixels;
		}

		int adWidth = (int) (adWidthPixels / density);

		return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity.requireContext(), adWidth);
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
			nbAgencies = dataSourceProvider.isInitialized() ? dataSourceProvider.getAllAgenciesCount() : null;
		}
		if (nbAgencies == null // number of agency unknown
				|| nbAgencies <= MIN_AGENCIES_FOR_ADS) { // no (real) agency installed
			return false; // not showing ads
		}
		if (showingAds == null) { // paying status unknown
			return false; // not showing ads
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
					this.crashReporter.w(this, t, "Error while destroying ad view!");
				}
			}
		}
		adLoaded = null;
		TaskUtils.cancelQuietly(setupAdTask, true);
		setupAdTask = null;
	}

	@Nullable
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
				ViewGroup adLayout = activity.findViewById(R.id.ad_layout);
				if (adLayout != null) {
					AdView adView = adLayout.findViewById(R.id.ad);
					if (adView == null) {
						adView = new AdView(activity.requireContext());
						adView.setLayoutParams(new ViewGroup.LayoutParams(
								ViewGroup.LayoutParams.MATCH_PARENT,
								ViewGroup.LayoutParams.WRAP_CONTENT
						));
						adView.setVisibility(View.GONE);
						adView.setId(R.id.ad);
						adView.setAdUnitId(activity.requireContext().getString(R.string.google_ads_banner_ad_unit_id));
						adLayout.removeAllViews();
						adLayout.addView(adView);

						final AdSize adSize = getAdSize(activity);
						adView.setAdSize(adSize);
					}
					adView.setAdListener(new MTAdListener(this.adManager, this.crashReporter, activity));

					adView.loadAd(getAdRequest(activity));
				}
			} else { // hide ads
				this.adManager.hideAds(activity);
			}
		}

		private AdRequest getAdRequest(IContext context) {
			AdRequest.Builder adRequestBd = new AdRequest.Builder();
			adRequestBd.setLocation(this.adManager.getLastLocation());
			for (String keyword : KEYWORDS) {
				adRequestBd.addKeyword(keyword);
			}
			if (DEBUG) {
				adRequestBd.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
				for (String deviceId : context.requireContext().getResources().getStringArray(R.array.google_ads_test_devices_ids)) {
					adRequestBd.addTestDevice(deviceId);
				}
			}
			AdRequest adRequest = adRequestBd.build();
			MTLog.d(this, "onPostExecute() > request.isTestDevice(): %s", adRequest.isTestDevice(context.requireContext()));
			return adRequest;
		}
	}

	private static class MTAdListener extends AdListener implements MTLog.Loggable {

		private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + MTAdListener.class.getSimpleName();

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
				this.crashReporter.w(this, "Failed to received ad! Internal error code: '%s'.", errorCode);
				break;
			case AdRequest.ERROR_CODE_INVALID_REQUEST:
				this.crashReporter.w(this, "Failed to received ad! Invalid request error code: '%s'.", errorCode);
				break;
			case AdRequest.ERROR_CODE_NETWORK_ERROR:
				MTLog.w(this, "Failed to received ad! Network error code: '%s'.", errorCode);
				break;
			case AdRequest.ERROR_CODE_NO_FILL:
				this.crashReporter.w(this, "Failed to received ad! No fill error code: '%s'.", errorCode);
				break;
			default:
				this.crashReporter.w(this, "Failed to received ad! Error code: '%s'.", errorCode);
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

		@Override
		public void onAdClicked() {
			super.onAdClicked();
		}

		@Override
		public void onAdImpression() {
			super.onAdImpression();
		}
	}
}
