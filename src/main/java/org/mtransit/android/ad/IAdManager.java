package org.mtransit.android.ad;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.common.IApplication;
import org.mtransit.android.ui.view.common.IActivity;

import android.content.res.Configuration;

public interface IAdManager {

	void init(@NonNull IApplication application);

	void onModulesUpdated(@NonNull IActivity activity);

	void setShowingAds(@Nullable Boolean newShowingAds, @NonNull IActivity activity);

	int getBannerHeightInPx(@Nullable IActivity activity);

	void adaptToScreenSize(@NonNull IActivity activity, @Nullable Configuration configuration);

	void resumeAd(@NonNull IActivity activity);

	void pauseAd(@NonNull IActivity activity);

	void destroyAd(@NonNull IActivity activity);
}
