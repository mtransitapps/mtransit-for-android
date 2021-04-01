package org.mtransit.android.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.AnyThread;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.collection.ArrayMap;

import org.mtransit.android.R;
import org.mtransit.android.analytics.AnalyticsUserProperties;
import org.mtransit.android.analytics.IAnalyticsManager;
import org.mtransit.android.common.IApplication;
import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.android.datasource.DataSourcesReader;
import org.mtransit.android.datasource.DataSourcesRepository;
import org.mtransit.android.di.Injection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import static org.mtransit.commons.FeatureFlags.F_CACHE_DATA_SOURCES;

@Deprecated
@SuppressWarnings("WeakerAccess")
@AnyThread
public class DataSourceProvider implements IContext, MTLog.Loggable {

	private static final String LOG_TAG = DataSourceProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private boolean initialized = false;

	@NonNull
	public static DataSourceProvider get(@SuppressWarnings("unused") @Nullable Context context) { // INIT SYNC
		DataSourceProvider instance = Injection.providesDataSourceProvider();
		if (!instance.isInitialized()) {
			initInstance();
		}
		return instance;
	}

	@NonNull
	public static DataSourceProvider get() { // NO NOT INIT
		return Injection.providesDataSourceProvider();
	}

	private synchronized static void initInstance() {
		DataSourceProvider instance = Injection.providesDataSourceProvider();
		if (instance.isInitialized()) {
			return; // already initialized
		}
		instance.init();
	}

	public static boolean isSet() {
		if (F_CACHE_DATA_SOURCES) {
			return false;
		}
		DataSourceProvider instance = Injection.providesDataSourceProvider();
		return instance.isInitialized();
	}

	public static void destroy() {
		if (F_CACHE_DATA_SOURCES) {
			return;
		}
		DataSourceProvider instance = Injection.providesDataSourceProvider();
		instance.onDestroy();
	}

	public boolean isInitialized() {
		return this.initialized;
	}

	private void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	@Nullable
	@Override
	public Context getContext() {
		return this.application.getContext();
	}

	@NonNull
	@Override
	public Context requireContext() throws IllegalStateException {
		return this.application.requireContext();
	}

	private static boolean resumed = false;

	public static void onPause() {
		resumed = false;
	}

	public static void onResume() {
		resumed = true;
		if (triggerModulesUpdated) {
			triggerModulesUpdated();
		}
	}

	@Nullable
	private static String agencyProviderMetaData;

	@NonNull
	public static String getAgencyProviderMetaData(@NonNull Context context) {
		if (agencyProviderMetaData == null) {
			agencyProviderMetaData = context.getString(R.string.agency_provider);
		}
		return agencyProviderMetaData;
	}

	@Nullable
	private static String scheduleProviderMetaData;

	@NonNull
	private static String getScheduleProviderMetaData(@NonNull Context context) {
		if (scheduleProviderMetaData == null) {
			scheduleProviderMetaData = context.getString(R.string.schedule_provider);
		}
		return scheduleProviderMetaData;
	}

	@Nullable
	private static String statusProviderMetaData;

	@NonNull
	private static String getStatusProviderMetaData(@NonNull Context context) {
		if (statusProviderMetaData == null) {
			statusProviderMetaData = context.getString(R.string.status_provider);
		}
		return statusProviderMetaData;
	}

	@Nullable
	private static String serviceUpdateProviderMetaData;

	@NonNull
	private static String getServiceUpdateProviderMetaData(@NonNull Context context) {
		if (serviceUpdateProviderMetaData == null) {
			serviceUpdateProviderMetaData = context.getString(R.string.service_update_provider);
		}
		return serviceUpdateProviderMetaData;
	}

	@Nullable
	private static String newsProviderMetaData;

	@NonNull
	private static String getNewsProviderMetaData(@NonNull Context context) {
		if (newsProviderMetaData == null) {
			newsProviderMetaData = context.getString(R.string.news_provider);
		}
		return newsProviderMetaData;
	}

