package org.mtransit.android.di;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.ad.AdManager;
import org.mtransit.android.ad.IAdManager;
import org.mtransit.android.analytics.AnalyticsManager;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.billing.IBillingManager;
import org.mtransit.android.billing.MTBillingManager;
import org.mtransit.android.common.IApplication;
import org.mtransit.android.common.repository.LocalPreferenceRepository;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.CrashlyticsCrashReporter;
import org.mtransit.android.dev.IStrictMode;
import org.mtransit.android.dev.LeakCanaryDetector;
import org.mtransit.android.dev.LeakDetector;
import org.mtransit.android.dev.StrictModeImpl;
import org.mtransit.android.provider.location.GoogleLocationProvider;
import org.mtransit.android.provider.location.MTLocationProvider;
import org.mtransit.android.provider.permission.LocationPermissionProvider;
import org.mtransit.android.provider.sensor.MTSensorManager;
import org.mtransit.android.provider.sensor.SensorManagerImpl;
import org.mtransit.android.ui.MTApplication;

public class Injection {

	@Nullable
	private static IApplication application;

	@Nullable
	private static LeakDetector leakDetector;

	@Nullable
	private static IStrictMode strictMode;

	@Nullable
	private static CrashReporter crashReporter;

	@Nullable
	private static LocationPermissionProvider locationPermissionProvider;

	@Nullable
	private static MTLocationProvider locationProvider;

	@Nullable
	private static MTSensorManager sensorManager;

	@Nullable
	private static IAdManager adManager;

	@Nullable
	private static IAnalyticsManager analyticsManager;

	@Nullable
	private static DataSourceProvider dataSourceProvider;

	@Nullable
	private static LocalPreferenceRepository localPreferenceRepository;

	@Nullable
	private static IBillingManager billingManager;

	@NonNull
	private static IApplication providesApplication() {
		if (application == null) {
			synchronized (Injection.class) {
				if (application == null) {
					application = MTApplication.getIApplication();
				}
			}
		}
		return application;
	}

	@NonNull
	public static LeakDetector providesLeakDetector() {
		if (leakDetector == null) {
			synchronized (Injection.class) {
				if (leakDetector == null) {
					leakDetector = new LeakCanaryDetector();
				}
			}
		}
		return leakDetector;
	}

	@NonNull
	public static IStrictMode providesStrictMode() {
		if (strictMode == null) {
			synchronized (Injection.class) {
				if (strictMode == null) {
					strictMode = new StrictModeImpl();
				}
			}
		}
		return strictMode;
	}

	@NonNull
	public static CrashReporter providesCrashReporter() {
		if (crashReporter == null) {
			synchronized (Injection.class) {
				if (crashReporter == null) {
					crashReporter = new CrashlyticsCrashReporter();
				}
			}
		}
		return crashReporter;
	}

	@NonNull
	public static LocationPermissionProvider providesLocationPermissionProvider() {
		if (locationPermissionProvider == null) {
			synchronized (Injection.class) {
				if (locationPermissionProvider == null) {
					locationPermissionProvider = new LocationPermissionProvider();
				}
			}
		}
		return locationPermissionProvider;
	}

	@NonNull
	public static MTLocationProvider providesLocationProvider() {
		if (locationProvider == null) {
			synchronized (Injection.class) {
				if (locationProvider == null) {
					locationProvider = new GoogleLocationProvider(
							providesApplication(),
							providesLocationPermissionProvider(),
							providesCrashReporter()
					);
				}
			}
		}
		return locationProvider;
	}

	@NonNull
	public static MTSensorManager providesSensorManager() {
		if (sensorManager == null) {
			synchronized (Injection.class) {
				if (sensorManager == null) {
					sensorManager = new SensorManagerImpl(
							providesApplication()
					);
				}
			}
		}
		return sensorManager;
	}

	@NonNull
	public static IAdManager providesAdManager() {
		if (adManager == null) {
			synchronized (Injection.class) {
				if (adManager == null) {
					adManager = new AdManager(
							providesApplication(),
							providesCrashReporter(),
							providesLocationProvider()
					);
				}
			}
		}
		return adManager;
	}

	@NonNull
	public static IAnalyticsManager providesAnalyticsManager() {
		if (analyticsManager == null) {
			synchronized (Injection.class) {
				if (analyticsManager == null) {
					analyticsManager = new AnalyticsManager(
							providesApplication()
					);
				}
			}
		}
		return analyticsManager;
	}

	@NonNull
	public static DataSourceProvider providesDataSourceProvider() {
		if (dataSourceProvider == null) {
			synchronized (Injection.class) {
				if (dataSourceProvider == null) {
					dataSourceProvider = new DataSourceProvider(
							providesApplication(),
							providesAnalyticsManager()
					);
				}
			}
		}
		return dataSourceProvider;
	}

	@NonNull
	private static LocalPreferenceRepository providesLocalPreferenceRepository() {
		org.mtransit.android.commons.MTLog.v(Injection.class.getSimpleName(), "providesLocalPreferenceRepository()");
		if (localPreferenceRepository == null) {
			synchronized (Injection.class) {
				if (localPreferenceRepository == null) {
					localPreferenceRepository = new LocalPreferenceRepository(
							providesApplication()
					);
				}
			}
		}
		return localPreferenceRepository;
	}

	@NonNull
	public static IBillingManager providesBillingManager() {
		if (billingManager == null) {
			synchronized (Injection.class) {
				if (billingManager == null) {
					billingManager = new MTBillingManager(
							providesApplication(),
							providesLocalPreferenceRepository()
					);
				}
			}
		}
		return billingManager;
	}
}
