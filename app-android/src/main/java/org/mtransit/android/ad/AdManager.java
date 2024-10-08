package org.mtransit.android.ad;

import android.content.Context;
import android.content.res.Configuration;
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
import org.mtransit.android.common.IContext;
import org.mtransit.android.common.repository.DefaultPreferenceRepository;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.DemoModeManager;
import org.mtransit.android.ui.EdgeToEdgeKt;
import org.mtransit.android.ui.view.common.IActivity;
import org.mtransit.commons.FeatureFlags;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * TESTING:
 * Add tools:replace="android:supportsRtl" in AndroidManifest.xml
 * Requires real app ID & unit IDs in keys.xml
 * AdMob test devices:
 * - <a href="https://support.google.com/admob/answer/9691433">...</a>
 * - <a href="https://apps.admob.com/v2/settings/test-devices/list">...</a>
 * Audience Network test devices:
 * - <a href="https://developers.facebook.com/docs/audience-network/guides/test">...</a>
 * - <a href="https://business.facebook.com/pub/testing">...</a>
 * MORE:
 * - <a href="https://developers.google.com/admob/android/test-ads">...</a>
 */
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

	private static final ArrayList<String> KEYWORDS = ArrayUtils.asArrayList(
			"transit", "bus", "subway", "bike", "sharing", "ferries", "boat", "trail", "lrt", "streetcar", "tram", "tramway",
			"light rail", "transport", "velo", "metro", "taxi", "train", "traversier");

	@Nullable
	private Boolean showingAds = null;
	@Nullable
	private Boolean hasAgenciesEnabled = null;
	@Nullable
	private Boolean adLoaded = null;

	@NonNull
	private final Context appContext;
	@NonNull
	private final CrashReporter crashReporter;
	@NonNull
	private final DemoModeManager demoModeManager;
	@NonNull
	private final DefaultPreferenceRepository defaultPrefRepository;
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	@Inject
	public AdManager(@NonNull @ApplicationContext Context appContext,
					 @NonNull CrashReporter crashReporter,
					 @NonNull DemoModeManager demoModeManager,
					 @NonNull DefaultPreferenceRepository defaultPrefRepository,
					 @NonNull DataSourcesRepository dataSourcesRepository) {
		this.appContext = appContext;
		this.crashReporter = crashReporter;
		this.demoModeManager = demoModeManager;
		this.defaultPrefRepository = defaultPrefRepository;
		this.dataSourcesRepository = dataSourcesRepository;
		this.dataSourcesRepository.readingHasAgenciesEnabled().observeForever(hasAgenciesEnabled -> { // SINGLETON
			this.hasAgenciesEnabled = hasAgenciesEnabled;
		});
	}

	@NonNull
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	@Override
	public void init(@NonNull IActivity activity) {
		if (!AD_ENABLED) {
			return;
		}
		final android.app.Activity theActivity = activity.getActivity();
		if (theActivity == null) {
			MTLog.w(this, "Trying to initialized w/o activity!");
			return; // SKIP
		}
		if (this.initialized.get()) {
			MTLog.d(this, "init() > SKIP (init: %s)", this.initialized.get());
			return; // SKIP
		}
		try {
			TaskUtils.execute(new InitTask(AdManager.this, activity));
		} catch (Exception e) {
			this.crashReporter.w(this, e, "Error while initializing Ads!");
		}
	}

	@WorkerThread
	private static void initOnBackgroundThread(@NonNull AdManager adManager, @NonNull IActivity activity) {
		if (DEBUG) {
			List<String> testDeviceIds = new ArrayList<>();
			testDeviceIds.add(AdRequest.DEVICE_ID_EMULATOR);
			testDeviceIds.addAll(Arrays.asList(activity.requireContext().getResources().getStringArray(R.array.google_ads_test_devices_ids)));
			MobileAds.setRequestConfiguration(
					new RequestConfiguration.Builder()
							.setTestDeviceIds(testDeviceIds)
							.build()
			);
		}
		// https://developers.google.com/admob/android/quick-start#initialize_the_mobile_ads_sdk
		MobileAds.initialize( // doing I/O #StrictMode
				activity.requireActivity(), // some adapters require activity
				new MTOnInitializationCompleteListener(adManager, activity)
		);
	}

	@SuppressWarnings("deprecation")
	private static class InitTask extends MTCancellableAsyncTask<Void, Void, Boolean> {

		private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + InitTask.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final AdManager adManager;
		@NonNull
		private final WeakReference<IActivity> activityWR;

		InitTask(@NonNull AdManager adManager, @NonNull IActivity activity) {
			this.adManager = adManager;
			this.activityWR = new WeakReference<>(activity);
		}

		@WorkerThread
		@Override
		protected Boolean doInBackgroundNotCancelledMT(Void... params) {
			if (!AD_ENABLED) {
				return false;
			}
			final IActivity activity = this.activityWR.get();
			if (activity == null) {
				return false;
			}
			if (this.adManager.initialized.getAndSet(true)) {
				return false;
			}
			initOnBackgroundThread(this.adManager, activity);
			return true;
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
		public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
			MTLog.d(this, "onInitializationComplete()");
			final IActivity activity = this.activityWR.get();
			if (activity != null) {
				this.adManager.setupAd(activity);
			}
			Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
			for (String adapterClass : statusMap.keySet()) {
				AdapterStatus status = statusMap.get(adapterClass);
				MTLog.d(this, "Adapter name: %s, Description: %s, Latency: %d",
						adapterClass,
						status == null ? null : status.getDescription(),
						status == null ? null : status.getLatency()
				);
			}
		}
	}

	@Override
	public void onHasAgenciesEnabledUpdated(@NonNull IActivity activity, @Nullable Boolean hasAgenciesEnabled) {
		this.hasAgenciesEnabled = hasAgenciesEnabled;
		refreshAdStatus(activity);
	}

	@Override
	public void setShowingAds(@Nullable Boolean newShowingAds, @NonNull IActivity activity) {
		showingAds = newShowingAds;
		refreshAdStatus(activity);
		refreshRewardedAdStatus(activity);
	}

	@Nullable
	private Boolean dailyUser = null;

	public boolean isDailyUser() {
		if (this.dailyUser == null) {
			this.dailyUser = this.defaultPrefRepository.getValue(
					DefaultPreferenceRepository.PREF_USER_DAILY,
					DefaultPreferenceRepository.PREF_USER_DAILY_DEFAULT
			);
		}
		return this.dailyUser;
	}

	@Nullable
	private Boolean hasLowLoadShowRatio = null;

	public boolean hasLowLoadShowRatio() {
		if (this.hasLowLoadShowRatio == null) {
			final int showCounts = this.defaultPrefRepository.getValue(
					DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS_DEFAULT);
			final int loadCounts = this.defaultPrefRepository.getValue(
					DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS_DEFAULT);
			final boolean newHasLowLoadShowRatio;
			if (loadCounts <= 0) {
				newHasLowLoadShowRatio = false;
			} else if (showCounts <= 0) {
				newHasLowLoadShowRatio = loadCounts >= 10;
			} else {
				newHasLowLoadShowRatio = (((float) showCounts) / loadCounts) < 0.10f;
			}
			this.hasLowLoadShowRatio = newHasLowLoadShowRatio;
		}
		return this.hasLowLoadShowRatio;
	}

	@Nullable
	private Long rewardedUntilInMs = null;

	@Override
	public long getRewardedUntilInMs() {
		if (!AD_ENABLED) {
			return Long.MAX_VALUE; // forever
		}
		if (this.rewardedUntilInMs == null) {
			this.rewardedUntilInMs = this.defaultPrefRepository.getValue(
					DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL,
					DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT);
		}
		return this.rewardedUntilInMs;
	}

	private void setRewardedUntilInMs(long newRewardedUntilInMs) {
		this.rewardedUntilInMs = newRewardedUntilInMs;
		this.defaultPrefRepository.saveAsync(
				DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL,
				rewardedUntilInMs
		);
	}

	@Override
	public void resetRewarded() {
		setRewardedUntilInMs(DefaultPreferenceRepository.PREF_USER_REWARDED_UNTIL_DEFAULT);
	}

	@Override
	public boolean isRewardedNow() {
		if (!AD_ENABLED) {
			return true;
		}
		if (this.demoModeManager.getEnabled()) {
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
				MTLog.d(this, "setRewardedAdListener() > clearing %s...", this.rewardedAdListenerWR.get());
				this.rewardedAdListenerWR.clear();
			}
			this.rewardedAdListenerWR = null;
		} else {
			MTLog.d(this, "setRewardedAdListener() > setting %s...", rewardedAdListener);
			this.rewardedAdListenerWR = new WeakReference<>(rewardedAdListener);
		}
	}

	@Override
	public boolean shouldSkipRewardedAd() {
		if (!isDailyUser()) {
			return true; // always skip for non-daily users
		}
		if (hasLowLoadShowRatio()) {
			return true; // too much loads for too less shows
		}
		if (!isRewardedNow()) {
			return false; // never skip for non-rewarded users
		}
		final long rewardedUntilInMs = getRewardedUntilInMs();
		final long skipRewardedAdUntilInMs = TimeUtils.currentTimeMillis()
				- TimeUnit.HOURS.toMillis(1L) // accounts for "recent" rewards
				+ 2L * getRewardedAdAmountInMs();
		return rewardedUntilInMs > skipRewardedAdUntilInMs;
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
			final int loadCounts = this.defaultPrefRepository.getValue(DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS_DEFAULT);
			this.defaultPrefRepository.saveAsync(DefaultPreferenceRepository.PREF_USER_REWARDED_LOAD_COUNTS, loadCounts + 1);
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
			MTLog.d(this, "refreshRewardedAdStatus() > SKIP (paying user or unknown)");
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
		if (this.demoModeManager.getEnabled()) {
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
		final int showCounts = this.defaultPrefRepository.getValue(DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS_DEFAULT);
		this.defaultPrefRepository.saveAsync(DefaultPreferenceRepository.PREF_USER_REWARDED_SHOW_COUNTS, showCounts + 1);
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
		public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
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
	public void openAdInspector() {
		MobileAds.openAdInspector(this.appContext, error -> {
			if (error == null) {
				MTLog.d(AdManager.this, "Ad inspector closed.");
			} else {
				MTLog.w(AdManager.this, "Ad inspector closed: %d > %s!", error.getCode(), error);
			}
		});
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
			if (adLoaded == null || !adLoaded) { // IF ad was not loaded DO
				setupAd(activity);
			}
		} else { // ELSE IF not showing ads DO
			if (adLoaded != null && adLoaded) { // IF ad was loaded DO
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
		if (hasAgenciesEnabled == null) {
			hasAgenciesEnabled = this.dataSourcesRepository.hasAgenciesEnabled();
		}
		if (!Boolean.TRUE.equals(this.initialized.get())) {
			MTLog.d(this, "isShowingAds() > Not showing ads (not initialized yet).");
			return false; // not showing ads
		}
		// number of agency unknown
		if (!hasAgenciesEnabled) { // no (real) agency installed
			MTLog.d(this, "isShowingAds() > Not showing ads (no agency added).");
			return false; // not showing ads
		} else if (demoModeManager.getEnabled()) {
			MTLog.d(this, "isShowingAds() > Not showing ads (demo mode).");
			return false; // not showing ads
		}
		if (showingAds == null) { // paying status unknown
			MTLog.d(this, "isShowingAds() > Not showing ads (paying status unknown).");
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
		final View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			final AdView adView = adLayout.findViewById(R.id.ad);
			if (adLayout.getVisibility() != View.GONE) {
				adLayout.setVisibility(View.GONE);
			}
			if (adView != null && adView.getVisibility() != View.GONE) {
				adView.setVisibility(View.GONE);
			}
			EdgeToEdgeKt.setNavBarColor(activity.getActivity(), true);
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
		final View adLayout = activity.findViewById(R.id.ad_layout);
		if (adLayout != null) {
			final AdView adView = adLayout.findViewById(R.id.ad);
			if (adView != null && adView.getVisibility() != View.VISIBLE) {
				adView.setVisibility(View.VISIBLE);
			}
			if (adLayout.getVisibility() != View.VISIBLE) {
				adLayout.setVisibility(View.VISIBLE);
			}
			EdgeToEdgeKt.setUpEdgeToEdgeBottom(adLayout);
			EdgeToEdgeKt.setNavBarColor(activity.getActivity(), false);
		}
	}

	private boolean isEnoughSpace(@Nullable Configuration configuration) {
		if (FeatureFlags.F_NAVIGATION) {
			return true; // always show
		}
		if (configuration == null) {
			return false;
		}
		if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
			return true;
		}
		final int sizeMask = configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		final boolean smallScreen = sizeMask == Configuration.SCREENLAYOUT_SIZE_SMALL || sizeMask == Configuration.SCREENLAYOUT_SIZE_NORMAL;
		return !smallScreen;
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
		public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
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
			final IActivity activity = this.activityWR.get();
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
			final IActivity activity = this.activityWR.get();
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
