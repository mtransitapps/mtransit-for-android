package org.mtransit.android.ad;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;

import org.mtransit.android.ad.banner.BannerAdManager;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.view.common.IActivity;

import java.lang.ref.WeakReference;
import java.util.Map;

class OnInitializationCompleteListener implements com.google.android.gms.ads.initialization.OnInitializationCompleteListener, MTLog.Loggable {

	private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + OnInitializationCompleteListener.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final BannerAdManager bannerAdManager;
	@NonNull
	private final WeakReference<IActivity> activityWR;

	OnInitializationCompleteListener(@NonNull BannerAdManager bannerAdManager, @Nullable IActivity activity) {
		this.bannerAdManager = bannerAdManager;
		this.activityWR = new WeakReference<>(activity);
	}

	@Override
	public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
		MTLog.d(this, "onInitializationComplete()");
		final IActivity activity = this.activityWR.get();
		if (activity != null) {
			this.bannerAdManager.setupBannerAd(activity, false);
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
