package org.mtransit.android.ad;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;

import org.mtransit.android.R;
import org.mtransit.android.ad.banner.BannerAdManager;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.ui.view.common.IActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO test this 1st
@SuppressWarnings("deprecation")
class InitTask extends MTCancellableAsyncTask<Void, Void, Boolean> {

	private static final String LOG_TAG = AdManager.class.getSimpleName() + ">" + InitTask.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final GlobalAdManager globalAdManager;
	@NonNull
	private final BannerAdManager bannerAdManager;
	@NonNull
	private final WeakReference<IActivity> activityWR;
	@NonNull
	private final WeakReference<IAdScreenFragment> adScreenFragmentWR;

	InitTask(@NonNull GlobalAdManager globalAdManager,
			 @NonNull BannerAdManager bannerAdManager,
			 @NonNull IActivity activity,
			 @Nullable IAdScreenFragment adScreenFragment) {
		this.globalAdManager = globalAdManager;
		this.bannerAdManager = bannerAdManager;
		this.activityWR = new WeakReference<>(activity);
		this.adScreenFragmentWR = new WeakReference<>(adScreenFragment);
	}

	@WorkerThread
	@Override
	protected Boolean doInBackgroundNotCancelledMT(Void... params) {
		if (!AdConstants.AD_ENABLED) {
			return false;
		}
		final IActivity activity = this.activityWR.get();
		if (activity == null) {
			return false;
		}
		if (this.globalAdManager.getAndSetInitialized(true)) {
			return false;
		}
		initOnBackgroundThread(activity);
		return true;
	}

	@WorkerThread
	private void initOnBackgroundThread(@NonNull IActivity activity) {
		if (AdConstants.DEBUG) {
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
				new OnInitializationCompleteListener(this.bannerAdManager, activity, this.adScreenFragmentWR.get())
		);
	}
}
