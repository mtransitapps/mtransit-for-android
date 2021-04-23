package org.mtransit.android.di;

import android.content.pm.PackageManager;

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
import org.mtransit.android.data.DataSourceType.POIManagerTypeShortNameComparator;
import org.mtransit.android.datasource.DataSourceRequestManager;
import org.mtransit.android.datasource.DataSourcesCache;
import org.mtransit.android.datasource.DataSourcesDatabase;
import org.mtransit.android.datasource.DataSourcesReader;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.dev.CrashReporter;
import org.mtransit.android.dev.CrashlyticsCrashReporter;
import org.mtransit.android.dev.IStrictMode;
import org.mtransit.android.dev.LeakCanaryDetector;
import org.mtransit.android.dev.LeakDetector;
import org.mtransit.android.dev.StrictModeImpl;
import org.mtransit.android.provider.FavoriteRepository;
import org.mtransit.android.provider.location.GoogleLocationProvider;
import org.mtransit.android.provider.location.MTLocationProvider;
import org.mtransit.android.provider.permission.LocationPermissionProvider;
import org.mtransit.android.provider.sensor.MTSensorManager;
import org.mtransit.android.provider.sensor.SensorManagerImpl;
import org.mtransit.android.ui.MTApplication;

@SuppressWarnings("WeakerAccess")
public class Injection {

	@Nullable
	private static IApplication application;

	@Nullable
	private static PackageManager packageManager;

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
	private static LocalPreferenceRepository localPreferenceRepository;

	@Nullable
	private static IBillingManager billingManager;

	@Nullable
	private static DataSourcesDatabase dataSourcesDatabase;

	@Nullable
	private static DataSourcesCache dataSourcesCache;

	@Nullable
	private static DataSourcesReader dataSourcesReader;

	@Nullable
	private static DataSourcesRepository dataSourcesRepository;

	@Nullable
	private static DataSourceRequestManager dataSourceRequestManager;

	@Nullable
	private static FavoriteRepository favoriteRepository;

	@Nullable
	private static POIManagerTypeShortNameComparator poiManagerTypeShortNameComparator;

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
	public static PackageManager providesPackageManager() {
		if (packageManager == null) {
			synchronized (Injection.class) {
				if (packageManager == null) {
					packageManager = providesApplication().requireApplication().getPackageManager();
				}
			}
		}
		return packageManager;
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
							providesLocationProvider(),
							providesDataSourcesRepository()
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
	private static LocalPreferenceRepository providesLocalPreferenceRepository() {
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

	@NonNull
	public static DataSourcesDatabase providesDataSourcesDatabase() {
		if (dataSourcesDatabase == null) {
			synchronized (Injection.class) {
				if (dataSourcesDatabase == null) {
					dataSourcesDatabase = DataSourcesDatabase.getInstance(
							providesApplication()
					);
				}
			}
		}
		return dataSourcesDatabase;
	}

	@NonNull
	public static DataSourcesCache providesDataSourcesCache() {
		if (dataSourcesCache == null) {
			synchronized (Injection.class) {
				if (dataSourcesCache == null) {
					dataSourcesCache = new DataSourcesCache(
							providesDataSourcesDatabase()
					);
				}
			}
		}
		return dataSourcesCache;
	}

	@NonNull
	public static DataSourcesReader providesDataSourcesReader() {
		if (dataSourcesReader == null) {
			synchronized (Injection.class) {
				if (dataSourcesReader == null) {
					dataSourcesReader = new DataSourcesReader(
							providesApplication(),
							providesPackageManager(),
							providesAnalyticsManager(),
							providesDataSourcesDatabase(),
							providesDataSourceRequestManager()
					);
				}
			}
		}
		return dataSourcesReader;
	}

	@NonNull
	public static DataSourcesRepository providesDataSourcesRepository() {
		if (dataSourcesRepository == null) {
			synchronized (Injection.class) {
				if (dataSourcesRepository == null) {
					dataSourcesRepository = new DataSourcesRepository(
							providesApplication(),
							providesDataSourcesCache(),
							providesDataSourcesReader()
					);
				}
			}
		}
		return dataSourcesRepository;
	}

	@NonNull
	public static DataSourceRequestManager providesDataSourceRequestManager() {
		if (dataSourceRequestManager == null) {
			synchronized (Injection.class) {
				if (dataSourceRequestManager == null) {
					dataSourceRequestManager = new DataSourceRequestManager(
							providesApplication()
					);
				}
			}
		}
		return dataSourceRequestManager;
	}

	@NonNull
	public static FavoriteRepository providesFavoriteRepository() {
		if (favoriteRepository == null) {
			synchronized (Injection.class) {
				if (favoriteRepository == null) {
					favoriteRepository = new FavoriteRepository(
							providesApplication()
					);
				}
			}
		}
		return favoriteRepository;
	}

	@NonNull
	public static POIManagerTypeShortNameComparator providesPOIManagerTypeShortNameComparator() {
		if (poiManagerTypeShortNameComparator == null) {
			synchronized (Injection.class) {
				if (poiManagerTypeShortNameComparator == null) {
					poiManagerTypeShortNameComparator = new POIManagerTypeShortNameComparator(
							providesApplication(),
							providesDataSourcesRepository()
					);
				}
			}
		}
		return poiManagerTypeShortNameComparator;
	}
}
