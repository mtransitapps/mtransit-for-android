package org.mtransit.android.ad;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
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
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.provider.location.MTLocationProvider;
import org.mtransit.android.ui.view.common.IActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TESTING:
 * Add tools:replace="android:supportsRtl" in AndroidManifest.xml
 * Requires real app ID & unit IDs in keys.xml
 * AdMob test devices:
 * - https://support.google.com/admob/answer/9691433
 * - https://apps.admob.com/v2/settings/test-devices/list
 * Audience Network test devices:
 * - https://developers.facebook.com/docs/audience-network/guides/test
 * - https://business.facebook.com/pub/testing
 * MORE:
 * - https://developers.google.com/admob/android/test-ads
 */
@SuppressLint("MissingPermission")
public class AdManager implements IAdManager, MTLog.Loggable {

	private static final String LOG_TAG = AdManager.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final boolean DEBUG = false;
	// private static final boolean DEBUG = true; // DEBUG

	private static final boolean AD_ENABLED = true;
	// private static final boolean AD_ENABLED = false; // DEBUG

	private static final boolean IGNORE_REWARD_HIDING_BANNER = false;
	// private static final boolean IGNORE_REWARD_HIDING_BANNER = true; // DEBUG

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
	@SuppressWarnings("FieldCanBeLocal")
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public AdManager(@NonNull IApplication application,
					 @NonNull CrashReporter crashReporter,
					 @NonNull MTLocationProvider locationProvider,
					 @NonNull DataSourcesRepository dataSourcesRepository) {
		this.application = application;
		this.crashReporter = crashReporter;
		this.locationProvider = locationProvider;
		this.dataSourcesRepository = dataSourcesRepository;
		this.dataSourcesRepository.readingAllAgenciesCount().observeForever(newNbAgencies -> { // SINGLETON
			this.nbAgencies = newNbAgencies;
		});
	}

	@Nullable
	private Boolean initialized = false;

