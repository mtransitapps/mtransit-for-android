package org.mtransit.android.ad;

import android.content.res.Configuration;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.R;
import org.mtransit.android.common.IApplication;
import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.provider.location.MTLocationProvider;
import org.mtransit.android.ui.view.common.IActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdManager implements IAdManager, MTLog.Loggable {

	private static final String LOG_TAG = AdManager.class.getSimpleName();

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
	private final IApplication application;
	@NonNull
	private final CrashReporter crashReporter;
	@NonNull
	private final MTLocationProvider locationProvider;

	public AdManager(@NonNull IApplication application,
					 @NonNull CrashReporter crashReporter,
					 @NonNull MTLocationProvider locationProvider) {
		this.application = application;
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
					application.requireApplication().getApplicationContext()
			);
			if (DEBUG) {
				List<String> testDeviceIds = new ArrayList<>();
				testDeviceIds.add(AdRequest.DEVICE_ID_EMULATOR);
				testDeviceIds.addAll(Arrays.asList(application.requireContext().getResources().getStringArray(R.array.google_ads_test_devices_ids)));
				MobileAds.setRequestConfiguration(
						new RequestConfiguration.Builder()
								.setTestDeviceIds(testDeviceIds)
								.build()
				);
			}
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
		refreshRewardedAdStatus(activity);
	}

	@Nullable
	private Long rewardedUntilInMs = null;

	@Override
	public long getRewardedUntilInMs() {
		if (!AD_ENABLED) {
			return Long.MAX_VALUE; // forever
		}
		if (this.rewardedUntilInMs == null) {
			this.rewardedUntilInMs = PreferenceUtils.getPrefDefault(
					this.application.requireContext(),
					PreferenceUtils.PREF_USER_REWARDED_UNTIL,
					PreferenceUtils.PREF_USER_REWARDED_UNTIL_DEFAULT);
		}
		return this.rewardedUntilInMs;
	}

	private void setRewardedUntilInMs(long newRewardedUntilInMs) {
		this.rewardedUntilInMs = newRewardedUntilInMs;
		PreferenceUtils.savePrefDefault(
				this.application.requireContext(),
				PreferenceUtils.PREF_USER_REWARDED_UNTIL,
				rewardedUntilInMs,
				false // asynchronous
		);
	}

	@Override
	public boolean isRewardedNow() {
		if (!AD_ENABLED) {
			return true;
		}
		return getRewardedUntilInMs() > TimeUtils.currentTimeMillis();
	}

	private void rewardUser(long newRewardInMs, @Nullable IActivity activity) {
		final long currentRewardedUntilOrNow = Math.max(getRewardedUntilInMs(), TimeUtils.currentTimeMillis());
		setRewardedUntilInMs(currentRewardedUntilOrNow + newRewardInMs);
		if (activity != null) {
			refreshAdStatus(activity);
		}
	}

	@Nullable
	private WeakReference<RewardedAdListener> rewardedAdListenerWR = null;

	@Override
	public void setRewardedAdListener(@Nullable RewardedAdListener rewardedAdListener) {
		if (rewardedAdListener == null) {
			if (this.rewardedAdListenerWR != null) {
				this.rewardedAdListenerWR.clear();
			}
			this.rewardedAdListenerWR = null;
		} else {
			this.rewardedAdListenerWR = new WeakReference<>(rewardedAdListener);
		}
	}

	@Nullable
	private RewardedAdListener getRewardedAdListener() {
		return this.rewardedAdListenerWR == null ? null : this.rewardedAdListenerWR.get();
	}

	@Nullable
	private RewardedAd rewardedAd = null;
	@Nullable
	private Integer rewardedAdActivityHashCode = null;

	@NonNull
	private RewardedAd getRewardedAd(@NonNull IActivity activity) {
		if (this.rewardedAd == null
				|| (this.rewardedAdActivityHashCode != null && !this.rewardedAdActivityHashCode.equals(activity.hashCode()))) {
			final android.app.Activity theActivity = activity.requireActivity();
			this.rewardedAdActivityHashCode = theActivity.hashCode();
			this.rewardedAd = new RewardedAd(
					theActivity, // require activity?
					theActivity.getString(R.string.google_ads_rewarded_ad_unit_id)
			);
		}
		return this.rewardedAd;
	}

	@Override
	public void linkRewardedAd(@NonNull IActivity activity) {
		if (this.rewardedAdActivityHashCode != null
				&& this.rewardedAdActivityHashCode.equals(activity.hashCode())) {
			return; // same activity
		}
		this.rewardedAd = null;
		this.rewardedAdActivityHashCode = null;
		getRewardedAd(activity);
	}

	@Override
	public void unlinkRewardedAd(@NonNull IActivity activity) {
		if (this.rewardedAdActivityHashCode != null
				&& this.rewardedAdActivityHashCode.equals(activity.hashCode())) {
			this.rewardedAd = null;
			this.rewardedAdActivityHashCode = null;
		}
	}

	@Override
	public void refreshRewardedAdStatus(@NonNull IActivity activity) {
		if (!AD_ENABLED) {
			return;
		}
		final boolean isNotPayingUser = Boolean.TRUE.equals(this.showingAds);
		if (!isNotPayingUser) {
			MTLog.d(this, "refreshRewardedAdStatus() > SKIP (paying user)");
			return;
		}
		final RewardedAdListener listener = getRewardedAdListener();
		if (listener == null) {
			MTLog.d(this, "refreshRewardedAdStatus() > SKIP (unknown screen)");
			return;
		}
		if (listener.skipRewardedAd()) {
			MTLog.d(this, "refreshRewardedAdStatus() > SKIP (not in this screen)");
			return;
		}
		loadRewardedAdIfNecessary(activity);
	}

	private void loadRewardedAdIfNecessary(@NonNull IActivity activity) {
		final RewardedAd rewardedAd = getRewardedAd(activity);
		if (!rewardedAd.isLoaded()) {
			rewardedAd.loadAd(getAdRequest(activity), new MTRewardedAdLoadCallback(this, this.crashReporter));
		}
	}

	@NonNull
	private AdRequest getAdRequest(@NonNull IContext context) {
		AdRequest.Builder adRequestBd = new AdRequest.Builder();
		adRequestBd.setLocation(getLastLocation());
		for (String keyword : KEYWORDS) {
			adRequestBd.addKeyword(keyword);
		}
		AdRequest adRequest = adRequestBd.build();
		if (BuildConfig.DEBUG) {
			MTLog.d(this, "getAdRequest() > test device? %s.", adRequest.isTestDevice(context.requireContext()));
		}
		return adRequest;
	}

	private static class MTRewardedAdLoadCallback extends RewardedAdLoadCallback implements MTLog.Loggable {

		private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + MTRewardedAdLoadCallback.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final AdManager adManager;
		@NonNull
		private final CrashReporter crashReporter;

		MTRewardedAdLoadCallback(@NonNull AdManager adManager, @NonNull CrashReporter crashReporter) {
			this.adManager = adManager;
			this.crashReporter = crashReporter;
		}

		@Override
		public void onRewardedAdLoaded() {
			super.onRewardedAdLoaded();
			final RewardedAdListener listener = this.adManager.getRewardedAdListener();
			if (listener != null) {
				listener.onRewardedAdStatusChanged();
			}
		}

		@Override
		public void onRewardedAdFailedToLoad(LoadAdError loadAdError) {
			super.onRewardedAdFailedToLoad(loadAdError);
			final RewardedAdListener listener = this.adManager.getRewardedAdListener();
			if (listener != null) {
				listener.onRewardedAdStatusChanged();
			}
			switch (loadAdError.getCode()) {
			case AdRequest.ERROR_CODE_APP_ID_MISSING:
				this.crashReporter.w(this, "Failed to received rewarded ad! App ID missing: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_INTERNAL_ERROR:
				this.crashReporter.w(this, "Failed to received rewarded ad! Internal error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_INVALID_REQUEST:
				this.crashReporter.w(this, "Failed to received rewarded ad! Invalid request error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_NETWORK_ERROR:
				MTLog.w(this, "Failed to received rewarded ad! Network error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_NO_FILL:
				this.crashReporter.w(this, "Failed to received rewarded ad! No fill error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			default:
				this.crashReporter.w(this, "Failed to received rewarded ad! Error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			}
		}
	}

	@Override
	public boolean isRewardedAdAvailableToShow() {
		if (!AD_ENABLED) {
			return false;
		}
		if (this.rewardedAd == null) { // do not trigger creation + loading
			return false;
		}
		return this.rewardedAd.isLoaded();
	}

	@Override
	public boolean showRewardedAd(@NonNull IActivity activity) {
		if (!AD_ENABLED) {
			return false;
		}
		if (this.rewardedAd == null) { // do not trigger creation + loading
			return false;
		}
		if (!this.rewardedAd.isLoaded()) {
			return false;
		}
		this.rewardedAd.show(activity.requireActivity(), new MTRewardedAdCallback(this, this.crashReporter, activity));
		return true;
	}

	@Override
	public int getRewardedAdAmount() {
		return 7; // 1 week
	}

	@Override
	public long getRewardedAdAmountInMs() {
		final long rewardAmount = getRewardedAdAmount(); // TODO custom amount? rewardItem.getAmount()
		final TimeUnit rewardType = TimeUnit.DAYS;// TODO custom type? rewardItem.getType();
		return rewardType.toMillis(rewardAmount);
	}

	private static class MTRewardedAdCallback extends RewardedAdCallback implements MTLog.Loggable {

		private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + MTRewardedAdCallback.class.getSimpleName();

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

		MTRewardedAdCallback(@NonNull AdManager adManager, @NonNull CrashReporter crashReporter, @NonNull IActivity activity) {
			this.adManager = adManager;
			this.crashReporter = crashReporter;
			this.activityWR = new WeakReference<>(activity);
		}

		@Override
		public void onRewardedAdOpened() {
			super.onRewardedAdOpened();
		}

		@Override
		public void onRewardedAdClosed() {
			super.onRewardedAdClosed();
			final IActivity activity = this.activityWR.get();
			if (activity != null) {
				this.adManager.refreshRewardedAdStatus(activity);
			}
		}

		@Override
		public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
			final IActivity activity = this.activityWR.get();
			this.adManager.rewardUser(this.adManager.getRewardedAdAmountInMs(), activity);
		}

		@Override
		public void onRewardedAdFailedToShow(AdError adError) {
			super.onRewardedAdFailedToShow(adError);
			this.crashReporter.w(this, "Failed to show rewarded ad! %s: '%s' (%s).", adError.getCode(), adError.getMessage(), adError.getDomain());
		}
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
			MTLog.d(this, "isShowingAds() > Not showing ads (no '%d' agency installed).", nbAgencies);
			return false; // not showing ads
		}
		if (showingAds == null) { // paying status unknown
			MTLog.d(this, "isShowingAds() > Not showing ads (paying status unknown: '%s').", showingAds);
			return false; // not showing ads
		}
		MTLog.d(this, "isShowingAds() > Showing ads: '%s'.", showingAds);
		if (isRewardedNow()) { // rewarded status
			MTLog.d(this, "isShowingAds() > Not showing ads (rewarded until: %s).", this.rewardedUntilInMs);
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

	private static class SetupAdTask extends MTCancellableAsyncTask<Void, Void, Boolean> {

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
		protected Boolean doInBackgroundNotCancelledMT(Void... params) {
			if (!AD_ENABLED) {
				return false;
			}
			return !isCancelled() && this.adManager.isShowingAds();
		}

		@Override
		protected void onPostExecuteNotCancelledMT(@Nullable Boolean result) {
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

					adView.loadAd(this.adManager.getAdRequest(activity));
				}
			} else { // hide ads
				this.adManager.hideAds(activity);
			}
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
		public void onAdFailedToLoad(LoadAdError loadAdError) {
			super.onAdFailedToLoad(loadAdError);
			switch (loadAdError.getCode()) {
			case AdRequest.ERROR_CODE_APP_ID_MISSING:
				this.crashReporter.w(this, "Failed to received ad! App ID missing: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_INTERNAL_ERROR:
				this.crashReporter.w(this, "Failed to received ad! Internal error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_INVALID_REQUEST:
				this.crashReporter.w(this, "Failed to received ad! Invalid request error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_NETWORK_ERROR:
				MTLog.w(this, "Failed to received ad! Network error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_NO_FILL:
				this.crashReporter.w(this, "Failed to received ad! No fill error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			default:
				this.crashReporter.w(this, "Failed to received ad! Error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
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
