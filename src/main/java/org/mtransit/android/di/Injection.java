package org.mtransit.android.di;

import androidx.annotation.NonNull;

import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.billing.IBillingManager;
import org.mtransit.android.common.repository.LocalPreferenceRepository;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.IStrictMode;
import org.mtransit.android.dev.LeakDetector;
import org.mtransit.android.provider.location.MTLocationProvider;
import org.mtransit.android.provider.permission.LocationPermissionProvider;
import org.mtransit.android.provider.sensor.MTSensorManager;

public class Injection {

	private Injection() {
	}

	@NonNull
	public static LeakDetector providesLeakDetector() {
		return ServiceLocator.getLeakDetector();
	}

	@NonNull
	public static IStrictMode providesStrictMode() {
		return ServiceLocator.getStrictMode();
	}

	@NonNull
	public static CrashReporter providesCrashReporter() {
		return ServiceLocator.getCrashReporter();
	}

	@NonNull
	public static LocationPermissionProvider providesLocationPermissionProvider() {
		return ServiceLocator.getLocationPermissionProvider();
	}

	@NonNull
	public static MTLocationProvider providesLocationProvider() {
		return ServiceLocator.getLocationProvider();
	}

	@NonNull
	public static MTSensorManager providesSensorManager() {
		return ServiceLocator.getSensorManager();
	}

	@NonNull
	public static IAdManager providesAdManager() {
		return ServiceLocator.getAdManager();
	}

	@NonNull
	public static IAnalyticsManager providesAnalyticsManager() {
		return ServiceLocator.getAnalyticsManager();
	}

	@NonNull
	public static DataSourceProvider providesDataSourceProvider() {
		return ServiceLocator.getDataSourceProvider();
	}

	@NonNull
	public static LocalPreferenceRepository providesLocalPreferenceRepository() {
		return ServiceLocator.getLocalPreferenceRepository();
	}

	@NonNull
	public static IBillingManager providesBillingManager() {
		return ServiceLocator.getBillingManager();
	}
}