	@Override
	public void init(@NonNull IActivity activity) {
		if (!AD_ENABLED) {
			return;
		}
		if (this.initialized == null // IN PROGRESS
				|| Boolean.TRUE.equals(this.initialized)) {
			MTLog.d(this, "init() > SKIP (init: %s)", this.initialized);
			return; // SKIP
		}
		final android.app.Activity theActivity = activity.getActivity();
		if (theActivity == null) {
			MTLog.w(this, "Trying to initialized w/o activity!");
			return; // SKIP
		}
		this.initialized = null; // IN PROGRESS
		try {
			MobileAds.initialize(
					theActivity, // some adapters require activity #MoPub
					new MTOnInitializationCompleteListener(this, activity)
			);
			if (DEBUG) {
				List<String> testDeviceIds = new ArrayList<>();
				testDeviceIds.add(AdRequest.DEVICE_ID_EMULATOR);
				testDeviceIds.addAll(Arrays.asList(this.application.requireContext().getResources().getStringArray(R.array.google_ads_test_devices_ids)));
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

	private static class MTOnInitializationCompleteListener implements OnInitializationCompleteListener, MTLog.Loggable {

		private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + MTOnInitializationCompleteListener.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final AdManager adManager;
		@NonNull
		private final WeakReference<IActivity> activityWR;

		MTOnInitializationCompleteListener(@NonNull AdManager adManager, @Nullable IActivity activity) {
			this.adManager = adManager;
			this.activityWR = new WeakReference<>(activity);
		}

		@Override
		public void onInitializationComplete(InitializationStatus initializationStatus) {
			MTLog.d(this, "onInitializationComplete()");
			this.adManager.initialized = true;
			final IActivity activity = this.activityWR.get();
			if (activity != null) {
				this.adManager.setupAd(activity);
			}
			Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
			for (String adapterClass : statusMap.keySet()) {
				AdapterStatus status = statusMap.get(adapterClass);
				MTLog.d(this, "Adapter name: %s, Description: %s, Latency: %d",
						adapterClass,
						status.getDescription(),
						status.getLatency()
				);
			}
		}
	}

	@Override
	public void onNbAgenciesUpdated(@NonNull IActivity activity, int nbAgencies) {
		this.nbAgencies = nbAgencies;
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
	public void resetRewarded() {
		setRewardedUntilInMs(PreferenceUtils.PREF_USER_REWARDED_UNTIL_DEFAULT);
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
			ToastUtils.makeTextAndShowCentered(activity.getContext(),
					R.string.support_watch_rewarded_ad_successful_message,
					Toast.LENGTH_LONG);
			refreshAdStatus(activity);
		}
	}

	@Nullable
	private WeakReference<RewardedAdListener> rewardedAdListenerWR = null;

	@Override
	public void setRewardedAdListener(@Nullable RewardedAdListener rewardedAdListener) {
		if (rewardedAdListener == null) {
			if (this.rewardedAdListenerWR != null) {
				MTLog.d(this, "setRewardedAdListener() > clearing %s...", this.rewardedAdListenerWR);
				this.rewardedAdListenerWR.clear();
			}
			this.rewardedAdListenerWR = null;
		} else {
			MTLog.d(this, "setRewardedAdListener() > setting %s...", rewardedAdListener);
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

	private void loadRewardedAdForActivity(@NonNull IActivity activity) {
		final android.app.Activity theActivity = activity.requireActivity();
		if (this.rewardedAd == null
				|| (this.rewardedAdActivityHashCode != null && !this.rewardedAdActivityHashCode.equals(theActivity.hashCode()))) {
			this.rewardedAdActivityHashCode = theActivity.hashCode();
			MTLog.d(this, "loadRewardedAdForActivity() > Loading rewarded ad for %s...", theActivity.getClass().getSimpleName());
			RewardedAd.load(
					theActivity, // require activity?
					theActivity.getString(R.string.google_ads_rewarded_ad_unit_id),
					getAdRequest(activity),
					new MTRewardedAdLoadCallback(this, this.crashReporter)
			);
		} else {
			MTLog.d(this, "loadRewardedAdForActivity() > NOT Loading rewarded ad for %s...", theActivity.getClass().getSimpleName());
		}
	}

	private void setRewardedAd(@Nullable RewardedAd rewardedAd) {
		if (this.rewardedAdActivityHashCode == null) {
			MTLog.d(this, "setRewardedAd() > SKIP rewarded ad (no activity) %s.", rewardedAd);
			return; // too late
		}
		this.rewardedAd = rewardedAd;
	}

	@Override
	public void linkRewardedAd(@NonNull IActivity activity) {
		final android.app.Activity theActivity = activity.requireActivity();
		if (this.rewardedAdActivityHashCode != null
				&& this.rewardedAdActivityHashCode.equals(theActivity.hashCode())) {
			MTLog.d(this, "linkRewardedAd() > SKIP (sane activity)");
			return; // same activity
		}
		this.rewardedAd = null;
		this.rewardedAdActivityHashCode = null;
	}

	@Override
	public void unlinkRewardedAd(@NonNull IActivity activity) {
		final android.app.Activity theActivity = activity.requireActivity();
		if (this.rewardedAdActivityHashCode != null
				&& this.rewardedAdActivityHashCode.equals(theActivity.hashCode())) {
			this.rewardedAd = null;
			this.rewardedAdActivityHashCode = null;
		} else {
			MTLog.d(this, "unlinkRewardedAd() > SKIP (not this activity)");
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
		MTLog.d(this, "refreshRewardedAdStatus() > Load if necessary...");
		loadRewardedAdIfNecessary(activity);
	}

	private void loadRewardedAdIfNecessary(@NonNull IActivity activity) {
		loadRewardedAdForActivity(activity);
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
		public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
			super.onAdLoaded(rewardedAd);
			MTLog.d(this, "onAdLoaded() > Rewarded ad loaded.");
			this.adManager.setRewardedAd(rewardedAd);
			final RewardedAdListener listener = this.adManager.getRewardedAdListener();
			if (listener != null) {
				listener.onRewardedAdStatusChanged();
			}
		}

		@Override
		public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
			super.onAdFailedToLoad(loadAdError);
			this.adManager.setRewardedAd(null);
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
		//noinspection RedundantIfStatement
		if (this.rewardedAd == null) { // do not trigger creation + loading
			return false;
		}
		return true;
	}

	@Override
	public boolean showRewardedAd(@NonNull IActivity activity) {
		if (!AD_ENABLED) {
			return false;
		}
		if (this.rewardedAd == null) { // do not trigger creation + loading
			return false;
		}
		final android.app.Activity theActivity = activity.requireActivity();
		MTLog.d(this, "showRewardedAd() > Showing rewarded ad for %s...", theActivity);
		this.rewardedAd.setFullScreenContentCallback(
				new MTRewardedAdFullScreenContentCallback(this, this.crashReporter, activity)
		);
		this.rewardedAd.show(theActivity, new MTRewardedAdOnUserEarnedRewardListener(this, activity));
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

	private static class MTRewardedAdFullScreenContentCallback extends FullScreenContentCallback implements MTLog.Loggable {

		private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + MTRewardedAdFullScreenContentCallback.class.getSimpleName();

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

		MTRewardedAdFullScreenContentCallback(@NonNull AdManager adManager, @NonNull CrashReporter crashReporter, @NonNull IActivity activity) {
			super();
			this.adManager = adManager;
			this.crashReporter = crashReporter;
			this.activityWR = new WeakReference<>(activity);
		}

		@Override
		public void onAdShowedFullScreenContent() { // Ad was shown
			super.onAdShowedFullScreenContent();
			this.adManager.setRewardedAd(null); // clear showed ad
			final IActivity activity = this.activityWR.get();
			if (activity != null) {
				this.adManager.refreshRewardedAdStatus(activity);
			}
		}

		@Override
		public void onAdFailedToShowFullScreenContent(AdError adError) {
			super.onAdFailedToShowFullScreenContent(adError);
			this.crashReporter.w(this, "Failed to show rewarded ad! %s: '%s' (%s).", adError.getCode(), adError.getMessage(), adError.getDomain());
		}

		@Override
		public void onAdDismissedFullScreenContent() {
			super.onAdDismissedFullScreenContent();
			MTLog.d(this, "onAdDismissedFullScreenContent() > Rewarded ad dismissed.");
			this.adManager.setRewardedAd(null); // clear dismissed ad
		}
	}

	private static class MTRewardedAdOnUserEarnedRewardListener implements OnUserEarnedRewardListener, MTLog.Loggable {

		private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + MTRewardedAdOnUserEarnedRewardListener.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final AdManager adManager;
		@NonNull
		private final WeakReference<IActivity> activityWR;

		MTRewardedAdOnUserEarnedRewardListener(@NonNull AdManager adManager, @NonNull IActivity activity) {
			this.adManager = adManager;
			this.activityWR = new WeakReference<>(activity);
		}

		@Override
		public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
			MTLog.d(this, "onUserEarnedReward() > User earned reward from ad %s.", rewardItem);
			final IActivity activity = this.activityWR.get();
			this.adManager.rewardUser(this.adManager.getRewardedAdAmountInMs(), activity);
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

	@AnyThread
	private boolean isShowingAds() {
		if (!AD_ENABLED) {
			return false;
		}
		if (nbAgencies == null) {
			nbAgencies = this.dataSourcesRepository.getAllAgenciesCount();
		}
		if (!Boolean.TRUE.equals(this.initialized)) {
			MTLog.d(this, "isShowingAds() > Not showing ads (not initialized yet).");
			return false; // not showing ads
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
		if (IGNORE_REWARD_HIDING_BANNER) {
			return showingAds;
		}
		if (isRewardedNow()) { // rewarded status
			MTLog.d(this, "isShowingAds() > Not showing banner ads (rewarded until: %s).", this.rewardedUntilInMs);
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

	@SuppressWarnings("deprecation")
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

		@WorkerThread
		@Override
		protected Boolean doInBackgroundNotCancelledMT(Void... params) {
			if (!AD_ENABLED) {
				return false;
			}
			return !isCancelled() && this.adManager.isShowingAds();
		}

		@MainThread
		@Override
		protected void onPostExecuteNotCancelledMT(@Nullable Boolean isShowingAds) {
			IActivity activity = this.activityWR.get();
			if (activity == null) {
				return;
			}
			if (Boolean.TRUE.equals(isShowingAds) && !isCancelled()) { // show ads
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
			MTLog.d(this, "onAdFailedToLoad(%s)", loadAdError);
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
			case AdRequest.ERROR_CODE_REQUEST_ID_MISMATCH:
				this.crashReporter.w(this, "Failed to received ad! Request ID mismatch error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_NETWORK_ERROR:
				MTLog.w(this, "Failed to received ad! Network error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_MEDIATION_NO_FILL:
				MTLog.w(this, "Failed to received ad! Mediation no fill error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			case AdRequest.ERROR_CODE_NO_FILL:
				MTLog.w(this, "Failed to received ad! No fill error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			default:
				this.crashReporter.w(this, "Failed to received ad! Error code: '%s' (%s).", loadAdError.getCode(), loadAdError);
				break;
			}
			this.adManager.adLoaded = null;
			IActivity activity = this.activityWR.get();
			if (activity == null) {
				MTLog.d(this, "onAdFailedToLoad() > SKIP (no activity)");
				return;
			}
			this.adManager.hideAds(activity); // hiding ads until next AUTOMATIC ad refresh
		}

		@Override
		public void onAdLoaded() {
			super.onAdLoaded();
			MTLog.d(this, "onAdLoaded()");
			this.adManager.adLoaded = true;
			IActivity activity = this.activityWR.get();
			if (activity == null) {
				MTLog.d(this, "onAdLoaded() > SKIP (no activity)");
				return;
			}
			this.adManager.showAdsIfEnoughSpace(activity); // showing ads if hidden because of no-fill/network error
		}

		@Override
		public void onAdClosed() {
			super.onAdClosed();
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