	public static boolean isProvider(@NonNull Context context, @Nullable String pkg) {
		if (F_CACHE_DATA_SOURCES) {
			//noinspection deprecation
			return DataSourcesReader.isAProvider(context, pkg);
		}
		if (TextUtils.isEmpty(pkg)) {
			return false;
		}
		ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(context, pkg);
		if (providers == null || providers.length == 0) {
			return false;
		}
		String agencyProviderMetaData = getAgencyProviderMetaData(context);
		String scheduleProviderMetaData = getScheduleProviderMetaData(context);
		String statusProviderMetaData = getStatusProviderMetaData(context);
		String serviceUpdateProviderMetaData = getServiceUpdateProviderMetaData(context);
		String newsProviderMetaData = getNewsProviderMetaData(context);
		for (ProviderInfo provider : providers) {
			final Bundle providerMetaData = provider.metaData;
			if (providerMetaData != null) {
				if (agencyProviderMetaData.equals(providerMetaData.getString(agencyProviderMetaData))) {
					return true;
				}
				if (scheduleProviderMetaData.equals(providerMetaData.getString(scheduleProviderMetaData))) {
					return true;
				}
				if (statusProviderMetaData.equals(providerMetaData.getString(statusProviderMetaData))) {
					return true;
				}
				if (serviceUpdateProviderMetaData.equals(providerMetaData.getString(serviceUpdateProviderMetaData))) {
					return true;
				}
				if (newsProviderMetaData.equals(providerMetaData.getString(newsProviderMetaData))) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean resetIfNecessary(@NonNull Context context) {
		DataSourceProvider instance = Injection.providesDataSourceProvider();
		if (instance.isInitialized()) {
			if (hasChanged(instance, context)) {
				destroy();
				triggerModulesUpdated();
				return true;
			}
		}
		return false;
	}

	@SuppressLint("QueryPermissionsNeeded")
	private synchronized static boolean hasChanged(DataSourceProvider current, @NonNull Context context) {
		if (current == null) {
			return true;
		}
		if (F_CACHE_DATA_SOURCES) {
			current.updateFromDataSourceRepository(false);
			return false; // will be handled by initFromDataSourceRepository()
		}
		String agencyProviderMetaData = getAgencyProviderMetaData(context);
		String scheduleProviderMetaData = getScheduleProviderMetaData(context);
		String statusProviderMetaData = getStatusProviderMetaData(context);
		String serviceUpdateProviderMetaData = getServiceUpdateProviderMetaData(context);
		String newsProviderMetaData = getNewsProviderMetaData(context);
		int nbAgencyProviders = 0, nbScheduleProviders = 0, nbStatusProviders = 0, nbServiceUpdateProviders = 0, nbNewsProviders = 0;
		PackageManager pm = context.getPackageManager();
		for (PackageInfo packageInfo : pm.getInstalledPackages(PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA)) {
			ProviderInfo[] providers = packageInfo.providers;
			if (providers == null) {
				continue;
			}
			for (ProviderInfo provider : providers) {
				if (provider == null) {
					continue;
				}
				final Bundle providerMetaData = provider.metaData;
				if (providerMetaData == null) {
					continue;
				}
				if (agencyProviderMetaData.equals(providerMetaData.getString(agencyProviderMetaData))) {
					if (!current.hasAgency(provider.authority)) {
						return true;
					}
					nbAgencyProviders++;
				}
				if (statusProviderMetaData.equals(providerMetaData.getString(statusProviderMetaData))) {
					if (current.getStatusProvider(provider.authority) == null) {
						return true;
					}
					nbStatusProviders++;
				}
				if (scheduleProviderMetaData.equals(providerMetaData.getString(scheduleProviderMetaData))) {
					if (current.getScheduleProvider(provider.authority) == null) {
						return true;
					}
					nbScheduleProviders++;
				}
				if (serviceUpdateProviderMetaData.equals(providerMetaData.getString(serviceUpdateProviderMetaData))) {
					if (current.getServiceUpdateProvider(provider.authority) == null) {
						return true;
					}
					nbServiceUpdateProviders++;
				}
				if (newsProviderMetaData.equals(providerMetaData.getString(newsProviderMetaData))) {
					if (current.getNewsProvider(provider.authority) == null) {
						return true;
					}
					nbNewsProviders++;
				}
			}
		}
		//noinspection RedundantIfStatement
		if (nbAgencyProviders != CollectionUtils.getSize(current.allAgenciesAuthority) //
				|| nbStatusProviders != CollectionUtils.getSize(current.allStatusProviders) //
				|| nbScheduleProviders != CollectionUtils.getSize(current.allScheduleProviders) //
				|| nbServiceUpdateProviders != CollectionUtils.getSize(current.allServiceUpdateProviders) //
				|| nbNewsProviders != CollectionUtils.getSize(current.allNewsProviders)) {
			return true;
		}
		return false;
	}

	@NonNull
	private final HashSet<String> allAgenciesAuthority = new HashSet<>();

	@NonNull
	private final ArrayMap<String, Integer> agenciesAuthorityTypeId = new ArrayMap<>();

	@NonNull
	private final ArrayMap<String, String> agenciesAuthorityPkg = new ArrayMap<>();

	@NonNull
	private final ArrayMap<String, Boolean> agenciesAuthorityIsRts = new ArrayMap<>();

	private final ArrayList<DataSourceType> allAgencyTypes = new ArrayList<>();

	@Nullable
	private ArrayList<AgencyProperties> allAgencies = null;

	private final ArrayList<StatusProviderProperties> allStatusProviders = new ArrayList<>();

	private final ArrayList<ScheduleProviderProperties> allScheduleProviders = new ArrayList<>();

	private final ArrayList<ServiceUpdateProviderProperties> allServiceUpdateProviders = new ArrayList<>();

	private final ArrayList<NewsProviderProperties> allNewsProviders = new ArrayList<>();

	@Nullable
	private ArrayMap<String, AgencyProperties> allAgenciesByAuthority = null;

	private final ArrayMap<String, StatusProviderProperties> allStatusProvidersByAuthority = new ArrayMap<>();

	private final ArrayMap<String, ScheduleProviderProperties> allScheduleProvidersByAuthority = new ArrayMap<>();

	private final ArrayMap<String, ServiceUpdateProviderProperties> allServiceUpdateProvidersByAuthority = new ArrayMap<>();

	private final ArrayMap<String, NewsProviderProperties> allNewsProvidersByAuthority = new ArrayMap<>();

	@Nullable
	private SparseArray<ArrayList<AgencyProperties>> allAgenciesByTypeId = null;

	private final ArrayMap<String, HashSet<StatusProviderProperties>> statusProvidersByTargetAuthority = new ArrayMap<>();

	private final ArrayMap<String, HashSet<ScheduleProviderProperties>> scheduleProvidersByTargetAuthority = new ArrayMap<>();

	private final ArrayMap<String, HashSet<ServiceUpdateProviderProperties>> serviceUpdateProvidersByTargetAuthority = new ArrayMap<>();

	private final ArrayMap<String, HashSet<NewsProviderProperties>> newsProvidersByTargetAuthority = new ArrayMap<>();

	private final ArrayMap<String, Integer> allAgenciesColorInts = new ArrayMap<>();

	@NonNull
	private final IApplication application;
	@NonNull
	private final IAnalyticsManager analyticsManager;
	@NonNull
	private final DataSourcesRepository dataSourcesRepository;

	public DataSourceProvider(@NonNull IApplication application,
							  @NonNull IAnalyticsManager analyticsManager) {
		this.application = application;
		this.analyticsManager = analyticsManager;
		this.dataSourcesRepository = Injection.providesDataSourcesRepository();
	}

	@NonNull
	public List<DataSourceType> getAvailableAgencyTypes() {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getAllDataSourceTypes();
		}
		return new ArrayList<>(this.allAgencyTypes); // copy
	}

	public int getAllAgenciesCount() {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getAllAgenciesCount();
		}
		return CollectionUtils.getSize(this.allAgenciesAuthority);
	}

	@Nullable
	public List<AgencyProperties> getAllAgencies(@SuppressWarnings("unused") @Nullable Context context) {
		if (!isAgencyPropertiesSet()) {
			initAgencyProperties();
		}
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getAllAgencies();
		}
		if (this.allAgencies == null) {
			return null;
		}
		return new ArrayList<>(this.allAgencies); // copy
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isAgencyPropertiesSet() {
		return this.allAgencies != null;
	}

	private synchronized void initAgencyProperties() {
		if (this.allAgencies != null) {
			return; // too late
		}
		if (F_CACHE_DATA_SOURCES) {
			updateFromDataSourceRepository(false);
			return;
		}
		try {
			Context context = requireContext();
			if (CollectionUtils.getSize(this.allAgenciesAuthority) > 0) {
				ArrayList<AgencyProperties> allAgencies = new ArrayList<>();
				ArrayMap<String, AgencyProperties> allAgenciesByAuthority = new ArrayMap<>();
				SparseArray<ArrayList<AgencyProperties>> allAgenciesByTypeId = new SparseArray<>();
				for (String authority : this.allAgenciesAuthority) {
					final String pkg = this.agenciesAuthorityPkg.get(authority);
					boolean isRTS = Boolean.TRUE.equals(this.agenciesAuthorityIsRts.get(authority));
					Integer typeId = this.agenciesAuthorityTypeId.get(authority);
					if (typeId != null && typeId >= 0 && pkg != null) {
						DataSourceType type = DataSourceType.parseId(typeId);
						if (type != null) {
							JPaths logo = null;
							if (isRTS) {
								logo = DataSourceManager.findAgencyRTSRouteLogo(context, authority);
							}
							AgencyProperties agency = DataSourceManager.findAgencyProperties(
									context,
									authority,
									type,
									isRTS,
									logo,
									pkg,
									PackageManagerUtils.getAppLongVersionCode(context, pkg),
									PackageManagerUtils.isAppEnabled(context, pkg),
									0
							);
							if (agency != null) {
								allAgencies.add(agency);
								allAgenciesByAuthority.put(agency.getAuthority(), agency);
								DataSourceType newAgencyType = agency.getType();
								if (allAgenciesByTypeId.get(newAgencyType.getId()) == null) {
									allAgenciesByTypeId.put(newAgencyType.getId(), new ArrayList<>());
								}
								allAgenciesByTypeId.get(newAgencyType.getId()).add(agency);
								this.allAgenciesColorInts.put(agency.getAuthority(), agency.hasColor() ? agency.getColorInt() : null);
							} else {
								MTLog.w(this, "Invalid agency '%s'!", authority);
							}
						} else {
							MTLog.w(this, "Invalid type for ID '%s', skipping agency provider '%s'!", typeId, authority);
						}
					} else {
						MTLog.w(this, "Invalid type ID '%s', skipping agency provider '%s'!.", typeId, authority);
					}
				}
				CollectionUtils.sort(allAgencies, AgencyProperties.getSHORT_NAME_COMPARATOR());
				for (int i = 0; i < allAgenciesByTypeId.size(); i++) {
					CollectionUtils.sort(allAgenciesByTypeId.get(allAgenciesByTypeId.keyAt(i)), AgencyProperties.getSHORT_NAME_COMPARATOR());
				}
				this.allAgencies = allAgencies;
				this.allAgenciesByAuthority = allAgenciesByAuthority;
				this.allAgenciesByTypeId = allAgenciesByTypeId;
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while initializing agencies properties!");
			this.allAgencies = null; // reset
			this.allAgenciesByAuthority = null; // reset
			this.allAgenciesByTypeId = null; // reset
			this.allAgenciesColorInts.clear(); // reset
		}
	}

	private boolean hasAgency(@NonNull String authority) {
		return this.allAgenciesAuthority.contains(authority);
	}

	@Nullable
	public AgencyProperties getAgency(@SuppressWarnings("unused") @Nullable Context context, @NonNull String authority) {
		if (!isAgencyPropertiesSet()) {
			initAgencyProperties();
		}
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getAgency(authority);
		}
		if (this.allAgenciesByAuthority == null) {
			return null;
		}
		return this.allAgenciesByAuthority.get(authority);
	}

	@Nullable
	public List<AgencyProperties> getTypeDataSources(@SuppressWarnings("unused") @Nullable Context context, int typeId) {
		final DataSourceType type = DataSourceType.parseId(typeId);
		if (type == null) {
			return null;
		}
		return getTypeDataSources(context, type);
	}

	@Nullable
	public List<AgencyProperties> getTypeDataSources(@SuppressWarnings("unused") @Nullable Context context, @NonNull DataSourceType type) {
		if (!isAgencyPropertiesSet()) {
			initAgencyProperties();
		}
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getTypeDataSources(type);
		}
		ArrayList<AgencyProperties> agencies = this.allAgenciesByTypeId == null ? null : this.allAgenciesByTypeId.get(type.getId());
		if (agencies == null) {
			return null;
		}
		return new ArrayList<>(agencies); // copy
	}

