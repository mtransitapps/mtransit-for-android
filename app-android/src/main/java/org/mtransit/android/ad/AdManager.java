package org.mtransit.android.ad;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;

import org.mtransit.android.BuildConfig;
import org.mtransit.android.ad.banner.BannerAdManager;
import org.mtransit.android.ad.rewarded.RewardedAdManager;
import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.common.IActivity;

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

	public static final String LOG_TAG = AdManager.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final Context appContext;
	@NonNull
	private final GlobalAdManager globalAdManager;
	@NonNull
	private final BannerAdManager bannerAdManager;
	@NonNull
	private final RewardedAdManager rewardedAdManager;

	@Inject
	AdManager(
			@NonNull @ApplicationContext Context appContext,
			@NonNull GlobalAdManager globalAdManager,
			@NonNull BannerAdManager bannerAdManager,
			@NonNull RewardedAdManager rewardedAdManager
	) {
		this.appContext = appContext;
		this.globalAdManager = globalAdManager;
		this.bannerAdManager = bannerAdManager;
		this.rewardedAdManager = rewardedAdManager;
	}

	@Override
	public void init(@NonNull IActivity activity, @Nullable IAdScreenFragment adScreenFragment) {
		this.globalAdManager.init(activity, adScreenFragment, this.bannerAdManager);
	}

	@Override
	public void onHasAgenciesEnabledUpdated(@Nullable Boolean hasAgenciesEnabled, @NonNull IActivity activity, @Nullable IAdScreenFragment adScreenFragment) {
		this.globalAdManager.onHasAgenciesEnabledUpdated(hasAgenciesEnabled);
		onShowingAdsUpdated(activity, adScreenFragment);
	}

	@Override
	public void setShowingAds(@Nullable Boolean newShowingAds, @NonNull IActivity activity, @Nullable IAdScreenFragment adScreenFragment) {
		this.globalAdManager.setShowingAds(newShowingAds);
		onShowingAdsUpdated(activity, adScreenFragment);
	}

	private void onShowingAdsUpdated(@NonNull IActivity activity, @Nullable IAdScreenFragment adScreenFragment) {
		this.bannerAdManager.refreshBannerAdStatus(activity, adScreenFragment);
		refreshRewardedAdStatus(activity);
	}

	@Override
	public long getRewardedUntilInMs() {
		return this.globalAdManager.getRewardedUntilInMs();
	}

	@Override
	public void resetRewarded() {
		this.globalAdManager.resetRewarded();
	}

	@Override
	public boolean isRewardedNow() {
		return this.globalAdManager.isRewardedNow();
	}

	@Override
	public void setRewardedAdListener(@Nullable RewardedAdListener rewardedAdListener) {
		this.rewardedAdManager.setRewardedAdListener(rewardedAdListener);
	}

	@Override
	public boolean shouldSkipRewardedAd() {
		return this.globalAdManager.shouldSkipRewardedAd();
	}

	@Override
	public void linkRewardedAd(@NonNull IActivity activity) {
		this.rewardedAdManager.linkRewardedAd(activity);
	}

	@Override
	public void unlinkRewardedAd(@NonNull IActivity activity) {
		this.rewardedAdManager.unlinkRewardedAd(activity);
	}

	@Override
	public void refreshRewardedAdStatus(@NonNull IActivity activity) {
		this.rewardedAdManager.refreshRewardedAdStatus(activity);
	}

	@NonNull
	public static AdRequest getAdRequest(@NonNull IContext context) {
		AdRequest.Builder adRequestBd = new AdRequest.Builder();
		for (String keyword : AdConstants.KEYWORDS) {
			adRequestBd.addKeyword(keyword);
		}
		AdRequest adRequest = adRequestBd.build();
		if (BuildConfig.DEBUG) {
			MTLog.d(LOG_TAG, "getAdRequest() > test device? %s.", adRequest.isTestDevice(context.requireContext()));
		}
		return adRequest;
	}

	@Override
	public boolean isRewardedAdAvailableToShow() {
		return this.rewardedAdManager.isRewardedAdAvailableToShow();
	}

	@Override
	public boolean showRewardedAd(@NonNull IActivity activity) {
		return this.rewardedAdManager.showRewardedAd(activity);
	}

	@Override
	public int getRewardedAdAmount() {
		return this.globalAdManager.getRewardedAdAmount();
	}

	@Override
	public long getRewardedAdAmountInMs() {
		return this.globalAdManager.getRewardedAdAmountInMs();
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
		return this.bannerAdManager.getBannerHeightInPx(activity);
	}

	@Override
	public void adaptToScreenSize(@NonNull IActivity activity, @Nullable Configuration configuration) {
		this.bannerAdManager.adaptToScreenSize(activity, configuration);
	}

	@Override
	public void pauseAd(@NonNull IActivity activity) {
		this.bannerAdManager.pauseAd(activity);
	}

	@Override
	public void resumeAd(@NonNull IActivity activity) {
		this.bannerAdManager.resumeAd(activity);
	}

	@Override
	public void destroyAd(@NonNull IActivity activity) {
		this.bannerAdManager.destroyAd(activity);
	}

	@Override
	public void onResumeScreen(@NonNull IActivity activity, @NonNull IAdScreenFragment adScreenFragment) {
		this.bannerAdManager.refreshBannerAdStatus(activity, adScreenFragment, true);
	}
}
