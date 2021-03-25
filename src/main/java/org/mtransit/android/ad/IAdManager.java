package org.mtransit.android.ad;

import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.ui.view.common.IActivity;

public interface IAdManager {

	void init(@NonNull IApplication application);

	void onNbAgenciesUpdated(@NonNull IActivity activity, int nbAgencies);

	void onModulesUpdated(@NonNull IActivity activity);

	void setShowingAds(@Nullable Boolean newShowingAds, @NonNull IActivity activity);

	int getRewardedAdAmount();

	long getRewardedAdAmountInMs();

	void linkRewardedAd(@NonNull IActivity activity);

	void unlinkRewardedAd(@NonNull IActivity activity);

	void refreshRewardedAdStatus(@NonNull IActivity activity);

	boolean isRewardedAdAvailableToShow();

	@SuppressWarnings("UnusedReturnValue")
	boolean showRewardedAd(@NonNull IActivity activity);

	int getBannerHeightInPx(@Nullable IActivity activity);

	void adaptToScreenSize(@NonNull IActivity activity, @Nullable Configuration configuration);

	void resumeAd(@NonNull IActivity activity);

	void pauseAd(@NonNull IActivity activity);

	void destroyAd(@NonNull IActivity activity);

	long getRewardedUntilInMs();

	boolean isRewardedNow();

	void setRewardedAdListener(@Nullable RewardedAdListener rewardedAdListener);

	interface RewardedAdListener {
		void onRewardedAdStatusChanged();

		boolean skipRewardedAd();
	}
}