	@Nullable
	@ColorInt
	Integer getAgencyColorInt(@SuppressWarnings("unused") @Nullable Context context, @NonNull String authority) {
		if (!isAgencyPropertiesSet()) {
			initAgencyProperties();
		}
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getAgencyColorInt(authority);
		}
		return this.allAgenciesColorInts.get(authority);
	}

	@SuppressWarnings("unused")
	@Nullable
	public String getAgencyPkg(@NonNull String authority) {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getAgencyPkg(authority);
		}
		return this.agenciesAuthorityPkg.get(authority);
	}

	@Nullable
	private StatusProviderProperties getStatusProvider(@NonNull String authority) {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getStatusProvider(authority);
		}
		return this.allStatusProvidersByAuthority.get(authority);
	}

	@Nullable
	private ScheduleProviderProperties getScheduleProvider(@NonNull String authority) {
		MTLog.v(this, "getScheduleProvider(%s)", authority);
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getScheduleProvider(authority);
		}
		return this.allScheduleProvidersByAuthority.get(authority);
	}

	@Nullable
	private ServiceUpdateProviderProperties getServiceUpdateProvider(@NonNull String authority) {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getServiceUpdateProvider(authority);
		}
		return this.allServiceUpdateProvidersByAuthority.get(authority);
	}

	@NonNull
	public Set<NewsProviderProperties> getAllNewsProvider() {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getAllNewsProviders();
		}
		return new HashSet<>(this.allNewsProviders); // copy
	}

	@Nullable
	private NewsProviderProperties getNewsProvider(@NonNull String authority) {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getNewsProvider(authority);
		}
		return this.allNewsProvidersByAuthority.get(authority);
	}

	@Nullable
	public Set<StatusProviderProperties> getTargetAuthorityStatusProviders(@NonNull String targetAuthority) {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getStatusProviders(targetAuthority);
		}
		HashSet<StatusProviderProperties> statusProviders = this.statusProvidersByTargetAuthority.get(targetAuthority);
		if (statusProviders == null) {
			return null;
		}
		return new HashSet<>(statusProviders); // copy
	}

	@Nullable
	public Set<ScheduleProviderProperties> getTargetAuthorityScheduleProviders(@NonNull String targetAuthority) {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getScheduleProviders(targetAuthority);
		}
		HashSet<ScheduleProviderProperties> scheduleProviders = this.scheduleProvidersByTargetAuthority.get(targetAuthority);
		if (scheduleProviders == null) {
			return null;
		}
		return new HashSet<>(scheduleProviders); // copy
	}

	@Nullable
	public Set<ServiceUpdateProviderProperties> getTargetAuthorityServiceUpdateProviders(@NonNull String targetAuthority) {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getServiceUpdateProviders(targetAuthority);
		}
		HashSet<ServiceUpdateProviderProperties> serviceUpdateProviders = this.serviceUpdateProvidersByTargetAuthority.get(targetAuthority);
		if (serviceUpdateProviders == null) {
			return null;
		}
		return new HashSet<>(serviceUpdateProviders); // copy
	}

	@Nullable
	public Set<NewsProviderProperties> getTargetAuthorityNewsProviders(@NonNull String targetAuthority) {
		if (F_CACHE_DATA_SOURCES) {
			return this.dataSourcesRepository.getNewsProviders(targetAuthority);
		}
		HashSet<NewsProviderProperties> newsProviders = this.newsProvidersByTargetAuthority.get(targetAuthority);
		if (newsProviders == null) {
			return null;
		}
		return new HashSet<>(newsProviders); // copy
	}

	private void onDestroy() {
		if (F_CACHE_DATA_SOURCES) {
			return;
		}
		if (!isInitialized()) {
			return; // already destroyed
		}
		if (this.allAgencies != null) {
			this.allAgencies.clear();
			this.allAgencies = null; // reset
		}
		if (this.allAgenciesByTypeId != null) {
			this.allAgenciesByTypeId.clear();
			this.allAgenciesByTypeId = null; // reset
		}
		if (this.allAgenciesByAuthority != null) {
			this.allAgenciesByAuthority.clear();
			this.allAgenciesByAuthority = null; // reset
		}
		this.allAgencyTypes.clear();
		this.allAgenciesAuthority.clear();
		this.agenciesAuthorityIsRts.clear();
		this.agenciesAuthorityTypeId.clear();
		this.agenciesAuthorityPkg.clear();
		this.allStatusProviders.clear();
		this.allStatusProvidersByAuthority.clear();
		this.statusProvidersByTargetAuthority.clear();
		this.allScheduleProviders.clear();
		this.allScheduleProvidersByAuthority.clear();
		this.scheduleProvidersByTargetAuthority.clear();
		this.allServiceUpdateProviders.clear();
		this.allServiceUpdateProvidersByAuthority.clear();
		this.serviceUpdateProvidersByTargetAuthority.clear();
		this.allNewsProviders.clear();
		this.allNewsProvidersByAuthority.clear();
		this.newsProvidersByTargetAuthority.clear();
		this.allAgenciesColorInts.clear();
		setInitialized(false);
	}

	@Nullable
	private UpdateDataSourceRepositoryTask updateDataSourceRepositoryTask;

	@SuppressWarnings("deprecation")
	private static class UpdateDataSourceRepositoryTask extends MTCancellableAsyncTask<Void, Void, Boolean> {

		private static final String LOG_TAG = UpdateDataSourceRepositoryTask.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final DataSourceProvider dataSourceProvider;
		private final boolean forceTriggerModulesUpdated;

		private UpdateDataSourceRepositoryTask(@NonNull DataSourceProvider dataSourceProvider, boolean forceTriggerModulesUpdated) {
			this.dataSourceProvider = dataSourceProvider;
			this.forceTriggerModulesUpdated = forceTriggerModulesUpdated;
		}

		@WorkerThread
		@Nullable
		@Override
		protected Boolean doInBackgroundNotCancelledMT(@Nullable Void... voids) {
			boolean updated = false;
			try {
				updated = this.dataSourceProvider.dataSourcesRepository.updateAsync().get();
				this.dataSourceProvider.setInitialized(true); // MARK AS INITIALIZED
				this.dataSourceProvider.allAgencies = new ArrayList<>(); // MARK AS INITIALIZED
			} catch (Exception e) {
				MTLog.w(this, e, "Error while updating data-sources from repository!");
			}
			return this.forceTriggerModulesUpdated || updated;
		}

		@Override
		protected void onPostExecuteNotCancelledMT(@Nullable Boolean updated) {
			super.onPostExecuteNotCancelledMT(updated);
			if (Boolean.TRUE.equals(updated)) {
				org.mtransit.android.data.DataSourceProvider.triggerModulesUpdated();
			}
		}
	}

	@SuppressLint("QueryPermissionsNeeded")
	private synchronized void init() {
		try {
			if (isInitialized()) {
				return; // already initialized
			}
			if (F_CACHE_DATA_SOURCES) {
				updateFromDataSourceRepository(false);
				return;
			}
			MTLog.i(this, "Initializing data-sources...");
			Context context = requireContext();
			String agencyProviderMetaData = getAgencyProviderMetaData(context);
			String agencyProviderTypeMetaData = context.getString(R.string.agency_provider_type);
			String rtsProviderMetaData = context.getString(R.string.rts_provider);
			String scheduleProviderMetaData = getScheduleProviderMetaData(context);
			String statusProviderMetaData = getStatusProviderMetaData(context);
			String serviceUpdateProviderMetaData = getServiceUpdateProviderMetaData(context);
			String newsProviderMetaData = getNewsProviderMetaData(context);
			String statusProviderTargetMetaData = context.getString(R.string.status_provider_target);
			String scheduleProviderTargetMetaData = context.getString(R.string.schedule_provider_target);
			String serviceUpdateProviderTargetMetaData = context.getString(R.string.service_update_provider_target);
			String newsProviderTargetMetaData = context.getString(R.string.news_provider_target);
			PackageManager pm = context.getPackageManager();
			for (PackageInfo packageInfo : pm.getInstalledPackages(PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA)) {
				ProviderInfo[] providers = packageInfo.providers;
				if (providers == null) {
					continue;
				}
				for (ProviderInfo provider : providers) {
					final String providerAuthority = provider.authority;
					final String providerPkg = provider.packageName;
					final Bundle providerMetaData = provider.metaData;
					if (providerMetaData == null) {
						continue;
					}
					if (agencyProviderMetaData.equals(providerMetaData.getString(agencyProviderMetaData))) {
						this.agenciesAuthorityPkg.put(providerAuthority, providerPkg);
						int agencyTypeId = providerMetaData.getInt(agencyProviderTypeMetaData, -1);
						if (agencyTypeId >= 0) {
							DataSourceType newAgencyType = DataSourceType.parseId(agencyTypeId);
							if (newAgencyType != null) {
								if (!this.allAgencyTypes.contains(newAgencyType)) {
									this.allAgencyTypes.add(newAgencyType);
								}
							}
							this.agenciesAuthorityTypeId.put(providerAuthority, agencyTypeId);
						}
						this.agenciesAuthorityIsRts.put( //
								providerAuthority, rtsProviderMetaData.equals(providerMetaData.getString(rtsProviderMetaData)));
						this.allAgenciesAuthority.add(providerAuthority);
					}
					if (statusProviderMetaData.equals(providerMetaData.getString(statusProviderMetaData))) {
						String targetAuthority = providerMetaData.getString(statusProviderTargetMetaData); // POI target authority OR empty
						StatusProviderProperties newStatusProvider = new StatusProviderProperties(providerAuthority, targetAuthority, providerPkg);
						this.allStatusProviders.add(newStatusProvider);
						this.allStatusProvidersByAuthority.put(newStatusProvider.getAuthority(), newStatusProvider);
						String newStatusProviderTargetAuthority = newStatusProvider.getTargetAuthority();
						HashSet<StatusProviderProperties> statusProviderProperties = this.statusProvidersByTargetAuthority.get(newStatusProviderTargetAuthority);
						if (statusProviderProperties == null) {
							statusProviderProperties = new HashSet<>();
						}
						statusProviderProperties.add(newStatusProvider);
						this.statusProvidersByTargetAuthority.put(newStatusProviderTargetAuthority, statusProviderProperties);
					}
					if (scheduleProviderMetaData.equals(providerMetaData.getString(scheduleProviderMetaData))) {
						String targetAuthority = providerMetaData.getString(scheduleProviderTargetMetaData); // POI target authority OR empty
						ScheduleProviderProperties newScheduleProvider = new ScheduleProviderProperties(providerAuthority, targetAuthority, providerPkg);
						this.allScheduleProviders.add(newScheduleProvider);
						this.allScheduleProvidersByAuthority.put(newScheduleProvider.getAuthority(), newScheduleProvider);
						String newScheduleProviderTargetAuthority = newScheduleProvider.getTargetAuthority();
						HashSet<ScheduleProviderProperties> scheduleProviderProperties = this.scheduleProvidersByTargetAuthority.get(newScheduleProviderTargetAuthority);
						if (scheduleProviderProperties == null) {
							scheduleProviderProperties = new HashSet<>();
						}
						scheduleProviderProperties.add(newScheduleProvider);
						this.scheduleProvidersByTargetAuthority.put(newScheduleProviderTargetAuthority, scheduleProviderProperties);
					}
					if (serviceUpdateProviderMetaData.equals(providerMetaData.getString(serviceUpdateProviderMetaData))) {
						String targetAuthority = providerMetaData.getString(serviceUpdateProviderTargetMetaData); // POI target authority OR empty
						ServiceUpdateProviderProperties newServiceUpdateProvider =
								new ServiceUpdateProviderProperties(providerAuthority, targetAuthority, providerPkg);
						this.allServiceUpdateProviders.add(newServiceUpdateProvider);
						this.allServiceUpdateProvidersByAuthority.put(newServiceUpdateProvider.getAuthority(), newServiceUpdateProvider);
						String newServiceUpdateProviderTargetAuthority = newServiceUpdateProvider.getTargetAuthority();
						HashSet<ServiceUpdateProviderProperties> serviceUpdateProviderProperties = this.serviceUpdateProvidersByTargetAuthority.get(newServiceUpdateProviderTargetAuthority);
						if (serviceUpdateProviderProperties == null) {
							serviceUpdateProviderProperties = new HashSet<>();
						}
						serviceUpdateProviderProperties.add(newServiceUpdateProvider);
						this.serviceUpdateProvidersByTargetAuthority.put(newServiceUpdateProviderTargetAuthority, serviceUpdateProviderProperties);
					}
					if (newsProviderMetaData.equals(providerMetaData.getString(newsProviderMetaData))) {
						String targetAuthority = providerMetaData.getString(newsProviderTargetMetaData); // POI target authority OR empty
						NewsProviderProperties newNewsProvider = new NewsProviderProperties(providerAuthority, targetAuthority, providerPkg);
						this.allNewsProviders.add(newNewsProvider);
						this.allNewsProvidersByAuthority.put(newNewsProvider.getAuthority(), newNewsProvider);
						String newNewsProviderTargetAuthority = newNewsProvider.getTargetAuthority();
						HashSet<NewsProviderProperties> newsProviderProperties = this.newsProvidersByTargetAuthority.get(newNewsProviderTargetAuthority);
						if (newsProviderProperties == null) {
							newsProviderProperties = new HashSet<>();
						}
						newsProviderProperties.add(newNewsProvider);
						this.newsProvidersByTargetAuthority.put(newNewsProviderTargetAuthority, newsProviderProperties);
					}
				}
			}
			CollectionUtils.sort(this.allAgencyTypes, new DataSourceType.DataSourceTypeShortNameComparator(context));
			analyticsManager.setUserProperty(AnalyticsUserProperties.MODULES_COUNT, allAgenciesAuthority.size());
			setInitialized(true);
			triggerModulesUpdated();
		} catch (Exception e) {
			MTLog.w(this, e, "Error while initializing properties!");
			destroy();
		}
		MTLog.i(this, "Initializing data-sources... DONE");
	}

	public void updateFromDataSourceRepository(boolean forceTriggerModulesUpdated) {
		if (this.updateDataSourceRepositoryTask != null && this.updateDataSourceRepositoryTask.getStatus() == UpdateDataSourceRepositoryTask.Status.RUNNING) {
			MTLog.d(this, "updateFromDataSourceRepository() > SKIP (already running)");
			return;
		}
		this.updateDataSourceRepositoryTask = new UpdateDataSourceRepositoryTask(this, forceTriggerModulesUpdated);
		TaskUtils.execute(this.updateDataSourceRepositoryTask);
	}

	@NonNull
	private static final WeakHashMap<ModulesUpdateListener, Object> modulesUpdateListeners = new WeakHashMap<>();

	public static void addModulesUpdateListener(@NonNull ModulesUpdateListener listener) {
		try {
			if (!modulesUpdateListeners.containsKey(listener)) {
				modulesUpdateListeners.put(listener, null);
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "addModulesUpdateListener() > error while adding listener '%s'!", listener);
		}
	}

	public static void removeModulesUpdateListener(@NonNull ModulesUpdateListener listener) {
		try {
			modulesUpdateListeners.remove(listener);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "removeModulesUpdateListener() > error while removing listener '%s'!", listener);
		}
	}

	@Nullable
	private static TriggerModulesUpdatedTask triggerModulesUpdatedTask = null;

	private static boolean triggerModulesUpdated = false;

	public static void triggerModulesUpdated() {
		if (F_CACHE_DATA_SOURCES) {
			return;
		}
		triggerModulesUpdated = true;
		if (!resumed) {
			return;
		}
		if (triggerModulesUpdatedTask != null && triggerModulesUpdatedTask.getStatus() != AsyncTask.Status.RUNNING) {
			triggerModulesUpdatedTask.cancel(true);
		}
		WeakHashMap<ModulesUpdateListener, Object> listenersCopy = new WeakHashMap<>(modulesUpdateListeners);
		triggerModulesUpdatedTask = new TriggerModulesUpdatedTask(listenersCopy);
		TaskUtils.execute(triggerModulesUpdatedTask);
		triggerModulesUpdated = false; // processed
	}

	public interface ModulesUpdateListener {
		void onModulesUpdated();
	}

	@SuppressWarnings("deprecation")
	private static class TriggerModulesUpdatedTask extends MTCancellableAsyncTask<Void, ModulesUpdateListener, Void> {

		private static final String LOG_TAG = DataSourceProvider.class.getSimpleName() + ">" + TriggerModulesUpdatedTask.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final WeakHashMap<ModulesUpdateListener, Object> listeners;

		TriggerModulesUpdatedTask(@NonNull WeakHashMap<ModulesUpdateListener, Object> listeners) {
			this.listeners = listeners;
		}

		@Override
		protected Void doInBackgroundNotCancelledMT(Void... params) {
			for (ModulesUpdateListener listener : this.listeners.keySet()) {
				if (listener != null) {
					publishProgress(listener);
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdateNotCancelledMT(ModulesUpdateListener... values) {
			if (values != null && values.length > 0) {
				try {
					values[0].onModulesUpdated();
				} catch (Throwable t) {
					MTLog.w(this, t, "Error while broadcasting module updated for %!", values[0]);
				}
			}
		}
	}
}
